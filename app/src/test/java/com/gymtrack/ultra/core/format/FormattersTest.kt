package com.gymtrack.ultra.core.format

import org.junit.Assert.assertEquals
import org.junit.Test

class FormattersTest {

    @Test
    fun `polish plurals`() {
        fun t(n: Int) = Formatters.countLabel(n, "trening", "treningi", "treningów")
        assertEquals("1 trening", t(1))
        assertEquals("2 treningi", t(2))
        assertEquals("5 treningów", t(5))
        assertEquals("12 treningów", t(12))
        assertEquals("22 treningi", t(22))
        assertEquals("0 treningów", t(0))
    }

    @Test
    fun `weights use polish decimal comma`() {
        assertEquals("32,5 kg", Formatters.weightKg(32.5))
        assertEquals("16 kg", Formatters.weightKg(16.0))
        assertEquals("1,25 kg", Formatters.weightKg(1.25))
    }

    @Test
    fun `clock and duration`() {
        assertEquals("0:42", Formatters.clock(42))
        assertEquals("46:12", Formatters.clock(46 * 60 + 12))
        assertEquals("1:02:03", Formatters.clock(3723))
        assertEquals("46 min", Formatters.durationMinutes(46))
        assertEquals("1 h 05 min", Formatters.durationMinutes(65))
        assertEquals("2 h", Formatters.durationMinutes(120))
    }
}
