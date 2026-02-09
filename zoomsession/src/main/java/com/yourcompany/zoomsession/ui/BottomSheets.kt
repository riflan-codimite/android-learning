package com.yourcompany.zoomsession.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.MicOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yourcompany.zoomsession.model.*

// ==================== MORE OPTIONS BOTTOM SHEET ====================

/**
 * Bottom sheet containing additional options not shown in the main bottom bar.
 * Quick reactions, recording, whiteboard, captions, and leave button.
 */
//@Preview(showBackground = true)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreOptionsBottomSheet(
    isRecording: Boolean,
    showWhiteboard: Boolean,
    showTranscription: Boolean,
    showSubsessions: Boolean,
    showReactions: Boolean,
    isHost: Boolean,
    selectedTranscriptionLanguage: String,
    availableTranscriptionLanguages: List<String>,
    onToggleRecording: () -> Unit,
    onToggleWhiteboard: () -> Unit,
    onToggleTranscription: () -> Unit,
    onToggleSubsessions: () -> Unit,
    onToggleReactions: () -> Unit,
    onLeaveSession: () -> Unit,
    onSendReaction: (String) -> Unit,
    onSelectTranscriptionLanguage: (String) -> Unit
) {
    val quickReactions = listOf("👍", "👏", "❤️", "😂", "😮", "🎉")
    var showLanguageDropdown by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
    ) {
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

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                quickReactions.take(6).forEach { emoji ->
                    EmojiButton(emoji = emoji, onClick = { onSendReaction(emoji) })
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
                        Text(selectedTranscriptionLanguage, color = Color.White, fontSize = 16.sp)
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
                    modifier = Modifier.background(ZoomColors.DarkCard)
                ) {
                    availableTranscriptionLanguages.forEach { language ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    language,
                                    color = if (language == selectedTranscriptionLanguage) ZoomColors.Purple else Color.White
                                )
                            },
                            onClick = { showLanguageDropdown = false },
                            leadingIcon = if (language == selectedTranscriptionLanguage) {
                                { Icon(Icons.Default.Check, null, tint = ZoomColors.Purple) }
                            } else null
                        )
                    }
                }
            }
        }

        HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

        // Options Grid
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MoreOptionItem(
                    icon = Icons.Default.Star,
                    label = if (isRecording) "Stop Recording" else "Record",
                    isActive = isRecording,
                    activeColor = ZoomColors.Error,
                    onClick = onToggleRecording
                )
//                MoreOptionItem(
//                    icon = Icons.Default.Edit,
//                    label = "Whiteboard",
//                    isActive = showWhiteboard,
//                    activeColor = ZoomColors.Orange,
//                    onClick = onToggleWhiteboard
//                )
                MoreOptionItem(
                    icon = Icons.Default.List,
                    label = "Captions",
                    isActive = showTranscription,
                    activeColor = ZoomColors.Purple,
                    onClick = onToggleTranscription
                )

                MoreOptionItem(
                    icon = Icons.Default.AccountBox,
                    label = "Breakout Rooms",
                    isActive = showSubsessions,
                    activeColor = ZoomColors.Cyan,
                    onClick = onToggleSubsessions
                )
                Spacer(Modifier.width(80.dp))
                Spacer(Modifier.width(80.dp))
            }


        }

//        Spacer(Modifier.height(16.dp))
        HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

        // Leave button
        Surface(
            onClick = onLeaveSession,
            color = ZoomColors.Error,
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
                textAlign = TextAlign.Center
            )
        }
    }
}

// ==================== CHAT BOTTOM SHEET ====================

/**
 * Segmented pill tab selector for switching between Everyone and Host chat tabs.
 */
