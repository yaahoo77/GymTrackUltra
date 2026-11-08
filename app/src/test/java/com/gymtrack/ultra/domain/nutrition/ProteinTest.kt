package com.gymtrack.ultra.domain.nutrition

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class ProteinTest {

    @Test
    fun `target is weight times goal rounded to 5 g`() {
        assertEquals(110, ProteinCalculator.dailyTarget(70.0, ProteinGoal.TRAINING)) // 112 -> 110
        assertEquals(85, ProteinCalculator.dailyTarget(70.0, ProteinGoal.BASIC)) // 84 -> 85
        assertEquals(95, ProteinCalculator.dailyTarget(60.0, ProteinGoal.TRAINING)) // 96 -> 95
    }

    @Test
    fun `no goal ever exceeds 1_6 g per kg`() {
        ProteinGoal.entries.forEach { assertTrue(it.gramsPerKg <= 1.6) }
    }

    @Test
    fun `weight outside sane range is rejected and targets are clamped`() {
        assertFalse(ProteinCalculator.isValidWeight(12.0))
        assertFalse(ProteinCalculator.isValidWeight(500.0))
        assertFalse(ProteinCalculator.isValidWeight(null))
        assertTrue(ProteinCalculator.isValidWeight(55.5))
        assertEquals(ProteinCalculator.MAX_TARGET, ProteinCalculator.resolveTarget(null, ProteinGoal.TRAINING, 900))
    }

    @Test
    fun `manual target wins and nothing configured means no target`() {
        assertEquals(120, ProteinCalculator.resolveTarget(70.0, ProteinGoal.TRAINING, 120))
        assertEquals(110, ProteinCalculator.resolveTarget(70.0, ProteinGoal.TRAINING, null))
        assertNull(ProteinCalculator.resolveTarget(null, ProteinGoal.TRAINING, null))
    }

    @Test
    fun `last days include empty days and sum entries per day`() {
        val today = LocalDate.of(2026, 11, 8)
        val entries = listOf(
            entry(today, 30), entry(today, 6), entry(today.minusDays(2), 50), entry(today.minusDays(10), 99),
        )
        val days = ProteinStats.lastDays(entries, today, 7)
        assertEquals(7, days.size)
        assertEquals(today, days.last().date)
        assertEquals(36, days.last().grams)
        assertEquals(50, days[4].grams)
        assertEquals(0, days[0].grams)
        assertEquals(1, ProteinStats.daysOnTarget(days, 50))
    }

    @Test
    fun `recent custom foods skip presets and duplicates`() {
        val d = LocalDate.of(2026, 11, 8)
        val entries = listOf(
            entry(d, 6, "Jajko", 1), entry(d, 20, "Kebab", 2), entry(d, 20, "kebab", 3), entry(d, 15, "Pierogi", 4),
        )
        val recent = ProteinStats.recentCustom(entries, FoodPresets.all.map { it.name }.toSet())
        assertEquals(listOf("Pierogi" to 15, "kebab" to 20), recent)
    }

    @Test
    fun `presets have unique ids and sane values`() {
        assertEquals(FoodPresets.all.size, FoodPresets.all.map { it.id }.toSet().size)
        FoodPresets.all.forEach { assertTrue(it.proteinGrams in 1..ProteinCalculator.MAX_ENTRY_GRAMS) }
    }

    private fun entry(date: LocalDate, grams: Int, name: String = "x", second: Long = 0) =
        FoodEntry(0, date, name, grams, Instant.ofEpochSecond(date.toEpochDay() * 86_400 + second))
}
