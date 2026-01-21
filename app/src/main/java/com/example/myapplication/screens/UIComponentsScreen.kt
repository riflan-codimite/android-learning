package com.example.myapplication.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * UI Components Screen
 *
 * Showcases various Material 3 UI components in Jetpack Compose.
 */
@Preview(showBackground = true)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UIComponentsScreen() {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Header
        item {
            Text(
                text = "UI Components",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Material 3 components showcase",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary
            )
        }

        // ==================== BUTTONS ====================
        item {
            SectionTitle("Buttons")
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(onClick = { }) {
                        Text("Filled")
                    }
                    ElevatedButton(onClick = { }) {
                        Text("Elevated")
                    }
                    FilledTonalButton(onClick = { }) {
                        Text("Tonal")
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedButton(onClick = { }) {
                        Text("Outlined")
                    }
                    TextButton(onClick = { }) {
                        Text("Text")
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = { },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(Icons.Default.Favorite, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("With Icon")
                    }
                    Button(onClick = { }, enabled = false) {
                        Text("Disabled")
                    }
                }
            }
        }

        // ==================== FABs ====================
        item {
            SectionTitle("Floating Action Buttons")
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FloatingActionButton(onClick = { }) {
                    Icon(Icons.Default.Add, contentDescription = "Add")
                }
                FloatingActionButton(
                    onClick = { },
                    containerColor = MaterialTheme.colorScheme.secondary
                ) {
                    Icon(Icons.Default.Favorite, contentDescription = "Favorite")
                }
                ExtendedFloatingActionButton(
                    onClick = { },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("Create") }
                )
            }
        }

        // ==================== ICON BUTTONS ====================
        item {
            SectionTitle("Icon Buttons")
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { }) {
                    Icon(Icons.Default.Home, contentDescription = "Home")
                }
                IconButton(onClick = { }) {
                    Icon(Icons.Default.Search, contentDescription = "Search")
                }
                IconButton(onClick = { }) {
                    Icon(Icons.Default.Person, contentDescription = "Profile")
                }

                // Badge example
                BadgedBox(
                    badge = {
                        Badge { Text("5") }
                    }
                ) {
                    Icon(Icons.Default.Email, contentDescription = "Email")
                }
            }
        }

        // ==================== TEXT FIELDS ====================
        item {
            SectionTitle("Text Fields")
            var text1 by remember { mutableStateOf("") }
            var text2 by remember { mutableStateOf("") }

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                TextField(
                    value = text1,
                    onValueChange = { text1 = it },
                    label = { Text("Filled TextField") },
                    placeholder = { Text("Enter text...") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = text2,
                    onValueChange = { text2 = it },
                    label = { Text("Outlined TextField") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // ==================== SELECTION CONTROLS ====================
        item {
            SectionTitle("Selection Controls")
            var checked by remember { mutableStateOf(true) }
            var switchOn by remember { mutableStateOf(true) }
            var selectedRadio by remember { mutableIntStateOf(0) }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Checkbox
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = checked, onCheckedChange = { checked = it })
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Checkbox")
                }

                // Switch
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Switch")
                    Switch(checked = switchOn, onCheckedChange = { switchOn = it })
                }

                // Radio Buttons
                Text("Radio Buttons:", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    listOf("Option 1", "Option 2", "Option 3").forEachIndexed { index, option ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = selectedRadio == index,
                                onClick = { selectedRadio = index }
                            )
                            Text(option, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }

        // ==================== SEGMENTED BUTTONS ====================
        item {
            SectionTitle("Segmented Buttons")
            var selectedIndex by remember { mutableIntStateOf(0) }
            val options = listOf("Day", "Week", "Month")

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                options.forEachIndexed { index, label ->
                    SegmentedButton(
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                        onClick = { selectedIndex = index },
                        selected = index == selectedIndex
                    ) {
                        Text(label)
                    }
                }
            }
        }

        // ==================== SLIDERS ====================
        item {
            SectionTitle("Sliders")
            var sliderValue by remember { mutableFloatStateOf(0.5f) }
            var rangeValues by remember { mutableStateOf(0.2f..0.8f) }

            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Column {
                    Text("Slider: ${(sliderValue * 100).toInt()}%")
                    Slider(
                        value = sliderValue,
                        onValueChange = { sliderValue = it }
                    )
                }
                Column {
                    Text("Range: ${(rangeValues.start * 100).toInt()}% - ${(rangeValues.endInclusive * 100).toInt()}%")
                    RangeSlider(
                        value = rangeValues,
                        onValueChange = { rangeValues = it }
                    )
                }
            }
        }

        // ==================== CHIPS ====================
        item {
            SectionTitle("Chips")
            var filterSelected by remember { mutableStateOf(false) }
            var inputVisible by remember { mutableStateOf(true) }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                AssistChip(
                    onClick = { },
                    label = { Text("Assist") },
                    leadingIcon = { Icon(Icons.Default.Star, contentDescription = null, Modifier.size(18.dp)) }
                )
                FilterChip(
                    selected = filterSelected,
                    onClick = { filterSelected = !filterSelected },
                    label = { Text("Filter") },
                    leadingIcon = if (filterSelected) {
                        { Icon(Icons.Default.Check, contentDescription = null, Modifier.size(18.dp)) }
                    } else null
                )
                if (inputVisible) {
                    InputChip(
                        selected = false,
                        onClick = { inputVisible = false },
                        label = { Text("Input") },
                        trailingIcon = { Text("×") }
                    )
                }
            }
        }

        // ==================== CARDS ====================
        item {
            SectionTitle("Cards")
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Filled Card", style = MaterialTheme.typography.titleMedium)
                        Text("This is a basic filled card", style = MaterialTheme.typography.bodyMedium)
                    }
                }

                ElevatedCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Elevated Card", style = MaterialTheme.typography.titleMedium)
                        Text("This card has elevation shadow", style = MaterialTheme.typography.bodyMedium)
                    }
                }

                OutlinedCard(
                    modifier = Modifier.fillMaxWidth(),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Outlined Card", style = MaterialTheme.typography.titleMedium)
                        Text("This card has an outline border", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }

        // ==================== PROGRESS INDICATORS ====================
        item {
            SectionTitle("Progress Indicators")
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator()
                    CircularProgressIndicator(
                        progress = { 0.7f },
                        color = MaterialTheme.colorScheme.secondary
                    )
                    CircularProgressIndicator(
                        modifier = Modifier.size(32.dp),
                        strokeWidth = 2.dp
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Linear Progress (Indeterminate)")
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())

                    Text("Linear Progress (70%)")
                    LinearProgressIndicator(
                        progress = { 0.7f },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // ==================== AVATARS & SHAPES ====================
        item {
            SectionTitle("Avatars & Shapes")
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Circle Avatar
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Text("JD", color = Color.White, fontWeight = FontWeight.Bold)
                }

                // Rounded Square
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.secondary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = Color.White)
                }

                // Badge Avatar
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.tertiary),
                    contentAlignment = Alignment.Center
                ) {
                    Text("AB", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }

        // ==================== DIVIDERS ====================
        item {
            SectionTitle("Dividers")
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Content above divider")
                HorizontalDivider()
                Text("Content below divider")
                HorizontalDivider(thickness = 2.dp, color = MaterialTheme.colorScheme.primary)
                Text("Thick colored divider above")
            }
        }

        // ==================== COLOR PALETTE ====================
        item {
            SectionTitle("Color Palette")
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ColorRow("Primary", MaterialTheme.colorScheme.primary)
                ColorRow("Secondary", MaterialTheme.colorScheme.secondary)
                ColorRow("Tertiary", MaterialTheme.colorScheme.tertiary)
                ColorRow("Error", MaterialTheme.colorScheme.error)
                ColorRow("Surface", MaterialTheme.colorScheme.surface)
            }
        }

        // ==================== TYPOGRAPHY ====================
        item {
            SectionTitle("Typography")
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Display Large", style = MaterialTheme.typography.displayLarge)
                Text("Headline Medium", style = MaterialTheme.typography.headlineMedium)
                Text("Title Large", style = MaterialTheme.typography.titleLarge)
                Text("Body Large", style = MaterialTheme.typography.bodyLarge)
                Text("Body Medium", style = MaterialTheme.typography.bodyMedium)
                Text("Label Small", style = MaterialTheme.typography.labelSmall)
            }
        }

        // Bottom spacer
        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
private fun ColorRow(name: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(40.dp, 24.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(color)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(name, style = MaterialTheme.typography.bodyMedium)
    }
}
