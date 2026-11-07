package com.gymtrack.ultra.ui.components

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
 * Pasek postępu z gradientem mint → lime. Każda zmiana wartości jedzie springiem,
 * przy ograniczonym ruchu — natychmiast.
 */
@Composable
fun GradientProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    height: Dp = 8.dp,
    trackColor: Color = GtColors.Elevated,
) {
    val reduced = LocalGtMotion.current.reduced
    val spec: AnimationSpec<Float> = if (reduced) {
        snap()
    } else {
        spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessLow)
    }
    val target = progress.coerceIn(0f, 1f)
    val animated by animateFloatAsState(targetValue = target, animationSpec = spec, label = "progress")

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .semantics { progressBarRangeInfo = ProgressBarRangeInfo(target, 0f..1f) },
    ) {
        val radius = CornerRadius(size.height / 2f, size.height / 2f)
        drawRoundRect(color = trackColor, cornerRadius = radius)
        if (animated > 0f) {
            val width = (size.width * animated).coerceAtLeast(size.height)
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
