package app.coreme.messenger.features.chats.domain.usecase

import app.coreme.messenger.features.chats.domain.model.Chat
import app.coreme.messenger.features.chats.domain.repository.ChatsRepository
import javax.inject.Inject

class CreateGroupChatUseCase @Inject constructor(private val repository: ChatsRepository) {
    suspend operator fun invoke(name: String, memberIds: List<String>): Result<Chat> {
        if (name.isBlank()) return Result.failure(Exception("Group name is required"))
        if (memberIds.isEmpty()) return Result.failure(Exception("Select at least one member"))
        return repository.createGroupChat(name, memberIds)
    }
}
