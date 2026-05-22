package app.coreme.messenger.features.settings.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AppSettingsDto(
    @SerialName("user_id") val userId: String = "",
    @SerialName("notifications_enabled") val notificationsEnabled: Boolean = true,
    @SerialName("sound_enabled") val soundEnabled: Boolean = true,
    @SerialName("show_read_receipts") val showReadReceipts: Boolean = true,
    @SerialName("last_seen_visible") val lastSeenVisible: Boolean = true,
    @SerialName("auto_reply_enabled") val autoReplyEnabled: Boolean = false,
    @SerialName("auto_reply_message") val autoReplyMessage: String? = null,
    val theme: String = "dark",
)

@Serializable
data class UpdateSettingsRequest(
    @SerialName("notifications_enabled") val notificationsEnabled: Boolean? = null,
    @SerialName("sound_enabled") val soundEnabled: Boolean? = null,
    @SerialName("show_read_receipts") val showReadReceipts: Boolean? = null,
    @SerialName("last_seen_visible") val lastSeenVisible: Boolean? = null,
    @SerialName("auto_reply_enabled") val autoReplyEnabled: Boolean? = null,
    @SerialName("auto_reply_message") val autoReplyMessage: String? = null,
)

@Serializable
data class ChangePasswordRequest(
    @SerialName("current_password") val currentPassword: String,
    @SerialName("new_password") val newPassword: String,
)
