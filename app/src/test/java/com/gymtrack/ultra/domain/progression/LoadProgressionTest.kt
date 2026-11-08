package com.gymtrack.ultra.domain.progression

import com.gymtrack.ultra.domain.model.PreviousSet
import com.gymtrack.ultra.domain.model.TrackingType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LoadProgressionTest {

    private fun sets(vararg reps: Int, kg: Double = 20.0) = reps.map { PreviousSet(kg, it, null) }

    @Test
    fun `all sets at top of range suggests one small step up`() {
        val advice = LoadProgression.advise("db_bench_press", TrackingType.REPS, sets(12, 12, 12), 3, 12)
        assertEquals(LoadAdvice.Increase(20.0, 22.0, 3, 12), advice)
    }

    @Test
    fun `missing reps in one set means keep the weight`() {
        val advice = LoadProgression.advise("db_bench_press", TrackingType.REPS, sets(12, 11, 10), 3, 12)
        assertEquals(LoadAdvice.Keep(20.0, 2), advice)
    }

    @Test
    fun `fewer completed sets than planned never increases`() {
        val advice = LoadProgression.advise("lat_pulldown", TrackingType.REPS, sets(12, 12, kg = 40.0), 3, 12)
        assertTrue(advice is LoadAdvice.Keep)
    }

    @Test
    fun `machine steps are bigger and isolation steps smaller`() {
        val press = LoadProgression.advise("leg_press", TrackingType.REPS, sets(12, 12, 12, kg = 80.0), 3, 12)
        assertEquals(90.0, (press as LoadAdvice.Increase).toKg, 0.001)
        val raise = LoadProgression.advise("lateral_raise", TrackingType.REPS, sets(15, 15, kg = 5.0), 2, 15)
        assertEquals(6.0, (raise as LoadAdvice.Increase).toKg, 0.001)
    }

    @Test
    fun `uses lightest working set as base`() {
        val mixed = listOf(PreviousSet(22.0, 12, null), PreviousSet(20.0, 12, null), PreviousSet(20.0, 12, null))
        val advice = LoadProgression.advise("db_bench_press", TrackingType.REPS, mixed, 3, 12) as LoadAdvice.Increase
        assertEquals(20.0, advice.fromKg, 0.001)
        assertEquals(22.0, advice.toKg, 0.001)
    }

    @Test
    fun `timed and bodyweight exercises get no advice`() {
        assertNull(LoadProgression.advise("plank", TrackingType.TIME, listOf(PreviousSet(null, null, 40)), 2, 40))
        assertNull(LoadProgression.advise("knee_raise", TrackingType.REPS, listOf(PreviousSet(null, 12, null)), 2, 12))
        assertNull(LoadProgression.advise("db_bench_press", TrackingType.REPS, emptyList(), 3, 12))
    }
}
