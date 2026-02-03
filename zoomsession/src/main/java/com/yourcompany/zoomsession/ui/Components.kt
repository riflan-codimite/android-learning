package com.yourcompany.zoomsession.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yourcompany.zoomsession.model.ReactionEmoji
import com.yourcompany.zoomsession.model.WaitingRoomUser

// ==================== BOTTOM BAR BUTTON ====================

/**
 * Reusable button component for the bottom control bar.
 * Displays an icon with a label underneath, with color states for active/inactive.
 */
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

// ==================== MORE OPTION ITEM ====================

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
            textAlign = TextAlign.Center,
            maxLines = 2
        )
    }
}

// ==================== EMOJI BUTTON ====================

/** Circular button displaying a single emoji. Used in the quick reactions section. */
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

// ==================== STATUS CHIP ====================

/** Small colored chip displaying status text (e.g., "Host", "Muted"). */
@Composable
fun StatusChip(text: String, color: Color) {
    Surface(color = color.copy(alpha = 0.2f), shape = RoundedCornerShape(16.dp)) {
        Text(
            text,
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

// ==================== RAISED HAND BUBBLE ====================

/** Persistent raised hand indicator bubble. Stays visible until lowered. */
@Composable
fun RaisedHandBubble(reaction: ReactionEmoji) {
    Surface(
        modifier = Modifier.padding(vertical = 4.dp),
        color = ZoomColors.Orange,
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

// ==================== ANIMATED REACTION BUBBLE ====================

/** Animated reaction bubble that floats up and fades out over 3 seconds. */
@Composable
fun AnimatedReactionBubble(reaction: ReactionEmoji) {
    var animationProgress by remember { mutableStateOf(0f) }

    val progress by animateFloatAsState(
        targetValue = animationProgress,
        animationSpec = tween(durationMillis = 3000),
        label = "progress"
    )

    val offsetY = (1f - progress) * 300f - 150f
    val alpha = if (progress > 0.7f) 1f - ((progress - 0.7f) / 0.3f) else 1f
    val scale = 1f - (progress * 0.2f)

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

// ==================== WAITING ROOM BUTTON ====================

/** Button for accessing the waiting room with a notification badge. Host only. */
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
                    containerColor = (if (isActive) ZoomColors.Brown else ZoomColors.BlueGrey).copy(alpha = 0.2f)
                ),
                shape = CircleShape
            ) {
                Icon(
                    if (isActive) Icons.Default.Lock else Icons.Outlined.Lock,
                    "Waiting Room",
                    tint = if (isActive) ZoomColors.Brown else ZoomColors.BlueGrey,
                    modifier = Modifier.size(26.dp)
                )
            }
            if (waitingCount > 0) {
                Surface(
                    modifier = Modifier.align(Alignment.TopEnd).offset(x = 4.dp, y = (-4).dp),
                    color = ZoomColors.Error,
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

// ==================== WAITING ROOM USER CARD ====================

@Composable
fun WaitingRoomUserCard(
    user: WaitingRoomUser,
    onAdmit: () -> Unit,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = ZoomColors.LightSurface)
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
                    color = ZoomColors.Brown.copy(alpha = 0.2f)
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(user.name.take(1).uppercase(), color = ZoomColors.Brown, fontWeight = FontWeight.Bold, fontSize = 18.sp)
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
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = ZoomColors.Success)
                ) {
                    Icon(Icons.Default.Check, "Admit", tint = Color.White, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(8.dp))
                FilledIconButton(
                    onClick = onRemove,
                    modifier = Modifier.size(40.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = ZoomColors.Error)
                ) {
                    Icon(Icons.Default.Close, "Remove", tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

// ==================== WAITING ROOM OVERLAY ====================

/** Full-screen overlay shown to participants waiting for the host. */
@Composable
fun WaitingRoomOverlay(sessionName: String, onLeave: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(ZoomColors.DarkBackground, ZoomColors.DarkSecondaryBackground, ZoomColors.DarkTertiaryBackground)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Surface(
                modifier = Modifier.size(120.dp),
                shape = CircleShape,
                color = ZoomColors.Brown.copy(alpha = 0.15f),
                border = androidx.compose.foundation.BorderStroke(3.dp, ZoomColors.Brown.copy(alpha = 0.3f))
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Lock, null, tint = ZoomColors.Brown, modifier = Modifier.size(55.dp))
                }
            }

            Spacer(Modifier.height(32.dp))
            Text("Waiting Room", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            Text("The meeting hasn't started yet", color = Color.White.copy(alpha = 0.9f), fontSize = 16.sp)
            Spacer(Modifier.height(4.dp))
            Text("You'll be admitted when the host arrives", color = Color.White.copy(alpha = 0.6f), fontSize = 14.sp)
            Spacer(Modifier.height(32.dp))

            Surface(
                color = Color.White.copy(alpha = 0.1f),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Phone, null, tint = ZoomColors.Success, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(12.dp))
                        Text(sessionName, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(color = ZoomColors.Brown, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Waiting for host to start...", color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
                    }
                }
            }

            Spacer(Modifier.height(48.dp))

            Surface(
                color = ZoomColors.Brown.copy(alpha = 0.2f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Info, null, tint = ZoomColors.Brown, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(12.dp))
                    Text("You'll be notified when admitted", color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp)
                }
            }

            Spacer(Modifier.height(32.dp))

            OutlinedButton(
                onClick = onLeave,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = ZoomColors.Error),
                border = androidx.compose.foundation.BorderStroke(1.dp, ZoomColors.Error.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth(0.6f)
            ) {
                Icon(Icons.Default.Close, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Leave Waiting Room", fontWeight = FontWeight.Medium)
            }
        }
    }
}
