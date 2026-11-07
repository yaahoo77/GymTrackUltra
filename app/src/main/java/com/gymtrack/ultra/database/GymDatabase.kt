package com.gymtrack.ultra.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.gymtrack.ultra.database.dao.ExerciseDao
import com.gymtrack.ultra.database.dao.PlanDao
import com.gymtrack.ultra.database.dao.ProgressDao
import com.gymtrack.ultra.database.dao.SessionDao
import com.gymtrack.ultra.database.entity.AchievementUnlockEntity
import com.gymtrack.ultra.database.entity.ExerciseEntity
import com.gymtrack.ultra.database.entity.PlanDayEntity
import com.gymtrack.ultra.database.entity.PlanExerciseEntity
import com.gymtrack.ultra.database.entity.SessionExerciseEntity
import com.gymtrack.ultra.database.entity.SetLogEntity
import com.gymtrack.ultra.database.entity.TechniqueViewEntity
import com.gymtrack.ultra.database.entity.WorkoutSessionEntity
import com.gymtrack.ultra.database.entity.XpEventEntity

/**
 * Wersja 1. Każda zmiana schematu = nowa wersja + jawna migracja.
 * Celowo BEZ fallbackToDestructiveMigration — dane treningowe nie mogą zniknąć.
 */
@Database(
    entities = [
        ExerciseEntity::class,
        PlanDayEntity::class,
        PlanExerciseEntity::class,
        WorkoutSessionEntity::class,
        SessionExerciseEntity::class,
        SetLogEntity::class,
        XpEventEntity::class,
        AchievementUnlockEntity::class,
        TechniqueViewEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class GymDatabase : RoomDatabase() {
    abstract fun exerciseDao(): ExerciseDao
    abstract fun planDao(): PlanDao
    abstract fun sessionDao(): SessionDao
    abstract fun progressDao(): ProgressDao

    companion object {
        const val NAME = "gymtrack_ultra.db"
    }
}
