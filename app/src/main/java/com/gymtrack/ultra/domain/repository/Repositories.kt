package com.gymtrack.ultra.domain.repository

import com.gymtrack.ultra.domain.model.PlanDay
import com.gymtrack.ultra.domain.model.ProgressSnapshot
import com.gymtrack.ultra.domain.model.UserSettings
import kotlinx.coroutines.flow.Flow
import java.time.Instant

/** Plan treningowy i biblioteka ćwiczeń. */
interface PlanRepository {
    fun observePlan(): Flow<List<PlanDay>>
    fun observePlanDay(id: Long): Flow<PlanDay?>
    suspend fun markTechniqueViewed(exerciseId: String)
}

/** XP, poziomy, historia ukończonych sesji, osiągnięcia. */
interface ProgressRepository {
    fun observeProgress(): Flow<ProgressSnapshot>

    /** Zaliczone serie w ukończonych treningach od [from] (null = cały okres). */
    fun observeCompletedSetsSince(from: Instant?): Flow<Int>
}

/** Ustawienia użytkownika (DataStore). */
interface SettingsRepository {
    val settings: Flow<UserSettings>
    suspend fun update(transform: (UserSettings) -> UserSettings)
}
