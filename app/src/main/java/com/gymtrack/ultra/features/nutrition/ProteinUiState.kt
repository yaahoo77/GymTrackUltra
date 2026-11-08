package com.gymtrack.ultra.features.nutrition

import com.gymtrack.ultra.domain.model.UserSettings
import com.gymtrack.ultra.domain.nutrition.FoodEntry
import com.gymtrack.ultra.domain.nutrition.FoodPresets
import com.gymtrack.ultra.domain.nutrition.ProteinCalculator
import com.gymtrack.ultra.domain.nutrition.ProteinDay
import com.gymtrack.ultra.domain.nutrition.ProteinGoal
import com.gymtrack.ultra.domain.nutrition.ProteinStats
import java.time.LocalDate

data class ProteinUiState(
    val isLoading: Boolean = true,
    val enabled: Boolean = true,
    /** null = cel jeszcze nieustawiony (pokazujemy konfigurację). */
    val target: Int? = null,
    val eatenToday: Int = 0,
    val todayEntries: List<FoodEntry> = emptyList(),
    val week: List<ProteinDay> = emptyList(),
    val daysOnTarget: Int = 0,
    val recentCustom: List<Pair<String, Int>> = emptyList(),
    val weightKg: Double? = null,
    val goal: ProteinGoal = ProteinGoal.TRAINING,
    val manualTarget: Int? = null,
) {
    val remaining: Int get() = target?.let { (it - eatenToday).coerceAtLeast(0) } ?: 0
    val progress: Float get() = target?.let { (eatenToday.toFloat() / it).coerceIn(0f, 1f) } ?: 0f
    val reachedToday: Boolean get() = target != null && eatenToday >= target
}

/** Karta na ekranie Start. null = funkcja wyłączona w Profilu. */
data class ProteinCardUi(val eaten: Int, val target: Int?) {
    val progress: Float get() = target?.let { (eaten.toFloat() / it).coerceIn(0f, 1f) } ?: 0f
}

internal object ProteinStateFactory {

    fun build(settings: UserSettings, entries: List<FoodEntry>, recent: List<FoodEntry>, today: LocalDate): ProteinUiState {
        val target = ProteinCalculator.resolveTarget(settings.bodyWeightKg, settings.proteinGoal, settings.manualProteinTarget)
        val todayEntries = entries.filter { it.date == today }.sortedByDescending { it.createdAt }
        val week = ProteinStats.lastDays(entries, today, 7)
        return ProteinUiState(
            isLoading = false,
            enabled = settings.proteinTracking,
            target = target,
            eatenToday = todayEntries.sumOf { it.proteinGrams },
            todayEntries = todayEntries,
            week = week,
            daysOnTarget = target?.let { ProteinStats.daysOnTarget(week, it) } ?: 0,
            recentCustom = ProteinStats.recentCustom(recent, FoodPresets.all.map { it.name }.toSet()),
            weightKg = settings.bodyWeightKg,
            goal = settings.proteinGoal,
            manualTarget = settings.manualProteinTarget,
        )
    }

    fun card(settings: UserSettings, entries: List<FoodEntry>, today: LocalDate): ProteinCardUi? {
        if (!settings.proteinTracking) return null
        val target = ProteinCalculator.resolveTarget(settings.bodyWeightKg, settings.proteinGoal, settings.manualProteinTarget)
        return ProteinCardUi(eaten = entries.filter { it.date == today }.sumOf { it.proteinGrams }, target = target)
    }
}
