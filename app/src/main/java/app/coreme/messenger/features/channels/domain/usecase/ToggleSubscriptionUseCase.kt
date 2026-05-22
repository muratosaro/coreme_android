package app.coreme.messenger.features.channels.domain.usecase

import app.coreme.messenger.features.channels.domain.repository.ChannelsRepository
import javax.inject.Inject

class ToggleSubscriptionUseCase @Inject constructor(private val repository: ChannelsRepository) {
    suspend operator fun invoke(channelId: String, isSubscribed: Boolean): Result<Unit> =
        if (isSubscribed) repository.unsubscribe(channelId) else repository.subscribe(channelId)
}
