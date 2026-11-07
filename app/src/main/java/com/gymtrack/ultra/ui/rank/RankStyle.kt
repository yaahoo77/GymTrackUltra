package com.gymtrack.ultra.ui.rank

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import com.gymtrack.ultra.domain.progression.RankTier
import com.gymtrack.ultra.ui.theme.GtColors

/** Metal odznaki (jasny → ciemny), kamień i kolor tekstu rangi. */
data class RankPalette(
    val light: Color,
    val mid: Color,
    val dark: Color,
    val gemLight: Color,
    val gemDark: Color,
    /** Kolor nazwy rangi na ciemnym tle (kontrast ≥ 4.5:1). */
    val text: Color,
) {
    /** Tło karty zabarwione kolorem rangi. */
    val container: Color get() = lerp(GtColors.Surface, text, 0.13f)
    val border: Color get() = text.copy(alpha = 0.42f)
}

fun RankTier.palette(): RankPalette = when (this) {
    RankTier.IRON -> RankPalette(Color(0xFFC3C7CC), Color(0xFF80858C), Color(0xFF4A4E54), Color(0xFF9AA0A6), Color(0xFF5E646A), Color(0xFFAEB4BA))
    RankTier.BRONZE -> RankPalette(Color(0xFFF2B48A), Color(0xFFC07A4A), Color(0xFF7A4528), Color(0xFFE9A37A), Color(0xFF8C5232), Color(0xFFE0A077))
    RankTier.SILVER -> RankPalette(Color(0xFFF4F7FA), Color(0xFFBFC8D2), Color(0xFF7E8894), Color(0xFFFFFFFF), Color(0xFF9AA6B4), Color(0xFFD5DCE3))
    RankTier.GOLD -> RankPalette(Color(0xFFFFE9A3), Color(0xFFE8B84A), Color(0xFFA87420), Color(0xFFFFF4C9), Color(0xFFD99A2B), Color(0xFFF2C55C))
    RankTier.PLATINUM -> RankPalette(Color(0xFFD6FFF8), Color(0xFF4FD1C5), Color(0xFF1F8A80), Color(0xFFE9FFFB), Color(0xFF2FB3A6), Color(0xFF5FE0D3))
    RankTier.EMERALD -> RankPalette(Color(0xFFC4FFD9), Color(0xFF2ECC71), Color(0xFF137A3E), Color(0xFFE6FFEE), Color(0xFF1FA85A), Color(0xFF4ADB8A))
    RankTier.DIAMOND -> RankPalette(Color(0xFFE2EAFF), Color(0xFF7AA2FF), Color(0xFF3A55C8), Color(0xFFFFFFFF), Color(0xFF5D86FF), Color(0xFF93B4FF))
    RankTier.MASTER -> RankPalette(Color(0xFFF1D6FF), Color(0xFFB45CFF), Color(0xFF6A23A8), Color(0xFFFBEFFF), Color(0xFF9B3DF0), Color(0xFFC98AFF))
    RankTier.LEGEND -> RankPalette(Color(0xFFFFF0B8), Color(0xFFFFB547), Color(0xFFC4421F), Color(0xFFFFFFFF), Color(0xFFFF5A3D), Color(0xFFFFC35C))
}

/** Szara wersja dla partii bez rangi i zablokowanych pozycji. */
val UnrankedPalette = RankPalette(
    Color(0xFF6B7279), Color(0xFF4A4F55), Color(0xFF2E3237),
    Color(0xFF5C6268), Color(0xFF3A3F45), GtColors.TextMuted,
)
