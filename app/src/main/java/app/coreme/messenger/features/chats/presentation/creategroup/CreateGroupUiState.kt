package app.coreme.messenger.features.chats.presentation.creategroup

import app.coreme.messenger.features.contacts.domain.model.Contact

data class CreateGroupUiState(
    val groupName: String = "",
    val contacts: List<Contact> = emptyList(),
    val selectedIds: Set<String> = emptySet(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val createdChatId: String? = null,
)
