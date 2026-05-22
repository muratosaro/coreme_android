package app.coreme.messenger.features.calls.presentation.active

enum class CallPhase { CONNECTING, CONNECTED, ENDED }

data class ActiveCallUiState(
    val callId: String = "",
    val participantName: String = "",
    val participantAvatarUrl: String? = null,
    val callType: String = "voice",
    val phase: CallPhase = CallPhase.CONNECTING,
    val isMuted: Boolean = false,
    val isSpeakerOn: Boolean = false,
    val isVideoEnabled: Boolean = false,
    val durationSeconds: Int = 0,
)
