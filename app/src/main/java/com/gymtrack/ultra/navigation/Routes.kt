package com.gymtrack.ultra.navigation

import com.gymtrack.ultra.ui.components.GtIcons
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.serialization.Serializable

// Trasy typowane (Navigation Compose 2.8+). Argumenty przetrwają obrót ekranu i śmierć procesu.

@Serializable data object HomeRoute
@Serializable data object WorkoutRoute
@Serializable data object StatsRoute
@Serializable data object AchievementsRoute
@Serializable data object ProfileRoute

/** Podgląd dnia planu z techniką ćwiczeń. */
@Serializable data class PlanDayRoute(val dayId: Long)

/**
 * Zakładki dolnego paska. [label] jest krótki, żeby 5 pozycji zmieściło się na 360 dp;
 * [accessibilityLabel] czyta TalkBack.
 */
enum class TopLevelDestination(
    val route: Any,
    val label: String,
    val accessibilityLabel: String,
    val icon: ImageVector,
) {
    HOME(HomeRoute, "HOME", "Strona główna", GtIcons.Home),
    WORKOUT(WorkoutRoute, "WORKOUT", "Plan treningowy", GtIcons.Workout),
    STATS(StatsRoute, "STATS", "Statystyki", GtIcons.Stats),
    ACHIEVEMENTS(AchievementsRoute, "AWARDS", "Osiągnięcia", GtIcons.Trophy),
    PROFILE(ProfileRoute, "PROFILE", "Profil i ustawienia", GtIcons.Profile),
}
