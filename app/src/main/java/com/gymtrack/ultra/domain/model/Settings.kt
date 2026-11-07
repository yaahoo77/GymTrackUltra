package com.gymtrack.ultra.domain.model

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
 * Ustawienia trzymane w DataStore. Celowo bez wagi, wzrostu, zdjęć ani wieku —
 * aplikacja ich nie potrzebuje.
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
) {
    companion object {
        val DEFAULT_TRAINING_DAYS: Set<DayOfWeek> =
            setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY)
    }
}
