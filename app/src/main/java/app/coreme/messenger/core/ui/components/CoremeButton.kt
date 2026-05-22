package app.coreme.messenger.core.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.coreme.messenger.core.ui.theme.CoremeColors
import app.coreme.messenger.core.ui.theme.InterFamily

enum class CoremeButtonVariant { Primary, Secondary, Danger, Glass }

@Composable
fun CoremeButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    variant: CoremeButtonVariant = CoremeButtonVariant.Primary,
    leadingIcon: @Composable (() -> Unit)? = null,
) {
    val haptic = LocalHapticFeedback.current
    var pressed by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = tween(90),
        label = "btn_scale",
    )

    val shape = RoundedCornerShape(16.dp)
    val isActive = enabled && !isLoading

    val contentColor = when (variant) {
        CoremeButtonVariant.Primary -> if (isActive) Color(0xFF0D0D12) else CoremeColors.textSecondary
        CoremeButtonVariant.Danger -> CoremeColors.danger
        CoremeButtonVariant.Secondary -> CoremeColors.textPrimary
        CoremeButtonVariant.Glass -> CoremeColors.textPrimary
    }

    val borderColor = when (variant) {
        CoremeButtonVariant.Secondary -> CoremeColors.accent.copy(alpha = 0.4f)
        CoremeButtonVariant.Danger -> CoremeColors.danger.copy(alpha = 0.55f)
        CoremeButtonVariant.Glass -> CoremeColors.glassBorder
        CoremeButtonVariant.Primary -> Color.Transparent
    }

    val bgModifier = when (variant) {
        CoremeButtonVariant.Primary -> if (isActive) {
            Modifier.background(
                Brush.linearGradient(
                    listOf(Color(0xFFE88A38), CoremeColors.accent, Color(0xFFC8621A)),
                ),
                shape,
            )
        } else {
            Modifier.background(CoremeColors.glassThick, shape)
        }
        CoremeButtonVariant.Secondary -> Modifier.background(Color.Transparent, shape)
        CoremeButtonVariant.Danger -> Modifier.background(CoremeColors.danger.copy(alpha = 0.07f), shape)
        CoremeButtonVariant.Glass -> Modifier.background(CoremeColors.glassThick, shape)
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .scale(scale)
            .clip(shape)
            .then(bgModifier)
            .border(
                width = if (variant == CoremeButtonVariant.Primary) 0.dp else 1.dp,
                color = borderColor,
                shape = shape,
            )
            .pointerInput(isActive) {
                detectTapGestures(
                    onPress = {
                        if (isActive) {
                            pressed = true
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            tryAwaitRelease()
                            pressed = false
                        }
                    },
                    onTap = { if (isActive) onClick() },
                )
            },
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                color = contentColor,
                strokeWidth = 2.dp,
                modifier = Modifier.size(22.dp),
            )
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (leadingIcon != null) {
                    leadingIcon()
                    Spacer(Modifier.width(8.dp))
                }
                Text(
                    text = text,
                    style = TextStyle(
                        fontFamily = InterFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        letterSpacing = 0.1.sp,
                        color = contentColor,
                    ),
                )
            }
        }
    }
}
