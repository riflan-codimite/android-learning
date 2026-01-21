package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.myapplication.navigation.Screen
import com.example.myapplication.navigation.bottomNavItems
import com.example.myapplication.screens.CounterScreen
import com.example.myapplication.screens.ListScreen
import com.example.myapplication.screens.SettingsScreen
import com.example.myapplication.screens.SideEffectsScreen
import com.example.myapplication.screens.UIComponentsScreen
import com.example.myapplication.state.LocalUserPreferences
import com.example.myapplication.state.rememberAppState
import com.example.myapplication.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            // Create global app state
            val appState = rememberAppState()

            // Apply dark mode from global state
            MyApplicationTheme(darkTheme = appState.preferences.isDarkMode) {
                // Provide global state to entire app via CompositionLocal
                CompositionLocalProvider(LocalUserPreferences provides appState.preferences) {
                    MainApp(appState = appState)
                }
            }
        }
    }
}

@Composable
fun MainApp(
    appState: com.example.myapplication.state.AppState,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                bottomNavItems.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = screen.title) },
                        label = { Text(screen.title) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Counter.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Counter.route) {
                CounterScreen()
            }
            composable(Screen.List.route) {
                ListScreen()
            }
            composable(Screen.SideEffects.route) {
                SideEffectsScreen()
            }
            composable(Screen.UIComponents.route) {
                UIComponentsScreen()
            }
            composable(Screen.Settings.route) {
                SettingsScreen(appState = appState)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainAppPreview() {
    val appState = rememberAppState()
    MyApplicationTheme {
        CompositionLocalProvider(LocalUserPreferences provides appState.preferences) {
            MainApp(appState = appState)
        }
    }
}
