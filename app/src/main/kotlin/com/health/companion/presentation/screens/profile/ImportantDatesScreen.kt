package com.health.companion.presentation.screens.profile

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
import com.health.companion.data.remote.api.ImportantDate
import com.health.companion.presentation.components.*
import com.health.companion.presentation.theme.LocalChatBackground

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportantDatesScreen(
    onBack: () -> Unit = {},
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val dates by viewModel.importantDates.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()
    
    var showAddDialog by remember { mutableStateOf(false) }
    
    GlassBackground {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Bar
            TopAppBar(
                title = { Text("📅 Важные даты", style = GlassTypography.heading) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, null, tint = GlassColors.textPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, null, tint = GlassColors.accent)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
            
            if (dates.isEmpty()) {
                // Empty state
                EmptyDatesState(onAdd = { showAddDialog = true })
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Upcoming section
                    val upcoming = dates.filter { (it.daysUntil ?: 999) <= 30 }
                    if (upcoming.isNotEmpty()) {
                        item {
                            Text(
                                text = "🔜 Скоро",
                                style = GlassTypography.titleSmall,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }
                        items(upcoming, key = { it.id }) { date ->
                            DateCard(
                                date = date,
                                onDelete = { viewModel.deleteImportantDate(date.id) }
                            )
                        }
                    }
                    
                    // Later section
                    val later = dates.filter { (it.daysUntil ?: 999) > 30 }
                    if (later.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "📆 Позже",
                                style = GlassTypography.titleSmall,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }
                        items(later, key = { it.id }) { date ->
                            DateCard(
                                date = date,
                                onDelete = { viewModel.deleteImportantDate(date.id) }
                            )
                        }
                    }
                    
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }
        
        // Add FAB
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.BottomEnd
        ) {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = GlassColors.accent
            ) {
                Icon(Icons.Default.Add, null, tint = Color.White)
            }
        }
        
        // Add Dialog
        if (showAddDialog) {
            AddDateDialog(
                onDismiss = { showAddDialog = false },
                onAdd = { date, title, type, recurring ->
                    viewModel.addImportantDate(date, title, type, recurring)
                    showAddDialog = false
                },
                isSaving = isSaving
            )
        }
    }
}

@Composable
private fun EmptyDatesState(onAdd: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Text(text = "📅", fontSize = 64.sp)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Нет важных дат",
                style = GlassTypography.titleMedium
            )
            Text(
                text = "Добавьте дни рождения, годовщины и другие важные события",
                style = GlassTypography.labelSmall,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            GlassButton(onClick = onAdd) {
                Icon(Icons.Default.Add, null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Добавить дату", color = Color.White)
            }
        }
    }
}

@Composable
private fun DateCard(
    date: ImportantDate,
    onDelete: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }
    
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
    
    val borderGradient = when {
        date.daysUntil == null -> listOf(GlassColors.whiteOverlay10, GlassColors.whiteOverlay10)
        date.daysUntil!! <= 7 -> listOf(GlassColors.coral, Color(0xFFFF9A9E))
        date.daysUntil!! <= 30 -> listOf(GlassColors.orange, Color(0xFFFFD93D))
        else -> listOf(GlassColors.mint, Color(0xFF6DD5FA))
    }
    
    val chatBg = LocalChatBackground.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(GlassShapes.large)
            .background(chatBg.surfaceColor.copy(alpha = 0.9f), GlassShapes.large)
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(borderGradient.map { it.copy(alpha = 0.5f) }),
                shape = GlassShapes.large
            )
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = borderGradient.map { it.copy(alpha = 0.3f) }
                        ),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(text = typeEmoji, fontSize = 28.sp)
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Content
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = date.title,
                    style = GlassTypography.labelMedium
                )
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = date.date,
                        style = GlassTypography.labelSmall
                    )
                    if (date.recurring) {
                        Text(text = "🔄", fontSize = 12.sp)
                    }
                }
            }
            
            // Days until
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                date.daysUntil?.let { days ->
                    Text(
                        text = days.toString(),
                        style = GlassTypography.titleLarge.copy(color = daysColor)
                    )
                    Text(
                        text = "дн.",
                        style = GlassTypography.labelSmall.copy(color = daysColor)
                    )
                }
            }
            
            // Delete button
            IconButton(onClick = { showDeleteConfirm = true }) {
                Icon(
                    Icons.Default.Delete,
                    null,
                    tint = GlassColors.textTertiary
                )
            }
        }
    }
    
    // Delete confirmation
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Удалить дату?") },
            text = { Text("\"${date.title}\" будет удалена") },
            confirmButton = {
                TextButton(onClick = {
                    onDelete()
                    showDeleteConfirm = false
                }) {
                    Text("Удалить", color = GlassColors.coral)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Отмена")
                }
            },
            containerColor = GlassColors.surface,
            titleContentColor = GlassColors.textPrimary,
            textContentColor = GlassColors.textSecondary
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddDateDialog(
    onDismiss: () -> Unit,
    onAdd: (date: String, title: String, type: String, recurring: Boolean) -> Unit,
    isSaving: Boolean
) {
    var title by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }
    var eventType by remember { mutableStateOf("birthday") }
    var recurring by remember { mutableStateOf(true) }
    
    val eventTypes = listOf(
        "birthday" to "🎂 День рождения",
        "anniversary" to "💍 Годовщина",
        "custom" to "📌 Другое"
    )
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Добавить дату", style = GlassTypography.titleMedium)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Title
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Название") },
                    placeholder = { Text("День рождения мамы") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GlassColors.accent,
                        unfocusedBorderColor = GlassColors.whiteOverlay20
                    )
                )
                
                // Date
                OutlinedTextField(
                    value = date,
                    onValueChange = { date = it },
                    label = { Text("Дата") },
                    placeholder = { Text("2026-03-15") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GlassColors.accent,
                        unfocusedBorderColor = GlassColors.whiteOverlay20
                    )
                )
                
                // Event type
                Text("Тип события", style = GlassTypography.labelSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    eventTypes.forEach { (type, label) ->
                        FilterChip(
                            selected = eventType == type,
                            onClick = { eventType = type },
                            label = { Text(label, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = GlassColors.accent.copy(alpha = 0.3f),
                                selectedLabelColor = GlassColors.accent
                            )
                        )
                    }
                }
                
                // Recurring
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = recurring,
                        onCheckedChange = { recurring = it },
                        colors = CheckboxDefaults.colors(
                            checkedColor = GlassColors.accent
                        )
                    )
                    Text("Повторяется ежегодно", style = GlassTypography.labelMedium)
                }
            }
        },
        confirmButton = {
            GlassButton(
                onClick = { onAdd(date, title, eventType, recurring) },
                enabled = title.isNotBlank() && date.isNotBlank() && !isSaving
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Добавить", color = Color.White)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена", color = GlassColors.textSecondary)
            }
        },
        containerColor = GlassColors.surface,
        titleContentColor = GlassColors.textPrimary,
        textContentColor = GlassColors.textSecondary
    )
}
