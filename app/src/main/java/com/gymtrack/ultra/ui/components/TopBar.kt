package com.gymtrack.ultra.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gymtrack.ultra.core.format.Formatters
import com.gymtrack.ultra.domain.progression.StreakState
import com.gymtrack.ultra.ui.theme.GtColors

/** Logo: heksagon z ptaszkiem + nazwa. */
@Composable
fun Wordmark(modifier: Modifier = Modifier, markSize: Dp = 26.dp) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Canvas(Modifier.size(markSize)) {
            val k = size.minDimension / 24f
            scale(scale = k, pivot = Offset.Zero) {
                val hex = Path().apply {
                    moveTo(12f, 1.5f); lineTo(21.5f, 7f); lineTo(21.5f, 17f)
                    lineTo(12f, 22.5f); lineTo(2.5f, 17f); lineTo(2.5f, 7f); close()
                }
                drawPath(hex, GtColors.Lime)
                val check = Path().apply { moveTo(7.5f, 12.3f); lineTo(10.8f, 15.5f); lineTo(16.8f, 8.8f) }
                drawPath(check, GtColors.OnLime, style = Stroke(width = 2.6f, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
        }
        Spacer(Modifier.width(8.dp))
        Text(
            buildAnnotatedString {
                append("GymTrack")
                withStyle(SpanStyle(color = GtColors.TextMuted, fontWeight = FontWeight.Medium)) { append(" ultra") }
            },
            style = MaterialTheme.typography.titleLarge.copy(fontSize = 21.sp, letterSpacing = (-0.3).sp),
            color = GtColors.TextPrimary,
        )
    }
}

/** Pasek górny: logo po lewej, tarcze i płomień serii po prawej. */
@Composable
fun GtTopBar(
    streak: StreakState,
    onStreakClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Wordmark()
        Spacer(Modifier.weight(1f))
        StreakChip(streak, onClick = onStreakClick)
    }
}

@Composable
fun StreakChip(streak: StreakState, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val description = buildString {
        append("Seria: ${Formatters.countLabel(streak.current, "trening", "treningi", "treningów")}")
        if (streak.shields > 0) append(", tarcze: ${streak.shields}")
    }
    Row(
        modifier = modifier
            .clip(MaterialTheme.shapes.small)
            .clickable(onClick = onClick)
            .clearAndSetSemantics {
                contentDescription = description
                role = Role.Button
            }
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (streak.shields > 0) {
            Icon(GtIcons.Shield, contentDescription = null, tint = GtColors.Shield, modifier = Modifier.size(18.dp))
            Text(streak.shields.toString(), style = MaterialTheme.typography.titleSmall, color = GtColors.Shield)
            Spacer(Modifier.width(4.dp))
        }
        StreakFlame(lit = streak.isLit, size = 24.dp)
        Text(
            streak.current.toString(),
            style = MaterialTheme.typography.titleLarge.copy(fontSize = 18.sp),
            color = if (streak.isLit) GtColors.Fire else GtColors.TextMuted,
        )
    }
}
