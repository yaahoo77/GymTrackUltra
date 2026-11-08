package com.gymtrack.ultra.domain.progression

import com.gymtrack.ultra.domain.model.PreviousSet
import com.gymtrack.ultra.domain.model.TrackingType
import kotlin.math.floor

/** Podpowiedź ciężaru na dziś na podstawie ostatniego treningu z tym ćwiczeniem. */
sealed interface LoadAdvice {
    /** Ostatnio wszystkie serie na górnej granicy zakresu — czas na mały krok w górę. */
    data class Increase(val fromKg: Double, val toKg: Double, val sets: Int, val reps: Int) : LoadAdvice {
        val stepKg: Double get() = toKg - fromKg
    }

    /** Zostań przy tym ciężarze i dokładaj powtórzenia. */
    data class Keep(val kg: Double, val missingSets: Int) : LoadAdvice
}

/**
 * Podwójna progresja: najpierw powtórzenia do górnej granicy zakresu w każdej serii, dopiero potem ciężar —
 * i to najmniejszym sensownym krokiem dla danego sprzętu. Nigdy nie podpowiada skoków "na rekord".
 */
object LoadProgression {

    private val steps: Map<String, Double> = mapOf(
        "leg_press" to 10.0,
        "lat_pulldown" to 5.0,
        "seated_cable_row" to 5.0,
        "chest_press_machine" to 5.0,
        "leg_extension" to 5.0,
        "leg_curl" to 5.0,
        "tricep_pushdown" to 2.5,
        "face_pull" to 2.5,
        "lateral_raise" to 1.0,
        "db_bicep_curl" to 1.0,
        "hammer_curl" to 1.0,
        "overhead_tricep_extension" to 1.0,
        "goblet_squat" to 2.0,
        "db_bench_press" to 2.0,
        "incline_db_press" to 2.0,
        "seated_db_shoulder_press" to 2.0,
        "db_romanian_deadlift" to 2.0,
        "one_arm_db_row" to 2.0,
        "chest_supported_row" to 2.0,
        "lunges" to 2.0,
        "hip_thrust" to 5.0,
    )

    fun stepFor(exerciseId: String): Double = steps[exerciseId] ?: 2.5

    fun advise(
        exerciseId: String,
        trackingType: TrackingType,
        previous: List<PreviousSet>,
        plannedSets: Int,
        targetMax: Int,
    ): LoadAdvice? {
        if (trackingType != TrackingType.REPS || previous.isEmpty()) return null
        val weights = previous.mapNotNull { it.weightKg }.filter { it > 0.0 }
        if (weights.size != previous.size) return null // ćwiczenie bez ciężaru (np. unoszenie kolan)
        val working = weights.min()
        val cleanSets = previous.count { (it.reps ?: 0) >= targetMax && (it.weightKg ?: 0.0) >= working }
        val needed = plannedSets.coerceAtLeast(1)
        return if (cleanSets >= needed) {
            val step = stepFor(exerciseId)
            val next = roundToStep(working + step, step)
            LoadAdvice.Increase(fromKg = working, toKg = next, sets = needed, reps = targetMax)
        } else {
            LoadAdvice.Keep(kg = working, missingSets = needed - cleanSets)
        }
    }

    private fun roundToStep(value: Double, step: Double): Double = floor(value / step + 1e-6) * step
}
