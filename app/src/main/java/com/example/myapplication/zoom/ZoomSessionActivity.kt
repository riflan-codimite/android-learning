package com.example.myapplication.zoom

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import us.zoom.sdk.*

// Data class for chat messages
data class ChatMessage(
    val id: String = System.currentTimeMillis().toString(),
    val messageId: String = java.util.UUID.randomUUID().toString(),
    val senderName: String,
    val message: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isFromMe: Boolean = false,
    val reactions: Map<String, List<String>> = emptyMap() // emoji -> list of userIds
)

// Data class for transcription messages
data class TranscriptionMessage(
    val id: String = System.currentTimeMillis().toString(),
    val speakerName: String,
    val originalText: String,
    val translatedText: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

// Data class for drawing paths
data class DrawingPath(
    val path: Path,
    val color: Color,
    val strokeWidth: Float
)

// Data class for reactions
data class ReactionEmoji(
    val id: String = System.currentTimeMillis().toString(),
    val emoji: String,
    val senderName: String,
    val senderId: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val isRaiseHand: Boolean = false
)

// Data class for subsessions
data class Subsession(
    val id: String,
    val name: String,
    val participantCount: Int
)

// Data class for waiting room users
data class WaitingRoomUser(
    val id: String,
    val name: String,
    val timestamp: Long = System.currentTimeMillis()
)

class ZoomSessionActivity : ComponentActivity() {

    companion object {
        const val TAG = "ZoomSessionActivity"
        const val EXTRA_SESSION_NAME = "session_name"
        const val EXTRA_DISPLAY_NAME = "display_name"
        const val EXTRA_SESSION_PASSWORD = "session_password"
        const val EXTRA_JWT_TOKEN = "jwt_token"
        const val EXTRA_IS_HOST = "is_host"
    }

    private var sessionName: String = ""
    private var displayName: String = ""
    private var sessionPassword: String = ""
    private var jwtToken: String = ""
    private var isHost: Boolean = true

    private var isInSession = mutableStateOf(false)
    private var isMuted = mutableStateOf(true)
    private var isVideoOn = mutableStateOf(false)
    private var statusMessage = mutableStateOf("Connecting...")
    private var participantCount = mutableStateOf(0)
    private var remoteParticipants = mutableStateOf(listOf<String>()) // List of participant names
    private var showChat = mutableStateOf(false)
    private var showWhiteboard = mutableStateOf(false)
    private var showTranscription = mutableStateOf(false)
    private var showSubsessions = mutableStateOf(false)
    private var showReactions = mutableStateOf(false)
    private var showWaitingRoom = mutableStateOf(false)
    private var isInWaitingRoom = mutableStateOf(false)
    private var isTranscriptionEnabled = mutableStateOf(false)
    private var isRecording = mutableStateOf(false)
    private var selectedSpokenLanguage = mutableStateOf("English") // Language user is speaking
    private var selectedTranslationLanguage = mutableStateOf("English") // Language to translate TO (captions)
    private var availableTranscriptionLanguages = mutableStateOf(listOf(
        "English", "Spanish", "French", "German", "Chinese (Simplified)", "Chinese (Traditional)",
        "Japanese", "Korean", "Portuguese", "Italian", "Russian", "Arabic", "Hindi",
        "Dutch", "Polish", "Vietnamese", "Ukrainian", "Turkish", "Indonesian", "Hebrew"
    ))
    private var sdkSpokenLanguages = mutableStateOf<List<Any>>(emptyList())
    private var sdkTranslationLanguages = mutableStateOf<List<Any>>(emptyList())
    private var chatMessages = mutableStateOf(listOf<ChatMessage>())
    private var transcriptionMessages = mutableStateOf(listOf<TranscriptionMessage>())
    private var activeReactions = mutableStateOf(listOf<ReactionEmoji>())
    private var raisedHands = mutableStateOf(listOf<ReactionEmoji>()) // Persistent raise hands
    private var waitingRoomUsers = mutableStateOf(listOf<WaitingRoomUser>())

    private val requiredPermissions = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO
    )

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.all { it.value }
        if (allGranted) {
            joinZoomSession()
        } else {
            Toast.makeText(this, "Permissions required for video call", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private val zoomListener = object : ZoomVideoSDKDelegate {
        override fun onSessionJoin() {
            Log.d(TAG, "Session joined successfully")
            runOnUiThread {
                // Check if participant joined before host (waiting room scenario)
                if (!isHost) {
                    val session = ZoomVideoSDK.getInstance().session
                    val hostUser = session?.remoteUsers?.find { user ->
                        session.sessionHost?.let { host -> user.userID == host.userID } ?: false
                    }

                    // If no host present and participant is not host, put in waiting room
                    if (hostUser == null && session?.sessionHost == null) {
                        isInWaitingRoom.value = true
                        statusMessage.value = "Waiting for host..."
                        return@runOnUiThread
                    }
                }

                isInSession.value = true
                isInWaitingRoom.value = false
                statusMessage.value = "Connected"
                updateParticipantCount()

                // Ensure audio is muted and video is off after joining
                ensureAudioVideoOff()
            }
        }

        override fun onSessionLeave() {
            runOnUiThread {
                isInSession.value = false
                isInWaitingRoom.value = false
                statusMessage.value = "Disconnected"
            }
        }

        override fun onSessionLeave(reason: ZoomVideoSDKSessionLeaveReason?) {
            runOnUiThread {
                isInSession.value = false
                isInWaitingRoom.value = false
                statusMessage.value = "Disconnected"
            }
        }

        override fun onError(errorCode: Int) {
            runOnUiThread {
                statusMessage.value = "Error: $errorCode"
                Toast.makeText(this@ZoomSessionActivity, "Error: $errorCode", Toast.LENGTH_SHORT).show()
            }
        }

        override fun onUserJoin(userHelper: ZoomVideoSDKUserHelper?, userList: MutableList<ZoomVideoSDKUser>?) {
            runOnUiThread {
                updateParticipantCount()

                // Check if host joined - admit waiting participants
                if (isInWaitingRoom.value && !isHost) {
                    val session = ZoomVideoSDK.getInstance().session
                    val hasHost = session?.sessionHost != null

                    if (hasHost) {
                        // Host has joined, admit from waiting room
                        isInWaitingRoom.value = false
                        isInSession.value = true
                        statusMessage.value = "Connected"
                    }
                }

                // If host, add joining users to waiting room list (for demo)
                if (isHost && userList != null) {
                    userList.forEach { user ->
                        val existingUser = waitingRoomUsers.value.find { it.id == user.userID }
                        if (existingUser == null) {
                            // In a real implementation, this would come from the SDK's waiting room API
                            // For demo, we add new participants to show the feature
                        }
                    }
                }
            }
        }

        override fun onUserLeave(userHelper: ZoomVideoSDKUserHelper?, userList: MutableList<ZoomVideoSDKUser>?) {
            runOnUiThread { updateParticipantCount() }
        }

        override fun onChatNewMessageNotify(chatHelper: ZoomVideoSDKChatHelper?, messageItem: ZoomVideoSDKChatMessage?) {
            messageItem?.let { msg ->
                runOnUiThread {
                    val myUser = ZoomVideoSDK.getInstance().session?.mySelf
                    val isFromSelf = msg.senderUser?.userID == myUser?.userID

                    if (!isFromSelf) {
                        addChatMessage(ChatMessage(
                            senderName = msg.senderUser?.userName ?: "Unknown",
                            message = msg.content ?: "",
                            isFromMe = false
                        ))
                    }
                }
            }
        }

        // Required empty implementations
        override fun onUserVideoStatusChanged(videoHelper: ZoomVideoSDKVideoHelper?, userList: MutableList<ZoomVideoSDKUser>?) {}
        override fun onUserAudioStatusChanged(audioHelper: ZoomVideoSDKAudioHelper?, userList: MutableList<ZoomVideoSDKUser>?) {}
        override fun onUserShareStatusChanged(shareHelper: ZoomVideoSDKShareHelper?, userInfo: ZoomVideoSDKUser?, status: ZoomVideoSDKShareStatus?) {}
        override fun onUserShareStatusChanged(shareHelper: ZoomVideoSDKShareHelper?, userInfo: ZoomVideoSDKUser?, shareAction: ZoomVideoSDKShareAction?) {}
        override fun onLiveStreamStatusChanged(liveStreamHelper: ZoomVideoSDKLiveStreamHelper?, status: ZoomVideoSDKLiveStreamStatus?) {}
        override fun onUserHostChanged(userHelper: ZoomVideoSDKUserHelper?, userInfo: ZoomVideoSDKUser?) {}
        override fun onUserManagerChanged(user: ZoomVideoSDKUser?) {}
        override fun onUserNameChanged(user: ZoomVideoSDKUser?) {}
        override fun onUserActiveAudioChanged(audioHelper: ZoomVideoSDKAudioHelper?, list: MutableList<ZoomVideoSDKUser>?) {}
        override fun onSessionNeedPassword(handler: ZoomVideoSDKPasswordHandler?) {}
        override fun onSessionPasswordWrong(handler: ZoomVideoSDKPasswordHandler?) {}
        override fun onMixedAudioRawDataReceived(rawData: ZoomVideoSDKAudioRawData?) {}
        override fun onOneWayAudioRawDataReceived(rawData: ZoomVideoSDKAudioRawData?, user: ZoomVideoSDKUser?) {}
        override fun onShareAudioRawDataReceived(rawData: ZoomVideoSDKAudioRawData?) {}
        override fun onCommandReceived(sender: ZoomVideoSDKUser?, strCmd: String?) {
            strCmd?.let { cmd ->
                runOnUiThread {
                    try {
                        val json = org.json.JSONObject(cmd)
                        val type = json.optString("type")

                        when (type) {
                            "chat_reaction" -> {
                                val messageId = json.getString("messageId")
                                val emoji = json.getString("emoji")
                                val odUserId = json.getString("userId")
                                updateMessageReaction(messageId, emoji, odUserId)
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to parse command: ${e.message}")
                    }
                }
            }
        }
        override fun onCommandChannelConnectResult(isSuccess: Boolean) {}
        override fun onCloudRecordingStatus(status: ZoomVideoSDKRecordingStatus?, handler: ZoomVideoSDKRecordingConsentHandler?) {}
        override fun onHostAskUnmute() {}
        override fun onInviteByPhoneStatus(status: ZoomVideoSDKPhoneStatus?, reason: ZoomVideoSDKPhoneFailedReason?) {}
        override fun onMultiCameraStreamStatusChanged(status: ZoomVideoSDKMultiCameraStreamStatus?, user: ZoomVideoSDKUser?, videoPipe: ZoomVideoSDKRawDataPipe?) {}
        override fun onMultiCameraStreamStatusChanged(status: ZoomVideoSDKMultiCameraStreamStatus?, user: ZoomVideoSDKUser?, canvas: ZoomVideoSDKVideoCanvas?) {}
        override fun onLiveTranscriptionStatus(status: ZoomVideoSDKLiveTranscriptionHelper.ZoomVideoSDKLiveTranscriptionStatus?) {
            Log.d(TAG, "=== TRANSCRIPTION STATUS CHANGED ===")
            Log.d(TAG, "Status: $status")
        }
        override fun onOriginalLanguageMsgReceived(messageItem: ZoomVideoSDKLiveTranscriptionHelper.ILiveTranscriptionMessageInfo?) {
            Log.d(TAG, "=== ORIGINAL LANGUAGE MSG RECEIVED ===")
            Log.d(TAG, "messageItem: $messageItem")
            Log.d(TAG, "content: ${messageItem?.messageContent}")
            Log.d(TAG, "speaker: ${messageItem?.speakerName}")
            messageItem?.let { msg ->
                val content = msg.messageContent ?: ""
                Log.d(TAG, "Content isNotBlank: ${content.isNotBlank()}")
                if (content.isNotBlank()) {
                    runOnUiThread {
                        Log.d(TAG, "Adding transcription message to UI: $content")
                        transcriptionMessages.value = transcriptionMessages.value + TranscriptionMessage(
                            speakerName = msg.speakerName ?: "Unknown",
                            originalText = content
                        )
                        Log.d(TAG, "Total transcription messages: ${transcriptionMessages.value.size}")
                    }
                }
            }
        }
        override fun onLiveTranscriptionMsgError(spokenLanguage: ZoomVideoSDKLiveTranscriptionHelper.ILiveTranscriptionLanguage?, transcriptLanguage: ZoomVideoSDKLiveTranscriptionHelper.ILiveTranscriptionLanguage?) {
            Log.e(TAG, "=== TRANSCRIPTION ERROR ===")
            Log.e(TAG, "spoken language: $spokenLanguage")
            Log.e(TAG, "transcript language: $transcriptLanguage")
        }
        override fun onCameraControlRequestResult(user: ZoomVideoSDKUser?, isApproved: Boolean) {}
        override fun onUserRecordingConsent(user: ZoomVideoSDKUser?) {}
        override fun onProxySettingNotification(handler: ZoomVideoSDKProxySettingHandler?) {}
        override fun onSSLCertVerifiedFailNotification(handler: ZoomVideoSDKSSLCertificateInfo?) {}
        override fun onShareNetworkStatusChanged(shareNetworkStatus: ZoomVideoSDKNetworkStatus?, isSendingShare: Boolean) {}
        override fun onShareContentChanged(shareHelper: ZoomVideoSDKShareHelper?, userInfo: ZoomVideoSDKUser?, shareAction: ZoomVideoSDKShareAction?) {}
        override fun onChatDeleteMessageNotify(chatHelper: ZoomVideoSDKChatHelper?, msgID: String?, deleteBy: ZoomVideoSDKChatMessageDeleteType?) {}
        override fun onChatPrivilegeChanged(chatHelper: ZoomVideoSDKChatHelper?, currentPrivilege: ZoomVideoSDKChatPrivilegeType?) {}
        override fun onLiveTranscriptionMsgInfoReceived(messageInfo: ZoomVideoSDKLiveTranscriptionHelper.ILiveTranscriptionMessageInfo?) {
            Log.d(TAG, "=== TRANSCRIPTION MSG INFO RECEIVED ===")
            Log.d(TAG, "messageInfo: $messageInfo")
            Log.d(TAG, "content: ${messageInfo?.messageContent}")
            Log.d(TAG, "speaker: ${messageInfo?.speakerName}")
            messageInfo?.let { msg ->
                val content = msg.messageContent ?: ""
                Log.d(TAG, "Content isNotBlank: ${content.isNotBlank()}")
                if (content.isNotBlank()) {
                    runOnUiThread {
                        Log.d(TAG, "Adding transcription message to UI: $content")
                        transcriptionMessages.value = transcriptionMessages.value + TranscriptionMessage(
                            speakerName = msg.speakerName ?: "Unknown",
                            originalText = content
                        )
                        Log.d(TAG, "Total transcription messages: ${transcriptionMessages.value.size}")
                    }
                }
            }
        }
        override fun onSpokenLanguageChanged(spokenLanguage: ZoomVideoSDKLiveTranscriptionHelper.ILiveTranscriptionLanguage?) {
            Log.d(TAG, "=== SPOKEN LANGUAGE CHANGED ===")
            Log.d(TAG, "New spoken language: $spokenLanguage")
        }

        override fun onCameraControlRequestReceived(user: ZoomVideoSDKUser?, requestType: ZoomVideoSDKCameraControlRequestType?, requestHandler: ZoomVideoSDKCameraControlRequestHandler?) {}
        override fun onUserVideoNetworkStatusChanged(status: ZoomVideoSDKNetworkStatus?, user: ZoomVideoSDKUser?) {}
        override fun onCallCRCDeviceStatusChanged(status: ZoomVideoSDKCRCCallStatus?) {}
        override fun onVideoCanvasSubscribeFail(fail_reason: ZoomVideoSDKVideoSubscribeFailReason?, pUser: ZoomVideoSDKUser?, view: ZoomVideoSDKVideoView?) {}
        override fun onShareCanvasSubscribeFail(fail_reason: ZoomVideoSDKVideoSubscribeFailReason?, pUser: ZoomVideoSDKUser?, view: ZoomVideoSDKVideoView?) {}
        override fun onShareCanvasSubscribeFail(pUser: ZoomVideoSDKUser?, view: ZoomVideoSDKVideoView?, shareAction: ZoomVideoSDKShareAction?) {}
        override fun onAnnotationHelperCleanUp(helper: ZoomVideoSDKAnnotationHelper?) {}
        override fun onAnnotationPrivilegeChange(shareOwner: ZoomVideoSDKUser?, shareAction: ZoomVideoSDKShareAction?) {}
        override fun onAnnotationToolTypeChanged(helper: ZoomVideoSDKAnnotationHelper?, view: ZoomVideoSDKVideoView?, toolType: ZoomVideoSDKAnnotationToolType?) {}
        override fun onTestMicStatusChanged(status: ZoomVideoSDKTestMicStatus?) {}
        override fun onMicSpeakerVolumeChanged(micVolume: Int, speakerVolume: Int) {}
        override fun onCalloutJoinSuccess(user: ZoomVideoSDKUser?, phoneNumber: String?) {}
        override fun onSendFileStatus(file: ZoomVideoSDKSendFile?, status: ZoomVideoSDKFileTransferStatus?) {}
        override fun onReceiveFileStatus(file: ZoomVideoSDKReceiveFile?, status: ZoomVideoSDKFileTransferStatus?) {}
        override fun onUVCCameraStatusChange(cameraId: String?, status: UVCCameraStatus?) {}
        override fun onVideoAlphaChannelStatusChanged(isAlphaModeOn: Boolean) {}
        override fun onSpotlightVideoChanged(videoHelper: ZoomVideoSDKVideoHelper?, userList: MutableList<ZoomVideoSDKUser>?) {}
        override fun onFailedToStartShare(shareHelper: ZoomVideoSDKShareHelper?, user: ZoomVideoSDKUser?) {}
        override fun onBindIncomingLiveStreamResponse(bSuccess: Boolean, streamKeyID: String?) {}
        override fun onUnbindIncomingLiveStreamResponse(bSuccess: Boolean, streamKeyID: String?) {}
        override fun onIncomingLiveStreamStatusResponse(bSuccess: Boolean, streamsStatusList: MutableList<IncomingLiveStreamStatus>?) {}
        override fun onStartIncomingLiveStreamResponse(bSuccess: Boolean, streamKeyID: String?) {}
        override fun onStopIncomingLiveStreamResponse(bSuccess: Boolean, streamKeyID: String?) {}
        override fun onShareContentSizeChanged(shareHelper: ZoomVideoSDKShareHelper?, user: ZoomVideoSDKUser?, shareAction: ZoomVideoSDKShareAction?) {}
        override fun onSubSessionStatusChanged(status: ZoomVideoSDKSubSessionStatus?, subSessionKitList: MutableList<SubSessionKit>?) {}
        override fun onSubSessionManagerHandle(manager: ZoomVideoSDKSubSessionManager?) {}
        override fun onSubSessionParticipantHandle(participant: ZoomVideoSDKSubSessionParticipant?) {}
        override fun onSubSessionUsersUpdate(subSessionKit: SubSessionKit?) {}
        override fun onBroadcastMessageFromMainSession(message: String?, userName: String?) {}
        override fun onSubSessionUserHelpRequest(handler: SubSessionUserHelpRequestHandler?) {}
        override fun onSubSessionUserHelpRequestResult(eResult: ZoomVideoSDKUserHelpRequestResult?) {}
        override fun onShareSettingChanged(setting: ZoomVideoSDKShareSetting?) {}
        override fun onStartBroadcastResponse(bSuccess: Boolean, channelID: String?) {}
        override fun onStopBroadcastResponse(bSuccess: Boolean) {}
        override fun onGetBroadcastControlStatus(bSuccess: Boolean, status: ZoomVideoSDKBroadcastControlStatus?) {}
        override fun onStreamingJoinStatusChanged(status: ZoomVideoSDKStreamingJoinStatus?) {}
        override fun onUserWhiteboardShareStatusChanged(user: ZoomVideoSDKUser?, helper: ZoomVideoSDKWhiteboardHelper?) {}
        override fun onWhiteboardExported(format: ZoomVideoSDKExportFormat?, data: ByteArray?) {}
        override fun onCanvasSnapshotTaken(user: ZoomVideoSDKUser?, isShare: Boolean) {}
        override fun onCanvasSnapshotIncompatible(user: ZoomVideoSDKUser?) {}
        override fun onMyAudioSourceTypeChanged(device: ZoomVideoSDKAudioHelper.ZoomVideoSDKAudioDevice?) {}
        override fun onUserNetworkStatusChanged(type: ZoomVideoSDKDataType?, level: ZoomVideoSDKNetworkStatus?, user: ZoomVideoSDKUser?) {}
        override fun onUserOverallNetworkStatusChanged(level: ZoomVideoSDKNetworkStatus?, user: ZoomVideoSDKUser?) {}
        override fun onAudioLevelChanged(level: Int, audioSharing: Boolean, user: ZoomVideoSDKUser?) {}
        override fun onRealTimeMediaStreamsStatus(status: RealTimeMediaStreamsStatus?) {}
        override fun onRealTimeMediaStreamsFail(failReason: RealTimeMediaStreamsFailReason?) {}
    }

    private fun addChatMessage(message: ChatMessage) {
        chatMessages.value = chatMessages.value + message
    }

    private fun sendChatMessage(message: String) {
        if (message.isBlank()) return
        addChatMessage(ChatMessage(senderName = displayName, message = message, isFromMe = true))
        try {
            ZoomVideoSDK.getInstance().chatHelper?.sendChatToAll(message)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send chat: ${e.message}")
        }
    }

    private fun updateMessageReaction(messageId: String, emoji: String, odUserId: String) {
        chatMessages.value = chatMessages.value.map { message ->
            if (message.messageId == messageId) {
                val updatedReactions = message.reactions.toMutableMap()
                val users = updatedReactions.getOrDefault(emoji, emptyList()).toMutableList()
                if (!users.contains(odUserId)) {
                    users.add(odUserId)
                    updatedReactions[emoji] = users
                }
                message.copy(reactions = updatedReactions)
            } else {
                message
            }
        }
    }

    private fun sendChatReaction(messageId: String, emoji: String) {
        val myUser = ZoomVideoSDK.getInstance().session?.mySelf
        val odUserId = myUser?.userID ?: displayName

        updateMessageReaction(messageId, emoji, odUserId)

        try {
            val reactionJson = org.json.JSONObject().apply {
                put("type", "chat_reaction")
                put("messageId", messageId)
                put("emoji", emoji)
                put("userId", odUserId)
                put("userName", displayName)
            }
            ZoomVideoSDK.getInstance().cmdChannel?.sendCommand(null, reactionJson.toString())
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send chat reaction: ${e.message}")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sessionName = intent.getStringExtra(EXTRA_SESSION_NAME) ?: ""
        displayName = intent.getStringExtra(EXTRA_DISPLAY_NAME) ?: ""
        sessionPassword = intent.getStringExtra(EXTRA_SESSION_PASSWORD) ?: ""
        jwtToken = intent.getStringExtra(EXTRA_JWT_TOKEN) ?: ""
        isHost = intent.getBooleanExtra(EXTRA_IS_HOST, true)

        if (sessionName.isEmpty() || jwtToken.isEmpty()) {
            Toast.makeText(this, "Invalid session parameters", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        ZoomVideoSDK.getInstance().addListener(zoomListener)

        setContent {
            // Auto-remove reactions after 3 seconds
            LaunchedEffect(activeReactions.value) {
                if (activeReactions.value.isNotEmpty()) {
                    kotlinx.coroutines.delay(3000)
                    val currentTime = System.currentTimeMillis()
                    activeReactions.value = activeReactions.value.filter {
                        currentTime - it.timestamp < 3000
                    }
                }
            }

            MaterialTheme {
                ZoomSessionScreen(
                    sessionName = sessionName,
                    displayName = displayName,
                    isInSession = isInSession.value,
                    isMuted = isMuted.value,
                    isVideoOn = isVideoOn.value,
                    statusMessage = statusMessage.value,
                    participantCount = participantCount.value,
                    remoteParticipants = remoteParticipants.value,
                    isHost = isHost,
                    showChat = showChat.value,
                    showWhiteboard = showWhiteboard.value,
                    showTranscription = showTranscription.value,
                    showSubsessions = showSubsessions.value,
                    showReactions = showReactions.value,
                    showWaitingRoom = showWaitingRoom.value,
                    isInWaitingRoom = isInWaitingRoom.value,
                    isTranscriptionEnabled = isTranscriptionEnabled.value,
                    isRecording = isRecording.value,
                    selectedTranscriptionLanguage = selectedTranslationLanguage.value,
                    availableTranscriptionLanguages = availableTranscriptionLanguages.value,
                    chatMessages = chatMessages.value,
                    transcriptionMessages = transcriptionMessages.value,
                    activeReactions = activeReactions.value,
                    raisedHands = raisedHands.value,
                    waitingRoomUsers = waitingRoomUsers.value,
                    onToggleMute = { toggleMute() },
                    onToggleVideo = { toggleVideo() },
                    onToggleChat = { showChat.value = !showChat.value },
                    onToggleWhiteboard = { showWhiteboard.value = !showWhiteboard.value },
                    onToggleTranscription = {
                        Log.d(TAG, "onToggleTranscription called, showTranscription: ${showTranscription.value} -> ${!showTranscription.value}")
                        showTranscription.value = !showTranscription.value
                    },
                    onToggleSubsessions = { showSubsessions.value = !showSubsessions.value },
                    onToggleReactions = { showReactions.value = !showReactions.value },
                    onToggleWaitingRoom = { showWaitingRoom.value = !showWaitingRoom.value },
                    onToggleTranscriptionEnabled = {
                        Log.d(TAG, "onToggleTranscriptionEnabled called, isTranscriptionEnabled: ${isTranscriptionEnabled.value} -> ${!isTranscriptionEnabled.value}")
                        isTranscriptionEnabled.value = !isTranscriptionEnabled.value
                        if (isTranscriptionEnabled.value) {
                            Log.d(TAG, "Starting live transcription...")
                            startLiveTranscription()
                        } else {
                            Log.d(TAG, "Stopping live transcription...")
                            stopLiveTranscription()
                        }
                    },
                    onToggleRecording = {
                        isRecording.value = !isRecording.value
                    },
                    onSendReaction = { emoji ->
                        val odUserId = ZoomVideoSDK.getInstance().session?.mySelf?.userID ?: displayName
                        if (emoji == "✋") {
                            // Raise hand - toggle on/off
                            val existingHand = raisedHands.value.find { it.senderId == odUserId }
                            if (existingHand != null) {
                                // Lower hand
                                raisedHands.value = raisedHands.value.filter { it.senderId != odUserId }
                            } else {
                                // Raise hand
                                raisedHands.value = raisedHands.value + ReactionEmoji(
                                    emoji = emoji,
                                    senderName = displayName,
                                    senderId = odUserId,
                                    isRaiseHand = true
                                )
                            }
                        } else {
                            // Regular reaction - add and will auto-remove
                            activeReactions.value = activeReactions.value + ReactionEmoji(
                                emoji = emoji,
                                senderName = displayName,
                                senderId = odUserId
                            )
                        }
                    },
                    onAdmitUser = { userId -> admitUserFromWaitingRoom(userId) },
                    onRemoveFromWaitingRoom = { userId -> removeFromWaitingRoom(userId) },
                    onAdmitAllUsers = { admitAllFromWaitingRoom() },
                    onSendMessage = { sendChatMessage(it) },
                    onChatReaction = { messageId, emoji -> sendChatReaction(messageId, emoji) },
                    onLeaveSession = { leaveSession() },
                    onSelectTranscriptionLanguage = { language -> setTranscriptionLanguage(language) }
                )
            }
        }

        if (hasRequiredPermissions()) joinZoomSession() else permissionLauncher.launch(requiredPermissions)
    }

    private fun hasRequiredPermissions() = requiredPermissions.all {
        ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
    }

    private fun joinZoomSession() {
        val audioOptions = ZoomVideoSDKAudioOption().apply { connect = false; mute = true }  // Don't auto-connect audio
        val videoOptions = ZoomVideoSDKVideoOption().apply { localVideoOn = false }
        val sessionContext = ZoomVideoSDKSessionContext().apply {
            this.sessionName = this@ZoomSessionActivity.sessionName
            this.userName = displayName
            this.sessionPassword = sessionPassword
            this.token = jwtToken
            this.audioOption = audioOptions
            this.videoOption = videoOptions
        }

        // If participant (not host), show waiting room initially
        if (!isHost) {
            isInWaitingRoom.value = true
            statusMessage.value = "Joining waiting room..."
        }

        val session = ZoomVideoSDK.getInstance().joinSession(sessionContext)
        if (session != null) {
            statusMessage.value = if (isHost) "Joining..." else "Waiting for host..."
        } else {
            statusMessage.value = "Failed to join"
            isInWaitingRoom.value = false
        }
    }

    private fun toggleMute() {
        val myUser = ZoomVideoSDK.getInstance().session?.mySelf
        myUser?.let {
            val audioHelper = ZoomVideoSDK.getInstance().audioHelper
            val audioStatus = it.audioStatus

            // Check if audio is connected (audioType != AUDIO_TYPE_NONE means connected)
            val isAudioConnected = audioStatus?.audioType != ZoomVideoSDKAudioStatus.ZoomVideoSDKAudioType.ZoomVideoSDKAudioType_None

            if (isMuted.value) {
                // User wants to unmute
                if (!isAudioConnected) {
                    // Need to connect to audio first, then unmute
                    Log.d(TAG, "Audio not connected, starting audio...")
                    audioHelper?.startAudio()
                    // After starting audio, unmute
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        audioHelper?.unMuteAudio(it)
                        isMuted.value = false
                    }, 500)
                } else {
                    audioHelper?.unMuteAudio(it)
                    isMuted.value = false
                }
            } else {
                // User wants to mute
                audioHelper?.muteAudio(it)
                isMuted.value = true
            }
        }
    }

    private fun toggleVideo() {
        if (isVideoOn.value) ZoomVideoSDK.getInstance().videoHelper?.stopVideo()
        else ZoomVideoSDK.getInstance().videoHelper?.startVideo()
        isVideoOn.value = !isVideoOn.value
    }

    private fun ensureAudioVideoOff() {
        Log.d(TAG, "Ensuring audio is muted and video is off")
        forceAudioVideoOff()

        // Delayed safety check - SDK might not be fully ready immediately
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            Log.d(TAG, "Running delayed audio/video safety check")
            forceAudioVideoOff()
        }, 1000)
    }

    private fun forceAudioVideoOff() {
        try {
            val myUser = ZoomVideoSDK.getInstance().session?.mySelf

            // Ensure audio is muted
            myUser?.let {
                val audioStatus = it.audioStatus
                Log.d(TAG, "Current audio status - isMuted: ${audioStatus?.isMuted}, audioType: ${audioStatus?.audioType}")
                if (audioStatus?.isMuted == false) {
                    Log.d(TAG, "Audio is not muted, muting now...")
                    ZoomVideoSDK.getInstance().audioHelper?.muteAudio(it)
                }
            }
            isMuted.value = true

            // Ensure video is off
            val videoHelper = ZoomVideoSDK.getInstance().videoHelper
            Log.d(TAG, "Stopping video to ensure it's off")
            videoHelper?.stopVideo()
            isVideoOn.value = false

            Log.d(TAG, "Audio/Video state ensured - isMuted: ${isMuted.value}, isVideoOn: ${isVideoOn.value}")
        } catch (e: Exception) {
            Log.e(TAG, "Error ensuring audio/video off: ${e.message}")
        }
    }

    private fun startLiveTranscription() {
        Log.d(TAG, "=== START LIVE TRANSCRIPTION (English Only) ===")
        try {
            val transcriptionHelper = ZoomVideoSDK.getInstance().liveTranscriptionHelper
            Log.d(TAG, "transcriptionHelper: $transcriptionHelper")

            if (transcriptionHelper == null) {
                Log.e(TAG, "Live transcription helper is NULL - transcription not available")
                return
            }

            // Check if live transcription is available
            val canStart = transcriptionHelper.canStartLiveTranscription()
            Log.d(TAG, "canStartLiveTranscription: $canStart")

            // Get transcription status
            val status = transcriptionHelper.liveTranscriptionStatus
            Log.d(TAG, "Current transcription status: $status")

            // Log available languages for debugging
            val spokenLanguages = transcriptionHelper.availableSpokenLanguages
            Log.d(TAG, "Available SPOKEN languages count: ${spokenLanguages?.size ?: 0}")
            spokenLanguages?.forEachIndexed { index, lang ->
                Log.d(TAG, "Spoken Language [$index]: $lang")
            }

            // Simply start live transcription without setting language
            // This will use the default language (usually English)
            val result = transcriptionHelper.startLiveTranscription()
            Log.d(TAG, "startLiveTranscription result: $result")

            // Check status after starting
            val statusAfter = transcriptionHelper.liveTranscriptionStatus
            Log.d(TAG, "Transcription status AFTER start: $statusAfter")
        } catch (e: Exception) {
            Log.e(TAG, "Exception in startLiveTranscription: ${e.message}")
            e.printStackTrace()
        }
        Log.d(TAG, "=== END START LIVE TRANSCRIPTION ===")
    }

    private fun setLanguageById(
        transcriptionHelper: ZoomVideoSDKLiveTranscriptionHelper,
        language: ZoomVideoSDKLiveTranscriptionHelper.ILiveTranscriptionLanguage,
        isSpoken: Boolean
    ) {
        try {
            val langClass = language::class.java
            Log.d(TAG, "Language class: ${langClass.name}")
            Log.d(TAG, "Language methods: ${langClass.methods.filter { it.parameterCount == 0 }.map { "${it.name}:${it.returnType.simpleName}" }}")

            // Try to get the language ID using reflection
            val possibleIdMethods = listOf("getLanguageID", "getLangID", "getId", "getLanguageId", "getID")
            var langId: Int? = null

            for (methodName in possibleIdMethods) {
                try {
                    val method = langClass.getMethod(methodName)
                    val result = method.invoke(language)
                    if (result is Int) {
                        langId = result
                        Log.d(TAG, "Found language ID via $methodName: $langId")
                        break
                    } else if (result is Number) {
                        langId = result.toInt()
                        Log.d(TAG, "Found language ID (as Number) via $methodName: $langId")
                        break
                    }
                } catch (e: NoSuchMethodException) {
                    // Method doesn't exist, try next
                }
            }

            if (langId != null) {
                val setResult = if (isSpoken) {
                    transcriptionHelper.setSpokenLanguage(langId)
                } else {
                    transcriptionHelper.setTranslationLanguage(langId)
                }
                Log.d(TAG, "${if (isSpoken) "setSpokenLanguage" else "setTranslationLanguage"}($langId) result: $setResult")
            } else {
                Log.w(TAG, "Could not find language ID. Trying to log all getter methods...")
                langClass.methods.filter { it.parameterCount == 0 && it.name.startsWith("get") }.forEach { method ->
                    try {
                        val value = method.invoke(language)
                        Log.d(TAG, "  ${method.name} -> $value (${value?.javaClass?.simpleName})")
                    } catch (e: Exception) {
                        Log.d(TAG, "  ${method.name} -> ERROR: ${e.message}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in setLanguageById: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun setTranscriptionLanguage(language: String) {
        Log.d(TAG, "=== SET TRANSCRIPTION LANGUAGE ===")
        Log.d(TAG, "Previous translation language: ${selectedTranslationLanguage.value}")
        Log.d(TAG, "New translation language: $language")

        selectedTranslationLanguage.value = language

        Log.d(TAG, "isTranscriptionEnabled: ${isTranscriptionEnabled.value}")
        Log.d(TAG, "Stored SDK spoken languages count: ${sdkSpokenLanguages.value.size}")
        Log.d(TAG, "Stored SDK translation languages count: ${sdkTranslationLanguages.value.size}")

        // If transcription is already running, restart with new language
        if (isTranscriptionEnabled.value) {
            Log.d(TAG, "Transcription is running, restarting with new language...")
            stopLiveTranscription()
            // Small delay before restarting
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                startLiveTranscription()
            }, 500)
        } else {
            Log.d(TAG, "Transcription not running, language will be applied when started")
        }
        Log.d(TAG, "=== END SET TRANSCRIPTION LANGUAGE ===")
    }

    private fun stopLiveTranscription() {
        try {
            val transcriptionHelper = ZoomVideoSDK.getInstance().liveTranscriptionHelper
            transcriptionHelper?.stopLiveTranscription()
            Log.d(TAG, "Live transcription stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping live transcription: ${e.message}")
        }
    }

    private fun leaveSession() {
        ZoomVideoSDK.getInstance().leaveSession(isHost)
        finish()
    }

    private fun updateParticipantCount() {
        val remoteUsers = ZoomVideoSDK.getInstance().session?.remoteUsers
        participantCount.value = (remoteUsers?.size ?: 0) + 1
        remoteParticipants.value = remoteUsers?.mapNotNull { it.userName } ?: emptyList()
    }

    private fun admitUserFromWaitingRoom(userId: String) {
        waitingRoomUsers.value = waitingRoomUsers.value.filter { it.id != userId }
    }

    private fun removeFromWaitingRoom(userId: String) {
        waitingRoomUsers.value = waitingRoomUsers.value.filter { it.id != userId }
    }

    private fun admitAllFromWaitingRoom() {
        waitingRoomUsers.value = emptyList()
    }

    override fun onDestroy() {
        super.onDestroy()
        ZoomVideoSDK.getInstance().removeListener(zoomListener)
    }
}

// ==================== COMPOSABLES ====================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZoomSessionScreen(
    sessionName: String,
    displayName: String,
    isInSession: Boolean,
    isMuted: Boolean,
    isVideoOn: Boolean,
    statusMessage: String,
    participantCount: Int,
    remoteParticipants: List<String>,
    isHost: Boolean,
    showChat: Boolean,
    showWhiteboard: Boolean,
    showTranscription: Boolean,
    showSubsessions: Boolean,
    showReactions: Boolean,
    showWaitingRoom: Boolean,
    isInWaitingRoom: Boolean,
    isTranscriptionEnabled: Boolean,
    isRecording: Boolean,
    selectedTranscriptionLanguage: String,
    availableTranscriptionLanguages: List<String>,
    chatMessages: List<ChatMessage>,
    transcriptionMessages: List<TranscriptionMessage>,
    activeReactions: List<ReactionEmoji>,
    raisedHands: List<ReactionEmoji>,
    waitingRoomUsers: List<WaitingRoomUser>,
    onToggleMute: () -> Unit,
    onToggleVideo: () -> Unit,
    onToggleChat: () -> Unit,
    onToggleWhiteboard: () -> Unit,
    onToggleTranscription: () -> Unit,
    onToggleSubsessions: () -> Unit,
    onToggleReactions: () -> Unit,
    onToggleWaitingRoom: () -> Unit,
    onToggleTranscriptionEnabled: () -> Unit,
    onToggleRecording: () -> Unit,
    onSendReaction: (String) -> Unit,
    onAdmitUser: (String) -> Unit,
    onRemoveFromWaitingRoom: (String) -> Unit,
    onAdmitAllUsers: () -> Unit,
    onSendMessage: (String) -> Unit,
    onChatReaction: (String, String) -> Unit,
    onLeaveSession: () -> Unit,
    onSelectTranscriptionLanguage: (String) -> Unit
) {
    val chatSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val whiteboardSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val subsessionSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val reactionSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val waitingRoomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val moreSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showMore by remember { mutableStateOf(false) }
    var showParticipants by remember { mutableStateOf(false) }
    val participantsSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black)
    ) {
        // Video tile - FULL SCREEN like Zoom app
        SelfVideoTile(displayName, isVideoOn, isMuted)

        // Recording indicator
        if (isRecording) {
            Surface(
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 80.dp),
                color = Color(0xFFE53935),
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.size(10.dp).clip(CircleShape).background(Color.White)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("REC", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }

        // Raised hands overlay (persistent - top right)
        if (raisedHands.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 100.dp, end = 16.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    raisedHands.forEach { reaction ->
                        RaisedHandBubble(reaction)
                    }
                }
            }
        }

        // Active reactions overlay (animated - bottom right, floats up)
        if (activeReactions.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 200.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    activeReactions.takeLast(5).forEach { reaction ->
                        AnimatedReactionBubble(reaction)
                    }
                }
            }
        }

        // Live transcription overlay (on screen, not bottom sheet)
        // Only show when there's actual transcribed content
        if (showTranscription && isTranscriptionEnabled && transcriptionMessages.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 130.dp, start = 16.dp, end = 16.dp)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    color = Color.Black.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = transcriptionMessages.last().originalText,
                        color = Color.White,
                        fontSize = 14.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        maxLines = 3,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp)
                    )
                }
            }
        }

        // Top bar - Clean design with participant button
        Surface(
            Modifier.fillMaxWidth().align(Alignment.TopCenter),
            color = Color.Black.copy(alpha = 0.6f)
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .statusBarsPadding(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left side - Session info
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text(
                            sessionName,
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (isInSession) Color(0xFF4CAF50) else Color.Yellow)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                statusMessage,
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                // Right side - Participants button
                Surface(
                    onClick = { showParticipants = true },
                    color = Color.White.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Row(
                        Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = "Participants",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "$participantCount",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // Bottom controls - Single row with 5 buttons
        Surface(
            Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .navigationBarsPadding(),
            color = Color.Black.copy(alpha = 0.8f),
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp, horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Mute/Unmute - Mic icon
                BottomBarButton(
                    onClick = onToggleMute,
                    icon = if (isMuted) Icons.Outlined.Notifications else Icons.Filled.Notifications,
                    label = if (isMuted) "Unmute" else "Mute",
                    isActive = !isMuted,
                    activeColor = Color.White,
                    inactiveColor = Color(0xFFE53935)
                )

                // Video - Video icon
                BottomBarButton(
                    onClick = onToggleVideo,
                    icon = if (isVideoOn) Icons.Filled.PlayArrow else Icons.Outlined.PlayArrow,
                    label = if (isVideoOn) "Stop Video" else "Start Video",
                    isActive = isVideoOn,
                    activeColor = Color.White,
                    inactiveColor = Color(0xFFE53935)
                )

                // Share Screen - Screen icon
                BottomBarButton(
                    onClick = { /* Screen share functionality */ },
                    icon = Icons.Filled.Share,
                    label = "Share",
                    isActive = false,
                    activeColor = Color(0xFF4CAF50),
                    inactiveColor = Color.White
                )

                // Chat - Chat icon
                BottomBarButton(
                    onClick = onToggleChat,
                    icon = Icons.Filled.Email,
                    label = "Chat",
                    isActive = showChat,
                    activeColor = Color(0xFF0084FF),
                    inactiveColor = Color.White
                )

                // More
                BottomBarButton(
                    onClick = { showMore = true },
                    icon = Icons.Default.MoreVert,
                    label = "More",
                    isActive = false,
                    activeColor = Color.White,
                    inactiveColor = Color.White
                )
            }
        }
    }

    // More Options Bottom Sheet
    if (showMore) {
        ModalBottomSheet(
            onDismissRequest = { showMore = false },
            sheetState = moreSheetState,
            containerColor = Color(0xFF1E1E1E),
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
        ) {
            MoreOptionsBottomSheet(
                isRecording = isRecording,
                showWhiteboard = showWhiteboard,
                showTranscription = showTranscription,
                showSubsessions = showSubsessions,
                showReactions = showReactions,
                showWaitingRoom = showWaitingRoom,
                isHost = isHost,
                waitingRoomCount = waitingRoomUsers.size,
                selectedTranscriptionLanguage = selectedTranscriptionLanguage,
                availableTranscriptionLanguages = availableTranscriptionLanguages,
                onToggleRecording = { onToggleRecording(); showMore = false },
                onToggleWhiteboard = { onToggleWhiteboard(); showMore = false },
                onToggleTranscription = { onToggleTranscription(); if (!isTranscriptionEnabled) onToggleTranscriptionEnabled(); showMore = false },
                onToggleSubsessions = { onToggleSubsessions(); showMore = false },
                onToggleReactions = { onToggleReactions(); showMore = false },
                onToggleWaitingRoom = { onToggleWaitingRoom(); showMore = false },
                onLeaveSession = onLeaveSession,
                onSendReaction = { emoji -> onSendReaction(emoji); showMore = false },
                onSelectTranscriptionLanguage = onSelectTranscriptionLanguage
            )
        }
    }

    // Participants Bottom Sheet
    if (showParticipants) {
        ModalBottomSheet(
            onDismissRequest = { showParticipants = false },
            sheetState = participantsSheetState,
            containerColor = Color(0xFF1E1E1E),
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
        ) {
            ParticipantsBottomSheet(
                participantCount = participantCount,
                displayName = displayName,
                isHost = isHost,
                remoteParticipants = remoteParticipants
            )
        }
    }

    // Chat Bottom Sheet
    if (showChat) {
        ModalBottomSheet(onDismissRequest = onToggleChat, sheetState = chatSheetState, containerColor = Color(0xFF1E1E1E), shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)) {
            ChatBottomSheetContent(chatMessages, onSendMessage, onChatReaction)
        }
    }

    // Whiteboard Bottom Sheet
    if (showWhiteboard) {
        ModalBottomSheet(onDismissRequest = onToggleWhiteboard, sheetState = whiteboardSheetState, containerColor = Color(0xFF1E1E1E), shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)) {
            WhiteboardBottomSheetContent()
        }
    }

    // Subsessions Bottom Sheet
    if (showSubsessions) {
        ModalBottomSheet(onDismissRequest = onToggleSubsessions, sheetState = subsessionSheetState, containerColor = Color(0xFF1E1E1E), shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)) {
            SubsessionsBottomSheetContent(isHost)
        }
    }

    // Waiting Room Bottom Sheet (Host only)
    if (showWaitingRoom && isHost) {
        ModalBottomSheet(onDismissRequest = onToggleWaitingRoom, sheetState = waitingRoomSheetState, containerColor = Color(0xFF1E1E1E), shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)) {
            WaitingRoomBottomSheetContent(
                waitingRoomUsers = waitingRoomUsers,
                onAdmitUser = onAdmitUser,
                onRemoveUser = onRemoveFromWaitingRoom,
                onAdmitAll = onAdmitAllUsers
            )
        }
    }

    // Waiting Room Overlay (for participants waiting to be admitted)
    if (isInWaitingRoom) {
        WaitingRoomOverlay(sessionName = sessionName, onLeave = onLeaveSession)
    }
}

