package com.gymtrack.ultra.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.gymtrack.ultra.ui.theme.LocalGtMotion
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

private val FaceColor = Color(0xFF3A1606)

/**
 * Iskra — płomyk-maskotka. Migocze, mruga, unosi się; po dotknięciu podskakuje ([reactKey] zmienia się
 * przy każdym dotknięciu). Przy ograniczonym ruchu stoi spokojnie i tylko zmienia minę.
 */
@Composable
fun Iskra(
    mood: IskraMood,
    modifier: Modifier = Modifier,
    size: Dp = 64.dp,
    reactKey: Int = 0,
) {
    val reduced = LocalGtMotion.current.reduced
    val paths = remember {
        listOf(FLAME_OUTER, FLAME_MIDDLE, FLAME_INNER).map { PathParser().parsePathString(it).toPath() }
    }

    val sway: Float
    val flicker: Float
    val bob: Float
    val zPhase: Float
    if (!reduced) {
        val t = rememberInfiniteTransition(label = "iskra")
        val s by t.animateFloat(-2.2f, 2.2f, infiniteRepeatable(tween(1100, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "sway")
        val f by t.animateFloat(0.97f, 1.03f, infiniteRepeatable(tween(560, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "flicker")
        val b by t.animateFloat(-1.6f, 1.6f, infiniteRepeatable(tween(1700, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "bob")
        val z by t.animateFloat(0f, 1f, infiniteRepeatable(tween(2400, easing = LinearEasing)), label = "z")
        sway = s
        flicker = f
        bob = b
        zPhase = z
    } else {
        sway = 0f
        flicker = 1f
        bob = 0f
        zPhase = 0.5f
    }

    // Mruganie w losowych odstępach — jak żywa postać, a nie metronom.
    val blink = remember { Animatable(0f) }
    LaunchedEffect(reduced, mood) {
        if (reduced || mood == IskraMood.SLEEPY || mood == IskraMood.CHEER) return@LaunchedEffect
        while (true) {
            delay(Random.nextLong(2200, 5200))
            blink.animateTo(1f, tween(70))
            blink.animateTo(0f, tween(110))
            if (Random.nextInt(5) == 0) {
                delay(120)
                blink.animateTo(1f, tween(60))
                blink.animateTo(0f, tween(100))
            }
        }
    }

    // Podskok po dotknięciu.
    val jump = remember { Animatable(0f) }
    val squash = remember { Animatable(1f) }
    var lastReact by remember { mutableStateOf(reactKey) }
    LaunchedEffect(reactKey) {
        if (reactKey == lastReact) return@LaunchedEffect
        lastReact = reactKey
        if (reduced) return@LaunchedEffect
        launch {
            squash.snapTo(0.86f)
            squash.animateTo(1f, spring(dampingRatio = 0.35f, stiffness = Spring.StiffnessMedium))
        }
        jump.animateTo(-16f, tween(150, easing = FastOutSlowInEasing))
        jump.animateTo(0f, spring(dampingRatio = 0.42f, stiffness = Spring.StiffnessMediumLow))
    }

    val outerBrush = Brush.verticalGradient(listOf(Color(0xFFFFB13B), Color(0xFFFF5A1F)), startY = 6f, endY = 94f)
    val middleBrush = Brush.verticalGradient(listOf(Color(0xFFFFD452), Color(0xFFFF9A2E)), startY = 34f, endY = 88f)
    val inner = Color(0xFFFFF2B0)

    Canvas(
        modifier
            .size(size)
            .semantics { contentDescription = "Iskra, maskotka" },
    ) {
        val k = this.size.minDimension / 100f
        scale(scale = k, pivot = Offset.Zero) {
            val base = Offset(50f, 94f)
            translate(top = bob + jump.value) {
                withTransform({
                    rotate(sway, pivot = base)
                    scale(scaleX = 2f - squash.value, scaleY = flicker * squash.value, pivot = base)
                }) {
                    drawPath(paths[0], outerBrush)
                    withTransform({ rotate(-sway * 0.5f, pivot = base) }) { drawPath(paths[1], middleBrush) }
                    drawPath(paths[2], inner)
                    drawFace(mood, blink.value, sway)
                }
            }
            if (mood == IskraMood.SLEEPY) drawSleepZ(zPhase)
        }
    }
}

private fun DrawScope.drawFace(mood: IskraMood, blink: Float, sway: Float) {
    val eyeY = 68f
    val look = sway * 0.25f // oczy lekko podążają za kołysaniem
    val eyes = listOf(43f + look, 57f + look)
    val stroke2 = Stroke(width = 2.1f, cap = StrokeCap.Round)
    when (mood) {
        IskraMood.HAPPY, IskraMood.FOCUS -> {
            val ry = (if (mood == IskraMood.FOCUS) 4f else 4.4f) * (1f - 0.86f * blink)
            eyes.forEachIndexed { i, x ->
                drawOval(FaceColor, Offset(x - 3.4f, eyeY - ry), Size(6.8f, ry * 2f))
                if (blink < 0.5f) drawCircle(Color.White, 1.15f, Offset(x + 1.1f, eyeY - 1.5f))
                if (mood == IskraMood.FOCUS) {
                    val dir = if (i == 0) 1f else -1f
                    drawLine(FaceColor, Offset(x - 4f, eyeY - 6.5f - 1f * dir), Offset(x + 4f, eyeY - 6.5f + 1f * dir), 1.8f, StrokeCap.Round)
                }
            }
            if (mood == IskraMood.FOCUS) {
                drawLine(FaceColor, Offset(46.5f + look, 77f), Offset(53.5f + look, 77f), 2f, StrokeCap.Round)
            } else {
                val mouth = Path().apply {
                    moveTo(46f + look, 76f)
                    quadraticTo(50f + look, 80f, 54f + look, 76f)
                }
                drawPath(mouth, FaceColor, style = stroke2)
            }
        }
        IskraMood.CHEER -> {
            eyes.forEach { x ->
                val arc = Path().apply {
                    moveTo(x - 3.5f, eyeY + 1.5f)
                    quadraticTo(x, eyeY - 4f, x + 3.5f, eyeY + 1.5f)
                }
                drawPath(arc, FaceColor, style = Stroke(width = 2.2f, cap = StrokeCap.Round))
            }
            val mouth = Path().apply {
                moveTo(45.5f + look, 75f)
                lineTo(54.5f + look, 75f)
                quadraticTo(54f + look, 81f, 50f + look, 81f)
                quadraticTo(46f + look, 81f, 45.5f + look, 75f)
                close()
            }
            drawPath(mouth, FaceColor)
        }
        IskraMood.SLEEPY -> {
            eyes.forEach { x ->
                val lid = Path().apply {
                    moveTo(x - 3.5f, eyeY)
                    quadraticTo(x, eyeY + 3f, x + 3.5f, eyeY)
                }
                drawPath(lid, FaceColor, style = stroke2)
            }
            drawOval(FaceColor, Offset(48.2f + look, 75.5f), Size(3.6f, 4f))
        }
    }
}

/** Unoszące się "z" przy drzemce. */
private fun DrawScope.drawSleepZ(phase: Float) {
    listOf(0f, 0.5f).forEach { offset ->
        val p = (phase + offset) % 1f
        val x = 74f + p * 8f
        val y = 34f - p * 22f
        val s = 4f + p * 3f
        val alpha = when {
            p < 0.2f -> p / 0.2f
            p > 0.75f -> (1f - p) / 0.25f
            else -> 1f
        }
        val z = Path().apply {
            moveTo(x, y)
            lineTo(x + s, y)
            lineTo(x, y + s)
            lineTo(x + s, y + s)
        }
        drawPath(z, Color.White.copy(alpha = 0.75f * alpha), style = Stroke(width = 1.6f, cap = StrokeCap.Round))
    }
}
