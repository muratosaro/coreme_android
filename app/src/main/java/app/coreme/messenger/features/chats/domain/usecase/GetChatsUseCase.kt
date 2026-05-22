package app.coreme.messenger.features.chats.domain.usecase

import app.coreme.messenger.features.chats.domain.model.Chat
import app.coreme.messenger.features.chats.domain.repository.ChatsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetChatsUseCase @Inject constructor(private val repository: ChatsRepository) {
    operator fun invoke(): Flow<List<Chat>> = repository.observeChats()
    suspend fun refresh(): Result<Unit> = repository.refreshChats()
}
