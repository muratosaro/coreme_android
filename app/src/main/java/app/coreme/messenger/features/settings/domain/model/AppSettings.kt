package app.coreme.messenger.features.settings.domain.model

data class AppSettings(
    val notificationsEnabled: Boolean = true,
    val soundEnabled: Boolean = true,
    val showReadReceipts: Boolean = true,
    val lastSeenVisible: Boolean = true,
    val autoReplyEnabled: Boolean = false,
    val autoReplyMessage: String? = null,
)
