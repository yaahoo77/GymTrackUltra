package com.gymtrack.ultra.ui.exercise

import androidx.compose.ui.geometry.Offset
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Szkielet 2D postaci demonstracyjnej (układ 100×100, oś Y w dół, podłoga na y≈88).
 * Pozy opisujemy położeniem bioder, pochyleniem tułowia oraz celami dłoni i stóp —
 * łokcie i kolana liczy odwrotna kinematyka (IK), więc stopy zostają na ziemi, a ruch jest naturalny.
 */
internal object Rig {
    const val TORSO = 27f
    const val UPPER_ARM = 15f
    const val FOREARM = 14f
    const val THIGH = 22f
    const val SHIN = 21f
    const val FOOT = 7f
    const val HEAD_R = 5.5f
    const val NECK = 8.5f
    const val FRONT_SHOULDER = 8f
    const val FRONT_HIP = 5f
}

enum class View { SIDE, FRONT }

/** Partie podświetlane jako pracujące mięśnie. CHEST/BACK/CORE/GLUTES to strefy tułowia (przód/tył), DELT — bark. */
enum class Segment { TORSO, CHEST, BACK, CORE, GLUTES, DELT, UPPER_ARM, FOREARM, THIGH, SHIN }

data class Pose(
    val hip: Offset,
    /** Pochylenie tułowia w stopniach: 0 = pionowo, + = do przodu (w prawo), ±90 = leżenie. */
    val lean: Float,
    val handNear: Offset,
    val handFar: Offset,
    val footNear: Offset,
    val footFar: Offset,
    /** Strona zgięcia łokcia/kolana (+1 / −1). */
    val elbowNear: Float = 1f,
    val elbowFar: Float = 1f,
    val kneeNear: Float = -1f,
    val kneeFar: Float = -1f,
    /** Kąt stopy w stopniach (0 = palce w prawo). */
    val footAngle: Float = 0f,
    /** Skrócenie dalszego przedramienia (widok z przodu, przedramię "w ekran"). */
    val farForearm: Float = 1f,
)

/** Wynik IK: wszystkie stawy w układzie 100×100. */
internal data class Skeleton(
    val hip: Offset,
    val shoulder: Offset,
    val head: Offset,
    val shoulderNear: Offset,
    val shoulderFar: Offset,
    val hipNear: Offset,
    val hipFar: Offset,
    val elbowNear: Offset,
    val handNear: Offset,
    val elbowFar: Offset,
    val handFar: Offset,
    val kneeNear: Offset,
    val ankleNear: Offset,
    val kneeFar: Offset,
    val ankleFar: Offset,
)

internal fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t
internal fun lerp(a: Offset, b: Offset, t: Float) = Offset(lerp(a.x, b.x, t), lerp(a.y, b.y, t))

internal fun Pose.mix(other: Pose, t: Float): Pose = Pose(
    hip = lerp(hip, other.hip, t),
    lean = lerp(lean, other.lean, t),
    handNear = lerp(handNear, other.handNear, t),
    handFar = lerp(handFar, other.handFar, t),
    footNear = lerp(footNear, other.footNear, t),
    footFar = lerp(footFar, other.footFar, t),
    elbowNear = elbowNear,
    elbowFar = elbowFar,
    kneeNear = kneeNear,
    kneeFar = kneeFar,
    footAngle = lerp(footAngle, other.footAngle, t),
    farForearm = lerp(farForearm, other.farForearm, t),
)

/** Dwukostna IK: zwraca (staw środkowy, koniec) dla korzenia, celu, długości i strony zgięcia. */
internal fun twoBone(root: Offset, target: Offset, a: Float, b: Float, side: Float): Pair<Offset, Offset> {
    val dx = target.x - root.x
    val dy = target.y - root.y
    val raw = hypot(dx, dy).coerceAtLeast(1e-4f)
    val ux = dx / raw
    val uy = dy / raw
    val d = max(abs(a - b) + 0.01f, min(a + b - 0.01f, raw))
    val x = (a * a - b * b + d * d) / (2 * d)
    val h = sqrt(max(0f, a * a - x * x))
    val joint = Offset(root.x + ux * x - uy * h * side, root.y + uy * x + ux * h * side)
    val end = Offset(root.x + ux * d, root.y + uy * d)
    return joint to end
}

