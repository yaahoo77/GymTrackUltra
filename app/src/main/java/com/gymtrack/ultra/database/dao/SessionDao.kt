package com.gymtrack.ultra.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.gymtrack.ultra.database.entity.SessionExerciseEntity
import com.gymtrack.ultra.database.entity.SetLogEntity
import com.gymtrack.ultra.database.entity.WorkoutSessionEntity
import com.gymtrack.ultra.database.relation.CompletedSetRow
import com.gymtrack.ultra.database.relation.PlannedSetsRow
import com.gymtrack.ultra.database.relation.SessionWithExercises
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {

    // ── Historia ────────────────────────────────────────────────────────

    @Query("SELECT * FROM workout_sessions WHERE status = 'COMPLETED' ORDER BY startedAt DESC")
    fun observeCompleted(): Flow<List<WorkoutSessionEntity>>

    @Query("SELECT * FROM workout_sessions WHERE status = 'COMPLETED' ORDER BY startedAt DESC")
    suspend fun getCompleted(): List<WorkoutSessionEntity>

    @Query("SELECT COUNT(*) FROM workout_sessions WHERE status = 'COMPLETED' AND startedAt >= :fromMillis")
    suspend fun countCompletedSince(fromMillis: Long): Int

    /** Wszystkie zaliczone serie, także w trwającej sesji (FIRST REP odblokowuje się od razu). */
    @Query("SELECT COUNT(*) FROM set_logs WHERE completed = 1")
    fun observeCompletedSetCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM set_logs WHERE completed = 1")
    suspend fun getCompletedSetCount(): Int

    @Query(
        """
        SELECT COUNT(*) FROM set_logs s
        INNER JOIN session_exercises se ON s.sessionExerciseId = se.id
        INNER JOIN workout_sessions w ON se.sessionId = w.id
        WHERE s.completed = 1 AND w.status = 'COMPLETED' AND w.startedAt >= :fromMillis
        """,
    )
    fun observeCompletedSetsInFinishedSessionsSince(fromMillis: Long): Flow<Int>

    @Query(COMPLETED_SET_ROWS)
    fun observeCompletedSetRows(): Flow<List<CompletedSetRow>>

    @Query(COMPLETED_SET_ROWS)
    suspend fun getCompletedSetRows(): List<CompletedSetRow>

    @Query(PLANNED_SETS)
    fun observePlannedSetsPerSession(): Flow<List<PlannedSetsRow>>

    @Query(PLANNED_SETS)
    suspend fun getPlannedSetsPerSession(): List<PlannedSetsRow>

    /** Serie z ostatniego ukończonego treningu, w którym było to ćwiczenie. */
    @Query(
        """
        SELECT * FROM set_logs WHERE completed = 1 AND sessionExerciseId = (
            SELECT se.id FROM session_exercises se
            INNER JOIN workout_sessions w ON w.id = se.sessionId
            WHERE se.exerciseId = :exerciseId AND w.status = 'COMPLETED'
            ORDER BY w.startedAt DESC LIMIT 1
        ) ORDER BY setIndex
        """,
    )
    suspend fun lastCompletedSetsFor(exerciseId: String): List<SetLogEntity>

    // ── Trening na żywo ─────────────────────────────────────────────────

    @Transaction
    @Query("SELECT * FROM workout_sessions WHERE status = 'ACTIVE' ORDER BY startedAt DESC LIMIT 1")
    fun observeActiveSessionDetail(): Flow<SessionWithExercises?>

    @Query("SELECT * FROM workout_sessions WHERE status = 'ACTIVE' ORDER BY startedAt DESC LIMIT 1")
    suspend fun getActiveSession(): WorkoutSessionEntity?

    @Transaction
    @Query("SELECT * FROM workout_sessions WHERE id = :id")
    suspend fun getSessionDetail(id: Long): SessionWithExercises?

    @Query("SELECT * FROM workout_sessions WHERE id = :id")
    suspend fun getSession(id: Long): WorkoutSessionEntity?

    @Query("SELECT * FROM session_exercises WHERE id = :id")
    suspend fun getSessionExercise(id: Long): SessionExerciseEntity?

    @Query("UPDATE session_exercises SET exerciseId = :exerciseId WHERE id = :id")
    suspend fun updateSessionExerciseExercise(id: Long, exerciseId: String)

    @Query("SELECT * FROM set_logs WHERE id = :id")
    suspend fun getSet(id: Long): SetLogEntity?

    @Query("SELECT * FROM set_logs WHERE sessionExerciseId = :sessionExerciseId ORDER BY setIndex")
    suspend fun getSets(sessionExerciseId: Long): List<SetLogEntity>

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

    @Update
    suspend fun updateSets(items: List<SetLogEntity>)

    @Query("DELETE FROM set_logs WHERE sessionExerciseId IN (SELECT id FROM session_exercises WHERE sessionId = :sessionId)")
    suspend fun deleteSetsOfSession(sessionId: Long)

    @Query("DELETE FROM session_exercises WHERE sessionId = :sessionId")
    suspend fun deleteExercisesOfSession(sessionId: Long)

    @Query("DELETE FROM workout_sessions WHERE id = :sessionId")
    suspend fun deleteSession(sessionId: Long)
}

private const val COMPLETED_SET_ROWS = """
    SELECT w.id AS sessionId, w.startedAt AS startedAt, w.planDayId AS planDayId,
           se.exerciseId AS exerciseId, e.trackingType AS trackingType, s.setIndex AS setIndex,
           se.targetSets AS targetSets, se.targetMin AS targetMin, se.targetMax AS targetMax,
           s.weightKg AS weightKg, s.reps AS reps, s.durationSeconds AS durationSeconds
    FROM set_logs s
    INNER JOIN session_exercises se ON s.sessionExerciseId = se.id
    INNER JOIN workout_sessions w ON se.sessionId = w.id
    INNER JOIN exercises e ON e.id = se.exerciseId
    WHERE s.completed = 1 AND w.status = 'COMPLETED'
    ORDER BY w.startedAt, se.position, s.setIndex
"""

private const val PLANNED_SETS = """
    SELECT se.sessionId AS sessionId, SUM(se.targetSets) AS plannedSets
    FROM session_exercises se
    INNER JOIN workout_sessions w ON w.id = se.sessionId
    WHERE w.status = 'COMPLETED'
    GROUP BY se.sessionId
"""
