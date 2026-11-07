package com.gymtrack.ultra.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.gymtrack.ultra.ui.theme.GtColors

/** Karta Cyber Lime: Surface na czarnym tle; [highlighted] = gradient + neonowy obrys (karta "hero"). */
@Composable
fun GtCard(
    modifier: Modifier = Modifier,
    highlighted: Boolean = false,
    onClick: (() -> Unit)? = null,
    onClickLabel: String? = null,
    contentPadding: PaddingValues = PaddingValues(18.dp),
    verticalSpacing: Dp = 12.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = MaterialTheme.shapes.large
    val interaction = remember { MutableInteractionSource() }
    val scale by rememberPressScale(interaction, pressedScale = 0.98f)

    val surfaceModifier = if (highlighted) {
        Modifier
            .background(GtColors.HeroGradient)
            .border(1.dp, GtColors.Lime.copy(alpha = 0.28f), shape)
    } else {
        Modifier.background(GtColors.Surface)
    }
    val clickModifier = if (onClick != null) {
        Modifier.clickable(
            interactionSource = interaction,
            indication = ripple(color = GtColors.Lime),
            onClickLabel = onClickLabel,
            role = Role.Button,
            onClick = onClick,
        )
    } else {
        Modifier
    }

    Column(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(shape)
            .then(surfaceModifier)
            .then(clickModifier)
            .padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(verticalSpacing),
        content = content,
    )
}

/** Mała etykieta sekcji: wersaliki, rozstrzelone. */
@Composable
fun SectionLabel(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = GtColors.TextSecondary,
) {
    Text(
        text = text.uppercase(),
        modifier = modifier,
        style = MaterialTheme.typography.labelMedium,
        color = color,
    )
}

/** Nagłówek ekranu: neonowy "eyebrow" + duży tytuł + opcjonalna akcja po prawej. */
@Composable
fun ScreenHeader(
    eyebrow: String,
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            SectionLabel(eyebrow, color = GtColors.Lime)
            Text(
                text = title,
                style = MaterialTheme.typography.headlineLarge,
                color = GtColors.TextPrimary,
                modifier = Modifier.semantics { heading() },
            )
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = GtColors.TextSecondary)
            }
        }
        if (trailing != null) trailing()
    }
}

/** Kafelek z dużą liczbą i podpisem. */
@Composable
fun StatTile(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    caption: String? = null,
    accent: Boolean = false,
) {
    val shape = MaterialTheme.shapes.medium
    Column(
        modifier = modifier
            .clip(shape)
            .background(if (accent) GtColors.Lime.copy(alpha = 0.12f) else GtColors.Surface)
            .then(if (accent) Modifier.border(1.dp, GtColors.Lime.copy(alpha = 0.4f), shape) else Modifier)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        SectionLabel(label, color = if (accent) GtColors.Lime else GtColors.TextSecondary)
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            color = if (accent) GtColors.Lime else GtColors.TextPrimary,
        )
        if (caption != null) {
            Text(caption, style = MaterialTheme.typography.bodySmall, color = GtColors.TextSecondary)
        }
    }
}
