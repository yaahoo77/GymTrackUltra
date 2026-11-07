package com.gymtrack.ultra.core.format

import java.time.DayOfWeek
import java.time.LocalDate
import java.util.Locale
import kotlin.math.roundToLong

/** Polskie formaty: dni tygodnia, czas, ciężar z przecinkiem, odmiana liczebników. */
object Formatters {

    private val shortDays = listOf("Pn", "Wt", "Śr", "Cz", "Pt", "So", "Nd")
    private val dayNames = listOf("poniedziałek", "wtorek", "środa", "czwartek", "piątek", "sobota", "niedziela")
    private val onDayNames = listOf(
        "w poniedziałek", "we wtorek", "w środę", "w czwartek", "w piątek", "w sobotę", "w niedzielę",
    )

    fun shortDay(day: DayOfWeek): String = shortDays[day.value - 1]
    fun dayName(day: DayOfWeek): String = dayNames[day.value - 1]
    fun onDay(day: DayOfWeek): String = onDayNames[day.value - 1]

    fun capitalize(text: String): String =
        text.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.forLanguageTag("pl")) else it.toString() }

    /** 46 min · 1 h 05 min */
    fun durationMinutes(totalMinutes: Long): String {
        if (totalMinutes < 60) return "$totalMinutes min"
        val h = totalMinutes / 60
        val m = totalMinutes % 60
        return if (m == 0L) "$h h" else "$h h ${m.toString().padStart(2, '0')} min"
    }

    /** 0:42 · 46:12 · 1:02:03 */
    fun clock(totalSeconds: Long): String {
        val s = totalSeconds.coerceAtLeast(0)
        val h = s / 3600
        val m = (s % 3600) / 60
        val sec = s % 60
        return if (h > 0) {
            "$h:${m.toString().padStart(2, '0')}:${sec.toString().padStart(2, '0')}"
        } else {
            "$m:${sec.toString().padStart(2, '0')}"
        }
    }

    /** 32,5 kg · 16 kg — bez zbędnych zer, polski przecinek. */
    fun weightKg(kg: Double): String {
        val rounded = (kg * 100).roundToLong() / 100.0
        val text = if (rounded % 1.0 == 0.0) rounded.toLong().toString() else rounded.toString().trimEnd('0')
        return text.replace('.', ',') + " kg"
    }

    /** 07.11 */
    fun dayMonth(date: LocalDate): String =
        "${date.dayOfMonth.toString().padStart(2, '0')}.${date.monthValue.toString().padStart(2, '0')}"

    /** Odmiana: 1 trening · 2 treningi · 5 treningów · 22 treningi · 12 treningów. */
    fun plural(count: Int, one: String, few: String, many: String): String {
        val n = kotlin.math.abs(count)
        return when {
            n == 1 -> one
            n % 10 in 2..4 && n % 100 !in 12..14 -> few
            else -> many
        }
    }

    fun countLabel(count: Int, one: String, few: String, many: String): String =
        "$count ${plural(count, one, few, many)}"
}
