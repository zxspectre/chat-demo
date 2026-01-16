package chat.backend.repository

import chat.backend.Conversation

interface ConversationRepository {
    fun create(name: String, participants: List<String>): Conversation
    fun findById(id: Long): Conversation?
    fun findAll(): List<Conversation>
    fun findByParticipant(userName: String): List<Conversation>
    fun addParticipant(conversationId: Long, userName: String): Boolean
}
