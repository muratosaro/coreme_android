package app.coreme.messenger.features.chats.presentation.list

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import app.coreme.messenger.core.ui.components.AnimatedGradientBackground
import app.coreme.messenger.core.ui.components.UserAvatar
import app.coreme.messenger.core.ui.theme.CoremeColors
import app.coreme.messenger.core.ui.theme.InterFamily
import app.coreme.messenger.core.ui.theme.PlusJakartaSansFamily
import app.coreme.messenger.features.auth.presentation.login.LogoWordmark
import app.coreme.messenger.features.chats.domain.model.Chat
import app.coreme.messenger.features.chats.domain.model.MessageType
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatsScreen(
    onOpenChat: (String) -> Unit,
    onNavigateToChannels: () -> Unit = {},
    onNavigateToNewChat: () -> Unit = {},
    viewModel: ChatsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    AnimatedGradientBackground(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                ChatsTopBar(onNavigateToNewChat = onNavigateToNewChat)

                if (uiState.isLoading && uiState.chats.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            color = CoremeColors.accent,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(36.dp),
                        )
                    }
                } else {
                    PullToRefreshBox(
                        isRefreshing = uiState.isRefreshing,
                        onRefresh = viewModel::refresh,
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        if (uiState.chats.isEmpty()) {
                            EmptyChatsPlaceholder()
                        } else {
                            val navBottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                            LazyColumn(
                                contentPadding = PaddingValues(
                                    top = 6.dp,
                                    bottom = navBottomPadding + 88.dp,
                                    start = 14.dp,
                                    end = 14.dp,
                                ),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.fillMaxSize(),
                            ) {
                                items(uiState.chats, key = { it.id }) { chat ->
                                    ChatListItem(
                                        chat = chat,
                                        onClick = { onOpenChat(chat.id) },
                                    )
                                }
                            }
                        }
                    }
                }
            }

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}

@Composable
private fun ChatsTopBar(onNavigateToNewChat: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xE81A1C22), Color(0xB0141618)),
                ),
            )
            .border(width = 0.5.dp, color = Color(0x0FFFFFFF)),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(start = 20.dp, end = 10.dp)
                .height(56.dp),
        ) {
            // Logo wordmark
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                // Orange accent dot
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(CoremeColors.accent, CoremeColors.accentDim),
                            ),
                        ),
                )
                Spacer(Modifier.width(8.dp))
                LogoWordmark(fontSize = 24)
            }

            // Search button
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0x0CFFFFFF))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {},
                    ),
            ) {
                Icon(Icons.Default.Search, contentDescription = "Пошук", tint = CoremeColors.textSecondary, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(6.dp))
            // New chat button
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(CoremeColors.accent.copy(alpha = 0.15f))
                    .border(0.5.dp, CoremeColors.accent.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onNavigateToNewChat,
                    ),
            ) {
                Icon(Icons.Default.Edit, contentDescription = "Новий чат", tint = CoremeColors.accent, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(4.dp))
        }
    }
}

