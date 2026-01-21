package com.example.myapplication.state.sideeffects

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * ============================================================
 * EXAMPLE 4: DisposableEffect - Setup and Cleanup
 * ============================================================
 *
 * DisposableEffect is used when you need to clean up resources
 * when the composable leaves the composition.
 *
 * KEY POINTS:
 * - Runs setup code when entering composition
 * - Runs cleanup code (onDispose) when leaving composition
 * - Also runs cleanup + setup when keys change
 * - Similar to onStart/onStop lifecycle
 *
 * USE CASES:
 * - Opening/closing connections (WebSocket, database)
 * - Registering/unregistering listeners
 * - Starting/stopping services
 * - Resource management
 */

@Composable
fun DisposableEffectExample() {
    var connectionStatus by remember { mutableStateOf("Connecting...") }
    var liveUsers by remember { mutableStateOf<List<User>>(emptyList()) }
    var connectionLogs by remember { mutableStateOf<List<String>>(emptyList()) }

    // DisposableEffect for setup/cleanup
    // Simulates WebSocket connection lifecycle
    DisposableEffect(Unit) {
        // SETUP - runs when entering composition
        connectionStatus = "🟢 Connected"
        connectionLogs = connectionLogs + "WebSocket: Connected to server"
        println("WebSocket: Connected to live updates")

        // CLEANUP - runs when leaving composition
        onDispose {
            connectionStatus = "🔴 Disconnected"
            println("WebSocket: Disconnected from live updates")
            // In real app: close connection, release resources
        }
    }

    // Fetch initial data
    LaunchedEffect(Unit) {
        liveUsers = FakeApiService.fetchUsers()
        connectionLogs = connectionLogs + "Received ${liveUsers.size} users"
    }

    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = "DisposableEffect Example",
            style = MaterialTheme.typography.headlineSmall
        )
        Text(
            text = "Simulates WebSocket connection lifecycle",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Connection status
        Text(
            text = "Status: $connectionStatus",
            style = MaterialTheme.typography.titleMedium,
            color = if (connectionStatus.contains("Connected"))
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Connection logs
        Text(
            text = "Connection Logs:",
            style = MaterialTheme.typography.labelLarge
        )
        connectionLogs.forEach { log ->
            Text(
                text = "• $log",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(16.dp))

        // User list
        Text(
            text = "Live Users:",
            style = MaterialTheme.typography.labelLarge
        )
        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn {
            items(liveUsers) { user ->
                UserCard(user = user)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}
