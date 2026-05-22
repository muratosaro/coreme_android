package app.coreme.messenger.core.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val CoremeColorScheme = darkColorScheme(
    primary = CoremeColors.hermesOrange,
    onPrimary = Color.White,
    primaryContainer = CoremeColors.hermesOrangeMuted,
    onPrimaryContainer = Color.White,
    secondary = CoremeColors.textSecondary,
    onSecondary = CoremeColors.backgroundBase,
    background = CoremeColors.backgroundBase,
    onBackground = CoremeColors.textPrimary,
    surface = CoremeColors.glassMedium,
    onSurface = CoremeColors.textPrimary,
    surfaceVariant = CoremeColors.glassThick,
    onSurfaceVariant = CoremeColors.textSecondary,
    outline = CoremeColors.glassBorder,
    error = CoremeColors.dangerRed,
    onError = Color.White,
    scrim = Color.Black.copy(alpha = 0.5f),
)

@Composable
fun CoremeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = CoremeColorScheme,
        typography = CoremeTypography,
        shapes = CoremeShapes,
        content = content,
    )
}
