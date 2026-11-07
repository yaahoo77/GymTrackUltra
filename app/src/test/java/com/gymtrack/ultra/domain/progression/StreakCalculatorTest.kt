package com.gymtrack.ultra.domain.progression

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class StreakCalculatorTest {

    private val monday = LocalDate.of(2026, 9, 7)
    private fun week(n: Long, vararg days: Long) = days.map { monday.plusWeeks(n).plusDays(it) }

    @Test
    fun `no workouts no streak`() {
        val s = StreakCalculator.compute(emptyList(), monday, 2)
        assertEquals(0, s.current)
    }

    @Test
    fun `rest days never break the streak`() {
        val dates = week(0, 0, 2, 4) + week(1, 1, 3) + week(2, 0)
        val s = StreakCalculator.compute(dates, monday.plusWeeks(2).plusDays(3), 2)
        assertEquals(6, s.current)
        assertEquals(1, s.thisWeekCount)
        assertEquals(1, s.neededThisWeek)
    }

    @Test
    fun `a weak week without shields resets`() {
        val dates = week(0, 0, 2) + week(1, 0) + week(2, 0, 3)
        val s = StreakCalculator.compute(dates, monday.plusWeeks(2).plusDays(4), 2)
        assertEquals(2, s.current)
        assertEquals(2, s.longest)
    }

    @Test
    fun `shield earned after 4 good weeks saves a weak week`() {
        val dates = (0L..3L).flatMap { week(it, 0, 3) } + week(4, 1) + week(5, 0)
        val s = StreakCalculator.compute(dates, monday.plusWeeks(5).plusDays(1), 2)
        assertEquals(10, s.current)
        assertEquals(0, s.shields)
        assertTrue(s.shieldUsedLastWeek)
    }

    @Test
    fun `trained today flag`() {
        val s = StreakCalculator.compute(week(0, 2), monday.plusDays(2), 2)
        assertTrue(s.trainedToday)
        assertEquals(1, s.current)
    }
}
