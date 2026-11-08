package com.gymtrack.ultra.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/**
 * Autorski zestaw ikon liniowych (24×24, kreska 2) — te same ścieżki co w makietach.
 * Bez zależności od material-icons; kolor nadaje `tint` w [androidx.compose.material3.Icon].
 */
object GtIcons {
    val Home: ImageVector by lazy { stroke("Home", "M4 11 L12 4 L20 11 V20 H14 V14 H10 V20 H4 Z") }
    val Workout: ImageVector by lazy { stroke("Workout", "M3 9 V15 M6 7 V17 M18 7 V17 M21 9 V15 M6 12 H18") }
    val Stats: ImageVector by lazy { stroke("Stats", "M4 20 V12 M10 20 V6 M16 20 V14 M22 20 H2") }
    val Trophy: ImageVector by lazy {
        stroke(
            "Trophy",
            "M7 4 H17 V9 C17 12 15 14 12 14 C9 14 7 12 7 9 Z M12 14 V18 M8 20 H16 " +
                "M7 6 H4 C4 9 5 10 7 10 M17 6 H20 C20 9 19 10 17 10",
        )
    }
    val Profile: ImageVector by lazy { stroke("Profile", "M12 12 A4 4 0 1 0 12 4 A4 4 0 1 0 12 12 Z M4 21 C5 16 19 16 20 21") }
    val Play: ImageVector by lazy { filled("Play", "M7 4 L19 12 L7 20 Z") }
    val Check: ImageVector by lazy { stroke("Check", "M5 12.5 L10 17 L19 7", width = 2.6f) }
    val Info: ImageVector by lazy { stroke("Info", "M12 3 A9 9 0 1 0 12 21 A9 9 0 1 0 12 3 Z M12 11 V16 M12 7.5 V8") }
    val ChevronRight: ImageVector by lazy { stroke("ChevronRight", "M9 5 L16 12 L9 19") }
    val ChevronLeft: ImageVector by lazy { stroke("ChevronLeft", "M15 5 L8 12 L15 19") }
    val Flame: ImageVector by lazy { stroke("Flame", "M12 3 C8 8 7 12 12 21 C17 12 16 8 12 3 Z") }
    val Clock: ImageVector by lazy { stroke("Clock", "M12 3 A9 9 0 1 0 12 21 A9 9 0 1 0 12 3 Z M12 7 V12 L15 14") }
    val Shield: ImageVector by lazy { stroke("Shield", "M12 3 L19 6 V12 C19 16 16 19 12 21 C8 19 5 16 5 12 V6 Z") }
    val Moon: ImageVector by lazy { stroke("Moon", "M20 14 A8 8 0 1 1 10 4 A6 6 0 0 0 20 14 Z") }
    val Lock: ImageVector by lazy { stroke("Lock", "M6 11 H18 V20 H6 Z M8 11 V8 A4 4 0 0 1 16 8 V11") }
    val Plus: ImageVector by lazy { stroke("Plus", "M12 5 V19 M5 12 H19", width = 2.4f) }
    val Close: ImageVector by lazy { stroke("Close", "M6 6 L18 18 M18 6 L6 18") }
    val Egg: ImageVector by lazy { stroke("Egg", "M12 3 C8 3 5 9 5 14 A7 7 0 0 0 19 14 C19 9 16 3 12 3 Z") }
    val Sparkle: ImageVector by lazy {
        stroke("Sparkle", "M12 3 L13.8 10.2 L21 12 L13.8 13.8 L12 21 L10.2 13.8 L3 12 L10.2 10.2 Z")
    }

    private fun stroke(name: String, pathData: String, width: Float = 2f): ImageVector =
        ImageVector.Builder(
            name = name,
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).addPath(
            pathData = PathParser().parsePathString(pathData).toNodes(),
            fill = null,
            stroke = SolidColor(Color.White),
            strokeLineWidth = width,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        ).build()

    private fun filled(name: String, pathData: String): ImageVector =
        ImageVector.Builder(
            name = name,
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).addPath(
            pathData = PathParser().parsePathString(pathData).toNodes(),
            fill = SolidColor(Color.White),
        ).build()
}
