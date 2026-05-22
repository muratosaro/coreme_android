package app.coreme.messenger.features.channels.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ChannelDto(
    val id: String = "",
    val name: String = "",
    val description: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("subscriber_count") val subscriberCount: Int = 0,
    @SerialName("is_subscribed") val isSubscribed: Boolean = false,
    @SerialName("owner_id") val ownerId: String = "",
    @SerialName("last_post_at") val lastPostAt: String? = null,
)

@Serializable
data class ChannelPostDto(
    val id: String = "",
    @SerialName("channel_id") val channelId: String = "",
    val content: String = "",
    @SerialName("author_id") val authorId: String = "",
    @SerialName("author_name") val authorName: String = "",
    @SerialName("view_count") val viewCount: Int = 0,
    @SerialName("created_at") val createdAt: String = "",
)
