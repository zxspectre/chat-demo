package chat.backend.repository

import chat.backend.Conversation
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

class InMemoryConversationRepository : ConversationRepository {
    private val conversations = ConcurrentHashMap<Long, Conversation>()
    private val idGenerator = AtomicLong(1)

    override fun create(name: String, participants: List<String>): Conversation {
        val id = idGenerator.incrementAndGet()
        val conversation = Conversation(
            id = id,
            name = name,
            participants = participants.toMutableSet()
        )
        conversations[id] = conversation
        return conversation
    }

    override fun findById(id: Long): Conversation? {
        return conversations[id]
    }

    override fun findAll(): List<Conversation> {
        return conversations.values.toList()
    }

    override fun findByParticipant(userName: String): List<Conversation> {
        return conversations.values.filter { it.hasParticipant(userName) }
    }

    override fun addParticipant(conversationId: Long, userName: String): Boolean {
        val conversation = conversations[conversationId] ?: return false
        conversation.addParticipant(userName)
        return true
    }
}
