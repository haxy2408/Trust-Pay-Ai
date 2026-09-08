package com.example.trustpay.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val TrustPayColorScheme = darkColorScheme(
    primary = TrustCyan,
    onPrimary = Navy900,
    primaryContainer = Navy700,
    onPrimaryContainer = TrustCyan,
    secondary = ElectricBlue,
    onSecondary = WhitePure,
    background = Navy900,
    onBackground = WhitePure,
    surface = Navy800,
    onSurface = WhitePure,
    surfaceVariant = Navy700,
    onSurfaceVariant = SlateLight,
    error = SecurityRed,
    onError = WhitePure
)

@Composable
fun TrustPayTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = TrustPayColorScheme,
        content = content
    )
}