@Composable
fun ChatTabSelector(
    selectedTab: ChatTab,
    unreadHostMessageCount: Int,
    onTabSelected: (ChatTab) -> Unit
) {
    Surface(
        color = ZoomColors.DarkOverlay,
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier.padding(4.dp)
        ) {
            ChatTab.entries.forEach { tab ->
                ChatTabPill(
                    tab = tab,
                    isSelected = selectedTab == tab,
                    badgeCount = if (tab == ChatTab.HOST && selectedTab != ChatTab.HOST) unreadHostMessageCount else 0,
                    onClick = { onTabSelected(tab) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/**
 * Individual pill button within the chat tab selector, with optional unread badge.
 */
@Composable
fun ChatTabPill(
    tab: ChatTab,
    isSelected: Boolean,
    badgeCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        color = if (isSelected) ZoomColors.Primary else Color.Transparent,
        shape = RoundedCornerShape(20.dp),
        modifier = modifier
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    tab.displayName,
                    color = if (isSelected) Color.White else Color.White.copy(alpha = 0.5f),
                    fontSize = 14.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
                if (badgeCount > 0) {
                    Spacer(Modifier.width(6.dp))
                    Surface(
                        color = ZoomColors.Error,
                        shape = CircleShape,
                        modifier = Modifier.size(18.dp)
                    ) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                if (badgeCount > 99) "99+" else badgeCount.toString(),
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Chat interface bottom sheet with message list and input field.
 * Supports message reactions via long-press.
 * Participants see a tab switcher for Everyone / Host private chat.
 */
@Composable
fun ChatBottomSheetContent(
    messages: List<ChatMessage>,
    hostMessages: List<ChatMessage>,
    selectedTab: ChatTab,
    isHost: Boolean,
    unreadHostMessageCount: Int,
    onTabSelected: (ChatTab) -> Unit,
    onSendMessage: (String) -> Unit,
    onSendHostMessage: (String) -> Unit,
    onReactionClick: (String, String) -> Unit
) {
    var messageText by remember { mutableStateOf("") }
    val displayedMessages = if (selectedTab == ChatTab.HOST && !isHost) hostMessages else messages
    val listState = rememberLazyListState()

    LaunchedEffect(displayedMessages.size) { if (displayedMessages.isNotEmpty()) listState.animateScrollToItem(displayedMessages.size - 1) }

    Column(Modifier.fillMaxWidth().height(500.dp)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Chat", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }

        // Show tab selector for participants only
        if (!isHost) {
            ChatTabSelector(
                selectedTab = selectedTab,
                unreadHostMessageCount = unreadHostMessageCount,
                onTabSelected = onTabSelected
            )
            Spacer(Modifier.height(8.dp))
        }

        HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

        if (displayedMessages.isEmpty()) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (selectedTab == ChatTab.HOST && !isHost) "No messages with host yet"
                    else "No messages yet",
                    color = Color.White.copy(alpha = 0.4f),
                    fontSize = 14.sp
                )
            }
        } else {
            LazyColumn(state = listState, modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
                items(displayedMessages) { message ->
                    ChatMessageBubble(message, onReactionClick)
                }
            }
        }

        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = messageText,
                onValueChange = { messageText = it },
                placeholder = {
                    Text(
                        if (selectedTab == ChatTab.HOST && !isHost) "Message host privately..." else "Type a message...",
                        color = Color.White.copy(alpha = 0.5f)
                    )
                },
                modifier = Modifier.weight(1f).height(52.dp),
                shape = RoundedCornerShape(24.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = ZoomColors.Primary,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.3f)
                )
            )
            Spacer(Modifier.width(8.dp))
            FilledIconButton(
                onClick = {
                    if (messageText.isNotBlank()) {
                        if (selectedTab == ChatTab.HOST && !isHost) onSendHostMessage(messageText) else onSendMessage(messageText)
                        messageText = ""
                    }
                },
                modifier = Modifier.size(48.dp),
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = ZoomColors.Primary)
            ) {
                Icon(Icons.Filled.Send, "Send", tint = Color.White)
            }
        }
    }
}

// ==================== CHAT MESSAGE BUBBLE ====================

