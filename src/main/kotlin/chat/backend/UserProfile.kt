package chat.backend

/**
 * Profile metadata for a chat user, keyed by their (unique) name.
 *
 * Age and eye color are optional so a user can exist with just a name.
 */
data class UserProfile(
    val name: String,
    val age: Int? = null,
    val eyeColor: String? = null
)
