package com.gymtrack.ultra.domain.progression

import com.gymtrack.ultra.domain.model.LoggedSet

/**
 * Punkty rang mięśni:
 *  • seria w ramach planu: +10 dla partii głównej, +5 dla pomocniczej (serie ponad plan: 0),
 *  • rekord osobisty: +15 dla partii głównych ćwiczenia,
 *  • maks. 60 pkt na partię w jednym treningu — nie da się "nabić" rangi jednym maratonem.
 */
object MuscleScoring {
    const val PRIMARY_SET = 10
    const val SECONDARY_SET = 5
    const val RECORD_BONUS = 15
    const val SESSION_CAP = 60

    /** Punkty zdobyte w każdej sesji osobno (kolejność chronologiczna). */
    fun perSession(history: List<LoggedSet>, records: List<PersonalRecord>): LinkedHashMap<Long, Map<MuscleGroup, Int>> {
        val recordKeys = records.map { it.sessionId to it.exerciseId }.toSet()
        val result = LinkedHashMap<Long, Map<MuscleGroup, Int>>()
        val sessions = history.groupBy { it.sessionId }.entries.sortedBy { it.value.first().sessionStart }

        for ((sessionId, sets) in sessions) {
            val points = HashMap<MuscleGroup, Int>()
            for ((exerciseId, exerciseSets) in sets.groupBy { it.exerciseId }) {
                val targets = MuscleMap.targetsOf(exerciseId)
                val counted = exerciseSets.size.coerceAtMost(exerciseSets.first().targetSets.coerceAtLeast(0))
                targets.primary.forEach { points.merge(it, counted * PRIMARY_SET, Int::plus) }
                targets.secondary.forEach { points.merge(it, counted * SECONDARY_SET, Int::plus) }
                if ((sessionId to exerciseId) in recordKeys) {
                    targets.primary.forEach { points.merge(it, RECORD_BONUS, Int::plus) }
                }
            }
            result[sessionId] = points.mapValues { it.value.coerceAtMost(SESSION_CAP) }
        }
        return result
    }

    fun totals(perSession: Map<Long, Map<MuscleGroup, Int>>): Map<MuscleGroup, Int> {
        val totals = HashMap<MuscleGroup, Int>()
        perSession.values.forEach { session -> session.forEach { (m, p) -> totals.merge(m, p, Int::plus) } }
        return totals
    }

    /** Liczba serii (w ramach planu) na partię główną — do wyzwań i opisu rangi. */
    fun plannedSetsByMuscle(history: List<LoggedSet>): Map<MuscleGroup, Int> {
        val result = HashMap<MuscleGroup, Int>()
        for (sets in history.groupBy { it.sessionId to it.exerciseId }.values) {
            val counted = sets.size.coerceAtMost(sets.first().targetSets.coerceAtLeast(0))
            MuscleMap.targetsOf(sets.first().exerciseId).primary.forEach { result.merge(it, counted, Int::plus) }
        }
        return result
    }
}