@Composable
fun ControlButtonM3(onClick: () -> Unit, isActive: Boolean, activeIcon: ImageVector, inactiveIcon: ImageVector, activeColor: Color, inactiveColor: Color, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        FilledIconButton(onClick = onClick, modifier = Modifier.size(52.dp), colors = IconButtonDefaults.filledIconButtonColors(containerColor = (if (isActive) activeColor else inactiveColor).copy(alpha = 0.2f)), shape = CircleShape) {
            Icon(if (isActive) activeIcon else inactiveIcon, label, tint = if (isActive) activeColor else inactiveColor, modifier = Modifier.size(26.dp))
        }
        Spacer(Modifier.height(4.dp))
        Text(label, color = Color.White.copy(alpha = 0.8f), fontSize = 10.sp, fontWeight = FontWeight.Medium)
    }
}

// ==================== BOTTOM BAR BUTTON ====================
@Composable
fun BottomBarButton(
    onClick: () -> Unit,
    icon: ImageVector,
    label: String,
    isActive: Boolean,
    activeColor: Color,
    inactiveColor: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint = if (isActive) activeColor else inactiveColor,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.height(4.dp))
        Text(
            label,
            color = if (isActive) activeColor else inactiveColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

// ==================== MORE OPTIONS BOTTOM SHEET ====================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreOptionsBottomSheet(
    isRecording: Boolean,
    showWhiteboard: Boolean,
    showTranscription: Boolean,
    showSubsessions: Boolean,
    showReactions: Boolean,
    showWaitingRoom: Boolean,
    isHost: Boolean,
    waitingRoomCount: Int,
    selectedTranscriptionLanguage: String,
    availableTranscriptionLanguages: List<String>,
    onToggleRecording: () -> Unit,
    onToggleWhiteboard: () -> Unit,
    onToggleTranscription: () -> Unit,
    onToggleSubsessions: () -> Unit,
    onToggleReactions: () -> Unit,
    onToggleWaitingRoom: () -> Unit,
    onLeaveSession: () -> Unit,
    onSendReaction: (String) -> Unit,
    onSelectTranscriptionLanguage: (String) -> Unit
) {
    val quickReactions = listOf("👍", "👏", "❤️", "😂", "😮", "🎉", "👎", "🔥")
    var showLanguageDropdown by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 32.dp)
    ) {
        // Header
        Text(
            "More Options",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
        )

        HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

        // Reactions Section
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text(
                "Reactions",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Emoji Row 1
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                quickReactions.take(4).forEach { emoji ->
                    EmojiButton(emoji = emoji, onClick = { onSendReaction(emoji) })
                }
            }

            Spacer(Modifier.height(8.dp))

            // Emoji Row 2
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                quickReactions.drop(4).forEach { emoji ->
                    EmojiButton(emoji = emoji, onClick = { onSendReaction(emoji) })
                }
            }

            Spacer(Modifier.height(12.dp))

            // Raise Hand Button
            Surface(
                onClick = { onSendReaction("✋") },
                color = Color(0xFFFF9800),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("✋", fontSize = 20.sp)
                    Spacer(Modifier.width(8.dp))
                    Text("Raise Hand", color = Color.White, fontWeight = FontWeight.Medium)
                }
            }
        }

        HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

        // Caption Language Selection
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text(
                "Caption Language",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            ExposedDropdownMenuBox(
                expanded = showLanguageDropdown,
                onExpandedChange = { showLanguageDropdown = !showLanguageDropdown }
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    color = Color.White.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            selectedTranscriptionLanguage,
                            color = Color.White,
                            fontSize = 16.sp
                        )
                        Icon(
                            if (showLanguageDropdown) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = "Select language",
                            tint = Color.White
                        )
                    }
                }

                ExposedDropdownMenu(
                    expanded = showLanguageDropdown,
                    onDismissRequest = { showLanguageDropdown = false },
                    modifier = Modifier.background(Color(0xFF2A2A2A))
                ) {
                    availableTranscriptionLanguages.forEach { language ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    language,
                                    color = if (language == selectedTranscriptionLanguage) Color(0xFF9C27B0) else Color.White
                                )
                            },
                            onClick = {
                                showLanguageDropdown = false
                            },
                            leadingIcon = if (language == selectedTranscriptionLanguage) {
                                { Icon(Icons.Default.Check, null, tint = Color(0xFF9C27B0)) }
                            } else null
                        )
                    }
                }
            }
        }

        HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

        // Options Grid
        Column(modifier = Modifier.padding(16.dp)) {
            // Row 1
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MoreOptionItem(
                    icon = Icons.Default.Star,
                    label = if (isRecording) "Stop Recording" else "Record",
                    isActive = isRecording,
                    activeColor = Color(0xFFE53935),
                    onClick = onToggleRecording
                )
                MoreOptionItem(
                    icon = Icons.Default.Edit,
                    label = "Whiteboard",
                    isActive = showWhiteboard,
                    activeColor = Color(0xFFFF9800),
                    onClick = onToggleWhiteboard
                )
                MoreOptionItem(
                    icon = Icons.Default.List,
                    label = "Captions",
                    isActive = showTranscription,
                    activeColor = Color(0xFF9C27B0),
                    onClick = onToggleTranscription
                )
            }

            Spacer(Modifier.height(20.dp))

            // Row 2
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MoreOptionItem(
                    icon = Icons.Default.AccountBox,
                    label = "Breakout Rooms",
                    isActive = showSubsessions,
                    activeColor = Color(0xFF00BCD4),
                    onClick = onToggleSubsessions
                )
                if (isHost) {
                    MoreOptionItem(
                        icon = Icons.Default.Lock,
                        label = "Waiting Room ($waitingRoomCount)",
                        isActive = showWaitingRoom,
                        activeColor = Color(0xFF607D8B),
                        onClick = onToggleWaitingRoom
                    )
                    Spacer(Modifier.width(80.dp))
                } else {
                    Spacer(Modifier.width(80.dp))
                    Spacer(Modifier.width(80.dp))
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

        // Leave button
        Surface(
            onClick = onLeaveSession,
            color = Color(0xFFE53935),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Text(
                "Leave Meeting",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(vertical = 14.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
fun MoreOptionItem(
    icon: ImageVector,
    label: String,
    isActive: Boolean,
    activeColor: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(80.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = if (isActive) activeColor.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.1f),
            modifier = Modifier.size(50.dp)
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(
                    icon,
                    contentDescription = label,
                    tint = if (isActive) activeColor else Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            label,
            color = Color.White.copy(alpha = 0.8f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            maxLines = 2
        )
    }
}

@Composable
fun EmojiButton(emoji: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = Color.White.copy(alpha = 0.1f),
        shape = CircleShape,
        modifier = Modifier.size(56.dp)
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(emoji, fontSize = 28.sp)
        }
    }
}

// ==================== PARTICIPANTS BOTTOM SHEET ====================
@Composable
fun ParticipantsBottomSheet(
    participantCount: Int,
    displayName: String,
    isHost: Boolean,
    remoteParticipants: List<String>
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(400.dp)
    ) {
        // Header
        Row(
            Modifier
                .fillMaxWidth()
                .background(Color(0xFF2196F3))
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Person, null, tint = Color.White, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(12.dp))
            Text("Participants ($participantCount)", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }

        // Participants list
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Self (You)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF4CAF50),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                displayName.take(1).uppercase(),
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(displayName, fontWeight = FontWeight.Medium, color = Color.White)
                            Spacer(Modifier.width(4.dp))
                            Text("(You)", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                            if (isHost) {
                                Spacer(Modifier.width(8.dp))
                                Surface(
                                    color = Color(0xFF2196F3),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        "Host",
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        Text("In this meeting", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
                    }
                }
            }

            // Remote participants with actual names
            items(remoteParticipants) { participantName ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF607D8B),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                participantName.take(1).uppercase(),
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(participantName, fontWeight = FontWeight.Medium, color = Color.White)
                        Text("In this meeting", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun SelfVideoTile(displayName: String, isVideoOn: Boolean, isMuted: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1a1a2e)),
        contentAlignment = Alignment.Center
    ) {
        if (isVideoOn) {
            // Show actual camera feed using Zoom SDK video view - FULL SCREEN PORTRAIT
            AndroidView(
                factory = { context ->
                    ZoomVideoSDKVideoView(context).apply {
                        // Set video aspect to Original for proper portrait display
                        ZoomVideoSDK.getInstance().session?.mySelf?.let { myUser ->
                            myUser.videoCanvas?.subscribe(
                                this,
                                ZoomVideoSDKVideoAspect.ZoomVideoSDKVideoAspect_Original
                            )
                        }
                    }
                },
                modifier = Modifier.fillMaxSize(),
                update = { view ->
                    // Re-subscribe when video state changes
                    ZoomVideoSDK.getInstance().session?.mySelf?.let { myUser ->
                        myUser.videoCanvas?.subscribe(
                            view,
                            ZoomVideoSDKVideoAspect.ZoomVideoSDKVideoAspect_Original
                        )
                    }
                },
                onRelease = { view ->
                    // Unsubscribe when view is released
                    ZoomVideoSDK.getInstance().session?.mySelf?.videoCanvas?.unSubscribe(view)
                }
            )

            // Name overlay at bottom left when video is on
            Surface(
                Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 16.dp, bottom = 200.dp),
                color = Color.Black.copy(alpha = 0.6f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Mute indicator
                    Icon(
                        if (isMuted) Icons.Default.Clear else Icons.Default.Done,
                        contentDescription = null,
                        tint = if (isMuted) Color(0xFFE53935) else Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "$displayName (You)",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        } else {
            // Show avatar when video is off - centered on full screen
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Surface(
                    modifier = Modifier.size(120.dp),
                    shape = CircleShape,
                    color = Color(0xFF4A4A6A)
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            displayName.take(1).uppercase(),
                            color = Color.White,
                            fontSize = 48.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    displayName,
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    if (isMuted) "Muted" else "Unmuted",
                    color = if (isMuted) Color(0xFFE53935) else Color(0xFF4CAF50),
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
fun StatusChip(text: String, color: Color) {
    Surface(color = color.copy(alpha = 0.2f), shape = RoundedCornerShape(16.dp)) {
        Text(text, color = color, fontSize = 11.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
    }
}

// ==================== CHAT BOTTOM SHEET ====================
@Composable
fun ChatBottomSheetContent(
    messages: List<ChatMessage>,
    onSendMessage: (String) -> Unit,
    onReactionClick: (String, String) -> Unit
) {
    var messageText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) { if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1) }

    Column(Modifier.fillMaxWidth().height(500.dp)) {
        Row(Modifier.fillMaxWidth().background(Color(0xFF0084FF)).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Email, null, tint = Color.White, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(12.dp))
            Text("Chat", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            Text("${messages.size} messages", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
        }
        Text(
            "Long press on a message to react",
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 11.sp,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
        )
        LazyColumn(state = listState, modifier = Modifier.weight(1f).fillMaxWidth().background(Color(0xFF2A2A2A)).padding(horizontal = 12.dp, vertical = 8.dp)) {
            items(messages) { message ->
                ChatMessageBubble(message, onReactionClick)
            }
        }
        Row(Modifier.fillMaxWidth().background(Color(0xFF2A2A2A)).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = messageText,
                onValueChange = { messageText = it },
                placeholder = { Text("Type a message...", color = Color.White.copy(alpha = 0.5f)) },
                modifier = Modifier.weight(1f).height(52.dp),
                shape = RoundedCornerShape(24.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFF0084FF),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.3f)
                )
            )
            Spacer(Modifier.width(8.dp))
            FilledIconButton(onClick = { if (messageText.isNotBlank()) { onSendMessage(messageText); messageText = "" } }, modifier = Modifier.size(48.dp), colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color(0xFF0084FF))) {
                Icon(Icons.Filled.Send, "Send", tint = Color.White)
            }
        }
    }
}
//@Preview(showBackground = true)
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatMessageBubble(
    message: ChatMessage,
    onReactionClick: (String, String) -> Unit
) {
    val isMe = message.isFromMe
    var showReactionPicker by remember { mutableStateOf(false) }
    val quickReactions = listOf("👍", "❤️", "😂", "😮", "👏")
    val hasReactions = message.reactions.isNotEmpty()

    Column(
        Modifier.fillMaxWidth().padding(vertical = if (hasReactions) 8.dp else 4.dp),
        horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
    ) {
        if (!isMe) {
            Text(message.senderName, color = Color(0xFF0084FF), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        }

        // Message with reactions overlay (relative positioning)
        Box {
            Card(
                colors = CardDefaults.cardColors(containerColor = if (isMe) Color(0xFF0084FF) else Color(0xFF3A3A3A)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .padding(bottom = if (hasReactions) 10.dp else 0.dp)
                    .combinedClickable(
                        onClick = { },
                        onLongClick = { showReactionPicker = true }
                    )
            ) {
                Text(
                    message.message,
                    color = Color.White,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(12.dp)
                )
            }

            // Reactions overlaid at the bottom of the message
            if (hasReactions) {
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .offset(x = 8.dp, y = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    message.reactions.forEach { (emoji, users) ->
                        Surface(
                            color = Color(0xFF3A3A3A),
                            shape = RoundedCornerShape(12.dp),
                            shadowElevation = 2.dp,
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF4A4A4A)),
                            onClick = { onReactionClick(message.messageId, emoji) }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(emoji, fontSize = 12.sp)
                                Spacer(Modifier.width(2.dp))
                                Text(users.size.toString(), fontSize = 10.sp, color = Color.White.copy(alpha = 0.7f))
                            }
                        }
                    }
                }
            }
        }

        // Reaction picker popup
        if (showReactionPicker) {
            Surface(
                color = Color(0xFF3A3A3A),
                shape = RoundedCornerShape(20.dp),
                shadowElevation = 8.dp,
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Row(modifier = Modifier.padding(8.dp)) {
                    quickReactions.forEach { emoji ->
                        Surface(
                            modifier = Modifier.size(36.dp),
                            shape = CircleShape,
                            color = Color.Transparent,
                            onClick = {
                                onReactionClick(message.messageId, emoji)
                                showReactionPicker = false
                            }
                        ) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(emoji, fontSize = 20.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==================== WHITEBOARD BOTTOM SHEET ====================
@Composable
fun WhiteboardBottomSheetContent() {
    var paths by remember { mutableStateOf(listOf<DrawingPath>()) }
    var currentPath by remember { mutableStateOf<Path?>(null) }
    var selectedColor by remember { mutableStateOf(Color.Black) }
    val colors = listOf(Color.Black, Color.Red, Color.Blue, Color.Green, Color(0xFFFF9800), Color(0xFF9C27B0))

    Column(Modifier.fillMaxWidth().height(500.dp)) {
        Row(Modifier.fillMaxWidth().background(Color(0xFFFF9800)).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Edit, null, tint = Color.White, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(12.dp))
            Text("Whiteboard", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            IconButton(onClick = { paths = emptyList() }) { Icon(Icons.Default.Delete, "Clear", tint = Color.White) }
        }
        Row(Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.Center) {
            colors.forEach { color ->
                Surface(
                    modifier = Modifier.size(36.dp).padding(4.dp).clip(CircleShape),
                    color = color,
                    border = if (selectedColor == color) androidx.compose.foundation.BorderStroke(2.dp, Color.White) else null,
                    onClick = { selectedColor = color }
                ) {}
            }
        }
        Canvas(
            modifier = Modifier.weight(1f).fillMaxWidth().background(Color.White).pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset -> currentPath = Path().apply { moveTo(offset.x, offset.y) } },
                    onDrag = { change, _ -> currentPath?.lineTo(change.position.x, change.position.y) },
                    onDragEnd = { currentPath?.let { paths = paths + DrawingPath(it, selectedColor, 5f) }; currentPath = null }
                )
            }
        ) {
            paths.forEach { drawPath(it.path, it.color, style = Stroke(width = it.strokeWidth, cap = StrokeCap.Round)) }
            currentPath?.let { drawPath(it, selectedColor, style = Stroke(width = 5f, cap = StrokeCap.Round)) }
        }
    }
}

// ==================== TRANSCRIPTION BOTTOM SHEET ====================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranscriptionBottomSheetContent(messages: List<TranscriptionMessage>, isEnabled: Boolean, onToggleEnabled: () -> Unit) {
    val listState = rememberLazyListState()
    var selectedLanguage by remember { mutableStateOf("English") }
    val languages = listOf("English", "Spanish", "French", "German", "Chinese", "Japanese", "Arabic")

    LaunchedEffect(messages.size) { if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1) }

    Column(Modifier.fillMaxWidth().height(500.dp)) {
        Row(Modifier.fillMaxWidth().background(Color(0xFF9C27B0)).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.List, null, tint = Color.White, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(12.dp))
            Column {
                Text("Live Transcription", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("Real-time captions & translation", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
            }
        }
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = isEnabled, onCheckedChange = { onToggleEnabled() })
                Spacer(Modifier.width(8.dp))
                Text(if (isEnabled) "Enabled" else "Disabled", fontWeight = FontWeight.Medium)
            }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Translate to:", fontWeight = FontWeight.Medium, modifier = Modifier.align(Alignment.CenterVertically))
            var expanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                OutlinedTextField(value = selectedLanguage, onValueChange = {}, readOnly = true, modifier = Modifier.menuAnchor().width(150.dp), trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) }, singleLine = true)
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    languages.forEach { DropdownMenuItem(text = { Text(it) }, onClick = { selectedLanguage = it; expanded = false }) }
                }
            }
        }
        HorizontalDivider(Modifier.padding(vertical = 8.dp))
        LazyColumn(state = listState, modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 12.dp)) {
            if (messages.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Outlined.List, null, tint = Color.Gray, modifier = Modifier.size(48.dp))
                            Spacer(Modifier.height(8.dp))
                            Text("No transcriptions yet", color = Color.Gray)
                            Text("Enable transcription to see captions", color = Color.Gray.copy(alpha = 0.7f), fontSize = 12.sp)
                        }
                    }
                }
            }
            items(messages) { TranscriptionBubble(it, selectedLanguage) }
        }
    }
}

