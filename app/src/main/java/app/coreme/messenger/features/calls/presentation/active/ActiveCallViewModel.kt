package app.coreme.messenger.features.calls.presentation.active

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.coreme.messenger.core.socket.SocketEvent
import app.coreme.messenger.core.socket.SocketManager
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
class ActiveCallViewModel @Inject constructor(
    private val socketManager: SocketManager,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val callId: String = checkNotNull(savedStateHandle["callId"])
    private val participantName: String = savedStateHandle["participantName"] ?: ""
    private val participantAvatar: String? = savedStateHandle["participantAvatarUrl"]
    private val callType: String = savedStateHandle["callType"] ?: "voice"

    private val _uiState = MutableStateFlow(
        ActiveCallUiState(
            callId = callId,
            participantName = participantName,
            participantAvatarUrl = participantAvatar,
            callType = callType,
        ),
    )
    val uiState: StateFlow<ActiveCallUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null

    init {
        observeSocket()
    }

    private fun observeSocket() {
        viewModelScope.launch {
            socketManager.events.collect { event ->
                when (event) {
                    is SocketEvent.CallAccepted -> if (event.callId == callId) onCallConnected()
                    is SocketEvent.CallRejected -> if (event.callId == callId) {
                        _uiState.update { it.copy(phase = CallPhase.ENDED) }
                    }
                    is SocketEvent.CallEnded -> if (event.callId == callId) {
                        stopTimer()
                        _uiState.update { it.copy(phase = CallPhase.ENDED) }
                    }
                    else -> Unit
                }
            }
        }
    }

    fun onCallConnected() {
        _uiState.update { it.copy(phase = CallPhase.CONNECTED) }
        startTimer()
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1_000)
                _uiState.update { it.copy(durationSeconds = it.durationSeconds + 1) }
            }
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    fun toggleMute() {
        _uiState.update { it.copy(isMuted = !it.isMuted) }
    }

    fun toggleSpeaker() {
        _uiState.update { it.copy(isSpeakerOn = !it.isSpeakerOn) }
    }

    fun toggleVideo() {
        _uiState.update { it.copy(isVideoEnabled = !it.isVideoEnabled) }
    }

    fun endCall() {
        stopTimer()
        socketManager.endCall(callId)
        _uiState.update { it.copy(phase = CallPhase.ENDED) }
    }

    override fun onCleared() {
        stopTimer()
        super.onCleared()
    }
}
