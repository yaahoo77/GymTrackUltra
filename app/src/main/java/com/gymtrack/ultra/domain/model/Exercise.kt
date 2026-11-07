package com.gymtrack.ultra.domain.model

/** Czy wynik serii to powtórzenia, czy czas trwania (deska, deska bokiem). */
enum class TrackingType { REPS, TIME }

data class Exercise(
    val id: String,
    val namePl: String,
    val nameEn: String,
    val muscleGroup: String,
    val trackingType: TrackingType,
    /** Np. "na stronę", "na nogę" — null, gdy ćwiczenie jest symetryczne. */
    val sideNote: String?,
    val technique: List<String>,
    val safety: List<String>,
)
