package com.gymtrack.ultra.database.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.gymtrack.ultra.database.entity.ExerciseEntity
import com.gymtrack.ultra.database.entity.PlanDayEntity
import com.gymtrack.ultra.database.entity.PlanExerciseEntity
import com.gymtrack.ultra.database.entity.SessionExerciseEntity
import com.gymtrack.ultra.database.entity.SetLogEntity
import com.gymtrack.ultra.database.entity.WorkoutSessionEntity

data class PlanExerciseWithExercise(
    @Embedded val planExercise: PlanExerciseEntity,
    @Relation(parentColumn = "exerciseId", entityColumn = "id")
    val exercise: ExerciseEntity,
)

data class PlanDayWithExercises(
    @Embedded val day: PlanDayEntity,
    @Relation(
        entity = PlanExerciseEntity::class,
        parentColumn = "id",
        entityColumn = "planDayId",
    )
    val exercises: List<PlanExerciseWithExercise>,
)

data class SessionExerciseWithSets(
    @Embedded val sessionExercise: SessionExerciseEntity,
    @Relation(parentColumn = "exerciseId", entityColumn = "id")
    val exercise: ExerciseEntity,
    @Relation(parentColumn = "id", entityColumn = "sessionExerciseId")
    val sets: List<SetLogEntity>,
)

data class SessionWithExercises(
    @Embedded val session: WorkoutSessionEntity,
    @Relation(
        entity = SessionExerciseEntity::class,
        parentColumn = "id",
        entityColumn = "sessionId",
    )
    val exercises: List<SessionExerciseWithSets>,
)

/** Wiersz historii: zaliczona seria z ukończonego treningu (wynik zapytania z JOIN-ami). */
data class CompletedSetRow(
    val sessionId: Long,
    val startedAt: Long,
    val planDayId: Long?,
    val exerciseId: String,
    val trackingType: String,
    val setIndex: Int,
    val targetSets: Int,
    val targetMin: Int,
    val targetMax: Int,
    val weightKg: Double?,
    val reps: Int?,
    val durationSeconds: Int?,
)

data class PlannedSetsRow(
    val sessionId: Long,
    val plannedSets: Int,
)
