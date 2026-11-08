package com.gymtrack.ultra.domain.model

import com.gymtrack.ultra.domain.nutrition.ProteinGoal
import java.time.DayOfWeek

enum class TrainingGoal(val label: String) {
    STRENGTH("Siła"),
    FITNESS("Ogólna sprawność"),
    MUSCLE("Budowa mięśni"),
    HABIT("Regularność"),
}

enum class ExperienceLevel(val label: String) {
    BEGINNER("Początkujący"),
    INTERMEDIATE("Średniozaawansowany"),
    RETURNING("Wracam po przerwie"),
}

/**
 * Ustawienia trzymane w DataStore, tylko na telefonie. Bez wzrostu, zdjęć ani wieku.
 * Waga jest opcjonalna — podaje się ją wyłącznie po to, żeby policzyć cel białka
 * (można też wpisać cel ręcznie i w ogóle jej nie podawać).
 */
data class UserSettings(
    val onboardingCompleted: Boolean = false,
    val displayName: String = "",
    val goal: TrainingGoal = TrainingGoal.STRENGTH,
    val experience: ExperienceLevel = ExperienceLevel.BEGINNER,
    val trainingDays: Set<DayOfWeek> = DEFAULT_TRAINING_DAYS,
    val reduceMotion: Boolean = false,
    val hapticsEnabled: Boolean = true,
    val celebrationsEnabled: Boolean = true,
    val warmupReminder: Boolean = true,
    val proteinTracking: Boolean = true,
    val bodyWeightKg: Double? = null,
    val proteinGoal: ProteinGoal = ProteinGoal.TRAINING,
    val manualProteinTarget: Int? = null,
) {
    companion object {
        val DEFAULT_TRAINING_DAYS: Set<DayOfWeek> =
            setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY)
    }
}
