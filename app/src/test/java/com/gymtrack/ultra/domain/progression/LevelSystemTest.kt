package com.gymtrack.ultra.domain.progression

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LevelSystemTest {

    @Test
    fun `zero xp is level 1 ignite`() {
        val p = LevelSystem.progressFor(0)
        assertEquals(1, p.level)
        assertEquals(Tier.IGNITE, p.tier)
        assertEquals(0, p.xpIntoLevel)
        assertEquals(100, p.xpForNextLevel)
    }

    @Test
    fun `level thresholds follow the curve`() {
        assertEquals(0, LevelSystem.totalXpForLevel(1))
        assertEquals(100, LevelSystem.totalXpForLevel(2))
        assertEquals(550, LevelSystem.totalXpForLevel(5))
        assertEquals(1_800, LevelSystem.totalXpForLevel(10))
        assertEquals(34_300, LevelSystem.totalXpForLevel(50))
    }

    @Test
    fun `boundary values land on the right level`() {
        assertEquals(1, LevelSystem.progressFor(99).level)
        assertEquals(2, LevelSystem.progressFor(100).level)
        assertEquals(4, LevelSystem.progressFor(549).level)
        assertEquals(5, LevelSystem.progressFor(550).level)
    }

    @Test
    fun `xp into level and next requirement are consistent`() {
        // Poziom 7 zaczyna się od 975 XP, przejście na 8 kosztuje 250.
        val p = LevelSystem.progressFor(1_155)
        assertEquals(7, p.level)
        assertEquals(180, p.xpIntoLevel)
        assertEquals(250, p.xpForNextLevel)
        assertEquals(0.72f, p.fraction, 0.001f)
    }

    @Test
    fun `tiers start at specified levels`() {
        assertEquals(Tier.IGNITE, Tier.forLevel(4))
        assertEquals(Tier.PATHFINDER, Tier.forLevel(5))
        assertEquals(Tier.FORGE, Tier.forLevel(10))
        assertEquals(Tier.VANGUARD, Tier.forLevel(20))
        assertEquals(Tier.COLOSSUS, Tier.forLevel(30))
        assertEquals(Tier.ASCENDANT, Tier.forLevel(50))
        assertEquals(Tier.ASCENDANT, Tier.forLevel(99))
    }

    @Test
    fun `negative xp is treated as zero`() {
        assertEquals(1, LevelSystem.progressFor(-500).level)
    }

    @Test
    fun `max level caps progress`() {
        val p = LevelSystem.progressFor(10_000_000)
        assertEquals(LevelSystem.MAX_LEVEL, p.level)
        assertTrue(p.isMaxLevel)
        assertEquals(1f, p.fraction, 0f)
    }

    @Test
    fun `levels gained detects multi level jumps`() {
        assertEquals(2..3, LevelSystem.levelsGained(90, 230))
        assertTrue(LevelSystem.levelsGained(110, 150).isEmpty())
    }
}
