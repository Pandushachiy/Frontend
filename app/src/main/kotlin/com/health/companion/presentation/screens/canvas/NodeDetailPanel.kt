package com.health.companion.presentation.screens.canvas

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.health.companion.data.canvas.CanvasNode

@Composable
fun NodeDetailPanel(
    node: CanvasNode,
    onAskAgent: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val nodeColor = remember(node.color) {
        try { Color(android.graphics.Color.parseColor(node.color)) }
        catch (e: Exception) { Color(0xFF4A9EFF) }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1A1F3A).copy(alpha = 0.97f),
                        Color(0xFF0F1226).copy(alpha = 0.97f)
                    )
                )
            )
            .border(
                1.dp,
                Brush.horizontalGradient(
                    listOf(nodeColor.copy(alpha = 0.5f), Color.White.copy(alpha = 0.1f))
                ),
                RoundedCornerShape(20.dp)
            )
            .padding(16.dp)
    ) {
        Column {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Category icon circle — no emoji
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(nodeColor.copy(alpha = 0.22f))
                        .border(1.5.dp, nodeColor.copy(alpha = 0.7f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Canvas(modifier = Modifier.size(24.dp)) {
                        drawCategoryIcon(
                            cluster  = node.cluster,
                            center   = Offset(size.width / 2f, size.height / 2f),
                            iconR    = size.width * 0.38f,
                            strokeW  = size.width * 0.075f,
                        )
                    }
                }
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = node.name,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "${node.cluster} · ${node.entityType}",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 11.sp
                    )
                }
                if (node.isHot) {
                    Box(
                        Modifier
                            .background(Color(0xFFFF6B6B).copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text("🔥 Активно", color = Color(0xFFFF6B6B), fontSize = 10.sp)
                    }
                    Spacer(Modifier.width(6.dp))
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Закрыть",
                        tint = Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Description
            node.description?.let { desc ->
                if (desc.isNotBlank()) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = desc,
                        color = Color.White.copy(alpha = 0.72f),
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        maxLines = 3
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Stats row
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                StatItem(
                    label = "Важность",
                    value = node.importance,
                    color = nodeColor,
                    modifier = Modifier.weight(1f)
                )
                StatItem(
                    label = "Активность",
                    value = node.activityScore,
                    color = Color(0xFF00D4AA),
                    modifier = Modifier.weight(1f)
                )
                StatItem(
                    label = "Уверенность",
                    value = node.confidence,
                    color = Color(0xFF8B5CF6),
                    modifier = Modifier.weight(1f)
                )
            }

            // Annotations
            if (node.annotations.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    node.annotations.take(3).forEach { ann ->
                        val annColor = try {
                            Color(android.graphics.Color.parseColor(ann.color))
                        } catch (e: Exception) { Color(0xFFFFD166) }
                        Box(
                            Modifier
                                .background(annColor.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                .border(1.dp, annColor.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = ann.text.take(30),
                                color = annColor,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // Action button
            FilledTonalButton(
                onClick = onAskAgent,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = nodeColor.copy(alpha = 0.22f),
                    contentColor = nodeColor
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    "Спросить агента про ${node.name.take(20)}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun StatItem(
    label: String,
    value: Float,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.45f),
            fontSize = 9.sp
        )
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { value.coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp)),
            color = color,
            trackColor = Color.White.copy(alpha = 0.1f)
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = "${(value * 100).toInt()}%",
            color = color,
            fontSize = 9.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
