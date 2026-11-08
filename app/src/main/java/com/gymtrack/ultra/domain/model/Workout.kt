package com.gymtrack.ultra.domain.model

import com.gymtrack.ultra.domain.progression.LoadAdvice
import com.gymtrack.ultra.domain.progression.AchievementDefinition
import com.gymtrack.ultra.domain.progression.LevelProgress
import com.gymtrack.ultra.domain.progression.MuscleGroup
import com.gymtrack.ultra.domain.progression.PersonalRecord
import com.gymtrack.ultra.domain.progression.QuestDefinition
import com.gymtrack.ultra.domain.progression.Rank
import com.gymtrack.ultra.domain.progression.StreakState
import com.gymtrack.ultra.domain.progression.XpLine
import java.time.DayOfWeek
import java.time.Instant
import kotlin.math.min

data class LiveSet(
    val id: Long,
    val index: Int,
    val weightKg: Double?,
    val reps: Int?,
    val durationSeconds: Int?,
    val completed: Boolean,
)

data class PreviousSet(val weightKg: Double?, val reps: Int?, val durationSeconds: Int?)

data class LiveExercise(
    val sessionExerciseId: Long,
    val exercise: Exercise,
    val position: Int,
    val targetSets: Int,
    val targetMin: Int,
    val targetMax: Int,
    val restSeconds: Int,
    val note: String?,
    val sets: List<LiveSet>,
    /** Serie z ostatniego ukończonego treningu z tym ćwiczeniem. */
    val previous: List<PreviousSet>,
    /** Podpowiedź ciężaru na dziś (podwójna progresja); null = brak danych albo ćwiczenie bez ciężaru. */
    val advice: LoadAdvice? = null,
) {
    val completedSets: Int get() = sets.count { it.completed }
    val isDone: Boolean get() = completedSets >= targetSets

    val targetLabel: String
        get() {
            val range = if (targetMin == targetMax) "$targetMin" else "$targetMin–$targetMax"
            val unit = if (exercise.trackingType == TrackingType.TIME) " s" else ""
            val side = exercise.sideNote?.let { " $it" }.orEmpty()
            return "$targetSets × $range$unit$side"
        }
}

data class LiveSession(
    val id: Long,
    val planDayId: Long?,
    val title: String,
    val startedAt: Instant,
    val warmupDone: Boolean,
    val exercises: List<LiveExercise>,
) {
    val plannedSets: Int get() = exercises.sumOf { it.targetSets }
    val completedPlannedSets: Int get() = exercises.sumOf { min(it.completedSets, it.targetSets) }
    val completedSetsTotal: Int get() = exercises.sumOf { it.completedSets }
    val progress: Float get() = if (plannedSets == 0) 0f else completedPlannedSets.toFloat() / plannedSets
}

/** Trwająca przerwa: koniec jako znacznik czasu, więc przetrwa obrót ekranu, tło i śmierć procesu. */
data class RestTimer(
    val endsAtMillis: Long,
    val totalSeconds: Int,
    val label: String,
)

/** Awans rangi: [muscle] = null oznacza rangę konta. */
data class RankUp(val muscle: MuscleGroup?, val from: Rank?, val to: Rank)

data class RecordWithName(val record: PersonalRecord, val exerciseName: String)

data class WorkoutResult(
    val sessionId: Long,
    val title: String,
    val durationMillis: Long,
    val completedExercises: Int,
    val totalExercises: Int,
    val completedSets: Int,
    val plannedSets: Int,
    val xpLines: List<XpLine>,
    val xpNote: String?,
    val levelBefore: LevelProgress,
    val levelAfter: LevelProgress,
    val rankUps: List<RankUp>,
    val muscleGains: Map<MuscleGroup, Int>,
    val records: List<RecordWithName>,
    val newAchievements: List<AchievementDefinition>,
    val completedQuests: List<QuestDefinition>,
    val streakBefore: Int,
    val streakAfter: StreakState,
    /** Dni bieżącego tygodnia z treningiem — do rzędu dni na ekranie serii. */
    val trainedThisWeek: Set<DayOfWeek>,
    val volumeKg: Double,
    val previousVolumeKg: Double?,
) {
    val xpTotal: Int get() = xpLines.sumOf { it.amount }
}
