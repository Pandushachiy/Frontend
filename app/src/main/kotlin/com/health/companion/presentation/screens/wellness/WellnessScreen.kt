package com.health.companion.presentation.screens.wellness

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.health.companion.data.remote.api.*
import com.health.companion.presentation.components.*
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

// ============================================================================
// 🎨 CUSTOM GLASS COLORS
// ============================================================================

private object WellnessTheme {
    // Mood colors - soft gradient palette
    val moodColors = listOf(
        Color(0xFFFF6B6B), // 1 - coral red
        Color(0xFFFFB347), // 2 - orange  
        Color(0xFFFFE066), // 3 - yellow
        Color(0xFF98D8AA), // 4 - soft green
        Color(0xFF7EB8DA)  // 5 - soft blue
    )
    
    // Glass surfaces
    val glassDark = Color(0xFF1A1F35)
    val glassLight = Color(0xFF252B45)
    val glassBorder = Color(0xFF3D4566)
    val glassHighlight = Color(0xFF4A5280)
    
    // Accents
    val accentPurple = Color(0xFF8B7CF6)
    val accentBlue = Color(0xFF6C9EF8)
    val accentMint = Color(0xFF6ED4A8)
    val accentPink = Color(0xFFE879A9)
    val accentOrange = Color(0xFFF5A962)
}

// ============================================================================
// 🧘 MAIN SCREEN
// ============================================================================

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WellnessScreen(
    viewModel: WellnessViewModel = hiltViewModel()
) {
    val habits by viewModel.habits.collectAsState()
    val habitsStats by viewModel.habitsStats.collectAsState()
    val moodToday by viewModel.moodToday.collectAsState()
    val moodHistory by viewModel.moodHistory.collectAsState()
    val moodStats by viewModel.moodStats.collectAsState()
    val isMoodRecorded by viewModel.isMoodRecordedToday.collectAsState()
    val dailyDigest by viewModel.dailyDigest.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    
    val pagerState = rememberPagerState(pageCount = { 3 })
    val scope = rememberCoroutineScope()
    
    var showMoodSheet by remember { mutableStateOf(false) }
    var showCreateHabitSheet by remember { mutableStateOf(false) }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(GlassGradients.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Glass Tab Selector
            GlassTabSelector(
                selectedIndex = pagerState.currentPage,
                onSelect = { scope.launch { pagerState.animateScrollToPage(it) } }
            )
            
            // Pages
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                when (page) {
                    0 -> MoodPage(
                        moodToday = moodToday,
                        moodHistory = moodHistory,
                        moodStats = moodStats,
                        isMoodRecorded = isMoodRecorded,
                        onRecordMood = { showMoodSheet = true }
                    )
                    1 -> HabitsPage(
                        habits = habits,
                        stats = habitsStats,
                        onComplete = { viewModel.completeHabit(it) },
                        onUncomplete = { viewModel.uncompleteHabit(it) },
                        onCreateHabit = { showCreateHabitSheet = true }
                    )
                    2 -> DigestPage(
                        digest = dailyDigest,
                        onRefresh = { viewModel.refreshDigest() }
                    )
                }
            }
        }
        
        // Loading
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                GlassLoadingIndicator()
            }
        }
    }
    
    // Sheets
    if (showMoodSheet) {
        MoodSheet(
            onDismiss = { showMoodSheet = false },
            onSave = { mood, energy, stress, activities, note ->
                viewModel.recordMood(
                    moodLevel = mood,
                    energyLevel = energy,
                    stressLevel = stress,
                    activities = activities,
                    journalText = note,
                    onSuccess = { showMoodSheet = false }
                )
            }
        )
    }
    
    if (showCreateHabitSheet) {
        HabitSheet(
            onDismiss = { showCreateHabitSheet = false },
            onSave = { name, emoji, color ->
                viewModel.createHabit(
                    name = name,
                    emoji = emoji,
                    color = color,
                    onSuccess = { showCreateHabitSheet = false }
                )
            }
        )
    }
}

// ============================================================================
// 🔘 GLASS TAB SELECTOR
// ============================================================================

