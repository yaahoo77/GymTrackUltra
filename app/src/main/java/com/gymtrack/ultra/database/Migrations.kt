package com.gymtrack.ultra.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * 1 → 2: dziennik białka (tabela food_entries). Istniejące dane nietknięte.
 * SQL odpowiada dokładnie temu, co Room generuje dla [com.gymtrack.ultra.database.entity.FoodEntryEntity]
 * (Room sprawdza zgodność schematu przy otwarciu bazy).
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `food_entries` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`epochDay` INTEGER NOT NULL, " +
                "`name` TEXT NOT NULL, " +
                "`proteinGrams` INTEGER NOT NULL, " +
                "`createdAt` INTEGER NOT NULL)",
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_food_entries_epochDay` ON `food_entries` (`epochDay`)")
    }
}

val ALL_MIGRATIONS = arrayOf(MIGRATION_1_2)
