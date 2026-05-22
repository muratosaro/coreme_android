package app.coreme.messenger.features.chats.presentation.detail.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.coreme.messenger.core.ui.theme.CoremeColors
import app.coreme.messenger.core.ui.theme.InterFamily
import app.coreme.messenger.features.chats.domain.model.Message

@Composable
fun MessageInput(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    replyTo: Message?,
    editingMessage: Message?,
    onClearReply: () -> Unit,
    onCancelEdit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val hasText = value.isNotBlank()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xCC0D0F14), Color(0xF01A1C22)),
                ),
            )
            .navigationBarsPadding(),
    ) {
        HorizontalDivider(color = Color(0x14FFFFFF), thickness = 0.5.dp)

        // Reply / Edit banner
        if (replyTo != null) {
            ReplyEditBanner(
                icon = Icons.AutoMirrored.Filled.Reply,
                accentLabel = replyTo.senderName ?: "Повідомлення",
                content = replyTo.content,
                onClose = onClearReply,
            )
        } else if (editingMessage != null) {
            ReplyEditBanner(
                icon = Icons.Filled.Edit,
                accentLabel = "Редагування",
                content = editingMessage.content,
                onClose = onCancelEdit,
            )
        }

        Row(
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 10.dp),
        ) {
            // Attach button
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0x0FFFFFFF))
                    .border(0.5.dp, Color(0x14FFFFFF), CircleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {},
                    ),
            ) {
                Icon(
                    imageVector = Icons.Default.AttachFile,
                    contentDescription = "Прикріпити",
                    tint = CoremeColors.textSecondary.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp),
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Text field
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = TextStyle(
                    color = CoremeColors.textPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Normal,
                    fontFamily = InterFamily,
                    lineHeight = 21.sp,
                ),
                cursorBrush = SolidColor(CoremeColors.accent),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                maxLines = 5,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color(0x18FFFFFF))
                    .border(0.7.dp, Color(0x12FFFFFF), RoundedCornerShape(22.dp))
                    .padding(horizontal = 16.dp, vertical = 11.dp),
                decorationBox = { inner ->
                    Box {
                        if (value.isEmpty()) {
                            Text(
                                text = "Напишіть повідомлення…",
                                color = CoremeColors.textHint,
                                fontSize = 15.sp,
                                fontFamily = InterFamily,
                            )
                        }
                        inner()
                    }
                },
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Send / Mic button with animation
            SendMicButton(hasText = hasText, onSend = onSend)
        }
    }
}

@Composable
private fun SendMicButton(hasText: Boolean, onSend: () -> Unit) {
    Box(modifier = Modifier.size(44.dp)) {
        AnimatedVisibility(
            visible = hasText,
            enter = scaleIn(animationSpec = tween(180)),
            exit = scaleOut(animationSpec = tween(140)),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(44.dp)
                    .shadow(
                        elevation = 12.dp,
                        shape = CircleShape,
                        spotColor = CoremeColors.accent.copy(alpha = 0.4f),
                        ambientColor = CoremeColors.accent.copy(alpha = 0.2f),
                    )
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(listOf(Color(0xFFEA8C42), CoremeColors.accent)),
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onSend,
                    ),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Надіслати",
                    tint = Color(0xFF0D0D12),
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        AnimatedVisibility(
            visible = !hasText,
            enter = scaleIn(animationSpec = tween(180)),
            exit = scaleOut(animationSpec = tween(140)),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color(0x18FFFFFF))
                    .border(0.7.dp, Color(0x14FFFFFF), CircleShape),
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "Мікрофон",
                    tint = CoremeColors.textSecondary,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
    }
}

@Composable
private fun ReplyEditBanner(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentLabel: String,
    content: String,
    onClose: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0x08FFFFFF))
            .padding(start = 16.dp, end = 10.dp, top = 9.dp, bottom = 9.dp),
    ) {
        // Vertical accent line
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(34.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(CoremeColors.accent, CoremeColors.accentDim),
                    ),
                ),
        )
        Spacer(modifier = Modifier.width(10.dp))
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = CoremeColors.accent,
            modifier = Modifier.size(14.dp),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = accentLabel,
                color = CoremeColors.accent,
                fontFamily = InterFamily,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.1.sp,
            )
            Spacer(Modifier.height(1.dp))
            Text(
                text = content,
                color = CoremeColors.textSecondary,
                fontFamily = InterFamily,
                fontSize = 12.sp,
                maxLines = 1,
                lineHeight = 17.sp,
            )
        }
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(Color(0x14FFFFFF))
                .border(0.5.dp, Color(0x10FFFFFF), CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClose,
                ),
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = null,
                tint = CoremeColors.textSecondary.copy(alpha = 0.7f),
                modifier = Modifier.size(13.dp),
            )
        }
    }
    HorizontalDivider(color = Color(0x0AFFFFFF), thickness = 0.5.dp)
}
