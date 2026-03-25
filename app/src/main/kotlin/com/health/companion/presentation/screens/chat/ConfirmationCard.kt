package com.health.companion.presentation.screens.chat

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.health.companion.data.remote.api.ConfirmationEvent
import com.health.companion.presentation.components.*

@Composable
fun ConfirmationCard(
    event: ConfirmationEvent,
    onApprove: () -> Unit,
    onReject: () -> Unit,
    isProcessing: Boolean = false,
    modifier: Modifier = Modifier
) {
    val warningColor = Color(0xFFF59E0B)
    val shape = RoundedCornerShape(16.dp)

    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = GlassSpacing.screenEdge)
            .animateContentSize(),
        shape = shape,
        borderColor = warningColor.copy(alpha = 0.3f),
        elevation = GlassElevation.assistantBubble
    ) {
        Column(
            modifier = Modifier.padding(GlassSpacing.bubbleHorizontal),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "⚠️",
                    style = GlassTypography.messageText
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Подтверждение",
                    style = GlassTypography.messageText.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = warningColor
                    )
                )
            }

            Text(
                text = event.preview,
                style = GlassTypography.messageText.copy(color = GlassColors.textPrimary)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End)
            ) {
                val btnShape = RoundedCornerShape(10.dp)

                Box(
                    modifier = Modifier
                        .clip(btnShape)
                        .background(Color(0x33F87171), btnShape)
                        .border(0.5.dp, Color(0x55F87171), btnShape)
                        .then(if (!isProcessing) Modifier.clickable { onReject() } else Modifier)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "❌ Отменить",
                        style = GlassTypography.labelSmall.copy(
                            color = GlassColors.error,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(btnShape)
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xFF4ADE80).copy(alpha = 0.25f), Color(0xFF22C55E).copy(alpha = 0.25f))
                            ),
                            btnShape
                        )
                        .border(0.5.dp, Color(0x554ADE80), btnShape)
                        .then(if (!isProcessing) Modifier.clickable { onApprove() } else Modifier)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isProcessing) "⏳" else "✅ Подтвердить",
                        style = GlassTypography.labelSmall.copy(
                            color = GlassColors.success,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }
        }
    }
}
