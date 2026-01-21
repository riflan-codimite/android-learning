package com.example.myapplication.state.sideeffects

import kotlinx.coroutines.delay

/**
 * FAKE API DATA MODELS
 */
data class User(
    val id: Int,
    val name: String,
    val email: String,
    val company: String
)

data class Post(
    val id: Int,
    val userId: Int,
    val title: String,
    val body: String
)

/**
 * UI STATE FOR API CALLS
 */
sealed class ApiState<out T> {
    object Loading : ApiState<Nothing>()
    data class Success<T>(val data: T) : ApiState<T>()
    data class Error(val message: String) : ApiState<Nothing>()
}

/**
 * FAKE API SERVICE - Simulates network requests
 *
 * This service simulates real API calls with delays to demonstrate
 * how side effects work with asynchronous operations.
 */
object FakeApiService {

    // Simulate fetching users from API
    suspend fun fetchUsers(): List<User> {
        delay(1500) // Simulate network delay
        return listOf(
            User(1, "John Doe", "john@example.com", "Tech Corp"),
            User(2, "Jane Smith", "jane@example.com", "Design Studio"),
            User(3, "Bob Wilson", "bob@example.com", "Marketing Inc"),
            User(4, "Alice Brown", "alice@example.com", "Data Labs"),
            User(5, "Charlie Davis", "charlie@example.com", "Cloud Systems")
        )
    }

    // Simulate fetching posts for a specific user
    suspend fun fetchPostsByUser(userId: Int): List<Post> {
        delay(1000) // Simulate network delay
        return listOf(
            Post(1, userId, "Getting Started with Compose", "Jetpack Compose is Android's modern toolkit for building native UI..."),
            Post(2, userId, "Understanding Side Effects", "Side effects are operations that escape the scope of a composable..."),
            Post(3, userId, "State Management Tips", "Managing state effectively is key to building robust apps...")
        )
    }

    // Simulate fetching a single user
    suspend fun fetchUserById(userId: Int): User {
        delay(800)
        return User(userId, "User $userId", "user$userId@example.com", "Company $userId")
    }
}
