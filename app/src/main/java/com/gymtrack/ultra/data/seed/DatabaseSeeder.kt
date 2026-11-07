package com.gymtrack.ultra.data.seed

import androidx.room.withTransaction
import com.gymtrack.ultra.data.mapper.toEntity
import com.gymtrack.ultra.database.GymDatabase
import com.gymtrack.ultra.database.entity.PlanDayEntity
import com.gymtrack.ultra.database.entity.PlanExerciseEntity
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Wypełnia bazę przy pierwszym uruchomieniu.
 * Ćwiczenia: upsert przy każdym starcie (aktualizacja tekstów techniki).
 * Plan: tylko gdy jest pusty — nigdy nie nadpisujemy zmian użytkownika.
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
                    planDao.insertExercises(
                        day.items.mapIndexed { index, item ->
                            PlanExerciseEntity(
                                planDayId = dayId,
                                exerciseId = item.exerciseId,
                                position = index,
                                targetSets = item.sets,
                                targetMin = item.min,
                                targetMax = item.max,
                                restSeconds = item.restSeconds,
                            )
                        },
                    )
                }
            }
        }
    }
}
