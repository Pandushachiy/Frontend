package com.health.companion.presentation.screens.medical

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.health.companion.presentation.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicalAssistantScreen(
    onNavigateToSymptoms: () -> Unit = {},
    onNavigateToDrugs: () -> Unit = {},
    onNavigateToLab: () -> Unit = {},
    onNavigateToRecommendations: () -> Unit = {},
    onNavigateToEmergency: () -> Unit = {},
    viewModel: MedicalViewModel = hiltViewModel()
) {
    val emergencyInfo by viewModel.emergencyInfo.collectAsState()
    
    GlassBackground {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            // Header with animation
            item {
                MedicalHeader()
            }
            
            // Disclaimer
            item {
                DisclaimerCard()
            }
            
            // Main Actions
            item {
                Text(
                    text = "🔍 Что вас беспокоит?",
                    style = GlassTypography.titleSmall,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            
            // Symptom Checker - Primary
            item {
                PrimaryActionCard(
                    emoji = "🩺",
                    title = "Проверка симптомов",
                    subtitle = "Опишите симптомы и получите рекомендации",
                    gradient = listOf(Color(0xFF667eea), Color(0xFF764ba2)),
                    onClick = onNavigateToSymptoms
                )
            }
            
            // Drug Interactions
            item {
                PrimaryActionCard(
                    emoji = "💊",
                    title = "Проверка лекарств",
                    subtitle = "Проверьте совместимость препаратов",
                    gradient = listOf(Color(0xFF11998e), Color(0xFF38ef7d)),
                    onClick = onNavigateToDrugs
                )
            }
            
            // Secondary Actions Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SecondaryActionCard(
                        modifier = Modifier.weight(1f),
                        emoji = "🔬",
                        title = "Анализы",
                        onClick = onNavigateToLab
                    )
                    SecondaryActionCard(
                        modifier = Modifier.weight(1f),
                        emoji = "💪",
                        title = "Советы",
                        onClick = onNavigateToRecommendations
                    )
                }
            }
            
            // Emergency Section
            item {
                EmergencyCard(
                    emergencyInfo = emergencyInfo,
                    onClick = onNavigateToEmergency
                )
            }
            
            // Quick Tips
            item {
                QuickTipsSection()
            }
            
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

@Composable
private fun MedicalHeader() {
    val infiniteTransition = rememberInfiniteTransition(label = "header")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )
    
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )
    
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = GlassShapes.card
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Background glow
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .align(Alignment.CenterEnd)
                    .offset(x = 20.dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF4facfe).copy(alpha = glowAlpha),
                                Color.Transparent
                            )
                        )
                    )
                    .blur(40.dp)
            )
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "🏥 Медицинский",
                        style = GlassTypography.titleLarge
                    )
                    Text(
                        text = "помощник",
                        style = GlassTypography.titleLarge.copy(
                            color = GlassColors.accent
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Информационная поддержка для вашего здоровья",
                        style = GlassTypography.labelSmall
                    )
                }
                
                // Animated cross
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .scale(pulseScale),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "⚕️", fontSize = 48.sp)
                }
            }
        }
    }
}

@Composable
private fun DisclaimerCard() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(GlassShapes.medium)
            .background(
                color = GlassColors.warning.copy(alpha = 0.1f),
                shape = GlassShapes.medium
            )
            .border(
                width = 1.dp,
                color = GlassColors.warning.copy(alpha = 0.3f),
                shape = GlassShapes.medium
            )
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Text(text = "⚠️", fontSize = 20.sp)
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "Важно!",
                    style = GlassTypography.labelMedium.copy(color = GlassColors.warning)
                )
                Text(
                    text = "Это НЕ медицинский диагноз. Всегда консультируйтесь с врачом перед принятием решений о лечении.",
                    style = GlassTypography.labelSmall
                )
            }
        }
    }
}

@Composable
private fun PrimaryActionCard(
    emoji: String,
    title: String,
    subtitle: String,
    gradient: List<Color>,
    onClick: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label = "scale"
    )
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(GlassShapes.large)
            .background(
                brush = Brush.linearGradient(
                    colors = gradient.map { it.copy(alpha = 0.15f) },
                    start = Offset.Zero,
                    end = Offset(500f, 500f)
                ),
                shape = GlassShapes.large
            )
            .border(
                width = 1.5.dp,
                brush = Brush.linearGradient(gradient.map { it.copy(alpha = 0.5f) }),
                shape = GlassShapes.large
            )
            .clickable(onClick = onClick)
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Icon with glow
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                gradient.first().copy(alpha = 0.3f),
                                Color.Transparent
                            )
                        ),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(text = emoji, fontSize = 36.sp)
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = GlassTypography.titleSmall
                )
                Text(
                    text = subtitle,
                    style = GlassTypography.labelSmall
                )
            }
            
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = gradient.first()
            )
        }
    }
}

@Composable
private fun SecondaryActionCard(
    modifier: Modifier = Modifier,
    emoji: String,
    title: String,
    onClick: () -> Unit
) {
    GlassCard(
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = emoji, fontSize = 32.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                style = GlassTypography.labelMedium,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun EmergencyCard(
    emergencyInfo: com.health.companion.data.remote.api.EmergencyInfoResponse?,
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "emergency")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(GlassShapes.large)
            .background(
                color = GlassColors.coral.copy(alpha = 0.1f),
                shape = GlassShapes.large
            )
            .border(
                width = 2.dp,
                color = GlassColors.coral.copy(alpha = pulseAlpha * 0.5f),
                shape = GlassShapes.large
            )
            .clickable(onClick = onClick)
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Pulsing icon
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(
                        color = GlassColors.coral.copy(alpha = pulseAlpha * 0.3f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "🚨", fontSize = 28.sp)
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Экстренная помощь",
                    style = GlassTypography.titleSmall.copy(color = GlassColors.coral)
                )
                Text(
                    text = "Важные номера и инструкции",
                    style = GlassTypography.labelSmall
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Quick numbers
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    emergencyInfo?.emergencyNumbers?.let { numbers ->
                        EmergencyNumber(number = numbers.ambulance, label = "Скорая")
                        EmergencyNumber(number = numbers.universal, label = "Экстренная")
                    }
                }
            }
        }
    }
}

@Composable
private fun EmergencyNumber(number: String, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(GlassColors.coral.copy(alpha = 0.2f))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = "📞",
            fontSize = 12.sp
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = number,
            style = GlassTypography.labelSmall.copy(
                color = GlassColors.coral,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
            )
        )
    }
}

@Composable
private fun QuickTipsSection() {
    Column {
        Text(
            text = "💡 Полезные советы",
            style = GlassTypography.titleSmall,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        
        val tips = listOf(
            "🩺" to "Описывайте симптомы подробно — когда начались, как часто",
            "💊" to "Сообщайте обо всех лекарствах, включая витамины",
            "📝" to "Записывайте показатели здоровья регулярно",
            "👨‍⚕️" to "При серьёзных симптомах — сразу к врачу!"
        )
        
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            tips.forEach { (emoji, tip) ->
                TipRow(emoji = emoji, text = tip)
            }
        }
    }
}

@Composable
private fun TipRow(emoji: String, text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(GlassShapes.small)
            .background(GlassColors.whiteOverlay05, GlassShapes.small)
            .padding(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(text = emoji, fontSize = 16.sp)
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            style = GlassTypography.labelSmall,
            modifier = Modifier.weight(1f)
        )
    }
}
