package com.gymtrack.ultra.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.gymtrack.ultra.database.entity.FoodEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FoodDao {

    @Query("SELECT * FROM food_entries WHERE epochDay >= :fromEpochDay ORDER BY createdAt DESC")
    fun observeSince(fromEpochDay: Long): Flow<List<FoodEntryEntity>>

    @Query("SELECT * FROM food_entries ORDER BY createdAt DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<FoodEntryEntity>>

    @Insert
    suspend fun insert(entry: FoodEntryEntity): Long

    @Query("DELETE FROM food_entries WHERE id = :id")
    suspend fun delete(id: Long)
}
