package com.health.companion.presentation.screens.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Mood
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.health.companion.data.remote.api.Widget
import com.health.companion.data.remote.api.WidgetAction
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = hiltViewModel(),
    onNavigate: (String) -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.navigationEvent.collectLatest { route ->
            onNavigate(mapDashboardRoute(route) ?: return@collectLatest)
        }
    }

    val pullRefreshState = rememberPullRefreshState(
        refreshing = state.isLoading,
        onRefresh = { viewModel.refresh() }
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pullRefresh(pullRefreshState)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                GreetingHeader(
                    greeting = state.greeting,
                    emoji = state.moodEmoji,
                    status = state.overallStatus
                )
            }

            val errorMessage = state.error
            if (errorMessage != null) {
                item { ErrorCard(message = errorMessage, onRetry = { viewModel.refresh() }) }
            }

            itemsIndexed(state.widgets) { index, widget ->
                var visible by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) {
                    delay(index * 50L)
                    visible = true
                }
                AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn(animationSpec = tween(300)) +
                        slideInVertically(
                            animationSpec = tween(300),
                            initialOffsetY = { it / 2 }
                        )
                ) {
                    WidgetCard(
                        widget = widget,
                        onAction = { route -> viewModel.navigate(route) }
                    )
                }
            }
        }

        PullRefreshIndicator(
            refreshing = state.isLoading,
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}

@Composable
private fun GreetingHeader(
    greeting: String,
    emoji: String,
    status: String
) {
    val statusColor = getStatusColor(status)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DashboardColors.cardBackground),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = emoji,
                fontSize = 48.sp,
                modifier = Modifier.padding(end = 16.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = greeting,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(statusColor, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = when (status) {
                            "good" -> "Всё отлично"
                            "needs_attention" -> "Требует внимания"
                            else -> "Нормально"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = statusColor
                    )
                }
            }
        }
    }
}

@Composable
private fun WidgetCard(
    widget: Widget,
    onAction: (String) -> Unit
) {
    when (widget.type) {
        "mood_alert" -> MoodAlertWidget(widget, onAction)
        "mood_summary" -> MoodSummaryWidget(widget, onAction)
        "health_metrics" -> HealthMetricsWidget(widget)
        "goals_progress" -> GoalsProgressWidget(widget)
        "recent_documents" -> RecentDocumentsWidget(widget)
        "quick_actions" -> QuickActionsWidget(widget, onAction)
        "daily_tip" -> DailyTipWidget(widget)
        "streak" -> StreakWidget(widget)
        else -> DefaultWidget(widget)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MoodAlertWidget(
    widget: Widget,
    onAction: (String) -> Unit
) {
    val message = widget.data.string("message")
    val emotions = widget.data.stringList("detected_emotions")
    val pulse = rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Reverse),
        label = "pulse_alpha"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DashboardColors.alertBackground),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            DashboardColors.statusAttention.copy(alpha = pulse.value)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "💙", fontSize = 24.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = (widget.title ?: "Поддержка").replace("💙 ", ""),
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = message.ifBlank { "Я рядом и готова помочь." },
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.9f)
            )
            if (emotions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    emotions.forEach { emotion ->
                        AssistChip(
                            onClick = {},
                            label = { Text(emotion, fontSize = 12.sp) },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = DashboardColors.surfaceColor
                            )
                        )
                    }
                }
            }
            widget.action?.let { action ->
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { onAction(action.route) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = DashboardColors.primary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(action.label)
                }
            }
        }
    }
}

