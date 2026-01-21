package com.example.myapplication.state.sideeffects

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
 * EXAMPLE 2: LaunchedEffect with Key - Re-run when key changes
 * ============================================================
 *
 * When you pass a key to LaunchedEffect, it will:
 * - Cancel the previous effect
 * - Restart with the new key value
 *
 * KEY POINTS:
 * - Effect restarts when any key changes
 * - Previous coroutine is cancelled automatically
 * - Useful for fetching data based on changing parameters
 *
 * USE CASES:
 * - Fetching data when userId/productId changes
 * - Updating search results when query changes
 * - Reacting to parameter changes
 */

@Composable
fun LaunchedEffectWithKeyExample() {
    var selectedUserId by remember { mutableIntStateOf(1) }
    var posts by remember { mutableStateOf<List<Post>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // Re-runs whenever selectedUserId changes
    // Previous coroutine is automatically cancelled
    LaunchedEffect(selectedUserId) {
        isLoading = true
        println("Fetching posts for user: $selectedUserId")
        posts = FakeApiService.fetchPostsByUser(selectedUserId)
        isLoading = false
    }

    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = "LaunchedEffect with Key",
            style = MaterialTheme.typography.headlineSmall
        )
        Text(
            text = "Changes user ID to re-fetch posts",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary
        )
        Spacer(modifier = Modifier.height(16.dp))

        // User selection buttons
        Row {
            for (userId in 1..3) {
                Button(
                    onClick = { selectedUserId = userId },
                    enabled = selectedUserId != userId
                ) {
                    Text("User $userId")
                }
                Spacer(modifier = Modifier.width(8.dp))
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Selected User ID: $selectedUserId",
            style = MaterialTheme.typography.labelMedium
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
            LazyColumn {
                items(posts) { post ->
                    PostCard(post = post)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}
