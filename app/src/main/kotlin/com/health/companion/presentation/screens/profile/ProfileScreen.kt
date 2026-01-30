package com.health.companion.presentation.screens.profile

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.health.companion.data.remote.api.*
import com.health.companion.presentation.components.*
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onNavigateToQuestionnaire: () -> Unit = {},
    onNavigateToDates: () -> Unit = {},
    onNavigateToPeople: () -> Unit = {},
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val profile by viewModel.profile.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val patterns by viewModel.patterns.collectAsState()
    val importantDates by viewModel.importantDates.collectAsState()
    val importantPeople by viewModel.importantPeople.collectAsState()
    
    LaunchedEffect(Unit) {
        viewModel.loadPatterns()
    }
    
    GlassBackground {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            // Header
            item {
                ProfileHeader(profile = profile, isLoading = isLoading)
            }
            
            // Quick Stats
            item {
                QuickStatsRow(patterns = patterns)
            }
            
            // Actions Grid
            item {
                ProfileActionsGrid(
                    onEditProfile = onNavigateToQuestionnaire,
                    onDates = onNavigateToDates,
                    onPeople = onNavigateToPeople
                )
            }
            
            // Upcoming Dates
            if (importantDates.isNotEmpty()) {
                item {
                    UpcomingDatesSection(dates = importantDates.take(3))
                }
            }
            
            // Important People
            if (importantPeople.isNotEmpty()) {
                item {
                    ImportantPeopleSection(people = importantPeople.take(5))
                }
            }
            
            // Health Summary
            item {
                HealthSummaryCard(profile = profile)
            }
            
            // Goals
            item {
                GoalsCard(profile = profile)
            }
            
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

@Composable
private fun ProfileHeader(profile: UserProfile?, isLoading: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "avatar")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )
    
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = GlassShapes.card
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Animated Avatar
            Box(contentAlignment = Alignment.Center) {
                // Glow effect
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    GlassColors.accent.copy(alpha = glowAlpha),
                                    Color.Transparent
                                )
                            ),
                            shape = CircleShape
                        )
                        .blur(20.dp)
                )
                
                // Avatar
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    GlassColors.accent,
                                    GlassColors.accentSecondary
                                )
                            ),
                            shape = CircleShape
                        )
                        .border(2.dp, GlassColors.whiteOverlay20, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = profile?.preferredName?.firstOrNull()?.uppercase() ?: "👤",
                        style = GlassTypography.titleLarge.copy(
                            fontSize = 32.sp,
                            color = Color.White
                        )
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Name
            Text(
                text = profile?.preferredName ?: "Настройте профиль",
                style = GlassTypography.titleLarge
            )
            
            // Location & Age
            if (profile != null) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    profile.location?.let {
                        GlassChip(text = "📍 $it", color = GlassColors.teal)
                    }
                    profile.age?.let {
                        GlassChip(text = "$it лет", color = GlassColors.accent)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Profile completion
            ProfileCompletionIndicator(profile = profile)
        }
    }
}

@Composable
private fun ProfileCompletionIndicator(profile: UserProfile?) {
    val completion = calculateProfileCompletion(profile)
    val animatedCompletion by animateFloatAsState(
        targetValue = completion,
        animationSpec = tween(1000, easing = FastOutSlowInEasing),
        label = "completion"
    )
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Заполненность профиля",
                style = GlassTypography.labelSmall
            )
            Text(
                text = "${(animatedCompletion * 100).toInt()}%",
                style = GlassTypography.labelSmall.copy(color = GlassColors.accent)
            )
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(GlassColors.whiteOverlay10)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(animatedCompletion)
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(GlassColors.accent, GlassColors.mint)
                        ),
                        shape = RoundedCornerShape(3.dp)
                    )
            )
        }
    }
}

@Composable
private fun QuickStatsRow(patterns: LifePatternsResponse?) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        QuickStatCard(
            modifier = Modifier.weight(1f),
            icon = "😊",
            label = "Настроение",
            value = patterns?.avgMood?.let { "%.1f".format(it) } ?: "—",
            trend = patterns?.moodTrend,
            color = GlassColors.mint
        )
        
        QuickStatCard(
            modifier = Modifier.weight(1f),
            icon = "⚡",
            label = "Стресс",
            value = patterns?.avgStress?.let { "%.1f".format(it) } ?: "—",
            color = GlassColors.orange
        )
    }
}

