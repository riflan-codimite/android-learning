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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * ============================================================
 * EXAMPLE 6: Auto-refresh with Polling
 * ============================================================
 *
 * Using LaunchedEffect with an infinite loop to create
 * a polling mechanism that refreshes data automatically.
 *
 * KEY POINTS:
 * - Use while(true) inside LaunchedEffect for polling
 * - Coroutine is automatically cancelled when leaving composition
 * - Use delay() to control polling interval
 * - Consider battery and network usage in production
 *
 * USE CASES:
 * - Live data updates (stocks, sports scores)
 * - Real-time notifications
 * - Periodic sync with server
 * - Auto-refreshing dashboards
 */

@Composable
fun PollingExample() {
    var users by remember { mutableStateOf<List<User>>(emptyList()) }
    var lastUpdated by remember { mutableStateOf("Never") }
    var refreshCount by remember { mutableIntStateOf(0) }

    val dateFormat = remember {
        SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    }

    // Polling effect - fetches data every 10 seconds
    LaunchedEffect(Unit) {
        while (true) {
            // Fetch fresh data
            users = FakeApiService.fetchUsers()
            refreshCount++
            lastUpdated = dateFormat.format(Date())

            // Wait before next poll
            delay(10000) // Poll every 10 seconds
        }
        // Note: This coroutine is automatically cancelled
        // when the composable leaves composition
    }

    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = "Auto-refresh Polling",
            style = MaterialTheme.typography.headlineSmall
        )
        Text(
            text = "Data refreshes every 10 seconds automatically",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Status info
        Text(
            text = "⏱ Last updated: $lastUpdated",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "🔄 Refresh count: $refreshCount",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.secondary
        )
        Spacer(modifier = Modifier.height(16.dp))

        // User list
        LazyColumn {
            items(users) { user ->
                UserCard(user = user)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}
