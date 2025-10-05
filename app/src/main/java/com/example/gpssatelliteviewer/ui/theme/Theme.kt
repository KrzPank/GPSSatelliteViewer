package com.example.gpssatelliteviewer.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    // Primary colors - using green as accent
    primary = GreenPrimary,
    onPrimary = Color.Black,
    primaryContainer = GreenDark,
    onPrimaryContainer = Color.White,
    
    // Secondary colors
    secondary = GreenLight,
    onSecondary = Color.Black,
    secondaryContainer = DarkSurfaceVariant,
    onSecondaryContainer = TextPrimary,
    
    // Tertiary colors
    tertiary = StatusWarning,
    onTertiary = Color.Black,
    
    // Background colors
    background = DarkBackground,
    onBackground = TextPrimary,
    
    // Surface colors (for cards and elevated elements)
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = CardBackground,
    onSurfaceVariant = TextSecondary,
    
    // Error colors
    error = StatusError,
    onError = Color.White,
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    
    // Outline and border colors
    outline = OutlineColor,
    outlineVariant = BorderColor
)

// Light color scheme - using same colors but inverted where appropriate
private val LightColorScheme = lightColorScheme(
    primary = GreenDark,
    onPrimary = Color.White,
    primaryContainer = GreenLight,
    onPrimaryContainer = Color.Black,
    
    secondary = GreenPrimary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE8F5E8),
    onSecondaryContainer = Color.Black,
    
    tertiary = StatusWarning,
    onTertiary = Color.White,
    
    background = Color.White,
    onBackground = Color.Black,
    
    surface = Color.White,
    onSurface = Color.Black,
    surfaceVariant = Color(0xFFF5F5F5),
    onSurfaceVariant = Color(0xFF424242),
    
    error = StatusError,
    onError = Color.White,
    
    outline = Color(0xFF757575),
    outlineVariant = Color(0xFFBDBDBD)
)

@Composable
fun GPSSatelliteViewerTheme(
    darkTheme: Boolean = true, // Default to dark theme
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
