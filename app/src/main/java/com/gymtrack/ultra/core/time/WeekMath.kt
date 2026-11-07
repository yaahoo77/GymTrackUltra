package com.gymtrack.ultra.core.time

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

/** Obliczenia tygodniowe. Tydzień zaczyna się w poniedziałek (ISO, jak w Polsce). */
object WeekMath {

    fun weekStart(date: LocalDate): LocalDate =
        date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

    fun localDate(instant: Instant, zone: ZoneId): LocalDate =
        instant.atZone(zone).toLocalDate()

    /**
     * Najdłuższa seria KOLEJNYCH tygodni, w których liczba treningów >= [minPerWeek].
     * Liczymy tygodnie, nie dni — wolny dzień nigdy nie przerywa serii.
     */
    fun longestWeekStreak(countsByWeekStart: Map<LocalDate, Int>, minPerWeek: Int): Int {
        val qualifying = countsByWeekStart
            .filterValues { it >= minPerWeek }
            .keys
            .sorted()
        var best = 0
        var current = 0
        var previous: LocalDate? = null
        for (week in qualifying) {
            current = if (previous != null && previous.plusWeeks(1) == week) current + 1 else 1
            if (current > best) best = current
            previous = week
        }
        return best
    }
}
