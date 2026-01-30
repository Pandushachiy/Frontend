package com.health.companion.presentation.screens.medical

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.health.companion.data.remote.api.EmergencyInfoResponse
import com.health.companion.data.remote.api.FirstAidItem
import com.health.companion.presentation.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmergencyScreen(
    onBack: () -> Unit = {},
    viewModel: MedicalViewModel = hiltViewModel()
) {
    val emergencyInfo by viewModel.emergencyInfo.collectAsState()
    val context = LocalContext.current
    
    GlassBackground {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Bar
            TopAppBar(
                title = { Text("🚨 Экстренная помощь", style = GlassTypography.heading) },
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
                // Emergency numbers
                item {
                    EmergencyNumbersCard(
                        emergencyInfo = emergencyInfo,
                        onCall = { number ->
                            val intent = Intent(Intent.ACTION_DIAL).apply {
                                data = Uri.parse("tel:$number")
                            }
                            context.startActivity(intent)
                        }
                    )
                }
                
                // When to call emergency
                item {
                    WhenToCallCard(
                        reasons = emergencyInfo?.whenToCallEmergency ?: emptyList()
                    )
                }
                
                // FAST stroke check
                item {
                    emergencyInfo?.fastStrokeCheck?.let { fast ->
                        FastStrokeCard(fast = fast)
                    }
                }
                
                // First aid basics
                emergencyInfo?.firstAidBasics?.let { items ->
                    if (items.isNotEmpty()) {
                        item {
                            Text(
                                text = "🩹 Первая помощь",
                                style = GlassTypography.titleSmall,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                        
                        items(items) { item ->
                            FirstAidCard(item = item)
                        }
                    }
                }
                
                // Disclaimer
                item {
                    DisclaimerCard()
                }
            }
        }
    }
}

@Composable
private fun EmergencyNumbersCard(
    emergencyInfo: EmergencyInfoResponse?,
    onCall: (String) -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .scale(pulseScale)
            .clip(GlassShapes.card)
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFB71C1C).copy(alpha = 0.2f),
                        Color(0xFFF44336).copy(alpha = 0.1f)
                    )
                ),
                shape = GlassShapes.card
            )
            .border(
                width = 2.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFB71C1C).copy(alpha = 0.6f),
                        Color(0xFFF44336).copy(alpha = 0.4f)
                    )
                ),
                shape = GlassShapes.card
            )
            .padding(20.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "📞", fontSize = 28.sp)
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Экстренные номера",
                    style = GlassTypography.titleMedium.copy(
                        color = GlassColors.coral
                    )
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            emergencyInfo?.emergencyNumbers?.let { numbers ->
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    EmergencyNumberButton(
                        number = numbers.ambulance,
                        label = "Скорая помощь",
                        icon = "🚑",
                        color = Color(0xFFF44336),
                        onClick = { onCall(numbers.ambulance) }
                    )
                    
                    EmergencyNumberButton(
                        number = numbers.universal,
                        label = "Единый номер экстренной помощи",
                        icon = "🆘",
                        color = Color(0xFFFF5722),
                        onClick = { onCall(numbers.universal) }
                    )
                    
                    EmergencyNumberButton(
                        number = numbers.poisonControl,
                        label = "Отравления",
                        icon = "☠️",
                        color = Color(0xFF9C27B0),
                        onClick = { onCall(numbers.poisonControl) }
                    )
                }
            }
        }
    }
}

@Composable
private fun EmergencyNumberButton(
    number: String,
    label: String,
    icon: String,
    color: Color,
    onClick: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label = "scale"
    )
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(GlassShapes.large)
            .background(color.copy(alpha = 0.15f), GlassShapes.large)
            .border(1.dp, color.copy(alpha = 0.4f), GlassShapes.large)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = icon, fontSize = 28.sp)
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = number,
                style = GlassTypography.titleLarge.copy(
                    color = color,
                    fontWeight = FontWeight.Bold
                )
            )
            Text(
                text = label,
                style = GlassTypography.labelSmall
            )
        }
        
        Icon(
            imageVector = Icons.Default.Phone,
            contentDescription = "Позвонить",
            tint = color,
            modifier = Modifier.size(28.dp)
        )
    }
}