@Composable
fun TranscriptionBubble(message: TranscriptionMessage, targetLanguage: String) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(message.speakerName, color = Color(0xFF9C27B0), fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                Spacer(Modifier.weight(1f))
                Text("Now", color = Color.Gray, fontSize = 10.sp)
            }
            Spacer(Modifier.height(4.dp))
            Text(message.originalText, fontSize = 14.sp)
            if (message.translatedText != null && targetLanguage != "English") {
                Spacer(Modifier.height(4.dp))
                Text("→ ${message.translatedText}", color = Color(0xFF9C27B0), fontSize = 13.sp, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
            }
        }
    }
}

// ==================== SUBSESSIONS BOTTOM SHEET ====================
@Composable
fun SubsessionsBottomSheetContent(isHost: Boolean) {
    val subsessions = remember {
        listOf(
            Subsession("1", "Breakout Room 1", 3),
            Subsession("2", "Breakout Room 2", 4),
            Subsession("3", "Discussion Group A", 2),
            Subsession("4", "Workshop Team", 5)
        )
    }
    var selectedRoom by remember { mutableStateOf<String?>(null) }

    Column(Modifier.fillMaxWidth().height(500.dp)) {
        // Header
        Row(
            Modifier.fillMaxWidth().background(Color(0xFF00BCD4)).padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.AccountBox, null, tint = Color.White, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(12.dp))
            Column {
                Text("Breakout Rooms", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("${subsessions.size} rooms available", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
            }
        }

        if (isHost) {
            // Host controls
            Row(
                Modifier.fillMaxWidth().padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00BCD4)),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Create Room")
                }
                OutlinedButton(
                    onClick = { },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Open All")
                }
            }
        }

        HorizontalDivider()

        // Room list
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            items(subsessions) { room ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (selectedRoom == room.id) Color(0xFF00BCD4).copy(alpha = 0.1f) else Color(0xFFF5F5F5)
                    ),
                    border = if (selectedRoom == room.id) androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF00BCD4)) else null,
                    onClick = { selectedRoom = room.id }
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                modifier = Modifier.size(40.dp),
                                shape = CircleShape,
                                color = Color(0xFF00BCD4).copy(alpha = 0.2f)
                            ) {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text(room.name.take(1), color = Color(0xFF00BCD4), fontWeight = FontWeight.Bold)
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(room.name, fontWeight = FontWeight.Medium)
                                Text("${room.participantCount} participants", color = Color.Gray, fontSize = 12.sp)
                            }
                        }
                        Button(
                            onClick = { },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00BCD4)),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text("Join", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Bottom action
        if (selectedRoom != null) {
            Surface(
                Modifier.fillMaxWidth(),
                color = Color(0xFFF5F5F5)
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Button(
                        onClick = { },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00BCD4))
                    ) {
                        Text("Join Selected Room")
                    }
                }
            }
        }
    }
}

