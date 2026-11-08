package com.gymtrack.ultra.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.gymtrack.ultra.features.history.HistoryDetailScreen
import com.gymtrack.ultra.features.history.HistoryScreen
import com.gymtrack.ultra.features.nutrition.NutritionScreen
import com.gymtrack.ultra.features.complete.WorkoutCompleteScreen
import com.gymtrack.ultra.features.home.HomeScreen
import com.gymtrack.ultra.features.live.LiveWorkoutScreen
import com.gymtrack.ultra.features.profile.ProfileScreen
import com.gymtrack.ultra.features.ranks.RanksScreen
import com.gymtrack.ultra.features.statistics.StatsScreen
import com.gymtrack.ultra.features.workout.PlanDayScreen
import com.gymtrack.ultra.features.workout.WorkoutPlanScreen
import com.gymtrack.ultra.ui.components.rememberGtHaptics
import com.gymtrack.ultra.ui.theme.GtColors
import com.gymtrack.ultra.ui.theme.GtDurations
import com.gymtrack.ultra.ui.theme.LocalGtMotion
import kotlinx.coroutines.launch

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
                onOpenProtein = { navController.navigate(NutritionRoute) { launchSingleTop = true } },
            )
        }
        composable<NutritionRoute> {
            NutritionScreen(onBack = { navController.popBackStack() })
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
        composable<StatsRoute> {
            StatsScreen(onOpenHistory = { navController.navigate(HistoryRoute) { launchSingleTop = true } })
        }
        composable<HistoryRoute> {
            HistoryScreen(
                onBack = { navController.popBackStack() },
                onOpen = { id -> navController.navigate(HistoryDetailRoute(id)) },
            )
        }
        composable<HistoryDetailRoute> {
            HistoryDetailScreen(onBack = { navController.popBackStack() })
        }
        composable<ProfileRoute> { ProfileScreen() }
    }
}

@Composable
private fun GymBottomBar(
    currentDestination: NavDestination?,
    onSelect: (TopLevelDestination) -> Unit,
) {
    val haptics = rememberGtHaptics()
    Column {
        HorizontalDivider(color = GtColors.Divider)
        NavigationBar(containerColor = GtColors.Background, tonalElevation = 0.dp) {
            TopLevelDestination.entries.forEach { destination ->
                val selected = currentDestination.isOnTab(destination)
                NavigationBarItem(
                    selected = selected,
                    onClick = {
                        if (!selected) haptics.tick()
                        onSelect(destination)
                    },
                    icon = { BouncyTabIcon(destination.icon, selected) },
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
                        selectedIconColor = GtColors.Lime,
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
    if (destination == TopLevelDestination.HOME && hierarchy.any { it.hasRoute(NutritionRoute::class) }) return true
    if (destination == TopLevelDestination.STATS &&
        hierarchy.any { it.hasRoute(HistoryRoute::class) || it.hasRoute(HistoryDetailRoute::class) }
    ) {
        return true
    }
    return destination == TopLevelDestination.WORKOUT && hierarchy.any { it.hasRoute(PlanDayRoute::class) }
}

fun NavHostController.navigateToTopLevel(destination: TopLevelDestination) {
    navigate(destination.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

/** Ikona zakładki: przy wybraniu podskakuje i "ściska się" jak guma. */
@Composable
private fun BouncyTabIcon(icon: ImageVector, selected: Boolean) {
    val reduced = LocalGtMotion.current.reduced
    val scale = remember { Animatable(1f) }
    val lift = remember { Animatable(0f) }
    var previous by remember { mutableStateOf(selected) }
    LaunchedEffect(selected) {
        val becameSelected = selected && !previous
        previous = selected
        if (!becameSelected || reduced) return@LaunchedEffect
        launch {
            scale.snapTo(0.7f)
            scale.animateTo(1f, spring(dampingRatio = 0.38f, stiffness = Spring.StiffnessMedium))
        }
        lift.animateTo(-5f, tween(120))
        lift.animateTo(0f, spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMediumLow))
    }
    Icon(
        icon,
        contentDescription = null,
        modifier = Modifier.graphicsLayer {
            scaleX = scale.value
            scaleY = scale.value
            translationY = lift.value * density
        },
    )
}