@Composable
private fun GlassTabSelector(
    selectedIndex: Int,
    onSelect: (Int) -> Unit
) {
    val tabs = listOf(
        TabInfo("Настроение", WellnessTheme.accentPink),
        TabInfo("Привычки", WellnessTheme.accentMint),
        TabInfo("Сводка", WellnessTheme.accentOrange)
    )
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        tabs.forEachIndexed { index, tab ->
            GlassTab(
                text = tab.title,
                color = tab.color,
                isSelected = index == selectedIndex,
                onClick = { onSelect(index) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

private data class TabInfo(val title: String, val color: Color)

@Composable
private fun GlassTab(
    text: String,
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val animatedAlpha by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0.5f,
        animationSpec = tween(200),
        label = "alpha"
    )
    
    Box(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(
                if (isSelected) {
                    Brush.horizontalGradient(
                        colors = listOf(
                            color.copy(alpha = 0.25f),
                            color.copy(alpha = 0.15f)
                        )
                    )
                } else {
                    Brush.horizontalGradient(
                        colors = listOf(
                            WellnessTheme.glassDark.copy(alpha = 0.6f),
                            WellnessTheme.glassDark.copy(alpha = 0.4f)
                        )
                    )
                }
            )
            .border(
                width = 1.dp,
                color = if (isSelected) color.copy(alpha = 0.5f) 
                       else WellnessTheme.glassBorder.copy(alpha = 0.3f),
                shape = RoundedCornerShape(22.dp)
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = GlassTypography.labelMedium.copy(
                fontSize = 13.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
            ),
            color = if (isSelected) color else GlassColors.textSecondary.copy(alpha = animatedAlpha)
        )
    }
}

// ============================================================================
// 🎨 GLASS CARD COMPONENT
// ============================================================================

@Composable
private fun GlassPanel(
    modifier: Modifier = Modifier,
    accentColor: Color? = null,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        WellnessTheme.glassLight.copy(alpha = 0.7f),
                        WellnessTheme.glassDark.copy(alpha = 0.8f)
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        accentColor?.copy(alpha = 0.3f) ?: WellnessTheme.glassHighlight.copy(alpha = 0.4f),
                        accentColor?.copy(alpha = 0.1f) ?: WellnessTheme.glassBorder.copy(alpha = 0.2f)
                    )
                ),
                shape = RoundedCornerShape(24.dp)
            ),
        content = content
    )
}

// ============================================================================
// 🔄 GLASS LOADING INDICATOR
// ============================================================================

@Composable
private fun GlassLoadingIndicator() {
    val infiniteTransition = rememberInfiniteTransition(label = "loading")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )
    
    Box(
        modifier = Modifier
            .size(56.dp)
            .graphicsLayer { rotationZ = rotation }
            .drawBehind {
                val strokeWidth = 4.dp.toPx()
                val radius = (size.minDimension - strokeWidth) / 2
                
                // Background circle
                drawCircle(
                    color = WellnessTheme.glassBorder.copy(alpha = 0.3f),
                    radius = radius,
                    style = Stroke(width = strokeWidth)
                )
                
                // Gradient arc
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            Color.Transparent,
                            WellnessTheme.accentPurple,
                            WellnessTheme.accentBlue
                        )
                    ),
                    startAngle = 0f,
                    sweepAngle = 270f,
                    useCenter = false,
                    style = Stroke(width = strokeWidth)
                )
            }
    )
}

// ============================================================================
// 😊 MOOD PAGE
// ============================================================================

@Composable
private fun MoodPage(
    moodToday: MoodEntry?,
    moodHistory: List<MoodEntry>,
    moodStats: MoodStats?,
    isMoodRecorded: Boolean,
    onRecordMood: () -> Unit
) {
    val moodEmojis = listOf("😞", "😕", "😐", "🙂", "😊")
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Main mood card
        item {
            MoodMainCard(
                moodToday = moodToday,
                isMoodRecorded = isMoodRecorded,
                moodEmojis = moodEmojis,
                onRecordMood = onRecordMood
            )
        }
        
        // Week visualization
        if (moodHistory.isNotEmpty() || true) {
            item {
                WeekMoodVisualization(
                    entries = moodHistory.takeLast(7),
                    moodEmojis = moodEmojis
                )
            }
        }
        
        // Stats row
        if (moodStats != null) {
            item {
                MoodStatsRow(stats = moodStats)
            }
        }
        
        item { Spacer(Modifier.height(100.dp)) }
    }
}

