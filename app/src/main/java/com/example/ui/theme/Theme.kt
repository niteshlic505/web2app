package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val IndianFuturisticColorScheme = darkColorScheme(
    primary = IndianSaffron,
    onPrimary = Color(0xFF1A0F00),
    primaryContainer = Color(0xFF663300),
    onPrimaryContainer = Color(0xFFFFE5CC),
    secondary = AshokaCyan,
    onSecondary = Color(0xFF021324),
    secondaryContainer = Color(0xFF0E2954),
    onSecondaryContainer = Color(0xFFE0F2FE),
    tertiary = IndiaGreenLight,
    onTertiary = Color(0xFF002110),
    tertiaryContainer = Color(0xFF08421B),
    onTertiaryContainer = Color(0xFFD1FAE5),
    background = DarkBackground,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = DarkCardBorder,
    outlineVariant = DividerDark,
    error = CrimsonError,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = IndianFuturisticColorScheme,
        typography = Typography,
        content = content
    )
}
