package com.example.trustpay.ui.theme

import androidx.compose.ui.graphics.Color

// Light Fintech Canvas & Surfaces
val LightBackground = Color(0xFFF8FAFC)    // Crisp clean canvas
val SurfaceBg = LightBackground            // Alias
val SurfaceWhite = Color(0xFFFFFFFF)       // Pure white cards
val SurfaceVariant = Color(0xFFF1F5F9)     // Soft slate secondary surface
val CardBorder = Color(0xFFE2E8F0)         // Subtle crisp border
val CardBorderLight = Color(0xFFF1F5F9)

// Text Hierarchy (Dark Navy / Charcoal & Soft Slate)
val CharcoalText = Color(0xFF0F172A)       // High contrast primary text
val SlateText = Color(0xFF475569)          // Soft gray secondary body text
val SlateMuted = Color(0xFF64748B)         // Tertiary / caption text
val SlateLight = Color(0xFF475569)         // High-contrast secondary text in light theme
val WhitePure = Color(0xFFFFFFFF)

// Primary Fintech Brand Blue
val TrustBlue = Color(0xFF1D4ED8)          // Deep professional royal blue
val TrustBlueLight = Color(0xFF2563EB)     // Modern fintech primary blue
val TrustBlueContainer = Color(0xFFEFF6FF) // Subtle light blue container
val ElectricBlue = Color(0xFF2563EB)       // Modern royal blue

// Backward-compatibility aliases for smooth light-theme transition
val Navy900 = LightBackground             // Main background is light
val Navy800 = SurfaceWhite                // Cards are crisp white
val Navy700 = SurfaceVariant              // Sub-surfaces are soft slate
val Navy600 = Color(0xFFE2E8F0)           // Subtle divider

val TrustCyan = Color(0xFF0284C7)         // Professional brand accent
val TrustCyanDark = Color(0xFF0369A1)
val BlueGlow = Color(0xFF93C5FD)

// Verification & Security Signals
val SecurityGreen = Color(0xFF059669)     // Success verification green
val SecurityGreenBg = Color(0xFFECFDF5)   // Light tint background for badges
val SecurityGreenBorder = Color(0xFFA7F3D0)

val SecurityAmber = Color(0xFFD97706)     // Warning amber
val SecurityAmberBg = Color(0xFFFFFBEB)   // Light tint warning background
val SecurityAmberBorder = Color(0xFFFDE68A)

val SecurityRed = Color(0xFFDC2626)       // Threat / failure red
val SecurityRedBg = Color(0xFFFEF2F2)     // Light tint threat background
val SecurityRedBorder = Color(0xFFFECACA)


