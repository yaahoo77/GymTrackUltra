package com.gymtrack.ultra

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gymtrack.ultra.domain.model.UserSettings
import com.gymtrack.ultra.domain.progression.Tier
import com.gymtrack.ultra.navigation.GymAppScaffold
import com.gymtrack.ultra.ui.components.HexBadge
import com.gymtrack.ultra.ui.theme.GtColors
import com.gymtrack.ultra.ui.theme.GtMotion
import com.gymtrack.ultra.ui.theme.GymTrackTheme
import com.gymtrack.ultra.ui.theme.LocalGtMotion
import com.gymtrack.ultra.ui.theme.rememberSystemReducedMotion

@Composable
fun GymTrackRoot(viewModel: MainViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val current = state
    val settings = (current as? AppState.Ready)?.settings ?: UserSettings()
    val systemReduced = rememberSystemReducedMotion()
    val motion = GtMotion(
        reduced = settings.reduceMotion || systemReduced,
        celebrations = settings.celebrationsEnabled,
        haptics = settings.hapticsEnabled,
    )

    GymTrackTheme(motion = motion) {
        Box(
            Modifier
                .fillMaxSize()
                .background(GtColors.Background),
        ) {
            Crossfade(
                targetState = current is AppState.Ready,
                animationSpec = tween(if (motion.reduced) 0 else 350),
                label = "appState",
            ) { ready ->
                if (ready) GymAppScaffold() else LaunchScreen()
            }
        }
    }
}

/** Ekran startowy na czas przygotowania bazy (zwykle ułamek sekundy). */
@Composable
private fun LaunchScreen() {
    val reduced = LocalGtMotion.current.reduced
    val pulse = if (reduced) {
        1f
    } else {
        val transition = rememberInfiniteTransition(label = "launch")
        val value by transition.animateFloat(
            initialValue = 0.55f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
            label = "launchPulse",
        )
        value
    }
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        HexBadge(
            fill = GtColors.LimeDim,
            stroke = GtColors.Lime,
            glyph = "M20 35 L28 43 L44 26",
            size = 72.dp,
            modifier = Modifier.alpha(pulse),
        )
        Text(
            text = buildAnnotatedString {
                append("GYMTRACK ")
                withStyle(SpanStyle(color = GtColors.Lime)) { append("ULTRA") }
            },
            style = MaterialTheme.typography.headlineMedium,
            color = GtColors.TextPrimary,
        )
        Text(
            text = Tier.IGNITE.motto,
            style = MaterialTheme.typography.bodyMedium,
            color = GtColors.TextSecondary,
        )
    }
}
