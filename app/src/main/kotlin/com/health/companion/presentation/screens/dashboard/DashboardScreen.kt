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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import com.health.companion.presentation.components.*
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

    // Определяем показывать ли скелетоны (первая загрузка без данных)
    val showSkeletons = state.isLoading && state.greeting.isEmpty()

    // Pull-to-refresh только если есть данные (не при первой загрузке)
    val pullRefreshState = rememberPullRefreshState(
        refreshing = state.isLoading && !showSkeletons,
        onRefresh = { viewModel.refresh() },
        refreshThreshold = 80.dp, // Более чувствительный
        refreshingOffset = 60.dp
    )

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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(GlassGradients.backgroundVertical)
            .pullRefresh(pullRefreshState, enabled = !showSkeletons)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = GlassSpacing.screenEdge + 8.dp,
                end = GlassSpacing.screenEdge + 8.dp,
                top = 16.dp + statusBarPadding,
                bottom = 24.dp + bottomPadding
            ),
            verticalArrangement = Arrangement.spacedBy(GlassSpacing.betweenSections)
        ) {
            if (showSkeletons) {
                // Скелетоны при первой загрузке
                item { HeroCardSkeleton() }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StreakMiniSkeleton(modifier = Modifier.width(90.dp))
                        FactCardSkeleton(modifier = Modifier.weight(1f))
                    }
                }
            } else {
                // Реальные данные
                item {
                    HeroCardV2(
                        greeting = state.greeting,
                        insight = state.insight,
                        messagesThisWeek = state.messagesThisWeek
                    )
                }

                item {
                    val widgetHeight = 100.dp
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(widgetHeight),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StreakMiniCard(
                            streak = state.streak,
                            modifier = Modifier
                                .width(widgetHeight) // Квадрат
                                .fillMaxHeight()
                        )
                        
                        state.factAboutMe?.let { fact ->
                            FactCardVertical(
                                fact = fact,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            )
                        } ?: FactCardSkeleton(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                    }
                }

                state.error?.let { error ->
                    item {
                        ErrorCardV2(message = error, onRetry = { viewModel.refresh() })
                    }
                }
            }
        }

        // Индикатор обновления - под status bar
        if (!showSkeletons) {
            PullRefreshIndicator(
                refreshing = state.isLoading,
                state = pullRefreshState,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = statusBarPadding),
                backgroundColor = GlassColors.surface,
                contentColor = GlassColors.accent,
                scale = true
            )
        }
    }
}

// ==================== SKELETON COMPONENTS ====================

@Composable
private fun HeroCardSkeleton() {
    val transition = rememberInfiniteTransition(label = "skeleton")
    val alpha by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Reverse),
        label = "alpha"
    )

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = GlassShapes.card,
        backgroundColor = GlassColors.surface.copy(alpha = 0.9f),
        elevation = 4.dp
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            // Title skeleton
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(28.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White.copy(alpha = alpha * 0.15f))
            )
            Spacer(modifier = Modifier.height(16.dp))
            // Insight skeleton lines
            repeat(2) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(if (it == 1) 0.8f else 1f)
                        .height(16.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.White.copy(alpha = alpha * 0.1f))
                )
                if (it == 0) Spacer(modifier = Modifier.height(8.dp))
            }
            Spacer(modifier = Modifier.height(16.dp))
            // Chip skeleton
            Box(
                modifier = Modifier
                    .width(180.dp)
                    .height(32.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(GlassColors.accent.copy(alpha = alpha * 0.2f))
            )
        }
    }
}

@Composable
private fun StreakMiniSkeleton(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "skeleton")
    val alpha by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Reverse),
        label = "alpha"
    )

    GlassCard(
        modifier = modifier.aspectRatio(1f),
        shape = GlassShapes.extraLarge,
        backgroundColor = GlassColors.surface.copy(alpha = 0.9f)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = alpha * 0.15f))
            )
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .size(32.dp, 24.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.White.copy(alpha = alpha * 0.12f))
            )
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .size(40.dp, 10.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.White.copy(alpha = alpha * 0.08f))
            )
        }
    }
}

@Composable
private fun FactCardSkeleton(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "skeleton")
    val alpha by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Reverse),
        label = "alpha"
    )

    GlassCard(
        modifier = modifier,
        shape = GlassShapes.extraLarge,
        backgroundColor = GlassColors.surface.copy(alpha = 0.9f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(GlassColors.accent.copy(alpha = alpha * 0.2f))
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .width(80.dp)
                        .height(12.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(GlassColors.accent.copy(alpha = alpha * 0.3f))
                )
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(14.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.White.copy(alpha = alpha * 0.1f))
                )
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .height(14.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.White.copy(alpha = alpha * 0.08f))
                )
            }
        }
    }
}