@Composable
private fun MoodMainCard(
    moodToday: MoodEntry?,
    isMoodRecorded: Boolean,
    moodEmojis: List<String>,
    onRecordMood: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    
    GlassPanel(
        modifier = Modifier.fillMaxWidth(),
        accentColor = if (isMoodRecorded && moodToday != null) {
            WellnessTheme.moodColors.getOrElse(moodToday.moodLevel - 1) { WellnessTheme.accentPurple }
        } else WellnessTheme.accentPink
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onRecordMood()
                }
                .padding(24.dp)
        ) {
            if (isMoodRecorded && moodToday != null) {
                // Recorded mood
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Custom mood indicator
                    MoodOrb(
                        level = moodToday.moodLevel,
                        size = 64.dp
                    )
                    
                    Spacer(Modifier.width(20.dp))
                    
                    Column {
                        Text(
                            text = "Сегодня",
                            style = GlassTypography.labelSmall,
                            color = GlassColors.textMuted
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = when (moodToday.moodLevel) {
                                1 -> "Тяжёлый день"
                                2 -> "Не очень"
                                3 -> "Нормально"
                                4 -> "Хорошо"
                                else -> "Отлично!"
                            },
                            style = GlassTypography.titleLarge.copy(fontSize = 24.sp),
                            color = WellnessTheme.moodColors.getOrElse(moodToday.moodLevel - 1) { Color.White }
                        )
                    }
                }
                
                // Activities & Note
                if (moodToday.activities.isNotEmpty() || moodToday.journalText != null) {
                    Spacer(Modifier.height(20.dp))
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        WellnessTheme.glassBorder.copy(alpha = 0.5f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )
                    
                    Spacer(Modifier.height(16.dp))
                    
                    if (moodToday.activities.isNotEmpty()) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            moodToday.activities.take(3).forEach { activity ->
                                ActivityChip(text = activity)
                            }
                        }
                    }
                    
                    moodToday.journalText?.let { note ->
                        if (moodToday.activities.isNotEmpty()) Spacer(Modifier.height(12.dp))
                        Text(
                            text = "\"$note\"",
                            style = GlassTypography.messageText.copy(
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                fontSize = 14.sp
                            ),
                            color = GlassColors.textSecondary,
                            maxLines = 2
                        )
                    }
                }
                
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "Нажми, чтобы изменить",
                    style = GlassTypography.timestamp,
                    color = GlassColors.textMuted
                )
                
            } else {
                // Prompt
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Как ты сегодня?",
                        style = GlassTypography.titleLarge,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(Modifier.height(28.dp))
                    
                    // Mood orbs row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        (1..5).forEach { level ->
                            MoodOrb(
                                level = level,
                                size = 48.dp,
                                showGlow = true
                            )
                        }
                    }
                    
                    Spacer(Modifier.height(20.dp))
                    
                    Text(
                        text = "Нажми, чтобы записать",
                        style = GlassTypography.labelSmall,
                        color = WellnessTheme.accentPink
                    )
                }
            }
        }
    }
}

// Custom Mood Orb - glass style indicator
@Composable
private fun MoodOrb(
    level: Int,
    size: Dp,
    showGlow: Boolean = false
) {
    val color = WellnessTheme.moodColors.getOrElse(level - 1) { WellnessTheme.accentPurple }
    
    val infiniteTransition = rememberInfiniteTransition(label = "orb")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )
    
    Box(
        modifier = Modifier
            .size(size)
            .then(
                if (showGlow) {
                    Modifier.drawBehind {
                        drawCircle(
                            color = color.copy(alpha = glowAlpha * 0.3f),
                            radius = this.size.minDimension / 2 + 8.dp.toPx()
                        )
                    }
                } else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        // Outer ring
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            color.copy(alpha = 0.3f),
                            color.copy(alpha = 0.1f)
                        )
                    )
                )
                .border(
                    width = 2.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            color.copy(alpha = 0.8f),
                            color.copy(alpha = 0.4f)
                        )
                    ),
                    shape = CircleShape
                )
        )
        
        // Inner glow
        Box(
            modifier = Modifier
                .size(size * 0.6f)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            color.copy(alpha = 0.6f),
                            color.copy(alpha = 0.2f)
                        )
                    )
                )
        )
    }
}

