package app.coreme.messenger.features.calls.data.mapper

import app.coreme.messenger.features.calls.data.dto.CallDto
import app.coreme.messenger.features.calls.domain.model.Call
import app.coreme.messenger.features.calls.domain.model.CallStatus
import app.coreme.messenger.features.calls.domain.model.CallType
import java.time.Instant

fun CallDto.toDomain(): Call = Call(
    id = id,
    chatId = chatId,
    callType = when (callType.lowercase()) {
        "video" -> CallType.VIDEO
        else -> CallType.VOICE
    },
    status = when (status.lowercase()) {
        "outgoing" -> CallStatus.OUTGOING
        "incoming" -> CallStatus.INCOMING
        "missed" -> CallStatus.MISSED
        "declined" -> CallStatus.DECLINED
        else -> CallStatus.ANSWERED
    },
    duration = duration,
    participantId = participantId,
    participantName = participantName,
    participantAvatarUrl = participantAvatarUrl,
    startedAt = try { Instant.parse(startedAt) } catch (_: Exception) { Instant.now() },
)
