package app.coreme.messenger.features.channels.domain.model

import java.time.Instant

data class Channel(
    val id: String,
    val name: String,
    val description: String?,
    val avatarUrl: String?,
    val subscriberCount: Int,
    val isSubscribed: Boolean,
    val ownerId: String,
    val lastPostAt: Instant?,
)

data class ChannelPost(
    val id: String,
    val channelId: String,
    val content: String,
    val authorId: String,
    val authorName: String,
    val viewCount: Int,
    val createdAt: Instant,
)
