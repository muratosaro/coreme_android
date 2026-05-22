package app.coreme.messenger.core.ui.components

import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.ui.graphics.Color
import app.coreme.messenger.core.ui.theme.CoremeColors
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private data class Blob(
    val color: Color,
    val fx: Float, val fy: Float,
    val r: Float,
    val ax: Float, val ay: Float,
    val sx: Float, val phase: Float,
)

private val BLOBS = listOf(
    Blob(Color(0x0BFFFFFF), -0.25f, -0.35f, 0.55f, 0.09f, 0.08f, 1.0f, 0.0f),
    Blob(Color(0x07FFFFFF),  0.60f, -0.20f, 0.45f, 0.07f, 0.06f, 1.3f, 1.4f),
    Blob(Color(0x14E27D2D),  0.00f,  0.45f, 0.52f, 0.08f, 0.07f, 0.8f, 1.9f),
    Blob(Color(0x06FFFFFF),  0.70f,  0.50f, 0.38f, 0.06f, 0.06f, 1.6f, 0.5f),
    Blob(Color(0x09E27D2D),  0.50f,  0.05f, 0.32f, 0.07f, 0.05f, 1.1f, 2.3f),
    Blob(Color(0x05FFFFFF), -0.10f,  0.60f, 0.29f, 0.06f, 0.07f, 0.9f, 3.1f),
)

/**
 * Animated gradient background matching Flutter's AnimatedGradientBackground:
 * 6 moving blobs (radial gradients) + static dot grid, throttled via 32s animation.
 */
@Composable
fun AnimatedGradientBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val transition = rememberInfiniteTransition(label = "bg")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 32_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "bg_t",
    )

    Box(
        modifier = modifier.drawBehind {
            // Base background
            drawRect(color = CoremeColors.backgroundBase)

            val tau = (2 * PI).toFloat()

            // Draw 6 blobs as large radial gradient circles
            for (blob in BLOBS) {
                val cx = (blob.fx + 0.5f + blob.ax * sin(t * tau * blob.sx + blob.phase)) * size.width
                val cy = (blob.fy + 0.5f + blob.ay * cos(t * tau * blob.sx + blob.phase)) * size.height
                val radius = blob.r * size.width.coerceAtLeast(size.height)

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(blob.color, Color.Transparent),
                        center = Offset(cx, cy),
                        radius = radius,
                    ),
                    radius = radius,
                    center = Offset(cx, cy),
                )
            }

            // Static dot grid — 38dp step, 0.85dp radius, 3% white opacity
            val step = 38f * density
            val dotRadius = 0.85f * density
            var x = step / 2f
            while (x < size.width) {
                var y = step / 2f
                while (y < size.height) {
                    drawCircle(
                        color = Color(0x08FFFFFF),
                        radius = dotRadius,
                        center = Offset(x, y),
                    )
                    y += step
                }
                x += step
            }
        },
        content = content,
    )
}
