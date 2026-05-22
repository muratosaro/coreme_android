package app.coreme.messenger.features.channels.domain.usecase

import app.coreme.messenger.features.channels.domain.model.ChannelPost
import app.coreme.messenger.features.channels.domain.repository.ChannelsRepository
import javax.inject.Inject

class GetChannelPostsUseCase @Inject constructor(private val repository: ChannelsRepository) {
    suspend operator fun invoke(channelId: String): Result<List<ChannelPost>> =
        repository.getChannelPosts(channelId)
}
