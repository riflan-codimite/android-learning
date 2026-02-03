package com.yourcompany.zoomsession

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yourcompany.zoomsession.ui.ZoomSessionScreen
import com.yourcompany.zoomsession.viewmodel.ZoomSessionViewModel
import us.zoom.sdk.ZoomVideoSDK

/**
 * Activity for hosting/joining a Zoom Video SDK session.
 *
 * Responsibilities are limited to:
 * - Extracting intent extras and configuring the ViewModel
 * - Requesting runtime permissions
 * - Registering/removing the SDK listener
 * - Hosting the Compose UI via [ZoomSessionScreen]
 *
 * All state, business logic, and SDK callbacks live in [ZoomSessionViewModel].
 */
class ZoomSessionActivity : ComponentActivity() {

    companion object {
        const val EXTRA_SESSION_NAME = "session_name"
        const val EXTRA_DISPLAY_NAME = "display_name"
        const val EXTRA_SESSION_PASSWORD = "session_password"
        const val EXTRA_JWT_TOKEN = "jwt_token"
        const val EXTRA_IS_HOST = "is_host"
    }

    private val requiredPermissions = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO
    )

    private var viewModelRef: ZoomSessionViewModel? = null

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.all { it.value }) {
            viewModelRef?.joinZoomSession()
        } else {
            Toast.makeText(this, "Permissions required for video call", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sessionName = intent.getStringExtra(EXTRA_SESSION_NAME) ?: ""
        val displayName = intent.getStringExtra(EXTRA_DISPLAY_NAME) ?: ""
        val sessionPassword = intent.getStringExtra(EXTRA_SESSION_PASSWORD) ?: ""
        val jwtToken = intent.getStringExtra(EXTRA_JWT_TOKEN) ?: ""
        val isHost = intent.getBooleanExtra(EXTRA_IS_HOST, true)

        if (sessionName.isEmpty() || jwtToken.isEmpty()) {
            Toast.makeText(this, "Invalid session parameters", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setContent {
            val vm: ZoomSessionViewModel = viewModel()

            // Configure once
            LaunchedEffect(Unit) {
                vm.configure(sessionName, displayName, sessionPassword, jwtToken, isHost)
                viewModelRef = vm
                ZoomVideoSDK.getInstance().addListener(vm.zoomListener)

                if (hasRequiredPermissions()) {
                    vm.joinZoomSession()
                } else {
                    permissionLauncher.launch(requiredPermissions)
                }
            }

            // Auto-remove reactions after 3 seconds
            LaunchedEffect(vm.activeReactions.value) {
                if (vm.activeReactions.value.isNotEmpty()) {
                    kotlinx.coroutines.delay(3000)
                    vm.cleanUpExpiredReactions()
                }
            }

            MaterialTheme {
                ZoomSessionScreen(
                    sessionName = vm.sessionName,
                    displayName = vm.displayName,
                    isInSession = vm.isInSession.value,
                    isMuted = vm.isMuted.value,
                    isVideoOn = vm.isVideoOn.value,
                    statusMessage = vm.statusMessage.value,
                    participantCount = vm.participantCount.value,
                    remoteParticipants = vm.remoteParticipants.value,
                    isHost = vm.isHost,
                    showChat = vm.showChat.value,
                    showWhiteboard = vm.showWhiteboard.value,
                    showTranscription = vm.showTranscription.value,
                    showSubsessions = vm.showSubsessions.value,
                    showReactions = vm.showReactions.value,
                    showWaitingRoom = vm.showWaitingRoom.value,
                    isInWaitingRoom = vm.isInWaitingRoom.value,
                    isTranscriptionEnabled = vm.isTranscriptionEnabled.value,
                    isRecording = vm.isRecording.value,
                    selectedTranscriptionLanguage = vm.selectedTranslationLanguage.value,
                    availableTranscriptionLanguages = vm.availableTranscriptionLanguages.value,
                    unreadMessageCount = vm.unreadMessageCount.value,
                    chatMessages = vm.chatMessages.value,
                    transcriptionMessages = vm.transcriptionMessages.value,
                    activeReactions = vm.activeReactions.value,
                    raisedHands = vm.raisedHands.value,
                    hostNotification = vm.hostNotification.value,
                    onDismissHostNotification = { vm.hostNotification.value = null },
                    waitingRoomUsers = vm.waitingRoomUsers.value,
                    onToggleMute = { vm.toggleMute() },
                    onToggleVideo = { vm.toggleVideo() },
                    onToggleChat = {
                        vm.showChat.value = !vm.showChat.value
                        if (vm.showChat.value) vm.unreadMessageCount.value = 0
                    },
                    onToggleWhiteboard = { vm.showWhiteboard.value = !vm.showWhiteboard.value },
                    onToggleTranscription = {
                        vm.showTranscription.value = !vm.showTranscription.value
                    },
                    onToggleSubsessions = { vm.showSubsessions.value = !vm.showSubsessions.value },
                    onToggleReactions = { vm.showReactions.value = !vm.showReactions.value },
                    onToggleWaitingRoom = { vm.showWaitingRoom.value = !vm.showWaitingRoom.value },
                    onToggleTranscriptionEnabled = {
                        vm.isTranscriptionEnabled.value = !vm.isTranscriptionEnabled.value
                        if (vm.isTranscriptionEnabled.value) {
                            vm.startLiveTranscription()
                        } else {
                            vm.stopLiveTranscription()
                        }
                    },
                    onToggleRecording = {
                        vm.isRecording.value = !vm.isRecording.value
                    },
                    onSendReaction = { emoji -> vm.sendReaction(emoji) },
                    onAdmitUser = { userId -> vm.admitUserFromWaitingRoom(userId) },
                    onRemoveFromWaitingRoom = { userId -> vm.removeFromWaitingRoom(userId) },
                    onAdmitAllUsers = { vm.admitAllFromWaitingRoom() },
                    onSendMessage = { vm.sendChatMessage(it) },
                    onChatReaction = { messageId, emoji -> vm.sendChatReaction(messageId, emoji) },
                    onLeaveSession = {
                        vm.leaveSession()
                        finish()
                    },
                    onSelectTranscriptionLanguage = { language -> vm.setTranscriptionLanguage(language) }
                )
            }
        }
    }

    private fun hasRequiredPermissions() = requiredPermissions.all {
        ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModelRef?.let {
            ZoomVideoSDK.getInstance().removeListener(it.zoomListener)
        }
    }
}
