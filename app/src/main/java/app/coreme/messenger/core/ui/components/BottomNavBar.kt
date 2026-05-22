package app.coreme.messenger.core.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Contacts
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.coreme.messenger.core.ui.theme.CoremeColors
import app.coreme.messenger.core.ui.theme.InterFamily
import app.coreme.messenger.navigation.Routes

private data class NavItem(
    val route: String,
    val label: String,
    val activeIcon: ImageVector,
    val inactiveIcon: ImageVector,
    val badgeCount: Int = 0,
)

@Composable
fun BottomNavBar(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    unreadChatsCount: Int = 0,
    modifier: Modifier = Modifier,
) {
    val navBottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    val items = listOf(
        NavItem(Routes.CHATS, "Чати", Icons.Filled.ChatBubble, Icons.Outlined.ChatBubbleOutline, unreadChatsCount),
        NavItem(Routes.CALLS, "Дзвінки", Icons.Filled.Call, Icons.Outlined.Call),
        NavItem(Routes.CONTACTS, "Контакти", Icons.Filled.Contacts, Icons.Outlined.Contacts),
        NavItem(Routes.PROFILE, "Профіль", Icons.Filled.Person, Icons.Outlined.Person),
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = navBottomPadding + 14.dp, start = 24.dp, end = 24.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 24.dp,
                    shape = RoundedCornerShape(28.dp),
                    spotColor = Color(0x66000000),
                    ambientColor = Color(0x33000000),
                )
                .clip(RoundedCornerShape(28.dp))
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xD4111420), Color(0xD4101318)),
                    ),
                )
                .border(0.7.dp, Color(0x1EFFFFFF), RoundedCornerShape(28.dp))
                .padding(vertical = 6.dp, horizontal = 4.dp),
        ) {
            items.forEach { item ->
                val isActive = currentRoute == item.route
                NavBarItem(
                    item = item,
                    isActive = isActive,
                    onClick = { if (!isActive) onNavigate(item.route) },
                )
            }
        }
    }
}

@Composable
private fun NavBarItem(item: NavItem, isActive: Boolean, onClick: () -> Unit) {
    val pillAlpha by animateFloatAsState(
        targetValue = if (isActive) 1f else 0f,
        animationSpec = tween(250),
        label = "pill_alpha",
    )
    val scale by animateFloatAsState(
        targetValue = if (isActive) 1f else 0.92f,
        animationSpec = tween(200),
        label = "icon_scale",
    )
    val interactionSource = remember { MutableInteractionSource() }

    Box {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick,
                )
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            // Icon with pill highlight
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        CoremeColors.accent.copy(alpha = pillAlpha * 0.15f),
                        RoundedCornerShape(12.dp),
                    )
                    .padding(horizontal = 10.dp, vertical = 5.dp),
            ) {
                Icon(
                    imageVector = if (isActive) item.activeIcon else item.inactiveIcon,
                    contentDescription = item.label,
                    tint = if (isActive) CoremeColors.accent else CoremeColors.textTertiary.copy(alpha = 0.5f),
                    modifier = Modifier
                        .size(22.dp)
                        .scale(scale),
                )
            }
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = item.label,
                fontFamily = InterFamily,
                fontSize = 10.sp,
                fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isActive) CoremeColors.accent else CoremeColors.textTertiary.copy(alpha = 0.5f),
            )
        }

        // Unread badge
        if (item.badgeCount > 0) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 6.dp, end = 10.dp)
                    .size(if (item.badgeCount > 9) 18.dp else 15.dp)
                    .clip(CircleShape)
                    .background(CoremeColors.accent),
            ) {
                Text(
                    text = if (item.badgeCount > 99) "99+" else item.badgeCount.toString(),
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    lineHeight = 8.sp,
                )
            }
        }
    }
}
