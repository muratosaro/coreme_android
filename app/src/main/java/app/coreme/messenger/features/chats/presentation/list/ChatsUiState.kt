package app.coreme.messenger.features.chats.presentation.list

import app.coreme.messenger.features.chats.domain.model.Chat

data class ChatsUiState(
    val chats: List<Chat> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
)
