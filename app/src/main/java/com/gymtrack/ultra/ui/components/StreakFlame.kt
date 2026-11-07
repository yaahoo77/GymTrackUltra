package com.gymtrack.ultra.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.gymtrack.ultra.ui.theme.LocalGtMotion

private const val OUTER = "M52 6 C60 20 80 32 80 58 C80 80 66 94 50 94 C33 94 20 81 20 62 C20 48 27 40 33 30 C35 38 38 43 43 45 C42 30 45 17 52 6 Z"
private const val MIDDLE = "M51 34 C57 44 68 52 68 66 C68 80 60 88 50 88 C40 88 32 80 32 69 C32 60 37 55 41 49 C43 54 45 57 48 58 C47 49 48 41 51 34 Z"
private const val INNER = "M50 58 C54 64 60 68 60 76 C60 83 56 86 50 86 C44 86 40 83 40 77 C40 71 45 66 50 58 Z"

/**
 * Płomień serii. Zapalony — migocze i lekko się kołysze; zgaszony — szary i nieruchomy.
 * [boost] > 1 robi płomień większym i żywszym (ekran świętowania serii).
 */
@Composable
fun StreakFlame(
    lit: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    boost: Float = 1f,
) {
    val paths = remember {
        listOf(OUTER, MIDDLE, INNER).map { PathParser().parsePathString(it).toPath() }
    }
    val animate = lit && !LocalGtMotion.current.reduced
    val flicker: Float
    val sway: Float
    val core: Float
    if (animate) {
        val t = rememberInfiniteTransition(label = "flame")
        val f by t.animateFloat(0.96f, 1.04f, infiniteRepeatable(tween(520, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "flicker")
        val s by t.animateFloat(-3f, 3f, infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "sway")
        val c by t.animateFloat(0.9f, 1.08f, infiniteRepeatable(tween(380, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "core")
        flicker = f
        sway = s
        core = c
    } else {
        flicker = 1f
        sway = 0f
        core = 1f
    }

    val outerBrush = if (lit) {
        Brush.verticalGradient(listOf(Color(0xFFFFB13B), Color(0xFFFF5A1F)), startY = 6f, endY = 94f)
    } else {
        Brush.verticalGradient(listOf(Color(0xFF6B7178), Color(0xFF3E4348)), startY = 6f, endY = 94f)
    }
    val middleBrush = if (lit) {
        Brush.verticalGradient(listOf(Color(0xFFFFD452), Color(0xFFFF9A2E)), startY = 34f, endY = 88f)
    } else {
        Brush.verticalGradient(listOf(Color(0xFF555B61), Color(0xFF4A4F55)), startY = 34f, endY = 88f)
    }
    val innerColor = if (lit) Color(0xFFFFF2B0) else Color(0xFF5E646A)

    Canvas(modifier.size(size)) {
        val k = this.size.minDimension / 100f
        scale(scale = k, pivot = Offset.Zero) {
            val base = Offset(50f, 94f)
            withTransform({
                rotate(sway, pivot = base)
                scale(scaleX = boost, scaleY = flicker * boost, pivot = base)
            }) {
                drawPath(paths[0], outerBrush)
                withTransform({ rotate(-sway * 0.6f, pivot = base) }) {
                    drawPath(paths[1], middleBrush)
                }
                withTransform({ scale(scaleX = core, scaleY = core, pivot = base) }) {
                    drawPath(paths[2], innerColor)
                }
            }
        }
    }
}
