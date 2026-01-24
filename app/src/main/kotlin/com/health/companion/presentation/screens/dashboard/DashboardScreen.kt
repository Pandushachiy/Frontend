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
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.statusBars
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
import com.health.companion.data.remote.api.EmotionalStateResponse
import com.health.companion.data.remote.api.MemorySummaryResponse
import com.health.companion.data.remote.api.Widget
import com.health.companion.data.remote.api.WidgetAction
import com.health.companion.presentation.components.GlassCard
import com.health.companion.presentation.components.GlassTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
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
    onNavigate: (String) -> Unit,
    bottomPadding: androidx.compose.ui.unit.Dp = 0.dp
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val statusBarPadding = androidx.compose.foundation.layout.WindowInsets.statusBars
        .asPaddingValues().calculateTopPadding()
    val lifecycleOwner = LocalLifecycleOwner.current

    // Автообновление при входе/выходе с экрана
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    viewModel.loadDashboard()      // Обновить при входе
                    viewModel.startAutoRefresh()   // Запустить polling
                }
                Lifecycle.Event.ON_PAUSE -> {
                    viewModel.stopAutoRefresh()    // Остановить polling
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

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
            .background(GlassTheme.backgroundGradient)
            .pullRefresh(pullRefreshState)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 16.dp + statusBarPadding,
                bottom = 16.dp + bottomPadding
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp)
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

            // Новый виджет: Эмоциональный статус
            state.emotionalState?.let { emotionalState ->
                item {
                    EmotionalStateWidget(emotionalState)
                }
            }

            // Новый виджет: AI память
            state.memorySummary?.let { memorySummary ->
                item {
                    MemorySummaryWidget(memorySummary)
                }
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
    val statusColor = when (status) {
        "good" -> GlassTheme.statusGood
        "needs_attention" -> GlassTheme.statusWarning
        else -> GlassTheme.accentPrimary
    }

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 24.dp,
        backgroundColor = GlassTheme.glassWhite,
        borderColor = GlassTheme.glassBorder
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Emoji в круге с градиентом
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                GlassTheme.accentPrimary.copy(alpha = 0.3f),
                                GlassTheme.accentSecondary.copy(alpha = 0.2f)
                            )
                        ),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = emoji,
                    fontSize = 36.sp
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = greeting,
                    style = MaterialTheme.typography.titleLarge,
                    color = GlassTheme.textPrimary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(statusColor, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = when (status) {
                            "good" -> "Самочувствие отличное"
                            "needs_attention" -> "Нужна поддержка"
                            else -> "Всё в порядке"
                        },
                        style = MaterialTheme.typography.bodyMedium,
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
        "daily_tip" -> DailyTipWidget(widget)
        "streak" -> StreakWidget(widget)
        // Скрытые виджеты (фильтруются в ViewModel)
        "recent_documents", "quick_actions" -> { /* Скрыты */ }
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
        colors = CardDefaults.cardColors(containerColor = GlassTheme.statusWarning.copy(alpha = 0.1f)),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            GlassTheme.statusWarning.copy(alpha = pulse.value)
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
                                containerColor = GlassTheme.glassWhite
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
                    colors = ButtonDefaults.buttonColors(containerColor = GlassTheme.accentPrimary),
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
        colors = CardDefaults.cardColors(containerColor = GlassTheme.glassWhite),
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
                    Text(action.label, color = GlassTheme.accentPrimary)
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
        colors = CardDefaults.cardColors(containerColor = GlassTheme.glassWhite),
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
        colors = CardDefaults.cardColors(containerColor = GlassTheme.glassWhite),
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

// RecentDocumentsWidget и QuickActionsWidget удалены - заменены на новые виджеты

@Composable
private fun DailyTipWidget(widget: Widget) {
    val tip = widget.data.string("tip")

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 20.dp,
        backgroundColor = GlassTheme.accentPrimary.copy(alpha = 0.12f),
        borderColor = GlassTheme.accentPrimary.copy(alpha = 0.25f)
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        GlassTheme.accentPrimary.copy(alpha = 0.2f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "💡", fontSize = 22.sp)
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = "СОВЕТ ДНЯ",
                    style = MaterialTheme.typography.labelSmall,
                    color = GlassTheme.accentPrimary,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = tip,
                    style = MaterialTheme.typography.bodyLarge,
                    color = GlassTheme.textPrimary,
                    lineHeight = 24.sp
                )
            }
        }
    }
}

