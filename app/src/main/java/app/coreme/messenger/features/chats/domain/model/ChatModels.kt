package app.coreme.messenger.features.chats.domain.model

import java.time.Instant

enum class ChatType { DIRECT, GROUP }

enum class MessageType { TEXT, IMAGE, VIDEO, AUDIO, FILE, VOICE, STICKER }

enum class MemberRole { OWNER, ADMIN, MEMBER }

data class OtherUser(
    val id: String,
    val username: String,
    val displayName: String,
    val avatarUrl: String? = null,
    val isOnline: Boolean = false,
)

data class LastMessage(
    val id: String,
    val chatId: String,
    val senderId: String,
    val type: MessageType,
    val content: String,
    val isRead: Boolean,
    val createdAt: Instant,
    val isEdited: Boolean,
    val isDeleted: Boolean,
    val senderName: String? = null,
)

data class Chat(
    val id: String,
    val type: ChatType,
    val name: String? = null,
    val description: String? = null,
    val groupAvatarUrl: String? = null,
    val createdAt: Instant,
    val lastMessage: LastMessage? = null,
    val unreadCount: Int = 0,
    val memberCount: Int = 0,
    val otherUser: OtherUser? = null,
) {
    val displayName: String
        get() = when (type) {
            ChatType.DIRECT -> otherUser?.displayName ?: "Unknown"
            ChatType.GROUP -> name ?: "Group"
        }

    val avatarUrl: String?
        get() = when (type) {
            ChatType.DIRECT -> otherUser?.avatarUrl
            ChatType.GROUP -> groupAvatarUrl
        }

    val isOnline: Boolean
        get() = type == ChatType.DIRECT && (otherUser?.isOnline == true)
}

data class Reaction(
    val emoji: String,
    val userId: String,
)

data class Message(
    val id: String,
    val chatId: String,
    val senderId: String,
    val type: MessageType,
    val content: String,
    val isRead: Boolean,
    val createdAt: Instant,
    val isEdited: Boolean,
    val isDeleted: Boolean,
    val replyToId: String? = null,
    val replyToContent: String? = null,
    val replyToSenderName: String? = null,
    val duration: Int? = null,
    val fileName: String? = null,
    val fileSize: Long? = null,
    val caption: String? = null,
    val reactions: List<Reaction> = emptyList(),
    val senderName: String? = null,
)

data class Member(
    val userId: String,
    val username: String,
    val displayName: String,
    val avatarUrl: String? = null,
    val isOnline: Boolean = false,
    val role: MemberRole,
    val joinedAt: Instant,
)
