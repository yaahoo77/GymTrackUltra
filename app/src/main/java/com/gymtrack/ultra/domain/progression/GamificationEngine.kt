package com.gymtrack.ultra.domain.progression

import com.gymtrack.ultra.core.time.WeekMath
import com.gymtrack.ultra.domain.model.LoggedSet
import com.gymtrack.ultra.domain.model.SessionSummary
import java.time.LocalDate
import java.time.ZoneId

data class MuscleRank(
    val muscle: MuscleGroup,
    val progress: RankProgress,
    /** Wszystkie serie (w ramach planu), w których ta partia była główna. */
    val plannedSets: Int,
) {
    /** Partia bez żadnej zaliczonej serii nie ma jeszcze rangi ("BEZ RANGI"). */
    val isRanked: Boolean get() = progress.totalPoints > 0
}

data class GamificationSnapshot(
    val muscles: List<MuscleRank>,
    val streak: StreakState,
    val quests: List<QuestProgress>,
    val records: List<PersonalRecord>,
) {
    val bestMuscleRank: Rank?
        get() = muscles.filter { it.isRanked }.maxOfOrNull { it.progress.rank }

    companion object {
        val Empty = GamificationSnapshot(
            muscles = MuscleGroup.entries.map { MuscleRank(it, MuscleRankCurve.progressFor(0), 0) },
            streak = StreakState.Empty,
            quests = emptyList(),
            records = emptyList(),
        )
    }
}

/** Składa rangi mięśni, serię, wyzwania i rekordy z historii — czysta funkcja, łatwa do testów. */
object GamificationEngine {

    fun build(
        sessions: List<SessionSummary>,
        history: List<LoggedSet>,
        plannedSetsBySession: Map<Long, Int>,
        planDays: Int,
        today: LocalDate,
        zone: ZoneId,
    ): GamificationSnapshot {
        val records = RecordDetector.detect(history)
        val totals = MuscleScoring.totals(MuscleScoring.perSession(history, records))
        val setsByMuscle = MuscleScoring.plannedSetsByMuscle(history)
        val muscles = MuscleGroup.entries.map { muscle ->
            MuscleRank(muscle, MuscleRankCurve.progressFor(totals[muscle] ?: 0), setsByMuscle[muscle] ?: 0)
        }

        val streak = StreakCalculator.compute(
            sessionDates = sessions.map { WeekMath.localDate(it.startedAt, zone) },
            today = today,
            weeklyMinimum = StreakCalculator.weeklyMinimum(planDays),
        )

        val weekStart = WeekMath.weekStart(today)
        val weekSessions = sessions.filter { WeekMath.weekStart(WeekMath.localDate(it.startedAt, zone)) == weekStart }
        val weekIds = weekSessions.map { it.id }.toSet()
        val weekHistory = history.filter { it.sessionId in weekIds }
        val cappedBySession = cappedCompletedBySession(weekHistory)
        val activity = WeekActivity(
            workouts = weekSessions.size,
            warmups = weekSessions.count { it.warmupDone },
            fullSessions = weekSessions.count { s ->
                val planned = plannedSetsBySession[s.id] ?: 0
                planned > 0 && (cappedBySession[s.id] ?: 0) >= planned
            },
            records = records.count { it.sessionId in weekIds },
            distinctPlanDays = weekSessions.mapNotNull { it.planDayId }.distinct().size,
            setsTotal = cappedBySession.values.sum(),
            setsByMuscle = MuscleScoring.plannedSetsByMuscle(weekHistory),
        )
        val quests = WeeklyQuests.progress(WeeklyQuests.forWeek(weekStart.toEpochDay(), planDays), activity)

        return GamificationSnapshot(muscles, streak, quests, records)
    }

    /** Serie zaliczone w ramach planu, per sesja (dodatkowe serie nie liczą się). */
    fun cappedCompletedBySession(history: List<LoggedSet>): Map<Long, Int> =
        history.groupBy { it.sessionId }.mapValues { (_, sets) ->
            sets.groupBy { it.exerciseId }.values.sumOf { exerciseSets ->
                exerciseSets.size.coerceAtMost(exerciseSets.first().targetSets.coerceAtLeast(0))
            }
        }
}
