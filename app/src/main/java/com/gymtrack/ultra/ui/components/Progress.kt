package com.gymtrack.ultra.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.gymtrack.ultra.ui.theme.GtColors
import com.gymtrack.ultra.ui.theme.LocalGtMotion

/**
 * Pasek postępu z gradientem mint → lime. Przy pierwszym pokazaniu napełnia się od zera,
 * każda kolejna zmiana jedzie sprężyną; przy ograniczonym ruchu — natychmiast.
 */
@Composable
fun GradientProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    height: Dp = 8.dp,
    trackColor: Color = GtColors.Elevated,
    /** Jednolity kolor (np. kolor rangi); null = gradient mint → lime. */
    color: Color? = null,
) {
    val reduced = LocalGtMotion.current.reduced
    val target = progress.coerceIn(0f, 1f)
    // Pierwsze pokazanie: pasek napełnia się od zera; kolejne zmiany jadą sprężyną.
    val anim = remember { Animatable(if (reduced) target else 0f) }
    var first by remember { mutableStateOf(true) }
    LaunchedEffect(target, reduced) {
        when {
            reduced -> anim.snapTo(target)
            first -> anim.animateTo(target, tween(900, delayMillis = 120, easing = FastOutSlowInEasing))
            else -> anim.animateTo(target, spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessLow))
        }
        first = false
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .semantics { progressBarRangeInfo = ProgressBarRangeInfo(target, 0f..1f) },
    ) {
        val animated = anim.value // odczyt w fazie rysowania — bez rekompozycji co klatkę
        val radius = CornerRadius(size.height / 2f, size.height / 2f)
        drawRoundRect(color = trackColor, cornerRadius = radius)
        if (animated > 0f) {
            val width = (size.width * animated).coerceAtLeast(size.height)
            if (color != null) {
                drawRoundRect(color = color, size = Size(width, size.height), cornerRadius = radius)
            } else {
                drawRoundRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(GtColors.Success, GtColors.Lime),
                        startX = 0f,
                        endX = width,
                    ),
                    size = Size(width, size.height),
                    cornerRadius = radius,
                )
            }
        }
    }
}