// ==================== REACTIONS BOTTOM SHEET ====================
@Composable
fun ReactionsBottomSheetContent(onSendReaction: (String) -> Unit, onDismiss: () -> Unit) {
    val reactions = listOf(
        "👍" to "Thumbs Up",
        "👏" to "Clap",
        "❤️" to "Love",
        "😂" to "Laugh",
        "😮" to "Wow",
        "🎉" to "Celebrate",
        "🙌" to "Raise Hand",
        "🔥" to "Fire",
        "💯" to "100",
        "✅" to "Check"
    )

    Column(Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
        // Header
        Row(
            Modifier.fillMaxWidth().background(Color(0xFFE91E63)).padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Favorite, null, tint = Color.White, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(12.dp))
            Text("Reactions", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(16.dp))

        // Quick reactions row
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            reactions.take(5).forEach { (emoji, name) ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(
                        modifier = Modifier.size(56.dp),
                        shape = CircleShape,
                        color = Color(0xFFF5F5F5),
                        onClick = { onSendReaction(emoji); onDismiss() }
                    ) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(emoji, fontSize = 28.sp)
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(name, fontSize = 10.sp, color = Color.Gray)
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // More reactions row
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            reactions.drop(5).forEach { (emoji, name) ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(
                        modifier = Modifier.size(56.dp),
                        shape = CircleShape,
                        color = Color(0xFFF5F5F5),
                        onClick = { onSendReaction(emoji); onDismiss() }
                    ) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(emoji, fontSize = 28.sp)
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(name, fontSize = 10.sp, color = Color.Gray)
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // Raise Hand button
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            Button(
                onClick = { onSendReaction("✋"); onDismiss() },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("✋", fontSize = 20.sp)
                Spacer(Modifier.width(8.dp))
                Text("Raise Hand", fontWeight = FontWeight.Medium)
            }
        }
    }
}

// ==================== REACTION BUBBLE OVERLAY ====================

// Raised hand - persistent, no animation
@Composable
fun RaisedHandBubble(reaction: ReactionEmoji) {
    Surface(
        modifier = Modifier.padding(vertical = 4.dp),
        color = Color(0xFFFF9800),
        shape = RoundedCornerShape(20.dp),
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(reaction.emoji, fontSize = 24.sp)
            Spacer(Modifier.width(8.dp))
            Text(reaction.senderName.split(" ").first(), fontSize = 12.sp, color = Color.White)
        }
    }
}

