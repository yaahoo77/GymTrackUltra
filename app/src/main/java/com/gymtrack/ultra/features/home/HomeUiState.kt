package com.gymtrack.ultra.features.home

import com.gymtrack.ultra.core.format.Formatters
import com.gymtrack.ultra.core.time.WeekMath
import com.gymtrack.ultra.domain.model.PlanDay
import com.gymtrack.ultra.domain.model.RecordWithName
import com.gymtrack.ultra.domain.model.ProgressSnapshot
import com.gymtrack.ultra.domain.model.SessionSummary
import com.gymtrack.ultra.domain.model.UserSettings
import com.gymtrack.ultra.domain.plan.TodayPlanner
import com.gymtrack.ultra.domain.progression.AchievementProgress
import com.gymtrack.ultra.domain.progression.LevelProgress
import com.gymtrack.ultra.domain.progression.LevelSystem
import com.gymtrack.ultra.domain.progression.MuscleRank
import com.gymtrack.ultra.domain.progression.QuestProgress
import com.gymtrack.ultra.domain.progression.StreakState
import java.time.Instant
import java.time.ZoneId

enum class WeekDayState { DONE, TODAY_PLANNED, TODAY_REST, PLANNED, REST }

data class WeekDayUi(
    val label: String,
    val mark: String,
    val state: WeekDayState,
    val accessibilityText: String,
)

data class TodayCardUi(
    val dayId: Long,
    val eyebrow: String,
    val whenText: String,
    val title: String,
    val exercisePreview: String,
    val exerciseCount: Int,
    val setCount: Int,
    val minutes: Int,
    val isToday: Boolean,
    val doneToday: Boolean,
)

data class ActiveSessionUi(val title: String, val done: Int, val planned: Int)

data class HomeUiState(
    val isLoading: Boolean = true,
    val activeSession: ActiveSessionUi? = null,
    val streak: StreakState = StreakState.Empty,
    val quests: List<QuestProgress> = emptyList(),
    val topMuscles: List<MuscleRank> = emptyList(),
    val recentRecords: List<RecordWithName> = emptyList(),
    val greeting: String = "",
    val headline: String = "",
    val level: LevelProgress = LevelSystem.progressFor(0),
    val today: TodayCardUi? = null,
    val week: List<WeekDayUi> = emptyList(),
    val weeklyDone: Int = 0,
    val weeklyTarget: Int = 0,
    val lastSession: SessionSummary? = null,
    val latestAchievement: AchievementProgress? = null,
    val showWarmupHint: Boolean = true,
)

/** Czysta funkcja: dane → stan ekranu. Testowalna bez Androida. */
internal object HomeStateFactory {

