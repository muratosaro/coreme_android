package app.coreme.messenger.core.ui.theme

import androidx.compose.ui.graphics.Color

object CoremeColors {
    // Background — radial gradient base
    val backgroundBase = Color(0xFF1A1A20)
    val backgroundCenter = Color(0xFF1E1E26)
    val backgroundDeep = Color(0xFF14141A)

    // Hermès Orange — signature accent (MINIMAL USE: active tab, send button, read receipts, typing)
    val hermesOrange = Color(0xFFE27D2D)
    val hermesOrangeMuted = Color(0xFFB8632D)

    // Status colors
    val onlineGreen = Color(0xFF5A8A6F)
    val dangerRed = Color(0xFFE5A5A5)

    // Glass surfaces (translucent overlays)
    val glassThick = Color.White.copy(alpha = 0.08f)
    val glassMedium = Color.White.copy(alpha = 0.06f)
    val glassThin = Color.White.copy(alpha = 0.04f)
    val glassBorder = Color.White.copy(alpha = 0.06f)

    // Text hierarchy
    val textPrimary = Color(0xFFFFFFFF)
    val textSecondary = Color(0xFFBBBBBB)
    val textTertiary = Color(0xFF7A7A82)
    val textPlaceholder = Color(0xFF5A5A60)

    // Message bubbles
    val bubbleMine = Color(0xFF3D2E25)     // Dark brown — own messages
    val bubbleOthers = Color.White.copy(alpha = 0.06f) // Glass — others' messages
}
