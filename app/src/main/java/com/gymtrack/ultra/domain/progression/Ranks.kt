package com.gymtrack.ultra.domain.progression

/** Drabinka rang — wspólna dla konta i każdej partii mięśni. */
enum class RankTier(val label: String) {
    IRON("Żelazo"),
    BRONZE("Brąz"),
    SILVER("Srebro"),
    GOLD("Złoto"),
    PLATINUM("Platyna"),
    EMERALD("Szmaragd"),
    DIAMOND("Diament"),
    MASTER("Mistrz"),
    LEGEND("Legenda"),
}

/**
 * Ranga = pozycja 0…24 na drabince: 8 tierów × 3 dywizje (I < II < III) + Legenda bez dywizji.
 */
data class Rank(val index: Int) : Comparable<Rank> {
    init {
        require(index in 0..MAX_INDEX) { "Rank index out of range: $index" }
    }

    val tier: RankTier
        get() = if (index >= MAX_INDEX) RankTier.LEGEND else RankTier.entries[index / 3]

    /** 1…3; 0 dla Legendy. */
    val division: Int
        get() = if (tier == RankTier.LEGEND) 0 else index % 3 + 1

    /** "ZŁOTO II", "LEGENDA". */
    val label: String
        get() = if (division == 0) tier.label.uppercase() else "${tier.label.uppercase()} ${roman(division)}"

    val next: Rank? get() = if (index < MAX_INDEX) Rank(index + 1) else null

    override fun compareTo(other: Rank): Int = index.compareTo(other.index)

    companion object {
        const val MAX_INDEX = 24

        fun of(tier: RankTier, division: Int): Rank =
            if (tier == RankTier.LEGEND) Rank(MAX_INDEX) else Rank(tier.ordinal * 3 + (division.coerceIn(1, 3) - 1))

        fun roman(n: Int): String = when (n) {
            1 -> "I"
            2 -> "II"
            3 -> "III"
            else -> ""
        }
    }
}

data class RankProgress(
    val rank: Rank,
    val pointsIntoRank: Int,
    /** Punkty potrzebne do następnej dywizji; 0 na Legendzie. */
    val pointsForNext: Int,
    val totalPoints: Int,
) {
    val isMax: Boolean get() = pointsForNext == 0
    val fraction: Float
        get() = if (isMax) 1f else (pointsIntoRank.toFloat() / pointsForNext).coerceIn(0f, 1f)
}

/**
 * Krzywa rang partii mięśni: dywizja n → n+1 kosztuje 40 + 15·n punktów.
 * Partia trenowana ~2× w tygodniu: Złoto po ~3 miesiącach, Diament po ~roku, Legenda po ~1,5 roku.
 */
object MuscleRankCurve {
    fun costOf(index: Int): Int = 40 + 15 * index

    fun totalFor(index: Int): Int {
        val n = index.coerceIn(0, Rank.MAX_INDEX)
        return 40 * n + 15 * n * (n - 1) / 2
    }

    fun progressFor(points: Int): RankProgress {
        val p = points.coerceAtLeast(0)
        var i = 0
        while (i < Rank.MAX_INDEX && p >= totalFor(i + 1)) i++
        val need = if (i >= Rank.MAX_INDEX) 0 else costOf(i)
        return RankProgress(Rank(i), if (need == 0) 0 else p - totalFor(i), need, p)
    }
}

/** Ranga konta wynika z poziomu: LVL 1 = Żelazo I, LVL 20 = Złoto I, LVL 50+ = Legenda. */
object AccountRank {
    fun forLevel(level: Int): Rank =
        Rank((((level.coerceAtLeast(1) - 1) * Rank.MAX_INDEX) / 49).coerceIn(0, Rank.MAX_INDEX))
}
