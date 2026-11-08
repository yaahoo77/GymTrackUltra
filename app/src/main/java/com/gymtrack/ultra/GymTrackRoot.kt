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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gymtrack.ultra.domain.model.UserSettings
import com.gymtrack.ultra.features.onboarding.OnboardingScreen
import com.gymtrack.ultra.navigation.GymAppScaffold
import com.gymtrack.ultra.ui.components.Wordmark
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
            val screen = when {
                current !is AppState.Ready -> RootScreen.LAUNCH
                !current.settings.onboardingCompleted -> RootScreen.ONBOARDING
                else -> RootScreen.APP
            }
            Crossfade(
                targetState = screen,
                animationSpec = tween(if (motion.reduced) 0 else 350),
                label = "appState",
            ) { target ->
                when (target) {
                    RootScreen.LAUNCH -> LaunchScreen()
                    RootScreen.ONBOARDING -> OnboardingScreen()
                    RootScreen.APP -> GymAppScaffold()
                }
            }
        }
    }
}

private enum class RootScreen { LAUNCH, ONBOARDING, APP }

/** Ekran startowy na czas przygotowania bazy (zwykle ułamek sekundy). */
@Composable
private fun LaunchScreen() {
    val reduced = LocalGtMotion.current.reduced
    val pulse = if (reduced) {
        1f
    } else {
        val transition = rememberInfiniteTransition(label = "launch")
        val value by transition.animateFloat(
            initialValue = 0.6f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
            label = "launchPulse",
        )
        value
    }
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Wordmark(modifier = Modifier.alpha(pulse), markSize = 34.dp)
    }
}
