package app.coreme.messenger.features.chats.domain.usecase

import app.coreme.messenger.features.chats.domain.model.Message
import app.coreme.messenger.features.chats.domain.repository.ChatsRepository
import javax.inject.Inject

class EditMessageUseCase @Inject constructor(private val repository: ChatsRepository) {
    suspend operator fun invoke(chatId: String, msgId: String, content: String): Result<Message> {
        if (content.isBlank()) return Result.failure(Exception("Message cannot be empty"))
        return repository.editMessage(chatId, msgId, content.trim())
    }
}
