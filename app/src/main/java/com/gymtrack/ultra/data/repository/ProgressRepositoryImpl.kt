package com.gymtrack.ultra.data.repository

import com.gymtrack.ultra.core.time.TimeProvider
import com.gymtrack.ultra.data.mapper.toSummary
import com.gymtrack.ultra.database.dao.PlanDao
import com.gymtrack.ultra.database.dao.ProgressDao
import com.gymtrack.ultra.database.dao.SessionDao
import com.gymtrack.ultra.domain.model.ProgressSnapshot
import com.gymtrack.ultra.domain.progression.AchievementEvaluator
import com.gymtrack.ultra.domain.progression.AchievementStatsBuilder
import com.gymtrack.ultra.domain.progression.LevelSystem
import com.gymtrack.ultra.domain.repository.ProgressRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProgressRepositoryImpl @Inject constructor(
    private val sessionDao: SessionDao,
    private val progressDao: ProgressDao,
    private val planDao: PlanDao,
    private val time: TimeProvider,
) : ProgressRepository {

    override fun observeProgress(): Flow<ProgressSnapshot> {
        val history = combine(
            progressDao.observeTotalXp(),
            sessionDao.observeCompleted(),
            sessionDao.observeCompletedSetCount(),
        ) { xp, sessions, sets -> Triple(xp, sessions, sets) }

        val achievementInputs = combine(
            progressDao.observeUnlocks(),
            progressDao.observeTechniqueViewedInPlan(),
            planDao.observeDistinctExerciseCount(),
        ) { unlocks, viewed, total -> Triple(unlocks, viewed, total) }

        return combine(history, achievementInputs) { (xp, sessions, sets), (unlocks, viewed, total) ->
            val summaries = sessions.map { it.toSummary() }
            val stats = AchievementStatsBuilder.build(
                completedSessions = summaries,
                completedSets = sets,
                techniqueViewed = viewed,
                techniqueTotal = total,
                zone = time.zone(),
            )
            ProgressSnapshot(
                totalXp = xp,
                level = LevelSystem.progressFor(xp),
                completedSessions = summaries,
                completedSetCount = sets,
                achievements = AchievementEvaluator.evaluate(stats, unlocks.associate { it.code to it.unlockedAt }),
            )
        }.distinctUntilChanged()
    }

    override fun observeCompletedSetsSince(from: Instant?): Flow<Int> =
        sessionDao.observeCompletedSetsInFinishedSessionsSince(from?.toEpochMilli() ?: 0L)
}
