package app.coreme.messenger.features.channels.data.repository

import app.coreme.messenger.core.network.safeApiCall
import app.coreme.messenger.features.channels.data.api.ChannelsApi
import app.coreme.messenger.features.channels.data.mapper.toDomain
import app.coreme.messenger.features.channels.domain.model.Channel
import app.coreme.messenger.features.channels.domain.model.ChannelPost
import app.coreme.messenger.features.channels.domain.repository.ChannelsRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChannelsRepositoryImpl @Inject constructor(
    private val api: ChannelsApi,
) : ChannelsRepository {

    override suspend fun getChannels(): Result<List<Channel>> = safeApiCall {
        api.getChannels().map { it.toDomain() }
    }

    override suspend fun getChannelPosts(channelId: String): Result<List<ChannelPost>> = safeApiCall {
        api.getChannelPosts(channelId).map { it.toDomain() }
    }

    override suspend fun subscribe(channelId: String): Result<Unit> = safeApiCall {
        api.subscribe(channelId)
    }

    override suspend fun unsubscribe(channelId: String): Result<Unit> = safeApiCall {
        api.unsubscribe(channelId)
    }
}
