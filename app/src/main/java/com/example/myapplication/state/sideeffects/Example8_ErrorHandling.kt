package com.example.myapplication.state.sideeffects

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * ============================================================
 * EXAMPLE 8: Error Handling with Snackbar
 * ============================================================
 *
 * Using LaunchedEffect to show and auto-dismiss error messages.
 *
 * KEY POINTS:
 * - LaunchedEffect can react to error state changes
 * - Use delay() for auto-dismissing notifications
 * - Combine with try-catch for error handling
 * - Clear error state after showing message
 *
 * USE CASES:
 * - Showing error messages after failed API calls
 * - Success notifications after actions
 * - Auto-dismissing toasts/snackbars
 * - Any temporary notification
 */

@Composable
fun ErrorHandlingExample() {
    var users by remember { mutableStateOf<List<User>>(emptyList()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var simulateError by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    // Fetch data with error handling
    LaunchedEffect(simulateError) {
        isLoading = true
        errorMessage = null

        try {
            if (simulateError) {
                // Simulate a network error
                delay(1000)
                throw Exception("Network connection failed")
            } else {
                users = FakeApiService.fetchUsers()
            }
        } catch (e: Exception) {
            errorMessage = "Failed to load users: ${e.message}"
            users = emptyList()
        }

        isLoading = false
    }

    // Auto-dismiss error message after 5 seconds
    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            println("Showing error: $errorMessage")
            delay(5000) // Show for 5 seconds
            errorMessage = null // Auto-dismiss
        }
    }

    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = "Error Handling",
            style = MaterialTheme.typography.headlineSmall
        )
        Text(
            text = "Shows auto-dismissing error messages",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Toggle error simulation
        Button(
            onClick = {
                scope.launch {
                    simulateError = !simulateError
                }
            },
            enabled = !isLoading
        ) {
            Text(
                if (simulateError) "Load Successfully" else "Simulate Error"
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Error message card (auto-dismisses after 5 seconds)
        if (errorMessage != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "⚠️ Error",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = errorMessage ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "This message will auto-dismiss in 5 seconds",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f)
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Content
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (users.isNotEmpty()) {
            LazyColumn {
                items(users) { user ->
                    UserCard(user = user)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        } else if (errorMessage == null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("No users to display")
            }
        }
    }
}
