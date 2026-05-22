package app.coreme.messenger.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.coreme.messenger.core.ui.theme.CoremeColors

/**
 * Glass container matching Flutter's GlassContainer widget.
 * Semi-transparent surface + border + top highlight gradient.
 *
 * Note: We intentionally do NOT apply Modifier.blur() here because in Compose
 * blur() blurs the composable's own content (children), not what's behind it.
 * True backdrop blur requires View-level RenderEffect (API 31+) and is not used here
 * to keep content readable. The glass look comes from transparency + border + highlight.
 */
@Composable
fun GlassContainer(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(0.dp),
    surface: Color = CoremeColors.glassRegular,
    borderColor: Color = CoremeColors.glassBorder,
    borderWidth: Dp = 0.5.dp,
    showHighlight: Boolean = true,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(surface, shape)
            .border(borderWidth, borderColor, shape),
    ) {
        if (showHighlight) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                CoremeColors.glassHighlight,
                                Color.Transparent,
                            ),
                        ),
                    ),
            )
        }
        content()
    }
}

// Legacy modifier extensions
fun Modifier.glassSurface(alpha: Float = 0.078f): Modifier =
    background(Color.White.copy(alpha = alpha))

fun Modifier.glassCard(): Modifier = glassSurface(alpha = 0.125f)
fun Modifier.glassThinModifier(): Modifier = glassSurface(alpha = 0.047f)
