package com.gymtrack.ultra.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.gymtrack.ultra.features.complete.WorkoutCompleteScreen
import com.gymtrack.ultra.features.home.HomeScreen
import com.gymtrack.ultra.features.live.LiveWorkoutScreen
import com.gymtrack.ultra.features.profile.ProfileScreen
import com.gymtrack.ultra.features.ranks.RanksScreen
import com.gymtrack.ultra.features.statistics.StatsScreen
import com.gymtrack.ultra.features.workout.PlanDayScreen
import com.gymtrack.ultra.features.workout.WorkoutPlanScreen
import com.gymtrack.ultra.ui.theme.GtColors
import com.gymtrack.ultra.ui.theme.GtDurations
import com.gymtrack.ultra.ui.theme.LocalGtMotion

@Composable
fun GymAppScaffold() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val showBottomBar = currentDestination != null &&
        !currentDestination.hierarchy.any { it.hasRoute(LiveWorkoutRoute::class) || it.hasRoute(WorkoutCompleteRoute::class) }

    Scaffold(
        containerColor = GtColors.Background,
        bottomBar = {
            AnimatedVisibility(visible = showBottomBar, enter = slideInVertically { it }, exit = slideOutVertically { it }) {
                GymBottomBar(
                    currentDestination = currentDestination,
                    onSelect = { navController.navigateToTopLevel(it) },
                )
            }
        },
    ) { innerPadding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            GymNavHost(navController)
        }
    }
}

@Composable
private fun GymNavHost(navController: NavHostController) {
    val reduced = LocalGtMotion.current.reduced
    val enter: EnterTransition = if (reduced) {
        fadeIn(tween(GtDurations.Reduced))
    } else {
        fadeIn(tween(GtDurations.Medium)) + slideInHorizontally(tween(GtDurations.Medium)) { it / 24 }
    }
    val exit: ExitTransition = fadeOut(tween(if (reduced) GtDurations.Reduced else GtDurations.Fast))

    NavHost(
        navController = navController,
        startDestination = HomeRoute,
        enterTransition = { enter },
        exitTransition = { exit },
        popEnterTransition = { enter },
        popExitTransition = { exit },
    ) {
        composable<HomeRoute> {
            HomeScreen(
                onOpenLiveWorkout = { navController.navigate(LiveWorkoutRoute) { launchSingleTop = true } },
                onOpenPlanDay = { dayId -> navController.navigate(PlanDayRoute(dayId)) },
                onOpenRanks = { navController.navigateToTopLevel(TopLevelDestination.RANKS) },
            )
        }
        composable<WorkoutRoute> {
            WorkoutPlanScreen(onOpenDay = { dayId -> navController.navigate(PlanDayRoute(dayId)) })
        }
        composable<PlanDayRoute> {
            PlanDayScreen(
                onBack = { navController.popBackStack() },
                onStarted = { navController.navigate(LiveWorkoutRoute) { launchSingleTop = true } },
            )
        }
        composable<LiveWorkoutRoute> {
            LiveWorkoutScreen(
                onMinimize = {
                    if (!navController.popBackStack()) navController.navigateToTopLevel(TopLevelDestination.HOME)
                },
                onFinished = { sessionId ->
                    navController.navigate(WorkoutCompleteRoute(sessionId)) {
                        popUpTo(LiveWorkoutRoute) { inclusive = true }
                    }
                },
                onDiscarded = {
                    navController.navigate(HomeRoute) {
                        popUpTo(navController.graph.findStartDestination().id) { inclusive = false }
                        launchSingleTop = true
                    }
                },
            )
        }
        composable<WorkoutCompleteRoute> {
            WorkoutCompleteScreen(
                onDone = {
                    navController.navigate(HomeRoute) {
                        popUpTo(navController.graph.findStartDestination().id) { inclusive = false }
                        launchSingleTop = true
                    }
                },
            )
        }
        composable<RanksRoute> { RanksScreen() }
        composable<StatsRoute> { StatsScreen() }
        composable<ProfileRoute> { ProfileScreen() }
    }
}

@Composable
private fun GymBottomBar(
    currentDestination: NavDestination?,
    onSelect: (TopLevelDestination) -> Unit,
) {
    Column {
        HorizontalDivider(color = GtColors.Divider)
        NavigationBar(containerColor = GtColors.Background, tonalElevation = 0.dp) {
            TopLevelDestination.entries.forEach { destination ->
                val selected = currentDestination.isOnTab(destination)
                NavigationBarItem(
                    selected = selected,
                    onClick = { onSelect(destination) },
                    icon = { Icon(destination.icon, contentDescription = null) },
                    label = {
                        Text(
                            text = destination.label,
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Clip,
                        )
                    },
                    modifier = Modifier.semantics { contentDescription = destination.accessibilityLabel },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = GtColors.TextPrimary,
                        selectedTextColor = GtColors.TextPrimary,
                        indicatorColor = Color.Transparent,
                        unselectedIconColor = GtColors.TextMuted,
                        unselectedTextColor = GtColors.TextMuted,
                    ),
                )
            }
        }
    }
}

private fun NavDestination?.isOnTab(destination: TopLevelDestination): Boolean {
    if (this == null) return false
    if (hierarchy.any { it.hasRoute(destination.route::class) }) return true
    return destination == TopLevelDestination.WORKOUT && hierarchy.any { it.hasRoute(PlanDayRoute::class) }
}

fun NavHostController.navigateToTopLevel(destination: TopLevelDestination) {
    navigate(destination.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
