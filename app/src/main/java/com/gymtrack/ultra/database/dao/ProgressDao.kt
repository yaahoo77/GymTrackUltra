package com.gymtrack.ultra.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.gymtrack.ultra.database.entity.AchievementUnlockEntity
import com.gymtrack.ultra.database.entity.TechniqueViewEntity
import com.gymtrack.ultra.database.entity.XpEventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProgressDao {

    @Query("SELECT COALESCE(SUM(amount), 0) FROM xp_events")
    fun observeTotalXp(): Flow<Int>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM xp_events WHERE createdAt < :beforeMillis")
    suspend fun sumXpBefore(beforeMillis: Long): Int

    @Query("SELECT * FROM xp_events WHERE sessionId = :sessionId ORDER BY id")
    suspend fun getXpForSession(sessionId: Long): List<XpEventEntity>

    @Insert
    suspend fun insertXpEvents(events: List<XpEventEntity>)

    /** Czy dziś przyznano już XP za trening (limit: raz dziennie). */
    @Query("SELECT COUNT(*) FROM xp_events WHERE epochDay = :epochDay AND reason = :reason")
    suspend fun countXpOnDay(epochDay: Long, reason: String): Int

    /** Czy w tym tygodniu przyznano już premię za cel tygodnia. */
    @Query("SELECT COUNT(*) FROM xp_events WHERE weekStartEpochDay = :weekStartEpochDay AND reason = :reason")
    suspend fun countXpInWeek(weekStartEpochDay: Long, reason: String): Int

    @Query("SELECT * FROM achievement_unlocks")
    fun observeUnlocks(): Flow<List<AchievementUnlockEntity>>

    @Query("SELECT * FROM achievement_unlocks")
    suspend fun getUnlocks(): List<AchievementUnlockEntity>

    @Query("SELECT * FROM achievement_unlocks WHERE unlockedAt = :atMillis")
    suspend fun getUnlocksAt(atMillis: Long): List<AchievementUnlockEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertUnlocks(unlocks: List<AchievementUnlockEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTechniqueView(view: TechniqueViewEntity)

    /** Ile ćwiczeń Z PLANU ma przeczytaną technikę. */
    @Query(
        """
        SELECT COUNT(*) FROM technique_views
        WHERE exerciseId IN (SELECT DISTINCT exerciseId FROM plan_exercises)
        """,
    )
    fun observeTechniqueViewedInPlan(): Flow<Int>

    @Query(
        """
        SELECT COUNT(*) FROM technique_views
        WHERE exerciseId IN (SELECT DISTINCT exerciseId FROM plan_exercises)
        """,
    )
    suspend fun getTechniqueViewedInPlan(): Int
}
