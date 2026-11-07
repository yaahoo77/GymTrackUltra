package com.gymtrack.ultra.domain.model

import java.time.Instant

/** Jedna zaliczona seria z ukończonego treningu — surowiec dla rang, rekordów, serii i wyzwań. */
data class LoggedSet(
    val sessionId: Long,
    val sessionStart: Instant,
    val planDayId: Long?,
    val exerciseId: String,
    val tracking: TrackingType,
    val setIndex: Int,
    val targetSets: Int,
    val targetMin: Int,
    val targetMax: Int,
    val weightKg: Double?,
    val reps: Int?,
    val durationSeconds: Int?,
)
