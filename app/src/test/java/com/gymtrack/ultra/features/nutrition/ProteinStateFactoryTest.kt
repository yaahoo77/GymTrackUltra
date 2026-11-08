package com.gymtrack.ultra.features.nutrition

import com.gymtrack.ultra.domain.model.UserSettings
import com.gymtrack.ultra.domain.nutrition.FoodEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class ProteinStateFactoryTest {

    private val today = LocalDate.of(2026, 11, 8)
    private fun entry(date: LocalDate, grams: Int, minute: Long) =
        FoodEntry(minute, date, "x$minute", grams, Instant.ofEpochSecond(date.toEpochDay() * 86_400 + minute * 60))

    @Test
    fun `without weight or manual goal there is no target`() {
        val state = ProteinStateFactory.build(UserSettings(), emptyList(), emptyList(), today)
        assertNull(state.target)
        assertFalse(state.reachedToday)
    }

    @Test
    fun `sums only today and reports remaining`() {
        val settings = UserSettings(bodyWeightKg = 70.0)
        val entries = listOf(entry(today, 30, 1), entry(today, 22, 2), entry(today.minusDays(1), 90, 3))
        val state = ProteinStateFactory.build(settings, entries, entries, today)
        assertEquals(110, state.target)
        assertEquals(52, state.eatenToday)
        assertEquals(58, state.remaining)
        assertEquals(2, state.todayEntries.size)
        assertEquals(22, state.todayEntries.first().proteinGrams) // najnowszy pierwszy
        assertEquals(7, state.week.size)
    }

    @Test
    fun `home card is hidden when tracking is off`() {
        assertNull(ProteinStateFactory.card(UserSettings(proteinTracking = false), emptyList(), today))
        val card = ProteinStateFactory.card(UserSettings(manualProteinTarget = 100), listOf(entry(today, 120, 1)), today)
        assertEquals(100, card?.target)
        assertTrue(card!!.progress == 1f)
    }
}
