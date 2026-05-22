package app.coreme.messenger.features.channels.domain.usecase

import app.coreme.messenger.features.channels.domain.model.Channel
import app.coreme.messenger.features.channels.domain.repository.ChannelsRepository
import javax.inject.Inject

class GetChannelsUseCase @Inject constructor(private val repository: ChannelsRepository) {
    suspend operator fun invoke(): Result<List<Channel>> = repository.getChannels()
}
