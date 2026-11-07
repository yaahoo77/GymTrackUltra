package com.gymtrack.ultra.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

/** Aplikacja jest wyłącznie ciemna — to część tożsamości Cyber Lime Dark. */
@Composable
fun GymTrackTheme(
    motion: GtMotion = GtMotion(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = GtColorScheme,
        typography = GtTypography,
        shapes = GtShapes,
    ) {
        CompositionLocalProvider(LocalGtMotion provides motion, content = content)
    }
}
