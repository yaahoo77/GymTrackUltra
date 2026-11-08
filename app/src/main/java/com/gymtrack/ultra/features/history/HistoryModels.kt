package com.gymtrack.ultra.features.history

import com.gymtrack.ultra.domain.model.LoggedSet
import com.gymtrack.ultra.domain.model.SessionSummary
import com.gymtrack.ultra.domain.model.TrackingType
import com.gymtrack.ultra.domain.progression.PersonalRecord
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId

data class HistoryItem(
    val sessionId: Long,
    val title: String,
    val startedAt: Instant,
    val durationMillis: Long,
    val sets: Int,
    val volumeKg: Double,
    val xp: Int,
    val records: Int,
    val exerciseNames: List<String>,
)

data class HistoryMonth(val label: String, val items: List<HistoryItem>)

data class HistoryExercise(
    val exerciseId: String,
    val name: String,
    val tracking: TrackingType,
    val sets: List<LoggedSet>,
    /** Czy w tym treningu padł rekord osobisty w tym ćwiczeniu. */
    val hasRecord: Boolean,
)

data class HistoryDetail(val item: HistoryItem, val exercises: List<HistoryExercise>)

/** Czyste funkcje: zapisane dane → historia. Testowalne bez Androida. */
internal object HistoryBuilder {

    private val months = listOf(
        "Styczeń", "Luty", "Marzec", "Kwiecień", "Maj", "Czerwiec",
        "Lipiec", "Sierpień", "Wrzesień", "Październik", "Listopad", "Grudzień",
    )

    fun volumeOf(set: LoggedSet): Double = (set.weightKg ?: 0.0) * (set.reps ?: 0)

    fun items(
        sessions: List<SessionSummary>,
        sets: List<LoggedSet>,
        records: List<PersonalRecord>,
        names: Map<String, String>,
    ): List<HistoryItem> {
        val setsBySession = sets.groupBy { it.sessionId }
        val recordsBySession = records.groupBy { it.sessionId }
        return sessions.sortedByDescending { it.startedAt }.map { session ->
            val own = setsBySession[session.id].orEmpty()
            HistoryItem(
                sessionId = session.id,
                title = session.title,
                startedAt = session.startedAt,
                durationMillis = session.durationMillis,
                sets = own.size,
                volumeKg = own.sumOf { volumeOf(it) },
                xp = session.xpEarned,
                records = recordsBySession[session.id]?.size ?: 0,
                exerciseNames = own.map { it.exerciseId }.distinct().map { names[it] ?: it },
            )
        }
    }

    fun months(items: List<HistoryItem>, zone: ZoneId): List<HistoryMonth> =
        items.groupBy { YearMonth.from(it.startedAt.atZone(zone)) }
            .toSortedMap(compareByDescending { it })
            .map { (month, list) -> HistoryMonth("${months[month.monthValue - 1]} ${month.year}", list) }

    fun detail(
        item: HistoryItem,
        sets: List<LoggedSet>,
        records: List<PersonalRecord>,
        names: Map<String, String>,
    ): HistoryDetail {
        val own = sets.filter { it.sessionId == item.sessionId }
        val recordExercises = records.filter { it.sessionId == item.sessionId }.map { it.exerciseId }.toSet()
        // Kolejność ćwiczeń jak w treningu: po pierwszej zaliczonej serii (ćwiczenia z planu idą po kolei).
        val exercises = own.groupBy { it.exerciseId }.map { (id, list) ->
            HistoryExercise(
                exerciseId = id,
                name = names[id] ?: id,
                tracking = list.first().tracking,
                sets = list.sortedBy { it.setIndex },
                hasRecord = id in recordExercises,
            )
        }
        return HistoryDetail(item, exercises)
    }
}
