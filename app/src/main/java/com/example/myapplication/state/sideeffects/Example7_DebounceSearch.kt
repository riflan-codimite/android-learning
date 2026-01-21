package com.example.myapplication.state.sideeffects

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import kotlinx.coroutines.delay

/**
 * ============================================================
 * EXAMPLE 7: Search with Debouncing
 * ============================================================
 *
 * Using LaunchedEffect with a key to implement search debouncing.
 * Debouncing prevents too many API calls by waiting until the
 * user stops typing.
 *
 * KEY POINTS:
 * - LaunchedEffect cancels previous coroutine when key changes
 * - Use delay() to wait before making API call
 * - If user types again, previous effect is cancelled
 * - Only the last search query triggers an API call
 *
 * USE CASES:
 * - Search-as-you-type
 * - Auto-complete suggestions
 * - Real-time filtering
 * - Any input that triggers expensive operations
 *
 * HOW IT WORKS:
 * 1. User types "J" → LaunchedEffect starts, waits 500ms
 * 2. User types "o" → Previous effect cancelled, new one starts
 * 3. User types "h" → Previous effect cancelled, new one starts
 * 4. User stops typing → 500ms passes → API call made with "Joh"
 */

@Composable
fun DebounceSearchExample() {
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<User>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var searchMessage by remember { mutableStateOf("Type to search users") }

    // Debounced search - waits 500ms after user stops typing
    LaunchedEffect(searchQuery) {
        if (searchQuery.isNotEmpty()) {
            isSearching = true
            searchMessage = "Waiting for you to stop typing..."

            // Debounce delay - if user types again, this coroutine
            // will be cancelled and a new one will start
            delay(500)

            searchMessage = "Searching for '$searchQuery'..."

            // Now perform the actual search
            val allUsers = FakeApiService.fetchUsers()
            searchResults = allUsers.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                it.email.contains(searchQuery, ignoreCase = true) ||
                it.company.contains(searchQuery, ignoreCase = true)
            }

            searchMessage = "Found ${searchResults.size} results"
            isSearching = false
        } else {
            searchResults = emptyList()
            searchMessage = "Type to search users"
        }
    }

    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = "Debounced Search",
            style = MaterialTheme.typography.headlineSmall
        )
        Text(
            text = "Waits 500ms after you stop typing",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Search input
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("Search users") },
            placeholder = { Text("Try: John, Jane, Tech...") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Search status
        Text(
            text = searchMessage,
            style = MaterialTheme.typography.labelMedium,
            color = if (isSearching)
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.secondary
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (isSearching) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn {
                items(searchResults) { user ->
                    UserCard(user = user)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}
