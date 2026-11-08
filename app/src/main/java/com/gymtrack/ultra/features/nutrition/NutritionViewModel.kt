package com.gymtrack.ultra.features.nutrition

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymtrack.ultra.core.format.Formatters
import com.gymtrack.ultra.core.time.TimeProvider
import com.gymtrack.ultra.domain.nutrition.FoodEntry
import com.gymtrack.ultra.domain.nutrition.FoodPreset
import com.gymtrack.ultra.domain.nutrition.ProteinCalculator
import com.gymtrack.ultra.domain.nutrition.ProteinGoal
import com.gymtrack.ultra.domain.repository.NutritionRepository
import com.gymtrack.ultra.domain.repository.SettingsRepository
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
import kotlin.math.roundToInt

sealed interface NutritionEvent {
    data class Added(val entryId: Long, val text: String) : NutritionEvent
    data class Message(val text: String) : NutritionEvent
}

@HiltViewModel
class NutritionViewModel @Inject constructor(
    private val nutrition: NutritionRepository,
    private val settings: SettingsRepository,
    private val time: TimeProvider,
) : ViewModel() {

    val uiState: StateFlow<ProteinUiState> = combine(
        settings.settings,
        nutrition.observeEntries(7),
        nutrition.observeRecent(40),
    ) { userSettings, entries, recent ->
        ProteinStateFactory.build(userSettings, entries, recent, time.today())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProteinUiState())

    private val _events = Channel<NutritionEvent>(Channel.BUFFERED)
    val events: Flow<NutritionEvent> = _events.receiveAsFlow()

    fun addPreset(preset: FoodPreset) = add(preset.name, preset.proteinGrams)

    fun addCustom(name: String, gramsText: String): Boolean {
        val grams = gramsText.trim().toIntOrNull()
        if (grams == null || grams !in 1..ProteinCalculator.MAX_ENTRY_GRAMS) {
            launchSafely { _events.send(NutritionEvent.Message("Wpisz od 1 do ${ProteinCalculator.MAX_ENTRY_GRAMS} g białka.")) }
            return false
        }
        add(name, grams)
        return true
    }

    fun add(name: String, grams: Int) = launchSafely {
        val id = nutrition.add(name, grams)
        _events.send(NutritionEvent.Added(id, "+$grams g · ${name.trim().ifBlank { "Posiłek" }}"))
    }

    fun undo(entryId: Long) = launchSafely { nutrition.delete(entryId) }

    fun delete(entry: FoodEntry) = launchSafely { nutrition.delete(entry.id) }

    /** Zapis celu z wagi. Zwraca false, gdy waga jest poza rozsądnym zakresem. */
    fun saveFromWeight(weightText: String, goal: ProteinGoal): Boolean {
        val weight = parseWeight(weightText)
        if (!ProteinCalculator.isValidWeight(weight)) {
            launchSafely { _events.send(NutritionEvent.Message("Podaj wagę od 30 do 200 kg.")) }
            return false
        }
        launchSafely {
            settings.update { it.copy(bodyWeightKg = weight, proteinGoal = goal, manualProteinTarget = null, proteinTracking = true) }
        }
        return true
    }

    /** Cel wpisany ręcznie — bez podawania wagi. */
    fun saveManual(targetText: String): Boolean {
        val target = targetText.trim().toIntOrNull()
        if (target == null || target !in ProteinCalculator.MIN_TARGET..ProteinCalculator.MAX_TARGET) {
            launchSafely {
                _events.send(NutritionEvent.Message("Cel od ${ProteinCalculator.MIN_TARGET} do ${ProteinCalculator.MAX_TARGET} g."))
            }
            return false
        }
        launchSafely { settings.update { it.copy(manualProteinTarget = target, bodyWeightKg = null, proteinTracking = true) } }
        return true
    }

    /** Usuwa wagę i cel — nic nie zostaje w ustawieniach. Wpisy jedzenia zostają. */
    fun clearGoal() = launchSafely { settings.update { it.copy(bodyWeightKg = null, manualProteinTarget = null) } }

    fun preview(weightText: String, goal: ProteinGoal): Int? =
        parseWeight(weightText)?.takeIf { ProteinCalculator.isValidWeight(it) }?.let { ProteinCalculator.dailyTarget(it, goal) }

    fun formatWeight(kg: Double): String = Formatters.weightKg(kg)

    fun setEnabled(enabled: Boolean) = launchSafely { settings.update { it.copy(proteinTracking = enabled) } }

    private fun parseWeight(text: String): Double? = text.trim().replace(',', '.').toDoubleOrNull()
        ?.let { (it * 10).roundToInt() / 10.0 }

    private fun launchSafely(block: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _events.send(NutritionEvent.Message("Nie udało się zapisać. Spróbuj jeszcze raz."))
            }
        }
    }
}
