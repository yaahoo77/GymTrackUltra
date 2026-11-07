package com.gymtrack.ultra.domain.progression

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RanksTest {

    @Test
    fun `ladder has 8 tiers with three divisions plus legend`() {
        assertEquals("ŻELAZO I", Rank(0).label)
        assertEquals("ŻELAZO III", Rank(2).label)
        assertEquals("BRĄZ I", Rank(3).label)
        assertEquals("ZŁOTO II", Rank.of(RankTier.GOLD, 2).label)
        assertEquals("MISTRZ III", Rank(23).label)
        assertEquals("LEGENDA", Rank(24).label)
        assertEquals(0, Rank(24).division)
        assertNull(Rank(24).next)
    }

    @Test
    fun `muscle curve thresholds`() {
        assertEquals(0, MuscleRankCurve.totalFor(0))
        assertEquals(40, MuscleRankCurve.totalFor(1))
        assertEquals(900, MuscleRankCurve.totalFor(9)) // Złoto I
        assertEquals(5_100, MuscleRankCurve.totalFor(24)) // Legenda
    }

    @Test
    fun `muscle progress lands on the right division`() {
        assertEquals(0, MuscleRankCurve.progressFor(39).rank.index)
        assertEquals(1, MuscleRankCurve.progressFor(40).rank.index)
        val gold = MuscleRankCurve.progressFor(950)
        assertEquals(RankTier.GOLD, gold.rank.tier)
        assertEquals(50, gold.pointsIntoRank)
        assertEquals(175, gold.pointsForNext)
        val legend = MuscleRankCurve.progressFor(1_000_000)
        assertTrue(legend.isMax)
        assertEquals(RankTier.LEGEND, legend.rank.tier)
    }

    @Test
    fun `ranks compare by position`() {
        assertTrue(Rank.of(RankTier.SILVER, 3) < Rank.of(RankTier.GOLD, 1))
    }
}
