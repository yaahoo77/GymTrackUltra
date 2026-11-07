package com.gymtrack.ultra.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Cyber Lime Dark — jedna paleta, jeden akcent.
 * Neon pojawia się tylko tam, gdzie coś jest aktywne albo właśnie zostało zrobione.
 */
object GtColors {
    val Background = Color(0xFF080A0B)
    val Surface = Color(0xFF141719)
    val Elevated = Color(0xFF1D2224)
    val Outline = Color(0xFF2A3033)
    val OutlineStrong = Color(0xFF3A4245)
    val Divider = Color(0xFF222829)

    val Lime = Color(0xFFB6FF4D)
    val Success = Color(0xFF72FF91)
    val OnLime = Color(0xFF0B0F05)
    val LimeDim = Color(0xFF1F2A12)
    val LimeDeep = Color(0xFF10140C)
    val LimeSoft = Color(0xFFD7FF9A)

    val TextPrimary = Color(0xFFFFFFFF)
    val TextSecondary = Color(0xFF929A9E)
    val TextMuted = Color(0xFF5E666A)
    val TextSoft = Color(0xFFC9CFD2)

    val Error = Color(0xFFFF7A6B)

    val ProgressGradient: Brush = Brush.horizontalGradient(listOf(Success, Lime))
    val HeroGradient: Brush = Brush.linearGradient(listOf(Elevated, Surface, LimeDeep))
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
    tertiary = GtColors.LimeSoft,
    onTertiary = GtColors.OnLime,
    background = GtColors.Background,
    onBackground = GtColors.TextPrimary,
    surface = GtColors.Surface,
    onSurface = GtColors.TextPrimary,
    surfaceVariant = GtColors.Elevated,
    onSurfaceVariant = GtColors.TextSecondary,
    surfaceTint = GtColors.Lime,
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
