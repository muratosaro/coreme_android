package app.coreme.messenger.features.chats.presentation.newchat

import app.coreme.messenger.features.users.domain.model.UserProfile

data class NewChatUiState(
    val query: String = "",
    val results: List<UserProfile> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val createdChatId: String? = null,
)
