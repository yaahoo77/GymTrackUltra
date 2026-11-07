package com.gymtrack.ultra.features.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymtrack.ultra.core.time.TimeProvider
import com.gymtrack.ultra.domain.repository.PlanRepository
import com.gymtrack.ultra.domain.repository.ProgressRepository
import com.gymtrack.ultra.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    planRepository: PlanRepository,
    progressRepository: ProgressRepository,
    settingsRepository: SettingsRepository,
    private val time: TimeProvider,
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = combine(
        planRepository.observePlan(),
        progressRepository.observeProgress(),
        settingsRepository.settings,
    ) { plan, progress, settings ->
        HomeStateFactory.build(plan, progress, settings, time.now(), time.zone())
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState(),
    )
}
