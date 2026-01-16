package chat.backend

import chat.backend.repository.ConversationRepository
import chat.backend.repository.InMemoryConversationRepository
import chat.backend.repository.InMemoryMessageRepository
import chat.backend.repository.MessageRepository
import java.util.concurrent.CopyOnWriteArrayList
import java.util.function.Consumer

/**
 * Backend service for managing chat conversations and messages.
 */
class ChatService(
    private val conversationRepository: ConversationRepository = InMemoryConversationRepository(),
    private val messageRepository: MessageRepository = InMemoryMessageRepository()
) {
    private val messageListeners = CopyOnWriteArrayList<Consumer<Message>>()

    companion object {
        private const val MAX_MESSAGE_LENGTH = 10_000
    }

    fun createConversation(name: String, participants: List<String>): Conversation {
        val conversation = conversationRepository.create(name, participants)
        if (messageRepository is InMemoryMessageRepository) {
            messageRepository.initConversation(conversation.id)
        }
        return conversation
    }

    fun getConversation(conversationId: Long): Conversation? {
        return conversationRepository.findById(conversationId)
    }

    fun getConversationsForUser(userName: String): List<Conversation> {
        return conversationRepository.findByParticipant(userName)
    }

    fun getAllConversations(): List<Conversation> {
        return conversationRepository.findAll()
    }

    @JvmOverloads
    fun sendMessage(conversationId: Long, senderName: String, text: String, imageData: ByteArray? = null): Message? {
        val conversation = conversationRepository.findById(conversationId) ?: return null

        // Truncate text to max length, ensure at least 1 char for notifications
        val truncatedText = text.substring(0, text.length.coerceIn(1, MAX_MESSAGE_LENGTH))

        if (!conversation.hasParticipant(senderName)) {
            conversationRepository.addParticipant(conversationId, senderName)
        }

        // TODO: implement image attachment support
        val message = messageRepository.create(conversationId, senderName, truncatedText)

        messageListeners.forEach { it.accept(message) }

        return message
    }

    fun getMessages(conversationId: Long): List<Message> {
        return messageRepository.findByConversationId(conversationId)
    }

    fun addParticipant(conversationId: Long, userName: String): Boolean {
        return conversationRepository.addParticipant(conversationId, userName)
    }

    fun addMessageListener(listener: Consumer<Message>) {
        messageListeners.add(listener)
    }

    fun removeMessageListener(listener: Consumer<Message>) {
        messageListeners.remove(listener)
    }
}
