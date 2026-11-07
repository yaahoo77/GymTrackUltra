package com.gymtrack.ultra.domain.progression

import com.gymtrack.ultra.domain.model.SessionSummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

class AchievementEvaluatorTest {

    private val zone: ZoneId = ZoneOffset.UTC

    private fun progressOf(code: String, stats: AchievementStats, unlocked: Map<String, Long> = emptyMap()) =
        AchievementEvaluator.evaluate(stats, unlocked).first { it.definition.code == code }

    @Test
    fun `nothing is unlocked for a fresh user`() {
        val all = AchievementEvaluator.evaluate(AchievementStats(techniqueTotal = 19), emptyMap())
        assertEquals(Achievements.all.size, all.size)
        assertTrue(all.none { it.isUnlocked })
    }

    @Test
    fun `first rep unlocks after one completed set`() {
        assertTrue(progressOf(Achievements.FIRST_REP, AchievementStats(completedSets = 1)).isUnlocked)
    }

    @Test
    fun `workout counters progress and clamp at target`() {
        val p = progressOf(Achievements.WORKOUTS_10, AchievementStats(completedWorkouts = 7))
        assertFalse(p.isUnlocked)
        assertEquals(0.7f, p.fraction, 0.001f)
        assertTrue(p.showsProgressBar)
        val done = progressOf(Achievements.WORKOUTS_10, AchievementStats(completedWorkouts = 25))
        assertEquals(10, done.current)
        assertTrue(done.isUnlocked)
    }

    @Test
    fun `technique master target follows plan size`() {
        val stats = AchievementStats(techniqueViewed = 12, techniqueTotal = 19)
        val p = progressOf(Achievements.TECHNIQUE_MASTER, stats)
        assertEquals(19, p.target)
        assertFalse(p.isUnlocked)
        assertTrue(progressOf(Achievements.TECHNIQUE_MASTER, stats.copy(techniqueViewed = 19)).isUnlocked)
    }

    @Test
    fun `action achievements unlock only when stored`() {
        assertFalse(progressOf(Achievements.SMART_LOAD, AchievementStats()).isUnlocked)
        val p = progressOf(Achievements.SMART_LOAD, AchievementStats(), mapOf(Achievements.SMART_LOAD to 123L))
        assertTrue(p.isUnlocked)
        assertEquals(123L, p.unlockedAt)
    }

    @Test
    fun `newly reached excludes already stored unlocks`() {
        val stats = AchievementStats(completedSets = 5, completedWorkouts = 1)
        val fresh = AchievementEvaluator.newlyReached(stats, emptyMap()).map { it.code }.toSet()
        assertEquals(setOf(Achievements.FIRST_REP, Achievements.FIRST_WORKOUT), fresh)
        val later = AchievementEvaluator.newlyReached(stats, mapOf(Achievements.FIRST_REP to 1L)).map { it.code }
        assertEquals(listOf(Achievements.FIRST_WORKOUT), later)
    }

    @Test
    fun `stats builder counts weeks streaks and full cycles`() {
        fun session(id: Long, date: LocalDate, planDay: Long, warmup: Boolean = true) = SessionSummary(
            id = id,
            planDayId = planDay,
            title = "W",
            startedAt = date.atTime(17, 0).toInstant(ZoneOffset.UTC),
            endedAt = date.atTime(17, 45).toInstant(ZoneOffset.UTC),
            warmupDone = warmup,
            xpEarned = 100,
        )
        val monday = LocalDate.of(2026, 10, 5)
        val sessions = listOf(
            // Tydzień 1: A, B, C
            session(1, monday, 1), session(2, monday.plusDays(2), 2), session(3, monday.plusDays(4), 3),
            // Tydzień 2: A, B
            session(4, monday.plusWeeks(1), 1), session(5, monday.plusWeeks(1).plusDays(2), 2, warmup = false),
            // Tydzień 3: tylko 1 trening (przerywa serię ≥2)
            session(6, monday.plusWeeks(2), 1),
            // Tydzień 4–5: po 2
            session(7, monday.plusWeeks(3), 1), session(8, monday.plusWeeks(3).plusDays(3), 2),
            session(9, monday.plusWeeks(4), 1), session(10, monday.plusWeeks(4).plusDays(1), 3),
        )
        val stats = AchievementStatsBuilder.build(sessions, completedSets = 120, techniqueViewed = 3, techniqueTotal = 19, zone = zone)
        assertEquals(10, stats.completedWorkouts)
        assertEquals(9, stats.warmupWorkouts)
        assertEquals(3, stats.maxPlanDaysInOneWeek)
        assertEquals(2, stats.longestWeekStreak)
        assertTrue(progressOf(Achievements.FULL_CYCLE, stats).isUnlocked)
        assertFalse(progressOf(Achievements.CONSISTENT_BUILDER, stats).isUnlocked)
    }
}
