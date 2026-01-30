@file:OptIn(ExperimentalLayoutApi::class)

package com.health.companion.presentation.screens.profile

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.health.companion.data.remote.api.ImportantPerson
import com.health.companion.presentation.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportantPeopleScreen(
    onBack: () -> Unit = {},
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val people by viewModel.importantPeople.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()
    
    var showAddDialog by remember { mutableStateOf(false) }
    
    GlassBackground {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Bar
            TopAppBar(
                title = { Text("👥 Близкие люди", style = GlassTypography.heading) },
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
            
            if (people.isEmpty()) {
                EmptyPeopleState(onAdd = { showAddDialog = true })
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Group by relation
                    val grouped = people.groupBy { categorizeRelation(it.relation) }
                    
                    grouped.forEach { (category, categoryPeople) ->
                        item {
                            Text(
                                text = category,
                                style = GlassTypography.titleSmall,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                        
                        items(categoryPeople, key = { it.id }) { person ->
                            PersonCard(
                                person = person,
                                onDelete = { viewModel.deleteImportantPerson(person.id) }
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
            AddPersonDialog(
                onDismiss = { showAddDialog = false },
                onAdd = { name, relation, details, birthday ->
                    viewModel.addImportantPerson(name, relation, details, birthday)
                    showAddDialog = false
                },
                isSaving = isSaving
            )
        }
    }
}

private fun categorizeRelation(relation: String): String {
    val lower = relation.lowercase()
    return when {
        lower.contains("мама") || lower.contains("папа") || 
        lower.contains("брат") || lower.contains("сестра") ||
        lower.contains("бабушка") || lower.contains("дедушка") ||
        lower.contains("родител") -> "👨‍👩‍👧‍👦 Семья"
        
        lower.contains("муж") || lower.contains("жена") ||
        lower.contains("парень") || lower.contains("девушка") ||
        lower.contains("партнер") -> "❤️ Партнёр"
        
        lower.contains("друг") || lower.contains("подруга") -> "🤝 Друзья"
        
        lower.contains("коллега") || lower.contains("начальник") ||
        lower.contains("работа") -> "💼 Коллеги"
        
        else -> "👤 Другие"
    }
}

@Composable
private fun EmptyPeopleState(onAdd: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Text(text = "👥", fontSize = 64.sp)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Нет близких людей",
                style = GlassTypography.titleMedium
            )
            Text(
                text = "Добавьте семью и друзей, чтобы AI лучше понимал контекст ваших разговоров",
                style = GlassTypography.labelSmall,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            GlassButton(onClick = onAdd) {
                Icon(Icons.Default.Add, null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Добавить человека", color = Color.White)
            }
        }
    }
}

@Composable
private fun PersonCard(
    person: ImportantPerson,
    onDelete: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var isExpanded by remember { mutableStateOf(false) }
    
    val relationEmoji = when {
        person.relation.lowercase().contains("мама") -> "👩"
        person.relation.lowercase().contains("папа") -> "👨"
        person.relation.lowercase().contains("брат") -> "👦"
        person.relation.lowercase().contains("сестра") -> "👧"
        person.relation.lowercase().contains("бабушка") -> "👵"
        person.relation.lowercase().contains("дедушка") -> "👴"
        person.relation.lowercase().contains("муж") || 
        person.relation.lowercase().contains("жена") -> "💑"
        person.relation.lowercase().contains("друг") ||
        person.relation.lowercase().contains("подруга") -> "🤝"
        else -> "👤"
    }
    
    val avatarGradient = listOf(
        Color(0xFF667eea),
        Color(0xFF764ba2)
    )
    
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .clickable { isExpanded = !isExpanded }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Avatar
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(
                            brush = Brush.linearGradient(avatarGradient),
                            shape = CircleShape
                        )
                        .border(2.dp, avatarGradient.first().copy(alpha = 0.5f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = person.name.firstOrNull()?.uppercase() ?: relationEmoji,
                        fontSize = 24.sp,
                        color = Color.White
                    )
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                // Info
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = person.name,
                        style = GlassTypography.titleSmall
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(text = relationEmoji, fontSize = 14.sp)
                        Text(
                            text = person.relation,
                            style = GlassTypography.labelSmall
                        )
                    }
                    person.birthday?.let { birthday ->
                        Text(
                            text = "🎂 $birthday",
                            style = GlassTypography.labelSmall.copy(color = GlassColors.accent)
                        )
                    }
                }
                
                // Expand/Delete
                IconButton(onClick = { showDeleteConfirm = true }) {
                    Icon(
                        Icons.Default.Delete,
                        null,
                        tint = GlassColors.textTertiary
                    )
                }
            }
            
            // Details (expanded)
            AnimatedVisibility(
                visible = isExpanded && !person.details.isNullOrBlank()
            ) {
                Column {
                    GlassDivider(modifier = Modifier.padding(vertical = 12.dp))
                    
                    Text(
                        text = "📝 Заметки",
                        style = GlassTypography.labelSmall.copy(color = GlassColors.textTertiary)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = person.details ?: "",
                        style = GlassTypography.messageText
                    )
                }
            }
        }
    }
    
    // Delete confirmation
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Удалить?") },
            text = { Text("${person.name} будет удалён из списка") },
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
private fun AddPersonDialog(
    onDismiss: () -> Unit,
    onAdd: (name: String, relation: String, details: String?, birthday: String?) -> Unit,
    isSaving: Boolean
) {
    var name by remember { mutableStateOf("") }
    var relation by remember { mutableStateOf("") }
    var details by remember { mutableStateOf("") }
    var birthday by remember { mutableStateOf("") }
    
    val quickRelations = listOf(
        "👩 Мама", "👨 Папа", "👧 Сестра", "👦 Брат",
        "💑 Муж", "💑 Жена", "🤝 Друг", "🤝 Подруга"
    )
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Добавить человека", style = GlassTypography.titleMedium)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Name
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Имя") },
                    placeholder = { Text("Анна") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GlassColors.accent,
                        unfocusedBorderColor = GlassColors.whiteOverlay20
                    )
                )
                
                // Relation
                OutlinedTextField(
                    value = relation,
                    onValueChange = { relation = it },
                    label = { Text("Кто это") },
                    placeholder = { Text("Мама, друг, коллега...") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GlassColors.accent,
                        unfocusedBorderColor = GlassColors.whiteOverlay20
                    )
                )
                
                // Quick selection
                Text("Быстрый выбор:", style = GlassTypography.labelSmall)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    quickRelations.forEach { rel ->
                        val relText = rel.drop(2).trim()
                        FilterChip(
                            selected = relation == relText,
                            onClick = { relation = relText },
                            label = { Text(rel, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = GlassColors.accent.copy(alpha = 0.3f)
                            )
                        )
                    }
                }
                
                // Birthday (optional)
                OutlinedTextField(
                    value = birthday,
                    onValueChange = { birthday = it },
                    label = { Text("День рождения (опционально)") },
                    placeholder = { Text("2000-05-15") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GlassColors.accent,
                        unfocusedBorderColor = GlassColors.whiteOverlay20
                    )
                )
                
                // Details (optional)
                OutlinedTextField(
                    value = details,
                    onValueChange = { details = it },
                    label = { Text("Заметки (опционально)") },
                    placeholder = { Text("Любит цветы, звонить по субботам...") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GlassColors.accent,
                        unfocusedBorderColor = GlassColors.whiteOverlay20
                    )
                )
            }
        },
        confirmButton = {
            GlassButton(
                onClick = { 
                    onAdd(
                        name, 
                        relation, 
                        details.takeIf { it.isNotBlank() },
                        birthday.takeIf { it.isNotBlank() }
                    ) 
                },
                enabled = name.isNotBlank() && relation.isNotBlank() && !isSaving
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

@Composable
private fun FlowRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable () -> Unit
) {
    // Simple flow row implementation
    androidx.compose.foundation.layout.FlowRow(
        modifier = modifier,
        horizontalArrangement = horizontalArrangement,
        verticalArrangement = verticalArrangement
    ) {
        content()
    }
}
