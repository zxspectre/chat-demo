package chat.backend

/**
 * Represents a group conversation.
 */
data class Conversation(
    val id: Long,
    val name: String,
    val participants: MutableSet<String> = mutableSetOf()
) {
    fun addParticipant(userName: String) {
        participants.add(userName)
    }

    fun removeParticipant(userName: String) {
        participants.remove(userName)
    }

    fun hasParticipant(userName: String): Boolean {
        return participants.contains(userName)
    }

    override fun toString(): String {
        return "$name (ID: $id)"
    }
}
