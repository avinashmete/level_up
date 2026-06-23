package com.levelup.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.GpsFixed
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.levelup.app.data.LevelUpContainer
import com.levelup.app.ui.screens.CreateMissionScreen
import com.levelup.app.ui.screens.DashboardScreen
import com.levelup.app.ui.screens.MissionDetailScreen
import com.levelup.app.ui.screens.MissionsScreen
import com.levelup.app.ui.screens.ProfileScreen
import com.levelup.app.ui.screens.QuestForgeScreen
import com.levelup.app.ui.screens.SettingsScreen

private sealed class Route(val path: String, val title: String, val icon: ImageVector) {
    data object Dashboard : Route("dashboard", "DASHBOARD", Icons.Outlined.Home)
    data object Missions : Route("missions", "QUESTS", Icons.Outlined.GpsFixed)
    data object Forge : Route("forge", "FORGE", Icons.Filled.AutoAwesome)
    data object Profile : Route("profile", "HUNTER", Icons.Filled.Person)
    data object Settings : Route("settings", "SYSTEM", Icons.Filled.Settings)

    companion object {
        val bottomBar = listOf(Dashboard, Missions, Forge, Profile, Settings)
    }
}

private const val ROUTE_MISSION_DETAIL = "mission/{id}"
private const val ROUTE_CREATE = "create"

@Composable
fun LevelUpApp(container: LevelUpContainer) {
    val viewModel: LevelUpViewModel = viewModel(factory = LevelUpViewModel.factory(container))
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    // Surface completion / level-up events as snackbars
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is UiEvent.Completed -> {
                    val r = event.result
                    val parts = buildList {
                        add("+${r.xpGained} XP")
                        if (r.statGained > 0) add("+${r.statGained} ${r.statKey.emoji}")
                        if (r.leveledUp) add("LEVEL UP! L${r.newLevel}")
                        if (r.rankedUp) add("RANK UP! ${r.newRank.label}")
                        if (r.currentStreak > 1) add("Streak x${r.currentStreak}")
                        r.newAchievements.firstOrNull()?.let { add("🏅 ${it.title}") }
                    }
                    snackbarHostState.showSnackbar(parts.joinToString(" · "))
                }
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp
            ) {
                Route.bottomBar.forEach { route ->
                    val selected = currentRoute == route.path
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(route.path) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(route.icon, contentDescription = route.title, modifier = Modifier.size(22.dp)) },
                        label = { Text(route.title, style = MaterialTheme.typography.labelSmall) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            indicatorColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.TopCenter) {
            NavHost(
                navController = navController,
                startDestination = Route.Dashboard.path
            ) {
                composable(Route.Dashboard.path) {
                    DashboardScreen(
                        viewModel = viewModel,
                        onOpenMission = { navController.navigate("mission/${it.id}") },
                        onCreate = { navController.navigate(ROUTE_CREATE) },
                        onSeeAll = { navController.navigate(Route.Missions.path) },
                        onForge = { navController.navigate(Route.Forge.path) }
                    )
                }
                composable(Route.Missions.path) {
                    MissionsScreen(
                        viewModel = viewModel,
                        onOpenMission = { navController.navigate("mission/${it.id}") },
                        onCreate = { navController.navigate(ROUTE_CREATE) }
                    )
                }
                composable(Route.Forge.path) {
                    QuestForgeScreen(viewModel = viewModel)
                }
                composable(Route.Profile.path) {
                    ProfileScreen(viewModel = viewModel)
                }
                composable(Route.Settings.path) {
                    SettingsScreen(viewModel = viewModel)
                }
                composable(ROUTE_CREATE) {
                    CreateMissionScreen(
                        viewModel = viewModel,
                        onDone = { navController.popBackStack() }
                    )
                }
                composable(ROUTE_MISSION_DETAIL) { backStackEntry ->
                    val id = backStackEntry.arguments?.getString("id")?.toLongOrNull() ?: -1L
                    MissionDetailScreen(
                        viewModel = viewModel,
                        missionId = id,
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}

@Suppress("unused")
@Composable
private fun ItemSpacer() {
    Spacer(modifier = Modifier.height(12.dp))
}
