package com.gymtrack.ultra.ui.exercise

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.lerp
import com.gymtrack.ultra.ui.theme.GtColors
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/*
 * Rysowanie postaci "manekina": kończyny jako zwężające się kapsuły, tułów z profilem klatki i pleców,
 * dłonie, stopy, barki. Pracujące partie świecą na lime (strefy tułowia przycinane do jego kształtu).
 * Wszystkie wymiary w układzie 100×100 — przeliczane przez FigureGeometry.
 */

private object FigureColors {
    val Near = Color(0xFFD5DADF)
    val Far = Color(0xFF7A8189)
    val HighlightFar = Color(0xFF6E9E33)
    val HighlightDim = Color(0xFF8FCC3A)
    val Equipment = Color(0xFF454B52)
    val EquipmentLight = Color(0xFF7A8189)
    val Cable = Color(0xFF9AA1A8)
}

/** Promienie stawów (grubość kończyn). */
private object Radius {
    const val SHOULDER = 3.1f
    const val ELBOW = 2.3f
    const val WRIST = 1.8f
    const val HIP = 4.7f
    const val KNEE = 3.1f
    const val ANKLE = 2.1f
    const val HAND = 2.1f
    const val DELT = 3.6f
    const val HEAD = 5.4f
    const val OUTLINE = 0.9f
}

/** Profil tułowia: (pozycja wzdłuż kręgosłupa 0 = biodro … 1 = bark, odsunięcie z przodu, odsunięcie z tyłu). */
private val SideProfile = listOf(
    Triple(-0.08f, 4.2f, 5.6f),
    Triple(0.18f, 4.0f, 5.2f),
    Triple(0.42f, 4.3f, 4.2f),
    Triple(0.66f, 6.0f, 4.8f),
    Triple(0.86f, 6.2f, 5.4f),
    Triple(1.02f, 4.4f, 4.6f),
)
private val FrontProfile = listOf(
    Triple(-0.06f, 6.6f, 6.6f),
    Triple(0.2f, 6.4f, 6.4f),
    Triple(0.45f, 5.9f, 5.9f),
    Triple(0.7f, 7.6f, 7.6f),
    Triple(0.92f, 9.6f, 9.6f),
    Triple(1.04f, 8.4f, 8.4f),
)

/** Mapowanie z układu 100×100 (skala postaci wokół stóp) na piksele, wyśrodkowane w dostępnym miejscu. */
internal class FigureGeometry(size: Size, private val scale: Float) {
    val unit = minOf(size.width, size.height) / 100f
    private val ox = (size.width - 100f * unit) / 2f
    private val oy = (size.height - 100f * unit) / 2f
    val width = size.width

    fun p(x: Float, y: Float) = Offset(ox + (50f + (x - 50f) * scale) * unit, oy + (88f + (y - 88f) * scale) * unit)
    fun p(o: Offset) = p(o.x, o.y)
    fun w(v: Float) = v * scale * unit
    fun floorY() = oy + 89.5f * unit
}

/**
 * Cała scena: podłoga, sprzęt, postać. [ghost] = dodatkowa półprzezroczysta poza (tryb bez ruchu).
 * [fade] < 1 przygasza całość (ćwiczenie ukończone).
 */
internal fun DrawScope.drawExerciseScene(
    motion: ExerciseMotion,
    pose: Pose,
    glow: Float,
    fade: Float,
    ghost: Pose? = null,
    outline: Color = GtColors.Background,
) {
    val g = FigureGeometry(size, motion.scale)
    if (motion.floor) {
        drawLine(GtColors.Outline.copy(alpha = fade), Offset(0f, g.floorY()), Offset(g.width, g.floorY()), 1.5f * g.unit)
    }
    drawBlocks(motion, g, fade)
    if (ghost != null) {
        withLayerAlpha(0.26f * fade) { drawFigure(motion, ghost, g, glow = 0f, outline = outline) }
    }
    if (fade < 0.999f) {
        withLayerAlpha(fade) { drawFigure(motion, pose, g, glow, outline) }
    } else {
        drawFigure(motion, pose, g, glow, outline)
    }
}

/** Warstwa z przezroczystością — nakładające się części nie prześwitują przez siebie. */
private inline fun DrawScope.withLayerAlpha(alpha: Float, block: DrawScope.() -> Unit) {
    val paint = Paint().apply { this.alpha = alpha }
    drawIntoCanvas { it.saveLayer(Rect(Offset.Zero, size), paint) }
    block()
    drawIntoCanvas { it.restore() }
}

