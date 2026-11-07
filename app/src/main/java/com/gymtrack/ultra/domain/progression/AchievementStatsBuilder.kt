package com.gymtrack.ultra.domain.progression

import com.gymtrack.ultra.core.time.WeekMath
import com.gymtrack.ultra.domain.model.SessionSummary
import java.time.ZoneId

/** Zamienia historię ukończonych sesji na liczby, z których korzystają osiągnięcia. */
object AchievementStatsBuilder {

    const val MIN_WORKOUTS_PER_STREAK_WEEK = 2

    fun build(
        completedSessions: List<SessionSummary>,
        completedSets: Int,
        techniqueViewed: Int,
        techniqueTotal: Int,
        zone: ZoneId,
    ): AchievementStats {
        val byWeek = completedSessions.groupBy { WeekMath.weekStart(WeekMath.localDate(it.startedAt, zone)) }
        val countsByWeek = byWeek.mapValues { it.value.size }
        val maxPlanDays = byWeek.values.maxOfOrNull { week ->
            week.mapNotNull { it.planDayId }.distinct().size
        } ?: 0

        return AchievementStats(
            completedSets = completedSets,
            completedWorkouts = completedSessions.size,
            warmupWorkouts = completedSessions.count { it.warmupDone },
            maxPlanDaysInOneWeek = maxPlanDays,
            longestWeekStreak = WeekMath.longestWeekStreak(countsByWeek, MIN_WORKOUTS_PER_STREAK_WEEK),
            techniqueViewed = techniqueViewed,
            techniqueTotal = techniqueTotal,
        )
    }
}