@Composable
private fun ActivityChip(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(WellnessTheme.accentPurple.copy(alpha = 0.15f))
            .border(1.dp, WellnessTheme.accentPurple.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            style = GlassTypography.labelSmall.copy(fontSize = 12.sp),
            color = WellnessTheme.accentPurple
        )
    }
}

@Composable
private fun WeekMoodVisualization(
    entries: List<MoodEntry>,
    moodEmojis: List<String>
) {
    val days = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")
    
    GlassPanel(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Эта неделя",
                style = GlassTypography.titleSmall
            )
            
            Spacer(Modifier.height(20.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                days.forEachIndexed { index, day ->
                    val entry = entries.getOrNull(index)
                    val level = entry?.moodLevel ?: 0
                    
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Mini mood indicator
                        MiniMoodIndicator(
                            level = level,
                            isToday = index == days.lastIndex
                        )
                        
                        Spacer(Modifier.height(8.dp))
                        
                        Text(
                            text = day,
                            style = GlassTypography.timestamp.copy(fontSize = 11.sp),
                            color = if (level > 0) GlassColors.textSecondary 
                                   else GlassColors.textMuted
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MiniMoodIndicator(level: Int, isToday: Boolean) {
    val color = if (level > 0) {
        WellnessTheme.moodColors.getOrElse(level - 1) { WellnessTheme.glassBorder }
    } else {
        WellnessTheme.glassBorder
    }
    
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(
                if (level > 0) {
                    Brush.radialGradient(
                        colors = listOf(
                            color.copy(alpha = 0.4f),
                            color.copy(alpha = 0.1f)
                        )
                    )
                } else {
                    Brush.radialGradient(
                        colors = listOf(
                            WellnessTheme.glassDark.copy(alpha = 0.5f),
                            WellnessTheme.glassDark.copy(alpha = 0.3f)
                        )
                    )
                }
            )
            .border(
                width = if (level > 0) 2.dp else 1.dp,
                color = color.copy(alpha = if (level > 0) 0.7f else 0.3f),
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        if (level > 0) {
            // Inner dot
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.8f))
            )
        }
    }
}

@Composable
private fun MoodStatsRow(stats: MoodStats) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        GlassStatCard(
            value = String.format("%.1f", stats.averageMood),
            label = "Среднее",
            color = WellnessTheme.moodColors[(stats.averageMood - 1).roundToInt().coerceIn(0, 4)],
            modifier = Modifier.weight(1f)
        )
        
        GlassStatCard(
            value = when (stats.trend) {
                "improving" -> "↑"
                "declining" -> "↓"
                else -> "→"
            },
            label = when (stats.trend) {
                "improving" -> "Растёт"
                "declining" -> "Падает"
                else -> "Стабильно"
            },
            color = when (stats.trend) {
                "improving" -> WellnessTheme.accentMint
                "declining" -> WellnessTheme.moodColors[0]
                else -> WellnessTheme.accentOrange
            },
            modifier = Modifier.weight(1f)
        )
        
        GlassStatCard(
            value = stats.totalEntries.toString(),
            label = "Записей",
            color = WellnessTheme.accentBlue,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun GlassStatCard(
    value: String,
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    GlassPanel(
        modifier = modifier,
        accentColor = color
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Value with glow effect
            Text(
                text = value,
                style = GlassTypography.titleLarge.copy(
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = color
            )
            
            Spacer(Modifier.height(4.dp))
            
            Text(
                text = label,
                style = GlassTypography.timestamp,
                color = GlassColors.textSecondary
            )
        }
    }
}

// ============================================================================
// ✅ HABITS PAGE
// ============================================================================

@Composable
private fun HabitsPage(
    habits: List<Habit>,
    stats: HabitsStats?,
    onComplete: (String) -> Unit,
    onUncomplete: (String) -> Unit,
    onCreateHabit: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Progress card
        item {
            HabitsProgressCard(
                completed = habits.count { it.completedToday },
                total = habits.size,
                stats = stats
            )
        }
        
        // Habits list
        if (habits.isNotEmpty()) {
            items(habits, key = { it.id }) { habit ->
                HabitItem(
                    habit = habit,
                    onToggle = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        if (habit.completedToday) onUncomplete(habit.id)
                        else onComplete(habit.id)
                    }
                )
            }
        } else {
            item { EmptyHabitsCard() }
        }
        
        // Add button
        item {
            AddHabitButton(onClick = onCreateHabit)
        }
        
        item { Spacer(Modifier.height(100.dp)) }
    }
}

@Composable
private fun HabitsProgressCard(
    completed: Int,
    total: Int,
    stats: HabitsStats?
) {
    val progress = if (total > 0) completed.toFloat() / total else 0f
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(800, easing = FastOutSlowInEasing),
        label = "progress"
    )
    
    val isComplete = progress >= 1f
    
    GlassPanel(
        modifier = Modifier.fillMaxWidth(),
        accentColor = if (isComplete) WellnessTheme.accentMint else WellnessTheme.accentPurple
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Custom circular progress
            Box(
                modifier = Modifier.size(72.dp),
                contentAlignment = Alignment.Center
            ) {
                // Background ring
                CircularProgressRing(
                    progress = 1f,
                    color = WellnessTheme.glassBorder.copy(alpha = 0.3f),
                    strokeWidth = 6.dp
                )
                
                // Progress ring
                CircularProgressRing(
                    progress = animatedProgress,
                    color = if (isComplete) WellnessTheme.accentMint else WellnessTheme.accentPurple,
                    strokeWidth = 6.dp
                )
                
                // Center text
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$completed",
                        style = GlassTypography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp
                        ),
                        color = if (isComplete) WellnessTheme.accentMint else GlassColors.textPrimary
                    )
                    Text(
                        text = "из $total",
                        style = GlassTypography.timestamp.copy(fontSize = 10.sp),
                        color = GlassColors.textMuted
                    )
                }
            }
            
            Spacer(Modifier.width(20.dp))
            
            Column {
                Text(
                    text = if (isComplete) "Всё выполнено! 🎉"
                          else if (total == 0) "Добавь привычки"
                          else "Сегодня",
                    style = GlassTypography.titleSmall
                )
                
                if (stats != null && stats.longestStreak > 0) {
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        StreakIndicator(streak = stats.longestStreak)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Макс. серия",
                            style = GlassTypography.timestamp,
                            color = GlassColors.textSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CircularProgressRing(
    progress: Float,
    color: Color,
    strokeWidth: Dp,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val stroke = strokeWidth.toPx()
        val radius = (size.minDimension - stroke) / 2
        
        drawArc(
            color = color,
            startAngle = -90f,
            sweepAngle = progress * 360f,
            useCenter = false,
            style = Stroke(width = stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round),
            topLeft = Offset(stroke / 2, stroke / 2),
            size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2)
        )
    }
}

