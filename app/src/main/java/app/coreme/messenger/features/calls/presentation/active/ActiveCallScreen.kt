package app.coreme.messenger.features.calls.presentation.active

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import app.coreme.messenger.core.ui.components.UserAvatar
import app.coreme.messenger.core.ui.theme.CoremeColors

@Composable
fun ActiveCallScreen(
    onCallEnded: () -> Unit,
    viewModel: ActiveCallViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.phase) {
        if (uiState.phase == CallPhase.ENDED) onCallEnded()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0D0D14), Color(0xFF1A1A28)),
                ),
            ),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(vertical = 48.dp),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                UserAvatar(
                    displayName = uiState.participantName,
                    avatarUrl = uiState.participantAvatarUrl,
                    size = 96.dp,
                )

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = uiState.participantName,
                    color = Color.White,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.SemiBold,
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = when (uiState.phase) {
                        CallPhase.CONNECTING -> "Connecting..."
                        CallPhase.CONNECTED -> formatDuration(uiState.durationSeconds)
                        CallPhase.ENDED -> "Call ended"
                    },
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 16.sp,
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CallControlButton(
                        icon = if (uiState.isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                        label = if (uiState.isMuted) "Unmute" else "Mute",
                        active = uiState.isMuted,
                        onClick = viewModel::toggleMute,
                    )
                    CallControlButton(
                        icon = if (uiState.isSpeakerOn) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                        label = "Speaker",
                        active = uiState.isSpeakerOn,
                        onClick = viewModel::toggleSpeaker,
                    )
                    if (uiState.callType == "video") {
                        CallControlButton(
                            icon = if (uiState.isVideoEnabled) Icons.Default.Videocam else Icons.Default.VideocamOff,
                            label = "Video",
                            active = uiState.isVideoEnabled,
                            onClick = viewModel::toggleVideo,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE53935)),
                ) {
                    IconButton(onClick = viewModel::endCall, modifier = Modifier.size(72.dp)) {
                        Icon(Icons.Default.CallEnd, contentDescription = "End call", tint = Color.White, modifier = Modifier.size(32.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun CallControlButton(
    icon: ImageVector,
    label: String,
    active: Boolean,
    onClick: () -> Unit,
    size: Dp = 56.dp,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(if (active) Color.White.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.08f)),
        ) {
            IconButton(onClick = onClick, modifier = Modifier.size(size)) {
                Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(24.dp))
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(label, color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
    }
}

private fun formatDuration(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return "%02d:%02d".format(m, s)
}
