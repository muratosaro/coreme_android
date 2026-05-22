package app.coreme.messenger.features.chats.domain.usecase

import app.coreme.messenger.features.chats.domain.model.Message
import app.coreme.messenger.features.chats.domain.repository.ChatsRepository
import javax.inject.Inject

class GetMessagesUseCase @Inject constructor(private val repository: ChatsRepository) {
    suspend operator fun invoke(chatId: String, limit: Int = 50, before: String? = null): Result<List<Message>> =
        repository.getMessages(chatId, limit, before)
}
