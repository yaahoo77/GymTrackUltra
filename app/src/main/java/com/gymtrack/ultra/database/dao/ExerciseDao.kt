package com.gymtrack.ultra.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.gymtrack.ultra.database.entity.ExerciseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseDao {

    /** Upsert: poprawki w treściach techniki trafiają do istniejących instalacji bez kasowania historii. */
    @Upsert
    suspend fun upsertAll(items: List<ExerciseEntity>)

    @Query("SELECT COUNT(*) FROM exercises")
    suspend fun count(): Int

    @Query("SELECT * FROM exercises ORDER BY namePl")
    fun observeAll(): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercises WHERE id = :id")
    suspend fun get(id: String): ExerciseEntity?
}
