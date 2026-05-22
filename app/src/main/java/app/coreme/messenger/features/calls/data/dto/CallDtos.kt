package app.coreme.messenger.features.calls.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CallDto(
    val id: String = "",
    @SerialName("chat_id") val chatId: String = "",
    @SerialName("call_type") val callType: String = "voice",
    val status: String = "answered",
    val duration: Int = 0,
    @SerialName("participant_id") val participantId: String = "",
    @SerialName("participant_name") val participantName: String = "",
    @SerialName("participant_avatar_url") val participantAvatarUrl: String? = null,
    @SerialName("started_at") val startedAt: String = "",
)

@Serializable
data class InitiateCallRequest(
    @SerialName("target_user_id") val targetUserId: String,
    @SerialName("call_type") val callType: String,
)

@Serializable
data class InitiateCallResponse(
    @SerialName("call_id") val callId: String,
    @SerialName("chat_id") val chatId: String,
)
