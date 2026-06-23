package com.levelup.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.levelup.app.AppContainer
import com.levelup.app.platform.BackupBridge
import com.levelup.app.platform.FlowToaster
import com.levelup.app.platform.Toaster
import com.levelup.app.ui.screens.CreateMissionScreen
import com.levelup.app.ui.screens.DashboardScreen
import com.levelup.app.ui.screens.MissionDetailScreen
import com.levelup.app.ui.screens.MissionsScreen
import com.levelup.app.ui.screens.ProfileScreen
import com.levelup.app.ui.screens.QuestForgeScreen
import com.levelup.app.ui.screens.SettingsScreen
import com.levelup.app.ui.theme.LevelUpTheme

/**
 * Single Compose root for both Android (`ComponentActivity.setContent { LevelUpApp(...) }`)
 * and iOS (`ComposeUIViewController { LevelUpApp(...) }`).
 *
 * The two platform-specific objects ([BackupBridge], [Toaster]) are passed in by the
 * platform host so we can keep file pickers and toasts native.
 */
@Composable
fun LevelUpApp(
    container: AppContainer,
    backupBridge: BackupBridge,
    toaster: Toaster? = null
) {
    LevelUpTheme {
        val scope = rememberCoroutineScope()
        val state = remember(container) {
            LevelUpAppState(
                scope = scope,
                repository = container.repository,
                suggester = container.questSuggester,
                preferences = container.preferences
            )
        }
        val router = remember { AppRouter() }
        val snackbar = remember { SnackbarHostState() }
        val flowToaster = remember { FlowToaster() }
        val effectiveToaster: Toaster = toaster ?: flowToaster

        LaunchedEffect(flowToaster) {
            flowToaster.messages.collect { snackbar.showSnackbar(it) }
        }

        LaunchedEffect(state) {
            state.events.collect { event ->
                when (event) {
                    is UiEvent.Completed -> {
                        val r = event.result
                        val parts = buildList {
                            add("+${r.xpGained} XP")
                            if (r.statGained > 0) add("+${r.statGained} ${r.statKey.emoji}")
                            if (r.leveledUp) add("LEVEL UP! L${r.newLevel}")
                            if (r.rankedUp) add("RANK UP! ${r.newRank.label}")
                            if (r.currentStreak > 1) add("Streak x${r.currentStreak}")
                            r.newAchievements.firstOrNull()?.let { add("Unlocked: ${it.title}") }
                        }
                        snackbar.showSnackbar(parts.joinToString(" · "))
                    }
                }
            }
        }

        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = { SnackbarHost(snackbar) },
            bottomBar = {
                BottomBar(current = router.current, onSelect = router::select)
            }
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
                when (val screen = router.current) {
                    Screen.Dashboard -> DashboardScreen(
                        state = state,
                        onOpenMission = { router.push(Screen.MissionDetail(it.id)) },
                        onCreate = { router.push(Screen.Create) },
                        onSeeAll = { router.select(Screen.Missions) },
                        onForge = { router.select(Screen.Forge) }
                    )
                    Screen.Missions -> MissionsScreen(
                        state = state,
                        onOpenMission = { router.push(Screen.MissionDetail(it.id)) },
                        onCreate = { router.push(Screen.Create) }
                    )
                    Screen.Forge -> QuestForgeScreen(state = state)
                    Screen.Profile -> ProfileScreen(state = state)
                    Screen.Settings -> SettingsScreen(state = state, backup = backupBridge, toaster = effectiveToaster)
                    Screen.Create -> CreateMissionScreen(state = state, onDone = { router.back() })
                    is Screen.MissionDetail -> MissionDetailScreen(
                        state = state,
                        missionId = screen.id,
                        onBack = { router.back() }
                    )
                }
            }
        }
    }
}

private data class BottomItem(val screen: Screen, val title: String, val icon: ImageVector)

private val bottomItems = listOf(
    BottomItem(Screen.Dashboard, "DASHBOARD", Icons.Outlined.Home),
    BottomItem(Screen.Missions, "QUESTS", Icons.Outlined.GpsFixed),
    BottomItem(Screen.Forge, "FORGE", Icons.Filled.AutoAwesome),
    BottomItem(Screen.Profile, "HUNTER", Icons.Filled.Person),
    BottomItem(Screen.Settings, "SYSTEM", Icons.Filled.Settings),
)

@Composable
private fun BottomBar(current: Screen, onSelect: (Screen) -> Unit) {
    NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp) {
        bottomItems.forEach { item ->
            val selected = current::class == item.screen::class
            NavigationBarItem(
                selected = selected,
                onClick = { onSelect(item.screen) },
                icon = { Icon(item.icon, contentDescription = item.title, modifier = Modifier.size(22.dp)) },
                label = { Text(item.title, style = MaterialTheme.typography.labelSmall) },
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
