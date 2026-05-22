package app.coreme.messenger.core.socket

import app.coreme.messenger.BuildConfig
import app.coreme.messenger.core.storage.SecureTokenStorage
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import org.json.JSONObject
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

    private var socket: Socket? = null

    private val _events = MutableSharedFlow<SocketEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<SocketEvent> = _events.asSharedFlow()

    fun connect() {
        if (socket?.connected() == true) return
        val token = tokenStorage.getAccessToken() ?: return

        val opts = IO.Options.builder()
            .setAuth(mapOf("token" to token))
            .setReconnection(true)
            .setReconnectionAttempts(Int.MAX_VALUE)
            .setReconnectionDelay(1_000)
            .build()

        socket = IO.socket(BuildConfig.BASE_URL, opts).apply {
            on(Socket.EVENT_CONNECT) { _events.tryEmit(SocketEvent.Connected) }
            on(Socket.EVENT_DISCONNECT) { _events.tryEmit(SocketEvent.Disconnected) }

            on("new_message") { args ->
                (args.firstOrNull() as? JSONObject)?.let {
                    _events.tryEmit(SocketEvent.NewMessage(it))
                }
            }
            on("reaction_updated") { args ->
                (args.firstOrNull() as? JSONObject)?.let {
                    _events.tryEmit(SocketEvent.ReactionUpdated(it))
                }
            }
            on("user_typing") { args ->
                (args.firstOrNull() as? JSONObject)?.let { obj ->
                    _events.tryEmit(
                        SocketEvent.UserTyping(
                            chatId = obj.optString("chatId"),
                            userId = obj.optString("userId"),
                            isTyping = obj.optBoolean("isTyping"),
                        ),
                    )
                }
            }
            on("message_read") { args ->
                (args.firstOrNull() as? JSONObject)?.let { obj ->
                    _events.tryEmit(
                        SocketEvent.MessageRead(
                            chatId = obj.optString("chatId"),
                            userId = obj.optString("userId"),
                        ),
                    )
                }
            }
            on("user_online") { args ->
                (args.firstOrNull() as? JSONObject)?.let { obj ->
                    _events.tryEmit(SocketEvent.UserOnline(obj.optString("userId"), true))
                }
            }
            on("user_offline") { args ->
                (args.firstOrNull() as? JSONObject)?.let { obj ->
                    _events.tryEmit(SocketEvent.UserOnline(obj.optString("userId"), false))
                }
            }
            on("incoming_call") { args ->
                (args.firstOrNull() as? JSONObject)?.let { obj ->
                    _events.tryEmit(
                        SocketEvent.IncomingCall(
                            callId = obj.optString("callId"),
                            chatId = obj.optString("chatId"),
                            callType = obj.optString("callType", "voice"),
                            callerName = obj.optString("callerName"),
                            callerAvatarUrl = obj.optString("callerAvatarUrl").ifBlank { null },
                        ),
                    )
                }
            }
            on("call_accepted") { args ->
                (args.firstOrNull() as? JSONObject)?.let { obj ->
                    _events.tryEmit(SocketEvent.CallAccepted(obj.optString("callId")))
                }
            }
            on("call_rejected") { args ->
                (args.firstOrNull() as? JSONObject)?.let { obj ->
                    _events.tryEmit(SocketEvent.CallRejected(obj.optString("callId")))
                }
            }
            on("call_ended") { args ->
                (args.firstOrNull() as? JSONObject)?.let { obj ->
                    _events.tryEmit(SocketEvent.CallEnded(obj.optString("callId")))
                }
            }

            connect()
        }
    }

    fun disconnect() {
        socket?.disconnect()
        socket?.off()
        socket = null
    }

    fun joinChat(chatId: String) {
        socket?.emit("join_chat", JSONObject().put("chatId", chatId))
    }

    fun sendTypingStart(chatId: String) {
        socket?.emit("typing_start", JSONObject().put("chatId", chatId))
    }

    fun sendTypingStop(chatId: String) {
        socket?.emit("typing_stop", JSONObject().put("chatId", chatId))
    }

    fun markRead(chatId: String) {
        socket?.emit("mark_read", JSONObject().put("chatId", chatId))
    }

    fun initiateCall(targetUserId: String, callType: String) {
        socket?.emit("initiate_call", JSONObject()
            .put("targetUserId", targetUserId)
            .put("callType", callType))
    }

    fun acceptCall(callId: String) {
        socket?.emit("accept_call", JSONObject().put("callId", callId))
    }

    fun rejectCall(callId: String) {
        socket?.emit("reject_call", JSONObject().put("callId", callId))
    }

    fun endCall(callId: String) {
        socket?.emit("end_call", JSONObject().put("callId", callId))
    }

    val isConnected: Boolean get() = socket?.connected() == true
}
