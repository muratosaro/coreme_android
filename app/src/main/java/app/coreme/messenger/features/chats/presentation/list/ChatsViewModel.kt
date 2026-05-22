package app.coreme.messenger.features.chats.presentation.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.coreme.messenger.core.socket.SocketEvent
import app.coreme.messenger.core.socket.SocketManager
import app.coreme.messenger.features.chats.domain.usecase.GetChatsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatsViewModel @Inject constructor(
    private val getChatsUseCase: GetChatsUseCase,
    private val socketManager: SocketManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatsUiState(isLoading = true))
    val uiState: StateFlow<ChatsUiState> = _uiState

    init {
        observeChats()
        observeSocket()
        refresh()
        socketManager.connect()
    }

    private fun observeChats() {
        getChatsUseCase()
            .onEach { chats ->
                _uiState.update { it.copy(chats = chats, isLoading = false) }
            }
            .launchIn(viewModelScope)
    }

    private fun observeSocket() {
        socketManager.events
            .onEach { event ->
                when (event) {
                    is SocketEvent.NewMessage -> refresh()
                    is SocketEvent.MessageRead -> refresh()
                    else -> Unit
                }
            }
            .launchIn(viewModelScope)
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true, error = null) }
            getChatsUseCase.refresh()
                .onFailure { e ->
                    _uiState.update { it.copy(error = e.message, isLoading = false) }
                }
            _uiState.update { it.copy(isRefreshing = false) }
        }
    }

    fun clearError() = _uiState.update { it.copy(error = null) }
}
