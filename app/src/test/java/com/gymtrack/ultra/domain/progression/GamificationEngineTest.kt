package com.gymtrack.ultra.domain.progression

import com.gymtrack.ultra.domain.model.LoggedSet
import com.gymtrack.ultra.domain.model.SessionSummary
import com.gymtrack.ultra.domain.model.TrackingType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

class GamificationEngineTest {

    private val zone = ZoneOffset.UTC
    private val monday = LocalDate.of(2026, 11, 2)

    private fun at(date: LocalDate) = date.atTime(17, 0).toInstant(ZoneOffset.UTC)

    private fun session(id: Long, date: LocalDate, planDay: Long, warmup: Boolean = true) =
        SessionSummary(id, planDay, "W", at(date), at(date).plusSeconds(2_700), warmup, 0)

    private fun sets(id: Long, date: LocalDate, ex: String, count: Int, target: Int = 3, w: Double = 20.0, r: Int = 10) =
        (0 until count).map { LoggedSet(id, at(date), 1, ex, TrackingType.REPS, it, target, 8, 12, w, r, null) }

    @Test
    fun `untrained muscles have no rank`() {
        val g = GamificationEngine.build(emptyList(), emptyList(), emptyMap(), 3, monday, zone)
        assertTrue(g.muscles.none { it.isRanked })
        assertEquals(3, g.quests.size)
    }

    @Test
    fun `sets beyond plan add no muscle points and session cap applies`() {
        val history = sets(1, monday, "db_bench_press", count = 6, target = 3)
        val g = GamificationEngine.build(listOf(session(1, monday, 1)), history, mapOf(1L to 3), 3, monday, zone)
        val chest = g.muscles.first { it.muscle == MuscleGroup.CHEST }
        assertEquals(30, chest.progress.totalPoints) // 3 serie × 10, nie 6
        val triceps = g.muscles.first { it.muscle == MuscleGroup.TRICEPS }
        assertEquals(15, triceps.progress.totalPoints)
    }

    @Test
    fun `weekly quests count this week only`() {
        val lastWeek = monday.minusWeeks(1)
        val sessions = listOf(session(1, lastWeek, 1), session(2, monday, 1), session(3, monday.plusDays(2), 2))
        val history = sets(1, lastWeek, "goblet_squat", 3) + sets(2, monday, "goblet_squat", 3) +
            sets(3, monday.plusDays(2), "lat_pulldown", 3)
        val g = GamificationEngine.build(sessions, history, mapOf(1L to 3, 2L to 3, 3L to 3), 3, monday.plusDays(3), zone)
        val core = g.quests.first { it.quest.id == "WORKOUTS" }
        assertEquals(2, core.current)
        assertFalse(core.isDone)
        assertEquals(2, g.streak.current) // zeszły tydzień: 1 trening < minimum 2
    }

    @Test
    fun `quest selection is stable within a week and rotates`() {
        val a = WeeklyQuests.forWeek(monday.toEpochDay(), 3)
        val b = WeeklyQuests.forWeek(monday.toEpochDay(), 3)
        assertEquals(a, b)
        assertEquals("WORKOUTS", a.first().id)
        assertEquals(3, a.map { it.id }.distinct().size)
    }
}
