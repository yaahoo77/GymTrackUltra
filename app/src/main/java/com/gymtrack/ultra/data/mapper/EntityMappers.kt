package com.gymtrack.ultra.data.mapper

import com.gymtrack.ultra.data.seed.SeedExercise
import com.gymtrack.ultra.database.entity.ExerciseEntity
import com.gymtrack.ultra.database.entity.WorkoutSessionEntity
import com.gymtrack.ultra.database.relation.PlanDayWithExercises
import com.gymtrack.ultra.domain.model.Exercise
import com.gymtrack.ultra.domain.model.PlanDay
import com.gymtrack.ultra.domain.model.PlanExercise
import com.gymtrack.ultra.domain.model.SessionSummary
import com.gymtrack.ultra.domain.model.TrackingType
import java.time.DayOfWeek
import java.time.Instant

private const val STEP_SEPARATOR = "\n"

internal fun SeedExercise.toEntity(): ExerciseEntity = ExerciseEntity(
    id = id,
    namePl = namePl,
    nameEn = nameEn,
    muscleGroup = muscleGroup,
    trackingType = if (timed) TrackingType.TIME.name else TrackingType.REPS.name,
    sideNote = sideNote,
    techniqueSteps = technique.joinToString(STEP_SEPARATOR),
    safetyTips = safety.joinToString(STEP_SEPARATOR),
    isCustom = false,
)

internal fun ExerciseEntity.toDomain(): Exercise = Exercise(
    id = id,
    namePl = namePl,
    nameEn = nameEn,
    muscleGroup = muscleGroup,
    trackingType = runCatching { TrackingType.valueOf(trackingType) }.getOrDefault(TrackingType.REPS),
    sideNote = sideNote,
    technique = techniqueSteps.split(STEP_SEPARATOR).filter { it.isNotBlank() },
    safety = safetyTips.split(STEP_SEPARATOR).filter { it.isNotBlank() },
)

internal fun PlanDayWithExercises.toDomain(): PlanDay = PlanDay(
    id = day.id,
    code = day.code,
    title = day.title,
    dayOfWeek = DayOfWeek.of(day.dayOfWeek.coerceIn(1, 7)),
    position = day.position,
    exercises = exercises
        .sortedBy { it.planExercise.position }
        .map { item ->
            PlanExercise(
                id = item.planExercise.id,
                exercise = item.exercise.toDomain(),
                position = item.planExercise.position,
                targetSets = item.planExercise.targetSets,
                targetMin = item.planExercise.targetMin,
                targetMax = item.planExercise.targetMax,
                restSeconds = item.planExercise.restSeconds,
            )
        },
)

internal fun WorkoutSessionEntity.toSummary(): SessionSummary = SessionSummary(
    id = id,
    planDayId = planDayId,
    title = title,
    startedAt = Instant.ofEpochMilli(startedAt),
    endedAt = endedAt?.let(Instant::ofEpochMilli),
    warmupDone = warmupDone,
    xpEarned = xpEarned,
)

internal fun com.gymtrack.ultra.database.relation.CompletedSetRow.toLogged(): com.gymtrack.ultra.domain.model.LoggedSet =
    com.gymtrack.ultra.domain.model.LoggedSet(
        sessionId = sessionId,
        sessionStart = Instant.ofEpochMilli(startedAt),
        planDayId = planDayId,
        exerciseId = exerciseId,
        tracking = runCatching { TrackingType.valueOf(trackingType) }.getOrDefault(TrackingType.REPS),
        setIndex = setIndex,
        targetSets = targetSets,
        targetMin = targetMin,
        targetMax = targetMax,
        weightKg = weightKg,
        reps = reps,
        durationSeconds = durationSeconds,
    )
