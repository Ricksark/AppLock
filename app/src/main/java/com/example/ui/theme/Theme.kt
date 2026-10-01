package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = CyberCyan,
    onPrimary = ShieldNavy,
    primaryContainer = DeepCobalt,
    onPrimaryContainer = Color.White,
    secondary = DeepCobalt,
    onSecondary = Color.White,
    secondaryContainer = ContainerNavy,
    onSecondaryContainer = CyberCyan,
    tertiary = SafeEmerald,
    onTertiary = Color.White,
    tertiaryContainer = EmeraldContainer,
    onTertiaryContainer = SafeEmerald,
    error = ThreatCrimson,
    onError = Color.White,
    errorContainer = CrimsonContainer,
    onErrorContainer = ThreatCrimson,
    background = ShieldNavy,
    onBackground = TextPrimary,
    surface = CardSlate,
    onSurface = TextPrimary,
    surfaceVariant = ContainerNavy,
    onSurfaceVariant = TextSecondary,
    outline = BorderSlate
)

private val LightColorScheme = DarkColorScheme // Security vaults look best in dark mode

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
