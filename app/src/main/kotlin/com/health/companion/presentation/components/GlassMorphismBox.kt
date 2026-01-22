package com.health.companion.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun GlassMorphismBox(
    modifier: Modifier = Modifier,
    alpha: Float = 0.1f,
    borderAlpha: Float = 0.2f,
    cornerRadius: Dp = 16.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    
    Box(
        modifier = modifier
            .clip(shape)
            .background(
                color = Color.White.copy(alpha = alpha),
                shape = shape
            )
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = borderAlpha),
                shape = shape
            )
    ) {
        content()
    }
}

@Composable
fun GlassMorphismCard(
    modifier: Modifier = Modifier,
    alpha: Float = 0.12f,
    content: @Composable BoxScope.() -> Unit
) {
    GlassMorphismBox(
        modifier = modifier,
        alpha = alpha,
        borderAlpha = 0.25f,
        cornerRadius = 20.dp,
        content = content
    )
}
