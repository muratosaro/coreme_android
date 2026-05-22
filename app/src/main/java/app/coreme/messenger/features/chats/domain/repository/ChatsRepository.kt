package app.coreme.messenger.features.chats.domain.repository

import app.coreme.messenger.features.chats.domain.model.Chat
import app.coreme.messenger.features.chats.domain.model.Message
import kotlinx.coroutines.flow.Flow

interface ChatsRepository {
    fun observeChats(): Flow<List<Chat>>
    suspend fun refreshChats(): Result<Unit>
    suspend fun getChatById(id: String): Result<Chat>
    suspend fun createDirectChat(userId: String): Result<Chat>
    suspend fun getMessages(chatId: String, limit: Int = 50, before: String? = null): Result<List<Message>>
    suspend fun sendMessage(chatId: String, content: String, replyToId: String? = null): Result<Message>
    suspend fun editMessage(chatId: String, msgId: String, content: String): Result<Message>
    suspend fun deleteMessage(chatId: String, msgId: String): Result<Unit>
    suspend fun createGroupChat(name: String, memberIds: List<String>): Result<Chat>
    suspend fun addReaction(chatId: String, msgId: String, emoji: String): Result<Unit>
    suspend fun removeReaction(chatId: String, msgId: String, emoji: String): Result<Unit>
}