@Composable
private fun QuickStatCard(
    modifier: Modifier = Modifier,
    icon: String,
    label: String,
    value: String,
    trend: String? = null,
    color: Color
) {
    GlassCard(
        modifier = modifier,
        backgroundColor = color.copy(alpha = 0.1f),
        borderColor = color.copy(alpha = 0.3f)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = icon, fontSize = 28.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = GlassTypography.titleMedium.copy(color = color)
            )
            Text(
                text = label,
                style = GlassTypography.labelSmall
            )
            trend?.let {
                val trendIcon = when (it) {
                    "improving" -> "📈"
                    "declining" -> "📉"
                    else -> "➡️"
                }
                Text(text = trendIcon, fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun ProfileActionsGrid(
    onEditProfile: () -> Unit,
    onDates: () -> Unit,
    onPeople: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ProfileActionCard(
                modifier = Modifier.weight(1f),
                icon = "✏️",
                title = "Редактировать",
                subtitle = "Анкета профиля",
                gradient = listOf(Color(0xFF667eea), Color(0xFF764ba2)),
                onClick = onEditProfile
            )
            ProfileActionCard(
                modifier = Modifier.weight(1f),
                icon = "📅",
                title = "Важные даты",
                subtitle = "ДР, годовщины",
                gradient = listOf(Color(0xFFf093fb), Color(0xFFf5576c)),
                onClick = onDates
            )
        }
        
        ProfileActionCard(
            modifier = Modifier.fillMaxWidth(),
            icon = "👥",
            title = "Близкие люди",
            subtitle = "Семья и друзья",
            gradient = listOf(Color(0xFF4facfe), Color(0xFF00f2fe)),
            onClick = onPeople
        )
    }
}

@Composable
private fun ProfileActionCard(
    modifier: Modifier = Modifier,
    icon: String,
    title: String,
    subtitle: String,
    gradient: List<Color>,
    onClick: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label = "scale"
    )
    
    Box(
        modifier = modifier
            .scale(scale)
            .clip(GlassShapes.large)
            .background(
                brush = Brush.linearGradient(gradient.map { it.copy(alpha = 0.2f) }),
                shape = GlassShapes.large
            )
            .border(1.dp, gradient.first().copy(alpha = 0.4f), GlassShapes.large)
            .clickable(
                onClick = onClick,
                indication = null,
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
            )
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = icon, fontSize = 32.sp)
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(text = title, style = GlassTypography.labelMedium)
                Text(text = subtitle, style = GlassTypography.labelSmall)
            }
        }
    }
}

@Composable
private fun UpcomingDatesSection(dates: List<ImportantDate>) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "📅 Ближайшие даты", style = GlassTypography.titleSmall)
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(dates) { date ->
                DateCard(date = date)
            }
        }
    }
}

@Composable
private fun DateCard(date: ImportantDate) {
    val typeEmoji = when (date.eventType) {
        "birthday" -> "🎂"
        "anniversary" -> "💍"
        else -> "📌"
    }
    
    val daysColor = when {
        date.daysUntil == null -> GlassColors.textTertiary
        date.daysUntil!! <= 7 -> GlassColors.coral
        date.daysUntil!! <= 30 -> GlassColors.orange
        else -> GlassColors.mint
    }
    
    GlassCard(
        modifier = Modifier.width(140.dp),
        borderColor = daysColor.copy(alpha = 0.3f)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = typeEmoji, fontSize = 28.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = date.title,
                style = GlassTypography.labelMedium,
                maxLines = 1,
                textAlign = TextAlign.Center
            )
            Text(
                text = date.daysUntil?.let { "через $it дн." } ?: date.date,
                style = GlassTypography.labelSmall.copy(color = daysColor)
            )
        }
    }
}

