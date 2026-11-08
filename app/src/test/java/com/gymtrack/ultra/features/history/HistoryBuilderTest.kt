package com.gymtrack.ultra.features.history

import com.gymtrack.ultra.domain.model.LoggedSet
import com.gymtrack.ultra.domain.model.SessionSummary
import com.gymtrack.ultra.domain.model.TrackingType
import com.gymtrack.ultra.domain.progression.PersonalRecord
import com.gymtrack.ultra.domain.progression.RecordType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneOffset

class HistoryBuilderTest {

    private val oct = Instant.parse("2026-10-30T17:00:00Z")
    private val nov = Instant.parse("2026-11-04T17:00:00Z")
    private val sessions = listOf(
        SessionSummary(1, 1, "WORKOUT A", oct, oct.plusSeconds(3600), true, 110),
        SessionSummary(2, 2, "WORKOUT B", nov, nov.plusSeconds(2700), false, 90),
    )
    private fun set(session: Long, ex: String, i: Int, kg: Double?, reps: Int) =
        LoggedSet(session, if (session == 1L) oct else nov, 1, ex, TrackingType.REPS, i, 3, 8, 12, kg, reps, null)

    private val sets = listOf(
        set(1, "db_bench_press", 0, 20.0, 10), set(1, "db_bench_press", 1, 20.0, 10),
        set(2, "lat_pulldown", 0, 40.0, 12), set(2, "knee_raise", 0, null, 10),
    )
    private val names = mapOf("db_bench_press" to "Wyciskanie", "lat_pulldown" to "Ściąganie", "knee_raise" to "Kolana")
    private val records = listOf(PersonalRecord(2, "lat_pulldown", RecordType.WEIGHT, 40.0, 12, null))

    @Test
    fun `items are newest first with sets volume and records`() {
        val items = HistoryBuilder.items(sessions, sets, records, names)
        assertEquals(listOf(2L, 1L), items.map { it.sessionId })
        assertEquals(2, items[0].sets)
        assertEquals(480.0, items[0].volumeKg, 0.001)
        assertEquals(1, items[0].records)
        assertEquals(400.0, items[1].volumeKg, 0.001)
        assertEquals(listOf("Ściąganie", "Kolana"), items[0].exerciseNames)
    }

    @Test
    fun `months are grouped newest first with polish names`() {
        val months = HistoryBuilder.months(HistoryBuilder.items(sessions, sets, records, names), ZoneOffset.UTC)
        assertEquals(listOf("Listopad 2026", "Październik 2026"), months.map { it.label })
    }

    @Test
    fun `detail lists exercises with sets and record flags`() {
        val item = HistoryBuilder.items(sessions, sets, records, names).first { it.sessionId == 2L }
        val detail = HistoryBuilder.detail(item, sets, records, names)
        assertEquals(2, detail.exercises.size)
        assertTrue(detail.exercises.first { it.exerciseId == "lat_pulldown" }.hasRecord)
    }
}
