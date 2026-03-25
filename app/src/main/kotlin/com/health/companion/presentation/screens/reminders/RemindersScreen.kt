package com.health.companion.presentation.screens.reminders

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.health.companion.data.remote.api.*
import com.health.companion.presentation.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemindersScreen(
    viewModel: RemindersViewModel = hiltViewModel(),
    onBack: () -> Unit = {}
) {
    val reminders by viewModel.reminders.collectAsState()
    val stats by viewModel.stats.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val filter by viewModel.statusFilter.collectAsState()
    val error by viewModel.error.collectAsState()

    var showQuickAdd by remember { mutableStateOf(false) }
    var quickAddText by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current
    val snackbarHostState = remember { SnackbarHostState() }

    // Listen for success messages
    LaunchedEffect(Unit) {
        viewModel.successMessage.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    // Show error
    LaunchedEffect(error) {
        error?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearError()
        }
    }

    GlassBackground {
        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            "🔔 Напоминания",
                            style = GlassTypography.heading
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Назад",
                                tint = GlassColors.textPrimary
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { showQuickAdd = !showQuickAdd }) {
                            Icon(
                                if (showQuickAdd) Icons.Default.Close else Icons.Default.Add,
                                contentDescription = "Создать",
                                tint = GlassColors.accent
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // Quick-add panel (AI parsing)
                AnimatedVisibility(
                    visible = showQuickAdd,
                    enter = slideInVertically() + fadeIn(),
                    exit = slideOutVertically() + fadeOut()
                ) {
                    QuickAddReminder(
                        text = quickAddText,
                        onTextChange = { quickAddText = it },
                        onSubmit = {
                            if (quickAddText.isNotBlank()) {
                                viewModel.createFromText(quickAddText)
                                quickAddText = ""
                                showQuickAdd = false
                                focusManager.clearFocus()
                            }
                        },
                        isLoading = isLoading
                    )
                }

                // Filter chips
                FilterChipsRow(
                    selected = filter,
                    onSelect = { viewModel.setFilter(it) }
                )

                // Stats bar
                stats?.let { s ->
                    StatsBar(stats = s)
                }

                // Content
                if (isLoading && reminders.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = GlassColors.accent)
                    }
                } else if (reminders.isEmpty()) {
                    EmptyRemindersPlaceholder(
                        modifier = Modifier.weight(1f),
                        onCreateClick = { showQuickAdd = true }
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = 8.dp,
                            bottom = 16.dp
                        )
                    ) {
                        items(reminders, key = { it.id }) { reminder ->
                            ReminderCard(
                                reminder = reminder,
                                onComplete = { viewModel.completeReminder(it) },
                                onSnooze = { viewModel.snoozeReminder(it) },
                                onDelete = { viewModel.deleteReminder(it) }
                            )
                        }
                    }
                }
            }
        }
    }
}

// ============================================================================
// Quick-add with AI parsing
// ============================================================================

@Composable
private fun QuickAddReminder(
    text: String,
    onTextChange: (String) -> Unit,
    onSubmit: () -> Unit,
    isLoading: Boolean
) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        backgroundColor = GlassColors.surface.copy(alpha = 0.8f)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                "✨ AI-создание",
                style = GlassTypography.labelSmall.copy(color = GlassColors.accent),
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                GlassTextField(
                    value = text,
                    onValueChange = onTextChange,
                    placeholder = "Напомни завтра в 10 купить молоко...",
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { onSubmit() })
                )
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            if (text.isNotBlank()) GlassGradients.accent
                            else Brush.solidColor(GlassColors.surface)
                        )
                        .clickable(enabled = text.isNotBlank() && !isLoading) { onSubmit() },
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Создать",
                            tint = if (text.isNotBlank()) Color.White else GlassColors.textMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
            // Hint
            Text(
                "💡 Пиши как думаешь — AI сам разберёт время и частоту",
                style = GlassTypography.timestamp.copy(
                    color = GlassColors.textTertiary,
                    fontSize = 10.sp
                ),
                modifier = Modifier.padding(top = 6.dp)
            )
        }
    }
}

// ============================================================================
// Filter chips
// ============================================================================

@Composable
private fun FilterChipsRow(selected: String, onSelect: (String) -> Unit) {
    val filters = listOf(
        "active" to "🟢 Активные",
        "all" to "📋 Все",
        "completed" to "✅ Выполненные",
        "snoozed" to "⏰ Отложенные"
    )

    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(vertical = 8.dp)
    ) {
        items(filters) { (value, label) ->
            val isSelected = selected == value
            GlassChip(
                text = label,
                color = if (isSelected) GlassColors.accent else GlassColors.textTertiary,
                onClick = { onSelect(value) }
            )
        }
    }
}

// ============================================================================
// Stats bar
// ============================================================================

@Composable
private fun StatsBar(stats: ReminderStatsResponse) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        StatItem("📊", "${stats.active}", "активных")
        StatItem("🔁", "${stats.recurring}", "повторяющихся")
        StatItem("✅", "${stats.completed}", "выполненных")
        StatItem("🔔", "${stats.totalTriggers}", "срабатываний")
    }
}

@Composable
private fun StatItem(emoji: String, value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "$emoji $value",
            style = GlassTypography.labelMedium,
            fontSize = 13.sp
        )
        Text(
            text = label,
            style = GlassTypography.timestamp.copy(fontSize = 9.sp)
        )
    }
}

// ============================================================================
// Reminder Card
// ============================================================================

