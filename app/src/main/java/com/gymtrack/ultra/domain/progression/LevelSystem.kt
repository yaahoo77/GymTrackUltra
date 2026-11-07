package com.gymtrack.ultra.domain.progression

/**
 * Rangi. Progi poziomów zgodne ze specyfikacją (1/5/10/20/30/50), nazwy autorskie.
 * Wygląd odznak jest w ui/components/TierStyle.kt — domena nie zna kolorów.
 */
enum class Tier(val minLevel: Int, val title: String, val motto: String) {
    IGNITE(1, "IGNITE", "Iskra zapalona. Każdy trening dokłada drewna."),
    PATHFINDER(5, "PATHFINDER", "Znasz już drogę do sali i technikę ruchów."),
    FORGE(10, "FORGE", "Nawyk wykuty. Regularność to twoja przewaga."),
    VANGUARD(20, "VANGUARD", "Prowadzisz — tydzień za tygodniem."),
    COLOSSUS(30, "COLOSSUS", "Siła zbudowana cierpliwie, bez skrótów."),
    ASCENDANT(50, "ASCENDANT", "Legenda własnego planu.");

    companion object {
        fun forLevel(level: Int): Tier = entries.last { level >= it.minLevel }

        /** Następna ranga albo null na szczycie. */
        fun nextAfter(tier: Tier): Tier? = entries.getOrNull(tier.ordinal + 1)
    }
}

data class LevelProgress(
    val level: Int,
    val tier: Tier,
    val xpIntoLevel: Int,
    /** XP potrzebne, by przejść z [level] na kolejny; 0 na maksymalnym poziomie. */
    val xpForNextLevel: Int,
    val totalXp: Int,
) {
    val isMaxLevel: Boolean get() = xpForNextLevel == 0
    val fraction: Float
        get() = if (isMaxLevel) 1f else (xpIntoLevel.toFloat() / xpForNextLevel).coerceIn(0f, 1f)
}

/**
 * Krzywa poziomów: przejście z poziomu n na n+1 kosztuje 100 + 25·(n−1) XP.
 * Przy ~3 treningach tygodniowo: LVL 5 ≈ 2 tyg., LVL 10 ≈ 5 tyg., LVL 50 ≈ 1,5–2 lata.
 */
object LevelSystem {
    const val MAX_LEVEL = 99

    fun xpToAdvanceFrom(level: Int): Int = 100 + 25 * (level - 1)

    /** Łączne XP potrzebne, by OSIĄGNĄĆ dany poziom (poziom 1 = 0 XP). */
    fun totalXpForLevel(level: Int): Int {
        val n = (level.coerceIn(1, MAX_LEVEL) - 1)
        return 100 * n + 25 * n * (n - 1) / 2
    }

    fun progressFor(totalXp: Int): LevelProgress {
        val xp = totalXp.coerceAtLeast(0)
        var level = 1
        while (level < MAX_LEVEL && xp >= totalXpForLevel(level + 1)) level++
        val into = xp - totalXpForLevel(level)
        val need = if (level >= MAX_LEVEL) 0 else xpToAdvanceFrom(level)
        return LevelProgress(
            level = level,
            tier = Tier.forLevel(level),
            xpIntoLevel = if (need == 0) 0 else into,
            xpForNextLevel = need,
            totalXp = xp,
        )
    }

    /** Poziomy zdobyte między dwoma stanami XP (do animacji awansu). Pusty, gdy brak awansu. */
    fun levelsGained(xpBefore: Int, xpAfter: Int): IntRange {
        val before = progressFor(xpBefore).level
        val after = progressFor(xpAfter).level
        return if (after > before) (before + 1)..after else IntRange.EMPTY
    }
}
