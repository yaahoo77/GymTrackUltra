package com.gymtrack.ultra.features.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymtrack.ultra.domain.model.ExperienceLevel
import com.gymtrack.ultra.domain.model.TrainingGoal
import com.gymtrack.ultra.domain.nutrition.ProteinCalculator
import com.gymtrack.ultra.domain.nutrition.ProteinGoal
import com.gymtrack.ultra.domain.repository.PlanRepository
import com.gymtrack.ultra.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import javax.inject.Inject

/** Wartości startowe kreatora — z obecnych ustawień i planu (kreator może się pojawić u kogoś, kto już ćwiczy). */
data class OnboardingDefaults(
    val loaded: Boolean = false,
    val name: String = "",
    val goal: TrainingGoal = TrainingGoal.STRENGTH,
    val experience: ExperienceLevel = ExperienceLevel.BEGINNER,
    val days: Set<DayOfWeek> = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
    val planDays: Int = 3,
    val reduceMotion: Boolean = false,
    val haptics: Boolean = true,
    val celebrations: Boolean = true,
)

/** Wynik kreatora. Waga opcjonalna — null = krok białka pominięty. */
data class OnboardingResult(
    val name: String,
    val goal: TrainingGoal,
    val experience: ExperienceLevel,
    val days: Set<DayOfWeek>,
    val weightKg: Double?,
    val proteinGoal: ProteinGoal,
    val reduceMotion: Boolean,
    val haptics: Boolean,
    val celebrations: Boolean,
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val settings: SettingsRepository,
    private val plan: PlanRepository,
) : ViewModel() {

    val defaults: StateFlow<OnboardingDefaults> = combine(settings.settings, plan.observePlan()) { s, days ->
        OnboardingDefaults(
            loaded = true,
            name = s.displayName,
            goal = s.goal,
            experience = s.experience,
            days = days.map { it.dayOfWeek }.toSet().takeIf { it.size == days.size && it.isNotEmpty() } ?: s.trainingDays,
            planDays = days.size.coerceAtLeast(1),
            reduceMotion = s.reduceMotion,
            haptics = s.hapticsEnabled,
            celebrations = s.celebrationsEnabled,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), OnboardingDefaults())

    fun proteinPreview(weightText: String, goal: ProteinGoal): Int? =
        weightText.trim().replace(',', '.').toDoubleOrNull()
            ?.takeIf { ProteinCalculator.isValidWeight(it) }
            ?.let { ProteinCalculator.dailyTarget(it, goal) }

    fun finish(result: OnboardingResult) {
        viewModelScope.launch {
            val sortedDays = result.days.sortedBy { it.value }
            runCatching { plan.assignWeekdays(sortedDays) }
            settings.update {
                it.copy(
                    onboardingCompleted = true,
                    displayName = result.name,
                    goal = result.goal,
                    experience = result.experience,
                    trainingDays = result.days,
                    reduceMotion = result.reduceMotion,
                    hapticsEnabled = result.haptics,
                    celebrationsEnabled = result.celebrations,
                    bodyWeightKg = result.weightKg?.takeIf { w -> ProteinCalculator.isValidWeight(w) } ?: it.bodyWeightKg,
                    proteinGoal = if (result.weightKg != null) result.proteinGoal else it.proteinGoal,
                    manualProteinTarget = if (result.weightKg != null) null else it.manualProteinTarget,
                )
            }
        }
    }

    fun skip() {
        viewModelScope.launch { settings.update { it.copy(onboardingCompleted = true) } }
    }
}
