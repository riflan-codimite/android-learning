package com.yourcompany.zoomsession.model

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path

/**
 * Data class representing a chat message in the Zoom session.
 *
 * @property id Unique identifier based on timestamp
 * @property messageId UUID for tracking reactions to this message
 * @property senderName Display name of the message sender
 * @property message The actual text content of the message
 * @property timestamp Unix timestamp when message was sent
 * @property isFromMe True if current user sent this message (for UI positioning)
 * @property reactions Map of emoji to map of (userId to userName) who reacted with that emoji
 */
data class ChatMessage(
    val id: String = System.currentTimeMillis().toString(),
    val messageId: String = java.util.UUID.randomUUID().toString(),
    val senderName: String,
    val message: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isFromMe: Boolean = false,
    val isPrivate: Boolean = false,
    val reactions: Map<String, Map<String, String>> = emptyMap()
)

/**
 * Enum representing the chat tab selection in the bottom sheet.
 */
enum class ChatTab(val displayName: String) {
    EVERYONE("Everyone"),
    HOST("Host")
}

/**
 * Data class representing a live transcription/caption message.
 *
 * @property id Unique identifier based on timestamp
 * @property speakerName Name of the person speaking
 * @property originalText The transcribed text in the original spoken language
 * @property translatedText Optional translated text if translation is enabled
 * @property timestamp Unix timestamp when transcription was captured
 */
data class TranscriptionMessage(
    val id: String = System.currentTimeMillis().toString(),
    val speakerName: String,
    val originalText: String,
    val translatedText: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Data class representing a drawing path on the whiteboard.
 *
 * @property path The Compose Path object containing the drawing coordinates
 * @property color Color of the stroke
 * @property strokeWidth Width of the stroke in pixels
 */
data class DrawingPath(
    val path: Path,
    val color: Color,
    val strokeWidth: Float
)

/**
 * Data class representing a reaction emoji sent during the session.
 *
 * @property id Unique identifier based on timestamp
 * @property emoji The emoji string (e.g., "👍", "❤️")
 * @property senderName Display name of who sent the reaction
 * @property senderId User ID for tracking
 * @property timestamp Unix timestamp when reaction was sent
 */
data class ReactionEmoji(
    val id: String = System.currentTimeMillis().toString(),
    val emoji: String,
    val senderName: String,
    val senderId: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Data class representing a raised hand, matching the web app's RaiseHandPayload.
 *
 * @property userId User ID of the participant who raised/lowered hand
 * @property userName Display name for UI rendering
 * @property raised True if hand is raised, false if lowered
 * @property timestamp Unix timestamp when the action occurred
 */
data class RaisedHand(
    val userId: String,
    val userName: String,
    val raised: Boolean = true,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Data class representing a talk request from a participant, matching the web app's talkRequest payload.
 *
 * @property userId User ID of the participant requesting to talk
 * @property userName Display name for UI rendering
 * @property requesting True if requesting, false if cancelling
 * @property timestamp Unix timestamp when the request was made
 */
data class TalkRequest(
    val userId: String,
    val userName: String,
    val requesting: Boolean = true,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Data class representing a breakout room/subsession.
 *
 * @property id Unique identifier for the subsession
 * @property name Display name of the subsession (e.g., "Breakout Room 1")
 * @property participantCount Number of participants in this subsession
 */
data class Subsession(
    val id: String,
    val name: String,
    val participantCount: Int
)

/**
 * Enum representing the role of a participant in the Zoom session.
 */
enum class ParticipantRole(val displayName: String) {
    HOST("Host"),
    MANAGER("Manager"),
    PARTICIPANT("Participant"),
    GUEST("Guest")
}

/**
 * Data class representing a participant in the Zoom session.
 *
 * @property id Unique identifier for the participant
 * @property name Display name of the participant
 * @property role Role in the session
 * @property imageUrl Optional URL/path for the participant's profile picture
 * @property isMuted Whether the participant is currently muted by the host
 */
data class Participant(
    val id: String,
    val name: String,
    val role: ParticipantRole = ParticipantRole.PARTICIPANT,
    val imageUrl: String? = null,
    val isMuted: Boolean = true
)
