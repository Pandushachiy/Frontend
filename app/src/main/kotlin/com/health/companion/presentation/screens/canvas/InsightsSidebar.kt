package com.health.companion.presentation.screens.canvas

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.health.companion.data.canvas.CanvasInsight

private fun insightTypeConfig(type: String): Pair<String, Color> = when (type) {
    "pattern"    -> "🔄" to Color(0xFF6366F1)
    "connection" -> "🔗" to Color(0xFF00D4AA)
    "gap"        -> "⚡" to Color(0xFFFF9F43)
    "trend"      -> "📈" to Color(0xFF8B5CF6)
    else         -> "💡" to Color(0xFF60A5FA)
}

@Composable
fun InsightsSidebar(
    insights: List<CanvasInsight>,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        AnimatedVisibility(
            visible = expanded,
            enter = expandHorizontally(tween(280), expandFrom = Alignment.End),
            exit = shrinkHorizontally(tween(200), shrinkTowards = Alignment.End)
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 200.dp)
                    .fillMaxHeight(0.6f)
                    .clip(RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0xFF1A1F3A).copy(alpha = 0.96f),
                                Color(0xFF0F1226).copy(alpha = 0.96f)
                            )
                        )
                    )
                    .border(
                        1.dp,
                        Color.White.copy(alpha = 0.1f),
                        RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp)
                    )
                    .padding(12.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    "AI Инсайты",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
                insights.take(6).forEach { insight ->
                    InsightCard(insight)
                }
            }
        }

        // Toggle button
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(if (expanded) RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp)
                      else CircleShape)
                .background(
                    if (expanded) Color(0xFF1A1F3A).copy(alpha = 0.96f)
                    else Color(0xFF6366F1).copy(alpha = 0.85f)
                )
                .border(
                    1.dp,
                    Color.White.copy(alpha = 0.15f),
                    if (expanded) RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp)
                    else CircleShape
                )
                .clickable { expanded = !expanded },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (expanded) "›" else "💡",
                color = Color.White,
                fontSize = if (expanded) 18.sp else 14.sp
            )
        }
    }
}

@Composable
private fun InsightCard(insight: CanvasInsight) {
    val (icon, color) = insightTypeConfig(insight.type)

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(color.copy(alpha = 0.1f))
            .border(1.dp, color.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
            .padding(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Text(text = icon, fontSize = 12.sp)
            Text(
                text = insight.title,
                color = color,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }
        Spacer(Modifier.height(3.dp))
        Text(
            text = insight.body,
            color = Color.White.copy(alpha = 0.65f),
            fontSize = 9.sp,
            lineHeight = 13.sp,
            maxLines = 3
        )
    }
}
