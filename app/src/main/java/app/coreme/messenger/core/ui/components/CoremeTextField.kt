package app.coreme.messenger.core.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.coreme.messenger.core.ui.theme.CoremeColors
import app.coreme.messenger.core.ui.theme.InterFamily

@Composable
fun CoremeTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    label: String? = null,
    prefixIcon: ImageVector? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    enabled: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    singleLine: Boolean = true,
    errorText: String? = null,
) {
    var focused by remember { mutableStateOf(false) }
    val hasError = errorText != null

    val borderColor by animateColorAsState(
        targetValue = when {
            hasError -> CoremeColors.danger
            focused -> CoremeColors.accent.copy(alpha = 0.7f)
            else -> CoremeColors.glassBorderSubtle
        },
        animationSpec = tween(180),
        label = "border",
    )
    val borderWidth = if (focused || hasError) 1.5.dp else 1.dp

    val labelColor by animateColorAsState(
        targetValue = when {
            hasError -> CoremeColors.danger
            focused -> CoremeColors.accent
            else -> CoremeColors.textSecondary
        },
        animationSpec = tween(180),
        label = "label_color",
    )

    val shape = RoundedCornerShape(14.dp)

    Column(modifier = modifier) {
        if (label != null) {
            Text(
                text = label,
                color = labelColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = InterFamily,
                letterSpacing = 0.2.sp,
            )
            Spacer(Modifier.height(7.dp))
        }

        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            singleLine = singleLine,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            visualTransformation = visualTransformation,
            textStyle = TextStyle(
                color = CoremeColors.textPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Normal,
                fontFamily = InterFamily,
                lineHeight = 22.sp,
            ),
            cursorBrush = SolidColor(CoremeColors.accent),
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(
                    if (focused) CoremeColors.glassThick else CoremeColors.glassRegular,
                    shape,
                )
                .border(borderWidth, borderColor, shape)
                .onFocusChanged { focused = it.isFocused },
            decorationBox = { innerTextField ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 15.dp),
                ) {
                    if (prefixIcon != null) {
                        Icon(
                            imageVector = prefixIcon,
                            contentDescription = null,
                            tint = if (focused) CoremeColors.accent.copy(alpha = 0.8f) else CoremeColors.textSecondary.copy(alpha = 0.6f),
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.size(11.dp))
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        if (value.isEmpty()) {
                            Text(
                                text = placeholder,
                                color = CoremeColors.textHint,
                                fontSize = 15.sp,
                                fontFamily = InterFamily,
                            )
                        }
                        innerTextField()
                    }
                    if (trailingIcon != null) {
                        trailingIcon()
                    }
                }
            },
        )

        if (hasError) {
            Spacer(Modifier.height(5.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 4.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .background(CoremeColors.danger, shape = RoundedCornerShape(2.dp)),
                )
                Spacer(Modifier.size(6.dp))
                Text(
                    text = errorText!!,
                    color = CoremeColors.danger,
                    fontSize = 12.sp,
                    fontFamily = InterFamily,
                )
            }
        }
    }
}
