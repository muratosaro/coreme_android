package app.coreme.messenger.core.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val CoremeColorScheme = darkColorScheme(
    primary = CoremeColors.accent,
    onPrimary = CoremeColors.textOnLight,
    primaryContainer = CoremeColors.accentSoft,
    onPrimaryContainer = CoremeColors.textPrimary,
    secondary = CoremeColors.textSecondary,
    onSecondary = CoremeColors.backgroundBase,
    background = CoremeColors.backgroundBase,
    onBackground = CoremeColors.textPrimary,
    surface = CoremeColors.surface,
    onSurface = CoremeColors.textPrimary,
    surfaceVariant = CoremeColors.glassThick,
    onSurfaceVariant = CoremeColors.textSecondary,
    outline = CoremeColors.glassBorder,
    outlineVariant = CoremeColors.glassBorderSubtle,
    error = CoremeColors.stateDanger,
    onError = Color.White,
    scrim = Color.Black.copy(alpha = 0.5f),
    inverseSurface = CoremeColors.textPrimary,
    inverseOnSurface = CoremeColors.backgroundBase,
    inversePrimary = CoremeColors.accentDim,
    tertiaryContainer = CoremeColors.glassRegular,
    onTertiaryContainer = CoremeColors.textPrimary,
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
