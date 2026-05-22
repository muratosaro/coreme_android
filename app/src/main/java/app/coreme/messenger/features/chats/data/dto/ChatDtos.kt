package app.coreme.messenger.features.chats.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OtherUserDto(
    val id: String,
    val username: String,
    @SerialName("display_name") val displayName: String,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("is_online") val isOnline: Boolean = false,
)

@Serializable
data class LastMessageDto(
    val id: String,
    @SerialName("chat_id") val chatId: String,
    @SerialName("sender_id") val senderId: String,
    val type: String,
    val content: String,
    @SerialName("is_read") val isRead: Boolean = false,
    @SerialName("created_at") val createdAt: String,
    @SerialName("is_edited") val isEdited: Boolean = false,
    @SerialName("is_deleted") val isDeleted: Boolean = false,
    @SerialName("sender_name") val senderName: String? = null,
)

@Serializable
data class ChatDto(
    val id: String,
    val type: String,
    val name: String? = null,
    val description: String? = null,
    @SerialName("group_avatar_url") val groupAvatarUrl: String? = null,
    @SerialName("created_at") val createdAt: String,
    @SerialName("last_message") val lastMessage: LastMessageDto? = null,
    @SerialName("unread_count") val unreadCount: Int = 0,
    @SerialName("member_count") val memberCount: Int = 0,
    @SerialName("other_user") val otherUser: OtherUserDto? = null,
)

@Serializable
data class ReactionDto(
    val emoji: String,
    @SerialName("user_id") val userId: String,
)

@Serializable
data class MessageDto(
    val id: String,
    @SerialName("chat_id") val chatId: String,
    @SerialName("sender_id") val senderId: String,
    val type: String,
    val content: String,
    @SerialName("is_read") val isRead: Boolean = false,
    @SerialName("created_at") val createdAt: String,
    @SerialName("is_edited") val isEdited: Boolean = false,
    @SerialName("is_deleted") val isDeleted: Boolean = false,
    @SerialName("reply_to_id") val replyToId: String? = null,
    @SerialName("reply_to_content") val replyToContent: String? = null,
    @SerialName("reply_to_sender_name") val replyToSenderName: String? = null,
    val duration: Int? = null,
    @SerialName("file_name") val fileName: String? = null,
    @SerialName("file_size") val fileSize: Long? = null,
    val caption: String? = null,
    val reactions: List<ReactionDto> = emptyList(),
    @SerialName("sender_name") val senderName: String? = null,
)

@Serializable
data class MemberDto(
    @SerialName("user_id") val userId: String,
    val username: String,
    @SerialName("display_name") val displayName: String,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("is_online") val isOnline: Boolean = false,
    val role: String,
    @SerialName("joined_at") val joinedAt: String,
)

@Serializable
data class SendMessageRequest(
    val type: String = "text",
    val content: String,
    @SerialName("reply_to_id") val replyToId: String? = null,
)

@Serializable
data class EditMessageRequest(val content: String)

@Serializable
data class CreateDirectChatRequest(
    val type: String = "direct",
    @SerialName("member_ids") val memberIds: List<String>,
)

@Serializable
data class CreateGroupChatRequest(
    val type: String = "group",
    val name: String,
    @SerialName("member_ids") val memberIds: List<String>,
)

@Serializable
data class AddReactionRequest(val emoji: String)
