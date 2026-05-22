package app.coreme.messenger.features.channels.data.mapper

import app.coreme.messenger.features.channels.data.dto.ChannelDto
import app.coreme.messenger.features.channels.data.dto.ChannelPostDto
import app.coreme.messenger.features.channels.domain.model.Channel
import app.coreme.messenger.features.channels.domain.model.ChannelPost
import java.time.Instant

fun ChannelDto.toDomain(): Channel = Channel(
    id = id,
    name = name,
    description = description,
    avatarUrl = avatarUrl,
    subscriberCount = subscriberCount,
    isSubscribed = isSubscribed,
    ownerId = ownerId,
    lastPostAt = lastPostAt?.let { runCatching { Instant.parse(it) }.getOrNull() },
)

fun ChannelPostDto.toDomain(): ChannelPost = ChannelPost(
    id = id,
    channelId = channelId,
    content = content,
    authorId = authorId,
    authorName = authorName,
    viewCount = viewCount,
    createdAt = runCatching { Instant.parse(createdAt) }.getOrElse { Instant.now() },
)