internal fun solve(p: Pose, view: View): Skeleton {
    val lean = Math.toRadians(p.lean.toDouble())
    val tdx = sin(lean).toFloat()
    val tdy = -cos(lean).toFloat()
    val shoulder = Offset(p.hip.x + tdx * Rig.TORSO, p.hip.y + tdy * Rig.TORSO)
    val head = Offset(shoulder.x + tdx * Rig.NECK, shoulder.y + tdy * Rig.NECK)

    val (shN, shF, hpN, hpF) = if (view == View.FRONT) {
        val px = cos(lean).toFloat()
        val py = sin(lean).toFloat()
        listOf(
            Offset(shoulder.x + px * Rig.FRONT_SHOULDER, shoulder.y + py * Rig.FRONT_SHOULDER),
            Offset(shoulder.x - px * Rig.FRONT_SHOULDER, shoulder.y - py * Rig.FRONT_SHOULDER),
            Offset(p.hip.x + px * Rig.FRONT_HIP, p.hip.y + py * Rig.FRONT_HIP),
            Offset(p.hip.x - px * Rig.FRONT_HIP, p.hip.y - py * Rig.FRONT_HIP),
        )
    } else {
        listOf(shoulder, shoulder, p.hip, p.hip)
    }

    val (elN, haN) = twoBone(shN, p.handNear, Rig.UPPER_ARM, Rig.FOREARM, p.elbowNear)
    val (elF, haF) = twoBone(shF, p.handFar, Rig.UPPER_ARM, Rig.FOREARM * p.farForearm, p.elbowFar)
    val (knN, anN) = twoBone(hpN, p.footNear, Rig.THIGH, Rig.SHIN, p.kneeNear)
    val (knF, anF) = twoBone(hpF, p.footFar, Rig.THIGH, Rig.SHIN, p.kneeFar)

    return Skeleton(p.hip, shoulder, head, shN, shF, hpN, hpF, elN, haN, elF, haF, knN, anN, knF, anF)
}

/** Sprzęt dookoła postaci — części statyczne i przyczepione do stawów. */
sealed interface Prop {
    data class Block(val cx: Float, val cy: Float, val w: Float, val h: Float, val angle: Float = 0f) : Prop
    data class Rail(val from: Offset, val to: Offset) : Prop
    data class Cable(val anchor: Offset) : Prop
    data object Handle : Prop
    data object Dumbbells : Prop

    /** Hantla tylko w bliższej dłoni (ćwiczenia jednorącz). */
    data object DumbbellNear : Prop

    /** Hantle chwytem młotkowym — gryf prostopadle do przedramienia. */
    data object HammerDumbbells : Prop
    data object DumbbellVertical : Prop

    /** Ramię maszyny od osi obrotu do dłoni (za postacią). */
    data class Lever(val pivot: Offset) : Prop

    /** Ramię maszyny od osi obrotu do kostki (np. prostowanie nóg). */
    data class LeverToAnkle(val pivot: Offset) : Prop
    data object PlateOnHips : Prop
    data object Roller : Prop
    data object Bar : Prop
    data class FootPlate(val angle: Float, val length: Float) : Prop
}

/** Kompletna animacja ćwiczenia: dwie pozy + tempo + sprzęt + pracujące mięśnie. */
data class ExerciseMotion(
    val view: View,
    val start: Pose,
    val end: Pose,
    val highlight: Set<Segment>,
    val props: List<Prop> = emptyList(),
    val floor: Boolean = true,
    val scale: Float = 1f,
    /** true = ruch start→koniec to faza "w górę" (szybsza), powrót wolniej; false = odwrotnie. */
    val liftFirst: Boolean = false,
    /** Ćwiczenia izometryczne (deska): delikatny "oddech" zamiast pełnego ruchu. */
    val isometric: Boolean = false,
    /** Druga strona ruchu (np. martwy robak: raz prawa ręka + lewa noga, raz odwrotnie). */
    val alternate: Pose? = null,
    /** Krótka wskazówka do fazy start → koniec. */
    val cueOut: String = "",
    /** Krótka wskazówka do powrotu. */
    val cueBack: String = "",
)
