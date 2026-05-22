package app.coreme.messenger.features.contacts.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.coreme.messenger.features.contacts.domain.usecase.AddContactUseCase
import app.coreme.messenger.features.contacts.domain.usecase.GetContactsUseCase
import app.coreme.messenger.features.contacts.domain.usecase.RemoveContactUseCase
import app.coreme.messenger.features.users.domain.usecase.SearchUsersUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ContactsViewModel @Inject constructor(
    private val getContacts: GetContactsUseCase,
    private val addContact: AddContactUseCase,
    private val removeContact: RemoveContactUseCase,
    private val searchUsers: SearchUsersUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ContactsUiState())
    val uiState: StateFlow<ContactsUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    init {
        loadContacts()
    }

    private fun loadContacts() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            getContacts().fold(
                onSuccess = { list ->
                    _uiState.update { it.copy(isLoading = false, contacts = list) }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                },
            )
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        searchJob?.cancel()
        if (query.length < 2) {
            _uiState.update { it.copy(searchResults = emptyList(), isSearching = false) }
            return
        }
        searchJob = viewModelScope.launch {
            delay(300)
            _uiState.update { it.copy(isSearching = true) }
            searchUsers(query).fold(
                onSuccess = { results ->
                    _uiState.update { it.copy(isSearching = false, searchResults = results) }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isSearching = false, error = e.message) }
                },
            )
        }
    }

    fun addContact(userId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isAddingContact = userId) }
            addContact.invoke(userId).fold(
                onSuccess = {
                    _uiState.update { it.copy(isAddingContact = null) }
                    loadContacts()
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isAddingContact = null, error = e.message) }
                },
            )
        }
    }

    fun removeContact(contactId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(removingContact = contactId) }
            removeContact.invoke(contactId).fold(
                onSuccess = {
                    _uiState.update { state ->
                        state.copy(
                            removingContact = null,
                            contacts = state.contacts.filter { it.id != contactId },
                        )
                    }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(removingContact = null, error = e.message) }
                },
            )
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
