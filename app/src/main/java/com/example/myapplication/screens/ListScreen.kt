package com.example.myapplication.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Preview(showBackground = true)
@Composable
fun ListScreen(modifier: Modifier = Modifier) {
    var isVisible by remember { mutableStateOf(true) }
    var items by remember { mutableStateOf(listOf("Item 1", "Item 2")) }

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Text(text = "List & Visibility State", fontSize = 24.sp)

        Spacer(modifier = Modifier.height(16.dp))

        // Visibility toggle example
        if (isVisible) {
            Text("I'm visible!")
        }
        Button(onClick = { isVisible = !isVisible }) {
            Text(if (isVisible) "Hide" else "Show")
        }

        Spacer(modifier = Modifier.height(24.dp))

        // List state example
        Text(text = "Dynamic List", fontSize = 18.sp)
        Button(onClick = { items = items + "Item ${items.size + 1}" }) {
            Text("Add Item")
        }

        Spacer(modifier = Modifier.height(8.dp))

        items.forEach { item ->
            Text(text = item, modifier = Modifier.padding(vertical = 4.dp))
        }

        Spacer(modifier = Modifier.height(24.dp))

        // State hoisting example
        Text(text = "State Hoisting Example", fontSize = 18.sp, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))
        Parent()
    }
}

@Composable
fun Parent() {
    var name by remember { mutableStateOf("") }
    Child(name = name, onNameChange = { name = it })
    Text("Parent sees: $name")
}

@Composable
fun Child(name: String, onNameChange: (String) -> Unit) {
    TextField(
        value = name,
        onValueChange = onNameChange,
        label = { Text("Child TextField") },
        modifier = Modifier.fillMaxWidth()
    )
}
