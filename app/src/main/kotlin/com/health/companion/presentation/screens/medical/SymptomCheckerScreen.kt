@file:OptIn(ExperimentalLayoutApi::class)

package com.health.companion.presentation.screens.medical

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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.health.companion.data.remote.api.SymptomCheckResponse
import com.health.companion.presentation.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SymptomCheckerScreen(
    onBack: () -> Unit = {},
    viewModel: MedicalViewModel = hiltViewModel()
) {
    val isLoading by viewModel.isLoading.collectAsState()
    val result by viewModel.symptomsResult.collectAsState()
    val error by viewModel.error.collectAsState()
    
    var symptoms by remember { mutableStateOf("") }
    var duration by remember { mutableStateOf("") }
    
    GlassBackground {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Bar
            TopAppBar(
                title = { Text("🩺 Проверка симптомов", style = GlassTypography.heading) },
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
                // Disclaimer
                item {
                    DisclaimerBanner()
                }
                
                // Input form
                item {
                    SymptomInputCard(
                        symptoms = symptoms,
                        duration = duration,
                        onSymptomsChange = { symptoms = it },
                        onDurationChange = { duration = it },
                        onCheck = { viewModel.checkSymptoms(symptoms, duration.takeIf { it.isNotBlank() }) },
                        isLoading = isLoading
                    )
                }
                
                // Quick symptoms
                item {
                    QuickSymptomsSelector(
                        onSelect = { selected -> 
                            symptoms = if (symptoms.isBlank()) selected 
                            else "$symptoms, $selected"
                        }
                    )
                }
                
                // Result
                if (result != null) {
                    item {
                        SymptomResultCard(
                            result = result!!,
                            onClear = { viewModel.clearSymptomsResult() }
                        )
                    }
                }
                
                // Error
                error?.let { errorMsg ->
                    item {
                        ErrorCard(error = errorMsg, onDismiss = { viewModel.clearError() })
                    }
                }
            }
        }
    }
}

@Composable
private fun DisclaimerBanner() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(GlassShapes.medium)
            .background(GlassColors.warning.copy(alpha = 0.1f), GlassShapes.medium)
            .border(1.dp, GlassColors.warning.copy(alpha = 0.3f), GlassShapes.medium)
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Text(text = "⚠️", fontSize = 18.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Это не медицинский диагноз. При серьёзных симптомах обратитесь к врачу!",
                style = GlassTypography.labelSmall.copy(color = GlassColors.warning)
            )
        }
    }
}

