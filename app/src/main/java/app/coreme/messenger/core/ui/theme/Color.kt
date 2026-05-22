package app.coreme.messenger.core.ui.theme

import androidx.compose.ui.graphics.Color

object CoremeColors {
    // Backgrounds
    val backgroundBase = Color(0xFF1A1A20)
    val backgroundCenter = Color(0xFF1E1E26)
    val backgroundDeep = Color(0xFF14141A)
    val surface = Color(0xFF1E1E26)
    val surfaceLight = Color(0xFF24242E)

    // Accent — Hermès Orange
    val accent = Color(0xFFE27D2D)
    val accentDim = Color(0xFFB66425)
    val accentSoft = Color(0x33E27D2D)
    val accentGlow = Color(0x20E27D2D)
    val accentInteractive = Color(0x14E27D2D)

    // Text
    val textPrimary = Color(0xFFF5F5F7)
    val textSecondary = Color(0xFFA1A1A8)
    val textTertiary = Color(0xFF6E6E76)
    val textDisabled = Color(0xFF3E3E46)
    val textOnLight = Color(0xFF0D0D12)
    val textHint = Color(0xFF4E4E56)

    // Glass surfaces — 6 levels
    val glassUltraThin = Color(0x05FFFFFF)
    val glassThin = Color(0x0CFFFFFF)
    val glassRegular = Color(0x14FFFFFF)
    val glassThick = Color(0x1FFFFFFF)
    val glassHeavy = Color(0x2EFFFFFF)
    val glassFrosted = Color(0x40FFFFFF)

    // Glass borders
    val glassBorderSubtle = Color(0x0FFFFFFF)
    val glassBorder = Color(0x1AFFFFFF)
    val glassBorderStrong = Color(0x33FFFFFF)

    // Glass highlights
    val glassHighlight = Color(0x1AFFFFFF)
    val glassHighlightTop = Color(0x28FFFFFF)
    val glassShadow = Color(0x66000000)

    // Status
    val stateOnline = Color(0xFF5A8A6F)
    val stateDanger = Color(0xFFE5A5A5)
    val danger = Color(0xFFE5A5A5)
    val blue = Color(0xFF3D9BE9)

    // Backward-compat aliases (used across existing files)
    val hermesOrange = accent
    val hermesOrangeMuted = accentDim
    val onlineGreen = stateOnline
    val dangerRed = stateDanger
    val glassMedium = glassRegular
    val glassSurfaceMid = glassThick
    val textPlaceholder = textHint
}
