package com.gymtrack.ultra.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import com.gymtrack.ultra.ui.theme.GtColors
import com.gymtrack.ultra.ui.theme.LocalGtMotion
import kotlin.random.Random

private data class Piece(
    val x: Float,
    val vx: Float,
    val vy: Float,
    val spin: Float,
    val w: Float,
    val h: Float,
    val color: Color,
)

/** Jednorazowe konfetti (~1,8 s). Wyłączone przy ograniczonym ruchu i bez efektów celebracji. */
@Composable
fun ConfettiBurst(
    modifier: Modifier = Modifier,
    seed: Int = 7,
    count: Int = 70,
) {
    if (!LocalGtMotion.current.showCelebrations) return
    val pieces = remember(seed) {
        val r = Random(seed)
        val colors = listOf(GtColors.Lime, GtColors.Success, Color.White, GtColors.Fire, Color(0xFFFFD452))
        List(count) {
            Piece(
                x = r.nextFloat(),
                vx = (r.nextFloat() - 0.5f) * 0.5f,
                vy = 0.35f + r.nextFloat() * 0.65f,
                spin = (r.nextFloat() - 0.5f) * 1080f,
                w = 6f + r.nextFloat() * 6f,
                h = 10f + r.nextFloat() * 8f,
                color = colors[r.nextInt(colors.size)],
            )
        }
    }
    val progress = remember(seed) { Animatable(0f) }
    LaunchedEffect(seed) { progress.animateTo(1f, tween(1800, easing = LinearEasing)) }
    if (progress.value >= 1f) return

    Canvas(modifier) {
        val t = progress.value
        for (p in pieces) {
            // Start nad ekranem, spadanie z lekką grawitacją, wygaszanie pod koniec.
            val x = (p.x + p.vx * t) * size.width
            val y = -40f + (p.vy * t + 0.9f * t * t) * size.height
            val alpha = if (t > 0.75f) (1f - t) / 0.25f else 1f
            rotate(degrees = p.spin * t, pivot = Offset(x, y)) {
                drawRect(
                    color = p.color,
                    topLeft = Offset(x - p.w / 2f, y - p.h / 2f),
                    size = Size(p.w, p.h),
                    alpha = alpha.coerceIn(0f, 1f),
                )
            }
        }
    }
}
