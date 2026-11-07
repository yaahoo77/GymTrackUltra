package com.gymtrack.ultra.domain.progression

import com.gymtrack.ultra.core.time.WeekMath
import java.time.LocalDate

data class StreakState(
    /** Liczba treningów w trwającej serii. */
    val current: Int,
    val longest: Int,
    /** Tarcze chroniące serię w słabszym tygodniu (choroba, wyjazd). */
    val shields: Int,
    val thisWeekCount: Int,
    val weeklyMinimum: Int,
    val trainedToday: Boolean,
    /** Tarcza zużyta w poprzednim tygodniu — do komunikatu "tarcza uratowała serię". */
    val shieldUsedLastWeek: Boolean,
) {
    val isSafeThisWeek: Boolean get() = thisWeekCount >= weeklyMinimum
    val neededThisWeek: Int get() = (weeklyMinimum - thisWeekCount).coerceAtLeast(0)
    val isLit: Boolean get() = current > 0

    companion object {
        val Empty = StreakState(0, 0, 0, 0, 2, trainedToday = false, shieldUsedLastWeek = false)
    }
}

/**
 * Seria w stylu Duolingo, ale bez kar za odpoczynek:
 *  • licznik = liczba treningów w nieprzerwanej serii,
 *  • dni wolne niczego nie psują — liczy się tydzień: min. [weeklyMinimum] treningi,
 *  • słabszy tydzień gasi serię, chyba że masz tarczę (1 za każde 4 pełne tygodnie, maks. 2).
 */
object StreakCalculator {
    const val MAX_SHIELDS = 2
    const val WEEKS_PER_SHIELD = 4

    fun weeklyMinimum(planDays: Int): Int = planDays.coerceIn(1, 2)

    fun compute(sessionDates: List<LocalDate>, today: LocalDate, weeklyMinimum: Int): StreakState {
        val minimum = weeklyMinimum.coerceAtLeast(1)
        val counts = sessionDates.groupingBy { WeekMath.weekStart(it) }.eachCount()
        val currentWeek = WeekMath.weekStart(today)
        val firstWeek = counts.keys.minOrNull()
            ?: return StreakState.Empty.copy(weeklyMinimum = minimum)

        var streak = 0
        var longest = 0
        var shields = 0
        var goalRun = 0
        var shieldUsedLastWeek = false

        var week = firstWeek
        while (week.isBefore(currentWeek)) {
            val count = counts[week] ?: 0
            val isLastWeek = week.plusWeeks(1) == currentWeek
            when {
                count >= minimum -> {
                    streak += count
                    goalRun++
                    if (goalRun % WEEKS_PER_SHIELD == 0 && shields < MAX_SHIELDS) shields++
                }
                shields > 0 && streak > 0 -> {
                    shields--
                    streak += count
                    goalRun = 0
                    if (isLastWeek) shieldUsedLastWeek = true
                }
                else -> {
                    streak = 0
                    goalRun = 0
                }
            }
            if (streak > longest) longest = streak
            week = week.plusWeeks(1)
        }

        val thisWeek = counts[currentWeek] ?: 0
        streak += thisWeek
        if (streak > longest) longest = streak

        return StreakState(
            current = streak,
            longest = longest,
            shields = shields,
            thisWeekCount = thisWeek,
            weeklyMinimum = minimum,
            trainedToday = today in sessionDates,
            shieldUsedLastWeek = shieldUsedLastWeek,
        )
    }
}
