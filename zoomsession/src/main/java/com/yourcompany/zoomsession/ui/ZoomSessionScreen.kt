package com.yourcompany.zoomsession.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.yourcompany.zoomsession.model.ChatMessage
import com.yourcompany.zoomsession.model.ReactionEmoji
import com.yourcompany.zoomsession.model.TranscriptionMessage
import com.yourcompany.zoomsession.model.WaitingRoomUser
import us.zoom.sdk.ZoomVideoSDK
import us.zoom.sdk.ZoomVideoSDKVideoAspect
import us.zoom.sdk.ZoomVideoSDKVideoView

/**
 * Main composable screen for the Zoom session.
 *
 * Layout Structure:
 * - Full-screen video tile (background)
 * - Top bar: Session name, status, participant count button
 * - Bottom bar: Mute, Video, Share, Chat, More buttons
 * - Overlays: Recording indicator, raised hands, floating reactions, transcription
 * - Bottom sheets: Chat, Whiteboard, Participants, More options, Waiting room
 */
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
    unreadMessageCount: Int,
    chatMessages: List<ChatMessage>,
    transcriptionMessages: List<TranscriptionMessage>,
    activeReactions: List<ReactionEmoji>,
    raisedHands: List<ReactionEmoji>,
    hostNotification: String? = null,
    onDismissHostNotification: () -> Unit = {},
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
        // Video tile - FULL SCREEN
        SelfVideoTile(displayName, isVideoOn, isMuted, isHost)

        // Recording indicator
        if (isRecording) {
            Surface(
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 80.dp),
                color = ZoomColors.Error,
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color.White))
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
                Column(horizontalAlignment = Alignment.End) {
                    raisedHands.forEach { reaction ->
                        RaisedHandBubble(reaction)
                    }
                }
            }
        }

        // Host notification overlay
        if (hostNotification != null) {
            LaunchedEffect(hostNotification) {
                kotlinx.coroutines.delay(4000)
                onDismissHostNotification()
            }
            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = if (isRecording) 120.dp else 80.dp),
                color = ZoomColors.Orange,
                shape = RoundedCornerShape(12.dp),
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        hostNotification,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Active reactions overlay (animated - bottom right)
        if (activeReactions.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 200.dp)
            ) {
                Column(horizontalAlignment = Alignment.End) {
                    activeReactions.takeLast(5).forEach { reaction ->
                        AnimatedReactionBubble(reaction)
                    }
                }
            }
        }

        // Live transcription overlay
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
                        textAlign = TextAlign.Center,
                        maxLines = 3,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp)
                    )
                }
            }
        }

        // Top bar
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
                                    .background(if (isInSession) ZoomColors.Success else Color.Yellow)
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

        // Bottom controls
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
                if (isHost) {
                    BottomBarButton(
                        onClick = onToggleMute,
                        icon = if (isMuted) Icons.Outlined.Mic else Icons.Outlined.MicOff,
                        label = if (isMuted) "Unmute" else "Mute",
                        isActive = !isMuted,
                        activeColor = Color.White,
                        inactiveColor = ZoomColors.Error
                    )

                    BottomBarButton(
                        onClick = onToggleVideo,
                        icon = if (isVideoOn) Icons.Outlined.Videocam else Icons.Outlined.VideocamOff,
                        label = if (isVideoOn) "Stop Video" else "Start Video",
                        isActive = isVideoOn,
                        activeColor = Color.White,
                        inactiveColor = ZoomColors.Error
                    )

                    BottomBarButton(
                        onClick = { },
                        icon = Icons.Outlined.ScreenShare,
                        label = "Share",
                        isActive = false,
                        activeColor = ZoomColors.Success,
                        inactiveColor = Color.White
                    )
                }

                BadgedBox(
                    badge = {
                        if (unreadMessageCount > 0) {
                            Badge(containerColor = ZoomColors.Error) {
                                Text(
                                    text = if (unreadMessageCount > 99) "99+" else unreadMessageCount.toString(),
                                    color = Color.White,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                ) {
                    BottomBarButton(
                        onClick = onToggleChat,
                        icon = Icons.Filled.ChatBubbleOutline,
                        label = "Chat",
                        isActive = showChat,
                        activeColor = ZoomColors.Primary,
                        inactiveColor = Color.White
                    )
                }

                if (isHost) {
                    BottomBarButton(
                        onClick = { showMore = true },
                        icon = Icons.Default.MoreHoriz,
                        label = "More",
                        isActive = false,
                        activeColor = Color.White,
                        inactiveColor = Color.White
                    )
                } else {

                }
            }
        }
    }

    // More Options Bottom Sheet
    if (showMore) {
        ModalBottomSheet(
            onDismissRequest = { showMore = false },
            sheetState = moreSheetState,
            containerColor = ZoomColors.DarkSurface,
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
            containerColor = ZoomColors.DarkSurface,
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
        ModalBottomSheet(onDismissRequest = onToggleChat, sheetState = chatSheetState, containerColor = ZoomColors.DarkSurface, shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)) {
            ChatBottomSheetContent(chatMessages, onSendMessage, onChatReaction)
        }
    }

    // Whiteboard Bottom Sheet
    if (showWhiteboard) {
        ModalBottomSheet(onDismissRequest = onToggleWhiteboard, sheetState = whiteboardSheetState, containerColor = ZoomColors.DarkSurface, shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)) {
            WhiteboardBottomSheetContent()
        }
    }

    // Subsessions Bottom Sheet
    if (showSubsessions) {
        ModalBottomSheet(onDismissRequest = onToggleSubsessions, sheetState = subsessionSheetState, containerColor = ZoomColors.DarkSurface, shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)) {
            SubsessionsBottomSheetContent(isHost)
        }
    }

    // Waiting Room Bottom Sheet (Host only)
    if (showWaitingRoom && isHost) {
        ModalBottomSheet(onDismissRequest = onToggleWaitingRoom, sheetState = waitingRoomSheetState, containerColor = ZoomColors.DarkSurface, shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)) {
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

// ==================== SELF VIDEO TILE ====================

/**
 * Full-screen video tile showing the current user's camera feed or avatar.
 * Host sees own camera; participant always sees host's camera.
 */
@Composable
fun SelfVideoTile(displayName: String, isVideoOn: Boolean, isMuted: Boolean, isHost: Boolean = true) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ZoomColors.DarkBackground),
        contentAlignment = Alignment.Center
    ) {
        if (isHost && isVideoOn) {
            AndroidView(
                factory = { context ->
                    ZoomVideoSDKVideoView(context).apply {
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
                    ZoomVideoSDK.getInstance().session?.mySelf?.let { myUser ->
                        myUser.videoCanvas?.subscribe(
                            view,
                            ZoomVideoSDKVideoAspect.ZoomVideoSDKVideoAspect_Original
                        )
                    }
                },
                onRelease = { view ->
                    ZoomVideoSDK.getInstance().session?.mySelf?.videoCanvas?.unSubscribe(view)
                }
            )
        } else if (!isHost) {
            AndroidView(
                factory = { context ->
                    ZoomVideoSDKVideoView(context).apply {
                        val session = ZoomVideoSDK.getInstance().session
                        val hostUser = session?.sessionHost ?: session?.remoteUsers?.firstOrNull()
                        hostUser?.videoCanvas?.subscribe(
                            this,
                            ZoomVideoSDKVideoAspect.ZoomVideoSDKVideoAspect_Original
                        )
                    }
                },
                modifier = Modifier.fillMaxSize(),
                update = { view ->
                    val session = ZoomVideoSDK.getInstance().session
                    val hostUser = session?.sessionHost ?: session?.remoteUsers?.firstOrNull()
                    hostUser?.videoCanvas?.subscribe(
                        view,
                        ZoomVideoSDKVideoAspect.ZoomVideoSDKVideoAspect_Original
                    )
                },
                onRelease = { view ->
                    val session = ZoomVideoSDK.getInstance().session
                    val hostUser = session?.sessionHost ?: session?.remoteUsers?.firstOrNull()
                    hostUser?.videoCanvas?.unSubscribe(view)
                }
            )

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
                    Icon(
                        if (isMuted) Icons.Default.Clear else Icons.Default.Done,
                        contentDescription = null,
                        tint = if (isMuted) ZoomColors.Error else Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (isHost) "$displayName (You)" else displayName,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        } else if (isHost) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Surface(
                    modifier = Modifier.size(120.dp),
                    shape = CircleShape,
                    color = ZoomColors.DarkAvatarBg
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
                Text(displayName, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(4.dp))
                Text(
                    if (isMuted) "Muted" else "Unmuted",
                    color = if (isMuted) ZoomColors.Error else ZoomColors.Success,
                    fontSize = 14.sp
                )
            }
        }
    }
}

