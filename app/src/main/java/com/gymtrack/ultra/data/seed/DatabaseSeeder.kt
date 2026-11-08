package com.gymtrack.ultra.data.seed

import androidx.room.withTransaction
import com.gymtrack.ultra.data.mapper.toEntity
import com.gymtrack.ultra.database.GymDatabase
import com.gymtrack.ultra.database.dao.PlanDao
import com.gymtrack.ultra.database.entity.PlanDayEntity
import com.gymtrack.ultra.database.entity.PlanExerciseEntity
import com.gymtrack.ultra.database.relation.PlanDayWithExercises
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Wypełnia bazę przy pierwszym uruchomieniu.
 * Ćwiczenia: upsert przy każdym starcie (aktualizacja tekstów techniki, nowe ćwiczenia).
 * Plan: tworzony, gdy jest pusty. Jeśli w bazie leży nietknięty plan z pierwszej wersji,
 * jego ćwiczenia są podmieniane na nowy zestaw (dni tygodnia zostają). Historia treningów
 * się nie zmienia — sesje mają własną kopię ćwiczeń, a stare ćwiczenia zostają w bibliotece.
 */
@Singleton
class DatabaseSeeder @Inject constructor(
    private val db: GymDatabase,
) {
    suspend fun seedIfNeeded() {
        db.withTransaction {
            db.exerciseDao().upsertAll(DefaultContent.exercises.map { it.toEntity() })

            val planDao = db.planDao()
            if (planDao.countDays() == 0) {
                DefaultContent.plan.forEachIndexed { dayIndex, day ->
                    val dayId = planDao.insertDay(
                        PlanDayEntity(
                            code = day.code,
                            title = day.title,
                            dayOfWeek = day.dayOfWeek.value,
                            position = dayIndex,
                        ),
                    )
                    planDao.insertExercises(day.items.toEntities(dayId))
                }
            } else {
                replaceLegacyPlan(planDao)
            }
        }
    }

    private suspend fun replaceLegacyPlan(planDao: PlanDao) {
        val days = planDao.getPlanDays()
        if (!isLegacyPlan(days)) return
        days.zip(DefaultContent.plan).forEach { (current, seed) ->
            planDao.deleteExercisesForDay(current.day.id)
            planDao.insertExercises(seed.items.toEntities(current.day.id))
        }
    }

    private fun isLegacyPlan(days: List<PlanDayWithExercises>): Boolean {
        if (days.size != DefaultContent.legacyPlanV1.size) return false
        val ids = days.map { day -> day.exercises.sortedBy { it.planExercise.position }.map { it.planExercise.exerciseId } }
        return ids == DefaultContent.legacyPlanV1
    }

    private fun List<SeedPlanItem>.toEntities(dayId: Long) = mapIndexed { index, item ->
        PlanExerciseEntity(
            planDayId = dayId,
            exerciseId = item.exerciseId,
            position = index,
            targetSets = item.sets,
            targetMin = item.min,
            targetMax = item.max,
            restSeconds = item.restSeconds,
        )
    }
}
