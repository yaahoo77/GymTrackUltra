package com.gymtrack.ultra.features.live

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymtrack.ultra.core.format.Formatters
import com.gymtrack.ultra.core.service.WorkoutServiceController
import com.gymtrack.ultra.domain.model.LiveExercise
import com.gymtrack.ultra.domain.model.LiveSession
import com.gymtrack.ultra.domain.model.LiveSet
import com.gymtrack.ultra.domain.model.RestTimer
import com.gymtrack.ultra.domain.model.TrackingType
import com.gymtrack.ultra.domain.repository.RestTimerRepository
import com.gymtrack.ultra.domain.repository.WorkoutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LiveUiState(
    val isLoading: Boolean = true,
    val session: LiveSession? = null,
    val timer: RestTimer? = null,
    /** Zmienia się, gdy wartości serii zmieniły się "z zewnątrz" (np. Lżej −10%) — pola wczytują je na nowo. */
    val inputRevision: Int = 0,
    val isFinishing: Boolean = false,
)

sealed interface LiveEvent {
    data class Finished(val sessionId: Long) : LiveEvent
    data object Discarded : LiveEvent
    data class SetCompleted(val setId: Long, val label: String) : LiveEvent
    data class Message(val text: String) : LiveEvent
}

@HiltViewModel
class LiveWorkoutViewModel @Inject constructor(
    private val workout: WorkoutRepository,
    private val rest: RestTimerRepository,
    private val service: WorkoutServiceController,
) : ViewModel() {

    private val revision = MutableStateFlow(0)
    private val finishing = MutableStateFlow(false)

    val uiState: StateFlow<LiveUiState> = combine(
        workout.observeActiveSession(),
        rest.timer,
        revision,
        finishing,
    ) { session, timer, rev, isFinishing ->
        LiveUiState(isLoading = false, session = session, timer = timer, inputRevision = rev, isFinishing = isFinishing)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LiveUiState())

    private val _events = Channel<LiveEvent>(Channel.BUFFERED)
    val events: Flow<LiveEvent> = _events.receiveAsFlow()

    fun onScreenShown() = service.ensureRunning()

    /** Zapis na bieżąco — przerwany trening niczego nie gubi. */
    fun onValuesChanged(exercise: LiveExercise, set: LiveSet, weightText: String, valueText: String) {
        val weight = Formatters.parseWeight(weightText)
        val value = Formatters.parseCount(valueText)
        launchSafely {
            if (exercise.exercise.trackingType == TrackingType.TIME) {
                workout.updateSetValues(set.id, weight, null, value)
            } else {
                workout.updateSetValues(set.id, weight, value, null)
            }
        }
    }

    fun onToggleSet(exercise: LiveExercise, set: LiveSet, weightText: String, valueText: String) {
        launchSafely {
            if (set.completed) {
                workout.setCompleted(set.id, false)
                return@launchSafely
            }
            val timed = exercise.exercise.trackingType == TrackingType.TIME
            val weight = Formatters.parseWeight(weightText) ?: set.weightKg
            val value = Formatters.parseCount(valueText) ?: placeholderValue(exercise, set)
            if (timed) {
                workout.updateSetValues(set.id, weight, null, value)
            } else {
                workout.updateSetValues(set.id, weight, value, null)
            }
            workout.setCompleted(set.id, true)

            val session = uiState.value.session
            val isLastOverall = session != null && session.exercises.all { ex ->
                ex.sets.all { it.completed || it.id == set.id }
            }
            if (!isLastOverall && exercise.restSeconds > 0) {
                val nextLabel = nextUpLabel(session, exercise, set)
                rest.start(exercise.restSeconds, nextLabel)
            }
            _events.send(LiveEvent.SetCompleted(set.id, "Seria ${set.index + 1} zaliczona"))
        }
    }

    fun onUndo(setId: Long) {
        launchSafely {
            workout.setCompleted(setId, false)
            rest.clear()
        }
    }

    fun onAddSet(exercise: LiveExercise) = launchSafely { workout.addSet(exercise.sessionExerciseId) }

    fun onWarmupChanged(done: Boolean) {
        val id = uiState.value.session?.id ?: return
        launchSafely { workout.setWarmupDone(id, done) }
    }

    fun onNoteSaved(exercise: LiveExercise, note: String) =
        launchSafely { workout.setNote(exercise.sessionExerciseId, note) }

    fun onLighter(exercise: LiveExercise) {
        launchSafely {
            workout.applyLighterLoad(exercise.sessionExerciseId)
            revision.value += 1
            _events.send(LiveEvent.Message("Ciężar −10%. Dobra decyzja — technika przede wszystkim."))
        }
    }

    fun onRestAdjust(deltaSeconds: Int) = launchSafely { rest.adjust(deltaSeconds) }

    fun onRestSkip() = launchSafely { rest.clear() }

    /** Wywoływane przez UI, gdy odliczanie dobiegło końca. Czyści tylko "tę samą" przerwę. */
    fun onRestFinished(endsAtMillis: Long) {
        launchSafely {
            if (uiState.value.timer?.endsAtMillis == endsAtMillis) rest.clear()
        }
    }

    fun onFinish() {
        val session = uiState.value.session ?: return
        if (finishing.value) return
        finishing.value = true
        viewModelScope.launch {
            try {
                val id = workout.finish(session.id)
                rest.clear()
                _events.send(LiveEvent.Finished(id))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                finishing.value = false
                _events.send(LiveEvent.Message("Nie udało się zakończyć treningu. Twoje serie są zapisane — spróbuj ponownie."))
            }
        }
    }

    fun onDiscard() {
        val session = uiState.value.session ?: return
        finishing.value = true
        viewModelScope.launch {
            try {
                rest.clear()
                workout.discard(session.id)
                _events.send(LiveEvent.Discarded)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                finishing.value = false
                _events.send(LiveEvent.Message("Nie udało się odrzucić treningu."))
            }
        }
    }

    private fun placeholderValue(exercise: LiveExercise, set: LiveSet): Int {
        val previous = exercise.previous.getOrNull(set.index) ?: exercise.previous.lastOrNull()
        return when (exercise.exercise.trackingType) {
            TrackingType.TIME -> previous?.durationSeconds ?: exercise.targetMin
            TrackingType.REPS -> previous?.reps ?: exercise.targetMax
        }
    }

    private fun nextUpLabel(session: LiveSession?, exercise: LiveExercise, justDone: LiveSet): String {
        val remainingHere = exercise.sets.firstOrNull { !it.completed && it.id != justDone.id }
        if (remainingHere != null) return "${exercise.exercise.namePl}, seria ${remainingHere.index + 1}"
        val next = session?.exercises?.firstOrNull { ex -> ex.position > exercise.position && ex.sets.any { !it.completed } }
        return next?.exercise?.namePl ?: exercise.exercise.namePl
    }

    private fun launchSafely(block: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _events.send(LiveEvent.Message("Coś poszło nie tak przy zapisie. Spróbuj jeszcze raz."))
            }
        }
    }
}
