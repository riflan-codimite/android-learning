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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * ============================================================
 * EXAMPLE 1: LaunchedEffect - Fetch users on screen load
 * ============================================================
 *
 * LaunchedEffect is used to run suspend functions in a composable.
 *
 * KEY POINTS:
 * - Runs when the composable enters the composition
 * - Takes a "key" parameter - when key changes, effect restarts
 * - Using Unit as key means it runs only once
 * - Automatically cancelled when composable leaves composition
 *
 * USE CASES:
 * - Fetching data when screen loads
 * - Starting animations
 * - One-time initialization
 */

@Composable
fun LaunchedEffectExample() {
    var apiState by remember { mutableStateOf<ApiState<List<User>>>(ApiState.Loading) }

    // LaunchedEffect runs once when composable enters composition
    // Key = Unit means it won't restart
    LaunchedEffect(Unit) {
        apiState = ApiState.Loading
        try {
            // Fetch users from fake API
            val users = FakeApiService.fetchUsers()
            apiState = ApiState.Success(users)
        } catch (e: Exception) {
            apiState = ApiState.Error(e.message ?: "Unknown error")
        }
    }

    // UI based on state
    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = "LaunchedEffect Example",
            style = MaterialTheme.typography.headlineSmall
        )
        Text(
            text = "Fetches users when screen loads",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary
        )
        Spacer(modifier = Modifier.height(16.dp))

        when (val state = apiState) {
            is ApiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Loading users...")
                    }
                }
            }
            is ApiState.Success -> {
                LazyColumn {
                    items(state.data) { user ->
                        UserCard(user = user)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
            is ApiState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Error: ${state.message}",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