@Composable
private fun StreakIndicator(streak: Int) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(WellnessTheme.accentOrange.copy(alpha = 0.15f))
            .border(1.dp, WellnessTheme.accentOrange.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Fire indicator
        Box(
            modifier = Modifier
                .size(16.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            WellnessTheme.accentOrange,
                            WellnessTheme.moodColors[0]
                        )
                    )
                )
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = "$streak дн.",
            style = GlassTypography.labelSmall.copy(fontSize = 12.sp),
            color = WellnessTheme.accentOrange,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun HabitItem(
    habit: Habit,
    onToggle: () -> Unit
) {
    val habitColor = try {
        Color(android.graphics.Color.parseColor(habit.color))
    } catch (e: Exception) {
        WellnessTheme.accentPurple
    }
    
    val scale by animateFloatAsState(
        targetValue = if (habit.completedToday) 0.98f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "scale"
    )
    
    GlassPanel(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale),
        accentColor = if (habit.completedToday) habitColor else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggle() }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Emoji with glass background
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                habitColor.copy(alpha = 0.25f),
                                habitColor.copy(alpha = 0.1f)
                            )
                        )
                    )
                    .border(1.dp, habitColor.copy(alpha = 0.4f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = habit.emoji, fontSize = 24.sp)
            }
            
            Spacer(Modifier.width(14.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = habit.name,
                    style = GlassTypography.labelMedium,
                    color = if (habit.completedToday) GlassColors.textSecondary
                           else GlassColors.textPrimary,
                    textDecoration = if (habit.completedToday) {
                        androidx.compose.ui.text.style.TextDecoration.LineThrough
                    } else null
                )
                
                if (habit.currentStreak > 0) {
                    Spacer(Modifier.height(4.dp))
                    StreakIndicator(streak = habit.currentStreak)
                }
            }
            
            // Custom checkbox
            HabitCheckbox(
                isChecked = habit.completedToday,
                color = habitColor
            )
        }
    }
}

