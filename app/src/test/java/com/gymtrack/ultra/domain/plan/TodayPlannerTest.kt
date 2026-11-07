package com.gymtrack.ultra.domain.plan

import com.gymtrack.ultra.domain.model.PlanDay
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class TodayPlannerTest {

    private fun day(id: Long, code: String, dow: DayOfWeek) =
        PlanDay(id = id, code = code, title = "WORKOUT $code", dayOfWeek = dow, position = id.toInt(), exercises = emptyList())

    private val plan = listOf(
        day(1, "A", DayOfWeek.MONDAY),
        day(2, "B", DayOfWeek.WEDNESDAY),
        day(3, "C", DayOfWeek.FRIDAY),
    )

    @Test
    fun `planned day returns today's workout`() {
        val wednesday = LocalDate.of(2026, 11, 4)
        val result = TodayPlanner.resolve(plan, wednesday, emptySet())!!
        assertEquals("B", result.day.code)
        assertTrue(result.isToday)
        assertFalse(result.doneToday)
    }

    @Test
    fun `rest day points to the next workout`() {
        val saturday = LocalDate.of(2026, 11, 7)
        val result = TodayPlanner.resolve(plan, saturday, emptySet())!!
        assertEquals("A", result.day.code)
        assertFalse(result.isToday)
        assertEquals(2, result.daysUntil)
    }

    @Test
    fun `completed today is reported`() {
        val monday = LocalDate.of(2026, 11, 2)
        val result = TodayPlanner.resolve(plan, monday, setOf(1L))!!
        assertTrue(result.doneToday)
    }

    @Test
    fun `empty plan returns null`() {
        assertNull(TodayPlanner.resolve(emptyList(), LocalDate.of(2026, 11, 2), emptySet()))
    }
}
