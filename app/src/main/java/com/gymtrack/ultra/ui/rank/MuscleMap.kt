package com.gymtrack.ultra.ui.rank

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.gymtrack.ultra.domain.progression.MuscleGroup
import com.gymtrack.ultra.domain.progression.MuscleRank
import com.gymtrack.ultra.ui.theme.GtColors
import com.gymtrack.ultra.ui.theme.LocalGtMotion

/**
 * Mapa mięśni: Twoja sylwetka z przodu i z tyłu, każda partia w kolorze swojej rangi.
 * Bez porównywania z kimkolwiek — tylko Twoje postępy. Dotknięcie partii otwiera jej szczegóły.
 */
@Composable
fun MuscleMapView(
    muscles: List<MuscleRank>,
    onSelect: (MuscleGroup) -> Unit,
    modifier: Modifier = Modifier,
) {
    val reduced = LocalGtMotion.current.reduced
    val fill = remember { Animatable(if (reduced) 1f else 0f) }
    LaunchedEffect(Unit) { if (!reduced) fill.animateTo(1f, tween(900, delayMillis = 150, easing = FastOutSlowInEasing)) }
    val colors = muscles.associate { rank ->
        rank.muscle to if (rank.isRanked) rank.progress.rank.tier.palette().mid else UnrankedPalette.mid
    }
    val description = muscles.joinToString { "${it.muscle.label}: ${if (it.isRanked) it.progress.rank.label else "bez rangi"}" }

    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Canvas(
            Modifier
                .fillMaxWidth()
                .aspectRatio(VIEW_W / VIEW_H)
                .semantics { contentDescription = "Mapa mięśni. $description" }
                .pointerInput(muscles) {
                    detectTapGestures { tap ->
                        val unit = size.width / VIEW_W
                        val x = tap.x / unit
                        val y = tap.y / unit
                        val (shapes, localX) = if (x < VIEW_W / 2f) FrontShapes to x - FIGURE_PAD else BackShapes to x - VIEW_W / 2f - FIGURE_PAD
                        hitTest(shapes, localX, y - TOP_PAD)?.let(onSelect)
                    }
                },
        ) {
            val unit = size.width / VIEW_W
            drawFigure(FrontShapes, Offset(FIGURE_PAD * unit, TOP_PAD * unit), unit, colors, fill.value)
            drawFigure(BackShapes, Offset((VIEW_W / 2f + FIGURE_PAD) * unit, TOP_PAD * unit), unit, colors, fill.value)
        }
        Row(Modifier.fillMaxWidth()) {
            Text("PRZÓD", style = MaterialTheme.typography.labelSmall, color = GtColors.TextMuted, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
            Text("TYŁ", style = MaterialTheme.typography.labelSmall, color = GtColors.TextMuted, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
        }
    }
}

private const val VIEW_W = 240f
private const val VIEW_H = 222f
private const val FIGURE_PAD = 10f
private const val TOP_PAD = 4f

/** Kształt ciała: punkty (x, y) w układzie 100×210; [mirrored] = rysuj też lustrzane odbicie (lewa strona). */
private class BodyShape(val muscle: MuscleGroup?, val points: FloatArray, val mirrored: Boolean) {
    val right: List<Offset> = points.toList().chunked(2) { Offset(it[0], it[1]) }
    val left: List<Offset> = right.map { Offset(100f - it.x, it.y) }
    val outlines: List<List<Offset>> get() = if (mirrored) listOf(right, left) else listOf(right)
}

private fun DrawScope.drawFigure(
    shapes: List<BodyShape>,
    origin: Offset,
    unit: Float,
    colors: Map<MuscleGroup, Color>,
    fill: Float,
) {
    val outline = Stroke(width = 1.2f * unit, join = StrokeJoin.Round)
    shapes.forEach { shape ->
        val target = shape.muscle?.let { colors[it] } ?: GtColors.Elevated
        val color = if (shape.muscle == null) target else lerp(GtColors.Elevated, target, fill)
        shape.outlines.forEach { points ->
            val path = smoothPath(points.map { Offset(origin.x + it.x * unit, origin.y + it.y * unit) })
            drawPath(path, color)
            drawPath(path, GtColors.Background, style = outline)
        }
    }
}

private fun smoothPath(points: List<Offset>): Path = Path().apply {
    val n = points.size
    moveTo(points[0].x, points[0].y)
    for (i in 0 until n) {
        val p0 = points[(i - 1 + n) % n]
        val p1 = points[i]
        val p2 = points[(i + 1) % n]
        val p3 = points[(i + 2) % n]
        cubicTo(
            p1.x + (p2.x - p0.x) / 6f, p1.y + (p2.y - p0.y) / 6f,
            p2.x - (p3.x - p1.x) / 6f, p2.y - (p3.y - p1.y) / 6f,
            p2.x, p2.y,
        )
    }
    close()
}

/** Która partia jest pod palcem (test punkt-w-wielokącie na punktach kształtu). Ostatni narysowany wygrywa. */
private fun hitTest(shapes: List<BodyShape>, x: Float, y: Float): MuscleGroup? =
    shapes.lastOrNull { shape -> shape.muscle != null && shape.outlines.any { inside(it, x, y) } }?.muscle

private fun inside(polygon: List<Offset>, x: Float, y: Float): Boolean {
    var result = false
    var j = polygon.lastIndex
    for (i in polygon.indices) {
        val a = polygon[i]
        val b = polygon[j]
        if ((a.y > y) != (b.y > y) && x < (b.x - a.x) * (y - a.y) / (b.y - a.y) + a.x) result = !result
        j = i
    }
    return result
}

private val FrontShapes: List<BodyShape> = listOf(
    BodyShape(null, floatArrayOf(50f, 6f, 57f, 8f, 60f, 16f, 59f, 25f, 54f, 30f, 50f, 31f, 46f, 30f, 41f, 25f, 40f, 16f, 43f, 8f), mirrored = false),
    BodyShape(null, floatArrayOf(45f, 28f, 55f, 28f, 56f, 37f, 44f, 37f), mirrored = false),
    BodyShape(MuscleGroup.SHOULDERS, floatArrayOf(60f, 38f, 68f, 37f, 75f, 41f, 78f, 49f, 76f, 56f, 70f, 55f, 65f, 48f, 60f, 44f), mirrored = true),
    BodyShape(MuscleGroup.CHEST, floatArrayOf(51f, 41f, 59f, 40f, 66f, 44f, 70f, 52f, 68f, 60f, 60f, 64f, 52f, 63f, 51f, 52f), mirrored = true),
    BodyShape(MuscleGroup.BICEPS, floatArrayOf(70f, 58f, 75f, 58f, 79f, 66f, 80f, 78f, 77f, 88f, 72f, 88f, 69f, 78f, 68f, 66f), mirrored = true),
    BodyShape(null, floatArrayOf(72f, 91f, 78f, 91f, 81f, 101f, 80f, 114f, 76f, 124f, 72f, 124f, 70f, 112f, 70f, 100f), mirrored = true),
    BodyShape(null, floatArrayOf(72f, 126f, 77f, 126f, 79f, 132f, 76f, 137f, 72f, 136f, 71f, 131f), mirrored = true),
    BodyShape(MuscleGroup.CORE, floatArrayOf(44f, 66f, 56f, 66f, 58f, 80f, 57f, 96f, 55f, 106f, 50f, 108f, 45f, 106f, 43f, 96f, 42f, 80f), mirrored = false),
    BodyShape(null, floatArrayOf(57f, 66f, 62f, 64f, 66f, 72f, 65f, 90f, 62f, 104f, 57f, 106f, 58f, 94f, 59f, 80f), mirrored = true),
    BodyShape(null, floatArrayOf(44f, 106f, 56f, 106f, 64f, 108f, 66f, 116f, 58f, 120f, 50f, 122f, 42f, 120f, 34f, 116f, 36f, 108f), mirrored = false),
    BodyShape(MuscleGroup.LEGS, floatArrayOf(52f, 121f, 60f, 118f, 66f, 120f, 67f, 134f, 65f, 150f, 62f, 160f, 56f, 161f, 53f, 150f, 51f, 136f), mirrored = true),
    BodyShape(null, floatArrayOf(54f, 165f, 62f, 164f, 64f, 174f, 63f, 188f, 60f, 198f, 56f, 198f, 54f, 188f, 53f, 175f), mirrored = true),
    BodyShape(null, floatArrayOf(55f, 200f, 61f, 200f, 64f, 205f, 60f, 207f, 54f, 206f), mirrored = true),
)

private val BackShapes: List<BodyShape> = listOf(
    BodyShape(null, floatArrayOf(50f, 6f, 57f, 8f, 60f, 16f, 59f, 25f, 54f, 30f, 50f, 31f, 46f, 30f, 41f, 25f, 40f, 16f, 43f, 8f), mirrored = false),
    BodyShape(MuscleGroup.BACK, floatArrayOf(45f, 30f, 55f, 30f, 64f, 37f, 58f, 44f, 50f, 52f, 42f, 44f, 36f, 37f), mirrored = false),
    BodyShape(MuscleGroup.SHOULDERS, floatArrayOf(63f, 38f, 70f, 37f, 76f, 41f, 78f, 49f, 76f, 55f, 70f, 54f, 66f, 47f), mirrored = true),
    BodyShape(MuscleGroup.BACK, floatArrayOf(52f, 47f, 59f, 42f, 66f, 46f, 68f, 58f, 64f, 74f, 58f, 88f, 52f, 92f, 51f, 70f), mirrored = true),
    BodyShape(MuscleGroup.TRICEPS, floatArrayOf(70f, 57f, 76f, 57f, 80f, 66f, 80f, 78f, 77f, 88f, 72f, 88f, 69f, 78f, 68f, 66f), mirrored = true),
    BodyShape(null, floatArrayOf(72f, 91f, 78f, 91f, 81f, 101f, 80f, 114f, 76f, 124f, 72f, 124f, 70f, 112f, 70f, 100f), mirrored = true),
    BodyShape(null, floatArrayOf(72f, 126f, 77f, 126f, 79f, 132f, 76f, 137f, 72f, 136f, 71f, 131f), mirrored = true),
    BodyShape(null, floatArrayOf(44f, 92f, 50f, 94f, 56f, 92f, 60f, 96f, 62f, 104f, 56f, 108f, 50f, 108f, 44f, 108f, 38f, 104f, 40f, 96f), mirrored = false),
    BodyShape(MuscleGroup.GLUTES, floatArrayOf(51f, 107f, 58f, 106f, 65f, 110f, 67f, 120f, 63f, 128f, 55f, 129f, 51f, 124f), mirrored = true),
    BodyShape(MuscleGroup.LEGS, floatArrayOf(52f, 130f, 60f, 129f, 66f, 131f, 67f, 142f, 65f, 154f, 62f, 161f, 56f, 162f, 53f, 152f, 51f, 140f), mirrored = true),
    BodyShape(null, floatArrayOf(54f, 165f, 62f, 164f, 65f, 172f, 64f, 186f, 60f, 198f, 56f, 198f, 54f, 188f, 53f, 175f), mirrored = true),
    BodyShape(null, floatArrayOf(55f, 200f, 61f, 200f, 64f, 205f, 60f, 207f, 54f, 206f), mirrored = true),
)