/**
 * Individual chat message bubble with reaction support via long-press.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatMessageBubble(
    message: ChatMessage,
    onReactionClick: (String, String) -> Unit
) {
    val isMe = message.isFromMe
    var showReactionPicker by remember { mutableStateOf(false) }
    var reactionDetail by remember { mutableStateOf<Pair<String, List<String>>?>(null) }
    val quickReactions = listOf("👍", "❤️", "😂", "😮", "👏")
    val hasReactions = message.reactions.isNotEmpty()

    Column(
        Modifier.fillMaxWidth().padding(vertical = if (hasReactions) 8.dp else 4.dp),
        horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
    ) {
        if (!isMe) {
            Text(message.senderName, color = ZoomColors.Primary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        }

        Box {
            Card(
                colors = CardDefaults.cardColors(containerColor = if (isMe) ZoomColors.Primary else ZoomColors.DarkOverlay),
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

            if (hasReactions) {
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .offset(x = 8.dp, y = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    message.reactions.forEach { (emoji, users) ->
                        Surface(
                            color = ZoomColors.DarkOverlay,
                            shape = RoundedCornerShape(12.dp),
                            shadowElevation = 2.dp,
                            border = BorderStroke(1.dp, ZoomColors.DarkDivider),
                            modifier = Modifier.combinedClickable(
                                onClick = { onReactionClick(message.messageId, emoji) },
                                onLongClick = { reactionDetail = emoji to users.values.toList() }
                            )
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

        if (showReactionPicker) {
            Surface(
                color = ZoomColors.DarkOverlay,
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

        if (reactionDetail != null) {
            val (emoji, names) = reactionDetail!!
            AlertDialog(
                onDismissRequest = { reactionDetail = null },
                containerColor = ZoomColors.DarkCard,
                shape = RoundedCornerShape(20.dp),
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(emoji, fontSize = 28.sp)
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                "Reactions",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "${names.size} ${if (names.size == 1) "person" else "people"}",
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 12.sp
                            )
                        }
                    }
                },
                text = {
                    Column {
                        HorizontalDivider(color = ZoomColors.DarkDivider)
                        Spacer(Modifier.height(8.dp))
                        names.forEach { name ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = ZoomColors.DarkAvatarBg,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Text(
                                            name.take(1).uppercase(),
                                            color = Color.White,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    name,
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { reactionDetail = null }) {
                        Text("Close", color = Color.White.copy(alpha = 0.7f), fontWeight = FontWeight.Medium)
                    }
                }
            )
        }
    }
}

// ==================== WHITEBOARD BOTTOM SHEET ====================

/** Interactive whiteboard/drawing canvas with color picker and clear button. */
@Composable
fun WhiteboardBottomSheetContent() {
    var paths by remember { mutableStateOf(listOf<DrawingPath>()) }
    var currentPath by remember { mutableStateOf<Path?>(null) }
    var selectedColor by remember { mutableStateOf(Color.Black) }
    val colors = listOf(Color.Black, Color.Red, Color.Blue, Color.Green, ZoomColors.Orange, ZoomColors.Purple)

    Column(Modifier.fillMaxWidth().height(500.dp)) {
        Row(Modifier.fillMaxWidth().background(ZoomColors.Orange).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
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
                    border = if (selectedColor == color) BorderStroke(2.dp, Color.White) else null,
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
        Row(
            Modifier.fillMaxWidth().background(ZoomColors.Cyan).padding(16.dp),
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
            Row(
                Modifier.fillMaxWidth().padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { },
                    colors = ButtonDefaults.buttonColors(containerColor = ZoomColors.Cyan),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Create Room")
                }
                OutlinedButton(onClick = { }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Open All")
                }
            }
        }

        HorizontalDivider()

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            items(subsessions) { room ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (selectedRoom == room.id) ZoomColors.Cyan.copy(alpha = 0.1f) else ZoomColors.LightSurface
                    ),
                    border = if (selectedRoom == room.id) BorderStroke(2.dp, ZoomColors.Cyan) else null,
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
                                color = ZoomColors.Cyan.copy(alpha = 0.2f)
                            ) {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text(room.name.take(1), color = ZoomColors.Cyan, fontWeight = FontWeight.Bold)
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
                            colors = ButtonDefaults.buttonColors(containerColor = ZoomColors.Cyan),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text("Join", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        if (selectedRoom != null) {
            Surface(Modifier.fillMaxWidth(), color = ZoomColors.LightSurface) {
                Row(
                    Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Button(
                        onClick = { },
                        colors = ButtonDefaults.buttonColors(containerColor = ZoomColors.Cyan)
                    ) {
                        Text("Join Selected Room")
                    }
                }
            }
        }
    }
}

// ==================== PARTICIPANTS BOTTOM SHEET ====================

/**
 * Bottom sheet showing all participants in the session.
 * Host sees mic toggle buttons to allow/disallow participants to speak.
 */
@Composable
fun ParticipantsBottomSheet(
    participantCount: Int,
    displayName: String,
    isHost: Boolean,
    hostRole: ParticipantRole,
    hostImageUrl: String?,
    remoteParticipants: List<Participant>,
    onToggleParticipantMute: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(400.dp)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .background(ZoomColors.DarkCard)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Person, null, tint = Color.White, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(12.dp))
            Text("Participants ($participantCount)", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Current user (self)
            item {
                Row(

                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ParticipantAvatar(
                        name = displayName,
                        imageUrl = hostImageUrl,
                        backgroundColor = ZoomColors.Success
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(displayName, fontWeight = FontWeight.Medium, color = Color.White)
                            Spacer(Modifier.width(4.dp))
                            Text("(You)", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                        }
                        Text(
                            hostRole.displayName,
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // Remote participants sorted by role: Host, Manager, Participant, Guest
            val sortedParticipants = remoteParticipants.sortedBy { it.role.ordinal }
            items(sortedParticipants) { participant ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ParticipantAvatar(
                        name = participant.name,
                        imageUrl = participant.imageUrl,
                        backgroundColor = ZoomColors.BlueGrey
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(participant.name, fontWeight = FontWeight.Medium, color = Color.White)
                        Text(
                            participant.role.displayName,
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 12.sp
                        )
                    }
                    if ((isHost || hostRole == ParticipantRole.MANAGER) &&
                        participant.role != ParticipantRole.HOST &&
                        participant.role != ParticipantRole.MANAGER
                    ) {
                        IconButton(
                            onClick = { onToggleParticipantMute(participant.id) }
                        ) {
                            Icon(
                                imageVector = if (participant.isMuted) Icons.Outlined.MicOff else Icons.Outlined.Mic,
                                contentDescription = if (participant.isMuted) "Unmute ${participant.name}" else "Mute ${participant.name}",
                                tint = if (participant.isMuted) ZoomColors.Error else ZoomColors.Success,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Avatar composable that shows a profile image if available, otherwise shows the first letter.
 */
@Composable
private fun ParticipantAvatar(
    name: String,
    imageUrl: String?,
    backgroundColor: Color
) {
    Surface(
        shape = CircleShape,
        color = backgroundColor,
        modifier = Modifier.size(40.dp)
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(name.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}

// ==================== PARTICIPANT MORE OPTIONS BOTTOM SHEET ====================

/**
 * Simplified More Options bottom sheet for participants.
 * Contains quick reactions, caption language selection, and leave meeting button.
 * No Recording, Whiteboard, or Breakout Rooms options.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParticipantMoreOptionsBottomSheet(
    showTranscription: Boolean = false,
    selectedTranscriptionLanguage: String,
    availableTranscriptionLanguages: List<String>,
    onSendReaction: (String) -> Unit,
    onToggleTranscription: () -> Unit = {},
    onSelectTranscriptionLanguage: (String) -> Unit,
    onLeaveSession: () -> Unit
) {
    val quickReactions = listOf("👍", "👏", "❤️", "😂", "😮", "🎉")
    var showLanguageDropdown by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
    ) {
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

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                quickReactions.take(6).forEach { emoji ->
                    EmojiButton(emoji = emoji, onClick = { onSendReaction(emoji) })
                }
            }
        }

        HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

        // Captions toggle
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            MoreOptionItem(
                icon = Icons.Default.List,
                label = "Captions",
                isActive = showTranscription,
                activeColor = ZoomColors.Purple,
                onClick = onToggleTranscription
            )
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
                        Text(selectedTranscriptionLanguage, color = Color.White, fontSize = 16.sp)
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
                    modifier = Modifier.background(ZoomColors.DarkCard)
                ) {
                    availableTranscriptionLanguages.forEach { language ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    language,
                                    color = if (language == selectedTranscriptionLanguage) ZoomColors.Purple else Color.White
                                )
                            },
                            onClick = {
                                onSelectTranscriptionLanguage(language)
                                showLanguageDropdown = false
                            },
                            leadingIcon = if (language == selectedTranscriptionLanguage) {
                                { Icon(Icons.Default.Check, null, tint = ZoomColors.Purple) }
                            } else null
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

        // Leave button
        Surface(
            onClick = onLeaveSession,
            color = ZoomColors.Error,
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
                textAlign = TextAlign.Center
            )
        }
    }
}

// ==================== PREVIEWS ====================

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, backgroundColor = 0xFF1C1C1E)
@Composable
private fun MoreOptionsBottomSheetPreview() {
    MoreOptionsBottomSheet(
        isRecording = false,
        showWhiteboard = false,
        showTranscription = true,
        showSubsessions = false,
        showReactions = true,
        isHost = true,
        selectedTranscriptionLanguage = "English",
        availableTranscriptionLanguages = listOf("English", "Spanish", "French", "German"),
        onToggleRecording = {},
        onToggleWhiteboard = {},
        onToggleTranscription = {},
        onToggleSubsessions = {},
        onToggleReactions = {},
        onLeaveSession = {},
        onSendReaction = {},
        onSelectTranscriptionLanguage = {}
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF1C1C1E)
@Composable
private fun ChatBottomSheetContentPreview() {
    val sampleMessages = listOf(
        ChatMessage(
            id = "1",
            messageId = "msg1",
            senderName = "Alice",
            message = "Hello everyone!",
            timestamp = System.currentTimeMillis(),
            isFromMe = false,
            reactions = mapOf("👍" to mapOf("bob_id" to "Bob"))
        ),
        ChatMessage(
            id = "2",
            messageId = "msg2",
            senderName = "Me",
            message = "Hi Alice!",
            timestamp = System.currentTimeMillis(),
            isFromMe = true,
            reactions = emptyMap()
        ),
        ChatMessage(
            id = "3",
            messageId = "msg3",
            senderName = "Bob",
            message = "Great to see you all!",
            timestamp = System.currentTimeMillis(),
            isFromMe = false,
            reactions = emptyMap()
        )
    )
    ChatBottomSheetContent(
        messages = sampleMessages,
        hostMessages = emptyList(),
        selectedTab = ChatTab.EVERYONE,
        isHost = false,
        unreadHostMessageCount = 2,
        onTabSelected = {},
        onSendMessage = {},
        onSendHostMessage = {},
        onReactionClick = { _, _ -> }
    )
}

@Preview(showBackground = true)
@Composable
private fun WhiteboardBottomSheetContentPreview() {
    WhiteboardBottomSheetContent()
}

@Preview(showBackground = true, backgroundColor = 0xFF1C1C1E)
@Composable
private fun SubsessionsBottomSheetContentPreview() {
    SubsessionsBottomSheetContent(isHost = true)
}

@Preview(showBackground = true, backgroundColor = 0xFF1C1C1E)
@Composable
private fun ParticipantsBottomSheetPreview() {
    ParticipantsBottomSheet(
        participantCount = 4,
        displayName = "John Doe",
        isHost = true,
        hostRole = ParticipantRole.HOST,
        hostImageUrl = null,
        remoteParticipants = listOf(
            Participant(id = "1", name = "Alice", role = ParticipantRole.PARTICIPANT),
            Participant(id = "2", name = "Bob", role = ParticipantRole.MANAGER),
            Participant(id = "3", name = "Charlie", role = ParticipantRole.GUEST, isMuted = false)
        ),
        onToggleParticipantMute = {}
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, backgroundColor = 0xFF1C1C1E)
@Composable
private fun ParticipantMoreOptionsBottomSheetPreview() {
    ParticipantMoreOptionsBottomSheet(
        selectedTranscriptionLanguage = "English",
        availableTranscriptionLanguages = listOf("English", "Spanish", "French", "German"),
        onSendReaction = {},
        onSelectTranscriptionLanguage = {},
        onLeaveSession = {}
    )
}

