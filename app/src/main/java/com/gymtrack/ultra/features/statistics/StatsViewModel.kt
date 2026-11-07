package com.gymtrack.ultra.features.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymtrack.ultra.core.time.TimeProvider
import com.gymtrack.ultra.domain.repository.ProgressRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import java.time.Duration
import javax.inject.Inject

enum class StatsRange(val days: Long?, val label: String) {
    WEEK(7, "7 dni"),
    MONTH(30, "30 dni"),
    QUARTER(90, "90 dni"),
    ALL(null, "Cały okres"),
}

/** Wszystkie liczby pochodzą z zapisanych sesji — żadnych przykładowych danych. */
data class StatsUiState(
    val isLoading: Boolean = true,
    val range: StatsRange = StatsRange.MONTH,
    val workouts: Int = 0,
    val totalMinutes: Long = 0,
    val averageMinutes: Long = 0,
    val completedSets: Int = 0,
    val hasAnyHistory: Boolean = false,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class StatsViewModel @Inject constructor(
    private val progressRepository: ProgressRepository,
    private val time: TimeProvider,
) : ViewModel() {

    private val range = MutableStateFlow(StatsRange.MONTH)

    val uiState: StateFlow<StatsUiState> = range
        .flatMapLatest { selected ->
            val from = selected.days?.let { time.now().minus(Duration.ofDays(it)) }
            combine(
                progressRepository.observeProgress(),
                progressRepository.observeCompletedSetsSince(from),
            ) { progress, sets ->
                val inRange = progress.completedSessions.filter { from == null || !it.startedAt.isBefore(from) }
                val minutes = inRange.sumOf { it.durationMillis } / 60_000
                StatsUiState(
                    isLoading = false,
                    range = selected,
                    workouts = inRange.size,
                    totalMinutes = minutes,
                    averageMinutes = if (inRange.isEmpty()) 0 else minutes / inRange.size,
                    completedSets = sets,
                    hasAnyHistory = progress.completedSessions.isNotEmpty(),
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StatsUiState())

    fun selectRange(newRange: StatsRange) {
        range.value = newRange
    }
}
