package com.gymtrack.ultra.ui.exercise

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.gymtrack.ultra.ui.theme.GtColors
import com.gymtrack.ultra.ui.theme.LocalGtMotion
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

/** Faza pętli animacji — do podpisów pod postacią. */
enum class MotionPhase { OUT, BACK }

@Stable
class ExercisePlayback internal constructor() {
    internal val progress = Animatable(0f)
    var phase by mutableStateOf(MotionPhase.OUT)
        internal set
    internal var useAlternate by mutableStateOf(false)
}

/**
 * Pętla ruchu: krótka pauza → faza 1 → pauza → faza 2. Faza "w dół" trwa dłużej niż "w górę" —
 * animacja pokazuje kontrolowane tempo, a nie szarpanie.
 * Przy ograniczonym ruchu albo [playing] = false nic się nie porusza (rysujemy start + koniec naraz).
 */
@Composable
fun rememberExercisePlayback(motion: ExerciseMotion?, playing: Boolean, startDelayMillis: Long = 0): ExercisePlayback {
    val playback = remember(motion) { ExercisePlayback() }
    val reduced = LocalGtMotion.current.reduced
    val animate = motion != null && playing && !reduced
    LaunchedEffect(playback, animate) {
        if (motion == null || !animate) {
            playback.progress.snapTo(1f)
            playback.phase = MotionPhase.OUT
            return@LaunchedEffect
        }
        val slow = 1500
        val fast = 950
        val outMs = when {
            motion.isometric -> 1800
            motion.liftFirst -> fast
            else -> slow
        }
        val backMs = when {
            motion.isometric -> 1800
            motion.liftFirst -> slow
            else -> fast
        }
        val holdTop = if (motion.isometric) 0L else 260L
        val holdStart = if (motion.isometric) 0L else 420L
        playback.progress.snapTo(0f)
        delay(startDelayMillis)
        while (true) {
            playback.phase = MotionPhase.OUT
            playback.progress.animateTo(1f, tween(outMs, easing = FastOutSlowInEasing))
            delay(holdTop)
            playback.phase = MotionPhase.BACK
            playback.progress.animateTo(0f, tween(backMs, easing = FastOutSlowInEasing))
            delay(holdStart)
            if (motion.alternate != null) playback.useAlternate = !playback.useAlternate
        }
    }
    return playback
}

/** Sama postać z animacją — bez podpisów. Do miniaturek. */
@Composable
fun ExerciseFigure(
    exerciseId: String,
    modifier: Modifier = Modifier,
    playing: Boolean = true,
    dimmed: Boolean = false,
    /** Przesunięcie startu — kilka miniaturek obok siebie nie rusza się jak jedna. */
    startDelayMillis: Long = 0,
) {
    val motion = remember(exerciseId) { ExerciseMotions.forId(exerciseId) }
    val playback = rememberExercisePlayback(motion, playing, startDelayMillis)
    ExerciseCanvas(motion, playback, playing, dimmed, modifier)
}

