package app.coreme.messenger.features.chats.domain.usecase

import app.coreme.messenger.features.chats.domain.model.Message
import app.coreme.messenger.features.chats.domain.repository.ChatsRepository
import javax.inject.Inject

class SendMessageUseCase @Inject constructor(private val repository: ChatsRepository) {
    suspend operator fun invoke(chatId: String, content: String, replyToId: String? = null): Result<Message> {
        if (content.isBlank()) return Result.failure(Exception("Message cannot be empty"))
        return repository.sendMessage(chatId, content.trim(), replyToId)
    }
}
