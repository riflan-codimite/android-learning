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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yourcompany.zoomsession.model.RaisedHand
import com.yourcompany.zoomsession.model.ReactionEmoji

// ==================== BOTTOM BAR BUTTON ====================

/**
 * Reusable button component for the bottom control bar.
 * Material 3 styled with icon and label, proper touch targets.
 */
@Composable
fun BottomBarButton(
    onClick: () -> Unit,
    icon: ImageVector,
    label: String,
    isActive: Boolean,
    activeColor: Color,
    inactiveColor: Color,
    modifier: Modifier = Modifier
) {
    val buttonColor = if (isActive) activeColor else inactiveColor

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .widthIn(min = 64.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(
                    if (!isActive && inactiveColor == ZoomColors.Error)
                        ZoomColors.Error.copy(alpha = 0.15f)
                    else
                        Color.Transparent
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = label,
                tint = buttonColor,
                modifier = Modifier.size(28.dp)
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            label,
            color = buttonColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            maxLines = 1
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
        modifier = Modifier.size(48.dp)
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(emoji, fontSize = 20.sp)
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
fun RaisedHandBubble(raisedHand: RaisedHand) {
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
            Text("✋", fontSize = 24.sp)
            Spacer(Modifier.width(8.dp))
            Text(raisedHand.userName.split(" ").first(), fontSize = 12.sp, color = Color.White)
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

// ==================== PREVIEWS ====================

@Preview(showBackground = true, backgroundColor = 0xFF1C1C1E)
@Composable
private fun BottomBarButtonActivePreview() {
    Row(
        modifier = Modifier.padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        BottomBarButton(
            onClick = {},
            icon = Icons.Filled.Notifications,
            label = "Mute",
            isActive = true,
            activeColor = Color.White,
            inactiveColor = ZoomColors.Error
        )
        BottomBarButton(
            onClick = {},
            icon = Icons.Filled.Notifications,
            label = "Unmute",
            isActive = false,
            activeColor = Color.White,
            inactiveColor = ZoomColors.Error
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF1C1C1E)
@Composable
private fun MoreOptionItemPreview() {
    Row(
        modifier = Modifier.padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        MoreOptionItem(
            icon = Icons.Default.Star,
            label = "Record",
            isActive = false,
            activeColor = ZoomColors.Error,
            onClick = {}
        )
        MoreOptionItem(
            icon = Icons.Default.Star,
            label = "Recording",
            isActive = true,
            activeColor = ZoomColors.Error,
            onClick = {}
        )
        MoreOptionItem(
            icon = Icons.Default.Edit,
            label = "Whiteboard",
            isActive = true,
            activeColor = ZoomColors.Orange,
            onClick = {}
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF1C1C1E)
@Composable
private fun EmojiButtonPreview() {
    Row(
        modifier = Modifier.padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        EmojiButton(emoji = "👍", onClick = {})
        EmojiButton(emoji = "❤️", onClick = {})
        EmojiButton(emoji = "😂", onClick = {})
        EmojiButton(emoji = "🎉", onClick = {})
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF1C1C1E)
@Composable
private fun StatusChipPreview() {
    Row(
        modifier = Modifier.padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StatusChip(text = "Host", color = ZoomColors.Primary)
        StatusChip(text = "Muted", color = ZoomColors.Error)
        StatusChip(text = "Speaking", color = ZoomColors.Success)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF1C1C1E)
@Composable
private fun RaisedHandBubblePreview() {
    Column(modifier = Modifier.padding(16.dp)) {
        RaisedHandBubble(
            raisedHand = RaisedHand(
                userId = "user123",
                userName = "John Doe"
            )
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF1C1C1E)
@Composable
private fun AnimatedReactionBubblePreview() {
    Column(modifier = Modifier.padding(16.dp)) {
        AnimatedReactionBubble(
            reaction = ReactionEmoji(
                emoji = "👍",
                senderName = "Alice Smith",
                senderId = "alice456"
            )
        )
    }
}

