package com.gymtrack.ultra.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** Biblioteka ćwiczeń. Teksty techniki i bezpieczeństwa: kroki rozdzielone znakiem nowej linii. */
@Entity(tableName = "exercises")
data class ExerciseEntity(
    @PrimaryKey val id: String,
    val namePl: String,
    val nameEn: String,
    val muscleGroup: String,
    /** "REPS" albo "TIME". */
    val trackingType: String,
    val sideNote: String?,
    val techniqueSteps: String,
    val safetyTips: String,
    val isCustom: Boolean = false,
)

@Entity(tableName = "plan_days", indices = [Index("position")])
data class PlanDayEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val code: String,
    val title: String,
    /** ISO: 1 = poniedziałek … 7 = niedziela. */
    val dayOfWeek: Int,
    val position: Int,
)

@Entity(
    tableName = "plan_exercises",
    foreignKeys = [
        ForeignKey(
            entity = PlanDayEntity::class,
            parentColumns = ["id"],
            childColumns = ["planDayId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index("planDayId"), Index("exerciseId")],
)
data class PlanExerciseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val planDayId: Long,
    val exerciseId: String,
    val position: Int,
    val targetSets: Int,
    val targetMin: Int,
    val targetMax: Int,
    val restSeconds: Int,
)

/**
 * Sesja treningowa. Status ACTIVE pozwala odtworzyć przerwany trening po zabiciu procesu.
 * [planDayId] może zniknąć (SET_NULL), ale historia zostaje dzięki [title] i kopii ćwiczeń.
 */
@Entity(
    tableName = "workout_sessions",
    foreignKeys = [
        ForeignKey(
            entity = PlanDayEntity::class,
            parentColumns = ["id"],
            childColumns = ["planDayId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("planDayId"), Index("status"), Index("startedAt")],
)
data class WorkoutSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val planDayId: Long?,
    val title: String,
    val startedAt: Long,
    val endedAt: Long?,
    /** ACTIVE / COMPLETED / ABANDONED */
    val status: String,
    val warmupDone: Boolean,
    val xpEarned: Int,
    val updatedAt: Long,
)

/** Kopia celu ćwiczenia w momencie startu — edycja planu nie psuje historii. */
@Entity(
    tableName = "session_exercises",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index("sessionId"), Index("exerciseId")],
)
data class SessionExerciseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val exerciseId: String,
    val position: Int,
    val targetSets: Int,
    val targetMin: Int,
    val targetMax: Int,
    val restSeconds: Int,
    val note: String? = null,
)

@Entity(
    tableName = "set_logs",
    foreignKeys = [
        ForeignKey(
            entity = SessionExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionExerciseId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["sessionExerciseId", "setIndex"], unique = true)],
)
data class SetLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionExerciseId: Long,
    val setIndex: Int,
    val weightKg: Double?,
    val reps: Int?,
    val durationSeconds: Int?,
    val completed: Boolean,
    val completedAt: Long?,
)

/** Księga XP: każde przyznanie to osobny wpis — łatwo audytować i nie da się "nabić" podwójnie. */
@Entity(tableName = "xp_events", indices = [Index("sessionId"), Index("epochDay"), Index("weekStartEpochDay")])
data class XpEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long?,
    val amount: Int,
    /** Nazwa z [com.gymtrack.ultra.domain.progression.XpReason]. */
    val reason: String,
    val epochDay: Long,
    val weekStartEpochDay: Long,
    val createdAt: Long,
)

@Entity(tableName = "achievement_unlocks")
data class AchievementUnlockEntity(
    @PrimaryKey val code: String,
    val unlockedAt: Long,
)

@Entity(tableName = "technique_views")
data class TechniqueViewEntity(
    @PrimaryKey val exerciseId: String,
    val firstViewedAt: Long,
)

/** Dziennik białka: jeden wpis = jeden produkt albo posiłek. Tylko gramy białka — bez kalorii. */
@Entity(tableName = "food_entries", indices = [Index("epochDay")])
data class FoodEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val epochDay: Long,
    val name: String,
    val proteinGrams: Int,
    val createdAt: Long,
)
