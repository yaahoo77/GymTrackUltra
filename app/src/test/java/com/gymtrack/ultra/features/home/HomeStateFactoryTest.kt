package com.gymtrack.ultra.features.home

import com.gymtrack.ultra.domain.model.PlanDay
import com.gymtrack.ultra.domain.model.ProgressSnapshot
import com.gymtrack.ultra.domain.model.SessionSummary
import com.gymtrack.ultra.domain.model.UserSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneOffset

class HomeStateFactoryTest {

    private val zone = ZoneOffset.UTC
    private val plan = listOf(
        PlanDay(1, "A", "WORKOUT A", DayOfWeek.MONDAY, 0, emptyList()),
        PlanDay(2, "B", "WORKOUT B", DayOfWeek.WEDNESDAY, 1, emptyList()),
        PlanDay(3, "C", "WORKOUT C", DayOfWeek.FRIDAY, 2, emptyList()),
    )

    private fun at(date: LocalDate, hour: Int = 18) = date.atTime(hour, 0).toInstant(ZoneOffset.UTC)

    @Test
    fun `wednesday shows workout B as today`() {
        val wed = LocalDate.of(2026, 11, 4)
        val state = HomeStateFactory.build(plan, ProgressSnapshot.Empty, UserSettings(), at(wed), zone)
        assertEquals("TODAY'S WORKOUT", state.today?.eyebrow)
        assertEquals("WORKOUT B", state.today?.title)
        assertEquals("Gotowy na B?", state.headline)
        assertEquals(WeekDayState.TODAY_PLANNED, state.week[2].state)
        assertEquals(3, state.weeklyTarget)
    }

    @Test
    fun `missed planned day is shown neutrally, never as a penalty`() {
        // Środa bez treningu w poniedziałek: poniedziałek = zwykła regeneracja.
        val wed = LocalDate.of(2026, 11, 4)
        val state = HomeStateFactory.build(plan, ProgressSnapshot.Empty, UserSettings(), at(wed), zone)
        assertEquals(WeekDayState.REST, state.week[0].state)
        assertEquals(WeekDayState.PLANNED, state.week[4].state)
    }

    @Test
    fun `completed session marks the day as done`() {
        val mon = LocalDate.of(2026, 11, 2)
        val session = SessionSummary(10, 1, "WORKOUT A", at(mon, 17), at(mon, 18), true, 120)
        val progress = ProgressSnapshot.Empty.copy(completedSessions = listOf(session))
        val state = HomeStateFactory.build(plan, progress, UserSettings(), at(mon, 20), zone)
        assertEquals(WeekDayState.DONE, state.week[0].state)
        assertEquals("DZIŚ ZROBIONE", state.today?.eyebrow)
        assertEquals(1, state.weeklyDone)
        assertTrue(state.today?.doneToday == true)
    }

    @Test
    fun `saturday is a recovery day pointing to monday`() {
        val sat = LocalDate.of(2026, 11, 7)
        val state = HomeStateFactory.build(plan, ProgressSnapshot.Empty, UserSettings(), at(sat), zone)
        assertEquals("Dziś regeneracja", state.headline)
        assertEquals("NEXT WORKOUT", state.today?.eyebrow)
        assertEquals("W poniedziałek", state.today?.whenText)
        assertEquals(WeekDayState.TODAY_REST, state.week[5].state)
    }

    @Test
    fun `greeting includes name when given`() {
        assertEquals("Dobry wieczór, Ola", HomeStateFactory.greetingFor(19, " Ola "))
        assertEquals("Dzień dobry", HomeStateFactory.greetingFor(8, ""))
    }
}
