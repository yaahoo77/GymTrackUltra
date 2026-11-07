package com.gymtrack.ultra.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gymtrack.ultra.ui.theme.GtColors
import com.gymtrack.ultra.ui.theme.LocalGtMotion

/** Sprężyste "wciśnięcie" — wyłączone przy ograniczonym ruchu. */
@Composable
fun rememberPressScale(
    interactionSource: InteractionSource,
    pressedScale: Float = 0.96f,
): State<Float> {
    val pressed by interactionSource.collectIsPressedAsState()
    val reduced = LocalGtMotion.current.reduced
    return animateFloatAsState(
        targetValue = if (pressed && !reduced) pressedScale else 1f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMediumLow),
        label = "pressScale",
    )
}

/** Główny neonowy przycisk (START WORKOUT itd.). Glow tylko w stanie aktywnym. */
@Composable
fun NeonButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    enabled: Boolean = true,
    height: Dp = 64.dp,
) {
    val shape = RoundedCornerShape(20.dp)
    val interaction = remember { MutableInteractionSource() }
    val scale by rememberPressScale(interaction)
    val contentColor = if (enabled) GtColors.OnLime else GtColors.TextSecondary

    Row(
        modifier = modifier
            .height(height)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .shadow(
                elevation = if (enabled) 18.dp else 0.dp,
                shape = shape,
                ambientColor = GtColors.Lime,
                spotColor = GtColors.Lime,
            )
            .clip(shape)
            .background(if (enabled) GtColors.Lime else GtColors.Elevated)
            .clickable(
                interactionSource = interaction,
                indication = ripple(color = GtColors.OnLime),
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leadingIcon != null) {
            Icon(leadingIcon, contentDescription = null, tint = contentColor, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(10.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge.copy(fontSize = 18.sp, letterSpacing = 1.sp),
            color = contentColor,
            textAlign = TextAlign.Center,
        )
    }
}

/** Przycisk drugorzędny: ciemna powierzchnia z obrysem. */
@Composable
fun GhostButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    accent: Boolean = false,
    height: Dp = 52.dp,
) {
    val shape = RoundedCornerShape(16.dp)
    val interaction = remember { MutableInteractionSource() }
    val scale by rememberPressScale(interaction)
    val color = if (accent) GtColors.Lime else GtColors.TextPrimary

    Row(
        modifier = modifier
            .height(height)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(shape)
            .background(if (accent) GtColors.Background else GtColors.Elevated)
            .border(1.dp, if (accent) GtColors.Lime else GtColors.Outline, shape)
            .clickable(
                interactionSource = interaction,
                indication = ripple(color = GtColors.Lime),
                role = Role.Button,
                onClick = onClick,
            )
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leadingIcon != null) {
            Icon(leadingIcon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text = text, style = MaterialTheme.typography.titleSmall, color = color)
    }
}

/** Kwadratowy przycisk z ikoną (min. 48 dp — wygodny jedną ręką). */
@Composable
fun IconSquareButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
) {
    val shape = RoundedCornerShape(16.dp)
    val interaction = remember { MutableInteractionSource() }
    val scale by rememberPressScale(interaction, pressedScale = 0.92f)
    Row(
        modifier = modifier
            .size(size)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(shape)
            .background(GtColors.Surface)
            .border(1.dp, GtColors.Outline, shape)
            .clickable(
                interactionSource = interaction,
                indication = ripple(color = GtColors.Lime),
                role = Role.Button,
                onClickLabel = contentDescription,
                onClick = onClick,
            ),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = contentDescription, tint = GtColors.TextPrimary, modifier = Modifier.size(22.dp))
    }
}
