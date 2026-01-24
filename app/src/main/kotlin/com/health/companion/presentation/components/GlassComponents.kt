package com.health.companion.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Glassmorphism Theme Colors
 */
object GlassTheme {
    // Background gradients - красивый мягкий градиент
    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF1a1a2e),  // Тёмно-фиолетовый
            Color(0xFF16213e),  // Тёмно-синий
            Color(0xFF0f3460)   // Глубокий синий
        )
    )
    
    // Альтернативный градиент с фиолетовым акцентом
    val backgroundGradientAlt = Brush.linearGradient(
        colors = listOf(
            Color(0xFF1a1a2e),
            Color(0xFF2d1b4e),  // Фиолетовый оттенок
            Color(0xFF1a1a2e)
        )
    )
    
    // Тёплый градиент для акцентов
    val warmBackgroundGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF1a1a2e),
            Color(0xFF2d2040),  // Тёплый фиолетовый
            Color(0xFF1e3a5f)   // Синий
        )
    )

    // Glass card colors
    val glassWhite = Color.White.copy(alpha = 0.08f)
    val glassWhiteHover = Color.White.copy(alpha = 0.12f)
    val glassBorder = Color.White.copy(alpha = 0.15f)
    val glassBorderLight = Color.White.copy(alpha = 0.1f)

    // Accent colors
    val accentPrimary = Color(0xFF00D9A5)      // Mint green
    val accentSecondary = Color(0xFF667eea)    // Purple blue
    val accentTertiary = Color(0xFFf093fb)     // Pink
    val accentWarm = Color(0xFFFFB347)         // Orange
    val accentCool = Color(0xFF4facfe)         // Light blue

    // Text colors
    val textPrimary = Color.White
    val textSecondary = Color.White.copy(alpha = 0.7f)
    val textTertiary = Color.White.copy(alpha = 0.5f)
    val textMuted = Color.White.copy(alpha = 0.3f)

    // Status colors
    val statusGood = Color(0xFF4ADE80)
    val statusWarning = Color(0xFFFBBF24)
    val statusError = Color(0xFFF87171)
    val statusInfo = Color(0xFF60A5FA)

    // Gradients for accents
    val accentGradient = Brush.linearGradient(
        colors = listOf(accentPrimary, accentSecondary)
    )
    
    val warmGradient = Brush.linearGradient(
        colors = listOf(Color(0xFFf093fb), Color(0xFFf5576c))
    )
    
    val coolGradient = Brush.linearGradient(
        colors = listOf(Color(0xFF4facfe), Color(0xFF00f2fe))
    )
    
    val purpleGradient = Brush.linearGradient(
        colors = listOf(Color(0xFF667eea), Color(0xFF764ba2))
    )
}

/**
 * Glass Card - основной компонент для карточек
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 20.dp,
    blurRadius: Dp = 0.dp, // 0 = без blur для производительности
    backgroundColor: Color = GlassTheme.glassWhite,
    borderColor: Color = GlassTheme.glassBorder,
    borderWidth: Dp = 1.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    
    Box(
        modifier = modifier
            .clip(shape)
            .then(
                if (blurRadius > 0.dp) Modifier.blur(blurRadius) else Modifier
            )
            .background(backgroundColor, shape)
            .border(borderWidth, borderColor, shape)
    ) {
        content()
    }
}

/**
 * Glass Card с градиентом
 */
@Composable
fun GlassCardGradient(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 20.dp,
    gradientColors: List<Color> = listOf(
        Color.White.copy(alpha = 0.1f),
        Color.White.copy(alpha = 0.05f)
    ),
    borderColor: Color = GlassTheme.glassBorder,
    content: @Composable BoxScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    
    Box(
        modifier = modifier
            .clip(shape)
            .background(
                Brush.linearGradient(gradientColors),
                shape
            )
            .border(1.dp, borderColor, shape)
    ) {
        content()
    }
}

/**
 * Accent Card - карточка с цветным акцентом
 */
@Composable
fun AccentCard(
    modifier: Modifier = Modifier,
    accentColor: Color = GlassTheme.accentPrimary,
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

/**
 * Glass Background - фон для экрана
 */
@Composable
fun GlassBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(GlassTheme.backgroundGradient)
    ) {
        content()
    }
}

/**
 * Glass Surface - поверхность с легким затемнением
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
            .background(Color(0xFF0F0F23).copy(alpha = alpha))
    ) {
        content()
    }
}
