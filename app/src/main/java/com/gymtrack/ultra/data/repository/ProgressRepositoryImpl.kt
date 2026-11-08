package com.gymtrack.ultra.data.repository

import kotlinx.coroutines.flow.map
import com.gymtrack.ultra.domain.model.LoggedSet
import com.gymtrack.ultra.core.time.TimeProvider
import com.gymtrack.ultra.data.mapper.toLogged
import com.gymtrack.ultra.data.mapper.toSummary
import com.gymtrack.ultra.database.dao.PlanDao
import com.gymtrack.ultra.database.dao.ProgressDao
import com.gymtrack.ultra.database.dao.SessionDao
import com.gymtrack.ultra.domain.model.ProgressSnapshot
import com.gymtrack.ultra.domain.progression.AchievementEvaluator
import com.gymtrack.ultra.domain.progression.AchievementStatsBuilder
import com.gymtrack.ultra.domain.progression.GamificationEngine
import com.gymtrack.ultra.domain.progression.LevelSystem
import com.gymtrack.ultra.domain.repository.ProgressRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.Dispatchers
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

        val gamificationInputs = combine(
            sessionDao.observeCompletedSetRows(),
            sessionDao.observePlannedSetsPerSession(),
            planDao.observeDayCount(),
        ) { rows, planned, days -> Triple(rows, planned, days) }

        return combine(history, achievementInputs, gamificationInputs) { h, a, g ->
            val (xp, sessions, sets) = h
            val (unlocks, viewed, total) = a
            val (rows, planned, days) = g
            val zone = time.zone()
            val summaries = sessions.map { it.toSummary() }
            val gamification = GamificationEngine.build(
                sessions = summaries,
                history = rows.map { it.toLogged() },
                plannedSetsBySession = planned.associate { it.sessionId to it.plannedSets },
                planDays = days,
                today = time.today(),
                zone = zone,
            )
            val stats = AchievementStatsBuilder.build(summaries, sets, viewed, total, zone, gamification)
            ProgressSnapshot(
                totalXp = xp,
                level = LevelSystem.progressFor(xp),
                completedSessions = summaries,
                completedSetCount = sets,
                achievements = AchievementEvaluator.evaluate(stats, unlocks.associate { it.code to it.unlockedAt }),
                gamification = gamification,
            )
        }
            .flowOn(Dispatchers.Default)
            .distinctUntilChanged()
    }

    override fun observeLoggedSets(): Flow<List<LoggedSet>> =
        sessionDao.observeCompletedSetRows().map { rows -> rows.map { it.toLogged() } }

    override fun observeCompletedSetsSince(from: Instant?): Flow<Int> =
        sessionDao.observeCompletedSetsInFinishedSessionsSince(from?.toEpochMilli() ?: 0L)
}
