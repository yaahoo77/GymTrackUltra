package com.gymtrack.ultra.features.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymtrack.ultra.domain.model.UserSettings
import com.gymtrack.ultra.domain.progression.LevelProgress
import com.gymtrack.ultra.domain.progression.LevelSystem
import com.gymtrack.ultra.domain.repository.ProgressRepository
import com.gymtrack.ultra.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val isLoading: Boolean = true,
    val settings: UserSettings = UserSettings(),
    val level: LevelProgress = LevelSystem.progressFor(0),
    val completedWorkouts: Int = 0,
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    progressRepository: ProgressRepository,
) : ViewModel() {

    val uiState: StateFlow<ProfileUiState> = combine(
        settingsRepository.settings,
        progressRepository.observeProgress(),
    ) { settings, progress ->
        ProfileUiState(
            isLoading = false,
            settings = settings,
            level = progress.level,
            completedWorkouts = progress.completedSessions.size,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProfileUiState())

    fun setReduceMotion(enabled: Boolean) = update { it.copy(reduceMotion = enabled) }
    fun setHaptics(enabled: Boolean) = update { it.copy(hapticsEnabled = enabled) }
    fun setCelebrations(enabled: Boolean) = update { it.copy(celebrationsEnabled = enabled) }
    fun setWarmupReminder(enabled: Boolean) = update { it.copy(warmupReminder = enabled) }
    fun setProteinTracking(enabled: Boolean) = update { it.copy(proteinTracking = enabled) }

    private fun update(transform: (UserSettings) -> UserSettings) {
        viewModelScope.launch { settingsRepository.update(transform) }
    }
}