@Composable
private fun WhenToCallCard(reasons: List<String>) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        borderColor = GlassColors.warning.copy(alpha = 0.3f)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "⚠️", fontSize = 24.sp)
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Когда вызывать скорую?",
                    style = GlassTypography.titleSmall.copy(color = GlassColors.warning)
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            if (reasons.isNotEmpty()) {
                reasons.forEach { reason ->
                    Row(
                        modifier = Modifier.padding(vertical = 4.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(top = 6.dp)
                                .size(8.dp)
                                .background(GlassColors.warning, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = reason, style = GlassTypography.messageText)
                    }
                }
            } else {
                // Default reasons
                listOf(
                    "Потеря сознания",
                    "Сильная боль в груди",
                    "Затруднённое дыхание",
                    "Сильное кровотечение",
                    "Подозрение на инсульт",
                    "Тяжёлая травма"
                ).forEach { reason ->
                    Row(
                        modifier = Modifier.padding(vertical = 4.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(top = 6.dp)
                                .size(8.dp)
                                .background(GlassColors.warning, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = reason, style = GlassTypography.messageText)
                    }
                }
            }
        }
    }
}

@Composable
private fun FastStrokeCard(fast: com.health.companion.data.remote.api.FastStrokeCheck) {
    val gradient = listOf(Color(0xFFF44336), Color(0xFFE91E63))
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(GlassShapes.card)
            .background(
                brush = Brush.linearGradient(gradient.map { it.copy(alpha = 0.1f) }),
                shape = GlassShapes.card
            )
            .border(
                width = 1.5.dp,
                brush = Brush.linearGradient(gradient.map { it.copy(alpha = 0.5f) }),
                shape = GlassShapes.card
            )
            .padding(20.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "🧠", fontSize = 28.sp)
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Тест FAST",
                        style = GlassTypography.titleMedium.copy(color = gradient.first())
                    )
                    Text(
                        text = "Распознаём инсульт",
                        style = GlassTypography.labelSmall
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // FAST items
            FastItem(letter = "F", title = "Face (Лицо)", description = fast.face, color = Color(0xFFF44336))
            FastItem(letter = "A", title = "Arms (Руки)", description = fast.arms, color = Color(0xFFE91E63))
            FastItem(letter = "S", title = "Speech (Речь)", description = fast.speech, color = Color(0xFF9C27B0))
            FastItem(letter = "T", title = "Time (Время)", description = fast.time, color = Color(0xFF673AB7))
        }
    }
}

@Composable
private fun FastItem(
    letter: String,
    title: String,
    description: String,
    color: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(color.copy(alpha = 0.2f), CircleShape)
                .border(1.dp, color.copy(alpha = 0.5f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = letter,
                style = GlassTypography.titleSmall.copy(
                    color = color,
                    fontWeight = FontWeight.Bold
                )
            )
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column {
            Text(
                text = title,
                style = GlassTypography.labelMedium.copy(color = color)
            )
            Text(
                text = description,
                style = GlassTypography.labelSmall
            )
        }
    }
}

@Composable
private fun FirstAidCard(item: FirstAidItem) {
    var isExpanded by remember { mutableStateOf(false) }
    
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .clickable { isExpanded = !isExpanded }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "🩹", fontSize = 24.sp)
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = item.situation,
                    style = GlassTypography.labelMedium,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = GlassColors.textTertiary
                )
            }
            
            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    GlassDivider()
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    item.steps.forEachIndexed { index, step ->
                        Row(
                            modifier = Modifier.padding(vertical = 4.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .background(GlassColors.accent.copy(alpha = 0.2f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${index + 1}",
                                    style = GlassTypography.labelSmall.copy(
                                        color = GlassColors.accent,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = step,
                                style = GlassTypography.messageText
                            )
                        }
                    }
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
            .background(GlassColors.textTertiary.copy(alpha = 0.1f))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Text(text = "ℹ️", fontSize = 16.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Информация носит справочный характер. В экстренной ситуации немедленно вызывайте скорую помощь!",
                style = GlassTypography.labelSmall.copy(color = GlassColors.textTertiary)
            )
        }
    }
}
