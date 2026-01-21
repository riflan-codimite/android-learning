package com.example.myapplication.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.myapplication.state.sideeffects.DebounceSearchExample
import com.example.myapplication.state.sideeffects.DisposableEffectExample
import com.example.myapplication.state.sideeffects.ErrorHandlingExample
import com.example.myapplication.state.sideeffects.LaunchedEffectExample
import com.example.myapplication.state.sideeffects.LaunchedEffectWithKeyExample
import com.example.myapplication.state.sideeffects.PollingExample
import com.example.myapplication.state.sideeffects.RememberCoroutineScopeExample
import com.example.myapplication.state.sideeffects.SideEffectExample

/**
 * Side Effects Examples Screen
 *
 * This screen displays all side effect examples in a navigable list.
 * Users can tap on each example to see it in action.
 */

data class SideEffectItem(
    val id: Int,
    val title: String,
    val description: String,
    val content: @Composable () -> Unit
)

@Preview(showBackground = true)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SideEffectsScreen() {
    var selectedExample by remember { mutableStateOf<SideEffectItem?>(null) }

    val examples = remember {
        listOf(
            SideEffectItem(
                id = 1,
                title = "LaunchedEffect",
                description = "Fetch data when screen loads",
                content = { LaunchedEffectExample() }
            ),
            SideEffectItem(
                id = 2,
                title = "LaunchedEffect with Key",
                description = "Re-fetch when parameter changes",
                content = { LaunchedEffectWithKeyExample() }
            ),
            SideEffectItem(
                id = 3,
                title = "SideEffect",
                description = "Analytics tracking on recomposition",
                content = { SideEffectExample() }
            ),
            SideEffectItem(
                id = 4,
                title = "DisposableEffect",
                description = "Setup & cleanup (WebSocket simulation)",
                content = { DisposableEffectExample() }
            ),
            SideEffectItem(
                id = 5,
                title = "rememberCoroutineScope",
                description = "Launch coroutines from button clicks",
                content = { RememberCoroutineScopeExample() }
            ),
            SideEffectItem(
                id = 6,
                title = "Polling",
                description = "Auto-refresh every 10 seconds",
                content = { PollingExample() }
            ),
            SideEffectItem(
                id = 7,
                title = "Debounce Search",
                description = "Search with 500ms debounce delay",
                content = { DebounceSearchExample() }
            ),
            SideEffectItem(
                id = 8,
                title = "Error Handling",
                description = "Auto-dismissing error messages",
                content = { ErrorHandlingExample() }
            )
        )
    }

    if (selectedExample != null) {
        // Show selected example
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(selectedExample!!.title) },
                    navigationIcon = {
                        IconButton(onClick = { selectedExample = null }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                selectedExample!!.content()
            }
        }
    } else {
        // Show examples list
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                text = "Side Effects",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Learn Compose side effects with examples",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary
            )
            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(examples) { example ->
                    ExampleCard(
                        example = example,
                        onClick = { selectedExample = example }
                    )
                }
            }
        }
    }
}


@Composable
private fun ExampleCard(
    example: SideEffectItem,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Example number badge
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Text(
                        text = "${example.id}",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = example.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = example.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Run example",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}
