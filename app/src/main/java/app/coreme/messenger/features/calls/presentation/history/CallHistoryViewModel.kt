package app.coreme.messenger.features.calls.presentation.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.coreme.messenger.core.notifications.NotificationHelper
import app.coreme.messenger.core.session.UserSession
import app.coreme.messenger.core.socket.SocketEvent
import app.coreme.messenger.core.socket.SocketManager
import app.coreme.messenger.features.calls.domain.usecase.GetCallHistoryUseCase
import app.coreme.messenger.features.calls.domain.usecase.InitiateCallUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CallHistoryViewModel @Inject constructor(
    private val getCallHistory: GetCallHistoryUseCase,
    private val initiateCall: InitiateCallUseCase,
    private val socketManager: SocketManager,
    private val notificationHelper: NotificationHelper,
    private val userSession: UserSession,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CallHistoryUiState())
    val uiState: StateFlow<CallHistoryUiState> = _uiState.asStateFlow()

    private val _incomingCall = MutableStateFlow<SocketEvent.IncomingCall?>(null)
    val incomingCall: StateFlow<SocketEvent.IncomingCall?> = _incomingCall.asStateFlow()

    init {
        loadHistory()
        observeSocket()
    }

    private fun loadHistory() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            getCallHistory().fold(
                onSuccess = { calls ->
                    _uiState.update { it.copy(isLoading = false, calls = calls) }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                },
            )
        }
    }

    private fun observeSocket() {
        viewModelScope.launch {
            socketManager.events.collect { event ->
                when (event) {
                    is SocketEvent.IncomingCall -> {
                        _incomingCall.value = event
                        notificationHelper.showIncomingCallNotification(event.callerName, event.callId)
                    }
                    is SocketEvent.CallEnded -> {
                        if (_incomingCall.value?.callId == event.callId) {
                            _incomingCall.value = null
                            notificationHelper.dismissCallNotification()
                        }
                    }
                    else -> Unit
                }
            }
        }
    }

    fun call(targetUserId: String, callType: String = "voice") {
        viewModelScope.launch {
            socketManager.initiateCall(targetUserId, callType)
        }
    }

    fun acceptCall(callId: String) {
        socketManager.acceptCall(callId)
        _incomingCall.value = null
        notificationHelper.dismissCallNotification()
    }

    fun rejectCall(callId: String) {
        socketManager.rejectCall(callId)
        _incomingCall.value = null
        notificationHelper.dismissCallNotification()
    }

    fun retry() = loadHistory()

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
