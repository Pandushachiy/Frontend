package com.health.companion.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// ============================================================================
// LEGACY COMPATIBILITY LAYER
// Для обратной совместимости со старым кодом
// Новый код должен использовать GlassDesignSystem.kt напрямую
// ============================================================================

/**
 * @deprecated Используй GlassColors, GlassGradients из GlassDesignSystem.kt
 */
object GlassTheme {
    // Background gradients — теперь используют GlassGradients
    val backgroundGradient = GlassGradients.backgroundVertical
    val backgroundGradientAlt = GlassGradients.background
    val warmBackgroundGradient = GlassGradients.warm

    // Glass card colors — более плотный фон (меньше прозрачности)
    val glassWhite = Color(0xFF1A1F2E).copy(alpha = 0.85f) // Тёмный фон 85%
    val glassWhiteHover = Color(0xFF252B3D).copy(alpha = 0.90f)
    val glassBorder = Color.White.copy(alpha = 0.15f)
    val glassBorderLight = Color.White.copy(alpha = 0.08f)

    // Accent colors
    val accentPrimary = GlassColors.mint
    val accentSecondary = GlassColors.accent
    val accentTertiary = Color(0xFFf093fb)
    val accentWarm = GlassColors.orange
    val accentCool = Color(0xFF4facfe)

    // Text colors
    val textPrimary = GlassColors.textPrimary
    val textSecondary = GlassColors.textSecondary
    val textTertiary = GlassColors.textTertiary
    val textMuted = GlassColors.textMuted

    // Status colors
    val statusGood = GlassColors.success
    val statusWarning = GlassColors.warning
    val statusError = GlassColors.error
    val statusInfo = GlassColors.info

    // Gradients for accents
    val accentGradient = GlassGradients.accent
    val warmGradient = GlassGradients.warm
    val coolGradient = GlassGradients.cool
    val purpleGradient = GlassGradients.purple
}

/**
 * @deprecated Используй GlassCard из GlassDesignSystem.kt
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 20.dp,
    blurRadius: Dp = 0.dp,
    backgroundColor: Color = GlassColors.surface.copy(alpha = 0.5f),
    borderColor: Color = GlassColors.whiteOverlay10,
    borderWidth: Dp = 1.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    
    Box(
        modifier = modifier
            .clip(shape)
            .background(backgroundColor, shape)
            .border(borderWidth, borderColor, shape)
    ) {
        content()
    }
}

/**
 * @deprecated Используй GlassCardGradient из GlassDesignSystem.kt
 */
@Composable
fun GlassCardGradient(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 20.dp,
    gradientColors: List<Color> = listOf(
        Color.White.copy(alpha = 0.1f),
        Color.White.copy(alpha = 0.05f)
    ),
    borderColor: Color = GlassColors.whiteOverlay10,
    content: @Composable BoxScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    
    Box(
        modifier = modifier
            .clip(shape)
            .background(Brush.linearGradient(gradientColors), shape)
            .border(1.dp, borderColor, shape)
    ) {
        content()
    }
}

/**
 * @deprecated Используй GlassCard из GlassDesignSystem.kt с accent цветом
 */
@Composable
fun AccentCard(
    modifier: Modifier = Modifier,
    accentColor: Color = GlassColors.mint,
    cornerRadius: Dp = 20.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    
    Box(
        modifier = modifier
            .clip(shape)
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        accentColor.copy(alpha = 0.2f),
                        accentColor.copy(alpha = 0.05f)
                    )
                ),
                shape
            )
            .border(1.dp, accentColor.copy(alpha = 0.3f), shape)
    ) {
        content()
    }
}

// GlassBackground перенесён в GlassDesignSystem.kt

/**
 * @deprecated
 */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    alpha: Float = 0.5f,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(GlassColors.background.copy(alpha = alpha))
    ) {
        content()
    }
}
