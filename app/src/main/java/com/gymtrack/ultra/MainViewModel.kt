package com.gymtrack.ultra

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymtrack.ultra.data.seed.DatabaseSeeder
import com.gymtrack.ultra.domain.model.UserSettings
import com.gymtrack.ultra.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface AppState {
    data object Loading : AppState
    data class Ready(val settings: UserSettings, val startupError: String?) : AppState
}

/** Start aplikacji: przygotowanie bazy (plan, ćwiczenia) + odczyt ustawień. */
@HiltViewModel
class MainViewModel @Inject constructor(
    private val seeder: DatabaseSeeder,
    settingsRepository: SettingsRepository,
) : ViewModel() {

    private val seeded = MutableStateFlow(false)
    private val startupError = MutableStateFlow<String?>(null)

    init {
        viewModelScope.launch {
            try {
                seeder.seedIfNeeded()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Aplikacja i tak startuje — historia użytkownika jest nienaruszona.
                Log.e(TAG, "Database seed failed", e)
                startupError.value = "Nie udało się przygotować planu. Uruchom aplikację ponownie."
            }
            seeded.value = true
        }
    }

    val state: StateFlow<AppState> = combine(
        seeded,
        startupError,
        settingsRepository.settings,
    ) { isSeeded, error, settings ->
        if (isSeeded) AppState.Ready(settings, error) else AppState.Loading
    }.stateIn(viewModelScope, SharingStarted.Eagerly, AppState.Loading)

    private companion object {
        const val TAG = "MainViewModel"
    }
}
