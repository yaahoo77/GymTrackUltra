package com.gymtrack.ultra.core.di

import com.gymtrack.ultra.core.time.SystemTimeProvider
import com.gymtrack.ultra.core.time.TimeProvider
import com.gymtrack.ultra.data.repository.PlanRepositoryImpl
import com.gymtrack.ultra.data.repository.ProgressRepositoryImpl
import com.gymtrack.ultra.data.settings.SettingsRepositoryImpl
import com.gymtrack.ultra.domain.repository.PlanRepository
import com.gymtrack.ultra.domain.repository.ProgressRepository
import com.gymtrack.ultra.domain.repository.SettingsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class BindingsModule {

    @Binds
    @Singleton
    abstract fun bindPlanRepository(impl: PlanRepositoryImpl): PlanRepository

    @Binds
    @Singleton
    abstract fun bindProgressRepository(impl: ProgressRepositoryImpl): ProgressRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository

    @Binds
    @Singleton
    abstract fun bindTimeProvider(impl: SystemTimeProvider): TimeProvider
}