@Composable
private fun ChatListItem(chat: Chat, onClick: () -> Unit) {
    val hasUnread = chat.unreadCount > 0

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (hasUnread) Color(0x14FFFFFF) else Color(0x0AFFFFFF),
                RoundedCornerShape(16.dp),
            )
            .border(
                width = if (hasUnread) 0.8.dp else 0.5.dp,
                color = if (hasUnread) Color(0x22FFFFFF) else Color(0x10FFFFFF),
                shape = RoundedCornerShape(16.dp),
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 14.dp, vertical = 11.dp),
    ) {
        // Unread indicator strip on left edge — rendered as tinted left border via Box overlay
        if (hasUnread) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(36.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        Brush.verticalGradient(listOf(CoremeColors.accent, CoremeColors.accentDim)),
                    ),
            )
            Spacer(Modifier.width(10.dp))
        }

        UserAvatar(
            displayName = chat.displayName,
            avatarUrl = chat.avatarUrl,
            size = 50.dp,
            isOnline = chat.isOnline,
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = chat.displayName,
                    style = TextStyle(
                        fontFamily = InterFamily,
                        fontSize = 15.sp,
                        fontWeight = if (hasUnread) FontWeight.Bold else FontWeight.SemiBold,
                        color = CoremeColors.textPrimary,
                        letterSpacing = 0.sp,
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                chat.lastMessage?.let { msg ->
                    Text(
                        text = formatTime(msg.createdAt),
                        style = TextStyle(
                            fontFamily = InterFamily,
                            fontSize = 11.sp,
                            fontWeight = if (hasUnread) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (hasUnread) CoremeColors.accent else CoremeColors.textTertiary,
                        ),
                    )
                }
            }

            Spacer(Modifier.height(3.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                val lastMsgText = chat.lastMessage?.let { msg ->
                    if (msg.isDeleted) "Повідомлення видалено"
                    else when (msg.type) {
                        MessageType.IMAGE -> "📷 Фото"
                        MessageType.VIDEO -> "🎥 Відео"
                        MessageType.VOICE -> "🎙 Голосове"
                        MessageType.AUDIO -> "🎵 Аудіо"
                        MessageType.FILE -> "📎 Файл"
                        MessageType.STICKER -> "Стікер"
                        MessageType.TEXT -> {
                            if (chat.type.name == "GROUP" && msg.senderName != null) {
                                "${msg.senderName.split(" ").first()}: ${msg.content}"
                            } else msg.content
                        }
                    }
                } ?: "Немає повідомлень"

                Text(
                    text = lastMsgText,
                    style = TextStyle(
                        fontFamily = InterFamily,
                        fontSize = 13.sp,
                        color = if (hasUnread) CoremeColors.textSecondary else CoremeColors.textTertiary,
                        lineHeight = 18.sp,
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )

                if (hasUnread) {
                    Spacer(modifier = Modifier.width(8.dp))
                    UnreadBadge(count = chat.unreadCount)
                }
            }
        }
    }
}

@Composable
private fun UnreadBadge(count: Int) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .clip(CircleShape)
            .background(
                Brush.linearGradient(listOf(CoremeColors.accent, CoremeColors.accentDim)),
                CircleShape,
            )
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(
            text = if (count > 99) "99+" else count.toString(),
            style = TextStyle(
                fontFamily = InterFamily,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0D0D12),
            ),
            lineHeight = 10.sp,
        )
    }
}

@Composable
private fun EmptyChatsPlaceholder() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(Color(0x0EFFFFFF))
                    .border(1.dp, Color(0x14FFFFFF), CircleShape),
            ) {
                Text(text = "💬", fontSize = 42.sp)
            }
            Spacer(Modifier.height(20.dp))
            Text(
                text = "Немає чатів",
                style = TextStyle(
                    fontFamily = PlusJakartaSansFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 20.sp,
                    color = CoremeColors.textSecondary,
                ),
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Натисніть ✏️ щоб почати нову розмову",
                style = TextStyle(
                    fontFamily = InterFamily,
                    fontSize = 14.sp,
                    color = CoremeColors.textTertiary,
                ),
            )
        }
    }
}

private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
private val ukrainianMonths = listOf("", "січ", "лют", "бер", "кві", "тра", "чер", "лип", "сер", "вер", "жов", "лис", "гру")

private fun formatTime(instant: Instant): String {
    val now = Instant.now()
    val zone = ZoneId.systemDefault()
    val local = instant.atZone(zone)
    return when {
        ChronoUnit.DAYS.between(instant, now) == 0L -> timeFormatter.format(local)
        ChronoUnit.DAYS.between(instant, now) == 1L -> "вчора"
        ChronoUnit.DAYS.between(instant, now) < 7L -> {
            val days = listOf("пн", "вт", "ср", "чт", "пт", "сб", "нд")
            days[local.dayOfWeek.value - 1]
        }
        else -> "${local.dayOfMonth} ${ukrainianMonths[local.monthValue]}"
    }
}
