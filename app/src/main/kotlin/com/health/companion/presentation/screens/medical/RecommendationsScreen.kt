package com.health.companion.presentation.screens.medical

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.health.companion.data.remote.api.HealthRecommendationsResponse
import com.health.companion.presentation.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecommendationsScreen(
    onBack: () -> Unit = {},
    viewModel: MedicalViewModel = hiltViewModel()
) {
    val isLoading by viewModel.isLoading.collectAsState()
    val recommendations by viewModel.recommendations.collectAsState()
    val error by viewModel.error.collectAsState()
    
    var selectedArea by remember { mutableStateOf<String?>(null) }
    
    val focusAreas = listOf(
        FocusArea("питание", "🥗", "Питание", listOf(Color(0xFF4CAF50), Color(0xFF8BC34A))),
        FocusArea("сон", "😴", "Сон", listOf(Color(0xFF3F51B5), Color(0xFF7986CB))),
        FocusArea("стресс", "🧘", "Стресс", listOf(Color(0xFF9C27B0), Color(0xFFCE93D8))),
        FocusArea("активность", "🏃", "Активность", listOf(Color(0xFFFF5722), Color(0xFFFF8A65))),
        FocusArea("профилактика", "🛡️", "Профилактика", listOf(Color(0xFF00BCD4), Color(0xFF80DEEA)))
    )
    
    GlassBackground {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Bar
            TopAppBar(
                title = { Text("💪 Рекомендации", style = GlassTypography.heading) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, null, tint = GlassColors.textPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
            
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 100.dp)
            ) {
                // Header
                item {
                    HeaderCard()
                }
                
                // Focus areas selector
                item {
                    Column {
                        Text(
                            text = "Выберите тему:",
                            style = GlassTypography.titleSmall,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(focusAreas) { area ->
                                FocusAreaCard(
                                    area = area,
                                    isSelected = selectedArea == area.key,
                                    onClick = {
                                        selectedArea = area.key
                                        viewModel.loadRecommendations(area.key)
                                    }
                                )
                            }
                        }
                    }
                }
                
                // Loading
                if (isLoading) {
                    item {
                        LoadingCard()
                    }
                }
                
                // Recommendations result
                if (recommendations != null && !isLoading) {
                    item {
                        RecommendationsResultCard(
                            result = recommendations!!,
                            focusArea = focusAreas.find { it.key == selectedArea }
                        )
                    }
                }
                
                // Error
                error?.let { errorMsg ->
                    item {
                        ErrorCard(error = errorMsg, onDismiss = { viewModel.clearError() })
                    }
                }
                
                // Default tips (if no selection)
                if (selectedArea == null && recommendations == null) {
                    item {
                        DefaultTipsCard()
                    }
                }
            }
        }
    }
}

private data class FocusArea(
    val key: String,
    val emoji: String,
    val title: String,
    val gradient: List<Color>
)

@Composable
private fun HeaderCard() {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "🌟", fontSize = 40.sp)
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = "Персональные советы",
                    style = GlassTypography.titleSmall
                )
                Text(
                    text = "AI учитывает ваш профиль здоровья для персонализированных рекомендаций",
                    style = GlassTypography.labelSmall
                )
            }
        }
    }
}

@Composable
private fun FocusAreaCard(
    area: FocusArea,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.05f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label = "scale"
    )
    
    Box(
        modifier = Modifier
            .size(100.dp)
            .scale(scale)
            .clip(GlassShapes.large)
            .background(
                brush = Brush.linearGradient(
                    area.gradient.map { it.copy(alpha = if (isSelected) 0.3f else 0.15f) }
                ),
                shape = GlassShapes.large
            )
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                brush = Brush.linearGradient(
                    area.gradient.map { it.copy(alpha = if (isSelected) 0.8f else 0.3f) }
                ),
                shape = GlassShapes.large
            )
            .clickable(onClick = onClick)
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = area.emoji, fontSize = 32.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = area.title,
                style = GlassTypography.labelSmall.copy(
                    color = if (isSelected) area.gradient.first() else GlassColors.textPrimary
                ),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun LoadingCard() {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(40.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(
                    color = GlassColors.accent,
                    modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Генерирую рекомендации...",
                    style = GlassTypography.labelMedium
                )
            }
        }
    }
}

@Composable
private fun RecommendationsResultCard(
    result: HealthRecommendationsResponse,
    focusArea: FocusArea?
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(result) { visible = true }
    
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + slideInVertically { it / 2 }
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // Main recommendations
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                borderColor = focusArea?.gradient?.first()?.copy(alpha = 0.3f) ?: GlassColors.accent.copy(alpha = 0.3f)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = focusArea?.emoji ?: "💡", fontSize = 28.sp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Рекомендации: ${result.focusArea}",
                                style = GlassTypography.titleSmall.copy(
                                    color = focusArea?.gradient?.first() ?: GlassColors.accent
                                )
                            )
                            if (result.personalized) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(text = "✨", fontSize = 12.sp)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Персонализировано для вас",
                                        style = GlassTypography.labelSmall.copy(color = GlassColors.mint)
                                    )
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = result.recommendations,
                        style = GlassTypography.messageText
                    )
                }
            }
            
            // Tips
            if (result.tips.isNotEmpty()) {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "💡 Полезные советы",
                            style = GlassTypography.titleSmall
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        result.tips.forEachIndexed { index, tip ->
                            Row(
                                modifier = Modifier.padding(vertical = 4.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .background(
                                            color = focusArea?.gradient?.first()?.copy(alpha = 0.2f) 
                                                ?: GlassColors.accent.copy(alpha = 0.2f),
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${index + 1}",
                                        style = GlassTypography.labelSmall.copy(
                                            color = focusArea?.gradient?.first() ?: GlassColors.accent
                                        )
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = tip, style = GlassTypography.messageText)
                            }
                        }
                    }
                }
            }
            
            // Disclaimer
            if (result.disclaimer.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(GlassShapes.small)
                        .background(GlassColors.textTertiary.copy(alpha = 0.1f))
                        .padding(12.dp)
                ) {
                    Text(
                        text = result.disclaimer,
                        style = GlassTypography.labelSmall.copy(color = GlassColors.textTertiary),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun DefaultTipsCard() {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "🌈 Общие советы для здоровья",
                style = GlassTypography.titleSmall
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            val tips = listOf(
                "🚶" to "Двигайтесь не менее 30 минут в день",
                "💧" to "Пейте 8 стаканов воды ежедневно",
                "😴" to "Спите 7-9 часов каждую ночь",
                "🥗" to "Ешьте 5 порций овощей и фруктов",
                "🧘" to "Практикуйте расслабление и медитацию",
                "👨‍⚕️" to "Регулярно проходите медосмотры"
            )
            
            tips.forEach { (emoji, tip) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .clip(GlassShapes.small)
                        .background(GlassColors.whiteOverlay05)
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = emoji, fontSize = 24.sp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(text = tip, style = GlassTypography.messageText)
                }
            }
        }
    }
}

@Composable
private fun ErrorCard(error: String, onDismiss: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(GlassShapes.medium)
            .background(GlassColors.error.copy(alpha = 0.1f), GlassShapes.medium)
            .border(1.dp, GlassColors.error.copy(alpha = 0.3f), GlassShapes.medium)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = "❌", fontSize = 20.sp)
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = error,
                style = GlassTypography.messageText.copy(color = GlassColors.error),
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, null, tint = GlassColors.error)
            }
        }
    }
}
