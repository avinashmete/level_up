package com.levelup.app.ui

import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf

/**
 * Minimal in-memory router. Avoids a navigation-compose dependency on iOS,
 * which is still in alpha in 2026.
 *
 * Screens are values of [Screen]. The bottom-bar pages live at the *base* of the
 * stack and are swapped via [select]. Detail screens are *pushed* on top.
 */
sealed interface Screen {
    data object Dashboard : Screen
    data object Missions : Screen
    data object Forge : Screen
    data object Profile : Screen
    data object Settings : Screen
    data object Create : Screen
    data class MissionDetail(val id: Long) : Screen
}

@Stable
class AppRouter(initial: Screen = Screen.Dashboard) {
    private val backStack = mutableStateListOf<Screen>(initial)
    private val currentState = mutableStateOf<Screen>(initial)

    val current: Screen get() = currentState.value

    /** Replace the *root* of the stack (used by the bottom bar tabs). */
    fun select(root: Screen) {
        backStack.clear()
        backStack += root
        currentState.value = root
    }

    fun push(screen: Screen) {
        backStack += screen
        currentState.value = screen
    }

    fun back(): Boolean {
        if (backStack.size <= 1) return false
        backStack.removeAt(backStack.lastIndex)
        currentState.value = backStack.last()
        return true
    }
}
