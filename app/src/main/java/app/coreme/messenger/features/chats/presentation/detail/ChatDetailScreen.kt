package app.coreme.messenger.features.chats.presentation.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import app.coreme.messenger.core.ui.components.AnimatedGradientBackground
import app.coreme.messenger.core.ui.components.UserAvatar
import app.coreme.messenger.core.ui.theme.CoremeColors
import app.coreme.messenger.core.ui.theme.InterFamily
import app.coreme.messenger.features.chats.domain.model.ChatType
import app.coreme.messenger.features.chats.presentation.detail.components.MessageBubble
import app.coreme.messenger.features.chats.presentation.detail.components.MessageInput
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
fun ChatDetailScreen(
    onBack: () -> Unit,
    viewModel: ChatDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()

    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.lastIndex)
        }
    }

    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .distinctUntilChanged()
            .collect { index ->
                if (index == 0 && uiState.hasMoreMessages && !uiState.isLoadingMore) {
                    viewModel.loadMore()
                }
            }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    AnimatedGradientBackground(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .imePadding(),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                ChatTopBar(uiState = uiState, onBack = onBack)

                if (uiState.isLoading) {
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = CoremeColors.accent)
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(vertical = 8.dp),
                        modifier = Modifier.weight(1f),
                    ) {
                        if (uiState.isLoadingMore) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    CircularProgressIndicator(
                                        color = CoremeColors.accent,
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp,
                                    )
                                }
                            }
                        }

                        items(uiState.messages, key = { it.id }) { message ->
                            val isOwn = message.senderId == uiState.currentUserId
                            val isGroup = uiState.chat?.type == ChatType.GROUP
                            MessageBubble(
                                message = message,
                                isOwn = isOwn,
                                currentUserId = uiState.currentUserId ?: "",
                                showSenderName = isGroup,
                                onReply = { viewModel.setReplyTo(it) },
                                onEdit = { viewModel.startEdit(it) },
                                onDelete = { viewModel.deleteMessage(it) },
                                onAddReaction = { msgId, emoji -> viewModel.addReaction(msgId, emoji) },
                                onRemoveReaction = { msgId, emoji -> viewModel.removeReaction(msgId, emoji) },
                            )
                        }
                    }
                }

                MessageInput(
                    value = uiState.inputText,
                    onValueChange = viewModel::onInputChange,
                    onSend = viewModel::send,
                    replyTo = uiState.replyTo,
                    editingMessage = uiState.editingMessage,
                    onClearReply = viewModel::clearReply,
                    onCancelEdit = viewModel::cancelEdit,
                )
            }

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}

@Composable
private fun ChatTopBar(uiState: ChatDetailUiState, onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xF0141618), Color(0xB0111316)),
                ),
            ),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(start = 4.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
        ) {
            // Back button
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color(0x0AFFFFFF)),
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Назад",
                        tint = CoremeColors.textPrimary,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            uiState.chat?.let { chat ->
                UserAvatar(
                    displayName = chat.displayName,
                    avatarUrl = chat.avatarUrl,
                    size = 40.dp,
                    isOnline = chat.isOnline,
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = chat.displayName,
                        color = CoremeColors.textPrimary,
                        fontFamily = InterFamily,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        letterSpacing = 0.sp,
                    )
                    when {
                        uiState.typingUserNames.isNotEmpty() -> Text(
                            text = "друкує…",
                            color = CoremeColors.accent,
                            fontFamily = InterFamily,
                            fontSize = 12.sp,
                        )
                        chat.isOnline -> Text(
                            text = "в мережі",
                            color = CoremeColors.stateOnline,
                            fontFamily = InterFamily,
                            fontSize = 12.sp,
                        )
                        chat.type == ChatType.GROUP -> Text(
                            text = "${chat.memberCount} учасників",
                            color = CoremeColors.textTertiary,
                            fontFamily = InterFamily,
                            fontSize = 12.sp,
                        )
                        else -> Text(
                            text = "CoreMe",
                            color = CoremeColors.textTertiary,
                            fontFamily = InterFamily,
                            fontSize = 12.sp,
                        )
                    }
                }

                // Action icons
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0x0CFFFFFF)),
                ) {
                    IconButton(onClick = {}) {
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = "Дзвінок",
                            tint = CoremeColors.textSecondary,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
                Spacer(Modifier.width(4.dp))
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0x0CFFFFFF)),
                ) {
                    IconButton(onClick = {}) {
                        Icon(
                            imageVector = Icons.Default.Videocam,
                            contentDescription = "Відеодзвінок",
                            tint = CoremeColors.textSecondary,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            } ?: Spacer(modifier = Modifier.weight(1f))
        }
    }
}
