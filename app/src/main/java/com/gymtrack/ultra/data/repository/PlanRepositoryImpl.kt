package com.gymtrack.ultra.data.repository

import androidx.room.withTransaction
import com.gymtrack.ultra.widget.WidgetUpdater
import com.gymtrack.ultra.core.time.TimeProvider
import com.gymtrack.ultra.data.mapper.toDomain
import com.gymtrack.ultra.database.GymDatabase
import com.gymtrack.ultra.database.dao.ExerciseDao
import com.gymtrack.ultra.database.dao.PlanDao
import com.gymtrack.ultra.database.dao.ProgressDao
import com.gymtrack.ultra.database.entity.AchievementUnlockEntity
import com.gymtrack.ultra.database.entity.ExerciseEntity
import com.gymtrack.ultra.database.entity.PlanExerciseEntity
import com.gymtrack.ultra.database.entity.TechniqueViewEntity
import com.gymtrack.ultra.domain.model.Exercise
import com.gymtrack.ultra.domain.model.PlanDay
import com.gymtrack.ultra.domain.model.TrackingType
import com.gymtrack.ultra.domain.plan.CustomExercises
import com.gymtrack.ultra.domain.progression.Achievements
import com.gymtrack.ultra.domain.progression.MuscleGroup
import com.gymtrack.ultra.domain.repository.PlanRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.time.DayOfWeek
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlanRepositoryImpl @Inject constructor(
    private val db: GymDatabase,
    private val planDao: PlanDao,
    private val exerciseDao: ExerciseDao,
    private val progressDao: ProgressDao,
    private val time: TimeProvider,
    private val widgets: WidgetUpdater,
) : PlanRepository {

    override fun observePlan(): Flow<List<PlanDay>> =
        planDao.observePlanDays()
            .map { days -> days.map { it.toDomain() } }
            .distinctUntilChanged()

    override fun observePlanDay(id: Long): Flow<PlanDay?> =
        planDao.observePlanDay(id)
            .map { it?.toDomain() }
            .distinctUntilChanged()

    override suspend fun markTechniqueViewed(exerciseId: String) {
        progressDao.insertTechniqueView(
            TechniqueViewEntity(exerciseId = exerciseId, firstViewedAt = time.now().toEpochMilli()),
        )
    }

    override fun observeLibrary(): Flow<List<Exercise>> =
        exerciseDao.observeAll().map { list -> list.map { it.toDomain() } }.distinctUntilChanged()

    override suspend fun updateTargets(planExerciseId: Long, sets: Int, min: Int, max: Int, restSeconds: Int) {
        val item = planDao.getExercise(planExerciseId) ?: return
        val low = min.coerceIn(1, 300)
        planDao.updateExercise(
            item.copy(
                targetSets = sets.coerceIn(1, 6),
                targetMin = low,
                targetMax = max.coerceIn(low, 300),
                restSeconds = restSeconds.coerceIn(15, 300),
            ),
        )
        markPlanEdited()
    }

    override suspend fun replaceExercise(planExerciseId: Long, exerciseId: String) {
        val item = planDao.getExercise(planExerciseId) ?: return
        val exercise = exerciseDao.get(exerciseId) ?: return
        val timedNow = exercise.trackingType == TrackingType.TIME.name
        val timedBefore = exerciseDao.get(item.exerciseId)?.trackingType == TrackingType.TIME.name
        // Zmiana powtórzenia ↔ czas: zakres z innej "jednostki" nie ma sensu, wracamy do domyślnego.
        val updated = if (timedNow == timedBefore) {
            item.copy(exerciseId = exerciseId)
        } else {
            val (min, max) = defaultRange(timedNow)
            item.copy(exerciseId = exerciseId, targetMin = min, targetMax = max)
        }
        planDao.updateExercise(updated)
        markPlanEdited()
    }

    override suspend fun removeExercise(planExerciseId: Long) {
        db.withTransaction {
            val item = planDao.getExercise(planExerciseId) ?: return@withTransaction
            val siblings = planDao.getExercisesForDay(item.planDayId)
            if (siblings.size <= 1) return@withTransaction // dzień nie może zostać pusty
            planDao.deleteExercise(planExerciseId)
            reindex(siblings.filter { it.id != planExerciseId })
        }
        markPlanEdited()
    }

    override suspend fun addExercise(dayId: Long, exerciseId: String) {
        db.withTransaction {
            val exercise = exerciseDao.get(exerciseId) ?: return@withTransaction
            val siblings = planDao.getExercisesForDay(dayId)
            val (min, max) = defaultRange(exercise.trackingType == TrackingType.TIME.name)
            planDao.insertExercises(
                listOf(
                    PlanExerciseEntity(
                        planDayId = dayId,
                        exerciseId = exerciseId,
                        position = siblings.size,
                        targetSets = 3,
                        targetMin = min,
                        targetMax = max,
                        restSeconds = if (exercise.trackingType == TrackingType.TIME.name) 60 else 90,
                    ),
                ),
            )
        }
        markPlanEdited()
    }

    override suspend fun moveExercise(planExerciseId: Long, delta: Int) {
        db.withTransaction {
            val item = planDao.getExercise(planExerciseId) ?: return@withTransaction
            val list = planDao.getExercisesForDay(item.planDayId).toMutableList()
            val from = list.indexOfFirst { it.id == planExerciseId }
            val to = (from + delta).coerceIn(0, list.lastIndex)
            if (from < 0 || from == to) return@withTransaction
            list.add(to, list.removeAt(from))
            reindex(list)
        }
        markPlanEdited()
    }

    override suspend fun createCustomExercise(name: String, muscle: MuscleGroup, tracking: TrackingType): String {
        val id = CustomExercises.newId(muscle, time.now().toEpochMilli())
        val clean = name.trim().take(40).ifBlank { "Moje ćwiczenie" }
        exerciseDao.upsertAll(
            listOf(
                ExerciseEntity(
                    id = id,
                    namePl = clean,
                    nameEn = clean,
                    muscleGroup = muscle.label,
                    trackingType = tracking.name,
                    sideNote = null,
                    techniqueSteps = "",
                    safetyTips = "",
                    isCustom = true,
                ),
            ),
        )
        return id
    }

    override suspend fun assignWeekdays(days: List<DayOfWeek>) {
        if (days.isEmpty()) return
        db.withTransaction {
            planDao.getDays().forEachIndexed { index, day ->
                val weekday = days[index % days.size]
                planDao.updateDayOfWeek(day.id, weekday.value)
            }
        }
        widgets.refresh()
    }

    private suspend fun reindex(items: List<PlanExerciseEntity>) {
        planDao.updateExercises(items.mapIndexed { index, item -> item.copy(position = index) })
    }

    private suspend fun markPlanEdited() {
        progressDao.insertUnlocks(listOf(AchievementUnlockEntity(Achievements.PLAN_ARCHITECT, time.now().toEpochMilli())))
    }

    private fun defaultRange(timed: Boolean): Pair<Int, Int> = if (timed) 20 to 40 else 8 to 12
}
