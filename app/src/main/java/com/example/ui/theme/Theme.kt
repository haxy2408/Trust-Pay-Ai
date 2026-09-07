package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

private val HighContrastDarkColorScheme =
  darkColorScheme(
    primary = DarkSecurityColorTokens.navyPrimary,
    onPrimary = Color(0xFF031E38),
    primaryContainer = DarkSecurityColorTokens.trustCyanContainer,
    onPrimaryContainer = DarkSecurityColorTokens.onTrustCyan,
    secondary = DarkSecurityColorTokens.navySecondary,
    onSecondary = Color(0xFF0F172A),
    secondaryContainer = DarkSecurityColorTokens.navyContainer,
    onSecondaryContainer = DarkSecurityColorTokens.onNavyContainer,
    tertiary = DarkSecurityColorTokens.securityGreen,
    onTertiary = DarkSecurityColorTokens.onSecurityGreen,
    background = DarkSecurityColorTokens.background,
    onBackground = DarkSecurityColorTokens.textPrimary,
    surface = DarkSecurityColorTokens.surface,
    onSurface = DarkSecurityColorTokens.textPrimary,
    surfaceVariant = DarkSecurityColorTokens.surfaceVariant,
    onSurfaceVariant = DarkSecurityColorTokens.textSecondary,
    outline = DarkSecurityColorTokens.outline,
    error = DarkSecurityColorTokens.securityRed,
    onError = Color(0xFF450A0A),
    errorContainer = DarkSecurityColorTokens.securityRedContainer,
    onErrorContainer = DarkSecurityColorTokens.onSecurityRed
  )

private val NavyLightColorScheme =
  lightColorScheme(
    primary = LightSecurityColorTokens.navyPrimary,
    onPrimary = Color.White,
    primaryContainer = LightSecurityColorTokens.navyContainer,
    onPrimaryContainer = LightSecurityColorTokens.onNavyContainer,
    secondary = LightSecurityColorTokens.navySecondary,
    onSecondary = Color.White,
    secondaryContainer = LightSecurityColorTokens.trustCyanContainer,
    onSecondaryContainer = LightSecurityColorTokens.onTrustCyan,
    tertiary = LightSecurityColorTokens.trustCyan,
    onTertiary = Color.White,
    background = LightSecurityColorTokens.background,
    onBackground = LightSecurityColorTokens.textPrimary,
    surface = LightSecurityColorTokens.surface,
    onSurface = LightSecurityColorTokens.textPrimary,
    surfaceVariant = LightSecurityColorTokens.surfaceVariant,
    onSurfaceVariant = LightSecurityColorTokens.textSecondary,
    outline = LightSecurityColorTokens.outline,
    error = LightSecurityColorTokens.securityRed,
    onError = Color.White,
    errorContainer = LightSecurityColorTokens.securityRedContainer,
    onErrorContainer = LightSecurityColorTokens.onSecurityRed
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = false,
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val securityColors = if (darkTheme) DarkSecurityColorTokens else LightSecurityColorTokens
  val colorScheme = if (darkTheme) HighContrastDarkColorScheme else NavyLightColorScheme

  CompositionLocalProvider(LocalSecurityColors provides securityColors) {
    MaterialTheme(
      colorScheme = colorScheme,
      typography = Typography,
      content = content
    )
  }
}

object SecurityTheme {
  val colors: SecurityColorTokens
    @Composable
    @ReadOnlyComposable
    get() = LocalSecurityColors.current
}

