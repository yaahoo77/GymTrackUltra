package com.gymtrack.ultra.data.repository

import com.gymtrack.ultra.core.time.TimeProvider
import com.gymtrack.ultra.database.dao.FoodDao
import com.gymtrack.ultra.database.entity.FoodEntryEntity
import com.gymtrack.ultra.domain.nutrition.FoodEntry
import com.gymtrack.ultra.domain.nutrition.ProteinCalculator
import com.gymtrack.ultra.domain.repository.NutritionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NutritionRepositoryImpl @Inject constructor(
    private val dao: FoodDao,
    private val time: TimeProvider,
) : NutritionRepository {

    override fun observeEntries(days: Int): Flow<List<FoodEntry>> {
        val from = time.today().minusDays((days - 1).coerceAtLeast(0).toLong()).toEpochDay()
        return dao.observeSince(from).map { list -> list.map { it.toDomain() } }
    }

    override fun observeRecent(limit: Int): Flow<List<FoodEntry>> =
        dao.observeRecent(limit).map { list -> list.map { it.toDomain() } }

    override suspend fun add(name: String, proteinGrams: Int): Long {
        val grams = proteinGrams.coerceIn(1, ProteinCalculator.MAX_ENTRY_GRAMS)
        val cleanName = name.trim().take(MAX_NAME).ifBlank { "Posiłek" }
        return dao.insert(
            FoodEntryEntity(
                epochDay = time.today().toEpochDay(),
                name = cleanName,
                proteinGrams = grams,
                createdAt = time.now().toEpochMilli(),
            ),
        )
    }

    override suspend fun delete(id: Long) = dao.delete(id)

    private fun FoodEntryEntity.toDomain() = FoodEntry(
        id = id,
        date = LocalDate.ofEpochDay(epochDay),
        name = name,
        proteinGrams = proteinGrams,
        createdAt = Instant.ofEpochMilli(createdAt),
    )

    private companion object {
        const val MAX_NAME = 40
    }
}