// Regular reaction - animated, floats up from bottom and fades out at top like Google Meet
@Composable
fun AnimatedReactionBubble(reaction: ReactionEmoji) {
    // Animation progress from 0f (start/bottom) to 1f (end/top)
    var animationProgress by remember { mutableStateOf(0f) }

    val progress by animateFloatAsState(
        targetValue = animationProgress,
        animationSpec = tween(durationMillis = 3000),
        label = "progress"
    )

    // Start at bottom (positive Y), move to top (0 or negative Y)
    // Moves up by 300dp total
    val offsetY = (1f - progress) * 300f - 150f  // Start at +150, end at -150

    // Fade out as it reaches the top (last 30% of animation)
    val alpha = if (progress > 0.7f) {
        1f - ((progress - 0.7f) / 0.3f)
    } else {
        1f
    }

    // Slight scale animation
    val scale = 1f - (progress * 0.2f)  // 1.0 -> 0.8

    LaunchedEffect(reaction.id) {
        animationProgress = 1f
    }

    if (alpha > 0.01f) {
        Surface(
            modifier = Modifier
                .padding(vertical = 4.dp)
                .graphicsLayer {
                    this.alpha = alpha
                    this.scaleX = scale
                    this.scaleY = scale
                    this.translationY = offsetY
                },
            color = Color.White,
            shape = RoundedCornerShape(20.dp),
            shadowElevation = 4.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(reaction.emoji, fontSize = 24.sp)
                Spacer(Modifier.width(8.dp))
                Text(reaction.senderName.split(" ").first(), fontSize = 12.sp, color = Color.Gray)
            }
        }
    }
}

