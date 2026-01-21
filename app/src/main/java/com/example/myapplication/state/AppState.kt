package com.example.myapplication.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/**
 * Global App State - Similar to Google's JetNews sample
 * CompositionLocal allows passing data down the composition tree
 * without explicitly passing it as parameters
 */

// Data class for user preferences (global state)
data class UserPreferences(
    val username: String = "Guest",
    val isDarkMode: Boolean = false,
    val notificationsEnabled: Boolean = true
)

// Create a CompositionLocal with default value
val LocalUserPreferences = compositionLocalOf { UserPreferences() }

// State holder class (similar to Google's pattern)
class AppState(
    initialPreferences: UserPreferences = UserPreferences()
) {
    var preferences by mutableStateOf(initialPreferences)
        private set

    fun updateUsername(name: String) {
        preferences = preferences.copy(username = name)
    }

    fun toggleDarkMode() {
        preferences = preferences.copy(isDarkMode = !preferences.isDarkMode)
    }

    fun toggleNotifications() {
        preferences = preferences.copy(notificationsEnabled = !preferences.notificationsEnabled)
    }
}

// Remember the app state
@Composable
fun rememberAppState(
    initialPreferences: UserPreferences = UserPreferences()
): AppState {
    return remember { AppState(initialPreferences) }
}