/** Duża wersja z podpisem fazy ruchu ("Zejdź powoli" / "Wstań…"). */
@Composable
fun ExerciseDemo(
    exerciseId: String,
    modifier: Modifier = Modifier,
    playing: Boolean = true,
) {
    val motion = remember(exerciseId) { ExerciseMotions.forId(exerciseId) } ?: return
    val playback = rememberExercisePlayback(motion, playing)
    val reduced = LocalGtMotion.current.reduced
    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        ExerciseCanvas(
            motion,
            playback,
            playing,
            dimmed = false,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = true),
        )
        if (motion.cueOut.isNotEmpty()) {
            if (reduced || !playing) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    CueText("1", motion.cueOut, Modifier.weight(1f))
                    CueText("2", motion.cueBack, Modifier.weight(1f))
                }
            } else {
                AnimatedContent(
                    targetState = playback.phase,
                    transitionSpec = {
                        (slideInVertically { it / 2 } + fadeIn(tween(220))) togetherWith
                            (slideOutVertically { -it / 2 } + fadeOut(tween(160)))
                    },
                    label = "cue",
                    modifier = Modifier.fillMaxWidth(),
                ) { phase ->
                    val (n, text) = if (phase == MotionPhase.OUT) "1" to motion.cueOut else "2" to motion.cueBack
                    CueText(n, text, Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@Composable
private fun CueText(number: String, text: String, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(20.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) { drawCircle(GtColors.LimeDim) }
            Text(number, style = MaterialTheme.typography.labelMedium, color = GtColors.Lime)
        }
        Text(text, style = MaterialTheme.typography.titleSmall, color = GtColors.TextPrimary)
    }
}

@Composable
private fun ExerciseCanvas(
    motion: ExerciseMotion?,
    playback: ExercisePlayback,
    playing: Boolean,
    dimmed: Boolean,
    modifier: Modifier,
) {
    val reduced = LocalGtMotion.current.reduced
    val still = reduced || !playing
    Canvas(modifier.semantics { contentDescription = "Animacja ruchu ćwiczenia" }) {
        if (motion == null) return@Canvas
        val fade = if (dimmed) 0.55f else 1f
        val geo = FigureGeometry(size, motion.scale)
        if (motion.floor) drawFloor(geo, fade)
        drawStaticProps(motion, geo, fade)
        if (still) {
            // Bez ruchu: duch pozycji startowej + wyraźna pozycja końcowa.
            drawFigure(motion, motion.start, geo, glow = 0f, alpha = 0.28f * fade)
            drawFigure(motion, motion.end, geo, glow = 0.6f, alpha = fade)
        } else {
            val t = playback.progress.value
            val target = motion.alternate?.takeIf { playback.useAlternate } ?: motion.end
            val pose = motion.start.mix(target, t)
            drawFigure(motion, pose, geo, glow = if (motion.isometric) 0.5f + 0.3f * t else t, alpha = fade)
        }
    }
}

// ---------------------------------------------------------------- rysowanie

private object FigureColors {
    val Near = Color(0xFFE6E9EC)
    val Far = Color(0xFF8D949B)
    val HighlightFar = Color(0xFF7DB33A)
    val Equipment = Color(0xFF4A5057)
    val EquipmentLight = Color(0xFF7A8189)
    val Cable = Color(0xFF9AA1A8)
}

/** Mapowanie z układu 100×100 (skala postaci wokół stóp) na piksele, wyśrodkowane w dostępnym miejscu. */
private class FigureGeometry(size: Size, private val scale: Float) {
    val unit = minOf(size.width, size.height) / 100f
    private val ox = (size.width - 100f * unit) / 2f
    private val oy = (size.height - 100f * unit) / 2f
    val width = size.width

    fun p(x: Float, y: Float) = Offset(ox + (50f + (x - 50f) * scale) * unit, oy + (88f + (y - 88f) * scale) * unit)
    fun p(o: Offset) = p(o.x, o.y)
    fun w(v: Float) = v * scale * unit
    fun floorY() = oy + 89.5f * unit
}

private fun DrawScope.drawFloor(g: FigureGeometry, alpha: Float) {
    drawLine(GtColors.Outline.copy(alpha = alpha), Offset(0f, g.floorY()), Offset(g.width, g.floorY()), 1.5f * g.unit)
}

private fun DrawScope.drawStaticProps(m: ExerciseMotion, g: FigureGeometry, alpha: Float) {
    m.props.forEach { prop ->
        when (prop) {
            is Prop.Block -> {
                val c = g.p(prop.cx, prop.cy)
                val w = g.w(prop.w)
                val h = g.w(prop.h)
                rotate(prop.angle, pivot = c) {
                    drawRoundRect(
                        color = FigureColors.Equipment.copy(alpha = alpha),
                        topLeft = Offset(c.x - w / 2f, c.y - h / 2f),
                        size = Size(w, h),
                        cornerRadius = CornerRadius(g.w(1.5f)),
                    )
                }
            }
            is Prop.Rail -> drawLine(
                FigureColors.Equipment.copy(alpha = alpha), g.p(prop.from), g.p(prop.to), g.w(3f), StrokeCap.Round,
            )
            else -> Unit
        }
    }
}

private fun DrawScope.drawFigure(m: ExerciseMotion, pose: Pose, g: FigureGeometry, glow: Float, alpha: Float) {
    val s = solve(pose, m.view)
    val front = m.view == View.FRONT

    fun tint(segment: Segment, near: Boolean): Color {
        val n = near || front
        val base = if (segment in m.highlight) {
            if (n) GtColors.Lime else FigureColors.HighlightFar
        } else {
            if (n) FigureColors.Near else FigureColors.Far
        }
        return base.copy(alpha = alpha)
    }

    fun bone(a: Offset, b: Offset, width: Float, segment: Segment, near: Boolean) {
        val c = tint(segment, near)
        if (segment in m.highlight && glow > 0.01f && alpha > 0.5f) {
            drawLine(GtColors.Lime.copy(alpha = 0.16f * glow * alpha), g.p(a), g.p(b), g.w(width * 2.1f), StrokeCap.Round)
        }
        drawLine(c, g.p(a), g.p(b), g.w(width), StrokeCap.Round)
    }

    // Kable rysujemy za postacią, ale przyczepione do aktualnej dłoni.
    m.props.filterIsInstance<Prop.Cable>().forEach { cable ->
        drawLine(FigureColors.Cable.copy(alpha = alpha), g.p(cable.anchor), g.p(s.handNear), 0.8f * g.unit)
        drawCircle(FigureColors.EquipmentLight.copy(alpha = alpha), g.w(2.2f), g.p(cable.anchor))
    }

    fun leg(near: Boolean) {
        val hip = if (near) s.hipNear else s.hipFar
        val knee = if (near) s.kneeNear else s.kneeFar
        val ankle = if (near) s.ankleNear else s.ankleFar
        bone(hip, knee, 8f, Segment.THIGH, near)
        bone(knee, ankle, 6.5f, Segment.SHIN, near)
        val toe = if (front) {
            Offset(ankle.x + if (near) 3f else -3f, ankle.y + 1f)
        } else {
            val a = Math.toRadians(pose.footAngle.toDouble())
            Offset(ankle.x + (cos(a) * Rig.FOOT).toFloat(), ankle.y + (sin(a) * Rig.FOOT).toFloat())
        }
        drawLine(tint(Segment.SHIN, near), g.p(ankle), g.p(toe), g.w(if (front) 4.5f else 4f), StrokeCap.Round)
    }

    fun arm(near: Boolean) {
        val shoulder = if (near) s.shoulderNear else s.shoulderFar
        val elbow = if (near) s.elbowNear else s.elbowFar
        val hand = if (near) s.handNear else s.handFar
        bone(shoulder, elbow, 5.5f, Segment.UPPER_ARM, near)
        bone(elbow, hand, 5f, Segment.FOREARM, near)
    }

    leg(near = false)
    arm(near = false)
    bone(s.hip, s.shoulder, if (front) 16f else 11f, Segment.TORSO, near = true)
    drawLine(FigureColors.Near.copy(alpha = alpha), g.p(s.shoulder), g.p(s.head), g.w(4f), StrokeCap.Round)
    drawCircle(FigureColors.Near.copy(alpha = alpha), g.w(Rig.HEAD_R), g.p(s.head))
    leg(near = true)
    arm(near = true)

    val light = FigureColors.EquipmentLight.copy(alpha = alpha)
    m.props.forEach { prop ->
        when (prop) {
            Prop.Dumbbells -> listOf(s.handNear, s.handFar).forEach { hand ->
                val c = g.p(hand)
                drawRoundRect(light, Offset(c.x - g.w(4f), c.y - g.w(3.5f)), Size(g.w(8f), g.w(7f)), CornerRadius(g.w(1.8f)))
            }
            Prop.DumbbellVertical -> {
                val c = g.p(lerp(s.handNear, s.handFar, 0.5f))
                drawRoundRect(light, Offset(c.x - g.w(3f), c.y - g.w(8f)), Size(g.w(6f), g.w(16f)), CornerRadius(g.w(2f)))
            }
            Prop.PlateOnHips -> {
                val c = g.p(s.hip)
                drawCircle(light, g.w(7f), Offset(c.x, c.y - g.w(6f)))
            }
            Prop.Roller -> drawCircle(light, g.w(4f), g.p(s.ankleNear))
            Prop.Handle -> {
                val c = g.p(s.handNear)
                drawRoundRect(light, Offset(c.x - g.w(2f), c.y - g.w(4f)), Size(g.w(4f), g.w(8f)), CornerRadius(g.unit))
            }
            Prop.Bar -> {
                val c = g.p(s.handNear)
                drawLine(light, Offset(c.x - g.w(9f), c.y), Offset(c.x + g.w(9f), c.y), g.w(2.5f), StrokeCap.Round)
            }
            is Prop.FootPlate -> {
                val c = g.p(s.ankleNear)
                val a = Math.toRadians(prop.angle.toDouble())
                val half = g.w(prop.length) / 2f
                val d = Offset((cos(a) * half).toFloat(), (sin(a) * half).toFloat())
                drawLine(light, c - d, c + d, g.w(4f), StrokeCap.Round)
            }
            else -> Unit
        }
    }
}
