package com.gymtrack.ultra.features.complete

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.gymtrack.ultra.domain.model.WorkoutResult
import com.gymtrack.ultra.domain.repository.WorkoutRepository
import com.gymtrack.ultra.navigation.WorkoutCompleteRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface CompleteUiState {
    data object Loading : CompleteUiState
    data object Missing : CompleteUiState
    data class Ready(val result: WorkoutResult) : CompleteUiState
}

@HiltViewModel
class WorkoutCompleteViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val workout: WorkoutRepository,
) : ViewModel() {

    private val sessionId = savedStateHandle.toRoute<WorkoutCompleteRoute>().sessionId

    private val _state = MutableStateFlow<CompleteUiState>(CompleteUiState.Loading)
    val state: StateFlow<CompleteUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val result = runCatching { workout.loadResult(sessionId) }.getOrNull()
            _state.value = if (result != null) CompleteUiState.Ready(result) else CompleteUiState.Missing
        }
    }
}
