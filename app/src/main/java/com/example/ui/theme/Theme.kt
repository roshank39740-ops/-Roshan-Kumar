package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = ElectricBlue,
    onPrimary = DeepNavyBg,
    primaryContainer = CardNavyBorder,
    onPrimaryContainer = ElectricBlue,
    secondary = AccentIndigo,
    onSecondary = Color.White,
    secondaryContainer = SurfaceElevated,
    onSecondaryContainer = AccentIndigo,
    tertiary = RecordingRed,
    onTertiary = Color.White,
    background = DeepNavyBg,
    onBackground = TextPrimary,
    surface = CardNavySurface,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceElevated,
    onSurfaceVariant = TextSecondary,
    outline = CardNavyBorder,
    error = RecordingRed,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = ElectricBlueVariant,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0F2FE),
    onPrimaryContainer = ElectricBlueVariant,
    secondary = AccentIndigo,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFEEF2FF),
    onSecondaryContainer = AccentIndigo,
    tertiary = RecordingRed,
    onTertiary = Color.White,
    background = LightNavyBg,
    onBackground = LightTextPrimary,
    surface = LightCardSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = Color(0xFFF8FAFC),
    onSurfaceVariant = LightTextSecondary,
    outline = LightCardBorder,
    error = RecordingRed,
    onError = Color.White
)

@Composable
fun RecordedAiTheme(
    darkTheme: Boolean = true, // Default to premium dark navy for professional voice recording
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
