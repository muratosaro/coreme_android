package app.coreme.messenger.features.chats.data.repository

import app.coreme.messenger.core.network.safeApiCall
import app.coreme.messenger.features.chats.data.api.ChatsApi
import app.coreme.messenger.features.chats.data.dto.AddReactionRequest
import app.coreme.messenger.features.chats.data.dto.CreateDirectChatRequest
import app.coreme.messenger.features.chats.data.dto.CreateGroupChatRequest
import app.coreme.messenger.features.chats.data.dto.EditMessageRequest
import app.coreme.messenger.features.chats.data.dto.SendMessageRequest
import app.coreme.messenger.features.chats.data.mapper.ChatMapper
import app.coreme.messenger.features.chats.domain.model.Chat
import app.coreme.messenger.features.chats.domain.model.Message
import app.coreme.messenger.features.chats.domain.repository.ChatsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatsRepositoryImpl @Inject constructor(
    private val api: ChatsApi,
    private val mapper: ChatMapper,
) : ChatsRepository {

    private val _chats = MutableStateFlow<List<Chat>>(emptyList())

    override fun observeChats(): Flow<List<Chat>> = _chats.asStateFlow()

    override suspend fun refreshChats(): Result<Unit> = safeApiCall {
        val chats = api.getChats().map { mapper.toDomain(it) }
        _chats.value = chats
    }

    override suspend fun getChatById(id: String): Result<Chat> =
        safeApiCall { mapper.toDomain(api.getChatById(id)) }

    override suspend fun createDirectChat(userId: String): Result<Chat> =
        safeApiCall { mapper.toDomain(api.createDirectChat(CreateDirectChatRequest(memberIds = listOf(userId)))) }

    override suspend fun getMessages(chatId: String, limit: Int, before: String?): Result<List<Message>> =
        safeApiCall { api.getMessages(chatId, limit, before).map { mapper.toDomain(it) } }

    override suspend fun sendMessage(chatId: String, content: String, replyToId: String?): Result<Message> =
        safeApiCall { mapper.toDomain(api.sendMessage(chatId, SendMessageRequest(content = content, replyToId = replyToId))) }

    override suspend fun editMessage(chatId: String, msgId: String, content: String): Result<Message> =
        safeApiCall { mapper.toDomain(api.editMessage(chatId, msgId, EditMessageRequest(content))) }

    override suspend fun deleteMessage(chatId: String, msgId: String): Result<Unit> =
        safeApiCall { api.deleteMessage(chatId, msgId, forAll = true) }

    override suspend fun createGroupChat(name: String, memberIds: List<String>): Result<Chat> =
        safeApiCall { mapper.toDomain(api.createGroupChat(CreateGroupChatRequest(name = name, memberIds = memberIds))) }

    override suspend fun addReaction(chatId: String, msgId: String, emoji: String): Result<Unit> =
        safeApiCall { api.addReaction(chatId, msgId, AddReactionRequest(emoji)) }

    override suspend fun removeReaction(chatId: String, msgId: String, emoji: String): Result<Unit> =
        safeApiCall { api.removeReaction(chatId, msgId, emoji) }

    fun updateChatFromSocket(chat: Chat) {
        _chats.value = _chats.value.map { if (it.id == chat.id) chat else it }
    }

    fun prependChat(chat: Chat) {
        val current = _chats.value.toMutableList()
        current.removeAll { it.id == chat.id }
        _chats.value = listOf(chat) + current
    }
}
