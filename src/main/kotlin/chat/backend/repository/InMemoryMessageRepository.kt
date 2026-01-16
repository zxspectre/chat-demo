package chat.backend.repository

import chat.backend.Message
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicLong

class InMemoryMessageRepository : MessageRepository {
    private val messages = ConcurrentHashMap<Long, CopyOnWriteArrayList<Message>>()
    private val idGenerator = AtomicLong(1)

    override fun create(conversationId: Long, senderName: String, text: String): Message {
        val message = Message(
            id = idGenerator.incrementAndGet(),
            conversationId = conversationId,
            senderName = senderName,
            text = text,
        )
        messages.computeIfAbsent(conversationId) { CopyOnWriteArrayList() }.add(message)
        return message
    }

    override fun findByConversationId(conversationId: Long): List<Message> {
        return messages[conversationId]?.toList() ?: emptyList()
    }

    fun initConversation(conversationId: Long) {
        messages.computeIfAbsent(conversationId) { CopyOnWriteArrayList() }
    }
}