@Composable
private fun HabitCheckbox(
    isChecked: Boolean,
    color: Color
) {
    val scale by animateFloatAsState(
        targetValue = if (isChecked) 1f else 0.9f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "check"
    )
    
    Box(
        modifier = Modifier
            .size(28.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(
                if (isChecked) {
                    Brush.radialGradient(
                        colors = listOf(color, color.copy(alpha = 0.7f))
                    )
                } else {
                    Brush.radialGradient(
                        colors = listOf(
                            WellnessTheme.glassDark.copy(alpha = 0.5f),
                            WellnessTheme.glassDark.copy(alpha = 0.3f)
                        )
                    )
                }
            )
            .border(
                width = 2.dp,
                color = if (isChecked) color else WellnessTheme.glassBorder,
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.animation.AnimatedVisibility(
            visible = isChecked,
            enter = scaleIn() + fadeIn(),
            exit = scaleOut() + fadeOut()
        ) {
            // Check mark as custom shape
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(Color.White)
            )
        }
    }
}

@Composable
private fun EmptyHabitsCard() {
    GlassPanel(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Custom empty state indicator
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                WellnessTheme.accentPurple.copy(alpha = 0.2f),
                                WellnessTheme.accentPurple.copy(alpha = 0.05f)
                            )
                        )
                    )
                    .border(2.dp, WellnessTheme.accentPurple.copy(alpha = 0.3f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "✨", fontSize = 28.sp)
            }
            
            Spacer(Modifier.height(16.dp))
            
            Text(
                text = "Пока нет привычек",
                style = GlassTypography.titleSmall
            )
            
            Spacer(Modifier.height(8.dp))
            
            Text(
                text = "Создай первую и начни трекать!",
                style = GlassTypography.labelSmall,
                color = GlassColors.textSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun AddHabitButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        WellnessTheme.accentPurple,
                        WellnessTheme.accentBlue
                    )
                )
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Plus indicator
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "+",
                    style = GlassTypography.titleSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.width(10.dp))
            Text(
                text = "Новая привычка",
                style = GlassTypography.labelMedium,
                color = Color.White,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

// ============================================================================
// 🌅 DIGEST PAGE
// ============================================================================

@Composable
private fun DigestPage(
    digest: DailyDigest?,
    onRefresh: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (digest != null) {
            // Greeting
            item {
                DigestGreetingCard(greeting = digest.greeting)
            }
            
            // Weather + Quote
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    digest.weather?.let { weather ->
                        WeatherCard(weather = weather, modifier = Modifier.weight(1f))
                    }
                    digest.quote?.let { quote ->
                        QuoteCard(
                            quote = quote,
                            modifier = Modifier.weight(if (digest.weather != null) 1.4f else 1f)
                        )
                    }
                }
            }
            
            // Habits summary
            digest.habitsSummary?.let { summary ->
                item {
                    HabitsSummaryCard(summary = summary)
                }
            }
            
            // Insights
            if (digest.insights.isNotEmpty()) {
                item {
                    InsightsCard(insights = digest.insights)
                }
            }
        } else {
            item {
                GlassPanel(modifier = Modifier.fillMaxWidth().height(180.dp)) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            GlassLoadingIndicator()
                            Spacer(Modifier.height(16.dp))
                            Text(
                                text = "Загружаем...",
                                style = GlassTypography.labelSmall,
                                color = GlassColors.textSecondary
                            )
                        }
                    }
                }
            }
        }
        
        // Refresh
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(WellnessTheme.glassDark.copy(alpha = 0.5f))
                    .border(1.dp, WellnessTheme.glassBorder.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                    .clickable { onRefresh() }
                    .padding(14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "↻ Обновить",
                    style = GlassTypography.labelSmall,
                    color = GlassColors.textSecondary
                )
            }
        }
        
        item { Spacer(Modifier.height(100.dp)) }
    }
}

@Composable
private fun DigestGreetingCard(greeting: String) {
    GlassPanel(
        modifier = Modifier.fillMaxWidth(),
        accentColor = WellnessTheme.accentOrange
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = greeting,
                style = GlassTypography.titleLarge.copy(fontSize = 22.sp),
                textAlign = TextAlign.Center,
                lineHeight = 30.sp
            )
        }
    }
}

