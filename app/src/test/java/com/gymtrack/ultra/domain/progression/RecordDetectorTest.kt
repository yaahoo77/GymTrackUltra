package com.gymtrack.ultra.domain.progression

import com.gymtrack.ultra.domain.model.LoggedSet
import com.gymtrack.ultra.domain.model.TrackingType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class RecordDetectorTest {

    private fun set(session: Long, w: Double?, r: Int?, ex: String = "goblet_squat", min: Int = 8, max: Int = 10, idx: Int = 0) =
        LoggedSet(session, Instant.ofEpochSecond(session * 86_400), 1, ex, TrackingType.REPS, idx, 3, min, max, w, r, null)

    @Test
    fun `first session is only a baseline`() {
        assertTrue(RecordDetector.detect(listOf(set(1, 16.0, 10))).isEmpty())
    }

    @Test
    fun `heavier clean set is a weight record`() {
        val records = RecordDetector.detect(listOf(set(1, 16.0, 10), set(2, 18.0, 8)))
        assertEquals(1, records.size)
        assertEquals(RecordType.WEIGHT, records[0].type)
        assertEquals(18.0, records[0].weightKg!!, 0.0)
    }

    @Test
    fun `heavier but too few reps is not a record`() {
        assertTrue(RecordDetector.detect(listOf(set(1, 16.0, 10), set(2, 20.0, 5))).isEmpty())
    }

    @Test
    fun `more reps at same weight is a reps record, capped at range top`() {
        val r = RecordDetector.detect(listOf(set(1, 16.0, 8), set(2, 16.0, 9)))
        assertEquals(RecordType.REPS, r.single().type)
        // 12 powtórzeń przy górnej granicy 10 — ponad 10 nie ma rekordu
        assertTrue(RecordDetector.detect(listOf(set(1, 16.0, 10), set(2, 16.0, 12))).isEmpty())
    }

    @Test
    fun `timed exercises track duration`() {
        val plank = { s: Long, d: Int ->
            LoggedSet(s, Instant.ofEpochSecond(s * 86_400), 3, "plank", TrackingType.TIME, 0, 2, 20, 40, null, null, d)
        }
        val r = RecordDetector.detect(listOf(plank(1, 25), plank(2, 30)))
        assertEquals(RecordType.TIME, r.single().type)
    }

    @Test
    fun `one record per exercise per session`() {
        val history = listOf(
            set(1, 16.0, 10, idx = 0), set(1, 16.0, 9, idx = 1),
            set(2, 18.0, 10, idx = 0), set(2, 18.0, 10, idx = 1),
        )
        assertEquals(1, RecordDetector.detect(history).size)
    }
}
