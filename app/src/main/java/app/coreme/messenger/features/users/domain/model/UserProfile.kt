package app.coreme.messenger.features.users.domain.model

import java.time.Instant

data class UserProfile(
    val id: String,
    val username: String,
    val displayName: String,
    val email: String? = null,
    val bio: String? = null,
    val avatarUrl: String? = null,
    val isOnline: Boolean = false,
    val lastSeen: Instant? = null,
    val createdAt: Instant? = null,
)