@Composable
private fun StreakWidget(widget: Widget) {
    val messagesToday = widget.data.int("messages_today") ?: 0
    val streakDays = widget.data.int("streak_days") ?: 0

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 14.dp,
        backgroundColor = GlassTheme.glassWhite,
        borderColor = GlassTheme.glassBorder
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Компактная иконка
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(
                        GlassTheme.accentWarm.copy(alpha = 0.12f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "💬", fontSize = 12.sp)
            }
            Spacer(modifier = Modifier.width(10.dp))
            
            // Текст занимает доступное пространство
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Активность",
                    style = MaterialTheme.typography.bodyMedium,
                    color = GlassTheme.textPrimary,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Сообщений: $messagesToday",
                    style = MaterialTheme.typography.labelSmall,
                    color = GlassTheme.textSecondary
                )
            }
            
            // Компактный streak badge справа
            Box(
                modifier = Modifier
                    .background(
                        GlassTheme.accentWarm.copy(alpha = 0.15f),
                        RoundedCornerShape(10.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "🔥", fontSize = 10.sp)
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = streakDays.toString(),
                        style = MaterialTheme.typography.labelMedium,
                        color = GlassTheme.accentWarm,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// ========== НОВЫЕ ВИДЖЕТЫ ==========

@Composable
private fun EmotionalStateWidget(emotionalState: EmotionalStateResponse) {
    val primaryEmotion = emotionalState.primaryEmotion ?: "спокойствие"
    val moodLabel = emotionalState.moodLabel ?: "Нейтральное"
    val valence = emotionalState.valence
    val arousal = emotionalState.arousal
    val needsSupport = emotionalState.needsSupport
    val triggers = emotionalState.detectedTriggers

    // Определяем эмодзи на основе valence
    val emoji = when {
        valence > 0.5f -> "😊"
        valence > 0.2f -> "🙂"
        valence > -0.2f -> "😐"
        valence > -0.5f -> "😔"
        else -> "😢"
    }

    // Цвет на основе valence
    val stateColor = when {
        valence > 0.3f -> GlassTheme.statusGood
        valence > 0f -> Color(0xFF81C784)
        valence > -0.3f -> GlassTheme.statusWarning
        else -> GlassTheme.statusError
    }

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 20.dp,
        backgroundColor = if (needsSupport) 
            GlassTheme.statusWarning.copy(alpha = 0.1f) 
        else 
            GlassTheme.glassWhite,
        borderColor = if (needsSupport)
            GlassTheme.statusWarning.copy(alpha = 0.3f)
        else
            GlassTheme.glassBorder
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                GlassTheme.accentSecondary.copy(alpha = 0.2f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "🎭", fontSize = 20.sp)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Эмоциональный статус",
                        style = MaterialTheme.typography.titleMedium,
                        color = GlassTheme.textPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Text(text = emoji, fontSize = 32.sp)
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Основная эмоция и состояние в карточках
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Эмоция
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(
                            Color.White.copy(alpha = 0.05f),
                            RoundedCornerShape(14.dp)
                        )
                        .padding(14.dp)
                ) {
                    Column {
                        Text(
                            text = "ЭМОЦИЯ",
                            style = MaterialTheme.typography.labelSmall,
                            color = GlassTheme.textTertiary,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = primaryEmotion.replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.titleMedium,
                            color = stateColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                // Состояние
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(
                            Color.White.copy(alpha = 0.05f),
                            RoundedCornerShape(14.dp)
                        )
                        .padding(14.dp)
                ) {
                    Column {
                        Text(
                            text = "СОСТОЯНИЕ",
                            style = MaterialTheme.typography.labelSmall,
                            color = GlassTheme.textTertiary,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = moodLabel,
                            style = MaterialTheme.typography.titleMedium,
                            color = GlassTheme.textPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Valence/Arousal индикаторы
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                EmotionIndicator(
                    label = "Позитивность",
                    value = valence,
                    modifier = Modifier.weight(1f)
                )
                EmotionIndicator(
                    label = "Энергия",
                    value = arousal,
                    modifier = Modifier.weight(1f)
                )
            }

            // Триггеры если есть
            if (triggers.isNotEmpty()) {
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "ВОЗМОЖНЫЕ ТРИГГЕРЫ",
                    style = MaterialTheme.typography.labelSmall,
                    color = GlassTheme.textTertiary,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = triggers.joinToString(" • "),
                    style = MaterialTheme.typography.bodyMedium,
                    color = GlassTheme.statusWarning
                )
            }

            // Нужна поддержка
            if (needsSupport) {
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            GlassTheme.accentPrimary.copy(alpha = 0.15f),
                            RoundedCornerShape(12.dp)
                        )
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "💙", fontSize = 22.sp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Я рядом, если захочешь поговорить",
                        style = MaterialTheme.typography.bodyMedium,
                        color = GlassTheme.textPrimary
                    )
                }
            }
        }
    }
}

