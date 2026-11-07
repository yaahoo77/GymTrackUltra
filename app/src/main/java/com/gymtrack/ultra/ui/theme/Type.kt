package com.gymtrack.ultra.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Typografia: ciężkie, ciasne nagłówki i cyfry tabelaryczne ("tnum"),
 * żeby animowane liczniki nie skakały w poziomie.
 *
 * Makiety używają Space Grotesk (nagłówki/liczby) i Inter (treść). Na razie
 * korzystamy z systemowego sans-serif; podmiana na pliki w res/font to zmiana
 * tylko tych dwóch stałych.
 */
val DisplayFamily: FontFamily = FontFamily.SansSerif
val BodyFamily: FontFamily = FontFamily.SansSerif

private const val TABULAR = "tnum"

val GtTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = DisplayFamily, fontWeight = FontWeight.Black,
        fontSize = 56.sp, lineHeight = 58.sp, letterSpacing = (-1.6).sp,
        fontFeatureSettings = TABULAR,
    ),
    displayMedium = TextStyle(
        fontFamily = DisplayFamily, fontWeight = FontWeight.Black,
        fontSize = 44.sp, lineHeight = 46.sp, letterSpacing = (-1.2).sp,
        fontFeatureSettings = TABULAR,
    ),
    displaySmall = TextStyle(
        fontFamily = DisplayFamily, fontWeight = FontWeight.Black,
        fontSize = 38.sp, lineHeight = 40.sp, letterSpacing = (-1.1).sp,
        fontFeatureSettings = TABULAR,
    ),
    headlineLarge = TextStyle(
        fontFamily = DisplayFamily, fontWeight = FontWeight.Bold,
        fontSize = 32.sp, lineHeight = 36.sp, letterSpacing = (-0.6).sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = DisplayFamily, fontWeight = FontWeight.Bold,
        fontSize = 28.sp, lineHeight = 32.sp, letterSpacing = (-0.5).sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = DisplayFamily, fontWeight = FontWeight.Bold,
        fontSize = 22.sp, lineHeight = 26.sp, letterSpacing = (-0.2).sp,
    ),
    titleLarge = TextStyle(
        fontFamily = DisplayFamily, fontWeight = FontWeight.Bold,
        fontSize = 20.sp, lineHeight = 24.sp, fontFeatureSettings = TABULAR,
    ),
    titleMedium = TextStyle(
        fontFamily = BodyFamily, fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp, lineHeight = 22.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = BodyFamily, fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp, lineHeight = 20.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = BodyFamily, fontWeight = FontWeight.Normal,
        fontSize = 16.sp, lineHeight = 24.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = BodyFamily, fontWeight = FontWeight.Normal,
        fontSize = 14.sp, lineHeight = 20.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = BodyFamily, fontWeight = FontWeight.Normal,
        fontSize = 12.sp, lineHeight = 16.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = DisplayFamily, fontWeight = FontWeight.Bold,
        fontSize = 16.sp, lineHeight = 20.sp, letterSpacing = 0.9.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = BodyFamily, fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 1.7.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = BodyFamily, fontWeight = FontWeight.SemiBold,
        fontSize = 10.sp, lineHeight = 14.sp, letterSpacing = 0.4.sp,
    ),
)
