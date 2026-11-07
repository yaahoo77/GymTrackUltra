package com.gymtrack.ultra.domain.progression

import com.gymtrack.ultra.domain.model.LoggedSet
import com.gymtrack.ultra.domain.model.TrackingType

enum class RecordType(val label: String) {
    WEIGHT("Rekord ciężaru"),
    REPS("Rekord powtórzeń"),
    TIME("Rekord czasu"),
}

data class PersonalRecord(
    val sessionId: Long,
    val exerciseId: String,
    val type: RecordType,
    val weightKg: Double?,
    val reps: Int?,
    val durationSeconds: Int?,
)

/**
 * Rekordy osobiste w duchu bezpiecznej progresji:
 *  • rekord ciężaru liczy się tylko przy czystej serii (powtórzenia ≥ dolna granica zakresu),
 *  • powtórzenia ponad górną granicę zakresu nie dają rekordu — to sygnał, by dołożyć ciężaru,
 *  • pierwsza sesja ćwiczenia ustala punkt odniesienia (bez rekordu),
 *  • najwyżej jeden rekord na ćwiczenie w treningu.
 */
object RecordDetector {

    private class Best {
        var maxCleanWeight: Double = -1.0
        val sets = mutableListOf<Pair<Double, Int>>() // ciężar → powtórzenia (przycięte do zakresu)
        var maxDuration: Int = -1
        var seen = false
    }

    /** [history] — serie z ukończonych treningów; kolejność sesji ustalana po czasie startu. */
    fun detect(history: List<LoggedSet>): List<PersonalRecord> {
        val bests = HashMap<String, Best>()
        val records = mutableListOf<PersonalRecord>()
        val sessions = history.groupBy { it.sessionId }.values.sortedBy { it.first().sessionStart }

        for (sessionSets in sessions) {
            for ((exerciseId, sets) in sessionSets.groupBy { it.exerciseId }) {
                val best = bests.getOrPut(exerciseId) { Best() }
                val record = if (best.seen) findRecord(sets, best) else null
                if (record != null) records += record
                absorb(sets, best)
            }
        }
        return records
    }

    private fun weightOf(set: LoggedSet) = set.weightKg ?: 0.0
    private fun cappedReps(set: LoggedSet) = (set.reps ?: 0).coerceAtMost(set.targetMax)

    private fun findRecord(sets: List<LoggedSet>, best: Best): PersonalRecord? {
        val first = sets.first()
        if (first.tracking == TrackingType.TIME) {
            val top = sets.maxByOrNull { it.durationSeconds ?: 0 } ?: return null
            val duration = (top.durationSeconds ?: 0).coerceAtMost(top.targetMax)
            return if (duration > best.maxDuration && duration > 0) {
                PersonalRecord(first.sessionId, first.exerciseId, RecordType.TIME, null, null, duration)
            } else {
                null
            }
        }

        val clean = sets.filter { (it.reps ?: 0) >= it.targetMin }
        val heaviestClean = clean.maxByOrNull { weightOf(it) }
        if (heaviestClean != null && weightOf(heaviestClean) > best.maxCleanWeight && best.maxCleanWeight >= 0.0) {
            return PersonalRecord(
                first.sessionId, first.exerciseId, RecordType.WEIGHT,
                weightOf(heaviestClean), heaviestClean.reps, null,
            )
        }

        val repsRecord = sets
            .filter { cappedReps(it) > 0 }
            .filter { set ->
                val previousBest = best.sets.filter { it.first >= weightOf(set) }.maxOfOrNull { it.second } ?: return@filter false
                cappedReps(set) > previousBest
            }
            .maxWithOrNull(compareBy<LoggedSet>({ weightOf(it) }, { cappedReps(it) }))
        return repsRecord?.let {
            PersonalRecord(first.sessionId, first.exerciseId, RecordType.REPS, weightOf(it), cappedReps(it), null)
        }
    }

    private fun absorb(sets: List<LoggedSet>, best: Best) {
        best.seen = true
        for (set in sets) {
            if (set.tracking == TrackingType.TIME) {
                best.maxDuration = maxOf(best.maxDuration, (set.durationSeconds ?: 0).coerceAtMost(set.targetMax))
            } else {
                if ((set.reps ?: 0) >= set.targetMin) best.maxCleanWeight = maxOf(best.maxCleanWeight, weightOf(set))
                if (cappedReps(set) > 0) best.sets += weightOf(set) to cappedReps(set)
            }
        }
    }
}
