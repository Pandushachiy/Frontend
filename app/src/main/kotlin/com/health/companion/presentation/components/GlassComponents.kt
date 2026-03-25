package com.health.companion.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import com.health.companion.presentation.theme.LocalAppTheme
import com.health.companion.presentation.theme.LocalChatBackground
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// ============================================================================
// LEGACY COMPATIBILITY LAYER
// Обёртки со старыми сигнатурами. Новый код → GlassDesignSystem.kt
// ============================================================================

/**
 * @deprecated Используй GlassCard из GlassDesignSystem.kt (shape вместо cornerRadius)
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 20.dp,
    blurRadius: Dp = 0.dp,
    backgroundColor: Color = Color.Unspecified,
    borderColor: Color = GlassColors.whiteOverlay10,
    borderWidth: Dp = 1.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    val chatBg = LocalChatBackground.current
    val appTheme = LocalAppTheme.current
    val resolvedBg = if (backgroundColor == Color.Unspecified)
        chatBg.surfaceColor.copy(alpha = 0.50f)
    else backgroundColor

    Box(
        modifier = modifier
            .clip(shape)
            .background(resolvedBg, shape)
            .background(appTheme.surfaceTint.copy(alpha = 0.06f), shape)
            .border(borderWidth, borderColor, shape)
    ) {
        content()
    }
}

/**
 * @deprecated Используй GlassCardGradient из GlassDesignSystem.kt (shape вместо cornerRadius)
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
