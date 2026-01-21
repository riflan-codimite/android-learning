package com.example.myapplication.state.sideeffects

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * ============================================================
 * EXAMPLE 3: SideEffect - Run after every recomposition
 * ============================================================
 *
 * SideEffect runs after every successful recomposition.
 *
 * KEY POINTS:
 * - Runs synchronously after composition completes
 * - Cannot contain suspend functions
 * - Runs on EVERY recomposition (be careful with performance)
 * - Good for syncing Compose state with non-Compose code
 *
 * USE CASES:
 * - Analytics tracking
 * - Logging state changes
 * - Syncing with external libraries
 * - Updating non-Compose UI elements
 */

@Composable
fun SideEffectExample() {
    var users by remember { mutableStateOf<List<User>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var recompositionCount by remember { mutableIntStateOf(0) }

    // Fetch data
    LaunchedEffect(Unit) {
        users = FakeApiService.fetchUsers()
        isLoading = false
    }

    // SideEffect runs after every successful recomposition
    // Use for non-suspend side effects like logging or analytics
    SideEffect {
        recompositionCount++

        // Simulate analytics tracking
        if (!isLoading) {
            println("Analytics Event: Displayed ${users.size} users")
            println("Recomposition count: $recompositionCount")
        }
    }

    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = "SideEffect Example",
            style = MaterialTheme.typography.headlineSmall
        )
        Text(
            text = "Tracks analytics after each recomposition",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Recomposition count: $recompositionCount",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.tertiary
        )
        Spacer(modifier = Modifier.height(16.dp))

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Text(
                text = "✓ Analytics: Logged ${users.size} users displayed",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn {
                items(users) { user ->
                    UserCard(user = user)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}
