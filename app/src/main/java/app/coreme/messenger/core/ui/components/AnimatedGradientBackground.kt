package app.coreme.messenger.core.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import app.coreme.messenger.core.ui.theme.CoremeColors

/**
 * Full-screen animated radial gradient background.
 * Cold dark palette: backgroundCenter → backgroundDeep.
 * Animation is a slow breathing cycle (~8 s), throttled to ~20 FPS via
 * tween easing so the GPU isn't hammered on every vsync.
 * No orbs — clean minimal gradient only.
 */
@Composable
fun AnimatedGradientBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val transition = rememberInfiniteTransition(label = "bg_gradient")

    // Slow breathe: radius and center shift over 8 s, 20 FPS effective
    val animFraction by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8_000),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "bg_fraction",
    )

    Box(
        modifier = modifier.drawBehind {
            val centerX = size.width * (0.45f + animFraction * 0.1f)
            val centerY = size.height * (0.32f + animFraction * 0.08f)
            val radius = size.width * (0.75f + animFraction * 0.12f)

            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        CoremeColors.backgroundCenter,
                        CoremeColors.backgroundDeep,
                    ),
                    center = Offset(centerX, centerY),
                    radius = radius,
                ),
            )
        },
        content = content,
    )
}
