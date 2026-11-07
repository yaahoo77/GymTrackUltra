package com.gymtrack.ultra.domain.progression

import kotlin.math.min
import kotlin.math.roundToInt

enum class XpReason(val label: String) {
    WORKOUT_COMPLETED("Ukończony trening"),
    PLAN_PROGRESS("Realizacja planu"),
    WARMUP("Rozgrzewka"),
    FULL_PLAN("Komplet serii"),
    WEEKLY_GOAL("Cel tygodnia"),
}

data class XpLine(val reason: XpReason, val amount: Int)

data class XpAward(
    val lines: List<XpLine>,
    /** Wyjaśnienie, gdy XP celowo nie przyznano (np. drugi trening tego samego dnia). */
    val note: String? = null,
) {
    val total: Int get() = lines.sumOf { it.amount }
}

data class SessionXpInput(
    val plannedSets: Int,
    /** Serie zaliczone W RAMACH planu — już przycięte per ćwiczenie, patrz [XpCalculator.cappedCompletedSets]. */
    val completedPlannedSets: Int,
    val warmupDone: Boolean,
    val workoutXpAlreadyAwardedToday: Boolean,
)

/**
 * Zasady XP — nagradzamy regularność i realizację planu, nie objętość:
 *  • serie ponad plan dają 0 XP,
 *  • XP za trening liczy się raz na dzień kalendarzowy,
 *  • maks. 120 XP za trening (+50 raz w tygodniu za cel tygodnia),
 *  • brak kar — pominięty dzień nic nie odbiera.
 */
object XpCalculator {
    const val COMPLETION_BASE = 50
    const val PLAN_MAX = 50
    const val WARMUP_BONUS = 10
    const val FULL_PLAN_BONUS = 10
    const val WEEKLY_GOAL_BONUS = 50
    const val MIN_RATIO_FOR_BASE = 0.5
    const val MAX_PER_WORKOUT = COMPLETION_BASE + PLAN_MAX + WARMUP_BONUS + FULL_PLAN_BONUS

    const val DAILY_LIMIT_NOTE =
        "XP za trening liczy się raz dziennie. Dzisiejsza praca jest zapisana — regeneracja też buduje siłę."

    /** Suma min(zaliczone, zaplanowane) po ćwiczeniach: dokładanie serii nie zwiększa XP. */
    fun cappedCompletedSets(targetsAndCompleted: List<Pair<Int, Int>>): Int =
        targetsAndCompleted.sumOf { (target, completed) -> min(target.coerceAtLeast(0), completed.coerceAtLeast(0)) }

    fun forSession(input: SessionXpInput): XpAward {
        if (input.workoutXpAlreadyAwardedToday) return XpAward(emptyList(), DAILY_LIMIT_NOTE)
        if (input.plannedSets <= 0 || input.completedPlannedSets <= 0) return XpAward(emptyList())

        val done = input.completedPlannedSets.coerceIn(0, input.plannedSets)
        val ratio = done.toDouble() / input.plannedSets

        val lines = buildList {
            if (ratio >= MIN_RATIO_FOR_BASE) add(XpLine(XpReason.WORKOUT_COMPLETED, COMPLETION_BASE))
            add(XpLine(XpReason.PLAN_PROGRESS, (PLAN_MAX * ratio).roundToInt().coerceAtLeast(1)))
            if (input.warmupDone) add(XpLine(XpReason.WARMUP, WARMUP_BONUS))
            if (done == input.plannedSets) add(XpLine(XpReason.FULL_PLAN, FULL_PLAN_BONUS))
        }
        return XpAward(lines)
    }

    /** Premia za osiągnięcie celu tygodnia — najwyżej raz w tygodniu. */
    fun weeklyGoalBonus(
        completedThisWeekIncludingCurrent: Int,
        weeklyTarget: Int,
        alreadyAwardedThisWeek: Boolean,
    ): XpLine? =
        if (!alreadyAwardedThisWeek && weeklyTarget > 0 && completedThisWeekIncludingCurrent >= weeklyTarget) {
            XpLine(XpReason.WEEKLY_GOAL, WEEKLY_GOAL_BONUS)
        } else {
            null
        }
}