@Composable
private fun HeroCardV2(
    greeting: String,
    insight: String,
    messagesThisWeek: Int
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = GlassShapes.card,
        backgroundColor = GlassColors.surface.copy(alpha = 0.9f),
        elevation = 4.dp
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            // Greeting
            Text(
                text = greeting.ifBlank { "Привет! 👋" },
                style = GlassTypography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            if (insight.isNotBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = insight,
                    style = GlassTypography.messageText.copy(
                        color = GlassColors.textSecondary
                    ),
                    lineHeight = 24.sp
                )
            }

            if (messagesThisWeek > 0) {
                Spacer(modifier = Modifier.height(16.dp))
                GlassChip(
                    text = "$messagesThisWeek сообщений на этой неделе",
                    color = GlassColors.accent
                )
            }
        }
    }
}

/**
 * Мини-блок Streak - маленький квадратик
 */
@Composable
private fun StreakMiniCard(
    streak: StreakInfo,
    modifier: Modifier = Modifier
) {
    val streakColor = when {
        streak.days >= 30 -> Color(0xFFFFD700) // Gold
        streak.days >= 7 -> GlassColors.coral   // Fire red
        streak.days >= 3 -> GlassColors.orange  // Orange
        streak.days >= 1 -> GlassColors.teal    // Teal
        else -> GlassColors.textMuted           // Gray
    }

    GlassCard(
        modifier = modifier,
        shape = GlassShapes.extraLarge,
        backgroundColor = GlassColors.surface.copy(alpha = 0.9f),
        borderColor = streakColor.copy(alpha = 0.3f)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(6.dp)
        ) {
            // Emoji
            Text(
                text = streak.emoji,
                fontSize = 22.sp
            )

            // Days count
            Text(
                text = if (streak.days > 0) "${streak.days}" else "—",
                style = GlassTypography.titleLarge.copy(
                    color = streakColor,
                    fontSize = 22.sp
                ),
                fontWeight = FontWeight.Bold
            )

            Text(
                text = getDaysLabel(streak.days),
                style = GlassTypography.labelSmall.copy(
                    color = GlassColors.textMuted,
                    fontSize = 9.sp
                )
            )
        }
    }
}

/**
 * Основной блок "Факт о тебе" - компактный горизонтальный
 */
@Composable
private fun FactCardVertical(
    fact: FactAboutMe,
    modifier: Modifier = Modifier
) {
    val displayText = remember(fact.text) {
        extractTextFromPossibleJson(fact.text)
    }

    GlassCardGradient(
        modifier = modifier,
        shape = GlassShapes.extraLarge,
        gradient = GlassGradients.purple,
        borderColor = GlassColors.accent.copy(alpha = 0.3f)
    ) {
        // Overlay для читаемости
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(GlassColors.background.copy(alpha = 0.85f))
        )
        
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Emoji слева
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(GlassColors.accent.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = fact.emoji,
                    fontSize = 20.sp
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Текст справа
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Факт о тебе",
                    style = GlassTypography.labelSmall.copy(
                        color = GlassColors.accent,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.3.sp,
                        fontSize = 10.sp
                    )
                )

                Spacer(modifier = Modifier.height(2.dp))

                // Текст с ограничением строк для компактности
                Text(
                    text = displayText,
                    style = GlassTypography.messageText.copy(
                        fontSize = 13.sp,
                        lineHeight = 16.sp
                    ),
                    color = GlassColors.textPrimary,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Workaround для бага бека: factAboutMe.text приходит как JSON/dict
 */
private fun extractTextFromPossibleJson(input: String): String {
    if (input.isBlank()) return ""
    
    val trimmed = input.trim()
    if (!trimmed.startsWith("{") && !trimmed.startsWith("[")) {
        return input
    }
    
    val textRegex = """['"]text['"]\s*:\s*['"]([^'"]+)['"]""".toRegex()
    textRegex.find(trimmed)?.let { return it.groupValues[1] }
    
    val valueRegex = """['"]value['"]\s*:\s*['"]([^'"]+)['"]""".toRegex()
    valueRegex.find(trimmed)?.let { return it.groupValues[1] }
    
    return "Узнай меня лучше в чате! 💬"
}

@Composable
private fun ErrorCardV2(message: String, onRetry: () -> Unit) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = GlassShapes.large,
        backgroundColor = GlassColors.error.copy(alpha = 0.1f),
        borderColor = GlassColors.error.copy(alpha = 0.3f)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = message,
                style = GlassTypography.messageText.copy(
                    color = GlassColors.textSecondary
                )
            )
            Spacer(modifier = Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .clip(GlassShapes.chip)
                    .background(GlassColors.error.copy(alpha = 0.2f))
                    .clickable { onRetry() }
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Text(
                    text = "Повторить",
                    style = GlassTypography.labelMedium.copy(
                        color = GlassColors.error,
                        fontWeight = FontWeight.Medium
                    )
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
