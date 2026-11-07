package com.gymtrack.ultra.data.repository

import com.gymtrack.ultra.core.time.TimeProvider
import com.gymtrack.ultra.data.mapper.toDomain
import com.gymtrack.ultra.database.dao.PlanDao
import com.gymtrack.ultra.database.dao.ProgressDao
import com.gymtrack.ultra.database.entity.TechniqueViewEntity
import com.gymtrack.ultra.domain.model.PlanDay
import com.gymtrack.ultra.domain.repository.PlanRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlanRepositoryImpl @Inject constructor(
    private val planDao: PlanDao,
    private val progressDao: ProgressDao,
    private val time: TimeProvider,
) : PlanRepository {

    override fun observePlan(): Flow<List<PlanDay>> =
        planDao.observePlanDays()
            .map { days -> days.map { it.toDomain() } }
            .distinctUntilChanged()

    override fun observePlanDay(id: Long): Flow<PlanDay?> =
        planDao.observePlanDay(id)
            .map { it?.toDomain() }
            .distinctUntilChanged()

    override suspend fun markTechniqueViewed(exerciseId: String) {
        progressDao.insertTechniqueView(
            TechniqueViewEntity(exerciseId = exerciseId, firstViewedAt = time.now().toEpochMilli()),
        )
    }
}
