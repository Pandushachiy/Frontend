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
import androidx.compose.foundation.text.KeyboardActions
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.health.companion.data.remote.api.DrugInteractionResponse
import com.health.companion.presentation.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrugInteractionsScreen(
    onBack: () -> Unit = {},
    viewModel: MedicalViewModel = hiltViewModel()
) {
    val isLoading by viewModel.isLoading.collectAsState()
    val result by viewModel.drugResult.collectAsState()
    val error by viewModel.error.collectAsState()
    
    var currentDrug by remember { mutableStateOf("") }
    var drugs by remember { mutableStateOf(listOf<String>()) }
    var includeCurrentMeds by remember { mutableStateOf(true) }
    
    GlassBackground {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Bar
            TopAppBar(
                title = { Text("💊 Проверка лекарств", style = GlassTypography.heading) },
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
                // Info banner
                item {
                    InfoBanner()
                }
                
                // Input section
                item {
                    DrugInputCard(
                        currentDrug = currentDrug,
                        drugs = drugs,
                        includeCurrentMeds = includeCurrentMeds,
                        onCurrentDrugChange = { currentDrug = it },
                        onAddDrug = {
                            if (currentDrug.isNotBlank() && currentDrug !in drugs) {
                                drugs = drugs + currentDrug.trim()
                                currentDrug = ""
                            }
                        },
                        onRemoveDrug = { drugs = drugs - it },
                        onIncludeCurrentMedsChange = { includeCurrentMeds = it },
                        onCheck = { viewModel.checkDrugInteractions(drugs, includeCurrentMeds) },
                        isLoading = isLoading
                    )
                }
                
                // Quick drug selector
                item {
                    QuickDrugSelector(
                        onSelect = { drug ->
                            if (drug !in drugs) {
                                drugs = drugs + drug
                            }
                        }
                    )
                }
                
                // Result
                if (result != null) {
                    item {
                        DrugResultCard(
                            result = result!!,
                            onClear = { viewModel.clearDrugResult() }
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
            Text(text = "💡", fontSize = 18.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Добавьте препараты для проверки их совместимости. AI учтёт ваши текущие лекарства из профиля.",
                style = GlassTypography.labelSmall.copy(color = GlassColors.info)
            )
        }
    }
}

@Composable
private fun DrugInputCard(
    currentDrug: String,
    drugs: List<String>,
    includeCurrentMeds: Boolean,
    onCurrentDrugChange: (String) -> Unit,
    onAddDrug: () -> Unit,
    onRemoveDrug: (String) -> Unit,
    onIncludeCurrentMedsChange: (Boolean) -> Unit,
    onCheck: () -> Unit,
    isLoading: Boolean
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Добавьте препараты",
                style = GlassTypography.titleSmall
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Input field with add button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = currentDrug,
                    onValueChange = onCurrentDrugChange,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Название препарата") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { onAddDrug() }),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GlassColors.accent,
                        unfocusedBorderColor = GlassColors.whiteOverlay20,
                        focusedContainerColor = GlassColors.whiteOverlay05,
                        unfocusedContainerColor = GlassColors.whiteOverlay05
                    ),
                    shape = GlassShapes.medium
                )
                
                IconButton(
                    onClick = onAddDrug,
                    enabled = currentDrug.isNotBlank(),
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            if (currentDrug.isNotBlank()) GlassColors.accent
                            else GlassColors.whiteOverlay10,
                            CircleShape
                        )
                ) {
                    Icon(
                        Icons.Default.Add,
                        null,
                        tint = if (currentDrug.isNotBlank()) Color.White else GlassColors.textTertiary
                    )
                }
            }
            
            // Added drugs
            if (drugs.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = "Выбрано (${drugs.size}):",
                    style = GlassTypography.labelSmall
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                androidx.compose.foundation.layout.FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    drugs.forEach { drug ->
                        DrugChip(
                            name = drug,
                            onRemove = { onRemoveDrug(drug) }
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Include current medications checkbox
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(GlassShapes.small)
                    .background(GlassColors.whiteOverlay05, GlassShapes.small)
                    .clickable { onIncludeCurrentMedsChange(!includeCurrentMeds) }
                    .padding(12.dp)
            ) {
                Checkbox(
                    checked = includeCurrentMeds,
                    onCheckedChange = onIncludeCurrentMedsChange,
                    colors = CheckboxDefaults.colors(
                        checkedColor = GlassColors.accent,
                        uncheckedColor = GlassColors.textTertiary
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Учитывать мои лекарства",
                        style = GlassTypography.labelMedium
                    )
                    Text(
                        text = "Из профиля здоровья",
                        style = GlassTypography.labelSmall
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Check button
            GlassButton(
                onClick = onCheck,
                modifier = Modifier.fillMaxWidth(),
                enabled = drugs.isNotEmpty() && !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Проверяю...", color = Color.White)
                } else {
                    Icon(Icons.Default.Search, null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Проверить взаимодействия", color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun DrugChip(
    name: String,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(GlassShapes.chip)
            .background(GlassColors.teal.copy(alpha = 0.15f), GlassShapes.chip)
            .border(1.dp, GlassColors.teal.copy(alpha = 0.3f), GlassShapes.chip)
            .padding(start = 12.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = "💊", fontSize = 14.sp)
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = name,
            style = GlassTypography.labelSmall.copy(color = GlassColors.teal)
        )
        IconButton(
            onClick = onRemove,
            modifier = Modifier.size(24.dp)
        ) {
            Icon(
                Icons.Default.Close,
                null,
                tint = GlassColors.teal,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun QuickDrugSelector(onSelect: (String) -> Unit) {
    val commonDrugs = listOf(
        "Ибупрофен", "Парацетамол", "Аспирин", "Нурофен",
        "Анальгин", "Цитрамон", "Супрастин", "Лоратадин",
        "Омепразол", "Мезим", "Активированный уголь"
    )
    
    Column {
        Text(
            text = "Популярные препараты:",
            style = GlassTypography.labelSmall
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        androidx.compose.foundation.layout.FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            commonDrugs.forEach { drug ->
                GlassChip(
                    text = drug,
                    color = GlassColors.textSecondary,
                    onClick = { onSelect(drug) }
                )
            }
        }
    }
}

@Composable
private fun DrugResultCard(
    result: DrugInteractionResponse,
    onClear: () -> Unit
) {
    val isSafe = result.safe && result.warnings.isEmpty()
    
    val statusGradient = if (isSafe) {
        listOf(Color(0xFF4CAF50), Color(0xFF81C784))
    } else {
        listOf(Color(0xFFF44336), Color(0xFFE57373))
    }
    
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + slideInVertically { it / 2 }
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // Status card
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                borderColor = statusGradient.first().copy(alpha = 0.5f)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Результат проверки", style = GlassTypography.titleSmall)
                        IconButton(onClick = onClear) {
                            Icon(Icons.Default.Close, null, tint = GlassColors.textTertiary)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Status indicator
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(GlassShapes.medium)
                            .background(
                                brush = Brush.horizontalGradient(
                                    statusGradient.map { it.copy(alpha = 0.2f) }
                                ),
                                shape = GlassShapes.medium
                            )
                            .border(
                                1.dp,
                                statusGradient.first().copy(alpha = 0.5f),
                                GlassShapes.medium
                            )
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isSafe) "✅" else "⚠️",
                            fontSize = 32.sp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = if (isSafe) "Безопасно" else "Внимание!",
                                style = GlassTypography.titleSmall.copy(
                                    color = statusGradient.first()
                                )
                            )
                            Text(
                                text = if (isSafe) 
                                    "Опасных взаимодействий не найдено"
                                else 
                                    "Обнаружены возможные взаимодействия",
                                style = GlassTypography.labelSmall
                            )
                        }
                    }
                    
                    // Checked drugs
                    if (result.drugsChecked.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Проверено: ${result.drugsChecked.joinToString(", ")}",
                            style = GlassTypography.labelSmall
                        )
                    }
                }
            }
            
            // Warnings
            if (result.warnings.isNotEmpty()) {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    borderColor = GlassColors.warning.copy(alpha = 0.3f)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "⚠️", fontSize = 20.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Предупреждения", style = GlassTypography.titleSmall)
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        result.warnings.forEach { warning ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clip(GlassShapes.small)
                                    .background(GlassColors.warning.copy(alpha = 0.1f))
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = warning,
                                    style = GlassTypography.messageText.copy(
                                        color = GlassColors.warning
                                    )
                                )
                            }
                        }
                    }
                }
            }
            
            // Detailed analysis
            result.detailedAnalysis?.let { analysis ->
                if (analysis.isNotBlank()) {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = "📋", fontSize = 20.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Подробный анализ", style = GlassTypography.titleSmall)
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            Text(
                                text = analysis,
                                style = GlassTypography.messageText
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
                    text = result.disclaimer.ifBlank { "Перед приёмом любых лекарств проконсультируйтесь с врачом." },
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