@Composable
private fun SymptomInputCard(
    symptoms: String,
    duration: String,
    onSymptomsChange: (String) -> Unit,
    onDurationChange: (String) -> Unit,
    onCheck: () -> Unit,
    isLoading: Boolean
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Опишите симптомы",
                style = GlassTypography.titleSmall
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Symptoms textarea
            OutlinedTextField(
                value = symptoms,
                onValueChange = onSymptomsChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { 
                    Text(
                        "Например: болит голова, тошнит, слабость...",
                        style = GlassTypography.placeholder
                    ) 
                },
                minLines = 3,
                maxLines = 5,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GlassColors.accent,
                    unfocusedBorderColor = GlassColors.whiteOverlay20,
                    focusedContainerColor = GlassColors.whiteOverlay05,
                    unfocusedContainerColor = GlassColors.whiteOverlay05
                ),
                shape = GlassShapes.medium
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Duration
            OutlinedTextField(
                value = duration,
                onValueChange = onDurationChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Как давно? (опционально)") },
                placeholder = { Text("Например: 3 дня, неделя...") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GlassColors.accent,
                    unfocusedBorderColor = GlassColors.whiteOverlay20,
                    focusedContainerColor = GlassColors.whiteOverlay05,
                    unfocusedContainerColor = GlassColors.whiteOverlay05
                ),
                shape = GlassShapes.medium
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Check button
            GlassButton(
                onClick = onCheck,
                modifier = Modifier.fillMaxWidth(),
                enabled = symptoms.isNotBlank() && !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Анализирую...", color = Color.White)
                } else {
                    Icon(Icons.Default.Search, null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Проверить", color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun QuickSymptomsSelector(onSelect: (String) -> Unit) {
    val quickSymptoms = listOf(
        "Головная боль", "Температура", "Кашель", "Насморк",
        "Боль в горле", "Тошнота", "Слабость", "Головокружение",
        "Боль в животе", "Бессонница"
    )
    
    Column {
        Text(
            text = "Быстрый выбор:",
            style = GlassTypography.labelSmall
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        androidx.compose.foundation.layout.FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            quickSymptoms.forEach { symptom ->
                GlassChip(
                    text = symptom,
                    color = GlassColors.textSecondary,
                    onClick = { onSelect(symptom) }
                )
            }
        }
    }
}

@Composable
private fun SymptomResultCard(
    result: SymptomCheckResponse,
    onClear: () -> Unit
) {
    val severity = Severity.fromString(result.severity)
    
    val severityGradient = when (severity) {
        Severity.LOW -> listOf(Color(0xFF4CAF50), Color(0xFF81C784))
        Severity.MEDIUM -> listOf(Color(0xFFFF9800), Color(0xFFFFB74D))
        Severity.HIGH -> listOf(Color(0xFFF44336), Color(0xFFE57373))
        Severity.URGENT -> listOf(Color(0xFFB71C1C), Color(0xFFF44336))
    }
    
    // Animate entry
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + slideInVertically { it / 2 }
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // Severity badge
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                borderColor = Color(severity.color.toInt()).copy(alpha = 0.5f)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Результат", style = GlassTypography.titleSmall)
                        IconButton(onClick = onClear) {
                            Icon(Icons.Default.Close, null, tint = GlassColors.textTertiary)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Severity indicator
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(GlassShapes.medium)
                            .background(
                                brush = Brush.horizontalGradient(
                                    severityGradient.map { it.copy(alpha = 0.2f) }
                                ),
                                shape = GlassShapes.medium
                            )
                            .border(
                                1.dp,
                                severityGradient.first().copy(alpha = 0.5f),
                                GlassShapes.medium
                            )
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = severity.icon, fontSize = 32.sp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Серьёзность: ${severity.label}",
                                style = GlassTypography.titleSmall.copy(
                                    color = Color(severity.color.toInt())
                                )
                            )
                            if (severity == Severity.URGENT) {
                                Text(
                                    text = "Требуется срочная медицинская помощь!",
                                    style = GlassTypography.labelSmall.copy(
                                        color = Color(severity.color.toInt())
                                    )
                                )
                            }
                        }
                    }
                }
            }
            
            // Possible causes
            if (result.possibleCauses.isNotEmpty()) {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "🔍", fontSize = 20.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Возможные причины", style = GlassTypography.titleSmall)
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        result.possibleCauses.forEach { cause ->
                            Row(
                                modifier = Modifier.padding(vertical = 4.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Box(
                                    modifier = Modifier
                                        .padding(top = 6.dp)
                                        .size(6.dp)
                                        .background(GlassColors.accent, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = cause, style = GlassTypography.messageText)
                            }
                        }
                    }
                }
            }
            
            // Recommendations
            if (result.recommendations.isNotEmpty()) {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "💡", fontSize = 20.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Рекомендации", style = GlassTypography.titleSmall)
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        result.recommendations.forEachIndexed { index, rec ->
                            Row(
                                modifier = Modifier.padding(vertical = 4.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .background(GlassColors.mint.copy(alpha = 0.2f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${index + 1}",
                                        style = GlassTypography.labelSmall.copy(color = GlassColors.mint)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = rec, style = GlassTypography.messageText)
                            }
                        }
                    }
                }
            }
            
            // When to see doctor
            if (result.whenToSeeDoctor.isNotBlank()) {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    borderColor = GlassColors.accent.copy(alpha = 0.3f)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "👨‍⚕️", fontSize = 20.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Когда к врачу", style = GlassTypography.titleSmall)
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = result.whenToSeeDoctor,
                            style = GlassTypography.messageText
                        )
                        
                        result.specialistType?.let { specialist ->
                            Spacer(modifier = Modifier.height(8.dp))
                            GlassChip(
                                text = "Специалист: $specialist",
                                color = GlassColors.accent
                            )
                        }
                    }
                }
            }
            
            // Disclaimer
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(GlassShapes.small)
                    .background(GlassColors.textTertiary.copy(alpha = 0.1f))
                    .padding(12.dp)
            ) {
                Text(
                    text = result.disclaimer.ifBlank { "Информация носит справочный характер и не является медицинским диагнозом." },
                    style = GlassTypography.labelSmall.copy(color = GlassColors.textTertiary),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
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
