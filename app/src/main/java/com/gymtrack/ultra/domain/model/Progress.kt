package com.gymtrack.ultra.domain.model

import com.gymtrack.ultra.domain.progression.AchievementEvaluator
import com.gymtrack.ultra.domain.progression.AchievementProgress
import com.gymtrack.ultra.domain.progression.AchievementStats
import com.gymtrack.ultra.domain.progression.GamificationSnapshot
import com.gymtrack.ultra.domain.progression.LevelProgress
import com.gymtrack.ultra.domain.progression.LevelSystem

/** Jedno źródło prawdy o postępie: XP, poziom, historia, osiągnięcia. */
data class ProgressSnapshot(
    val totalXp: Int,
    val level: LevelProgress,
    /** Najnowsze pierwsze. */
    val completedSessions: List<SessionSummary>,
    val completedSetCount: Int,
    val achievements: List<AchievementProgress>,
    val gamification: GamificationSnapshot = GamificationSnapshot.Empty,
) {
    companion object {
        val Empty = ProgressSnapshot(
            totalXp = 0,
            level = LevelSystem.progressFor(0),
            completedSessions = emptyList(),
            completedSetCount = 0,
            achievements = AchievementEvaluator.evaluate(AchievementStats(), emptyMap()),
        )
    }
}
