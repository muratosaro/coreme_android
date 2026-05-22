package app.coreme.messenger.features.chats.presentation.creategroup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.coreme.messenger.features.chats.domain.usecase.CreateGroupChatUseCase
import app.coreme.messenger.features.contacts.domain.usecase.GetContactsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CreateGroupViewModel @Inject constructor(
    private val getContacts: GetContactsUseCase,
    private val createGroupChat: CreateGroupChatUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateGroupUiState())
    val uiState: StateFlow<CreateGroupUiState> = _uiState.asStateFlow()

    init {
        loadContacts()
    }

    private fun loadContacts() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            getContacts().fold(
                onSuccess = { contacts -> _uiState.update { it.copy(isLoading = false, contacts = contacts) } },
                onFailure = { e -> _uiState.update { it.copy(isLoading = false, error = e.message) } },
            )
        }
    }

    fun onGroupNameChange(name: String) = _uiState.update { it.copy(groupName = name) }

    fun toggleSelection(contactId: String) {
        _uiState.update { s ->
            val ids = if (contactId in s.selectedIds) s.selectedIds - contactId else s.selectedIds + contactId
            s.copy(selectedIds = ids)
        }
    }

    fun createGroup() {
        val s = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            createGroupChat(s.groupName, s.selectedIds.toList()).fold(
                onSuccess = { chat -> _uiState.update { it.copy(isLoading = false, createdChatId = chat.id) } },
                onFailure = { e -> _uiState.update { it.copy(isLoading = false, error = e.message) } },
            )
        }
    }

    fun clearError() = _uiState.update { it.copy(error = null) }
}
