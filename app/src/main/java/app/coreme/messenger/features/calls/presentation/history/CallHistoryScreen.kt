package app.coreme.messenger.features.calls.presentation.history

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
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallMissed
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import app.coreme.messenger.core.ui.components.AnimatedGradientBackground
import app.coreme.messenger.core.ui.components.CoremeButton
import app.coreme.messenger.core.ui.components.UserAvatar
import app.coreme.messenger.core.ui.theme.CoremeColors
import app.coreme.messenger.core.ui.theme.InterFamily
import app.coreme.messenger.core.ui.theme.PlusJakartaSansFamily
import app.coreme.messenger.features.calls.domain.model.Call
import app.coreme.messenger.features.calls.domain.model.CallStatus
import app.coreme.messenger.features.calls.domain.model.CallType
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@Composable
fun CallHistoryScreen(
    onNavigateToActiveCall: (String) -> Unit,
    viewModel: CallHistoryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val incomingCall by viewModel.incomingCall.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val navBottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    incomingCall?.let { call ->
        AlertDialog(
            onDismissRequest = { viewModel.rejectCall(call.callId) },
            title = {
                Text(
                    "Вхідний ${if (call.callType == "video") "відео" else "голосовий"} дзвінок",
                    color = CoremeColors.textPrimary,
                    fontFamily = InterFamily,
                    fontWeight = FontWeight.SemiBold,
                )
            },
            text = {
                Text(call.callerName, color = CoremeColors.textSecondary, fontFamily = InterFamily)
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.acceptCall(call.callId)
                    onNavigateToActiveCall(call.callId)
                }) {
                    Text("Прийняти", color = CoremeColors.stateOnline, fontFamily = InterFamily, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.rejectCall(call.callId) }) {
                    Text("Відхилити", color = CoremeColors.danger, fontFamily = InterFamily)
                }
            },
            containerColor = Color(0xFF1A1C22),
        )
    }

    AnimatedGradientBackground(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {

                // Top bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(listOf(Color(0xE81A1C22), Color(0xB0141618))),
                        )
                        .border(width = 0.5.dp, color = Color(0x0FFFFFFF)),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(start = 20.dp, end = 16.dp)
                            .height(56.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(listOf(CoremeColors.accent, CoremeColors.accentDim)),
                                ),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Дзвінки",
                            style = TextStyle(
                                fontFamily = PlusJakartaSansFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 24.sp,
                                letterSpacing = (-0.3).sp,
                                color = CoremeColors.textPrimary,
                            ),
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                when {
                    uiState.isLoading -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(
                                color = CoremeColors.accent,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(36.dp),
                            )
                        }
                    }
                    uiState.error != null -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(32.dp),
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(80.dp)
                                        .clip(CircleShape)
                                        .background(Color(0x0EFFFFFF))
                                        .border(1.dp, Color(0x14FFFFFF), CircleShape),
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = null,
                                        tint = CoremeColors.textTertiary,
                                        modifier = Modifier.size(36.dp),
                                    )
                                }
                                Spacer(Modifier.height(16.dp))
                                Text(
                                    text = "Не вдалося завантажити",
                                    style = TextStyle(
                                        fontFamily = PlusJakartaSansFamily,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 18.sp,
                                        color = CoremeColors.textSecondary,
                                    ),
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = uiState.error ?: "Перевірте підключення до мережі",
                                    style = TextStyle(
                                        fontFamily = InterFamily,
                                        fontSize = 13.sp,
                                        color = CoremeColors.textTertiary,
                                    ),
                                )
                                Spacer(Modifier.height(24.dp))
                                CoremeButton(
                                    text = "Спробувати знову",
                                    onClick = viewModel::retry,
                                    modifier = Modifier.fillMaxWidth(0.65f),
                                )
                            }
                        }
                    }
                    uiState.calls.isEmpty() -> {
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
                                    Icon(
                                        imageVector = Icons.Default.Phone,
                                        contentDescription = null,
                                        tint = CoremeColors.textTertiary,
                                        modifier = Modifier.size(46.dp),
                                    )
                                }
                                Spacer(Modifier.height(20.dp))
                                Text(
                                    text = "Немає дзвінків",
                                    style = TextStyle(
                                        fontFamily = PlusJakartaSansFamily,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 20.sp,
                                        color = CoremeColors.textSecondary,
                                    ),
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = "Відкрийте чат та натисніть кнопку дзвінка",
                                    style = TextStyle(
                                        fontFamily = InterFamily,
                                        fontSize = 14.sp,
                                        color = CoremeColors.textTertiary,
                                    ),
                                )
                            }
                        }
                    }
                    else -> {
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
                            items(uiState.calls, key = { it.id }) { call ->
                                CallHistoryItem(
                                    call = call,
                                    onCallBack = {
                                        viewModel.call(
                                            call.participantId,
                                            if (call.callType == CallType.VIDEO) "video" else "voice",
                                        )
                                        onNavigateToActiveCall(call.id)
                                    },
                                )
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
private fun CallHistoryItem(call: Call, onCallBack: () -> Unit) {
    val isMissed = call.status == CallStatus.MISSED
    val isOutgoing = call.status == CallStatus.OUTGOING
    val isVideo = call.callType == CallType.VIDEO

    val directionIcon = when {
        isMissed -> Icons.AutoMirrored.Filled.CallMissed
        isOutgoing -> Icons.AutoMirrored.Filled.CallMade
        else -> Icons.AutoMirrored.Filled.CallReceived
    }
    val directionColor = when {
        isMissed || call.status == CallStatus.DECLINED -> CoremeColors.danger
        isOutgoing -> CoremeColors.textSecondary
        else -> CoremeColors.stateOnline
    }
    val statusText = when {
        isMissed -> "Пропущений"
        isOutgoing -> "Вихідний"
        else -> "Вхідний"
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (isMissed) Color(0x10E5A5A5) else Color(0x0AFFFFFF),
                RoundedCornerShape(16.dp),
            )
            .border(
                width = if (isMissed) 0.7.dp else 0.5.dp,
                color = if (isMissed) CoremeColors.danger.copy(alpha = 0.18f) else Color(0x10FFFFFF),
                shape = RoundedCornerShape(16.dp),
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onCallBack,
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        UserAvatar(
            displayName = call.participantName,
            avatarUrl = call.participantAvatarUrl,
            size = 50.dp,
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = call.participantName,
                style = TextStyle(
                    fontFamily = InterFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = if (isMissed) CoremeColors.danger else CoremeColors.textPrimary,
                ),
            )
            Spacer(modifier = Modifier.height(3.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = directionIcon,
                    contentDescription = null,
                    tint = directionColor,
                    modifier = Modifier.size(13.dp),
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = statusText,
                    style = TextStyle(
                        fontFamily = InterFamily,
                        fontSize = 12.sp,
                        color = directionColor,
                    ),
                )
                if (isVideo) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Default.Videocam,
                        contentDescription = null,
                        tint = CoremeColors.textTertiary,
                        modifier = Modifier.size(13.dp),
                    )
                }
                if (call.duration > 0) {
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "· ${formatDuration(call.duration)}",
                        style = TextStyle(
                            fontFamily = InterFamily,
                            fontSize = 12.sp,
                            color = CoremeColors.textTertiary,
                        ),
                    )
                }
            }
        }

        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = formatCallDate(call.startedAt),
                style = TextStyle(
                    fontFamily = InterFamily,
                    fontSize = 11.sp,
                    color = CoremeColors.textTertiary,
                ),
            )
            Spacer(modifier = Modifier.height(6.dp))
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(CoremeColors.accent.copy(alpha = 0.12f))
                    .border(0.5.dp, CoremeColors.accent.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onCallBack,
                    ),
            ) {
                Icon(
                    imageVector = if (isVideo) Icons.Default.Videocam else Icons.Default.Phone,
                    contentDescription = "Подзвонити",
                    tint = CoremeColors.accent,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

private fun formatDuration(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return if (m > 0) "${m}хв ${s}с" else "${s}с"
}

private fun formatCallDate(instant: Instant): String {
    val now = Instant.now()
    val zone = ZoneId.systemDefault()
    val local = instant.atZone(zone)
    val diffDays = ChronoUnit.DAYS.between(instant.truncatedTo(java.time.temporal.ChronoUnit.DAYS), now.truncatedTo(java.time.temporal.ChronoUnit.DAYS))
    return when {
        diffDays == 0L -> DateTimeFormatter.ofPattern("HH:mm").format(local)
        diffDays < 7L -> {
            val days = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Нд")
            days[local.dayOfWeek.value - 1]
        }
        else -> "${local.dayOfMonth}.${local.monthValue.toString().padStart(2, '0')}"
    }
}
