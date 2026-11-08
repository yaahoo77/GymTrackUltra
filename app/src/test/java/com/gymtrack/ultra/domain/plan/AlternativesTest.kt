package com.gymtrack.ultra.domain.plan

import com.gymtrack.ultra.data.seed.DefaultContent
import com.gymtrack.ultra.domain.model.Exercise
import com.gymtrack.ultra.domain.model.TrackingType
import com.gymtrack.ultra.domain.progression.MuscleGroup
import com.gymtrack.ultra.domain.progression.MuscleMap
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AlternativesTest {

    private val library = DefaultContent.exercises.map {
        Exercise(it.id, it.namePl, it.nameEn, it.muscleGroup, if (it.timed) TrackingType.TIME else TrackingType.REPS, it.sideNote, it.technique, it.safety)
    }

    @Test
    fun `curated alternatives come first and unusual ones are never suggested`() {
        val alt = ExerciseAlternatives.forExercise("chest_press_machine", library).map { it.id }
        assertEquals(listOf("db_bench_press", "incline_db_press"), alt.take(2))
        DefaultContent.plan.flatMap { it.items }.forEach { item ->
            val ids = ExerciseAlternatives.forExercise(item.exerciseId, library).map { it.id }
            assertFalse(ids.any { it in ExerciseAlternatives.unusual })
            assertFalse(item.exerciseId in ids)
            assertTrue("${item.exerciseId} ma zamiennik", ids.isNotEmpty())
        }
    }

    @Test
    fun `custom exercise id carries its muscle`() {
        val id = CustomExercises.newId(MuscleGroup.BACK, 123L)
        assertTrue(CustomExercises.isCustom(id))
        assertEquals(MuscleGroup.BACK, CustomExercises.muscleOf(id))
        assertEquals(setOf(MuscleGroup.BACK), MuscleMap.targetsOf(id).primary)
        assertEquals(null, CustomExercises.muscleOf("lat_pulldown"))
    }

    @Test
    fun `grouping covers every library exercise`() {
        assertEquals(library.size, ExerciseAlternatives.grouped(library).sumOf { it.second.size })
    }
}
