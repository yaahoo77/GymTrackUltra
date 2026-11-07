package com.gymtrack.ultra.domain.progression

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class XpCalculatorTest {

    private fun input(planned: Int, done: Int, warmup: Boolean = false, already: Boolean = false) =
        SessionXpInput(planned, done, warmup, already)

    @Test
    fun `full plan with warmup gives the 120 xp maximum`() {
        val award = XpCalculator.forSession(input(14, 14, warmup = true))
        assertEquals(XpCalculator.MAX_PER_WORKOUT, award.total)
        assertEquals(120, award.total)
    }

    @Test
    fun `half of the plan earns base plus proportional plan xp`() {
        val award = XpCalculator.forSession(input(14, 7))
        // 50 bazowe + 25 za połowę planu, bez premii za komplet.
        assertEquals(75, award.total)
        assertTrue(award.lines.none { it.reason == XpReason.FULL_PLAN })
    }

    @Test
    fun `short session below half still earns some plan xp but no base`() {
        val award = XpCalculator.forSession(input(14, 3))
        assertTrue(award.lines.none { it.reason == XpReason.WORKOUT_COMPLETED })
        assertEquals(11, award.total) // round(50 * 3/14) = 11
    }

    @Test
    fun `extra sets beyond the plan never add xp`() {
        val capped = XpCalculator.cappedCompletedSets(listOf(3 to 6, 3 to 3, 2 to 10))
        assertEquals(8, capped)
        val award = XpCalculator.forSession(input(8, capped))
        val awardWithoutExtras = XpCalculator.forSession(input(8, 8))
        assertEquals(awardWithoutExtras.total, award.total)
    }

    @Test
    fun `second workout on the same day earns zero with an explanation`() {
        val award = XpCalculator.forSession(input(14, 14, warmup = true, already = true))
        assertEquals(0, award.total)
        assertNotNull(award.note)
    }

    @Test
    fun `no completed sets means no xp`() {
        assertEquals(0, XpCalculator.forSession(input(14, 0, warmup = true)).total)
        assertEquals(0, XpCalculator.forSession(input(0, 0)).total)
    }

    @Test
    fun `weekly bonus is granted once when target reached`() {
        assertNotNull(XpCalculator.weeklyGoalBonus(3, 3, alreadyAwardedThisWeek = false))
        assertNull(XpCalculator.weeklyGoalBonus(3, 3, alreadyAwardedThisWeek = true))
        assertNull(XpCalculator.weeklyGoalBonus(2, 3, alreadyAwardedThisWeek = false))
        assertNull(XpCalculator.weeklyGoalBonus(5, 0, alreadyAwardedThisWeek = false))
    }

    @Test
    fun `completed sets above planned are clamped`() {
        val award = XpCalculator.forSession(input(10, 25))
        assertEquals(110, award.total) // 50 + 50 + 10 (komplet), bez rozgrzewki
    }
}
