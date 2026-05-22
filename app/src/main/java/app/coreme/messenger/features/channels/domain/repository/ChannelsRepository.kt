package app.coreme.messenger.features.channels.domain.repository

import app.coreme.messenger.features.channels.domain.model.Channel
import app.coreme.messenger.features.channels.domain.model.ChannelPost

interface ChannelsRepository {
    suspend fun getChannels(): Result<List<Channel>>
    suspend fun getChannelPosts(channelId: String): Result<List<ChannelPost>>
    suspend fun subscribe(channelId: String): Result<Unit>
    suspend fun unsubscribe(channelId: String): Result<Unit>
}
