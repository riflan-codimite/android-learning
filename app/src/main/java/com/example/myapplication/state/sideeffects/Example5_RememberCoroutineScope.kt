package com.example.myapplication.state.sideeffects

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * ============================================================
 * EXAMPLE 5: rememberCoroutineScope - Launch coroutines from callbacks
 * ============================================================
 *
 * rememberCoroutineScope provides a CoroutineScope that you can use
 * to launch coroutines from event handlers like onClick.
 *
 * KEY POINTS:
 * - Use when you need to launch coroutines from callbacks
 * - Scope is cancelled when composable leaves composition
 * - Don't use LaunchedEffect for user-triggered actions
 * - Perfect for button clicks, gestures, etc.
 *
 * USE CASES:
 * - Refresh button clicks
 * - Form submissions
 * - User-triggered API calls
 * - Any action initiated by user interaction
 *
 * DIFFERENCE FROM LaunchedEffect:
 * - LaunchedEffect: Runs automatically based on keys
 * - rememberCoroutineScope: Runs when YOU call scope.launch()
 */

@Composable
fun RememberCoroutineScopeExample() {
    var users by remember { mutableStateOf<List<User>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var refreshCount by remember { mutableStateOf(0) }

    // Get a coroutine scope for launching from callbacks
    val scope = rememberCoroutineScope()

    // Initial load with LaunchedEffect
    LaunchedEffect(Unit) {
        users = FakeApiService.fetchUsers()
        isLoading = false
    }

    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = "rememberCoroutineScope",
            style = MaterialTheme.typography.headlineSmall
        )
        Text(
            text = "Click refresh to trigger API call",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary
        )
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Users", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "Refreshed $refreshCount times",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            Button(
                onClick = {
                    // Use rememberCoroutineScope for event handlers!
                    // This is the correct way to launch coroutines from callbacks
                    scope.launch {
                        isLoading = true
                        users = FakeApiService.fetchUsers()
                        refreshCount++
                        isLoading = false
                    }
                },
                enabled = !isLoading
            ) {
                Text(if (isLoading) "Loading..." else "Refresh")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn {
                items(users) { user ->
                    UserCard(user = user)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}
