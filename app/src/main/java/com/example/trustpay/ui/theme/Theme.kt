package com.example.trustpay.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val TrustPayLightColorScheme = lightColorScheme(
    primary = TrustBlue,
    onPrimary = WhitePure,
    primaryContainer = TrustBlueContainer,
    onPrimaryContainer = TrustBlue,
    secondary = ElectricBlue,
    onSecondary = WhitePure,
    background = LightBackground,
    onBackground = CharcoalText,
    surface = SurfaceWhite,
    onSurface = CharcoalText,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = SlateText,
    outline = CardBorder,
    error = SecurityRed,
    onError = WhitePure
)

@Composable
fun TrustPayTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = TrustPayLightColorScheme,
        content = content
    )
}
