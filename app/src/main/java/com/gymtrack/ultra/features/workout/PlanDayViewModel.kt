package com.gymtrack.ultra.features.workout

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.gymtrack.ultra.domain.model.PlanDay
import com.gymtrack.ultra.domain.repository.PlanRepository
import com.gymtrack.ultra.navigation.PlanDayRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface PlanDayUiState {
    data object Loading : PlanDayUiState
    data object NotFound : PlanDayUiState
    data class Ready(val day: PlanDay) : PlanDayUiState
}

@HiltViewModel
class PlanDayViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val planRepository: PlanRepository,
) : ViewModel() {

    private val dayId: Long = savedStateHandle.toRoute<PlanDayRoute>().dayId

    val uiState: StateFlow<PlanDayUiState> = planRepository.observePlanDay(dayId)
        .map { day -> if (day == null) PlanDayUiState.NotFound else PlanDayUiState.Ready(day) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PlanDayUiState.Loading)

    /** Zapis do osiągnięcia TECHNIQUE MASTER — liczy się pierwsze otwarcie każdego ćwiczenia. */
    fun onTechniqueOpened(exerciseId: String) {
        viewModelScope.launch { planRepository.markTechniqueViewed(exerciseId) }
    }
}
