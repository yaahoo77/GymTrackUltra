package com.gymtrack.ultra.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.gymtrack.ultra.domain.nutrition.ProteinCalculator
import com.gymtrack.ultra.domain.nutrition.ProteinGoal
import com.gymtrack.ultra.domain.model.ExperienceLevel
import com.gymtrack.ultra.domain.model.TrainingGoal
import com.gymtrack.ultra.domain.model.UserSettings
import com.gymtrack.ultra.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.io.IOException
import java.time.DayOfWeek
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val store: DataStore<Preferences>,
) : SettingsRepository {

    private object Keys {
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val DISPLAY_NAME = stringPreferencesKey("display_name")
        val GOAL = stringPreferencesKey("goal")
        val EXPERIENCE = stringPreferencesKey("experience")
        val TRAINING_DAYS = stringSetPreferencesKey("training_days")
        val REDUCE_MOTION = booleanPreferencesKey("reduce_motion")
        val HAPTICS = booleanPreferencesKey("haptics_enabled")
        val CELEBRATIONS = booleanPreferencesKey("celebrations_enabled")
        val WARMUP_REMINDER = booleanPreferencesKey("warmup_reminder")
        val PROTEIN_TRACKING = booleanPreferencesKey("protein_tracking")
        val BODY_WEIGHT = doublePreferencesKey("body_weight_kg")
        val PROTEIN_GOAL = stringPreferencesKey("protein_goal")
        val MANUAL_PROTEIN = intPreferencesKey("manual_protein_target")
    }

    override val settings: Flow<UserSettings> = store.data
        .catch { error ->
            // Uszkodzony odczyt nie może wysypać aplikacji — wracamy do domyślnych.
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map { it.toSettings() }
        .distinctUntilChanged()

    override suspend fun update(transform: (UserSettings) -> UserSettings) {
        store.edit { prefs ->
            val updated = transform(prefs.toSettings())
            prefs.write(updated)
        }
    }

    private fun Preferences.toSettings(): UserSettings {
        val defaults = UserSettings()
        val days = this[Keys.TRAINING_DAYS]
            ?.mapNotNull { it.toIntOrNull()?.takeIf { v -> v in 1..7 }?.let(DayOfWeek::of) }
            ?.toSet()
            ?.takeIf { it.isNotEmpty() }
            ?: defaults.trainingDays
        return UserSettings(
            onboardingCompleted = this[Keys.ONBOARDING_COMPLETED] ?: defaults.onboardingCompleted,
            displayName = this[Keys.DISPLAY_NAME] ?: defaults.displayName,
            goal = this[Keys.GOAL]?.let { name -> TrainingGoal.entries.firstOrNull { it.name == name } } ?: defaults.goal,
            experience = this[Keys.EXPERIENCE]?.let { name -> ExperienceLevel.entries.firstOrNull { it.name == name } }
                ?: defaults.experience,
            trainingDays = days,
            reduceMotion = this[Keys.REDUCE_MOTION] ?: defaults.reduceMotion,
            hapticsEnabled = this[Keys.HAPTICS] ?: defaults.hapticsEnabled,
            celebrationsEnabled = this[Keys.CELEBRATIONS] ?: defaults.celebrationsEnabled,
            warmupReminder = this[Keys.WARMUP_REMINDER] ?: defaults.warmupReminder,
            proteinTracking = this[Keys.PROTEIN_TRACKING] ?: defaults.proteinTracking,
            bodyWeightKg = this[Keys.BODY_WEIGHT]?.takeIf { ProteinCalculator.isValidWeight(it) },
            proteinGoal = this[Keys.PROTEIN_GOAL]?.let { name -> ProteinGoal.entries.firstOrNull { it.name == name } }
                ?: defaults.proteinGoal,
            manualProteinTarget = this[Keys.MANUAL_PROTEIN]
                ?.takeIf { it in ProteinCalculator.MIN_TARGET..ProteinCalculator.MAX_TARGET },
        )
    }

    private fun MutablePreferences.write(settings: UserSettings) {
        this[Keys.ONBOARDING_COMPLETED] = settings.onboardingCompleted
        this[Keys.DISPLAY_NAME] = settings.displayName.trim().take(MAX_NAME_LENGTH)
        this[Keys.GOAL] = settings.goal.name
        this[Keys.EXPERIENCE] = settings.experience.name
        this[Keys.TRAINING_DAYS] = settings.trainingDays.map { it.value.toString() }.toSet()
        this[Keys.REDUCE_MOTION] = settings.reduceMotion
        this[Keys.HAPTICS] = settings.hapticsEnabled
        this[Keys.CELEBRATIONS] = settings.celebrationsEnabled
        this[Keys.WARMUP_REMINDER] = settings.warmupReminder
        this[Keys.PROTEIN_TRACKING] = settings.proteinTracking
        this[Keys.PROTEIN_GOAL] = settings.proteinGoal.name
        // Waga i cel ręczny są opcjonalne — brak wartości = klucz usunięty, nic nie zostaje w pliku.
        val weight = settings.bodyWeightKg?.takeIf { ProteinCalculator.isValidWeight(it) }
        if (weight != null) this[Keys.BODY_WEIGHT] = weight else remove(Keys.BODY_WEIGHT)
        val manual = settings.manualProteinTarget
            ?.coerceIn(ProteinCalculator.MIN_TARGET, ProteinCalculator.MAX_TARGET)
        if (manual != null) this[Keys.MANUAL_PROTEIN] = manual else remove(Keys.MANUAL_PROTEIN)
    }

    private companion object {
        const val MAX_NAME_LENGTH = 24
    }
}
