package chat.backend

import java.time.Instant

/**
 * Represents a chat message with optional image attachment.
 */
data class Message(
    val id: Long,
    val conversationId: Long,
    val senderName: String,
    val text: String,
    val imageData: ByteArray? = null,
    val timestamp: Instant = Instant.now()
) {
    fun hasImage(): Boolean = imageData != null

    override fun toString(): String {
        val imgIndicator = if (hasImage()) " [IMAGE]" else ""
        return "$senderName: $text$imgIndicator"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as Message
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}
