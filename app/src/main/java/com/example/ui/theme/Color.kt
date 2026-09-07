package com.example.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Semantic security color tokens supporting both Navy-Blue Light Mode
 * and High-Contrast Dark Security Mode with accessible status colors.
 */
data class SecurityColorTokens(
    val isDark: Boolean,
    val navyPrimary: Color,
    val navySecondary: Color,
    val navyDark: Color,
    val navyContainer: Color,
    val onNavyContainer: Color,
    val trustCyan: Color,
    val trustCyanContainer: Color,
    val onTrustCyan: Color,
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val outline: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    // Status colors
    val securityGreen: Color,
    val securityGreenContainer: Color,
    val onSecurityGreen: Color,
    val securityAmber: Color,
    val securityAmberContainer: Color,
    val onSecurityAmber: Color,
    val securityRed: Color,
    val securityRedContainer: Color,
    val onSecurityRed: Color
)

// Navy-Blue Light Mode Tokens
val LightSecurityColorTokens = SecurityColorTokens(
    isDark = false,
    navyPrimary = Color(0xFF0F1E36),
    navySecondary = Color(0xFF1E3A8A),
    navyDark = Color(0xFF070D18),
    navyContainer = Color(0xFFE8EEF5),
    onNavyContainer = Color(0xFF0A1424),
    trustCyan = Color(0xFF0284C7),
    trustCyanContainer = Color(0xFFE0F2FE),
    onTrustCyan = Color(0xFF0369A1),
    background = Color(0xFFF8FAFC),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFF1F5F9),
    outline = Color(0xFFCBD5E1),
    textPrimary = Color(0xFF0F172A),
    textSecondary = Color(0xFF475569),
    textMuted = Color(0xFF64748B),
    securityGreen = Color(0xFF059669),
    securityGreenContainer = Color(0xFFD1FAE5),
    onSecurityGreen = Color(0xFF065F46),
    securityAmber = Color(0xFFD97706),
    securityAmberContainer = Color(0xFFFEF3C7),
    onSecurityAmber = Color(0xFF92400E),
    securityRed = Color(0xFFDC2626),
    securityRedContainer = Color(0xFFFEE2E2),
    onSecurityRed = Color(0xFF991B1B)
)

// High-Contrast Dark Security Mode Tokens
val DarkSecurityColorTokens = SecurityColorTokens(
    isDark = true,
    navyPrimary = Color(0xFF38BDF8), // Luminous Cyber Cyan (high contrast on dark card)
    navySecondary = Color(0xFF60A5FA), // Luminous Sky Blue
    navyDark = Color(0xFF050811), // Midnight Black Tactical Base
    navyContainer = Color(0xFF142036), // Elevated Tactical Card/Chip Surface
    onNavyContainer = Color(0xFFF1F5F9), // Crisp silver-white text
    trustCyan = Color(0xFF38BDF8),
    trustCyanContainer = Color(0xFF0C4A6E),
    onTrustCyan = Color(0xFFE0F2FE),
    background = Color(0xFF070B14), // Tactical midnight base canvas
    surface = Color(0xFF0E172A), // High-contrast navy-slate card surface
    surfaceVariant = Color(0xFF162338), // Elevated variant
    outline = Color(0xFF2A3D5D), // Crisp visible high-contrast card borders
    textPrimary = Color(0xFFF8FAFC), // Near white (15.8:1 contrast)
    textSecondary = Color(0xFFCBD5E1), // Crisp slate (9.5:1 contrast)
    textMuted = Color(0xFF94A3B8), // Muted slate
    securityGreen = Color(0xFF34D399), // Luminous emerald (11.5:1 contrast on dark canvas)
    securityGreenContainer = Color(0xFF064E3B), // Deep emerald container
    onSecurityGreen = Color(0xFFA7F3D0), // Luminous mint text (9.2:1 contrast on container)
    securityAmber = Color(0xFFFBBF24), // Luminous gold (12.8:1 contrast on dark canvas)
    securityAmberContainer = Color(0xFF78350F), // Deep amber container
    onSecurityAmber = Color(0xFFFEF3C7), // Luminous gold text (9.5:1 contrast on container)
    securityRed = Color(0xFFF87171), // Luminous coral red (8.8:1 contrast on dark canvas)
    securityRedContainer = Color(0xFF7F1D1D), // Deep crimson container
    onSecurityRed = Color(0xFFFECACA) // Luminous rose text (8.6:1 contrast on container)
)

val LocalSecurityColors = staticCompositionLocalOf { LightSecurityColorTokens }

// Dynamic theme-aware accessors for seamless Composable consumption
val NavyPrimary: Color @Composable @ReadOnlyComposable get() = LocalSecurityColors.current.navyPrimary
val NavySecondary: Color @Composable @ReadOnlyComposable get() = LocalSecurityColors.current.navySecondary
val NavyDark: Color @Composable @ReadOnlyComposable get() = LocalSecurityColors.current.navyDark
val NavyContainer: Color @Composable @ReadOnlyComposable get() = LocalSecurityColors.current.navyContainer
val OnNavyContainer: Color @Composable @ReadOnlyComposable get() = LocalSecurityColors.current.onNavyContainer

val TrustCyan: Color @Composable @ReadOnlyComposable get() = LocalSecurityColors.current.trustCyan
val TrustCyanContainer: Color @Composable @ReadOnlyComposable get() = LocalSecurityColors.current.trustCyanContainer
val OnTrustCyan: Color @Composable @ReadOnlyComposable get() = LocalSecurityColors.current.onTrustCyan

val BackgroundLight: Color @Composable @ReadOnlyComposable get() = LocalSecurityColors.current.background
val SurfaceLight: Color @Composable @ReadOnlyComposable get() = LocalSecurityColors.current.surface
val SurfaceVariantLight: Color @Composable @ReadOnlyComposable get() = LocalSecurityColors.current.surfaceVariant
val OutlineLight: Color @Composable @ReadOnlyComposable get() = LocalSecurityColors.current.outline
val TextPrimary: Color @Composable @ReadOnlyComposable get() = LocalSecurityColors.current.textPrimary
val TextSecondary: Color @Composable @ReadOnlyComposable get() = LocalSecurityColors.current.textSecondary
val TextMuted: Color @Composable @ReadOnlyComposable get() = LocalSecurityColors.current.textMuted

// Status Tokens
val SecurityGreen: Color @Composable @ReadOnlyComposable get() = LocalSecurityColors.current.securityGreen
val SecurityGreenContainer: Color @Composable @ReadOnlyComposable get() = LocalSecurityColors.current.securityGreenContainer
val OnSecurityGreen: Color @Composable @ReadOnlyComposable get() = LocalSecurityColors.current.onSecurityGreen

val SecurityAmber: Color @Composable @ReadOnlyComposable get() = LocalSecurityColors.current.securityAmber
val SecurityAmberContainer: Color @Composable @ReadOnlyComposable get() = LocalSecurityColors.current.securityAmberContainer
val OnSecurityAmber: Color @Composable @ReadOnlyComposable get() = LocalSecurityColors.current.onSecurityAmber

val SecurityRed: Color @Composable @ReadOnlyComposable get() = LocalSecurityColors.current.securityRed
val SecurityRedContainer: Color @Composable @ReadOnlyComposable get() = LocalSecurityColors.current.securityRedContainer
val OnSecurityRed: Color @Composable @ReadOnlyComposable get() = LocalSecurityColors.current.onSecurityRed


