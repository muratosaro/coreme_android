package app.coreme.messenger.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.coreme.messenger.core.ui.theme.CoremeColors
import app.coreme.messenger.core.ui.theme.InterFamily
import coil3.compose.AsyncImage

// Matches Flutter's AppAvatar._avatarShades — 8 gradient pairs
private val AVATAR_SHADES = listOf(
    listOf(Color(0xFF26263A), Color(0xFF383852)), // indigo-grey
    listOf(Color(0xFF1E2A2A), Color(0xFF2E3E3E)), // teal-grey
    listOf(Color(0xFF2A1E2A), Color(0xFF3E2E3E)), // plum-grey
    listOf(Color(0xFF2A2A1E), Color(0xFF3E3E2E)), // warm-grey
    listOf(Color(0xFF1E1E2A), Color(0xFF2E2E40)), // blue-grey
    listOf(Color(0xFF2A1E1E), Color(0xFF3E2E2E)), // rust-grey
    listOf(Color(0xFF1E2A1E), Color(0xFF2E3E2E)), // sage-grey
    listOf(Color(0xFF222222), Color(0xFF363636)), // pure grey
)

private fun shadesForName(name: String): List<Color> {
    if (name.isEmpty()) return AVATAR_SHADES[0]
    val hash = name.codePoints().sum()
    return AVATAR_SHADES[hash % AVATAR_SHADES.size]
}

@Composable
fun UserAvatar(
    displayName: String,
    avatarUrl: String?,
    size: Dp = 44.dp,
    isOnline: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val shades = shadesForName(displayName)
    val dotSize = (size.value * 0.28f).dp

    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(colors = shades),
                    CircleShape,
                )
                .border(0.5.dp, Color(0x1AFFFFFF), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (avatarUrl != null) {
                AsyncImage(
                    model = avatarUrl,
                    contentDescription = displayName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(size)
                        .clip(CircleShape),
                )
            } else {
                AvatarInitials(displayName = displayName, size = size)
            }
        }

        if (isOnline) {
            Box(
                modifier = Modifier
                    .size(dotSize)
                    .clip(CircleShape)
                    .background(CoremeColors.stateOnline)
                    .border(2.dp, CoremeColors.backgroundBase, CircleShape)
                    .align(Alignment.BottomEnd),
            )
        }
    }
}

@Composable
private fun AvatarInitials(displayName: String, size: Dp) {
    val parts = displayName.trim().split(Regex("\\s+"))
    val initials = if (parts.size >= 2) {
        "${parts[0].firstOrNull()?.uppercaseChar() ?: ""}${parts[1].firstOrNull()?.uppercaseChar() ?: ""}"
    } else {
        displayName.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
    }

    Text(
        text = initials,
        color = Color.White,
        fontSize = (size.value * 0.38f).sp,
        fontWeight = FontWeight.Bold,
        fontFamily = InterFamily,
    )
}