// ==================== PREVIEWS ====================

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun ZoomSessionScreenHostPreview() {
    ZoomSessionScreen(
        sessionName = "Team Standup",
        displayName = "John Doe",
        isInSession = true,
        isMuted = false,
        isVideoOn = false,
        statusMessage = "Connected",
        participantCount = 5,
        remoteParticipants = listOf("Alice", "Bob", "Charlie", "Diana"),
        isHost = true,
        showChat = false,
        showWhiteboard = false,
        showTranscription = true,
        showSubsessions = false,
        showReactions = true,
        showWaitingRoom = false,
        isInWaitingRoom = false,
        isTranscriptionEnabled = true,
        isRecording = true,
        selectedTranscriptionLanguage = "English",
        availableTranscriptionLanguages = listOf("English", "Spanish", "French"),
        unreadMessageCount = 3,
        chatMessages = emptyList(),
        transcriptionMessages = listOf(
            TranscriptionMessage(
                speakerName = "Alice",
                originalText = "Hello everyone, let's start the meeting.",
                translatedText = null
            )
        ),
        activeReactions = listOf(
            ReactionEmoji(emoji = "👍", senderName = "Bob", senderId = "bob123"),
            ReactionEmoji(emoji = "❤️", senderName = "Charlie", senderId = "charlie456")
        ),
        raisedHands = listOf(
            ReactionEmoji(emoji = "✋", senderName = "Diana", senderId = "diana789")
        ),
        hostNotification = null,
        waitingRoomUsers = listOf(
            WaitingRoomUser(id = "1", name = "Eve", timestamp = System.currentTimeMillis())
        ),
        onToggleMute = {},
        onToggleVideo = {},
        onToggleChat = {},
        onToggleWhiteboard = {},
        onToggleTranscription = {},
        onToggleSubsessions = {},
        onToggleReactions = {},
        onToggleWaitingRoom = {},
        onToggleTranscriptionEnabled = {},
        onToggleRecording = {},
        onSendReaction = {},
        onAdmitUser = {},
        onRemoveFromWaitingRoom = {},
        onAdmitAllUsers = {},
        onSendMessage = {},
        onChatReaction = { _, _ -> },
        onLeaveSession = {},
        onSelectTranscriptionLanguage = {}
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun ZoomSessionScreenParticipantPreview() {
    ZoomSessionScreen(
        sessionName = "Project Review",
        displayName = "Jane Smith",
        isInSession = true,
        isMuted = true,
        isVideoOn = false,
        statusMessage = "Connected",
        participantCount = 3,
        remoteParticipants = listOf("Host User", "Other Participant"),
        isHost = false,
        showChat = false,
        showWhiteboard = false,
        showTranscription = false,
        showSubsessions = false,
        showReactions = false,
        showWaitingRoom = false,
        isInWaitingRoom = false,
        isTranscriptionEnabled = false,
        isRecording = false,
        selectedTranscriptionLanguage = "English",
        availableTranscriptionLanguages = listOf("English"),
        unreadMessageCount = 0,
        chatMessages = emptyList(),
        transcriptionMessages = emptyList(),
        activeReactions = emptyList(),
        raisedHands = emptyList(),
        hostNotification = null,
        waitingRoomUsers = emptyList(),
        onToggleMute = {},
        onToggleVideo = {},
        onToggleChat = {},
        onToggleWhiteboard = {},
        onToggleTranscription = {},
        onToggleSubsessions = {},
        onToggleReactions = {},
        onToggleWaitingRoom = {},
        onToggleTranscriptionEnabled = {},
        onToggleRecording = {},
        onSendReaction = {},
        onAdmitUser = {},
        onRemoveFromWaitingRoom = {},
        onAdmitAllUsers = {},
        onSendMessage = {},
        onChatReaction = { _, _ -> },
        onLeaveSession = {},
        onSelectTranscriptionLanguage = {}
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun SelfVideoTileVideoOffPreview() {
    SelfVideoTile(
        displayName = "John Doe",
        isVideoOn = false,
        isMuted = true,
        isHost = true
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun SelfVideoTileVideoOnPreview() {
    SelfVideoTile(
        displayName = "John Doe",
        isVideoOn = true,
        isMuted = false,
        isHost = true
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun SelfVideoTileParticipantPreview() {
    SelfVideoTile(
        displayName = "Participant",
        isVideoOn = false,
        isMuted = true,
        isHost = false
    )
}
