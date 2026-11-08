package com.gymtrack.ultra.domain.repository

import com.gymtrack.ultra.domain.nutrition.FoodEntry
import com.gymtrack.ultra.domain.model.LiveSession
import com.gymtrack.ultra.domain.model.PlanDay
import com.gymtrack.ultra.domain.model.ProgressSnapshot
import com.gymtrack.ultra.domain.model.RestTimer
import com.gymtrack.ultra.domain.model.UserSettings
import com.gymtrack.ultra.domain.model.WorkoutResult
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

/** Trening na żywo: każda zmiana zapisuje się od razu — przerwany trening da się przywrócić. */
interface WorkoutRepository {
    fun observeActiveSession(): Flow<LiveSession?>
    suspend fun startOrResume(planDayId: Long): Long
    suspend fun updateSetValues(setId: Long, weightKg: Double?, reps: Int?, durationSeconds: Int?)
    suspend fun setCompleted(setId: Long, completed: Boolean)
    suspend fun addSet(sessionExerciseId: Long)
    suspend fun setWarmupDone(sessionId: Long, done: Boolean)
    suspend fun setNote(sessionExerciseId: Long, note: String?)

    /** −10% ciężaru w niezaliczonych seriach; odblokowuje SMART LOAD. */
    suspend fun applyLighterLoad(sessionExerciseId: Long)

    /** Zamyka trening, przyznaje XP, wyzwania i osiągnięcia. Zwraca id sesji do podsumowania. */
    suspend fun finish(sessionId: Long): Long
    suspend fun discard(sessionId: Long)
    suspend fun loadResult(sessionId: Long): WorkoutResult?
}

interface RestTimerRepository {
    val timer: Flow<RestTimer?>
    suspend fun start(seconds: Int, label: String)
    suspend fun adjust(deltaSeconds: Int)
    suspend fun clear()
}

/** Ustawienia użytkownika (DataStore). */
/** Dziennik białka. Wpisy trzymane w Room, tylko na telefonie. */
interface NutritionRepository {
    /** Wpisy z ostatnich [days] dni (licząc dzisiaj), najnowsze pierwsze. */
    fun observeEntries(days: Int): Flow<List<FoodEntry>>

    /** Ostatnie wpisy niezależnie od daty — do podpowiedzi "dodaj jeszcze raz". */
    fun observeRecent(limit: Int): Flow<List<FoodEntry>>

    /** Zwraca id wpisu — do cofnięcia. */
    suspend fun add(name: String, proteinGrams: Int): Long

    suspend fun delete(id: Long)
}

interface SettingsRepository {
    val settings: Flow<UserSettings>
    suspend fun update(transform: (UserSettings) -> UserSettings)
}
