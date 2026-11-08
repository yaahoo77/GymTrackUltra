package com.gymtrack.ultra.features.history

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.gymtrack.ultra.core.time.TimeProvider
import com.gymtrack.ultra.domain.repository.PlanRepository
import com.gymtrack.ultra.domain.repository.ProgressRepository
import com.gymtrack.ultra.navigation.HistoryDetailRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import java.time.ZoneId
import javax.inject.Inject

data class HistoryUiState(
    val isLoading: Boolean = true,
    val months: List<HistoryMonth> = emptyList(),
    val total: Int = 0,
    val zone: ZoneId = ZoneId.systemDefault(),
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    progress: ProgressRepository,
    plan: PlanRepository,
    time: TimeProvider,
) : ViewModel() {

    val uiState: StateFlow<HistoryUiState> = combine(
        progress.observeProgress(),
        progress.observeLoggedSets(),
        plan.observeLibrary(),
    ) { snapshot, sets, library ->
        val names = library.associate { it.id to it.namePl }
        val items = HistoryBuilder.items(snapshot.completedSessions, sets, snapshot.gamification.records, names)
        HistoryUiState(isLoading = false, months = HistoryBuilder.months(items, time.zone()), total = items.size, zone = time.zone())
    }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryUiState())
}

sealed interface HistoryDetailUiState {
    data object Loading : HistoryDetailUiState
    data object NotFound : HistoryDetailUiState
    data class Ready(val detail: HistoryDetail, val zone: ZoneId) : HistoryDetailUiState
}

@HiltViewModel
class HistoryDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    progress: ProgressRepository,
    plan: PlanRepository,
    time: TimeProvider,
) : ViewModel() {

    private val sessionId: Long = savedStateHandle.toRoute<HistoryDetailRoute>().sessionId

    val uiState: StateFlow<HistoryDetailUiState> = combine(
        progress.observeProgress(),
        progress.observeLoggedSets(),
        plan.observeLibrary(),
    ) { snapshot, sets, library ->
        val names = library.associate { it.id to it.namePl }
        val item = HistoryBuilder.items(
            snapshot.completedSessions.filter { it.id == sessionId },
            sets,
            snapshot.gamification.records,
            names,
        ).firstOrNull()
        if (item == null) {
            HistoryDetailUiState.NotFound
        } else {
            HistoryDetailUiState.Ready(HistoryBuilder.detail(item, sets, snapshot.gamification.records, names), time.zone())
        }
    }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryDetailUiState.Loading)
}