@Composable
private fun WeatherCard(weather: WeatherInfo, modifier: Modifier = Modifier) {
    GlassPanel(modifier = modifier, accentColor = WellnessTheme.accentBlue) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = weather.emoji, fontSize = 36.sp)
            Spacer(Modifier.height(8.dp))
            Text(
                text = "${weather.temp}°",
                style = GlassTypography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = weather.city,
                style = GlassTypography.timestamp,
                color = GlassColors.textSecondary
            )
        }
    }
}

@Composable
private fun QuoteCard(quote: QuoteInfo, modifier: Modifier = Modifier) {
    GlassPanel(modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "💬", fontSize = 20.sp)
            Spacer(Modifier.height(8.dp))
            Text(
                text = "\"${quote.text}\"",
                style = GlassTypography.labelSmall.copy(
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    lineHeight = 18.sp
                ),
                color = GlassColors.textSecondary,
                maxLines = 3
            )
        }
    }
}

@Composable
private fun HabitsSummaryCard(summary: HabitsSummary) {
    GlassPanel(modifier = Modifier.fillMaxWidth(), accentColor = WellnessTheme.accentMint) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Привычки вчера",
                    style = GlassTypography.titleSmall
                )
                Text(
                    text = "${summary.completedYesterday}/${summary.total}",
                    style = GlassTypography.titleSmall,
                    color = WellnessTheme.accentMint
                )
            }
            
            if (summary.message.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                Text(
                    text = summary.message,
                    style = GlassTypography.labelSmall,
                    color = GlassColors.textSecondary
                )
            }
        }
    }
}

@Composable
private fun InsightsCard(insights: List<String>) {
    GlassPanel(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "💡 Инсайты",
                style = GlassTypography.titleSmall
            )
            Spacer(Modifier.height(12.dp))
            insights.forEach { insight ->
                Text(
                    text = insight,
                    style = GlassTypography.labelSmall,
                    color = GlassColors.textSecondary,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }
    }
}

