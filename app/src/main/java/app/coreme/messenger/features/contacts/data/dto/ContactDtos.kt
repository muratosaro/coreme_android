package app.coreme.messenger.features.contacts.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ContactDto(
    val id: String,
    val nickname: String? = null,
    val username: String,
    @SerialName("display_name") val displayName: String,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("is_online") val isOnline: Boolean = false,
    @SerialName("last_seen") val lastSeen: String? = null,
    val bio: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
)

@Serializable
data class AddContactRequest(
    @SerialName("contact_id") val contactId: String,
    val nickname: String? = null,
)
