package com.yourcompany.zoomsession.viewmodel

import android.app.Application
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.yourcompany.zoomsession.model.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import us.zoom.sdk.*

/**
 * ViewModel for ZoomSessionActivity. Holds all UI state, business logic,
 * and the SDK event listener so state survives configuration changes.
 */
class ZoomSessionViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "ZoomSessionVM"
    }

    private val sdk: ZoomVideoSDK get() = ZoomVideoSDK.getInstance()

    // ==================== SESSION CONFIGURATION ====================
    var sessionName: String = ""
        private set
    var displayName: String = ""
        private set
    var sessionPassword: String = ""
        private set
    var jwtToken: String = ""
        private set
    var isHost: Boolean = false
        private set

    fun configure(
        sessionName: String,
        displayName: String,
        sessionPassword: String,
        jwtToken: String,
        isHost: Boolean
    ) {
        this.sessionName = sessionName
        this.displayName = if (isHost) "Coach $displayName" else displayName
        this.sessionPassword = sessionPassword
        this.jwtToken = jwtToken
        this.isHost = isHost
    }

    // ==================== UI STATE ====================
    var isInSession = mutableStateOf(false)
    var isMuted = mutableStateOf(true)
    var isVideoOn = mutableStateOf(false)
    var statusMessage = mutableStateOf("Connecting...")
    var participantCount = mutableStateOf(0)
    var remoteParticipants = mutableStateOf(listOf<Participant>())
    var showChat = mutableStateOf(false)
    var unreadMessageCount = mutableStateOf(0)
    var showWhiteboard = mutableStateOf(false)
    var showTranscription = mutableStateOf(false)
    var showSubsessions = mutableStateOf(false)
    var showReactions = mutableStateOf(false)
    var isTranscriptionEnabled = mutableStateOf(false)
    var isRecording = mutableStateOf(false)
    var selectedSpokenLanguage = mutableStateOf("English")
    var selectedTranslationLanguage = mutableStateOf("English")
    var availableTranscriptionLanguages = mutableStateOf(
        listOf(
            "English", "Spanish", "French", "German", "Chinese (Simplified)", "Chinese (Traditional)",
            "Japanese", "Korean", "Portuguese", "Italian", "Russian", "Arabic", "Hindi",
            "Dutch", "Polish", "Vietnamese", "Ukrainian", "Turkish", "Indonesian", "Hebrew"
        )
    )

    // ==================== DATA LISTS ====================
    var chatMessages = mutableStateOf(listOf<ChatMessage>())
    var hostChatMessages = mutableStateOf(listOf<ChatMessage>())
    var selectedChatTab = mutableStateOf(ChatTab.EVERYONE)
    var unreadHostMessageCount = mutableStateOf(0)
    var transcriptionMessages = mutableStateOf(listOf<TranscriptionMessage>())
    var activeReactions = mutableStateOf(listOf<ReactionEmoji>())
    var raisedHands = mutableStateOf(listOf<RaisedHand>())
    var hostNotification = mutableStateOf<String?>(null)
    var unmuteRequest = mutableStateOf<String?>(null)
    var isHostSharing = mutableStateOf(false)
    var hostVideoVersion = mutableStateOf(0)
    var isHostVideoOn = mutableStateOf(false)
    var isHostInSession = mutableStateOf(false)

    // ==================== ZOOM SDK EVENT LISTENER ====================
    val zoomListener = object : ZoomVideoSDKDelegate {
        override fun onSessionJoin() {
            isInSession.value = true
            statusMessage.value = "Connected"
            updateParticipantCount()
            ensureAudioVideoOff()
            // If we are the host, we're in session. Otherwise check if host is already present.
            if (isHost) {
                isHostInSession.value = true
            } else {
                val hostUser = sdk.session?.sessionHost
                isHostInSession.value = hostUser != null
                if (hostUser != null) {
                    isHostVideoOn.value = hostUser.videoCanvas != null
                }
            }
        }

        @Deprecated("Deprecated in SDK", ReplaceWith("onSessionLeave(reason)"))
        override fun onSessionLeave() {
            isInSession.value = false
            statusMessage.value = "Disconnected"
        }

        override fun onSessionLeave(reason: ZoomVideoSDKSessionLeaveReason?) {
            isInSession.value = false
            statusMessage.value = "Disconnected"
        }

        override fun onError(errorCode: Int) {
            statusMessage.value = "Error: $errorCode"
            Toast.makeText(getApplication(), "Error: $errorCode", Toast.LENGTH_SHORT).show()
        }

        override fun onUserJoin(userHelper: ZoomVideoSDKUserHelper?, userList: MutableList<ZoomVideoSDKUser>?) {
            updateParticipantCount()
            // Detect when the host joins
            if (!isHost) {
                val hostUser = sdk.session?.sessionHost
                if (hostUser != null) {
                    isHostInSession.value = true
                }
            }
        }

        override fun onUserLeave(userHelper: ZoomVideoSDKUserHelper?, userList: MutableList<ZoomVideoSDKUser>?) {
            updateParticipantCount()
        }

        override fun onChatNewMessageNotify(chatHelper: ZoomVideoSDKChatHelper?, messageItem: ZoomVideoSDKChatMessage?) {
            messageItem?.let { msg ->
                val isFromSelf = msg.isSelfSend
                val sdkMessageId = msg.getMessageId()

                // Parse out [msgId:...] prefix that web clients embed in content
                val rawContent = msg.getContent() ?: ""
                val webMsgIdRegex = Regex("""\[msgId:([^\]]+)]""")
                val webMsgIdMatch = webMsgIdRegex.find(rawContent)
                val webMsgId = webMsgIdMatch?.groupValues?.get(1)
                val cleanContent = if (webMsgIdMatch != null) rawContent.substring(webMsgIdMatch.range.last + 1) else rawContent

                // Use webMsgId if present (for reaction matching with web clients), fallback to SDK messageId
                val resolvedMessageId = webMsgId ?: sdkMessageId ?: java.util.UUID.randomUUID().toString()

                // Determine if this is a private (direct) message
                val isPrivateMessage = msg.getReceiverUser() != null

                if (isFromSelf) {
                    // Self-sent message already added optimistically with the same msgId - skip
                } else if (isPrivateMessage) {
                    addHostChatMessage(
                        ChatMessage(
                            messageId = resolvedMessageId,
                            senderName = msg.getSenderUser()?.userName ?: "Unknown",
                            message = cleanContent,
                            isFromMe = false,
                            isPrivate = true
                        )
                    )
                    if (!showChat.value || selectedChatTab.value != ChatTab.HOST) {
                        unreadHostMessageCount.value++
                    }
                    if (!showChat.value) {
                        unreadMessageCount.value++
                    }
                } else {
                    addChatMessage(
                        ChatMessage(
                            messageId = resolvedMessageId,
                            senderName = msg.getSenderUser()?.userName ?: "Unknown",
                            message = cleanContent,
                            isFromMe = false
                        )
                    )
                    if (!showChat.value) {
                        unreadMessageCount.value++
                    }
                }
            }
        }

        override fun onCommandReceived(sender: ZoomVideoSDKUser?, strCmd: String?) {
            strCmd?.let { cmd ->
                try {
                    val json = org.json.JSONObject(cmd)
                    val type = json.optString("type")
                    when (type) {
                        "chatReaction" -> {
                            val messageId = json.getString("messageId")
                            val emoji = json.getString("emoji")
                            val odUserId = json.getString("userId")
                            val userName = json.optString("userName", sender?.userName ?: "Unknown")
                            updateMessageReaction(messageId, emoji, odUserId, userName)
                        }
                        "toggle_video" -> {
                            if (!isHost) toggleVideo()
                        }
                        "toggle_mute" -> {
                            if (!isHost) toggleMute()
                        }
                        "raiseHand" -> {
                            // Web sends userId as number, read as string for consistent comparison
                            val userId = json.opt("userId")?.toString() ?: ""
                            val raised = json.optBoolean("raised", true)
                            val timestamp = json.optLong("timestamp", System.currentTimeMillis())
                            val userName = sender?.userName ?: "Someone"
                            if (raised) {
                                // Remove existing entry first to avoid duplicates, then add
                                raisedHands.value = raisedHands.value.filter { it.userId != userId } + RaisedHand(
                                    userId = userId,
                                    userName = userName,
                                    raised = true,
                                    timestamp = timestamp
                                )
                                if (isHost) {
                                    hostNotification.value = "✋ $userName raised hand"
                                }
                            } else {
                                raisedHands.value = raisedHands.value.filter { it.userId != userId }
                            }
                        }
                        "unmute_request" -> {
                            if (isHost) {
                                val userName = json.optString("userName", "Someone")
                                unmuteRequest.value = userName
                            }
                        }
                        "live_caption" -> {
                            if (!isHost) {
                                val text = json.getString("text")
                                val speakerName = json.optString("speakerName", "Host")
                                transcriptionMessages.value = transcriptionMessages.value + TranscriptionMessage(
                                    speakerName = speakerName,
                                    originalText = text
                                )
                                isTranscriptionEnabled.value = true
                            }
                        }
                        "reaction" -> {
                            val emoji = json.getString("emoji")
                            val senderName = json.optString("senderName", json.optString("userName", sender?.userName ?: "Unknown"))
                            val senderId = json.optString("senderId", json.optString("userId", ""))
                            val myUserId = sdk.session?.mySelf?.userID
                            if (senderId != myUserId) {
                                activeReactions.value = activeReactions.value + ReactionEmoji(
                                    emoji = emoji,
                                    senderName = senderName,
                                    senderId = senderId
                                )
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to parse command: ${e.message}")
                }
            }
        }

        override fun onLiveTranscriptionStatus(status: ZoomVideoSDKLiveTranscriptionHelper.ZoomVideoSDKLiveTranscriptionStatus?) {
            Log.d(TAG, "Transcription status changed: $status")
        }

        override fun onOriginalLanguageMsgReceived(messageItem: ZoomVideoSDKLiveTranscriptionHelper.ILiveTranscriptionMessageInfo?) {
            messageItem?.let { msg ->
                val content = msg.messageContent ?: ""
                if (content.isNotBlank()) {
                    val speakerName = msg.speakerName ?: "Unknown"
                    transcriptionMessages.value = transcriptionMessages.value + TranscriptionMessage(
                        speakerName = speakerName,
                        originalText = content
                    )
                    if (isHost) broadcastCaption(speakerName, content)
                }
            }
        }

        override fun onLiveTranscriptionMsgError(spokenLanguage: ZoomVideoSDKLiveTranscriptionHelper.ILiveTranscriptionLanguage?, transcriptLanguage: ZoomVideoSDKLiveTranscriptionHelper.ILiveTranscriptionLanguage?) {
            Log.e(TAG, "Transcription error - spoken: $spokenLanguage, transcript: $transcriptLanguage")
        }

        override fun onLiveTranscriptionMsgInfoReceived(messageInfo: ZoomVideoSDKLiveTranscriptionHelper.ILiveTranscriptionMessageInfo?) {
            messageInfo?.let { msg ->
                val content = msg.messageContent ?: ""
                if (content.isNotBlank()) {
                    val speakerName = msg.speakerName ?: "Unknown"
                    transcriptionMessages.value = transcriptionMessages.value + TranscriptionMessage(
                        speakerName = speakerName,
                        originalText = content
                    )
                    if (isHost) broadcastCaption(speakerName, content)
                }
            }
        }

        override fun onSpokenLanguageChanged(spokenLanguage: ZoomVideoSDKLiveTranscriptionHelper.ILiveTranscriptionLanguage?) {
            Log.d(TAG, "Spoken language changed: $spokenLanguage")
        }

        // Required empty implementations
        override fun onUserVideoStatusChanged(videoHelper: ZoomVideoSDKVideoHelper?, userList: MutableList<ZoomVideoSDKUser>?) {
            val session = sdk.session ?: return
            val hostUser = session.sessionHost ?: return
            val hostChanged = userList?.any { it.userID == hostUser.userID } ?: false
            if (hostChanged) {
                isHostVideoOn.value = hostUser.videoCanvas != null
                hostVideoVersion.value++
            }
        }
        override fun onUserAudioStatusChanged(audioHelper: ZoomVideoSDKAudioHelper?, userList: MutableList<ZoomVideoSDKUser>?) {
            updateParticipantCount()
        }
        override fun onUserShareStatusChanged(shareHelper: ZoomVideoSDKShareHelper?, userInfo: ZoomVideoSDKUser?, status: ZoomVideoSDKShareStatus?) {
            val session = sdk.session ?: return
            val hostUser = session.sessionHost
            if (userInfo != null && hostUser != null && userInfo.userID == hostUser.userID) {
                isHostSharing.value = status == ZoomVideoSDKShareStatus.ZoomVideoSDKShareStatus_Start
            }
        }
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
        override fun onCommandChannelConnectResult(isSuccess: Boolean) {}
        override fun onCloudRecordingStatus(status: ZoomVideoSDKRecordingStatus?, handler: ZoomVideoSDKRecordingConsentHandler?) {}
        override fun onHostAskUnmute() {}
        override fun onInviteByPhoneStatus(status: ZoomVideoSDKPhoneStatus?, reason: ZoomVideoSDKPhoneFailedReason?) {}
        override fun onMultiCameraStreamStatusChanged(status: ZoomVideoSDKMultiCameraStreamStatus?, user: ZoomVideoSDKUser?, videoPipe: ZoomVideoSDKRawDataPipe?) {}
        override fun onMultiCameraStreamStatusChanged(status: ZoomVideoSDKMultiCameraStreamStatus?, user: ZoomVideoSDKUser?, canvas: ZoomVideoSDKVideoCanvas?) {}
        override fun onCameraControlRequestResult(user: ZoomVideoSDKUser?, isApproved: Boolean) {}
        override fun onUserRecordingConsent(user: ZoomVideoSDKUser?) {}
        override fun onProxySettingNotification(handler: ZoomVideoSDKProxySettingHandler?) {}
        override fun onSSLCertVerifiedFailNotification(handler: ZoomVideoSDKSSLCertificateInfo?) {}
        @Deprecated("Deprecated in SDK")
        override fun onShareNetworkStatusChanged(shareNetworkStatus: ZoomVideoSDKNetworkStatus?, isSendingShare: Boolean) {}
        override fun onShareContentChanged(shareHelper: ZoomVideoSDKShareHelper?, userInfo: ZoomVideoSDKUser?, shareAction: ZoomVideoSDKShareAction?) {}
        override fun onChatDeleteMessageNotify(chatHelper: ZoomVideoSDKChatHelper?, msgID: String?, deleteBy: ZoomVideoSDKChatMessageDeleteType?) {}
        override fun onChatPrivilegeChanged(chatHelper: ZoomVideoSDKChatHelper?, currentPrivilege: ZoomVideoSDKChatPrivilegeType?) {}
        override fun onCameraControlRequestReceived(user: ZoomVideoSDKUser?, requestType: ZoomVideoSDKCameraControlRequestType?, requestHandler: ZoomVideoSDKCameraControlRequestHandler?) {}
        @Deprecated("Deprecated in SDK")
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

    // ==================== CHAT FUNCTIONS ====================

    private fun addChatMessage(message: ChatMessage) {
        chatMessages.value = chatMessages.value + message
    }

    private fun addHostChatMessage(message: ChatMessage) {
        hostChatMessages.value = hostChatMessages.value + message
    }

    fun sendHostChatMessage(message: String) {
        if (message.isBlank()) return
        val msgId = "${System.currentTimeMillis()}-${java.util.UUID.randomUUID().toString().take(8)}"
        addHostChatMessage(
            ChatMessage(
                messageId = msgId,
                senderName = displayName,
                message = message,
                isFromMe = true,
                isPrivate = true
            )
        )
        try {
            val hostUser = sdk.session?.sessionHost
            if (hostUser != null) {
                sdk.chatHelper?.sendChatToUser(hostUser, "[msgId:$msgId]$message")
            } else {
                Log.e(TAG, "Cannot send private message: host user not found")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send host chat: ${e.message}")
        }
    }

    fun sendChatMessage(message: String) {
        if (message.isBlank()) return
        val msgId = "${System.currentTimeMillis()}-${java.util.UUID.randomUUID().toString().take(8)}"
        addChatMessage(
            ChatMessage(
                messageId = msgId,
                senderName = displayName,
                message = message,
                isFromMe = true
            )
        )
        try {
            // Embed [msgId:] prefix so web clients can match reactions to this message
            sdk.chatHelper?.sendChatToAll("[msgId:$msgId]$message")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send chat: ${e.message}")
        }
    }

    fun updateMessageReaction(messageId: String, emoji: String, userId: String, userName: String) {
        val reactionMapper = { message: ChatMessage ->
            if (message.messageId == messageId) {
                val updatedReactions = message.reactions.toMutableMap()
                val users = updatedReactions.getOrDefault(emoji, emptyMap()).toMutableMap()
                if (!users.containsKey(userId)) {
                    users[userId] = userName
                    updatedReactions[emoji] = users
                }
                message.copy(reactions = updatedReactions)
            } else {
                message
            }
        }
        chatMessages.value = chatMessages.value.map(reactionMapper)
        hostChatMessages.value = hostChatMessages.value.map(reactionMapper)
    }

    fun sendChatReaction(messageId: String, emoji: String) {
        val myUser = sdk.session?.mySelf
        val odUserId = myUser?.userID ?: displayName
        updateMessageReaction(messageId, emoji, odUserId, displayName)
        try {
            val reactionJson = org.json.JSONObject().apply {
                put("type", "chatReaction")
                put("messageId", messageId)
                put("emoji", emoji)
                put("userId", odUserId)
                put("userName", displayName)
                put("timestamp", System.currentTimeMillis())
            }
            sdk.cmdChannel?.sendCommand(null, reactionJson.toString())
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send chat reaction: ${e.message}")
        }
    }

    // ==================== SESSION MANAGEMENT ====================

    fun joinZoomSession() {
        val audioOptions = ZoomVideoSDKAudioOption().apply { connect = false; mute = true }
        val videoOptions = ZoomVideoSDKVideoOption().apply { localVideoOn = false }
        val sessionContext = ZoomVideoSDKSessionContext().apply {
            this.sessionName = this@ZoomSessionViewModel.sessionName
            this.userName = displayName
            this.sessionPassword = this@ZoomSessionViewModel.sessionPassword
            this.token = jwtToken
            this.audioOption = audioOptions
            this.videoOption = videoOptions
        }

        val session = sdk.joinSession(sessionContext)
        if (session != null) {
            statusMessage.value = "Joining..."
        } else {
            statusMessage.value = "Failed to join"
        }
    }

    fun leaveSession() {
        try {
            sdk.videoHelper?.stopVideo()
            sdk.session?.mySelf?.let { sdk.audioHelper?.muteAudio(it) }
            sdk.leaveSession(isHost)
        } catch (e: Exception) {
            Log.e(TAG, "Error during leave: ${e.message}")
        }
        // Clear state
        isVideoOn.value = false
        isMuted.value = true
        isInSession.value = false
        isHostVideoOn.value = false
        isHostInSession.value = false
        chatMessages.value = emptyList()
        hostChatMessages.value = emptyList()
        selectedChatTab.value = ChatTab.EVERYONE
        unreadHostMessageCount.value = 0
    }

    // ==================== AUDIO/VIDEO CONTROLS ====================

    fun toggleMute() {
        val myUser = sdk.session?.mySelf
        myUser?.let {
            val audioHelper = sdk.audioHelper
            val audioStatus = it.audioStatus
            val isAudioConnected = audioStatus?.audioType != ZoomVideoSDKAudioStatus.ZoomVideoSDKAudioType.ZoomVideoSDKAudioType_None

            if (isMuted.value) {
                if (!isAudioConnected) {
                    Log.d(TAG, "Audio not connected, starting audio...")
                    audioHelper?.startAudio()
                    viewModelScope.launch {
                        delay(500)
                        audioHelper?.unMuteAudio(it)
                        isMuted.value = false
                    }
                } else {
                    audioHelper?.unMuteAudio(it)
                    isMuted.value = false
                }
            } else {
                audioHelper?.muteAudio(it)
                isMuted.value = true
            }
        }
    }

    fun toggleVideo() {
        if (isVideoOn.value) sdk.videoHelper?.stopVideo()
        else sdk.videoHelper?.startVideo()
        isVideoOn.value = !isVideoOn.value
    }

    private fun ensureAudioVideoOff() {
        Log.d(TAG, "Ensuring audio is muted and video is off")
        forceAudioVideoOff()
        viewModelScope.launch {
            delay(1000)
            Log.d(TAG, "Running delayed audio/video safety check")
            forceAudioVideoOff()
        }
    }

    private fun forceAudioVideoOff() {
        try {
            val myUser = sdk.session?.mySelf
            myUser?.let {
                val audioStatus = it.audioStatus
                if (audioStatus?.isMuted == false) {
                    sdk.audioHelper?.muteAudio(it)
                }
            }
            isMuted.value = true
            sdk.videoHelper?.stopVideo()
            isVideoOn.value = false
        } catch (e: Exception) {
            Log.e(TAG, "Error ensuring audio/video off: ${e.message}")
        }
    }

    // ==================== LIVE TRANSCRIPTION ====================

    fun startLiveTranscription() {
        Log.d(TAG, "Starting live transcription (English only)")
        try {
            val transcriptionHelper = sdk.liveTranscriptionHelper
            if (transcriptionHelper == null) {
                Log.e(TAG, "Live transcription helper is NULL")
                return
            }

            // Set spoken language to English before starting
            val spokenLanguages = transcriptionHelper.availableSpokenLanguages
            Log.d(TAG, "Available spoken languages: ${spokenLanguages?.map { "${it.lttLanguageName}(${it.lttLanguageID})" }}")
            val english = spokenLanguages?.firstOrNull {
                it.lttLanguageName?.contains("English", ignoreCase = true) == true
            }
            if (english != null) {
                val langResult = transcriptionHelper.setSpokenLanguage(english.lttLanguageID)
                Log.d(TAG, "setSpokenLanguage(${english.lttLanguageName}) result: $langResult")
            } else {
                Log.w(TAG, "English not found in available spoken languages")
            }

            // Enable receiving original spoken language content
            transcriptionHelper.enableReceiveSpokenLanguageContent(true)

            val result = transcriptionHelper.startLiveTranscription()
            Log.d(TAG, "startLiveTranscription result: $result")
        } catch (e: Exception) {
            Log.e(TAG, "Exception in startLiveTranscription: ${e.message}")
        }
    }

    private fun broadcastCaption(speakerName: String, text: String) {
        try {
            val command = org.json.JSONObject().apply {
                put("type", "live_caption")
                put("speakerName", speakerName)
                put("text", text)
            }.toString()
            sdk.cmdChannel?.sendCommand(null, command)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to broadcast caption: ${e.message}")
        }
    }

    fun stopLiveTranscription() {
        try {
            sdk.liveTranscriptionHelper?.stopLiveTranscription()
            Log.d(TAG, "Live transcription stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping live transcription: ${e.message}")
        }
    }

    fun setTranscriptionLanguage(language: String) {
        selectedTranslationLanguage.value = language
        if (isTranscriptionEnabled.value) {
            stopLiveTranscription()
            viewModelScope.launch {
                delay(500)
                startLiveTranscription()
            }
        }
    }

    private fun updateParticipantCount() {
        val remoteUsers = sdk.session?.remoteUsers
        participantCount.value = (remoteUsers?.size ?: 0) + 1
        remoteParticipants.value = remoteUsers?.mapNotNull { user ->
            user.userName?.let { name ->
                val role = when {
                    user == sdk.session?.sessionHost -> ParticipantRole.HOST
                    else -> ParticipantRole.PARTICIPANT
                }
                Participant(
                    id = user.userID ?: name,
                    name = name,
                    role = role,
                    isMuted = user.audioStatus?.isMuted != false
                )
            }
        } ?: emptyList()
    }

    fun toggleParticipantMute(participantId: String) {
        val remoteUser = sdk.session?.remoteUsers?.firstOrNull {
            (it.userID ?: it.userName) == participantId
        } ?: return
        // Read mute state from the local participant list (what the UI shows)
        // instead of the SDK, which may lag behind after optimistic updates.
        val participant = remoteParticipants.value.find { it.id == participantId } ?: return
        val isMuted = participant.isMuted
        if (isMuted) {
            // Host cannot directly unmute a remote participant;
            // send a command so the participant unmutes themselves.
            try {
                val command = org.json.JSONObject().apply {
                    put("type", "toggle_mute")
                    put("fromHost", true)
                }.toString()
                sdk.cmdChannel?.sendCommand(remoteUser, command)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send toggle mute command: ${e.message}")
            }
        } else {
            sdk.audioHelper?.muteAudio(remoteUser)
        }
        // Optimistically update UI so the bottom sheet reflects the change immediately,
        // rather than waiting for the async onUserAudioStatusChanged callback.
        remoteParticipants.value = remoteParticipants.value.map { participant ->
            if (participant.id == participantId) {
                participant.copy(isMuted = !isMuted)
            } else {
                participant
            }
        }
    }

    // ==================== REACTIONS ====================

    fun sendReaction(emoji: String) {
        val odUserId = sdk.session?.mySelf?.userID ?: displayName
        if (emoji == "✋") {
            val existingHand = raisedHands.value.find { it.userId == odUserId }
            val raised = existingHand == null
            if (raised) {
                raisedHands.value = raisedHands.value + RaisedHand(
                    userId = odUserId,
                    userName = displayName,
                    raised = true
                )
            } else {
                raisedHands.value = raisedHands.value.filter { it.userId != odUserId }
            }
            try {
                val command = org.json.JSONObject().apply {
                    put("type", "raiseHand")
                    put("userId", odUserId)
                    put("userName", displayName)
                    put("raised", raised)
                    put("timestamp", System.currentTimeMillis())
                }.toString()
                sdk.cmdChannel?.sendCommand(null, command)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send raise hand: ${e.message}")
            }
        } else {
            activeReactions.value = activeReactions.value + ReactionEmoji(
                emoji = emoji,
                senderName = displayName,
                senderId = odUserId
            )
            try {
                val command = org.json.JSONObject().apply {
                    put("type", "reaction")
                    put("emoji", emoji)
                    put("senderName", displayName)
                    put("senderId", odUserId)
                }.toString()
                sdk.cmdChannel?.sendCommand(null, command)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send emoji reaction: ${e.message}")
            }
        }
    }

    /** Removes expired reactions (older than 3 seconds). */
    fun cleanUpExpiredReactions() {
        val currentTime = System.currentTimeMillis()
        activeReactions.value = activeReactions.value.filter {
            currentTime - it.timestamp < 3000
        }
    }

    // ==================== HOST REMOTE CONTROL ====================

    fun toggleRemoteAudio(userName: String) {
        if (!isHost) return
        val remoteUsers = sdk.session?.remoteUsers ?: return
        val user = remoteUsers.find { it.userName == userName } ?: return
        val audioStatus = user.audioStatus
        if (audioStatus?.isMuted == true) {
            sdk.audioHelper?.unMuteAudio(user)
        } else {
            sdk.audioHelper?.muteAudio(user)
        }
    }

    fun toggleRemoteVideo(userName: String) {
        if (!isHost) return
        val remoteUsers = sdk.session?.remoteUsers ?: return
        val user = remoteUsers.find { it.userName == userName } ?: return
        try {
            val command = org.json.JSONObject().apply {
                put("type", "toggle_video")
                put("fromHost", true)
            }.toString()
            sdk.cmdChannel?.sendCommand(user, command)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send toggle video command: ${e.message}")
        }
    }

    // ==================== UNMUTE REQUEST ====================

    fun sendUnmuteRequest() {
        try {
            val command = org.json.JSONObject().apply {
                put("type", "unmute_request")
                put("userName", displayName)
            }.toString()
            sdk.cmdChannel?.sendCommand(null, command)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send unmute request: ${e.message}")
        }
    }

    fun approveUnmuteRequest(userName: String) {
        val remoteUsers = sdk.session?.remoteUsers ?: return
        val user = remoteUsers.find { it.userName == userName }
        user?.let {
            try {
                val command = org.json.JSONObject().apply {
                    put("type", "toggle_mute")
                    put("fromHost", true)
                }.toString()
                sdk.cmdChannel?.sendCommand(it, command)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send unmute approval command: ${e.message}")
            }
        }
        unmuteRequest.value = null
    }

    fun dismissUnmuteRequest() {
        unmuteRequest.value = null
    }
}
