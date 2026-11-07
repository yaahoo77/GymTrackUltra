package com.gymtrack.ultra.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.gymtrack.ultra.ui.theme.GtColors
import com.gymtrack.ultra.ui.theme.LocalGtMotion

/** Elegancki pusty stan: ikona w neonowym kółku, tytuł, jedno zdanie. */
@Composable
fun EmptyState(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    icon: ImageVector = GtIcons.Sparkle,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp, horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(GtColors.Lime.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = GtColors.Lime, modifier = Modifier.size(28.dp))
        }
        Text(title, style = MaterialTheme.typography.titleLarge, color = GtColors.TextPrimary, textAlign = TextAlign.Center)
        Text(message, style = MaterialTheme.typography.bodyMedium, color = GtColors.TextSecondary, textAlign = TextAlign.Center)
    }
}

/** Blok szkieletu ładowania — pulsuje delikatnie; przy ograniczonym ruchu stoi. */
@Composable
fun SkeletonBlock(
    height: Dp,
    modifier: Modifier = Modifier,
) {
    val reduced = LocalGtMotion.current.reduced
    val alpha = if (reduced) {
        0.6f
    } else {
        val transition = rememberInfiniteTransition(label = "skeleton")
        val value by transition.animateFloat(
            initialValue = 0.35f,
            targetValue = 0.75f,
            animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Reverse),
            label = "skeletonAlpha",
        )
        value
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .alpha(alpha)
            .clip(MaterialTheme.shapes.large)
            .background(GtColors.Surface),
    )
}
