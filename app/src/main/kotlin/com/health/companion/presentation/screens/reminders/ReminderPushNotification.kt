package com.health.companion.presentation.screens.reminders

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.health.companion.data.remote.api.InChatReminder
import com.health.companion.data.remote.api.ReminderNotification
import com.health.companion.data.remote.api.ReminderPriority
import com.health.companion.presentation.components.*

/**
 * Push notification toast overlay — slides down from top with spring animation.
 * Used in the main scaffold to show WebSocket reminder push notifications.
 */
@Composable
fun ReminderPushNotification(
    notification: ReminderNotification,
    onComplete: (String) -> Unit,
    onSnooze: (String) -> Unit,
    onDismiss: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // Slide-in from top animation
    val offsetY = remember { Animatable(-300f) }
    LaunchedEffect(Unit) {
        offsetY.animateTo(
            targetValue = 0f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            )
        )
    }

    val borderColor = when (notification.priority) {
        ReminderPriority.URGENT -> Color(0xFFF44336).copy(alpha = 0.5f)
        ReminderPriority.HIGH -> Color(0xFFFF9800).copy(alpha = 0.4f)
        else -> GlassColors.accent.copy(alpha = 0.3f)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .offset(y = offsetY.value.dp)
    ) {
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            backgroundColor = GlassColors.surface.copy(alpha = 0.95f),
            borderColor = borderColor
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                // Header: icon + title + description
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = notification.icon,
                        fontSize = 28.sp
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = notification.title,
                            style = GlassTypography.titleSmall.copy(
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        if (!notification.description.isNullOrBlank()) {
                            Text(
                                text = notification.description,
                                style = GlassTypography.timestamp.copy(
                                    color = GlassColors.textSecondary,
                                    fontSize = 12.sp
                                ),
                                maxLines = 2
                            )
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                // Metadata: priority + recurring + category
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PriorityBadge(notification.priority)

                    if (notification.isRecurring) {
                        Text(
                            text = "🔁 Повторяющееся",
                            style = GlassTypography.timestamp.copy(fontSize = 10.sp)
                        )
                    }

                    Text(
                        text = categoryEmoji(notification.category),
                        style = GlassTypography.timestamp.copy(fontSize = 10.sp)
                    )
                }

                Spacer(Modifier.height(12.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // ✅ Complete
                    PushActionButton(
                        text = "✅ Готово",
                        color = GlassColors.success,
                        modifier = Modifier.weight(1f),
                        onClick = { onComplete(notification.reminderId) }
                    )

                    // ⏰ Snooze
                    PushActionButton(
                        text = "⏰ +30мин",
                        color = GlassColors.warning,
                        modifier = Modifier.weight(1f),
                        onClick = { onSnooze(notification.reminderId) }
                    )

                    // ❌ Dismiss
                    PushActionButton(
                        text = "❌",
                        color = GlassColors.error,
                        modifier = Modifier.width(56.dp),
                        onClick = { onDismiss(notification.reminderId) }
                    )
                }
            }
        }
    }
}

@Composable
private fun PushActionButton(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(38.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(color.copy(alpha = 0.12f))
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = GlassTypography.labelSmall.copy(
                color = color,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        )
    }
}

// ============================================================================
// Priority Badge
// ============================================================================

@Composable
fun PriorityBadge(priority: ReminderPriority) {
    val color = Color(priority.color)
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = "${priority.emoji} ${priorityLabel(priority)}",
            style = GlassTypography.timestamp.copy(
                color = color,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium
            )
        )
    }
}

fun priorityLabel(priority: ReminderPriority): String = when (priority) {
    ReminderPriority.LOW -> "Низкий"
    ReminderPriority.MEDIUM -> "Средний"
    ReminderPriority.HIGH -> "Высокий"
    ReminderPriority.URGENT -> "Срочный"
}

private fun categoryEmoji(category: String): String = when (category) {
    "health" -> "💊 Здоровье"
    "work" -> "💼 Работа"
    "personal" -> "🏠 Личное"
    else -> "📌 Общее"
}

// ============================================================================
// In-Chat Reminder Card (for SSE stream)
// ============================================================================

@Composable
fun InChatReminderCard(reminder: InChatReminder) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        backgroundColor = GlassColors.info.copy(alpha = 0.08f),
        borderColor = GlassColors.info.copy(alpha = 0.2f)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("🔔", fontSize = 22.sp)
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = reminder.title,
                    style = GlassTypography.labelMedium.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp
                    )
                )
                Text(
                    text = if (reminder.isRecurring) "🔁 Повторяющееся" else "📍 Одноразовое",
                    style = GlassTypography.timestamp.copy(
                        color = GlassColors.textTertiary,
                        fontSize = 10.sp
                    )
                )
            }
            PriorityBadge(reminder.priority)
        }
    }
}
