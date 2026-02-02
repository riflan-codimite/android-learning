package com.example.myapplication.screens

import android.content.Intent
import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yourcompany.zoomsession.JWTGenerator
import com.yourcompany.zoomsession.ZoomSDKHelper
import com.yourcompany.zoomsession.ZoomSessionActivity

@Preview(showBackground = true)
@Composable
fun ZoomScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // Hardcoded SDK Credentials
    val sdkKey = "bKKscQ57BbpAeytGc2gV4QhDTg0oFHZKj3IR"
    val sdkSecret = "iRRvbwgbDPnEG38yt7epYG8Vvn5vRu4foxXv"

    // Session details
    var sessionName by remember { mutableStateOf("session") }
    var displayName by remember { mutableStateOf("") }
    var sessionPassword by remember { mutableStateOf("") }
    var isHost by remember { mutableStateOf(true) }

    // Status
    var statusMessage by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    var isSDKInitialized by remember { mutableStateOf(false) }

    // Initialize SDK on first composition
    LaunchedEffect(Unit) {
        isSDKInitialized = ZoomSDKHelper.initializeSDK(context)
        if (isSDKInitialized) {
            statusMessage = "SDK initialized - Ready to join"
            isError = false
        } else {
            statusMessage = "Failed to initialize SDK"
            isError = true
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState)
    ) {
        Text(
            text = "Zoom Video SDK",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = if (isSDKInitialized) "✓ SDK Ready" else "✗ SDK Not Ready",
            fontSize = 12.sp,
            color = if (isSDKInitialized) Color(0xFF4CAF50) else Color.Red
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Session Details Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "Join a Video Session",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = sessionName,
                    onValueChange = { sessionName = it },
                    label = { Text("Session Name") },
                    placeholder = { Text("Enter session name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = { Text("Your Name") },
                    placeholder = { Text("Enter your display name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = sessionPassword,
                    onValueChange = { sessionPassword = it },
                    label = { Text("Password (Optional)") },
                    placeholder = { Text("Enter session password") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Switch(
                        checked = isHost,
                        onCheckedChange = { isHost = it }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isHost) "Join as Host" else "Join as Participant",
                        fontSize = 14.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Join Session Button
        Button(
            onClick = {
                if (sessionName.isBlank() || displayName.isBlank()) {
                    statusMessage = "Please enter session name and your name"
                    isError = true
                    return@Button
                }

                if (!isSDKInitialized) {
                    statusMessage = "SDK not initialized"
                    isError = true
                    return@Button
                }

                // Generate JWT token
                val jwtToken: String
                try {
                    jwtToken = JWTGenerator.generateJWT(
                        sdkKey = sdkKey,
                        sdkSecret = sdkSecret,
                        sessionName = sessionName,
                        roleType = if (isHost) 1 else 0
                    )
                    Log.d("ZoomScreen", "JWT generated successfully")
                } catch (e: Exception) {
                    statusMessage = "Failed to generate token: ${e.message}"
                    isError = true
                    return@Button
                }

                // Launch ZoomSessionActivity
                val intent = Intent(context, ZoomSessionActivity::class.java).apply {
                    putExtra(ZoomSessionActivity.EXTRA_SESSION_NAME, sessionName)
                    putExtra(ZoomSessionActivity.EXTRA_DISPLAY_NAME, displayName)
                    putExtra(ZoomSessionActivity.EXTRA_SESSION_PASSWORD, sessionPassword)
                    putExtra(ZoomSessionActivity.EXTRA_JWT_TOKEN, jwtToken)
                    putExtra(ZoomSessionActivity.EXTRA_IS_HOST, isHost)
                }
                context.startActivity(intent)
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = isSDKInitialized
        ) {
            Text("Join Video Session", fontSize = 16.sp)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Status Message
        if (statusMessage.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (isError) Color(0xFFFFEBEE) else Color(0xFFE8F5E9)
                )
            ) {
                Text(
                    text = statusMessage,
                    fontSize = 14.sp,
                    color = if (isError) Color.Red else Color(0xFF2E7D32),
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}
