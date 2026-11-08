package com.gymtrack.ultra.domain.nutrition

import java.time.Instant
import java.time.LocalDate
import kotlin.math.roundToInt

/**
 * Cel białka w gramach na kilogram masy ciała.
 * Źródła: ISSN Position Stand "Protein and exercise" (2017) — 1,4–2,0 g/kg dla trenujących;
 * Morton i in. (2018, BJSM) — efekt plateau ok. 1,6 g/kg. Celowo bez wyższych progów:
 * więcej białka nie daje więcej mięśni, a aplikacja ma być bezpieczna także dla nastolatków.
 */
enum class ProteinGoal(val label: String, val gramsPerKg: Double, val hint: String) {
    TRAINING("Trening siłowy", 1.6, "Dla ćwiczących regularnie. Więcej nie daje już lepszych efektów."),
    BASIC("Podstawowy", 1.2, "Spokojny cel na start albo przy lżejszym trybie."),
}

object ProteinCalculator {
    const val MIN_WEIGHT_KG = 30.0
    const val MAX_WEIGHT_KG = 200.0
    const val MIN_TARGET = 30
    const val MAX_TARGET = 250
    const val MAX_ENTRY_GRAMS = 150

    fun isValidWeight(kg: Double?): Boolean = kg != null && kg in MIN_WEIGHT_KG..MAX_WEIGHT_KG

    /** Dzienny cel zaokrąglony do 5 g — łatwiej zapamiętać niż 112 g. */
    fun dailyTarget(weightKg: Double, goal: ProteinGoal): Int {
        val raw = weightKg.coerceIn(MIN_WEIGHT_KG, MAX_WEIGHT_KG) * goal.gramsPerKg
        return ((raw / 5.0).roundToInt() * 5).coerceIn(MIN_TARGET, MAX_TARGET)
    }

    /** Cel ręczny ma pierwszeństwo; bez wagi i bez celu ręcznego — brak celu (funkcja nieskonfigurowana). */
    fun resolveTarget(weightKg: Double?, goal: ProteinGoal, manualTarget: Int?): Int? = when {
        manualTarget != null -> manualTarget.coerceIn(MIN_TARGET, MAX_TARGET)
        weightKg != null && isValidWeight(weightKg) -> dailyTarget(weightKg, goal)
        else -> null
    }
}

/** Gotowy produkt do szybkiego dodania. Wartości orientacyjne, typowe dla produktów z polskiego sklepu. */
data class FoodPreset(val id: String, val name: String, val portion: String, val proteinGrams: Int)

object FoodPresets {
    val all: List<FoodPreset> = listOf(
        FoodPreset("egg", "Jajko", "1 szt.", 6),
        FoodPreset("chicken", "Pierś z kurczaka", "100 g po ugotowaniu", 30),
        FoodPreset("skyr", "Skyr naturalny", "150 g", 17),
        FoodPreset("cottage", "Serek wiejski", "200 g", 22),
        FoodPreset("quark", "Twaróg półtłusty", "100 g", 18),
        FoodPreset("tuna", "Tuńczyk w sosie własnym", "puszka, ok. 120 g", 28),
        FoodPreset("milk", "Mleko", "szklanka 250 ml", 8),
        FoodPreset("ham", "Szynka", "3 plasterki", 9),
        FoodPreset("cheese", "Ser żółty", "2 plasterki", 10),
        FoodPreset("yogurt", "Jogurt naturalny", "150 g", 6),
        FoodPreset("beef", "Wołowina", "100 g po ugotowaniu", 26),
        FoodPreset("salmon", "Łosoś", "100 g", 22),
        FoodPreset("sausages", "Parówki", "2 szt.", 10),
        FoodPreset("tofu", "Tofu", "100 g", 13),
        FoodPreset("lentils", "Soczewica", "100 g po ugotowaniu", 9),
        FoodPreset("peanuts", "Orzeszki ziemne", "garść, 30 g", 8),
        FoodPreset("oats", "Płatki owsiane", "50 g", 7),
        FoodPreset("bread", "Chleb", "2 kromki", 6),
        FoodPreset("pasta", "Makaron", "150 g po ugotowaniu", 8),
        FoodPreset("rice", "Ryż", "150 g po ugotowaniu", 4),
    )
}

data class FoodEntry(
    val id: Long,
    val date: LocalDate,
    val name: String,
    val proteinGrams: Int,
    val createdAt: Instant,
)

data class ProteinDay(val date: LocalDate, val grams: Int)

object ProteinStats {

    /** Ostatnie [days] dni (od najstarszego do dzisiaj), także dni bez wpisów (0 g). */
    fun lastDays(entries: List<FoodEntry>, today: LocalDate, days: Int = 7): List<ProteinDay> {
        val byDay = entries.groupBy { it.date }.mapValues { (_, list) -> list.sumOf { it.proteinGrams } }
        return (days - 1 downTo 0).map { back ->
            val date = today.minusDays(back.toLong())
            ProteinDay(date, byDay[date] ?: 0)
        }
    }

    /** Ile dni z listy dobiło do celu (z tolerancją 90% — nikt nie waży jedzenia co do grama). */
    fun daysOnTarget(days: List<ProteinDay>, target: Int): Int =
        days.count { it.grams >= target * 0.9 }

    /** Ostatnio wpisane własne produkty (bez duplikatów, najnowsze pierwsze) — do szybkiego powtórzenia. */
    fun recentCustom(entries: List<FoodEntry>, presetNames: Set<String>, limit: Int = 4): List<Pair<String, Int>> =
        entries.sortedByDescending { it.createdAt }
            .filter { it.name !in presetNames }
            .distinctBy { it.name.lowercase() to it.proteinGrams }
            .take(limit)
            .map { it.name to it.proteinGrams }
}