// Legacy - keeping for compatibility
@Composable
fun ReactionBubble(reaction: ReactionEmoji) {
    Surface(
        modifier = Modifier.padding(vertical = 4.dp),
        color = Color.White,
        shape = RoundedCornerShape(20.dp),
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(reaction.emoji, fontSize = 24.sp)
            Spacer(Modifier.width(8.dp))
            Text(reaction.senderName.split(" ").first(), fontSize = 12.sp, color = Color.Gray)
        }
    }
}

// ==================== WAITING ROOM BUTTON WITH BADGE ====================
@Composable
fun WaitingRoomButton(
    onClick: () -> Unit,
    isActive: Boolean,
    waitingCount: Int
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box {
            FilledIconButton(
                onClick = onClick,
                modifier = Modifier.size(52.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = (if (isActive) Color(0xFF795548) else Color(0xFF607D8B)).copy(alpha = 0.2f)
                ),
                shape = CircleShape
            ) {
                Icon(
                    if (isActive) Icons.Default.Lock else Icons.Outlined.Lock,
                    "Waiting Room",
                    tint = if (isActive) Color(0xFF795548) else Color(0xFF607D8B),
                    modifier = Modifier.size(26.dp)
                )
            }
            // Badge for waiting count
            if (waitingCount > 0) {
                Surface(
                    modifier = Modifier.align(Alignment.TopEnd).offset(x = 4.dp, y = (-4).dp),
                    color = Color(0xFFE53935),
                    shape = CircleShape
                ) {
                    Text(
                        text = if (waitingCount > 9) "9+" else waitingCount.toString(),
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(4.dp)
                    )
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        Text("Waiting", color = Color.White.copy(alpha = 0.8f), fontSize = 10.sp, fontWeight = FontWeight.Medium)
    }
}

// ==================== WAITING ROOM BOTTOM SHEET ====================
@Composable
fun WaitingRoomBottomSheetContent(
    waitingRoomUsers: List<WaitingRoomUser>,
    onAdmitUser: (String) -> Unit,
    onRemoveUser: (String) -> Unit,
    onAdmitAll: () -> Unit
) {
    Column(Modifier.fillMaxWidth().height(500.dp)) {
        // Header
        Row(
            Modifier.fillMaxWidth().background(Color(0xFF795548)).padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Lock, null, tint = Color.White, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Waiting Room", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text("${waitingRoomUsers.size} waiting", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                }
            }
            if (waitingRoomUsers.isNotEmpty()) {
                Button(
                    onClick = onAdmitAll,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text("Admit All", color = Color(0xFF795548), fontWeight = FontWeight.Medium)
                }
            }
        }

        // Waiting room users list
        if (waitingRoomUsers.isEmpty()) {
            Box(
                Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Outlined.Lock, null, tint = Color.Gray, modifier = Modifier.size(64.dp))
                    Spacer(Modifier.height(16.dp))
                    Text("No one in waiting room", color = Color.Gray, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(4.dp))
                    Text("Participants will appear here", color = Color.Gray.copy(alpha = 0.7f), fontSize = 14.sp)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                items(waitingRoomUsers) { user ->
                    WaitingRoomUserCard(
                        user = user,
                        onAdmit = { onAdmitUser(user.id) },
                        onRemove = { onRemoveUser(user.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun WaitingRoomUserCard(
    user: WaitingRoomUser,
    onAdmit: () -> Unit,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
    ) {
        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(44.dp),
                    shape = CircleShape,
                    color = Color(0xFF795548).copy(alpha = 0.2f)
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(user.name.take(1).uppercase(), color = Color(0xFF795548), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(user.name, fontWeight = FontWeight.Medium, fontSize = 16.sp)
                    Text("Waiting to join...", color = Color.Gray, fontSize = 12.sp)
                }
            }
            Row {
                FilledIconButton(
                    onClick = onAdmit,
                    modifier = Modifier.size(40.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color(0xFF4CAF50))
                ) {
                    Icon(Icons.Default.Check, "Admit", tint = Color.White, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(8.dp))
                FilledIconButton(
                    onClick = onRemove,
                    modifier = Modifier.size(40.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color(0xFFE53935))
                ) {
                    Icon(Icons.Default.Close, "Remove", tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

// ==================== WAITING ROOM OVERLAY (FOR PARTICIPANTS) ====================
@Composable
fun WaitingRoomOverlay(sessionName: String, onLeave: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF1a1a2e), Color(0xFF16213e), Color(0xFF0f3460))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            // Lock icon with animated ring
            Surface(
                modifier = Modifier.size(120.dp),
                shape = CircleShape,
                color = Color(0xFF795548).copy(alpha = 0.15f),
                border = androidx.compose.foundation.BorderStroke(3.dp, Color(0xFF795548).copy(alpha = 0.3f))
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Lock,
                        null,
                        tint = Color(0xFF795548),
                        modifier = Modifier.size(55.dp)
                    )
                }
            }

            Spacer(Modifier.height(32.dp))

            Text(
                "Waiting Room",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(12.dp))

            Text(
                "The meeting hasn't started yet",
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 16.sp
            )

            Spacer(Modifier.height(4.dp))

            Text(
                "You'll be admitted when the host arrives",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 14.sp
            )

            Spacer(Modifier.height(32.dp))

            // Session name card
            Surface(
                color = Color.White.copy(alpha = 0.1f),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Phone, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(12.dp))
                        Text(sessionName, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            color = Color(0xFF795548),
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Waiting for host to start...",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 13.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(48.dp))

            // Tips while waiting
            Surface(
                color = Color(0xFF795548).copy(alpha = 0.2f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Info, null, tint = Color(0xFF795548), modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "You'll be notified when admitted",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 13.sp
                    )
                }
            }

            Spacer(Modifier.height(32.dp))

            // Leave button
            OutlinedButton(
                onClick = onLeave,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color(0xFFE53935)
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE53935).copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth(0.6f)
            ) {
                Icon(Icons.Default.Close, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Leave Waiting Room", fontWeight = FontWeight.Medium)
            }
        }
    }
}

