package app.coreme.messenger.features.chats.presentation.detail

import app.coreme.messenger.features.chats.domain.model.Chat
import app.coreme.messenger.features.chats.domain.model.Message

data class ChatDetailUiState(
    val chat: Chat? = null,
    val messages: List<Message> = emptyList(),
    val isLoading: Boolean = true,
    val isSending: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasMoreMessages: Boolean = true,
    val currentUserId: String = "",
    val inputText: String = "",
    val replyTo: Message? = null,
    val editingMessage: Message? = null,
    val typingUserNames: List<String> = emptyList(),
    val error: String? = null,
)
