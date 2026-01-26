package com.health.companion.presentation.screens.dashboard

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.health.companion.data.remote.api.FactAboutMe
import com.health.companion.data.remote.api.QuickAction
import com.health.companion.data.remote.api.StreakInfo
import com.health.companion.presentation.components.GlassTheme
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = hiltViewModel(),
    onNavigate: (String) -> Unit,
    bottomPadding: androidx.compose.ui.unit.Dp = 0.dp
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    viewModel.loadDashboard()
                    viewModel.startAutoRefresh()
                }
                Lifecycle.Event.ON_PAUSE -> viewModel.stopAutoRefresh()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(Unit) {
        viewModel.navigationEvent.collectLatest { route ->
            onNavigate(route)
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
                start = 20.dp,
                end = 20.dp,
                top = 16.dp + statusBarPadding,
                bottom = 24.dp + bottomPadding
            ),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Hero card with greeting
            item {
                HeroCard(
                    greeting = state.greeting,
                    insight = state.insight,
                    messagesThisWeek = state.messagesThisWeek
                )
            }

            // Streak + Fact row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StreakCard(
                        streak = state.streak,
                        modifier = Modifier.weight(1f)
                    )
                    state.factAboutMe?.let { fact ->
                        FactCard(
                            fact = fact,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Quick actions
            if (state.quickActions.isNotEmpty()) {
                item {
                    QuickActionsRow(
                        actions = state.quickActions,
                        onAction = { viewModel.onQuickAction(it) }
                    )
                }
            }

            // Error
            state.error?.let { error ->
                item {
                    ErrorCard(message = error, onRetry = { viewModel.refresh() })
                }
            }
        }

        PullRefreshIndicator(
            refreshing = state.isLoading,
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter),
            backgroundColor = Color(0xFF1E1E2E),
            contentColor = GlassTheme.accentPrimary
        )
    }
}

