package com.gymtrack.ultra.core.time

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class WeekMathTest {

    @Test
    fun `week starts on monday`() {
        val sunday = LocalDate.of(2026, 11, 8)
        assertEquals(LocalDate.of(2026, 11, 2), WeekMath.weekStart(sunday))
        val monday = LocalDate.of(2026, 11, 2)
        assertEquals(monday, WeekMath.weekStart(monday))
    }

    @Test
    fun `streak counts only consecutive qualifying weeks`() {
        val w = LocalDate.of(2026, 9, 7)
        val counts = mapOf(
            w to 2,
            w.plusWeeks(1) to 3,
            w.plusWeeks(2) to 1, // przerwa w serii
            w.plusWeeks(3) to 2,
            w.plusWeeks(4) to 2,
            w.plusWeeks(5) to 4,
        )
        assertEquals(3, WeekMath.longestWeekStreak(counts, minPerWeek = 2))
    }

    @Test
    fun `missing week breaks streak`() {
        val w = LocalDate.of(2026, 9, 7)
        val counts = mapOf(w to 2, w.plusWeeks(2) to 2)
        assertEquals(1, WeekMath.longestWeekStreak(counts, minPerWeek = 2))
    }

    @Test
    fun `empty history has zero streak`() {
        assertEquals(0, WeekMath.longestWeekStreak(emptyMap(), minPerWeek = 2))
    }
}
