package com.gymtrack.ultra.data.seed

import com.gymtrack.ultra.domain.progression.MuscleMap
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultContentTest {

    private val libraryIds = DefaultContent.exercises.map { it.id }.toSet()
    private val planIds = DefaultContent.plan.flatMap { day -> day.items.map { it.exerciseId } }

    @Test
    fun `every plan exercise exists in the library and has muscle targets`() {
        planIds.forEach { id ->
            assertTrue("$id w bibliotece", id in libraryIds)
            assertTrue("$id ma partie mięśni", MuscleMap.targetsOf(id).primary.isNotEmpty())
        }
    }

    @Test
    fun `legacy plan exercises stay in the library so old history keeps working`() {
        DefaultContent.legacyPlanV1.flatten().forEach { id -> assertTrue(id in libraryIds) }
    }

    @Test
    fun `new plan uses only classic gym exercises`() {
        val unusual = setOf("hip_thrust", "dead_bug", "side_plank", "face_pull", "lunges", "chest_supported_row")
        assertFalse(planIds.any { it in unusual })
        assertNotEquals(DefaultContent.legacyPlanV1, DefaultContent.plan.map { d -> d.items.map { it.exerciseId } })
    }

    @Test
    fun `plan is three full body days with no duplicate exercises`() {
        assertEquals(3, DefaultContent.plan.size)
        assertEquals(planIds.size, planIds.toSet().size)
        DefaultContent.plan.forEach { day -> assertTrue(day.items.size in 5..7) }
    }

    @Test
    fun `library ids are unique`() {
        assertEquals(DefaultContent.exercises.size, libraryIds.size)
    }
}
