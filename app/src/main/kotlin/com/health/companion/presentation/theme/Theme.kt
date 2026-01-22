package com.health.companion.presentation.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// === NEW MODERN DESIGN PALETTE ===
// Based on screenshot 2 - Vibrant gradients with purple/teal theme

// Primary - Vibrant Teal/Cyan
val PrimaryTeal = Color(0xFF00D4AA)
val PrimaryTealDark = Color(0xFF00A88A)
val PrimaryTealLight = Color(0xFF4DFFCC)

// Secondary - Purple/Violet accent
val SecondaryPurple = Color(0xFF8B5CF6)
val SecondaryPurpleDark = Color(0xFF7C3AED)
val SecondaryPurpleLight = Color(0xFFA78BFA)

// Tertiary - Orange/Coral for accents
val TertiaryOrange = Color(0xFFFF7F50)
val TertiaryOrangeDark = Color(0xFFFF6B35)

// Backgrounds - Deep dark for modern look
val BackgroundDark = Color(0xFF0F0F23)
val BackgroundDarker = Color(0xFF0A0A1A)
val SurfaceDark = Color(0xFF1A1A2E)
val SurfaceVariantDark = Color(0xFF252542)
val SurfaceContainerDark = Color(0xFF16162A)
val SurfaceContainerHighDark = Color(0xFF2A2A4A)

// Light theme backgrounds
val BackgroundLight = Color(0xFFF8FAFC)
val SurfaceLight = Color(0xFFFFFFFF)
val SurfaceVariantLight = Color(0xFFF1F5F9)
val SurfaceContainerLight = Color(0xFFE2E8F0)

// Text
val OnBackgroundDark = Color(0xFFF1F5F9)
val OnBackgroundLight = Color(0xFF1E293B)
val OnSurfaceVariantDark = Color(0xFF94A3B8)
val OnSurfaceVariantLight = Color(0xFF64748B)

// Status Colors
val Success = Color(0xFF10B981)
val Warning = Color(0xFFF59E0B)
val ErrorColor = Color(0xFFEF4444)
val Info = Color(0xFF3B82F6)

// Chat bubble colors
val UserBubble = Color(0xFF00D4AA)
val AssistantBubble = Color(0xFF2A2A4A)
val UserBubbleLight = Color(0xFF00A88A)
val AssistantBubbleLight = Color(0xFFE2E8F0)

// Gradient presets for cards
val GradientTealPurple = Brush.linearGradient(
    colors = listOf(PrimaryTeal, SecondaryPurple)
)

val GradientPurpleOrange = Brush.linearGradient(
    colors = listOf(SecondaryPurple, TertiaryOrange)
)

val GradientTealBlue = Brush.linearGradient(
    colors = listOf(PrimaryTeal, Info)
)

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryTeal,
    onPrimary = BackgroundDark,
    primaryContainer = PrimaryTealDark,
    onPrimaryContainer = OnBackgroundDark,
    
    secondary = SecondaryPurple,
    onSecondary = OnBackgroundDark,
    secondaryContainer = SecondaryPurpleDark,
    onSecondaryContainer = OnBackgroundDark,
    
    tertiary = TertiaryOrange,
    onTertiary = OnBackgroundDark,
    tertiaryContainer = TertiaryOrangeDark,
    onTertiaryContainer = OnBackgroundDark,
    
    background = BackgroundDark,
    onBackground = OnBackgroundDark,
    
    surface = SurfaceDark,
    onSurface = OnBackgroundDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    surfaceContainerLowest = BackgroundDarker,
    surfaceContainerLow = SurfaceContainerDark,
    surfaceContainer = SurfaceDark,
    surfaceContainerHigh = SurfaceContainerHighDark,
    surfaceContainerHighest = Color(0xFF3A3A5A),
    
    error = ErrorColor,
    onError = OnBackgroundDark,
    errorContainer = ErrorColor.copy(alpha = 0.2f),
    onErrorContainer = ErrorColor,
    
    outline = Color(0xFF4A4A6A),
    outlineVariant = Color(0xFF3A3A5A)
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryTealDark,
    onPrimary = Color.White,
    primaryContainer = PrimaryTealLight.copy(alpha = 0.3f),
    onPrimaryContainer = PrimaryTealDark,
    
    secondary = SecondaryPurpleDark,
    onSecondary = Color.White,
    secondaryContainer = SecondaryPurpleLight.copy(alpha = 0.3f),
    onSecondaryContainer = SecondaryPurpleDark,
    
    tertiary = TertiaryOrangeDark,
    onTertiary = Color.White,
    
    background = BackgroundLight,
    onBackground = OnBackgroundLight,
    
    surface = SurfaceLight,
    onSurface = OnBackgroundLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF8FAFC),
    surfaceContainer = SurfaceVariantLight,
    surfaceContainerHigh = SurfaceContainerLight,
    surfaceContainerHighest = Color(0xFFCBD5E1),
    
    error = ErrorColor,
    onError = Color.White,
    errorContainer = ErrorColor.copy(alpha = 0.1f),
    onErrorContainer = ErrorColor,
    
    outline = Color(0xFFCBD5E1),
    outlineVariant = Color(0xFFE2E8F0)
)

@Composable
fun HealthCompanionTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.surfaceContainerHigh.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
