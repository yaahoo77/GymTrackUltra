package com.gymtrack.ultra.ui.theme

import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext

/**
 * Ustawienia ruchu dostępne w całym drzewie Compose.
 * [reduced] = przełącznik użytkownika LUB systemowe "usuń animacje".
 */
@Immutable
data class GtMotion(
    val reduced: Boolean = false,
    val celebrations: Boolean = true,
    val haptics: Boolean = true,
) {
    /** Konfetti i drobinki tylko gdy użytkownik ich chce i ruch nie jest ograniczony. */
    val showCelebrations: Boolean get() = celebrations && !reduced
}

val LocalGtMotion = staticCompositionLocalOf { GtMotion() }

/** Odczyt systemowej skali animacji. 0 = użytkownik wyłączył animacje w opcjach Androida. */
@Composable
fun rememberSystemReducedMotion(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        ) == 0f
    }
}

object GtDurations {
    const val Fast = 160
    const val Medium = 260
    const val Slow = 420
    const val Reduced = 100
}