@Composable
private fun MoodSummaryWidget(
    widget: Widget,
    onAction: (String) -> Unit
) {
    val moodLevel = widget.data.int("mood_level") ?: 3
    val stressLevel = widget.data.int("stress_level") ?: 3
    val energyLevel = widget.data.int("energy_level") ?: 3
    val emoji = widget.data.string("emoji").ifBlank { "😐" }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DashboardColors.cardBackground),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = widget.title ?: "Настроение",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
                Text(text = emoji, fontSize = 28.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MoodMetricCircle(
                    label = "Настроение",
                    value = moodLevel,
                    maxValue = 5,
                    color = getMoodColor(moodLevel)
                )
                MoodMetricCircle(
                    label = "Стресс",
                    value = stressLevel,
                    maxValue = 5,
                    color = getMoodColor(6 - stressLevel)
                )
                MoodMetricCircle(
                    label = "Энергия",
                    value = energyLevel,
                    maxValue = 5,
                    color = getMoodColor(energyLevel)
                )
            }

            widget.action?.let { action ->
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedButton(
                    onClick = { onAction(action.route) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(action.label, color = DashboardColors.primary)
                }
            }
        }
    }
}

@Composable
private fun MoodMetricCircle(
    label: String,
    value: Int,
    maxValue: Int,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                progress = { value.toFloat() / maxValue },
                modifier = Modifier.size(56.dp),
                color = color,
                trackColor = color.copy(alpha = 0.2f),
                strokeWidth = 6.dp
            )
            Text(
                text = "$value",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun HealthMetricsWidget(widget: Widget) {
    val steps = widget.data.int("steps")
    val heartRate = widget.data.int("heart_rate")
    val sleep = widget.data.double("sleep")

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DashboardColors.cardBackground),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = widget.title ?: "Показатели",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                steps?.let {
                    MetricItem(icon = "👟", value = it.toString(), label = "шагов")
                }
                heartRate?.let {
                    MetricItem(icon = "❤️", value = it.toString(), label = "уд/мин")
                }
                sleep?.let {
                    MetricItem(icon = "😴", value = String.format("%.1f", it), label = "часов")
                }
            }
        }
    }
}

@Composable
private fun MetricItem(icon: String, value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = icon, fontSize = 24.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.6f)
        )
    }
}

@Composable
private fun GoalsProgressWidget(widget: Widget) {
    val goals = widget.data.stringList("goals")
    val total = widget.data.int("total") ?: goals.size

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DashboardColors.cardBackground),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = widget.title ?: "Цели",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Всего целей: $total",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.7f)
            )
            goals.take(3).forEach { goal ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "• $goal",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
        }
    }
}

@Composable
private fun RecentDocumentsWidget(widget: Widget) {
    val documents = widget.data.stringList("documents")
    val total = widget.data.int("total") ?: documents.size

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DashboardColors.cardBackground),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = widget.title ?: "Документы",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Всего: $total",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.7f)
            )
            documents.take(3).forEach { name ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "• $name",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
        }
    }
}

@Composable
private fun QuickActionsWidget(
    widget: Widget,
    onAction: (String) -> Unit
) {
    val actions = widget.data.quickActions()

    Column {
        Text(
            text = widget.title ?: "Быстрые действия",
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(actions) { action ->
                QuickActionButton(action = action, onClick = { onAction(action.route) })
            }
        }
    }
}

@Composable
private fun QuickActionButton(
    action: QuickAction,
    onClick: () -> Unit
) {
    val icon = when (action.icon) {
        "chat" -> Icons.AutoMirrored.Filled.Chat
        "mood" -> Icons.Default.Mood
        "upload" -> Icons.Default.Upload
        "spa" -> Icons.Default.Spa
        else -> Icons.Default.Star
    }
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = DashboardColors.surfaceColor),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            androidx.compose.material3.Icon(
                imageVector = icon,
                contentDescription = null,
                tint = DashboardColors.primary,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = action.label,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White,
                textAlign = TextAlign.Center,
                maxLines = 2
            )
        }
    }
}

