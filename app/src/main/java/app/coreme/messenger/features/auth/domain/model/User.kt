package app.coreme.messenger.features.auth.domain.model

data class User(
    val id: String,
    val username: String,
    val displayName: String,
    val email: String? = null,
    val bio: String? = null,
    val avatarUrl: String? = null,
    val isOnline: Boolean = false,
)
