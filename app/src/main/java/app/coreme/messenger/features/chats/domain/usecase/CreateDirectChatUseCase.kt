package app.coreme.messenger.features.chats.domain.usecase

import app.coreme.messenger.features.chats.domain.model.Chat
import app.coreme.messenger.features.chats.domain.repository.ChatsRepository
import javax.inject.Inject

class CreateDirectChatUseCase @Inject constructor(private val repository: ChatsRepository) {
    suspend operator fun invoke(userId: String): Result<Chat> =
        repository.createDirectChat(userId)
}
