package com.gymtrack.ultra.domain.plan

import com.gymtrack.ultra.domain.model.PlanDay
import java.time.LocalDate

data class TodayPlan(
    val day: PlanDay,
    /** true = trening zaplanowany na dziś; false = dziś regeneracja, [day] to najbliższy kolejny. */
    val isToday: Boolean,
    val doneToday: Boolean,
    /** 0 = dziś, 1 = jutro… */
    val daysUntil: Int,
)

object TodayPlanner {

    /**
     * Wybiera trening na dziś albo najbliższy zaplanowany.
     * Dzień bez treningu to regeneracja — nie "zaległość".
     */
    fun resolve(plan: List<PlanDay>, today: LocalDate, completedTodayPlanDayIds: Set<Long>): TodayPlan? {
        if (plan.isEmpty()) return null

        plan.firstOrNull { it.dayOfWeek == today.dayOfWeek }?.let { day ->
            return TodayPlan(day, isToday = true, doneToday = day.id in completedTodayPlanDayIds, daysUntil = 0)
        }

        for (offset in 1..7) {
            val dow = today.plusDays(offset.toLong()).dayOfWeek
            val next = plan.firstOrNull { it.dayOfWeek == dow }
            if (next != null) return TodayPlan(next, isToday = false, doneToday = false, daysUntil = offset)
        }
        return null
    }
}
