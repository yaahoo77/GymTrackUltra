package com.gymtrack.ultra.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Grafitowa, płaska paleta. Kolor niesie znaczenie: lime = akcja i postęp,
 * kolory rang = twoja pozycja, pomarańcz = seria. Bez poświat i gradientowych teł.
 */
object GtColors {
    val Background = Color(0xFF121316)
    val Surface = Color(0xFF1B1D21)
    val Elevated = Color(0xFF25282D)
    val Outline = Color(0xFF2F3338)
    val OutlineStrong = Color(0xFF3D4248)
    val Divider = Color(0xFF24272B)

    val Lime = Color(0xFFB6FF4D)
    val Success = Color(0xFF72FF91)
    val OnLime = Color(0xFF10140A)
    val LimeDim = Color(0xFF243114)

    val Fire = Color(0xFFFF8A2B)
    val FireDim = Color(0xFF3A2416)
    val Shield = Color(0xFF7FB8FF)

    val TextPrimary = Color(0xFFF4F5F6)
    val TextSecondary = Color(0xFF9AA1A8)
    val TextMuted = Color(0xFF6B7279)
    val TextSoft = Color(0xFFC9CED3)

    val Error = Color(0xFFFF7A6B)
}

internal val GtColorScheme = darkColorScheme(
    primary = GtColors.Lime,
    onPrimary = GtColors.OnLime,
    primaryContainer = GtColors.LimeDim,
    onPrimaryContainer = GtColors.Lime,
    secondary = GtColors.Success,
    onSecondary = GtColors.OnLime,
    secondaryContainer = GtColors.Elevated,
    onSecondaryContainer = GtColors.TextPrimary,
    tertiary = GtColors.Fire,
    onTertiary = GtColors.OnLime,
    background = GtColors.Background,
    onBackground = GtColors.TextPrimary,
    surface = GtColors.Surface,
    onSurface = GtColors.TextPrimary,
    surfaceVariant = GtColors.Elevated,
    onSurfaceVariant = GtColors.TextSecondary,
    surfaceTint = Color.Transparent,
    surfaceContainerLowest = GtColors.Background,
    surfaceContainerLow = GtColors.Surface,
    surfaceContainer = GtColors.Surface,
    surfaceContainerHigh = GtColors.Elevated,
    surfaceContainerHighest = GtColors.Elevated,
    outline = GtColors.Outline,
    outlineVariant = GtColors.Divider,
    error = GtColors.Error,
    onError = GtColors.OnLime,
    scrim = Color(0xCC000000),
)
