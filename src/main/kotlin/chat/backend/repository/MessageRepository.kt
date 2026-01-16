package chat.backend.repository

import chat.backend.Message

interface MessageRepository {
    fun create(conversationId: Long, senderName: String, text: String): Message
    fun findByConversationId(conversationId: Long): List<Message>
}
