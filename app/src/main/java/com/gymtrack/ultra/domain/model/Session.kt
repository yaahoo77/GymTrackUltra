package com.gymtrack.ultra.domain.model

import java.time.Duration
import java.time.Instant

enum class SessionStatus { ACTIVE, COMPLETED, ABANDONED }

/** Lekkie podsumowanie zakończonej sesji — do historii, statystyk i osiągnięć. */
data class SessionSummary(
    val id: Long,
    val planDayId: Long?,
    val title: String,
    val startedAt: Instant,
    val endedAt: Instant?,
    val warmupDone: Boolean,
    val xpEarned: Int,
) {
    val durationMillis: Long
        get() = endedAt?.let { Duration.between(startedAt, it).toMillis().coerceAtLeast(0) } ?: 0L
}
