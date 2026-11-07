package com.gymtrack.ultra.core.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import com.gymtrack.ultra.database.GymDatabase
import com.gymtrack.ultra.database.dao.ExerciseDao
import com.gymtrack.ultra.database.dao.PlanDao
import com.gymtrack.ultra.database.dao.ProgressDao
import com.gymtrack.ultra.database.dao.SessionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): GymDatabase =
        Room.databaseBuilder(context, GymDatabase::class.java, GymDatabase.NAME)
            .build()

    @Provides
    fun provideExerciseDao(db: GymDatabase): ExerciseDao = db.exerciseDao()

    @Provides
    fun providePlanDao(db: GymDatabase): PlanDao = db.planDao()

    @Provides
    fun provideSessionDao(db: GymDatabase): SessionDao = db.sessionDao()

    @Provides
    fun provideProgressDao(db: GymDatabase): ProgressDao = db.progressDao()

    @Provides
    @Singleton
    fun provideSettingsStore(@ApplicationContext context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
            produceFile = { context.preferencesDataStoreFile(SETTINGS_FILE) },
        )
}

private const val SETTINGS_FILE = "gymtrack_settings"
