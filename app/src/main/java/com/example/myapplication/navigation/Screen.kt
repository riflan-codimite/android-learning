package com.example.myapplication.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    object Counter : Screen(
        route = "counter",
        title = "Counter",
        icon = Icons.Default.Home
    )

    object List : Screen(
        route = "list",
        title = "List",
        icon = Icons.Default.Add
    )

    object Zoom : Screen(
        route = "zoom",
        title = "Zoom",
        icon = Icons.Default.PlayArrow
    )

    object SideEffects : Screen(
        route = "side_effects",
        title = "Effects",
        icon = Icons.Default.Refresh
    )

    object UIComponents : Screen(
        route = "ui_components",
        title = "UI",
        icon = Icons.Default.Build
    )

    object Settings : Screen(
        route = "settings",
        title = "Settings",
        icon = Icons.Default.Settings
    )
}

val bottomNavItems = listOf(
    Screen.Zoom,
    Screen.Counter,
    Screen.SideEffects,
    Screen.UIComponents,
    Screen.Settings
)
