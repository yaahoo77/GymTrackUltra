package com.gymtrack.ultra.ui.exercise

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.gymtrack.ultra.ui.theme.GtColors
import com.gymtrack.ultra.ui.theme.LocalGtMotion
import kotlinx.coroutines.delay

/** Faza pętli animacji — do podpisów pod postacią. */
enum class MotionPhase { OUT, BACK }

@Stable
class ExercisePlayback internal constructor() {
    internal val progress = Animatable(0f)
    var phase by mutableStateOf(MotionPhase.OUT)
        internal set
    internal var useAlternate by mutableStateOf(false)
}

/**
 * Pętla ruchu: krótka pauza → faza 1 → pauza → faza 2. Faza "w dół" trwa dłużej niż "w górę" —
 * animacja pokazuje kontrolowane tempo, a nie szarpanie.
 * Przy ograniczonym ruchu albo [playing] = false nic się nie porusza (rysujemy start + koniec naraz).
 */
@Composable
fun rememberExercisePlayback(motion: ExerciseMotion?, playing: Boolean, startDelayMillis: Long = 0): ExercisePlayback {
    val playback = remember(motion) { ExercisePlayback() }
    val reduced = LocalGtMotion.current.reduced
    val animate = motion != null && playing && !reduced
    LaunchedEffect(playback, animate) {
        if (motion == null || !animate) {
            playback.progress.snapTo(1f)
            playback.phase = MotionPhase.OUT
            return@LaunchedEffect
        }
        val slow = 1500
        val fast = 950
        val outMs = when {
            motion.isometric -> 1800
            motion.liftFirst -> fast
            else -> slow
        }
        val backMs = when {
            motion.isometric -> 1800
            motion.liftFirst -> slow
            else -> fast
        }
        val holdTop = if (motion.isometric) 0L else 260L
        val holdStart = if (motion.isometric) 0L else 420L
        playback.progress.snapTo(0f)
        delay(startDelayMillis)
        while (true) {
            playback.phase = MotionPhase.OUT
            playback.progress.animateTo(1f, tween(outMs, easing = FastOutSlowInEasing))
            delay(holdTop)
            playback.phase = MotionPhase.BACK
            playback.progress.animateTo(0f, tween(backMs, easing = FastOutSlowInEasing))
            delay(holdStart)
            if (motion.alternate != null) playback.useAlternate = !playback.useAlternate
        }
    }
    return playback
}

/** Sama postać z animacją — bez podpisów. Do miniaturek. */
@Composable
fun ExerciseFigure(
    exerciseId: String,
    modifier: Modifier = Modifier,
    playing: Boolean = true,
    dimmed: Boolean = false,
    /** Przesunięcie startu — kilka miniaturek obok siebie nie rusza się jak jedna. */
    startDelayMillis: Long = 0,
) {
    val motion = remember(exerciseId) { ExerciseMotions.forId(exerciseId) }
    val playback = rememberExercisePlayback(motion, playing, startDelayMillis)
    ExerciseCanvas(motion, playback, playing, dimmed, modifier)
}

/** Duża wersja z podpisem fazy ruchu ("Zejdź powoli" / "Wstań…"). */
@Composable
fun ExerciseDemo(
    exerciseId: String,
    modifier: Modifier = Modifier,
    playing: Boolean = true,
) {
    val motion = remember(exerciseId) { ExerciseMotions.forId(exerciseId) } ?: return
    val playback = rememberExercisePlayback(motion, playing)
    val reduced = LocalGtMotion.current.reduced
    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        ExerciseCanvas(
            motion,
            playback,
            playing,
            dimmed = false,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = true),
        )
        if (motion.cueOut.isNotEmpty()) {
            if (reduced || !playing) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    CueText("1", motion.cueOut, Modifier.weight(1f))
                    CueText("2", motion.cueBack, Modifier.weight(1f))
                }
            } else {
                AnimatedContent(
                    targetState = playback.phase,
                    transitionSpec = {
                        (slideInVertically { it / 2 } + fadeIn(tween(220))) togetherWith
                            (slideOutVertically { -it / 2 } + fadeOut(tween(160)))
                    },
                    label = "cue",
                    modifier = Modifier.fillMaxWidth(),
                ) { phase ->
                    val (n, text) = if (phase == MotionPhase.OUT) "1" to motion.cueOut else "2" to motion.cueBack
                    CueText(n, text, Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@Composable
private fun CueText(number: String, text: String, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(20.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) { drawCircle(GtColors.LimeDim) }
            Text(number, style = MaterialTheme.typography.labelMedium, color = GtColors.Lime)
        }
        Text(text, style = MaterialTheme.typography.titleSmall, color = GtColors.TextPrimary)
    }
}

@Composable
private fun ExerciseCanvas(
    motion: ExerciseMotion?,
    playback: ExercisePlayback,
    playing: Boolean,
    dimmed: Boolean,
    modifier: Modifier,
) {
    val reduced = LocalGtMotion.current.reduced
    val still = reduced || !playing
    Canvas(modifier.semantics { contentDescription = "Animacja ruchu ćwiczenia" }) {
        if (motion == null) return@Canvas
        val fade = if (dimmed) 0.55f else 1f
        if (still) {
            // Bez ruchu: duch pozycji startowej + wyraźna pozycja końcowa.
            drawExerciseScene(motion, motion.end, glow = 0.6f, fade = fade, ghost = motion.start)
        } else {
            val t = playback.progress.value
            val target = motion.alternate?.takeIf { playback.useAlternate } ?: motion.end
            val glow = if (motion.isometric) 0.55f + 0.35f * t else 0.35f + 0.65f * t
            drawExerciseScene(motion, motion.start.mix(target, t), glow = glow, fade = fade)
        }
    }
}
