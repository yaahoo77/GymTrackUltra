package com.gymtrack.ultra.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.gymtrack.ultra.database.entity.SessionExerciseEntity
import com.gymtrack.ultra.database.entity.SetLogEntity
import com.gymtrack.ultra.database.entity.WorkoutSessionEntity
import com.gymtrack.ultra.database.relation.SessionWithExercises
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {

    // ── Odczyt ──────────────────────────────────────────────────────────

    @Query("SELECT * FROM workout_sessions WHERE status = 'COMPLETED' ORDER BY startedAt DESC")
    fun observeCompleted(): Flow<List<WorkoutSessionEntity>>

    /** Aktywna (przerwana) sesja — do przywrócenia po restarcie aplikacji. */
    @Query("SELECT * FROM workout_sessions WHERE status = 'ACTIVE' ORDER BY startedAt DESC LIMIT 1")
    fun observeActive(): Flow<WorkoutSessionEntity?>

    @Transaction
    @Query("SELECT * FROM workout_sessions WHERE id = :id")
    fun observeSessionDetail(id: Long): Flow<SessionWithExercises?>

    @Transaction
    @Query("SELECT * FROM workout_sessions WHERE id = :id")
    suspend fun getSessionDetail(id: Long): SessionWithExercises?

    /** Wszystkie zaliczone serie, także w trwającej sesji (FIRST REP odblokowuje się od razu). */
    @Query("SELECT COUNT(*) FROM set_logs WHERE completed = 1")
    fun observeCompletedSetCount(): Flow<Int>

    @Query(
        """
        SELECT COUNT(*) FROM set_logs s
        INNER JOIN session_exercises se ON s.sessionExerciseId = se.id
        INNER JOIN workout_sessions w ON se.sessionId = w.id
        WHERE s.completed = 1 AND w.status = 'COMPLETED' AND w.startedAt >= :fromMillis
        """,
    )
    fun observeCompletedSetsInFinishedSessionsSince(fromMillis: Long): Flow<Int>

    // ── Zapis (używany od Etapu 5: Live Workout) ────────────────────────

    @Insert
    suspend fun insertSession(session: WorkoutSessionEntity): Long

    @Update
    suspend fun updateSession(session: WorkoutSessionEntity)

    @Insert
    suspend fun insertSessionExercises(items: List<SessionExerciseEntity>): List<Long>

    @Update
    suspend fun updateSessionExercise(item: SessionExerciseEntity)

    @Insert
    suspend fun insertSets(items: List<SetLogEntity>): List<Long>

    @Update
    suspend fun updateSet(item: SetLogEntity)
}