private fun DrawScope.drawBlocks(m: ExerciseMotion, g: FigureGeometry, alpha: Float) {
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

/** Zwężająca się kapsuła a→b (promienie w px) dopisana do ścieżki. */
private fun Path.capsule(a: Offset, b: Offset, ra: Float, rb: Float) {
    val dx = b.x - a.x
    val dy = b.y - a.y
    val len = hypot(dx, dy).coerceAtLeast(1e-3f)
    val ux = dx / len
    val uy = dy / len
    val nx = -uy
    val ny = ux
    val steps = 8
    for (i in 0..steps) {
        val ang = PI / 2 - PI * i / steps
        val s = sin(ang).toFloat()
        val c = cos(ang).toFloat()
        val x = b.x + (nx * s + ux * c) * rb
        val y = b.y + (ny * s + uy * c) * rb
        if (i == 0) moveTo(x, y) else lineTo(x, y)
    }
    for (i in 0..steps) {
        val ang = -PI / 2 - PI * i / steps
        val s = sin(ang).toFloat()
        val c = cos(ang).toFloat()
        lineTo(a.x + (nx * s + ux * c) * ra, a.y + (ny * s + uy * c) * ra)
    }
    close()
}

/** Zamknięta, gładka krzywa przez punkty (Catmull-Rom → Bézier). */
private fun Path.smoothClosed(points: List<Offset>) {
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

private fun DrawScope.drawFigure(m: ExerciseMotion, pose: Pose, g: FigureGeometry, glow: Float, outline: Color) {
    val s = solve(pose, m.view)
    val front = m.view == View.FRONT
    val hl = m.highlight
    val lime = lerp(FigureColors.HighlightDim, GtColors.Lime, glow.coerceIn(0f, 1f))
    val outlineStroke = Stroke(width = g.w(Radius.OUTLINE), join = StrokeJoin.Round)
    val path = Path()

    fun body(near: Boolean) = if (near || front) FigureColors.Near else FigureColors.Far
    fun tint(segment: Segment, near: Boolean): Color =
        if (segment in hl) (if (near || front) lime else FigureColors.HighlightFar) else body(near)

    fun part(a: Offset, b: Offset, ra: Float, rb: Float, color: Color, outlined: Boolean) {
        path.reset()
        path.capsule(g.p(a), g.p(b), g.w(ra), g.w(rb))
        drawPath(path, color)
        if (outlined) drawPath(path, outline, style = outlineStroke)
    }

    // --- sprzęt za postacią: linki i ramiona maszyn przyczepione do aktualnych stawów
    m.props.forEach { prop ->
        when (prop) {
            is Prop.Cable -> {
                drawLine(FigureColors.Cable, g.p(prop.anchor), g.p(s.handNear), 0.8f * g.unit)
                drawCircle(FigureColors.EquipmentLight, g.w(2.2f), g.p(prop.anchor))
            }
            is Prop.Lever -> {
                drawLine(FigureColors.EquipmentLight, g.p(prop.pivot), g.p(s.handNear), g.w(2.6f), StrokeCap.Round)
                drawCircle(FigureColors.EquipmentLight, g.w(2.6f), g.p(prop.pivot))
            }
            else -> Unit
        }
    }

    fun leg(near: Boolean) {
        val hip = if (near) s.hipNear else s.hipFar
        val knee = if (near) s.kneeNear else s.kneeFar
        val ankle = if (near) s.ankleNear else s.ankleFar
        val toeDir = if (front) {
            Offset(if (near) 3.5f else -3.5f, 1.2f)
        } else {
            val a = Math.toRadians(pose.footAngle.toDouble())
            Offset((cos(a) * Rig.FOOT).toFloat(), (sin(a) * Rig.FOOT).toFloat())
        }
        val len = hypot(toeDir.x, toeDir.y).coerceAtLeast(1e-3f)
        val heel = Offset(ankle.x - toeDir.x / len * 1.6f, ankle.y - toeDir.y / len * 1.6f)
        val toe = Offset(ankle.x + toeDir.x, ankle.y + toeDir.y)
        part(heel, toe, 1.9f, 1.5f, body(near), near)
        part(knee, ankle, Radius.KNEE, Radius.ANKLE, tint(Segment.SHIN, near), near)
        part(hip, knee, Radius.HIP, Radius.KNEE, tint(Segment.THIGH, near), near)
    }

    fun arm(near: Boolean) {
        val shoulder = if (near) s.shoulderNear else s.shoulderFar
        val elbow = if (near) s.elbowNear else s.elbowFar
        val hand = if (near) s.handNear else s.handFar
        part(elbow, hand, Radius.ELBOW * 0.95f, Radius.WRIST, tint(Segment.FOREARM, near), near)
        drawCircle(body(near), g.w(Radius.HAND), g.p(hand))
        part(shoulder, elbow, Radius.SHOULDER, Radius.ELBOW, tint(Segment.UPPER_ARM, near), near)
        drawCircle(tint(Segment.DELT, near), g.w(Radius.DELT), g.p(shoulder))
    }

    leg(near = false)
    arm(near = false)

    // --- szyja, głowa, tułów
    part(s.shoulder, lerp(s.shoulder, s.head, 0.7f), 2.4f, 2.1f, FigureColors.Near, outlined = false)
    drawCircle(FigureColors.Near, g.w(Radius.HEAD), g.p(s.head))

    val spineLen = hypot(s.shoulder.x - s.hip.x, s.shoulder.y - s.hip.y).coerceAtLeast(1e-3f)
    val ux = (s.shoulder.x - s.hip.x) / spineLen
    val uy = (s.shoulder.y - s.hip.y) / spineLen
    val (nx, ny) = if (front) {
        val lean = Math.toRadians(pose.lean.toDouble())
        cos(lean).toFloat() to sin(lean).toFloat()
    } else {
        -uy to ux // przód tułowia
    }
    fun torsoPoint(along: Float, offset: Float) = g.p(
        s.hip.x + ux * along * spineLen + nx * offset,
        s.hip.y + uy * along * spineLen + ny * offset,
    )
    val profile = if (front) FrontProfile else SideProfile
    val outlinePoints = profile.map { (a, f, _) -> torsoPoint(a, f) } +
        profile.reversed().map { (a, _, b) -> torsoPoint(a, -b) }
    val torso = Path().apply { smoothClosed(outlinePoints) }
    drawPath(torso, if (Segment.TORSO in hl) lime else FigureColors.Near)

    val zones = buildList {
        if (Segment.CHEST in hl) add(if (front) floatArrayOf(0.58f, 1.1f, -12f, 12f) else floatArrayOf(0.55f, 1.1f, 0f, 12f))
        if (Segment.BACK in hl && !front) add(floatArrayOf(0.32f, 1.1f, -12f, 0f))
        if (Segment.CORE in hl) add(if (front) floatArrayOf(0f, 0.56f, -12f, 12f) else floatArrayOf(0.02f, 0.58f, 0f, 12f))
        if (Segment.GLUTES in hl && !front) add(floatArrayOf(-0.3f, 0.24f, -12f, 0.3f))
    }
    if (zones.isNotEmpty()) {
        clipPath(torso) {
            zones.forEach { (a0, a1, o0, o1) ->
                path.reset()
                val c1 = torsoPoint(a0, o0)
                val c2 = torsoPoint(a1, o0)
                val c3 = torsoPoint(a1, o1)
                val c4 = torsoPoint(a0, o1)
                path.moveTo(c1.x, c1.y)
                path.lineTo(c2.x, c2.y)
                path.lineTo(c3.x, c3.y)
                path.lineTo(c4.x, c4.y)
                path.close()
                drawPath(path, lime)
            }
        }
    }

    leg(near = true)
    arm(near = true)

    // --- sprzęt w dłoniach / przy stawach
    val light = FigureColors.EquipmentLight
    fun bell(at: Offset) {
        val c = g.p(at)
        drawRoundRect(light, Offset(c.x - g.w(4f), c.y - g.w(3.5f)), Size(g.w(8f), g.w(7f)), CornerRadius(g.w(1.8f)))
    }
    m.props.forEach { prop ->
        when (prop) {
            Prop.Dumbbells -> {
                bell(s.handNear)
                bell(s.handFar)
            }
            Prop.DumbbellNear -> bell(s.handNear)
            Prop.HammerDumbbells -> listOf(s.elbowNear to s.handNear, s.elbowFar to s.handFar).forEach { (elbow, hand) ->
                val c = g.p(hand)
                val angle = Math.toDegrees(atan2((hand.y - elbow.y).toDouble(), (hand.x - elbow.x).toDouble())).toFloat() + 90f
                rotate(angle, pivot = c) {
                    drawRoundRect(light, Offset(c.x - g.w(7f), c.y - g.w(2.4f)), Size(g.w(14f), g.w(4.8f)), CornerRadius(g.w(1.6f)))
                }
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
            is Prop.LeverToAnkle -> {
                drawLine(light, g.p(prop.pivot), g.p(s.ankleNear), g.w(2.6f), StrokeCap.Round)
                drawCircle(light, g.w(2.6f), g.p(prop.pivot))
            }
            else -> Unit
        }
    }
}
