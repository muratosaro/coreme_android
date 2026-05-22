package app.coreme.messenger.core.ui.components

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.coreme.messenger.core.ui.theme.CoremeColors

/**
 * Glass surface modifier.
 * API 31+ (Android 12): real background blur via Modifier.blur.
 * API 26-30: static semi-transparent overlay (no GPU blur).
 */
fun Modifier.glassSurface(alpha: Float = 0.06f): Modifier {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        this
            .background(Color.White.copy(alpha = alpha))
            .blur(20.dp)
    } else {
        this.background(CoremeColors.backgroundBase.copy(alpha = 0.85f))
    }
}

/**
 * Thicker glass variant for cards and bottom sheets.
 */
fun Modifier.glassCard(): Modifier = glassSurface(alpha = 0.08f)

/**
 * Thin glass variant for subtle overlays.
 */
fun Modifier.glassThin(): Modifier = glassSurface(alpha = 0.04f)
