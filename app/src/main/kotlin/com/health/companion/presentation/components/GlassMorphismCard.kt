package com.health.companion.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A card with glass morphism effect - semi-transparent with blur and border
 */
@Composable
fun GlassMorphismCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 16.dp,
    alpha: Float = 0.1f,
    borderAlpha: Float = 0.2f,
    content: @Composable BoxScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    
    Box(
        modifier = modifier
            .clip(shape)
            .background(
                color = MaterialTheme.colorScheme.surface.copy(alpha = alpha),
                shape = shape
            )
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = borderAlpha),
                shape = shape
            ),
        content = content
    )
}

/**
 * A box with glass morphism effect - more customizable version
 */
@Composable
fun GlassMorphismBox(
    modifier: Modifier = Modifier,
    alpha: Float = 0.1f,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .background(
                color = Color.White.copy(alpha = alpha),
                shape = RoundedCornerShape(16.dp)
            )
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.2f),
                shape = RoundedCornerShape(16.dp)
            )
    ) {
        content()
    }
}

/**
 * A gradient glass card with customizable gradient colors
 */
@Composable
fun GradientGlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 16.dp,
    gradientColors: List<Color> = listOf(
        Color.White.copy(alpha = 0.15f),
        Color.White.copy(alpha = 0.05f)
    ),
    content: @Composable BoxScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    
    Box(
        modifier = modifier
            .clip(shape)
            .background(
                brush = Brush.verticalGradient(colors = gradientColors),
                shape = shape
            )
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.3f),
                        Color.White.copy(alpha = 0.1f)
                    )
                ),
                shape = shape
            ),
        content = content
    )
}