@Composable
private fun ReminderCard(
    reminder: ReminderDTO,
    onComplete: (String) -> Unit,
    onSnooze: (String) -> Unit,
    onDelete: (String) -> Unit
) {
    val priority = ReminderPriority.from(reminder.priority)
    val status = ReminderStatus.from(reminder.status)
    val frequency = ReminderFrequency.from(reminder.frequency)
    val isCompleted = status == ReminderStatus.COMPLETED
    val isActive = status == ReminderStatus.ACTIVE || status == ReminderStatus.SNOOZED

    val borderColor = when (priority) {
        ReminderPriority.URGENT -> Color(0xFFF44336).copy(alpha = 0.4f)
        ReminderPriority.HIGH -> Color(0xFFFF9800).copy(alpha = 0.3f)
        else -> GlassColors.whiteOverlay10
    }

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        backgroundColor = GlassColors.surface.copy(alpha = 0.7f),
        borderColor = borderColor
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.Top,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Priority emoji
                Text(
                    text = priority.emoji,
                    fontSize = 22.sp,
                    modifier = Modifier.padding(end = 10.dp, top = 2.dp)
                )

                // Title & schedule
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = reminder.title,
                        style = GlassTypography.titleSmall.copy(fontSize = 14.sp),
                        fontWeight = FontWeight.Medium,
                        textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (!reminder.description.isNullOrBlank()) {
                        Text(
                            text = reminder.description,
                            style = GlassTypography.timestamp.copy(
                                color = GlassColors.textSecondary,
                                fontSize = 11.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }

                    // Schedule info
                    Text(
                        text = formatSchedule(reminder),
                        style = GlassTypography.timestamp.copy(fontSize = 11.sp),
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    // Trigger count for recurring
                    if (reminder.isRecurring && reminder.triggerCount > 0) {
                        Text(
                            text = "(${reminder.triggerCount} раз ✓)",
                            style = GlassTypography.timestamp.copy(
                                color = GlassColors.accent.copy(alpha = 0.8f),
                                fontSize = 10.sp
                            ),
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }

                // Frequency badge + Category
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Frequency badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(GlassColors.accent.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = frequency.value.uppercase(),
                            style = GlassTypography.timestamp.copy(
                                color = GlassColors.accent,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }

                    // Category badge
                    Text(
                        text = categoryLabel(reminder.category),
                        style = GlassTypography.timestamp.copy(fontSize = 10.sp)
                    )
                }
            }

            // Action buttons (only for active/snoozed)
            if (isActive) {
                Spacer(Modifier.height(8.dp))
                GlassDivider()
                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Complete button
                    GlassActionButton(
                        text = "✅ Готово",
                        color = GlassColors.success,
                        modifier = Modifier.weight(1f),
                        onClick = { onComplete(reminder.id) }
                    )

                    // Snooze button
                    GlassActionButton(
                        text = "⏰ +30мин",
                        color = GlassColors.warning,
                        modifier = Modifier.weight(1f),
                        onClick = { onSnooze(reminder.id) }
                    )

                    // Delete button
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(GlassColors.error.copy(alpha = 0.1f))
                            .border(1.dp, GlassColors.error.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                            .clickable { onDelete(reminder.id) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🗑️", fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun GlassActionButton(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(36.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.1f))
            .border(1.dp, color.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = GlassTypography.labelSmall.copy(
                color = color,
                fontSize = 12.sp
            )
        )
    }
}

// ============================================================================
// Empty state
// ============================================================================

@Composable
private fun EmptyRemindersPlaceholder(
    modifier: Modifier = Modifier,
    onCreateClick: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("🔕", fontSize = 56.sp)
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Нет активных напоминаний",
            style = GlassTypography.titleSmall,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Напиши в чат «Напомни мне...»\nили нажми + чтобы создать",
            style = GlassTypography.timestamp.copy(
                color = GlassColors.textSecondary,
                fontSize = 12.sp
            ),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))
        GlassButton(
            onClick = onCreateClick,
            isPrimary = true
        ) {
            Text("✨ Создать напоминание", color = Color.White, fontSize = 14.sp)
        }
    }
}

// ============================================================================
// Helpers
// ============================================================================

private fun formatSchedule(reminder: ReminderDTO): String {
    val freq = ReminderFrequency.from(reminder.frequency)
    return when (freq) {
        ReminderFrequency.ONCE -> {
            if (reminder.triggerAt != null) {
                "⏰ ${formatDateTime(reminder.triggerAt)}"
            } else "⏰ Одноразовое"
        }
        ReminderFrequency.DAILY -> "⏰ Каждый день в ${reminder.recurringTime}"
        ReminderFrequency.WEEKDAYS -> "⏰ По будням в ${reminder.recurringTime}"
        ReminderFrequency.WEEKLY -> "⏰ Еженедельно в ${reminder.recurringTime}"
        ReminderFrequency.MONTHLY -> "⏰ Ежемесячно в ${reminder.recurringTime}"
        ReminderFrequency.BIWEEKLY -> "⏰ Раз в 2 недели в ${reminder.recurringTime}"
        ReminderFrequency.CUSTOM -> {
            val mins = reminder.customIntervalMinutes
            if (mins != null) "⏰ Каждые $mins мин" else "⏰ Пользовательское"
        }
    }
}

private fun formatDateTime(isoString: String): String {
    return try {
        // Simple ISO parse — "2026-02-10T10:00:00" → "10.02 10:00"
        val parts = isoString.replace("Z", "").split("T")
        if (parts.size == 2) {
            val dateParts = parts[0].split("-")
            val timePart = parts[1].take(5) // "10:00"
            if (dateParts.size == 3) {
                "${dateParts[2]}.${dateParts[1]} $timePart"
            } else timePart
        } else isoString.take(16)
    } catch (e: Exception) {
        isoString.take(16)
    }
}

private fun categoryLabel(category: String): String = when (category) {
    "health" -> "💊 Здоровье"
    "work" -> "💼 Работа"
    "personal" -> "🏠 Личное"
    else -> "📌 Общее"
}
