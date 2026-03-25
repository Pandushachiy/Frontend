package com.health.companion.presentation.screens.canvas

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
fun MiniMap(
    renderInfos: List<NodeRenderInfo>,
    currentScale: Float,
    currentOffset: Offset,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(width = 110.dp, height = 80.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF0A0E17).copy(alpha = 0.88f))
            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(10.dp))
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            if (renderInfos.isEmpty()) return@Canvas

            val canvasW = size.width
            val canvasH = size.height

            val allX = renderInfos.map { it.x }
            val allY = renderInfos.map { it.y }
            val minX = allX.min()
            val maxX = allX.max()
            val minY = allY.min()
            val maxY = allY.max()
            val rangeX = (maxX - minX).coerceAtLeast(1f)
            val rangeY = (maxY - minY).coerceAtLeast(1f)
            val pad = 5f

            fun toMiniX(x: Float) = pad + (x - minX) / rangeX * (canvasW - 2f * pad)
            fun toMiniY(y: Float) = pad + (y - minY) / rangeY * (canvasH - 2f * pad)

            // Nodes as tiny coloured dots
            renderInfos.forEach { node ->
                val dotR = when {
                    node.radius > 30f -> 3.5f
                    node.radius > 20f -> 2.5f
                    else -> 1.8f
                }
                drawCircle(
                    color = Color(node.colorInt).copy(alpha = if (node.isFocused) 0.85f else 0.25f),
                    radius = dotR,
                    center = Offset(toMiniX(node.x), toMiniY(node.y)),
                )
            }

            // Viewport rectangle
            val vpW = 400f / currentScale
            val vpH = 800f / currentScale
            val vpLeft = -currentOffset.x / currentScale
            val vpTop = -currentOffset.y / currentScale

            val miniLeft = toMiniX(vpLeft).coerceIn(0f, canvasW)
            val miniTop = toMiniY(vpTop).coerceIn(0f, canvasH)
            val miniRight = toMiniX(vpLeft + vpW).coerceIn(0f, canvasW)
            val miniBottom = toMiniY(vpTop + vpH).coerceIn(0f, canvasH)

            drawRect(
                color = Color(0xFF4A9EFF).copy(alpha = 0.4f),
                topLeft = Offset(miniLeft, miniTop),
                size = Size(
                    (miniRight - miniLeft).coerceAtLeast(3f),
                    (miniBottom - miniTop).coerceAtLeast(3f),
                ),
                style = Stroke(width = 1.2f),
            )
        }
    }
}
