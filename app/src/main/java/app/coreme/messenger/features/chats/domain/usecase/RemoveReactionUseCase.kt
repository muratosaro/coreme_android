package app.coreme.messenger.features.chats.domain.usecase

import app.coreme.messenger.features.chats.domain.repository.ChatsRepository
import javax.inject.Inject

class RemoveReactionUseCase @Inject constructor(private val repository: ChatsRepository) {
    suspend operator fun invoke(chatId: String, msgId: String, emoji: String): Result<Unit> =
        repository.removeReaction(chatId, msgId, emoji)
}
