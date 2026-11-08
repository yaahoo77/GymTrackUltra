package com.gymtrack.ultra.data.repository

import androidx.room.withTransaction
import com.gymtrack.ultra.widget.WidgetUpdater
import com.gymtrack.ultra.domain.progression.LoadProgression
import com.gymtrack.ultra.domain.progression.LoadAdvice
import com.gymtrack.ultra.core.time.TimeProvider
import com.gymtrack.ultra.core.time.WeekMath
import com.gymtrack.ultra.data.mapper.toDomain
import com.gymtrack.ultra.data.mapper.toLogged
import com.gymtrack.ultra.data.mapper.toSummary
import com.gymtrack.ultra.database.GymDatabase
import com.gymtrack.ultra.database.entity.AchievementUnlockEntity
import com.gymtrack.ultra.database.entity.SessionExerciseEntity
import com.gymtrack.ultra.database.entity.SetLogEntity
import com.gymtrack.ultra.database.entity.WorkoutSessionEntity
import com.gymtrack.ultra.database.entity.XpEventEntity
import com.gymtrack.ultra.database.relation.SessionWithExercises
import com.gymtrack.ultra.domain.model.LiveExercise
import com.gymtrack.ultra.domain.model.LiveSession
import com.gymtrack.ultra.domain.model.LiveSet
import com.gymtrack.ultra.domain.model.LoggedSet
import com.gymtrack.ultra.domain.model.PreviousSet
import com.gymtrack.ultra.domain.model.RankUp
import com.gymtrack.ultra.domain.model.RecordWithName
import com.gymtrack.ultra.domain.model.SessionStatus
import com.gymtrack.ultra.domain.model.WorkoutResult
import com.gymtrack.ultra.domain.progression.AchievementEvaluator
import com.gymtrack.ultra.domain.progression.AchievementStatsBuilder
import com.gymtrack.ultra.domain.progression.Achievements
import com.gymtrack.ultra.domain.progression.GamificationEngine
import com.gymtrack.ultra.domain.progression.GamificationSnapshot
import com.gymtrack.ultra.domain.progression.LevelSystem
import com.gymtrack.ultra.domain.progression.SessionXpInput
import com.gymtrack.ultra.domain.progression.WeeklyQuests
import com.gymtrack.ultra.domain.progression.XpCalculator
import com.gymtrack.ultra.domain.progression.XpLine
import com.gymtrack.ultra.domain.progression.XpReason
import com.gymtrack.ultra.domain.repository.WorkoutRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