// ============================================================================
// 📝 MOOD SHEET
// ============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MoodSheet(
    onDismiss: () -> Unit,
    onSave: (mood: Int, energy: Int?, stress: Int?, activities: List<String>, note: String?) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val haptic = LocalHapticFeedback.current
    
    var selectedMood by remember { mutableStateOf(3) }
    var selectedActivities by remember { mutableStateOf<Set<String>>(emptySet()) }
    var note by remember { mutableStateOf("") }
    
    val activities = listOf("Работа", "Спорт", "Учёба", "Общение", "Отдых", "Сон")
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = WellnessTheme.glassDark,
        dragHandle = {
            Box(
                Modifier
                    .padding(vertical = 12.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(WellnessTheme.glassBorder)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .navigationBarsPadding()
        ) {
            Text(
                text = "Как настроение?",
                style = GlassTypography.titleMedium,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            
            Spacer(Modifier.height(28.dp))
            
            // Mood selection with custom orbs
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                (1..5).forEach { level ->
                    val isSelected = selectedMood == level
                    val scale by animateFloatAsState(
                        targetValue = if (isSelected) 1.15f else 1f,
                        label = "scale"
                    )
                    
                    Box(
                        modifier = Modifier
                            .scale(scale)
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                selectedMood = level
                            }
                    ) {
                        MoodOrb(
                            level = level,
                            size = if (isSelected) 56.dp else 48.dp,
                            showGlow = isSelected
                        )
                    }
                }
            }
            
            Spacer(Modifier.height(28.dp))
            
            // Activities
            Text(
                text = "Чем занимался?",
                style = GlassTypography.labelMedium,
                color = GlassColors.textSecondary
            )
            Spacer(Modifier.height(12.dp))
            
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(activities) { activity ->
                    val isSelected = activity in selectedActivities
                    val color = if (isSelected) WellnessTheme.accentPurple else WellnessTheme.glassBorder
                    
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(color.copy(alpha = if (isSelected) 0.2f else 0.1f))
                            .border(1.dp, color.copy(alpha = if (isSelected) 0.5f else 0.3f), RoundedCornerShape(12.dp))
                            .clickable {
                                selectedActivities = if (isSelected) {
                                    selectedActivities - activity
                                } else {
                                    selectedActivities + activity
                                }
                            }
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = activity,
                            style = GlassTypography.labelSmall,
                            color = if (isSelected) WellnessTheme.accentPurple else GlassColors.textSecondary
                        )
                    }
                }
            }
            
            Spacer(Modifier.height(20.dp))
            
            // Note
            GlassTextField(
                value = note,
                onValueChange = { note = it },
                placeholder = "Заметка (опционально)",
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(Modifier.height(24.dp))
            
            // Save
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                WellnessTheme.moodColors[selectedMood - 1],
                                WellnessTheme.moodColors[selectedMood - 1].copy(alpha = 0.7f)
                            )
                        )
                    )
                    .clickable {
                        onSave(selectedMood, null, null, selectedActivities.toList(), note.takeIf { it.isNotBlank() })
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Сохранить",
                    style = GlassTypography.labelMedium,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

// ============================================================================
// ➕ HABIT SHEET  
// ============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HabitSheet(
    onDismiss: () -> Unit,
    onSave: (name: String, emoji: String, color: String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    var name by remember { mutableStateOf("") }
    var selectedEmoji by remember { mutableStateOf("✅") }
    var selectedColorIndex by remember { mutableStateOf(0) }
    
    val emojis = listOf("✅", "💧", "🏃", "📚", "🧘", "💪", "🥗", "😴", "💊", "🎯")
    val colors = listOf("#8B7CF6", "#6C9EF8", "#6ED4A8", "#E879A9", "#F5A962", "#FF6B6B")
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = WellnessTheme.glassDark,
        dragHandle = {
            Box(
                Modifier
                    .padding(vertical = 12.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(WellnessTheme.glassBorder)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .navigationBarsPadding()
        ) {
            Text(
                text = "Новая привычка",
                style = GlassTypography.titleMedium,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            
            Spacer(Modifier.height(24.dp))
            
            // Name
            GlassTextField(
                value = name,
                onValueChange = { name = it },
                placeholder = "Название",
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            Spacer(Modifier.height(20.dp))
            
            // Emoji
            Text(
                text = "Иконка",
                style = GlassTypography.labelMedium,
                color = GlassColors.textSecondary
            )
            Spacer(Modifier.height(10.dp))
            
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(emojis) { emoji ->
                    val isSelected = emoji == selectedEmoji
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isSelected) WellnessTheme.accentPurple.copy(alpha = 0.2f)
                                else WellnessTheme.glassDark.copy(alpha = 0.5f)
                            )
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) WellnessTheme.accentPurple
                                       else WellnessTheme.glassBorder.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { selectedEmoji = emoji },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = emoji, fontSize = 22.sp)
                    }
                }
            }
            
            Spacer(Modifier.height(20.dp))
            
            // Color
            Text(
                text = "Цвет",
                style = GlassTypography.labelMedium,
                color = GlassColors.textSecondary
            )
            Spacer(Modifier.height(10.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                colors.forEachIndexed { index, colorHex ->
                    val color = Color(android.graphics.Color.parseColor(colorHex))
                    val isSelected = index == selectedColorIndex
                    
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(color, color.copy(alpha = 0.6f))
                                )
                            )
                            .border(
                                width = if (isSelected) 3.dp else 0.dp,
                                color = Color.White,
                                shape = CircleShape
                            )
                            .clickable { selectedColorIndex = index },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(Color.White)
                            )
                        }
                    }
                }
            }
            
            Spacer(Modifier.height(28.dp))
            
            // Create
            val canCreate = name.isNotBlank()
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        if (canCreate) {
                            Brush.horizontalGradient(
                                colors = listOf(
                                    WellnessTheme.accentPurple,
                                    WellnessTheme.accentBlue
                                )
                            )
                        } else {
                            Brush.horizontalGradient(
                                colors = listOf(
                                    WellnessTheme.glassBorder.copy(alpha = 0.5f),
                                    WellnessTheme.glassBorder.copy(alpha = 0.3f)
                                )
                            )
                        }
                    )
                    .then(
                        if (canCreate) Modifier.clickable {
                            onSave(name, selectedEmoji, colors[selectedColorIndex])
                        } else Modifier
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Создать",
                    style = GlassTypography.labelMedium,
                    color = if (canCreate) Color.White else GlassColors.textMuted,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
