package com.gymtrack.ultra.navigation

import androidx.compose.ui.graphics.vector.ImageVector
import com.gymtrack.ultra.ui.components.GtIcons
import kotlinx.serialization.Serializable

// Trasy typowane (Navigation Compose 2.8+). Argumenty przetrwają obrót ekranu i śmierć procesu.

@Serializable data object HomeRoute
@Serializable data object WorkoutRoute
@Serializable data object RanksRoute
@Serializable data object StatsRoute
@Serializable data object ProfileRoute

/** Podgląd dnia planu z techniką ćwiczeń i przyciskiem START. */
@Serializable data class PlanDayRoute(val dayId: Long)

/** Trening na żywo — zawsze dotyczy aktywnej sesji z bazy. */
@Serializable data object LiveWorkoutRoute

/** Podsumowanie, awanse i ekran serii po zakończonym treningu. */
@Serializable data class WorkoutCompleteRoute(val sessionId: Long)

enum class TopLevelDestination(
    val route: Any,
    val label: String,
    val accessibilityLabel: String,
    val icon: ImageVector,
) {
    HOME(HomeRoute, "Start", "Strona główna", GtIcons.Home),
    WORKOUT(WorkoutRoute, "Trening", "Plan treningowy", GtIcons.Workout),
    RANKS(RanksRoute, "Rangi", "Rangi i osiągnięcia", GtIcons.Trophy),
    STATS(StatsRoute, "Statystyki", "Statystyki", GtIcons.Stats),
    PROFILE(ProfileRoute, "Profil", "Profil i ustawienia", GtIcons.Profile),
}
