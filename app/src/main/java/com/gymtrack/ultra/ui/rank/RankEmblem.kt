package com.gymtrack.ultra.ui.rank

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
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
import com.gymtrack.ultra.domain.progression.Rank
import com.gymtrack.ultra.domain.progression.RankTier

private val OutlineInk = Color(0xFF0D0E10)
private val InsetFill = Color(0xFF15171A)

/**
 * Odznaka rangi — autorski projekt: tarcza + skrzydła/korona rosnące z tierem, kamień w środku,
 * dywizja jako romby pod spodem. [rank] = null → szara tarcza "bez rangi".
 */
@Composable
fun RankEmblem(
    rank: Rank?,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    showDivision: Boolean = true,
    contentDescription: String? = null,
) {
    val tier = rank?.tier ?: RankTier.IRON
    val palette = if (rank == null) UnrankedPalette else tier.palette()
    val division = if (rank == null) 0 else rank.division
    val layers = remember(tier) {
        EmblemGeometry.layers.getValue(tier).map { it.kind to PathParser().parsePathString(it.path).toPath() }
    }
    val pips = remember {
        (0 until 3).map { i ->
            val x = 50f + (i - 1) * 7f
            Path().apply {
                moveTo(x - 3f, 89f)
                lineTo(x, 86f)
                lineTo(x + 3f, 89f)
                lineTo(x, 92f)
                close()
            }
        }
    }
    val a11y = if (contentDescription != null) {
        Modifier.semantics {
            this.contentDescription = contentDescription
            role = Role.Image
        }
    } else {
        Modifier
    }

    Canvas(modifier = modifier.size(size).then(a11y)) {
        val k = this.size.minDimension / 100f
        scale(scale = k, pivot = Offset.Zero) {
            val metal = Brush.verticalGradient(listOf(palette.light, palette.mid, palette.dark), startY = 10f, endY = 86f)
            val gem = Brush.linearGradient(listOf(palette.gemLight, palette.gemDark), start = Offset(42f, 38f), end = Offset(58f, 62f))
            for ((kind, path) in layers) {
                when (kind) {
                    LayerKind.RAYS -> drawPath(path, palette.mid.copy(alpha = 0.55f))
                    LayerKind.METAL -> {
                        drawPath(path, metal)
                        drawPath(path, OutlineInk, style = Stroke(width = 1.4f, join = StrokeJoin.Round))
                    }
                    LayerKind.INSET -> {
                        drawPath(path, InsetFill)
                        drawPath(path, palette.dark, style = Stroke(width = 1f))
                    }
                    LayerKind.GEM -> {
                        drawPath(path, gem)
                        drawPath(path, OutlineInk, style = Stroke(width = 1f))
                    }
                    LayerKind.FACET -> drawPath(path, Color.White.copy(alpha = 0.45f), style = Stroke(width = 0.8f, cap = StrokeCap.Round))
                    LayerKind.RIVET -> {
                        drawPath(path, palette.light)
                        drawPath(path, OutlineInk, style = Stroke(width = 0.6f))
                    }
                }
            }
            if (showDivision && division > 0) {
                pips.forEachIndexed { i, pip ->
                    drawPath(pip, if (i < division) palette.mid else Color(0xFF2A2D31))
                    drawPath(pip, OutlineInk, style = Stroke(width = 0.6f))
                }
            }
        }
    }
}
