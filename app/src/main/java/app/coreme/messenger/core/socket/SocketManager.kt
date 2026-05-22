package app.coreme.messenger.core.socket

import app.coreme.messenger.BuildConfig
import app.coreme.messenger.core.storage.SecureTokenStorage
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

sealed interface SocketEvent {
    data class NewMessage(val json: JSONObject) : SocketEvent
    data class ReactionUpdated(val json: JSONObject) : SocketEvent
    data class UserTyping(val chatId: String, val userId: String, val isTyping: Boolean) : SocketEvent
    data class MessageRead(val chatId: String, val userId: String) : SocketEvent
    data class UserOnline(val userId: String, val isOnline: Boolean) : SocketEvent
    data class IncomingCall(
        val callId: String,
        val chatId: String,
        val callType: String,
        val callerName: String,
        val callerAvatarUrl: String?,
    ) : SocketEvent
    data class CallAccepted(val callId: String) : SocketEvent
    data class CallRejected(val callId: String) : SocketEvent
    data class CallEnded(val callId: String) : SocketEvent
    data object Connected : SocketEvent
    data object Disconnected : SocketEvent
}

@Singleton
class SocketManager @Inject constructor(
    private val tokenStorage: SecureTokenStorage,
) {
    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS) // no timeout for WebSocket
        .build()

    private var ws: WebSocket? = null

    private val _events = MutableSharedFlow<SocketEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<SocketEvent> = _events.asSharedFlow()

    fun connect() {
        if (ws != null) return
        val token = tokenStorage.getAccessToken() ?: return
        val request = Request.Builder()
            .url("${BuildConfig.REALTIME_URL}/ws?token=$token")
            .build()
        ws = client.newWebSocket(request, listener)
    }

    fun disconnect() {
        ws?.close(1000, "disconnect")
        ws = null
    }

    fun joinChat(chatId: String) = send(msg("join_chat") { put("chatId", chatId) })
    fun sendTypingStart(chatId: String) = send(msg("typing_start") { put("chatId", chatId) })
    fun sendTypingStop(chatId: String) = send(msg("typing_stop") { put("chatId", chatId) })
    fun markRead(chatId: String) = send(msg("mark_read") { put("chatId", chatId) })

    fun initiateCall(targetUserId: String, callType: String) = send(
        msg("initiate_call") { put("targetUserId", targetUserId); put("callType", callType) }
    )
    fun acceptCall(callId: String) = send(msg("accept_call") { put("callId", callId) })
    fun rejectCall(callId: String) = send(msg("reject_call") { put("callId", callId) })
    fun endCall(callId: String) = send(msg("end_call") { put("callId", callId) })

    val isConnected: Boolean get() = ws != null

    private fun send(json: JSONObject) { ws?.send(json.toString()) }

    private fun msg(type: String, block: JSONObject.() -> Unit = {}): JSONObject =
        JSONObject().apply { put("type", type); block() }

    private val listener = object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            _events.tryEmit(SocketEvent.Connected)
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            val obj = runCatching { JSONObject(text) }.getOrNull() ?: return
            when (obj.optString("type")) {
                "new_message" -> _events.tryEmit(SocketEvent.NewMessage(obj))
                "reaction_updated" -> _events.tryEmit(SocketEvent.ReactionUpdated(obj))
                "user_typing" -> _events.tryEmit(
                    SocketEvent.UserTyping(
                        chatId = obj.optString("chatId"),
                        userId = obj.optString("userId"),
                        isTyping = obj.optBoolean("isTyping"),
                    )
                )
                "message_read" -> _events.tryEmit(
                    SocketEvent.MessageRead(
                        chatId = obj.optString("chatId"),
                        userId = obj.optString("userId"),
                    )
                )
                "user_online" -> _events.tryEmit(SocketEvent.UserOnline(obj.optString("userId"), true))
                "user_offline" -> _events.tryEmit(SocketEvent.UserOnline(obj.optString("userId"), false))
                "incoming_call" -> _events.tryEmit(
                    SocketEvent.IncomingCall(
                        callId = obj.optString("callId"),
                        chatId = obj.optString("chatId"),
                        callType = obj.optString("callType", "voice"),
                        callerName = obj.optString("callerName"),
                        callerAvatarUrl = obj.optString("callerAvatarUrl").ifBlank { null },
                    )
                )
                "call_accepted" -> _events.tryEmit(SocketEvent.CallAccepted(obj.optString("callId")))
                "call_rejected" -> _events.tryEmit(SocketEvent.CallRejected(obj.optString("callId")))
                "call_ended" -> _events.tryEmit(SocketEvent.CallEnded(obj.optString("callId")))
            }
        }

        override fun onMessage(webSocket: WebSocket, bytes: ByteString) = Unit

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            ws = null
            _events.tryEmit(SocketEvent.Disconnected)
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            ws = null
            _events.tryEmit(SocketEvent.Disconnected)
        }
    }
}
