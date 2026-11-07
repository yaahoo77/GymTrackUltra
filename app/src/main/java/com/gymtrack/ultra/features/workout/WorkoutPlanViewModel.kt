package com.gymtrack.ultra.features.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymtrack.ultra.core.time.TimeProvider
import com.gymtrack.ultra.domain.model.PlanDay
import com.gymtrack.ultra.domain.repository.PlanRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.DayOfWeek
import javax.inject.Inject

data class WorkoutPlanUiState(
    val isLoading: Boolean = true,
    val days: List<PlanDay> = emptyList(),
    val today: DayOfWeek? = null,
)

@HiltViewModel
class WorkoutPlanViewModel @Inject constructor(
    planRepository: PlanRepository,
    private val time: TimeProvider,
) : ViewModel() {

    val uiState: StateFlow<WorkoutPlanUiState> = planRepository.observePlan()
        .map { days -> WorkoutPlanUiState(isLoading = false, days = days, today = time.today().dayOfWeek) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WorkoutPlanUiState())
}
