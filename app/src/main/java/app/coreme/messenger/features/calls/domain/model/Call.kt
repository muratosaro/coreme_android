package app.coreme.messenger.features.calls.domain.model

import java.time.Instant

enum class CallType { VOICE, VIDEO }
enum class CallStatus { OUTGOING, INCOMING, MISSED, ANSWERED, DECLINED }

data class Call(
    val id: String,
    val chatId: String,
    val callType: CallType,
    val status: CallStatus,
    val duration: Int,
    val participantId: String,
    val participantName: String,
    val participantAvatarUrl: String?,
    val startedAt: Instant,
)