@Composable
private fun HeroCard(
    greeting: String,
    insight: String,
    messagesThisWeek: Int
) {
    val infiniteTransition = rememberInfiniteTransition(label = "hero")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
    ) {
        // Glow background
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            GlassTheme.accentPrimary.copy(alpha = glowAlpha * 0.4f),
                            Color.Transparent
                        )
                    )
                )
                .blur(40.dp)
        )

        // Content
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Color(0xFF1A1A2E).copy(alpha = 0.85f),
                    RoundedCornerShape(28.dp)
                )
                .border(
                    1.dp,
                    Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.15f),
                            Color.White.copy(alpha = 0.05f)
                        )
                    ),
                    RoundedCornerShape(28.dp)
                )
                .padding(24.dp)
        ) {
            Column {
                // Greeting
                Text(
                    text = greeting.ifBlank { "Привет! 👋" },
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )

                if (insight.isNotBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = insight,
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White.copy(alpha = 0.7f),
                        lineHeight = 24.sp
                    )
                }

                if (messagesThisWeek > 0) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .background(
                                    GlassTheme.accentPrimary.copy(alpha = 0.2f),
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "$messagesThisWeek сообщений на этой неделе",
                                style = MaterialTheme.typography.labelMedium,
                                color = GlassTheme.accentPrimary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StreakCard(
    streak: StreakInfo,
    modifier: Modifier = Modifier
) {
    val streakColor = when {
        streak.days >= 30 -> Color(0xFFFFD700) // Gold
        streak.days >= 7 -> Color(0xFFFF6B6B)  // Fire red
        streak.days >= 3 -> Color(0xFFFF9F43) // Orange
        streak.days >= 1 -> Color(0xFF4ECDC4) // Teal
        else -> Color(0xFF95A5A6)              // Gray
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(
                Color(0xFF1A1A2E).copy(alpha = 0.9f)
            )
            .border(
                1.dp,
                streakColor.copy(alpha = 0.3f),
                RoundedCornerShape(20.dp)
            )
            .padding(16.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Emoji with glow effect
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(
                        streakColor.copy(alpha = 0.15f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = streak.emoji,
                    fontSize = 28.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Days count
            Text(
                text = if (streak.days > 0) "${streak.days}" else "—",
                style = MaterialTheme.typography.headlineMedium,
                color = streakColor,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = getDaysLabel(streak.days),
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.5f)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Message
            Text(
                text = streak.message,
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun FactCard(
    fact: FactAboutMe,
    modifier: Modifier = Modifier
) {
    // Workaround: если бек прислал JSON в text — парсим
    val displayText = remember(fact.text) {
        extractTextFromPossibleJson(fact.text)
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF667eea).copy(alpha = 0.15f),
                        Color(0xFF764ba2).copy(alpha = 0.15f)
                    )
                )
            )
            .border(
                1.dp,
                Color(0xFF667eea).copy(alpha = 0.3f),
                RoundedCornerShape(20.dp)
            )
            .padding(16.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Emoji
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(
                        Color(0xFF667eea).copy(alpha = 0.2f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = fact.emoji,
                    fontSize = 28.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Label
            Text(
                text = "Факт обо мне",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF667eea),
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.5.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Fact text
            Text(
                text = displayText,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.9f),
                textAlign = TextAlign.Center,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 20.sp
            )
        }
    }
}

/**
 * Workaround для бага бека: factAboutMe.text приходит как JSON/dict
 * Пытаемся извлечь реальный текст из полей 'text' или 'value'
 */
private fun extractTextFromPossibleJson(input: String): String {
    if (input.isBlank()) return ""
    
    // Если не похоже на JSON/dict — возвращаем как есть
    val trimmed = input.trim()
    if (!trimmed.startsWith("{") && !trimmed.startsWith("[")) {
        return input
    }
    
    // Пытаемся найти поле "text" или 'text'
    val textRegex = """['"]text['"]\s*:\s*['"]([^'"]+)['"]""".toRegex()
    textRegex.find(trimmed)?.let { match ->
        return match.groupValues[1]
    }
    
    // Пытаемся найти поле "value"
    val valueRegex = """['"]value['"]\s*:\s*['"]([^'"]+)['"]""".toRegex()
    valueRegex.find(trimmed)?.let { match ->
        return match.groupValues[1]
    }
    
    // Не нашли — показываем заглушку
    return "Узнай меня лучше в чате! 💬"
}

@Composable
private fun QuickActionsRow(
    actions: List<QuickAction>,
    onAction: (QuickAction) -> Unit
) {
    Column {
        Text(
            text = "Быстрые действия",
            style = MaterialTheme.typography.titleMedium,
            color = Color.White.copy(alpha = 0.6f),
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(12.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(actions) { action ->
                QuickActionChip(
                    action = action,
                    onClick = { onAction(action) }
                )
            }
        }
    }
}

@Composable
private fun QuickActionChip(
    action: QuickAction,
    onClick: () -> Unit
) {
    val chipColor = when (action.id) {
        "continue" -> GlassTheme.accentPrimary
        "new" -> Color(0xFF00D9A5)
        "docs" -> Color(0xFFFF9F43)
        else -> GlassTheme.accentSecondary
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(
                chipColor.copy(alpha = 0.12f)
            )
            .border(
                1.dp,
                chipColor.copy(alpha = 0.25f),
                RoundedCornerShape(16.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = action.emoji,
                fontSize = 20.sp
            )
            Text(
                text = action.title,
                style = MaterialTheme.typography.labelLarge,
                color = chipColor,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun ErrorCard(message: String, onRetry: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFFF6B6B).copy(alpha = 0.1f))
            .border(
                1.dp,
                Color(0xFFFF6B6B).copy(alpha = 0.3f),
                RoundedCornerShape(16.dp)
            )
            .padding(16.dp)
    ) {
        Column {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.8f)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFFF6B6B).copy(alpha = 0.2f))
                    .clickable { onRetry() }
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Text(
                    text = "Повторить",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFFFF6B6B),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

private fun getDaysLabel(days: Int): String {
    if (days == 0) return "дней"
    val lastTwo = days % 100
    val lastOne = days % 10
    return when {
        lastTwo in 11..14 -> "дней"
        lastOne == 1 -> "день"
        lastOne in 2..4 -> "дня"
        else -> "дней"
    }
}
