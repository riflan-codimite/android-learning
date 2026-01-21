package com.example.myapplication.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.state.AppState
import com.example.myapplication.state.LocalUserPreferences
import androidx.compose.ui.tooling.preview.Preview

/**
 * Settings Screen - Demonstrates Global State with CompositionLocal
 * Similar to Google's JetNews and Now in Android samples
 */


@Composable
fun SettingsScreen(
    appState: AppState,
    modifier: Modifier = Modifier
) {
    // Access global state from CompositionLocal
    val preferences = LocalUserPreferences.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Settings (Global State)",
            fontSize = 24.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        // User info card - reads from global state
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Welcome, ${preferences.username}!",
                    style = MaterialTheme.typography.headlineSmall
                )
                Text(
                    text = "Theme: ${if (preferences.isDarkMode) "Dark" else "Light"}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Username input - updates global state
        var usernameInput by remember { mutableStateOf(preferences.username) }
        TextField(
            value = usernameInput,
            onValueChange = {
                usernameInput = it
                appState.updateUsername(it)
            },
            label = { Text("Username") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Dark mode toggle
        SettingsSwitch(
            title = "Dark Mode",
            checked = preferences.isDarkMode,
            onCheckedChange = { appState.toggleDarkMode() }
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Notifications toggle
        SettingsSwitch(
            title = "Notifications",
            checked = preferences.notificationsEnabled,
            onCheckedChange = { appState.toggleNotifications() }
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Show current state
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Current Global State:",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("Username: ${preferences.username}")
                Text("Dark Mode: ${preferences.isDarkMode}")
                Text("Notifications: ${preferences.notificationsEnabled}")
            }
        }
    }
}


@Composable
fun SettingsSwitch(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}