@Composable
private fun ImportantPeopleSection(people: List<ImportantPerson>) {
    Column {
        Text(text = "👥 Близкие люди", style = GlassTypography.titleSmall)
        
        Spacer(modifier = Modifier.height(12.dp))
        
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(people) { person ->
                PersonCard(person = person)
            }
        }
    }
}

@Composable
private fun PersonCard(person: ImportantPerson) {
    GlassCard(modifier = Modifier.width(100.dp)) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(GlassColors.accent.copy(alpha = 0.2f), CircleShape)
                    .border(1.dp, GlassColors.accent.copy(alpha = 0.3f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = person.name.firstOrNull()?.uppercase() ?: "?",
                    style = GlassTypography.titleSmall.copy(color = GlassColors.accent)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = person.name,
                style = GlassTypography.labelMedium,
                maxLines = 1
            )
            Text(
                text = person.relation,
                style = GlassTypography.labelSmall,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun HealthSummaryCard(profile: UserProfile?) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "🏥", fontSize = 24.sp)
                Spacer(modifier = Modifier.width(12.dp))
                Text(text = "Здоровье", style = GlassTypography.titleSmall)
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            if (profile?.healthConditions?.isNotEmpty() == true) {
                Text(text = "Состояния:", style = GlassTypography.labelSmall)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(profile.healthConditions) { condition ->
                        GlassChip(text = condition, color = GlassColors.orange)
                    }
                }
            }
            
            if (profile?.allergies?.isNotEmpty() == true) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "Аллергии:", style = GlassTypography.labelSmall)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(profile.allergies) { allergy ->
                        GlassChip(text = allergy, color = GlassColors.coral)
                    }
                }
            }
            
            if (profile?.currentMedications?.isNotEmpty() == true) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "Лекарства:", style = GlassTypography.labelSmall)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(profile.currentMedications) { med ->
                        GlassChip(text = "💊 $med", color = GlassColors.teal)
                    }
                }
            }
            
            if (profile?.healthConditions.isNullOrEmpty() && 
                profile?.allergies.isNullOrEmpty() && 
                profile?.currentMedications.isNullOrEmpty()) {
                Text(
                    text = "Заполните анкету, чтобы AI лучше понимал вас",
                    style = GlassTypography.labelSmall
                )
            }
        }
    }
}

@Composable
private fun GoalsCard(profile: UserProfile?) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "🎯", fontSize = 24.sp)
                Spacer(modifier = Modifier.width(12.dp))
                Text(text = "Цели", style = GlassTypography.titleSmall)
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            if (profile?.shortTermGoals?.isNotEmpty() == true) {
                Text(text = "Краткосрочные:", style = GlassTypography.labelSmall)
                profile.shortTermGoals.forEach { goal ->
                    Row(
                        modifier = Modifier.padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(GlassColors.mint, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = goal, style = GlassTypography.messageText)
                    }
                }
            }
            
            if (profile?.longTermGoals?.isNotEmpty() == true) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "Долгосрочные:", style = GlassTypography.labelSmall)
                profile.longTermGoals.forEach { goal ->
                    Row(
                        modifier = Modifier.padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(GlassColors.accent, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = goal, style = GlassTypography.messageText)
                    }
                }
            }
            
            if (profile?.shortTermGoals.isNullOrEmpty() && profile?.longTermGoals.isNullOrEmpty()) {
                Text(
                    text = "Добавьте цели в анкете",
                    style = GlassTypography.labelSmall
                )
            }
        }
    }
}

// Helper function
private fun calculateProfileCompletion(profile: UserProfile?): Float {
    if (profile == null) return 0f
    
    var filled = 0
    var total = 10
    
    if (!profile.preferredName.isNullOrBlank()) filled++
    if (profile.age != null) filled++
    if (!profile.location.isNullOrBlank()) filled++
    if (profile.healthConditions.isNotEmpty()) filled++
    if (profile.allergies.isNotEmpty()) filled++
    if (profile.shortTermGoals.isNotEmpty()) filled++
    if (profile.longTermGoals.isNotEmpty()) filled++
    if (profile.hobbies.isNotEmpty()) filled++
    if (!profile.occupation.isNullOrBlank()) filled++
    if (!profile.livingSituation.isNullOrBlank()) filled++
    
    return filled.toFloat() / total
}
