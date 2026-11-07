package com.gymtrack.ultra.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.gymtrack.ultra.ui.theme.LocalGtMotion
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

/**
 * Połysk przesuwający się po elemencie co kilka sekund (odznaki rang, główny przycisk).
 * Rysowany trybem SrcAtop — błyszczy tylko to, co już jest narysowane, więc działa dla dowolnego kształtu.
 * Przy ograniczonym ruchu nic nie robi.
 */
fun Modifier.shine(
    enabled: Boolean = true,
    periodMillis: Int = 4200,
    sweepMillis: Int = 1100,
    delayMillis: Int = 600,
    strength: Float = 0.38f,
): Modifier = composed {
    if (!enabled || LocalGtMotion.current.reduced) return@composed this
    val transition = rememberInfiniteTransition(label = "shine")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(periodMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
            initialStartOffset = StartOffset(delayMillis),
        ),
        label = "shinePhase",
    )
    this
        .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
        .drawWithContent {
            drawContent()
            val sweep = phase * periodMillis / sweepMillis
            if (sweep >= 1f) return@drawWithContent
            val eased = FastOutSlowInEasing.transform(sweep)
            val w = size.width
            val h = size.height
            val band = maxOf(w, h) * 0.22f
            val travel = w + h + band * 4f
            val cx = -h / 2f - band * 2f + eased * travel
            val center = Offset(cx, h / 2f)
            drawRect(
                brush = Brush.linearGradient(
                    colors = listOf(Color.Transparent, Color.White.copy(alpha = strength), Color.Transparent),
                    start = Offset(center.x - band, center.y - band),
                    end = Offset(center.x + band, center.y + band),
                ),
                blendMode = BlendMode.SrcAtop,
            )
        }
}

/** Pamięta, które elementy listy już "wjechały" — przewijanie w górę nie odtwarza animacji drugi raz. */
@Stable
class AppearRegistry internal constructor() {
    internal val seen = mutableSetOf<Any>()
}

@Composable
fun rememberAppearRegistry(): AppearRegistry = remember { AppearRegistry() }

/** Wejście karty: delikatny wjazd z dołu i rozjaśnienie, kolejne karty z małym opóźnieniem. */
fun Modifier.appear(registry: AppearRegistry, key: Any, index: Int): Modifier = composed {
    val reduced = LocalGtMotion.current.reduced
    val skip = remember { reduced || key in registry.seen }
    val progress = remember { Animatable(if (skip) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (!skip) {
            registry.seen += key
            delay(index.coerceAtMost(10) * 55L)
            progress.animateTo(1f, spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessLow))
        }
    }
    graphicsLayer {
        val p = progress.value
        alpha = p.coerceIn(0f, 1f)
        translationY = (1f - p) * 28.dp.toPx()
    }
}

/**
 * Liczba, która "nabija się" od zera przy pierwszym pokazaniu i płynnie przechodzi do nowej wartości.
 * Przy ograniczonym ruchu zwraca od razu wartość docelową.
 */
@Composable
fun rememberCountUp(target: Int, durationMillis: Int = 900): Int {
    val reduced = LocalGtMotion.current.reduced
    val value = remember { Animatable(if (reduced) target.toFloat() else 0f) }
    LaunchedEffect(target, reduced) {
        if (reduced) {
            value.snapTo(target.toFloat())
        } else {
            value.animateTo(target.toFloat(), tween(durationMillis, easing = FastOutSlowInEasing))
        }
    }
    return value.value.roundToInt()
}

/** Delikatne unoszenie się w miejscu (maskotka, odznaki). Zwraca przesunięcie w px. */
@Composable
fun rememberFloat(amplitudePx: Float, periodMillis: Int = 1800, enabled: Boolean = true): Float {
    if (!enabled || LocalGtMotion.current.reduced) return 0f
    val transition = rememberInfiniteTransition(label = "float")
    val y by transition.animateFloat(
        initialValue = -amplitudePx,
        targetValue = amplitudePx,
        animationSpec = infiniteRepeatable(tween(periodMillis, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "floatY",
    )
    return y
}
