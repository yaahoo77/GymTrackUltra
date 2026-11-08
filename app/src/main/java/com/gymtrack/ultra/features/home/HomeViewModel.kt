package com.gymtrack.ultra.features.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymtrack.ultra.features.nutrition.ProteinStateFactory
import com.gymtrack.ultra.domain.repository.NutritionRepository
import com.gymtrack.ultra.core.time.TimeProvider
import com.gymtrack.ultra.domain.repository.PlanRepository
import com.gymtrack.ultra.domain.repository.ProgressRepository
import com.gymtrack.ultra.domain.repository.SettingsRepository
import com.gymtrack.ultra.domain.repository.WorkoutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    planRepository: PlanRepository,
    progressRepository: ProgressRepository,
    settingsRepository: SettingsRepository,
    nutritionRepository: NutritionRepository,
    private val workoutRepository: WorkoutRepository,
    private val time: TimeProvider,
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = combine(
        planRepository.observePlan(),
        progressRepository.observeProgress(),
        settingsRepository.settings,
        workoutRepository.observeActiveSession(),
        nutritionRepository.observeEntries(1),
    ) { plan, progress, settings, active, food ->
        HomeStateFactory.build(plan, progress, settings, time.now(), time.zone()).copy(
            activeSession = active?.let { ActiveSessionUi(it.title, it.completedPlannedSets, it.plannedSets) },
            protein = ProteinStateFactory.card(settings, food, time.today()),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState(),
    )

    private val _openLive = Channel<Unit>(Channel.BUFFERED)
    val openLive: Flow<Unit> = _openLive.receiveAsFlow()

    /** START WORKOUT: tworzy sesję (albo wznawia trwającą) i otwiera trening na żywo. */
    fun startWorkout(dayId: Long) {
        viewModelScope.launch {
            try {
                workoutRepository.startOrResume(dayId)
                _openLive.send(Unit)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Brak dnia w planie — karta odświeży się sama.
            }
        }
    }
}
