package com.gymtrack.ultra.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.gymtrack.ultra.database.entity.PlanDayEntity
import com.gymtrack.ultra.database.entity.PlanExerciseEntity
import com.gymtrack.ultra.database.relation.PlanDayWithExercises
import kotlinx.coroutines.flow.Flow

@Dao
interface PlanDao {

    @Transaction
    @Query("SELECT * FROM plan_days ORDER BY position")
    fun observePlanDays(): Flow<List<PlanDayWithExercises>>

    @Transaction
    @Query("SELECT * FROM plan_days WHERE id = :id")
    fun observePlanDay(id: Long): Flow<PlanDayWithExercises?>

    @Transaction
    @Query("SELECT * FROM plan_days WHERE id = :id")
    suspend fun getPlanDay(id: Long): PlanDayWithExercises?

    @Transaction
    @Query("SELECT * FROM plan_days ORDER BY position")
    suspend fun getPlanDays(): List<PlanDayWithExercises>

    @Query("DELETE FROM plan_exercises WHERE planDayId = :dayId")
    suspend fun deleteExercisesForDay(dayId: Long)

    @Query("SELECT COUNT(*) FROM plan_days")
    suspend fun countDays(): Int

    @Query("SELECT COUNT(*) FROM plan_days")
    fun observeDayCount(): Flow<Int>

    @Insert
    suspend fun insertDay(day: PlanDayEntity): Long

    @Insert
    suspend fun insertExercises(items: List<PlanExerciseEntity>)

    @Update
    suspend fun updateExercise(item: PlanExerciseEntity)

    @Query("UPDATE plan_days SET dayOfWeek = :dayOfWeek WHERE id = :id")
    suspend fun updateDayOfWeek(id: Long, dayOfWeek: Int)

    /** Liczba różnych ćwiczeń w całym planie — cel osiągnięcia TECHNIQUE MASTER. */
    @Query("SELECT COUNT(DISTINCT exerciseId) FROM plan_exercises")
    fun observeDistinctExerciseCount(): Flow<Int>

    @Query("SELECT COUNT(DISTINCT exerciseId) FROM plan_exercises")
    suspend fun getDistinctExerciseCount(): Int
}