@Singleton
class WorkoutRepositoryImpl @Inject constructor(
    private val db: GymDatabase,
    private val time: TimeProvider,
    private val widgets: WidgetUpdater,
) : WorkoutRepository {

    private val sessionDao = db.sessionDao()
    private val planDao = db.planDao()
    private val progressDao = db.progressDao()

    /** "Ostatnio: …" liczymy raz na ćwiczenie w danej sesji, nie przy każdej zmianie. */
    private val previousCache = ConcurrentHashMap<String, List<PreviousSet>>()
    private var cachedForSession: Long = -1

    // ── Odczyt ──────────────────────────────────────────────────────────

    override fun observeActiveSession(): Flow<LiveSession?> =
        sessionDao.observeActiveSessionDetail()
            .map { detail -> detail?.let { toLive(it) } }
            .distinctUntilChanged()

    private suspend fun toLive(detail: SessionWithExercises): LiveSession {
        if (cachedForSession != detail.session.id) {
            previousCache.clear()
            cachedForSession = detail.session.id
        }
        val exercises = detail.exercises
            .sortedBy { it.sessionExercise.position }
            .map { item ->
                val se = item.sessionExercise
                val previous = previousCache.getOrPut(se.exerciseId) {
                    sessionDao.lastCompletedSetsFor(se.exerciseId)
                        .map { PreviousSet(it.weightKg, it.reps, it.durationSeconds) }
                }
                val exercise = item.exercise.toDomain()
                LiveExercise(
                    sessionExerciseId = se.id,
                    exercise = exercise,
                    position = se.position,
                    targetSets = se.targetSets,
                    targetMin = se.targetMin,
                    targetMax = se.targetMax,
                    restSeconds = se.restSeconds,
                    note = se.note,
                    sets = item.sets.sortedBy { it.setIndex }.map {
                        LiveSet(it.id, it.setIndex, it.weightKg, it.reps, it.durationSeconds, it.completed)
                    },
                    previous = previous,
                    advice = LoadProgression.advise(se.exerciseId, exercise.trackingType, previous, se.targetSets, se.targetMax),
                )
            }
        return LiveSession(
            id = detail.session.id,
            planDayId = detail.session.planDayId,
            title = detail.session.title,
            startedAt = Instant.ofEpochMilli(detail.session.startedAt),
            warmupDone = detail.session.warmupDone,
            exercises = exercises,
        )
    }

    // ── Zapis serii ─────────────────────────────────────────────────────

    override suspend fun startOrResume(planDayId: Long): Long = startOrResumeInternal(planDayId).also { widgets.refresh() }

    private suspend fun startOrResumeInternal(planDayId: Long): Long = db.withTransaction {
        sessionDao.getActiveSession()?.let { return@withTransaction it.id }

        val day = planDao.getPlanDay(planDayId) ?: error("Plan day $planDayId not found")
        val now = time.now().toEpochMilli()
        val sessionId = sessionDao.insertSession(
            WorkoutSessionEntity(
                planDayId = day.day.id,
                title = day.day.title,
                startedAt = now,
                endedAt = null,
                status = SessionStatus.ACTIVE.name,
                warmupDone = false,
                xpEarned = 0,
                updatedAt = now,
            ),
        )
        val ordered = day.exercises.sortedBy { it.planExercise.position }
        val ids = sessionDao.insertSessionExercises(
            ordered.map { item ->
                val pe = item.planExercise
                SessionExerciseEntity(
                    sessionId = sessionId,
                    exerciseId = pe.exerciseId,
                    position = pe.position,
                    targetSets = pe.targetSets,
                    targetMin = pe.targetMin,
                    targetMax = pe.targetMax,
                    restSeconds = pe.restSeconds,
                )
            },
        )
        ids.forEachIndexed { i, sessionExerciseId ->
            val item = ordered[i]
            val pe = item.planExercise
            val previous = sessionDao.lastCompletedSetsFor(pe.exerciseId)
            val advice = LoadProgression.advise(
                pe.exerciseId,
                item.exercise.toDomain().trackingType,
                previous.map { PreviousSet(it.weightKg, it.reps, it.durationSeconds) },
                pe.targetSets,
                pe.targetMax,
            )
            sessionDao.insertSets(
                (0 until pe.targetSets).map { index ->
                    // Ciężar podpowiadamy z ostatniego treningu — a gdy ostatnio wszystkie serie były na górze
                    // zakresu, od razu o jeden mały krok wyżej (podwójna progresja).
                    val prev = previous.getOrNull(index) ?: previous.lastOrNull()
                    val weight = if (advice is LoadAdvice.Increase) advice.toKg else prev?.weightKg
                    SetLogEntity(
                        sessionExerciseId = sessionExerciseId,
                        setIndex = index,
                        weightKg = weight,
                        reps = null,
                        durationSeconds = null,
                        completed = false,
                        completedAt = null,
                    )
                },
            )
        }
        sessionId
    }

    override suspend fun updateSetValues(setId: Long, weightKg: Double?, reps: Int?, durationSeconds: Int?) {
        val set = sessionDao.getSet(setId) ?: return
        sessionDao.updateSet(set.copy(weightKg = weightKg, reps = reps, durationSeconds = durationSeconds))
    }

    override suspend fun setCompleted(setId: Long, completed: Boolean) {
        val set = sessionDao.getSet(setId) ?: return
        sessionDao.updateSet(
            set.copy(completed = completed, completedAt = if (completed) time.now().toEpochMilli() else null),
        )
    }

    override suspend fun addSet(sessionExerciseId: Long) {
        val sets = sessionDao.getSets(sessionExerciseId)
        val last = sets.lastOrNull()
        sessionDao.insertSets(
            listOf(
                SetLogEntity(
                    sessionExerciseId = sessionExerciseId,
                    setIndex = (last?.setIndex ?: -1) + 1,
                    weightKg = last?.weightKg,
                    reps = null,
                    durationSeconds = null,
                    completed = false,
                    completedAt = null,
                ),
            ),
        )
    }

    override suspend fun swapExercise(sessionExerciseId: Long, exerciseId: String, alsoInPlan: Boolean) {
        db.withTransaction {
            val item = sessionDao.getSessionExercise(sessionExerciseId) ?: return@withTransaction
            if (item.exerciseId == exerciseId) return@withTransaction
            val sets = sessionDao.getSets(sessionExerciseId)
            if (sets.any { it.completed }) return@withTransaction // zaliczone serie zostają przy starym ćwiczeniu
            val oldExerciseId = item.exerciseId
            sessionDao.updateSessionExerciseExercise(sessionExerciseId, exerciseId)
            // Ciężary z ostatniego razu z NOWYM ćwiczeniem; powtórzenia/czas czyścimy.
            val previous = sessionDao.lastCompletedSetsFor(exerciseId)
            sessionDao.updateSets(
                sets.map { set ->
                    val prev = previous.getOrNull(set.setIndex) ?: previous.lastOrNull()
                    set.copy(weightKg = prev?.weightKg, reps = null, durationSeconds = null)
                },
            )
            previousCache.remove(exerciseId)
            if (alsoInPlan) {
                val planDayId = sessionDao.getSession(item.sessionId)?.planDayId
                if (planDayId != null) {
                    planDao.replaceInDay(planDayId, oldExerciseId, exerciseId)
                    progressDao.insertUnlocks(
                        listOf(AchievementUnlockEntity(Achievements.PLAN_ARCHITECT, time.now().toEpochMilli())),
                    )
                }
            }
        }
    }

    override suspend fun setWarmupDone(sessionId: Long, done: Boolean) {
        val session = sessionDao.getSession(sessionId) ?: return
        sessionDao.updateSession(session.copy(warmupDone = done, updatedAt = time.now().toEpochMilli()))
    }

    override suspend fun setNote(sessionExerciseId: Long, note: String?) {
        val item = sessionDao.getSessionExercise(sessionExerciseId) ?: return
        sessionDao.updateSessionExercise(item.copy(note = note?.trim()?.takeIf { it.isNotEmpty() }?.take(280)))
    }

    override suspend fun applyLighterLoad(sessionExerciseId: Long) {
        db.withTransaction {
            val sets = sessionDao.getSets(sessionExerciseId)
            val lighter = sets.filter { !it.completed && (it.weightKg ?: 0.0) > 0.0 }.map { set ->
                val reduced = ((set.weightKg ?: 0.0) * 0.9 * 2).roundToInt() / 2.0
                set.copy(weightKg = reduced.coerceAtLeast(0.5))
            }
            if (lighter.isNotEmpty()) sessionDao.updateSets(lighter)
            progressDao.insertUnlocks(listOf(AchievementUnlockEntity(Achievements.SMART_LOAD, time.now().toEpochMilli())))
        }
    }

    override suspend fun discard(sessionId: Long) {
        db.withTransaction {
            sessionDao.deleteSetsOfSession(sessionId)
            sessionDao.deleteExercisesOfSession(sessionId)
            sessionDao.deleteSession(sessionId)
        }
        widgets.refresh()
    }

    // ── Zakończenie: XP, wyzwania, osiągnięcia ──────────────────────────

    override suspend fun finish(sessionId: Long): Long = finishInternal(sessionId).also { widgets.refresh() }

    private suspend fun finishInternal(sessionId: Long): Long = db.withTransaction {
        val detail = sessionDao.getSessionDetail(sessionId) ?: error("Session $sessionId not found")
        check(detail.exercises.any { e -> e.sets.any { it.completed } }) { "No completed sets" }

        val nowInstant = time.now()
        val now = nowInstant.toEpochMilli()
        val zone = time.zone()
        val today = nowInstant.atZone(zone).toLocalDate()
        val weekStart = WeekMath.weekStart(today)
        val weekStartDay = weekStart.toEpochDay()

        val planned = detail.exercises.sumOf { it.sessionExercise.targetSets }
        val capped = XpCalculator.cappedCompletedSets(
            detail.exercises.map { it.sessionExercise.targetSets to it.sets.count { s -> s.completed } },
        )
        val alreadyToday = progressDao.countXpOnDay(today.toEpochDay(), XpReason.PLAN_PROGRESS.name) > 0
        val award = XpCalculator.forSession(SessionXpInput(planned, capped, detail.session.warmupDone, alreadyToday))

        sessionDao.updateSession(
            detail.session.copy(status = SessionStatus.COMPLETED.name, endedAt = now, updatedAt = now),
        )

        val events = award.lines.map { line -> line to line.reason.name }.toMutableList()

        val planDays = planDao.countDays()
        val weekStartMillis = weekStart.atStartOfDay(zone).toInstant().toEpochMilli()
        val doneThisWeek = sessionDao.countCompletedSince(weekStartMillis)
        val weeklyAwarded = progressDao.countXpInWeek(weekStartDay, XpReason.WEEKLY_GOAL.name) > 0
        XpCalculator.weeklyGoalBonus(doneThisWeek, planDays, weeklyAwarded)?.let { events += it to it.reason.name }

        val gamification = loadGamification(today, zone, planDays)
        for (quest in gamification.quests.filter { it.isDone }) {
            val reason = WeeklyQuests.xpReason(quest.quest)
            if (progressDao.countXpInWeek(weekStartDay, reason) == 0) {
                events += XpLine(XpReason.QUEST, quest.quest.xp, quest.quest.title) to reason
            }
        }

        progressDao.insertXpEvents(
            events.map { (line, reason) ->
                XpEventEntity(
                    sessionId = sessionId,
                    amount = line.amount,
                    reason = reason,
                    epochDay = today.toEpochDay(),
                    weekStartEpochDay = weekStartDay,
                    createdAt = now,
                )
            },
        )
        sessionDao.updateSession(
            detail.session.copy(
                status = SessionStatus.COMPLETED.name,
                endedAt = now,
                updatedAt = now,
                xpEarned = events.sumOf { it.first.amount },
            ),
        )

        val sessions = sessionDao.getCompleted().map { it.toSummary() }
        val stats = AchievementStatsBuilder.build(
            completedSessions = sessions,
            completedSets = sessionDao.getCompletedSetCount(),
            techniqueViewed = progressDao.getTechniqueViewedInPlan(),
            techniqueTotal = planDao.getDistinctExerciseCount(),
            zone = zone,
            gamification = gamification,
        )
        val unlocked = progressDao.getUnlocks().associate { it.code to it.unlockedAt }
        val reached = AchievementEvaluator.newlyReached(stats, unlocked)
        progressDao.insertUnlocks(reached.map { AchievementUnlockEntity(it.code, now) })

        sessionId
    }

    private suspend fun loadGamification(today: LocalDate, zone: ZoneId, planDays: Int): GamificationSnapshot =
        GamificationEngine.build(
            sessions = sessionDao.getCompleted().map { it.toSummary() },
            history = sessionDao.getCompletedSetRows().map { it.toLogged() },
            plannedSetsBySession = sessionDao.getPlannedSetsPerSession().associate { it.sessionId to it.plannedSets },
            planDays = planDays,
            today = today,
            zone = zone,
        )

    // ── Podsumowanie (odtwarzalne z bazy — przetrwa obrót i śmierć procesu) ──

    override suspend fun loadResult(sessionId: Long): WorkoutResult? {
        val detail = sessionDao.getSessionDetail(sessionId) ?: return null
        val session = detail.session
        val endedAt = session.endedAt ?: return null
        val zone = time.zone()
        val endDate = Instant.ofEpochMilli(endedAt).atZone(zone).toLocalDate()
        val planDays = planDao.countDays()

        val xpEvents = progressDao.getXpForSession(sessionId)
        val lines = xpEvents.map { event -> toXpLine(event.reason, event.amount, planDays) }
        val xpBefore = progressDao.sumXpBefore(endedAt)
        val levelBefore = LevelSystem.progressFor(xpBefore)
        val levelAfter = LevelSystem.progressFor(xpBefore + lines.sumOf { it.amount })

        val allRows = sessionDao.getCompletedSetRows().map { it.toLogged() }
        val allSessions = sessionDao.getCompleted().map { it.toSummary() }
        val planned = sessionDao.getPlannedSetsPerSession().associate { it.sessionId to it.plannedSets }
        val start = Instant.ofEpochMilli(session.startedAt)
        val isBefore = { id: Long, at: Instant -> id != sessionId && !at.isAfter(start) }
        val rowsBefore = allRows.filter { isBefore(it.sessionId, it.sessionStart) }
        val sessionsBefore = allSessions.filter { isBefore(it.id, it.startedAt) }
        val rowsThis = allRows.filter { it.sessionId == sessionId }
        val sessionsThis = allSessions.filter { it.id == sessionId }

        val before = GamificationEngine.build(sessionsBefore, rowsBefore, planned, planDays, endDate, zone)
        val after = GamificationEngine.build(sessionsBefore + sessionsThis, rowsBefore + rowsThis, planned, planDays, endDate, zone)

        val rankUps = buildList {
            if (levelAfter.rank > levelBefore.rank) add(RankUp(null, levelBefore.rank, levelAfter.rank))
            after.muscles.forEach { a ->
                val b = before.muscles.first { it.muscle == a.muscle }
                if (a.isRanked && (!b.isRanked || a.progress.rank > b.progress.rank)) {
                    add(RankUp(a.muscle, if (b.isRanked) b.progress.rank else null, a.progress.rank))
                }
            }
        }
        val gains = after.muscles.associate { a ->
            a.muscle to (a.progress.totalPoints - before.muscles.first { it.muscle == a.muscle }.progress.totalPoints)
        }.filterValues { it > 0 }

        val names = detail.exercises.associate { it.exercise.id to it.exercise.namePl }
        val records = after.records.filter { it.sessionId == sessionId }
            .map { RecordWithName(it, names[it.exerciseId] ?: it.exerciseId) }

        val newAchievements = progressDao.getUnlocksAt(endedAt).mapNotNull { Achievements.byCode(it.code) }
        val quests = xpEvents.filter { it.reason.startsWith(WeeklyQuests.XP_REASON_PREFIX) }.mapNotNull { e ->
            val id = e.reason.removePrefix(WeeklyQuests.XP_REASON_PREFIX)
            WeeklyQuests.all(planDays).firstOrNull { it.id == id }
        }

        val volume = rowsThis.sumOf { volumeOf(it) }
        val previousSameDay = sessionsBefore
            .filter { it.planDayId != null && it.planDayId == session.planDayId }
            .maxByOrNull { it.startedAt }
        val previousVolume = previousSameDay?.let { prev ->
            allRows.filter { it.sessionId == prev.id }.sumOf { volumeOf(it) }
        }

        return WorkoutResult(
            sessionId = sessionId,
            title = session.title,
            durationMillis = (endedAt - session.startedAt).coerceAtLeast(0),
            completedExercises = detail.exercises.count { e -> e.sets.count { it.completed } >= e.sessionExercise.targetSets },
            totalExercises = detail.exercises.size,
            completedSets = detail.exercises.sumOf { e -> e.sets.count { it.completed } },
            plannedSets = detail.exercises.sumOf { it.sessionExercise.targetSets },
            xpLines = lines,
            xpNote = if (lines.isEmpty()) XpCalculator.DAILY_LIMIT_NOTE else null,
            levelBefore = levelBefore,
            levelAfter = levelAfter,
            rankUps = rankUps,
            muscleGains = gains,
            records = records,
            newAchievements = newAchievements,
            completedQuests = quests,
            streakBefore = before.streak.current,
            streakAfter = after.streak,
            trainedThisWeek = (sessionsBefore + sessionsThis)
                .map { WeekMath.localDate(it.startedAt, zone) }
                .filter { WeekMath.weekStart(it) == WeekMath.weekStart(endDate) }
                .map { it.dayOfWeek }
                .toSet(),
            volumeKg = volume,
            previousVolumeKg = previousVolume,
        )
    }

    private fun volumeOf(set: LoggedSet): Double = (set.weightKg ?: 0.0) * (set.reps ?: 0)

    private fun toXpLine(reason: String, amount: Int, planDays: Int): XpLine {
        if (reason.startsWith(WeeklyQuests.XP_REASON_PREFIX)) {
            val id = reason.removePrefix(WeeklyQuests.XP_REASON_PREFIX)
            val title = WeeklyQuests.all(planDays).firstOrNull { it.id == id }?.title
            return XpLine(XpReason.QUEST, amount, title)
        }
        val known = XpReason.entries.firstOrNull { it.name == reason } ?: XpReason.WORKOUT_COMPLETED
        return XpLine(known, amount)
    }
}
