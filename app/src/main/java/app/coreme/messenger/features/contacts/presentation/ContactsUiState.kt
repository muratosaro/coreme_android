package app.coreme.messenger.features.contacts.presentation

import app.coreme.messenger.features.contacts.domain.model.Contact
import app.coreme.messenger.features.users.domain.model.UserProfile

data class ContactsUiState(
    val contacts: List<Contact> = emptyList(),
    val searchQuery: String = "",
    val searchResults: List<UserProfile> = emptyList(),
    val isLoading: Boolean = false,
    val isSearching: Boolean = false,
    val isAddingContact: String? = null,
    val removingContact: String? = null,
    val error: String? = null,
)
