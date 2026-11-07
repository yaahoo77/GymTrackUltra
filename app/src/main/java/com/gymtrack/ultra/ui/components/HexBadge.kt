package com.gymtrack.ultra.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.gymtrack.ultra.domain.progression.Achievements
import com.gymtrack.ultra.domain.progression.Tier
import com.gymtrack.ultra.ui.theme.GtColors

private const val VIEW_W = 64f
private const val VIEW_H = 70f

/** Heksagonalna odznaka rysowana w układzie 64×70 (jak w makietach), skalowana do [size]. */
@Composable
fun HexBadge(
    fill: Color,
    stroke: Color,
    glyph: String,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    contentDescription: String? = null,
) {
    val hex = remember {
        Path().apply {
            moveTo(32f, 3f)
            lineTo(59f, 18.5f)
            lineTo(59f, 51.5f)
            lineTo(32f, 67f)
            lineTo(5f, 51.5f)
            lineTo(5f, 18.5f)
            close()
        }
    }
    val glyphPath = remember(glyph) { PathParser().parsePathString(glyph).toPath() }
    val a11y = if (contentDescription != null) {
        Modifier.semantics {
            this.contentDescription = contentDescription
            role = Role.Image
        }
    } else {
        Modifier
    }

    Canvas(modifier = modifier.size(width = size, height = size * (VIEW_H / VIEW_W)).then(a11y)) {
        scale(scaleX = this.size.width / VIEW_W, scaleY = this.size.height / VIEW_H, pivot = Offset.Zero) {
            drawPath(hex, color = fill)
            drawPath(hex, color = stroke, style = Stroke(width = 2.5f, join = StrokeJoin.Round))
            drawPath(glyphPath, color = stroke, style = Stroke(width = 3.4f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
    }
}

/** Wygląd rang — kolory i znak; domena zna tylko nazwy. */
data class BadgeStyle(val fill: Color, val stroke: Color, val glyph: String)

fun Tier.badgeStyle(): BadgeStyle = when (this) {
    Tier.IGNITE -> BadgeStyle(GtColors.Surface, GtColors.TextSecondary, "M32 22 C27 30 26 36 32 46 C38 36 37 30 32 22 Z")
    Tier.PATHFINDER -> BadgeStyle(Color(0xFF122016), GtColors.Success, "M22 44 L32 26 L42 44")
    Tier.FORGE -> BadgeStyle(Color(0xFF17210E), GtColors.Lime, "M20 30 H44 M24 30 V44 H40 V30 M28 24 H36")
    Tier.VANGUARD -> BadgeStyle(GtColors.LimeDim, GtColors.Lime, "M32 22 L42 30 V42 L32 48 L22 42 V30 Z M32 30 V40")
    Tier.COLOSSUS -> BadgeStyle(Color(0xFF26340F), GtColors.LimeSoft, "M22 46 V28 L32 22 L42 28 V46 M27 46 V34 H37 V46")
    Tier.ASCENDANT -> BadgeStyle(GtColors.Lime, GtColors.OnLime, "M20 44 L26 28 L32 38 L38 28 L44 44 Z")
}

/** Znak każdego osiągnięcia. Odblokowane: lime; zablokowane: przygaszone. */
fun achievementGlyph(code: String): String = when (code) {
    Achievements.FIRST_REP -> "M20 35 L28 43 L44 26"
    Achievements.FIRST_WORKOUT -> "M24 46 V22 M24 24 H42 L38 30 L42 36 H24"
    Achievements.WARMUP_RITUAL -> "M32 22 C27 30 26 36 32 46 C38 36 37 30 32 22 Z"
    Achievements.FULL_CYCLE -> "M22 34 A10 10 0 1 1 32 44 M22 34 L19 29 M22 34 L27 31"
    Achievements.WORKOUTS_10 -> "M22 26 V44 M30 30 C30 24 42 24 42 30 V40 C42 46 30 46 30 40 Z"
    Achievements.CONSISTENT_BUILDER -> "M20 44 V30 M28 44 V24 M36 44 V30 M44 44 V20"
    Achievements.SMART_LOAD -> "M32 22 V44 M24 36 L32 44 L40 36"
    Achievements.TECHNIQUE_MASTER -> "M18 35 C24 25 40 25 46 35 C40 45 24 45 18 35 Z M32 35 V35.5"
    Achievements.WORKOUTS_50 -> "M32 21 L36 31 L46 31 L38 37 L41 47 L32 41 L23 47 L26 37 L18 31 L28 31 Z"
    Achievements.STEADY_QUARTER -> "M21 26 H43 V45 H21 Z M21 32 H43 M27 22 V28 M37 22 V28"
    Achievements.PLAN_ARCHITECT -> "M22 24 H42 V44 H22 Z M32 24 V44 M22 34 H42"
    Achievements.WORKOUTS_100 -> "M20 44 L22 26 L28 34 L32 24 L36 34 L42 26 L44 44 Z"
    else -> "M20 35 L28 43 L44 26"
}
