package app.coreme.messenger.features.chats.domain.usecase

import app.coreme.messenger.features.chats.domain.repository.ChatsRepository
import javax.inject.Inject

class DeleteMessageUseCase @Inject constructor(private val repository: ChatsRepository) {
    suspend operator fun invoke(chatId: String, msgId: String): Result<Unit> =
        repository.deleteMessage(chatId, msgId)
}
