package com.gymtrack.ultra.domain.model

import java.time.DayOfWeek
import kotlin.math.roundToInt

data class PlanExercise(
    val id: Long,
    val exercise: Exercise,
    val position: Int,
    val targetSets: Int,
    /** Dolna granica: powtórzenia albo sekundy (dla [TrackingType.TIME]). */
    val targetMin: Int,
    val targetMax: Int,
    val restSeconds: Int,
) {
    /** "3 × 8–10", "2 × 20–30 s na stronę". */
    val targetLabel: String
        get() {
            val range = if (targetMin == targetMax) "$targetMin" else "$targetMin–$targetMax"
            val unit = if (exercise.trackingType == TrackingType.TIME) " s" else ""
            val side = exercise.sideNote?.let { " $it" }.orEmpty()
            return "$targetSets × $range$unit$side"
        }

    /** Szacowany czas pracy jednej serii w sekundach. */
    internal val workSecondsPerSet: Int
        get() = when (exercise.trackingType) {
            TrackingType.REPS -> if (exercise.sideNote != null) 60 else 40
            TrackingType.TIME -> targetMax * (if (exercise.sideNote != null) 2 else 1) + 10
        }
}

data class PlanDay(
    val id: Long,
    /** Krótki kod: "A", "B", "C". */
    val code: String,
    val title: String,
    val dayOfWeek: DayOfWeek,
    val position: Int,
    val exercises: List<PlanExercise>,
) {
    val totalSets: Int get() = exercises.sumOf { it.targetSets }

    /** Rozgrzewka + praca + przerwy, zaokrąglone do 5 minut. */
    val estimatedMinutes: Int
        get() {
            val seconds = WARMUP_SECONDS + exercises.sumOf { it.targetSets * (it.workSecondsPerSet + it.restSeconds) }
            val minutes = seconds / 60.0
            return ((minutes / 5.0).roundToInt() * 5).coerceAtLeast(5)
        }

    private companion object {
        const val WARMUP_SECONDS = 5 * 60
    }
}
