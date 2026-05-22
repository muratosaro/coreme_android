package app.coreme.messenger.features.chats.presentation.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.coreme.messenger.core.session.UserSession
import app.coreme.messenger.core.socket.SocketEvent
import app.coreme.messenger.core.socket.SocketManager
import app.coreme.messenger.features.chats.domain.model.Message
import app.coreme.messenger.features.chats.domain.model.MessageType
import app.coreme.messenger.features.chats.domain.model.Reaction
import app.coreme.messenger.features.chats.domain.usecase.AddReactionUseCase
import app.coreme.messenger.features.chats.domain.usecase.DeleteMessageUseCase
import app.coreme.messenger.features.chats.domain.usecase.EditMessageUseCase
import app.coreme.messenger.features.chats.domain.usecase.GetChatsUseCase
import app.coreme.messenger.features.chats.domain.usecase.GetMessagesUseCase
import app.coreme.messenger.features.chats.domain.usecase.RemoveReactionUseCase
import app.coreme.messenger.features.chats.domain.usecase.SendMessageUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.time.Instant
import javax.inject.Inject

@HiltViewModel
class ChatDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getMessagesUseCase: GetMessagesUseCase,
    private val sendMessageUseCase: SendMessageUseCase,
    private val editMessageUseCase: EditMessageUseCase,
    private val deleteMessageUseCase: DeleteMessageUseCase,
    private val addReactionUseCase: AddReactionUseCase,
    private val removeReactionUseCase: RemoveReactionUseCase,
    private val getChatsUseCase: GetChatsUseCase,
    private val socketManager: SocketManager,
    private val userSession: UserSession,
) : ViewModel() {

    val chatId: String = checkNotNull(savedStateHandle["chatId"])

    private val _uiState = MutableStateFlow(ChatDetailUiState(currentUserId = userSession.userId))
    val uiState: StateFlow<ChatDetailUiState> = _uiState

    private var typingJob: Job? = null
    private var isTyping = false

    init {
        loadInitialData()
        observeSocket()
        socketManager.connect()
        socketManager.joinChat(chatId)
        socketManager.markRead(chatId)
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, currentUserId = userSession.userId) }

            // Load chat info from cached chats flow
            getChatsUseCase().onEach { chats ->
                chats.find { it.id == chatId }?.let { chat ->
                    _uiState.update { it.copy(chat = chat) }
                }
            }.launchIn(viewModelScope)

            // Load messages
            getMessagesUseCase(chatId).onSuccess { msgs ->
                _uiState.update { it.copy(messages = msgs.reversed(), isLoading = false) }
            }.onFailure { e ->
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }

    private fun observeSocket() {
        socketManager.events.onEach { event ->
            when (event) {
                is SocketEvent.NewMessage -> handleNewMessage(event.json)
                is SocketEvent.ReactionUpdated -> handleReactionUpdated(event.json)
                is SocketEvent.UserTyping -> handleTyping(event)
                is SocketEvent.MessageRead -> handleMessageRead(event.chatId)
                else -> Unit
            }
        }.launchIn(viewModelScope)
    }

    private fun handleReactionUpdated(json: JSONObject) {
        val msgChatId = json.optString("chat_id")
        if (msgChatId != chatId) return
        val msgId = json.optString("message_id").ifEmpty { return }
        val reactionsArray = json.optJSONArray("reactions") ?: return
        val reactions = (0 until reactionsArray.length()).mapNotNull { i ->
            runCatching {
                val obj = reactionsArray.getJSONObject(i)
                Reaction(emoji = obj.optString("emoji"), userId = obj.optString("user_id"))
            }.getOrNull()
        }
        _uiState.update { state ->
            state.copy(messages = state.messages.map { msg ->
                if (msg.id == msgId) msg.copy(reactions = reactions) else msg
            })
        }
    }

    private fun handleNewMessage(json: JSONObject) {
        val msgChatId = json.optString("chat_id")
        if (msgChatId != chatId) return

        val senderId = json.optString("sender_id")
        if (senderId == userSession.userId) return // already added optimistically

        val msg = parseMessageFromJson(json) ?: return
        _uiState.update { state ->
            state.copy(messages = state.messages + msg)
        }
        socketManager.markRead(chatId)
    }

    private fun handleTyping(event: SocketEvent.UserTyping) {
        if (event.chatId != chatId || event.userId == userSession.userId) return
        val chat = _uiState.value.chat ?: return
        val name = when {
            chat.otherUser?.id == event.userId -> chat.otherUser.displayName
            else -> event.userId.take(8)
        }
        _uiState.update { state ->
            val current = state.typingUserNames.toMutableList()
            if (event.isTyping) {
                if (!current.contains(name)) current.add(name)
            } else {
                current.remove(name)
            }
            state.copy(typingUserNames = current)
        }
    }

    private fun handleMessageRead(readChatId: String) {
        if (readChatId != chatId) return
        _uiState.update { state ->
            state.copy(messages = state.messages.map { it.copy(isRead = true) })
        }
    }

    fun onInputChange(text: String) {
        _uiState.update { it.copy(inputText = text) }
        handleTypingDebounce(text)
    }

    private fun handleTypingDebounce(text: String) {
        if (text.isNotBlank() && !isTyping) {
            isTyping = true
            socketManager.sendTypingStart(chatId)
        }
        typingJob?.cancel()
        typingJob = viewModelScope.launch {
            delay(1_500)
            if (isTyping) {
                isTyping = false
                socketManager.sendTypingStop(chatId)
            }
        }
    }

    fun send() {
        val state = _uiState.value
        val text = state.inputText.trim()
        if (text.isBlank()) return

        if (state.editingMessage != null) {
            submitEdit(state.editingMessage.id, text)
            return
        }

        val replyToId = state.replyTo?.id
        _uiState.update { it.copy(inputText = "", replyTo = null, isSending = true) }
        stopTyping()

        viewModelScope.launch {
            sendMessageUseCase(chatId, text, replyToId)
                .onSuccess { msg ->
                    _uiState.update { it.copy(messages = it.messages + msg, isSending = false) }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(error = e.message, isSending = false) }
                }
        }
    }

    fun loadMore() {
        val state = _uiState.value
        if (state.isLoadingMore || !state.hasMoreMessages || state.messages.isEmpty()) return
        val oldest = state.messages.firstOrNull()?.createdAt?.toString() ?: return

        _uiState.update { it.copy(isLoadingMore = true) }
        viewModelScope.launch {
            getMessagesUseCase(chatId, before = oldest).onSuccess { older ->
                if (older.isEmpty()) {
                    _uiState.update { it.copy(isLoadingMore = false, hasMoreMessages = false) }
                } else {
                    _uiState.update { state ->
                        state.copy(
                            messages = older.reversed() + state.messages,
                            isLoadingMore = false,
                        )
                    }
                }
            }.onFailure {
                _uiState.update { it.copy(isLoadingMore = false) }
            }
        }
    }

    fun setReplyTo(message: Message) {
        _uiState.update { it.copy(replyTo = message, editingMessage = null) }
    }

    fun clearReply() {
        _uiState.update { it.copy(replyTo = null) }
    }

    fun startEdit(message: Message) {
        if (message.type != MessageType.TEXT) return
        _uiState.update { it.copy(editingMessage = message, inputText = message.content, replyTo = null) }
    }

    fun cancelEdit() {
        _uiState.update { it.copy(editingMessage = null, inputText = "") }
    }

    private fun submitEdit(msgId: String, content: String) {
        _uiState.update { it.copy(editingMessage = null, inputText = "", isSending = true) }
        viewModelScope.launch {
            editMessageUseCase(chatId, msgId, content)
                .onSuccess { updated ->
                    _uiState.update { state ->
                        state.copy(
                            messages = state.messages.map { if (it.id == updated.id) updated else it },
                            isSending = false,
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(error = e.message, isSending = false) }
                }
        }
    }

    fun deleteMessage(msgId: String) {
        // Optimistic remove
        _uiState.update { state -> state.copy(messages = state.messages.filter { it.id != msgId }) }
        viewModelScope.launch {
            deleteMessageUseCase(chatId, msgId).onFailure { e ->
                _uiState.update { it.copy(error = e.message) }
                // Reload on failure
                getMessagesUseCase(chatId).onSuccess { msgs ->
                    _uiState.update { it.copy(messages = msgs.reversed()) }
                }
            }
        }
    }

    fun addReaction(msgId: String, emoji: String) {
        viewModelScope.launch {
            addReactionUseCase(chatId, msgId, emoji).onFailure { e ->
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun removeReaction(msgId: String, emoji: String) {
        viewModelScope.launch {
            removeReactionUseCase(chatId, msgId, emoji).onFailure { e ->
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun clearError() = _uiState.update { it.copy(error = null) }

    private fun stopTyping() {
        typingJob?.cancel()
        if (isTyping) {
            isTyping = false
            socketManager.sendTypingStop(chatId)
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopTyping()
    }

    private fun parseMessageFromJson(json: JSONObject): Message? = try {
        Message(
            id = json.optString("id"),
            chatId = json.optString("chat_id"),
            senderId = json.optString("sender_id"),
            type = MessageType.TEXT,
            content = json.optString("content"),
            isRead = json.optBoolean("is_read"),
            createdAt = runCatching { Instant.parse(json.optString("created_at")) }.getOrDefault(Instant.now()),
            isEdited = json.optBoolean("is_edited"),
            isDeleted = json.optBoolean("is_deleted"),
            replyToId = json.optString("reply_to_id").ifEmpty { null },
            replyToContent = json.optString("reply_to_content").ifEmpty { null },
            replyToSenderName = json.optString("reply_to_sender_name").ifEmpty { null },
            senderName = json.optString("sender_name").ifEmpty { null },
        )
    } catch (_: Exception) { null }
}