@Composable
private fun EmotionIndicator(
    label: String,
    value: Float, // -1 to 1
    modifier: Modifier = Modifier
) {
    // Нормализуем: -1..1 -> 0..100%
    val normalizedValue = ((value + 1f) / 2f).coerceIn(0f, 1f)
    val percentValue = (normalizedValue * 100).toInt()
    
    val color = when {
        value > 0.3f -> GlassTheme.statusGood
        value > 0f -> Color(0xFF81C784)
        value > -0.3f -> GlassTheme.statusWarning
        else -> GlassTheme.statusError
    }

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = GlassTheme.textSecondary
            )
            Text(
                text = "$percentValue%",
                style = MaterialTheme.typography.bodySmall,
                color = color,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(4.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(normalizedValue)
                    .height(8.dp)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(color.copy(alpha = 0.7f), color)
                        ),
                        RoundedCornerShape(4.dp)
                    )
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MemorySummaryWidget(memorySummary: MemorySummaryResponse) {
    val factsCount = memorySummary.factsCount
    val recentFacts = memorySummary.recentFacts
    val aiKnowsAbout = memorySummary.aiKnowsAbout

    // Компактный виджет памяти
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 16.dp,
        backgroundColor = Color(0xFF667eea).copy(alpha = 0.08f),
        borderColor = Color(0xFF667eea).copy(alpha = 0.2f)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Компактный header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(Color(0xFF667eea), Color(0xFF764ba2))
                                ),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "🧠", fontSize = 14.sp)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Я помню о тебе",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "$factsCount ${getFactsWord(factsCount)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF667eea)
                        )
                    }
                }
                
                // Компактный счётчик
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color(0xFF667eea).copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = factsCount.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFF667eea),
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Компактные темы (только если есть и максимум 4)
            if (aiKnowsAbout.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    aiKnowsAbout.take(4).forEachIndexed { index, topic ->
                        val chipColors = listOf(
                            Color(0xFF667eea),
                            Color(0xFF764ba2),
                            Color(0xFF00D9A5),
                            Color(0xFF4facfe)
                        )
                        Box(
                            modifier = Modifier
                                .background(
                                    chipColors[index % chipColors.size].copy(alpha = 0.15f),
                                    RoundedCornerShape(12.dp)
                                )
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = topic,
                                fontSize = 11.sp,
                                color = chipColors[index % chipColors.size],
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // Компактные факты (максимум 2)
            if (recentFacts.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                recentFacts.take(2).forEach { fact ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(Color(0xFF00D9A5), CircleShape)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = fact,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.8f),
                            maxLines = 1
                        )
                    }
                }
            }

            // Пустое состояние (компактное)
            if (factsCount == 0 && aiKnowsAbout.isEmpty() && recentFacts.isEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "✨", fontSize = 16.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Расскажи о себе в чате!",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

private fun getFactsWord(count: Int): String {
    val lastTwo = count % 100
    val lastOne = count % 10
    return when {
        lastTwo in 11..14 -> "знаний"
        lastOne == 1 -> "знание"
        lastOne in 2..4 -> "знания"
        else -> "знаний"
    }
}

@Composable
private fun DefaultWidget(widget: Widget) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = GlassTheme.glassWhite),
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

// QuickAction удалён - не используется

private fun mapDashboardRoute(raw: String): String? {
    return when (raw.trim().lowercase()) {
        "/chat" -> "chat"
        "/documents" -> "documents"
        "/dashboard" -> "dashboard"
        "/settings" -> "settings"
        else -> null
    }
}

// DashboardColors удалены - используем GlassTheme

private fun getMoodColor(level: Int): Color = when (level) {
    5 -> GlassTheme.statusGood
    4 -> Color(0xFF81C784)
    3 -> GlassTheme.statusWarning
    2 -> Color(0xFFFF8A65)
    else -> GlassTheme.statusError
}

private fun getStatusColor(status: String): Color = when (status) {
    "good" -> GlassTheme.statusGood
    "needs_attention" -> GlassTheme.statusWarning
    else -> GlassTheme.accentPrimary
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

// quickActions() удалён - не используется

private fun JsonPrimitive?.contentSafe(): String? = this?.content
