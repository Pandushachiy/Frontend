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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.health.companion.data.remote.api.LabAnalysis
import com.health.companion.data.remote.api.LabResultsResponse
import com.health.companion.presentation.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LabResultsScreen(
    onBack: () -> Unit = {},
    viewModel: MedicalViewModel = hiltViewModel()
) {
    val isLoading by viewModel.isLoading.collectAsState()
    val result by viewModel.labResult.collectAsState()
    val error by viewModel.error.collectAsState()
    
    var labResults by remember { mutableStateOf(mapOf<String, Float>()) }
    var currentName by remember { mutableStateOf("") }
    var currentValue by remember { mutableStateOf("") }
    
    GlassBackground {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Bar
            TopAppBar(
                title = { Text("🔬 Анализ результатов", style = GlassTypography.heading) },
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
                // Info
                item {
                    InfoBanner()
                }
                
                // Input
                item {
                    LabInputCard(
                        currentName = currentName,
                        currentValue = currentValue,
                        labResults = labResults,
                        onNameChange = { currentName = it },
                        onValueChange = { currentValue = it },
                        onAdd = {
                            val value = currentValue.toFloatOrNull()
                            if (currentName.isNotBlank() && value != null) {
                                labResults = labResults + (currentName to value)
                                currentName = ""
                                currentValue = ""
                            }
                        },
                        onRemove = { labResults = labResults - it },
                        onAnalyze = { viewModel.analyzeLabResults(labResults) },
                        isLoading = isLoading
                    )
                }
                
                // Quick add common tests
                item {
                    CommonLabTests(
                        onSelect = { name ->
                            currentName = name
                        }
                    )
                }
                
                // Result
                if (result != null) {
                    item {
                        LabResultCard(
                            result = result!!,
                            onClear = { viewModel.clearLabResult() }
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
private fun InfoBanner() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(GlassShapes.medium)
            .background(GlassColors.info.copy(alpha = 0.1f), GlassShapes.medium)
            .border(1.dp, GlassColors.info.copy(alpha = 0.3f), GlassShapes.medium)
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Text(text = "🔬", fontSize = 18.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Введите показатели из результатов анализов. AI объяснит их значение и сравнит с нормами.",
                style = GlassTypography.labelSmall.copy(color = GlassColors.info)
            )
        }
    }
}

@Composable
private fun LabInputCard(
    currentName: String,
    currentValue: String,
    labResults: Map<String, Float>,
    onNameChange: (String) -> Unit,
    onValueChange: (String) -> Unit,
    onAdd: () -> Unit,
    onRemove: (String) -> Unit,
    onAnalyze: () -> Unit,
    isLoading: Boolean
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Добавить показатель",
                style = GlassTypography.titleSmall
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Name input
            OutlinedTextField(
                value = currentName,
                onValueChange = onNameChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Название показателя") },
                placeholder = { Text("Гемоглобин, глюкоза...") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GlassColors.accent,
                    unfocusedBorderColor = GlassColors.whiteOverlay20
                ),
                shape = GlassShapes.medium
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Value input + Add button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = currentValue,
                    onValueChange = onValueChange,
                    modifier = Modifier.weight(1f),
                    label = { Text("Значение") },
                    placeholder = { Text("145") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GlassColors.accent,
                        unfocusedBorderColor = GlassColors.whiteOverlay20
                    ),
                    shape = GlassShapes.medium
                )
                
                IconButton(
                    onClick = onAdd,
                    enabled = currentName.isNotBlank() && currentValue.toFloatOrNull() != null,
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            if (currentName.isNotBlank() && currentValue.toFloatOrNull() != null) 
                                GlassColors.accent
                            else 
                                GlassColors.whiteOverlay10,
                            CircleShape
                        )
                ) {
                    Icon(
                        Icons.Default.Add,
                        null,
                        tint = if (currentName.isNotBlank() && currentValue.toFloatOrNull() != null) 
                            Color.White 
                        else 
                            GlassColors.textTertiary
                    )
                }
            }
            
            // Added results
            if (labResults.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Добавленные показатели (${labResults.size}):",
                    style = GlassTypography.labelSmall
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    labResults.forEach { (name, value) ->
                        LabResultChip(
                            name = name,
                            value = value,
                            onRemove = { onRemove(name) }
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Analyze button
            GlassButton(
                onClick = onAnalyze,
                modifier = Modifier.fillMaxWidth(),
                enabled = labResults.isNotEmpty() && !isLoading
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
                    Icon(Icons.Default.Analytics, null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Проанализировать", color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun LabResultChip(
    name: String,
    value: Float,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(GlassShapes.medium)
            .background(GlassColors.whiteOverlay05, GlassShapes.medium)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = "🧪", fontSize = 16.sp)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = name,
            style = GlassTypography.labelMedium,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value.toString(),
            style = GlassTypography.titleSmall.copy(color = GlassColors.accent)
        )
        IconButton(
            onClick = onRemove,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                Icons.Default.Close,
                null,
                tint = GlassColors.textTertiary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun CommonLabTests(onSelect: (String) -> Unit) {
    val commonTests = listOf(
        "Гемоглобин", "Глюкоза", "Холестерин", "Эритроциты",
        "Лейкоциты", "Тромбоциты", "СОЭ", "Креатинин",
        "Билирубин", "АЛТ", "АСТ"
    )
    
    Column {
        Text(
            text = "Частые показатели:",
            style = GlassTypography.labelSmall
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        androidx.compose.foundation.layout.FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            commonTests.forEach { test ->
                GlassChip(
                    text = test,
                    color = GlassColors.textSecondary,
                    onClick = { onSelect(test) }
                )
            }
        }
    }
}

@Composable
private fun LabResultCard(
    result: LabResultsResponse,
    onClear: () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + slideInVertically { it / 2 }
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // Summary card
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Результаты анализа", style = GlassTypography.titleSmall)
                        IconButton(onClick = onClear) {
                            Icon(Icons.Default.Close, null, tint = GlassColors.textTertiary)
                        }
                    }
                    
                    // Critical values warning
                    if (result.criticalValues.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(GlassShapes.medium)
                                .background(GlassColors.error.copy(alpha = 0.15f))
                                .border(1.dp, GlassColors.error.copy(alpha = 0.5f), GlassShapes.medium)
                                .padding(12.dp)
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(text = "🚨", fontSize = 20.sp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Критические отклонения!",
                                        style = GlassTypography.labelMedium.copy(color = GlassColors.error)
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                result.criticalValues.forEach { critical ->
                                    Text(
                                        text = "• $critical",
                                        style = GlassTypography.labelSmall.copy(color = GlassColors.error)
                                    )
                                }
                            }
                        }
                    }
                    
                    // Concerns
                    if (result.concerns.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(GlassShapes.medium)
                                .background(GlassColors.warning.copy(alpha = 0.15f))
                                .padding(12.dp)
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(text = "⚠️", fontSize = 18.sp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Отклонения от нормы",
                                        style = GlassTypography.labelMedium.copy(color = GlassColors.warning)
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                result.concerns.forEach { concern ->
                                    Text(
                                        text = "• $concern",
                                        style = GlassTypography.labelSmall
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            // Individual results
            if (result.analyses.isNotEmpty()) {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Показатели", style = GlassTypography.titleSmall)
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        result.analyses.forEach { analysis ->
                            LabAnalysisRow(analysis = analysis)
                            if (analysis != result.analyses.last()) {
                                GlassDivider(modifier = Modifier.padding(vertical = 8.dp))
                            }
                        }
                    }
                }
            }
            
            // Explanation
            if (result.explanation.isNotBlank()) {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "📝", fontSize = 20.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Объяснение", style = GlassTypography.titleSmall)
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Text(
                            text = result.explanation,
                            style = GlassTypography.messageText
                        )
                    }
                }
            }
            
            // Recommendation
            if (result.recommendation.isNotBlank()) {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    borderColor = GlassColors.accent.copy(alpha = 0.3f)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "👨‍⚕️", fontSize = 20.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Рекомендация", style = GlassTypography.titleSmall)
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Text(
                            text = result.recommendation,
                            style = GlassTypography.messageText
                        )
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
                    text = result.disclaimer.ifBlank { "Интерпретация анализов — информационная. Для точной оценки обратитесь к врачу." },
                    style = GlassTypography.labelSmall.copy(color = GlassColors.textTertiary),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun LabAnalysisRow(analysis: LabAnalysis) {
    val status = LabStatus.fromString(analysis.status)
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = analysis.name,
                style = GlassTypography.labelMedium
            )
            
            // Reference range
            if (analysis.referenceLow != null && analysis.referenceHigh != null) {
                Text(
                    text = "Норма: ${analysis.referenceLow} - ${analysis.referenceHigh} ${analysis.unit}",
                    style = GlassTypography.labelSmall
                )
            }
        }
        
        Column(horizontalAlignment = Alignment.End) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${analysis.value}",
                    style = GlassTypography.titleSmall.copy(
                        color = Color(status.color.toInt())
                    )
                )
                if (analysis.unit.isNotBlank()) {
                    Text(
                        text = " ${analysis.unit}",
                        style = GlassTypography.labelSmall
                    )
                }
            }
            
            // Status badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(status.color.toInt()).copy(alpha = 0.2f))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = status.label,
                    style = GlassTypography.labelSmall.copy(
                        color = Color(status.color.toInt()),
                        fontSize = 10.sp
                    )
                )
            }
        }
    }
    
    // Visual indicator bar
    if (analysis.referenceLow != null && analysis.referenceHigh != null) {
        Spacer(modifier = Modifier.height(8.dp))
        LabValueIndicator(
            value = analysis.value,
            low = analysis.referenceLow!!,
            high = analysis.referenceHigh!!
        )
    }
}

@Composable
private fun LabValueIndicator(
    value: Float,
    low: Float,
    high: Float
) {
    val range = high - low
    val normalizedValue = ((value - low) / range).coerceIn(-0.5f, 1.5f)
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(GlassColors.whiteOverlay10)
    ) {
        // Normal zone (green)
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(0.8f)
                .align(Alignment.Center)
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFF2196F3).copy(alpha = 0.3f),
                            Color(0xFF4CAF50).copy(alpha = 0.5f),
                            Color(0xFF4CAF50).copy(alpha = 0.5f),
                            Color(0xFFFF9800).copy(alpha = 0.3f)
                        )
                    )
                )
        )
        
        // Value marker
        val markerPosition = (0.1f + normalizedValue * 0.8f).coerceIn(0.02f, 0.98f)
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(4.dp)
                .align(Alignment.CenterStart)
                .offset(x = (markerPosition * 300).dp) // Approximate, adjust as needed
                .background(
                    color = when {
                        value < low -> Color(0xFF2196F3)
                        value > high -> Color(0xFFFF9800)
                        else -> Color(0xFF4CAF50)
                    },
                    shape = RoundedCornerShape(2.dp)
                )
        )
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