    fun build(
        plan: List<PlanDay>,
        progress: ProgressSnapshot,
        settings: UserSettings,
        now: Instant,
        zone: ZoneId,
    ): HomeUiState {
        val nowZoned = now.atZone(zone)
        val today = nowZoned.toLocalDate()
        val sessionsByDate = progress.completedSessions.groupBy { WeekMath.localDate(it.startedAt, zone) }
        val completedTodayIds = sessionsByDate[today].orEmpty().mapNotNull { it.planDayId }.toSet()
        val todayPlan = TodayPlanner.resolve(plan, today, completedTodayIds)

        val weekStart = WeekMath.weekStart(today)
        val week = (0L..6L).map { offset ->
            val date = weekStart.plusDays(offset)
            val planned = plan.firstOrNull { it.dayOfWeek == date.dayOfWeek }
            val done = sessionsByDate[date].orEmpty().isNotEmpty()
            val isToday = date == today
            val state = when {
                done -> WeekDayState.DONE
                isToday && planned != null -> WeekDayState.TODAY_PLANNED
                isToday -> WeekDayState.TODAY_REST
                planned != null && date.isAfter(today) -> WeekDayState.PLANNED
                // Zaplanowany, ale minął bez treningu: neutralnie, bez czerwieni i bez kary.
                else -> WeekDayState.REST
            }
            val dayName = Formatters.dayName(date.dayOfWeek)
            WeekDayUi(
                label = Formatters.shortDay(date.dayOfWeek),
                mark = when (state) {
                    WeekDayState.DONE -> "✓"
                    WeekDayState.TODAY_PLANNED, WeekDayState.PLANNED -> planned?.code.orEmpty()
                    else -> "·"
                },
                state = state,
                accessibilityText = when (state) {
                    WeekDayState.DONE -> "$dayName: trening zrobiony"
                    WeekDayState.TODAY_PLANNED -> "Dziś, $dayName: ${planned?.title.orEmpty()}"
                    WeekDayState.TODAY_REST -> "Dziś, $dayName: regeneracja"
                    WeekDayState.PLANNED -> "$dayName: zaplanowany ${planned?.title.orEmpty()}"
                    WeekDayState.REST -> "$dayName: regeneracja"
                },
            )
        }
        val weeklyDone = (0L..6L).sumOf { sessionsByDate[weekStart.plusDays(it)].orEmpty().size }

        val todayCard = todayPlan?.let { tp ->
            val day = tp.day
            val names = day.exercises.map { it.exercise.namePl }
            TodayCardUi(
                dayId = day.id,
                eyebrow = when {
                    tp.isToday && tp.doneToday -> "DZIŚ ZROBIONE"
                    tp.isToday -> "TODAY'S WORKOUT"
                    else -> "NEXT WORKOUT"
                },
                whenText = when (tp.daysUntil) {
                    0 -> Formatters.capitalize(Formatters.dayName(day.dayOfWeek))
                    1 -> "Jutro"
                    else -> Formatters.capitalize(Formatters.onDay(day.dayOfWeek))
                },
                title = day.title,
                exercisePreview = names.take(3).joinToString(", ") + if (names.size > 3) "…" else "",
                exerciseCount = day.exercises.size,
                setCount = day.totalSets,
                minutes = day.estimatedMinutes,
                isToday = tp.isToday,
                doneToday = tp.doneToday,
            )
        }

        val headline = when {
            todayPlan == null -> "Ułóżmy plan"
            todayPlan.isToday && todayPlan.doneToday -> "Robota zrobiona"
            todayPlan.isToday -> "Gotowy na ${todayPlan.day.code}?"
            else -> "Dziś regeneracja"
        }

        val exerciseNames = plan.flatMap { it.exercises }.associate { it.exercise.id to it.exercise.namePl }
        val gamification = progress.gamification
        val topMuscles = gamification.muscles
            .sortedWith(compareByDescending<MuscleRank> { it.progress.totalPoints })
            .take(3)
        val recentRecords = gamification.records
            .takeLast(3)
            .reversed()
            .map { RecordWithName(it, exerciseNames[it.exerciseId] ?: it.exerciseId) }

        return HomeUiState(
            isLoading = false,
            streak = gamification.streak,
            quests = gamification.quests,
            topMuscles = topMuscles,
            recentRecords = recentRecords,
            greeting = greetingFor(nowZoned.hour, settings.displayName),
            headline = headline,
            level = progress.level,
            today = todayCard,
            week = week,
            weeklyDone = weeklyDone,
            weeklyTarget = plan.size,
            lastSession = progress.completedSessions.firstOrNull(),
            latestAchievement = progress.achievements
                .filter { it.isUnlocked }
                .maxByOrNull { it.unlockedAt ?: 0L },
            showWarmupHint = settings.warmupReminder,
        )
    }

    fun greetingFor(hour: Int, name: String): String {
        val base = when (hour) {
            in 5..10 -> "Dzień dobry"
            in 11..17 -> "Cześć"
            in 18..22 -> "Dobry wieczór"
            else -> "Późna pora"
        }
        return if (name.isBlank()) base else "$base, ${name.trim()}"
    }
}
