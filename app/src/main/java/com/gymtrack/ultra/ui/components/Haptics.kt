package com.gymtrack.ultra.ui.components

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.gymtrack.ultra.ui.theme.LocalGtMotion

/** Krótkie, wyczuwalne sygnały. Wyłączane jednym przełącznikiem w Profilu. */
class GtHaptics(private val vibrator: Vibrator?, private val enabled: Boolean) {

    /** Zaliczona seria. */
    fun success() = play(longArrayOf(0, 18, 50, 30))

    /** Lekkie stuknięcie (przełączniki, ±15 s). */
    fun tick() {
        if (!enabled || vibrator == null) return
        if (Build.VERSION.SDK_INT >= 29) {
            vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
        } else {
            vibrator.vibrate(VibrationEffect.createOneShot(12, VibrationEffect.DEFAULT_AMPLITUDE))
        }
    }

    /** Koniec przerwy. */
    fun restDone() = play(longArrayOf(0, 200, 120, 200))

    /** Awans, ukończony trening. */
    fun celebrate() = play(longArrayOf(0, 30, 60, 30, 60, 80))

    private fun play(pattern: LongArray) {
        if (!enabled || vibrator == null || !vibrator.hasVibrator()) return
        vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
    }

    companion object {
        fun vibratorOf(context: Context): Vibrator? =
            if (Build.VERSION.SDK_INT >= 31) {
                context.getSystemService(VibratorManager::class.java)?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
    }
}

@Composable
fun rememberGtHaptics(): GtHaptics {
    val context = LocalContext.current
    val enabled = LocalGtMotion.current.haptics
    return remember(context, enabled) { GtHaptics(GtHaptics.vibratorOf(context), enabled) }
}