@Composable
private fun DailyTipWidget(widget: Widget) {
    val tip = widget.data.string("tip")

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DashboardColors.primary.copy(alpha = 0.15f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Text(
                text = "💡",
                fontSize = 24.sp,
                modifier = Modifier.padding(end = 12.dp)
            )
            Column {
                Text(
                    text = "Совет дня",
                    style = MaterialTheme.typography.labelMedium,
                    color = DashboardColors.primary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = tip,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
        }
    }
}

@Composable
private fun StreakWidget(widget: Widget) {
    val messagesToday = widget.data.int("messages_today") ?: 0
    val streakDays = widget.data.int("streak_days") ?: 0

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DashboardColors.cardBackground),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = widget.title ?: "Серия",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Сегодня сообщений: $messagesToday",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
            Text(
                text = "🔥 $streakDays",
                style = MaterialTheme.typography.titleLarge,
                color = DashboardColors.primary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun DefaultWidget(widget: Widget) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DashboardColors.cardBackground),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = widget.title ?: "Виджет",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )
        }
    }
}

@Composable
private fun ErrorCard(message: String, onRetry: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onRetry,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Повторить")
            }
        }
    }
}

private data class QuickAction(
    val label: String,
    val route: String,
    val icon: String
)

private fun mapDashboardRoute(raw: String): String? {
    return when (raw.trim().lowercase()) {
        "/chat" -> "chat"
        "/documents" -> "documents"
        "/dashboard" -> "dashboard"
        "/settings" -> "settings"
        else -> null
    }
}

private object DashboardColors {
    val statusGood = Color(0xFF4CAF50)
    val statusNeutral = Color(0xFF2196F3)
    val statusAttention = Color(0xFFFF9800)

    val moodExcellent = Color(0xFF66BB6A)
    val moodGood = Color(0xFF81C784)
    val moodNeutral = Color(0xFFFFCA28)
    val moodLow = Color(0xFFFF8A65)
    val moodBad = Color(0xFFE57373)

    val alertBackground = Color(0x20FF9800)
    val cardBackground = Color(0xFF1E1E2E)
    val surfaceColor = Color(0xFF2D2D3A)

    val primary = Color(0xFF00D9A5)
}

private fun getMoodColor(level: Int): Color = when (level) {
    5 -> DashboardColors.moodExcellent
    4 -> DashboardColors.moodGood
    3 -> DashboardColors.moodNeutral
    2 -> DashboardColors.moodLow
    else -> DashboardColors.moodBad
}

private fun getStatusColor(status: String): Color = when (status) {
    "good" -> DashboardColors.statusGood
    "needs_attention" -> DashboardColors.statusAttention
    else -> DashboardColors.statusNeutral
}

private fun Map<String, JsonElement>.string(key: String): String {
    return (this[key] as? JsonPrimitive).contentSafe() ?: ""
}

private fun Map<String, JsonElement>.int(key: String): Int? {
    return (this[key] as? JsonPrimitive).contentSafe()?.toIntOrNull()
}

private fun Map<String, JsonElement>.double(key: String): Double? {
    return (this[key] as? JsonPrimitive).contentSafe()?.toDoubleOrNull()
}

private fun Map<String, JsonElement>.stringList(key: String): List<String> {
    val element = this[key] ?: return emptyList()
    return if (element is JsonArray) {
        element.mapNotNull { (it as? JsonPrimitive).contentSafe() }
    } else {
        emptyList()
    }
}

private fun Map<String, JsonElement>.quickActions(): List<QuickAction> {
    val element = this["actions"] ?: return emptyList()
    if (element !is JsonArray) return emptyList()
    return element.mapNotNull { item ->
        val obj = (item as? JsonObject) ?: return@mapNotNull null
        val label = (obj["label"] as? JsonPrimitive).contentSafe() ?: return@mapNotNull null
        val route = (obj["route"] as? JsonPrimitive).contentSafe() ?: ""
        val icon = (obj["icon"] as? JsonPrimitive).contentSafe() ?: "chat"
        QuickAction(label = label.replace(Regex("^[^\\s]+\\s"), ""), route = route, icon = icon)
    }
}

private fun JsonPrimitive?.contentSafe(): String? = this?.content
