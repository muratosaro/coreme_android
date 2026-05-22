package app.coreme.messenger.features.chats.presentation.detail.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.coreme.messenger.core.ui.theme.CoremeColors
import app.coreme.messenger.core.ui.theme.InterFamily
import app.coreme.messenger.features.chats.domain.model.Message
import app.coreme.messenger.features.chats.domain.model.MessageType
import app.coreme.messenger.features.chats.domain.model.Reaction
import coil3.compose.AsyncImage
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val COMMON_EMOJIS = listOf("👍", "❤️", "😂", "😮", "😢", "🔥")

private val SENT_SHAPE = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 5.dp)
private val RECEIVED_SHAPE = RoundedCornerShape(topStart = 5.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 18.dp)

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun MessageBubble(
    message: Message,
    isOwn: Boolean,
    currentUserId: String,
    showSenderName: Boolean,
    onReply: (Message) -> Unit,
    onEdit: (Message) -> Unit,
    onDelete: (String) -> Unit,
    onAddReaction: (String, String) -> Unit,
    onRemoveReaction: (String, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showMenu by remember { mutableStateOf(false) }
    var showEmojiPicker by remember { mutableStateOf(false) }

    val bubbleShape = if (isOwn) SENT_SHAPE else RECEIVED_SHAPE
    val boxAlignment = if (isOwn) Alignment.CenterEnd else Alignment.CenterStart
    val colAlignment = if (isOwn) Alignment.End else Alignment.Start

    val bubbleBg = if (isOwn) Color(0x22FFFFFF) else Color(0x12FFFFFF)
    val bubbleBorder = if (isOwn) Color(0x28FFFFFF) else Color(0x18FFFFFF)

    Box(
        contentAlignment = boxAlignment,
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = if (isOwn) 60.dp else 10.dp,
                end = if (isOwn) 10.dp else 60.dp,
                top = 2.dp,
                bottom = 2.dp,
            ),
    ) {
        Column(horizontalAlignment = colAlignment) {
            if (showSenderName && !isOwn && message.senderName != null) {
                Text(
                    text = message.senderName,
                    style = TextStyle(
                        fontFamily = InterFamily,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = CoremeColors.accent.copy(alpha = 0.85f),
                    ),
                    modifier = Modifier.padding(start = 10.dp, bottom = 3.dp),
                )
            }

            Box {
                Column(
                    modifier = Modifier
                        .widthIn(max = 288.dp)
                        .clip(bubbleShape)
                        .background(bubbleBg, bubbleShape)
                        .border(0.7.dp, bubbleBorder, bubbleShape)
                        .combinedClickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {},
                            onLongClick = { showMenu = true },
                        ),
                ) {
                    // Reply preview inside bubble
                    message.replyToContent?.let { replyContent ->
                        ReplyPreviewInBubble(
                            senderName = message.replyToSenderName,
                            content = replyContent,
                            modifier = Modifier.padding(start = 10.dp, end = 10.dp, top = 8.dp, bottom = 0.dp),
                        )
                    }

                    // Image content
                    if (!message.isDeleted && message.type == MessageType.IMAGE) {
                        AsyncImage(
                            model = message.content,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .widthIn(max = 288.dp)
                                .heightIn(max = 240.dp)
                                .clip(bubbleShape),
                        )
                        if (message.caption != null) {
                            Text(
                                text = message.caption,
                                style = TextStyle(
                                    fontFamily = InterFamily,
                                    fontSize = 14.sp,
                                    lineHeight = 20.sp,
                                    color = CoremeColors.textPrimary,
                                ),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                            )
                        }
                    } else {
                        Column(modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 9.dp)) {
                            BubbleContent(message = message)
                        }
                    }

                    // Time + status row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                    ) {
                        if (message.isEdited && !message.isDeleted) {
                            Text(
                                text = "ред.",
                                style = TextStyle(
                                    fontFamily = InterFamily,
                                    fontSize = 10.sp,
                                    color = CoremeColors.textTertiary,
                                ),
                                modifier = Modifier.padding(end = 4.dp),
                            )
                        }
                        Text(
                            text = timeFormatter.format(message.createdAt.atZone(ZoneId.systemDefault())),
                            style = TextStyle(
                                fontFamily = InterFamily,
                                fontSize = 10.sp,
                                color = CoremeColors.textTertiary,
                            ),
                        )
                        if (isOwn) {
                            Spacer(modifier = Modifier.width(3.dp))
                            Icon(
                                imageVector = if (message.isRead) Icons.Filled.DoneAll else Icons.Filled.Check,
                                contentDescription = null,
                                tint = if (message.isRead) CoremeColors.blue else CoremeColors.textTertiary.copy(alpha = 0.45f),
                                modifier = Modifier.size(13.dp),
                            )
                        }
                    }
                }

                // Context menu
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    modifier = Modifier
                        .background(Color(0xFF1E1F26))
                        .border(0.5.dp, Color(0x18FFFFFF), RoundedCornerShape(12.dp)),
                ) {
                    DropdownMenuItem(
                        text = { Text("Відповісти", fontFamily = InterFamily, fontSize = 14.sp) },
                        onClick = { showMenu = false; onReply(message) },
                        colors = MenuDefaults.itemColors(textColor = CoremeColors.textPrimary),
                    )
                    if (!message.isDeleted) {
                        DropdownMenuItem(
                            text = { Text("Реакція", fontFamily = InterFamily, fontSize = 14.sp) },
                            onClick = { showMenu = false; showEmojiPicker = true },
                            colors = MenuDefaults.itemColors(textColor = CoremeColors.textPrimary),
                        )
                    }
                    if (isOwn && message.type == MessageType.TEXT && !message.isDeleted) {
                        DropdownMenuItem(
                            text = { Text("Редагувати", fontFamily = InterFamily, fontSize = 14.sp) },
                            onClick = { showMenu = false; onEdit(message) },
                            colors = MenuDefaults.itemColors(textColor = CoremeColors.textPrimary),
                        )
                    }
                    if (isOwn && !message.isDeleted) {
                        DropdownMenuItem(
                            text = { Text("Видалити", fontFamily = InterFamily, fontSize = 14.sp) },
                            onClick = { showMenu = false; onDelete(message.id) },
                            colors = MenuDefaults.itemColors(textColor = CoremeColors.danger),
                        )
                    }
                }

                // Emoji picker
                DropdownMenu(
                    expanded = showEmojiPicker,
                    onDismissRequest = { showEmojiPicker = false },
                    modifier = Modifier
                        .background(Color(0xFF1E1F26))
                        .border(0.5.dp, Color(0x18FFFFFF), RoundedCornerShape(16.dp)),
                ) {
                    Row(modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)) {
                        COMMON_EMOJIS.forEach { emoji ->
                            Text(
                                text = emoji,
                                fontSize = 22.sp,
                                modifier = Modifier
                                    .padding(6.dp)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                        onClick = {
                                            showEmojiPicker = false
                                            onAddReaction(message.id, emoji)
                                        },
                                    ),
                            )
                        }
                    }
                }
            }

            // Reactions row
            if (message.reactions.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(top = 5.dp, start = 4.dp, end = 4.dp, bottom = 2.dp),
                ) {
                    groupedReactions(message.reactions).forEach { (emoji, users) ->
                        val myReaction = users.any { it.userId == currentUserId }
                        ReactionChip(
                            emoji = emoji,
                            count = users.size,
                            isSelected = myReaction,
                            onClick = {
                                if (myReaction) onRemoveReaction(message.id, emoji)
                                else onAddReaction(message.id, emoji)
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BubbleContent(message: Message) {
    if (message.isDeleted) {
        Text(
            text = "Повідомлення видалено",
            style = TextStyle(
                fontFamily = InterFamily,
                fontSize = 14.sp,
                fontStyle = FontStyle.Italic,
                color = Color.White.copy(alpha = 0.35f),
            ),
        )
        return
    }
    when (message.type) {
        MessageType.TEXT -> Text(
            text = message.content,
            style = TextStyle(
                fontFamily = InterFamily,
                fontSize = 15.sp,
                lineHeight = 22.sp,
                color = CoremeColors.textPrimary,
            ),
        )
        MessageType.VOICE -> Text(
            text = "🎙 Голосове",
            style = TextStyle(fontFamily = InterFamily, fontSize = 14.sp, color = CoremeColors.textSecondary),
        )
        MessageType.VIDEO -> Text(
            text = "🎥 Відео",
            style = TextStyle(fontFamily = InterFamily, fontSize = 14.sp, color = CoremeColors.textSecondary),
        )
        MessageType.AUDIO -> Text(
            text = "🎵 Аудіо",
            style = TextStyle(fontFamily = InterFamily, fontSize = 14.sp, color = CoremeColors.textSecondary),
        )
        MessageType.FILE -> Text(
            text = "📎 ${message.fileName ?: "Файл"}",
            style = TextStyle(fontFamily = InterFamily, fontSize = 14.sp, color = CoremeColors.textSecondary),
        )
        MessageType.STICKER -> Text(text = message.content, fontSize = 36.sp)
        else -> Unit
    }
}

@Composable
private fun ReactionChip(emoji: String, count: Int, isSelected: Boolean, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isSelected) CoremeColors.accentInteractive else Color(0x0EFFFFFF),
                RoundedCornerShape(12.dp),
            )
            .border(
                0.6.dp,
                if (isSelected) CoremeColors.accent.copy(alpha = 0.45f) else Color(0x14FFFFFF),
                RoundedCornerShape(12.dp),
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Text(text = emoji, fontSize = 13.sp)
        if (count > 1) {
            Spacer(Modifier.width(3.dp))
            Text(
                text = count.toString(),
                style = TextStyle(
                    fontFamily = InterFamily,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isSelected) CoremeColors.accent else CoremeColors.textSecondary,
                ),
            )
        }
    }
}

@Composable
private fun ReplyPreviewInBubble(
    senderName: String?,
    content: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0x0AFFFFFF), RoundedCornerShape(10.dp)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Left accent bar
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(36.dp)
                .background(
                    Brush.verticalGradient(listOf(CoremeColors.accent, CoremeColors.accentDim)),
                ),
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp, vertical = 6.dp),
        ) {
            if (senderName != null) {
                Text(
                    text = senderName,
                    style = TextStyle(
                        fontFamily = InterFamily,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = CoremeColors.accent,
                    ),
                )
                Spacer(Modifier.height(1.dp))
            }
            Text(
                text = content,
                style = TextStyle(
                    fontFamily = InterFamily,
                    fontSize = 12.sp,
                    color = CoremeColors.textSecondary,
                    lineHeight = 16.sp,
                ),
                maxLines = 2,
            )
        }
    }
    Spacer(modifier = Modifier.height(6.dp))
}

private fun groupedReactions(reactions: List<Reaction>): Map<String, List<Reaction>> =
    reactions.groupBy { it.emoji }

private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
