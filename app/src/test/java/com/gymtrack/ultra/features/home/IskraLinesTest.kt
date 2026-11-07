package com.gymtrack.ultra.features.home

import com.gymtrack.ultra.domain.model.PlanDay
import com.gymtrack.ultra.domain.model.ProgressSnapshot
import com.gymtrack.ultra.domain.model.UserSettings
import com.gymtrack.ultra.ui.components.IskraMood
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneOffset

class IskraLinesTest {

    private val zone = ZoneOffset.UTC
    private val plan = listOf(
        PlanDay(1, "A", "WORKOUT A", DayOfWeek.MONDAY, 0, emptyList()),
        PlanDay(2, "B", "WORKOUT B", DayOfWeek.WEDNESDAY, 1, emptyList()),
        PlanDay(3, "C", "WORKOUT C", DayOfWeek.FRIDAY, 2, emptyList()),
    )

    private fun stateOn(date: LocalDate) =
        HomeStateFactory.build(plan, ProgressSnapshot.Empty, UserSettings(), date.atTime(18, 0).toInstant(zone), zone)

    @Test
    fun `training day greets with the day's workout`() {
        val line = IskraLines.headline(stateOn(LocalDate.of(2026, 11, 4)))
        assertEquals(IskraMood.HAPPY, line.mood)
        assertTrue(line.text.contains("WORKOUT B"))
    }

    @Test
    fun `rest day is calm and never threatens the streak`() {
        val line = IskraLines.headline(stateOn(LocalDate.of(2026, 11, 5)))
        assertEquals(IskraMood.SLEEPY, line.mood)
        assertFalse(line.text.contains("strac", ignoreCase = true))
    }

    @Test
    fun `active session takes priority`() {
        val state = stateOn(LocalDate.of(2026, 11, 5)).copy(activeSession = ActiveSessionUi("WORKOUT B", 3, 15))
        val line = IskraLines.headline(state)
        assertEquals(IskraMood.FOCUS, line.mood)
        assertTrue(line.text.contains("3/15"))
    }

    @Test
    fun `tapping cycles through tips and wakes a sleepy Iskra`() {
        val state = stateOn(LocalDate.of(2026, 11, 5))
        val first = IskraLines.lineAt(state, 1)
        assertEquals(IskraLines.tips[0], first.text)
        assertEquals(IskraMood.HAPPY, first.mood)
        assertEquals(IskraLines.tips[0], IskraLines.lineAt(state, IskraLines.tips.size + 1).text)
    }
}
