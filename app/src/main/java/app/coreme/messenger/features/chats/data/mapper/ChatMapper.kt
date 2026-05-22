package app.coreme.messenger.features.chats.data.mapper

import app.coreme.messenger.features.chats.data.dto.ChatDto
import app.coreme.messenger.features.chats.data.dto.LastMessageDto
import app.coreme.messenger.features.chats.data.dto.MemberDto
import app.coreme.messenger.features.chats.data.dto.MessageDto
import app.coreme.messenger.features.chats.data.dto.OtherUserDto
import app.coreme.messenger.features.chats.data.dto.ReactionDto
import app.coreme.messenger.features.chats.domain.model.Chat
import app.coreme.messenger.features.chats.domain.model.ChatType
import app.coreme.messenger.features.chats.domain.model.LastMessage
import app.coreme.messenger.features.chats.domain.model.Member
import app.coreme.messenger.features.chats.domain.model.MemberRole
import app.coreme.messenger.features.chats.domain.model.Message
import app.coreme.messenger.features.chats.domain.model.MessageType
import app.coreme.messenger.features.chats.domain.model.OtherUser
import app.coreme.messenger.features.chats.domain.model.Reaction
import java.time.Instant
import javax.inject.Inject

class ChatMapper @Inject constructor() {

    fun toDomain(dto: ChatDto) = Chat(
        id = dto.id,
        type = dto.type.toChatType(),
        name = dto.name,
        description = dto.description,
        groupAvatarUrl = dto.groupAvatarUrl,
        createdAt = dto.createdAt.toInstant(),
        lastMessage = dto.lastMessage?.toDomain(),
        unreadCount = dto.unreadCount,
        memberCount = dto.memberCount,
        otherUser = dto.otherUser?.toDomain(),
    )

    fun toDomain(dto: MessageDto) = Message(
        id = dto.id,
        chatId = dto.chatId,
        senderId = dto.senderId,
        type = dto.type.toMessageType(),
        content = dto.content,
        isRead = dto.isRead,
        createdAt = dto.createdAt.toInstant(),
        isEdited = dto.isEdited,
        isDeleted = dto.isDeleted,
        replyToId = dto.replyToId,
        replyToContent = dto.replyToContent,
        replyToSenderName = dto.replyToSenderName,
        duration = dto.duration,
        fileName = dto.fileName,
        fileSize = dto.fileSize,
        caption = dto.caption,
        reactions = dto.reactions.map { it.toDomain() },
        senderName = dto.senderName,
    )

    fun toDomain(dto: MemberDto) = Member(
        userId = dto.userId,
        username = dto.username,
        displayName = dto.displayName,
        avatarUrl = dto.avatarUrl,
        isOnline = dto.isOnline,
        role = dto.role.toMemberRole(),
        joinedAt = dto.joinedAt.toInstant(),
    )

    private fun LastMessageDto.toDomain() = LastMessage(
        id = id,
        chatId = chatId,
        senderId = senderId,
        type = type.toMessageType(),
        content = content,
        isRead = isRead,
        createdAt = createdAt.toInstant(),
        isEdited = isEdited,
        isDeleted = isDeleted,
        senderName = senderName,
    )

    private fun OtherUserDto.toDomain() = OtherUser(
        id = id,
        username = username,
        displayName = displayName,
        avatarUrl = avatarUrl,
        isOnline = isOnline,
    )

    private fun ReactionDto.toDomain() = Reaction(emoji = emoji, userId = userId)

    private fun String.toChatType() = when (this) {
        "group" -> ChatType.GROUP
        else -> ChatType.DIRECT
    }

    private fun String.toMessageType() = when (this) {
        "image" -> MessageType.IMAGE
        "video" -> MessageType.VIDEO
        "audio" -> MessageType.AUDIO
        "file" -> MessageType.FILE
        "voice" -> MessageType.VOICE
        "sticker" -> MessageType.STICKER
        else -> MessageType.TEXT
    }

    private fun String.toMemberRole() = when (this) {
        "owner" -> MemberRole.OWNER
        "admin" -> MemberRole.ADMIN
        else -> MemberRole.MEMBER
    }

    private fun String.toInstant(): Instant = try {
        Instant.parse(this)
    } catch (_: Exception) {
        Instant.now()
    }
}
