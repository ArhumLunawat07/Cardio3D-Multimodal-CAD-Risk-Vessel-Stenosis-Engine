package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = ArterialRedPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF4A0F1E),
    onPrimaryContainer = Color(0xFFFFD9DF),
    secondary = VenousCyan,
    onSecondary = Color(0xFF00363D),
    secondaryContainer = Color(0xFF004F59),
    onSecondaryContainer = Color(0xFFBCEBFF),
    tertiary = PerfusionHealthyEmerald,
    onTertiary = Color(0xFF003919),
    background = CardioDarkBackground,
    onBackground = TextPrimaryDark,
    surface = CardioSurfaceDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = CardioSurfaceVariantDark,
    onSurfaceVariant = TextSecondaryDark,
    outline = CardioBorderDark,
    error = ArterialRedGlow,
    onError = Color.White
)

private val LightColorScheme = darkColorScheme(
    // We enforce the sleek clinical dark visualization theme for high-fidelity 3D contrast
    primary = ArterialRedPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF4A0F1E),
    onPrimaryContainer = Color(0xFFFFD9DF),
    secondary = VenousCyan,
    onSecondary = Color(0xFF00363D),
    tertiary = PerfusionHealthyEmerald,
    background = CardioDarkBackground,
    onBackground = TextPrimaryDark,
    surface = CardioSurfaceDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = CardioSurfaceVariantDark,
    onSurfaceVariant = TextSecondaryDark,
    outline = CardioBorderDark,
    error = ArterialRedGlow,
    onError = Color.White
)

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
