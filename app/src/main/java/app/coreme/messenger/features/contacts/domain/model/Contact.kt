package app.coreme.messenger.features.contacts.domain.model

import java.time.Instant

data class Contact(
    val id: String,
    val nickname: String? = null,
    val username: String,
    val displayName: String,
    val avatarUrl: String? = null,
    val isOnline: Boolean = false,
    val lastSeen: Instant? = null,
    val bio: String? = null,
) {
    val visibleName: String get() = nickname ?: displayName
}
