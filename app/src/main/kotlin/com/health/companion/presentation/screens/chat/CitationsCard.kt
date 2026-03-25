package com.health.companion.presentation.screens.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.health.companion.data.remote.api.Citation
import com.health.companion.presentation.components.GlassColors
import com.health.companion.presentation.components.GlassTypography

/**
 * 📚 Citations Card — Glassmorphism стиль
 * 
 * Показывает источники информации после web_search
 */
@Composable
fun CitationsCard(
    citations: List<Citation>,
    modifier: Modifier = Modifier
) {
    if (citations.isEmpty()) return
    
    var isExpanded by remember { mutableStateOf(true) }
    val uriHandler = LocalUriHandler.current
    
    // Анимация появления
    val infiniteTransition = rememberInfiniteTransition(label = "citations_glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 40.dp, end = 16.dp, top = 4.dp, bottom = 8.dp)
    ) {
        // Glassmorphism card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF1E3A5F).copy(alpha = 0.6f),
                            Color(0xFF0D1B2A).copy(alpha = 0.8f)
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF1FB8CD).copy(alpha = glowAlpha),
                            Color(0xFF7C3AED).copy(alpha = glowAlpha * 0.5f)
                        )
                    ),
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "📚",
                        fontSize = 16.sp
                    )
                    Text(
                        text = "Источники",
                        style = GlassTypography.labelMedium,
                        color = Color(0xFF1FB8CD),
                        fontWeight = FontWeight.SemiBold
                    )
                    // Badge с количеством
                    Box(
                        modifier = Modifier
                            .background(
                                Color(0xFF1FB8CD).copy(alpha = 0.2f),
                                RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${citations.size}",
                            style = GlassTypography.labelSmall,
                            color = Color(0xFF1FB8CD),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                // Expand/collapse arrow
                Text(
                    text = if (isExpanded) "▼" else "▶",
                    color = GlassColors.textSecondary,
                    fontSize = 10.sp
                )
            }
            
            // Citations list
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier.padding(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    citations.forEach { citation ->
                        CitationItem(
                            citation = citation,
                            onClick = {
                                try {
                                    uriHandler.openUri(citation.url)
                                } catch (e: Exception) {
                                    // Handle error silently
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CitationItem(
    citation: Citation,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Index number
        Box(
            modifier = Modifier
                .size(22.dp)
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF1FB8CD),
                            Color(0xFF7C3AED)
                        )
                    ),
                    RoundedCornerShape(6.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "${citation.index}",
                style = GlassTypography.labelSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
        
        // Domain and title
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = citation.domain,
                style = GlassTypography.labelSmall,
                color = Color(0xFF1FB8CD),
                textDecoration = TextDecoration.Underline,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (citation.title.isNotBlank()) {
                Text(
                    text = citation.title,
                    style = GlassTypography.timestamp,
                    color = GlassColors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        
        // External link icon
        Text(
            text = "↗",
            color = GlassColors.textSecondary,
            fontSize = 14.sp
        )
    }
}
