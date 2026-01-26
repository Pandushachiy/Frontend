package com.health.companion.presentation.screens.documents

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.health.companion.data.remote.api.DocumentResponse
import com.health.companion.presentation.components.GlassCard
import com.health.companion.presentation.components.GlassTheme
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.format.DateTimeParseException

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun DocumentsScreen(
    viewModel: DocumentsViewModel = hiltViewModel(),
    bottomPadding: androidx.compose.ui.unit.Dp = 0.dp
) {
    val uiState by viewModel.uiState.collectAsState()
    val documents by viewModel.documents.collectAsState()
    val isUploading by viewModel.isUploading.collectAsState()
    val displayNames by viewModel.displayNames.collectAsState()
    val now by produceState(initialValue = System.currentTimeMillis()) {
        while (true) {
            delay(30_000)
            value = System.currentTimeMillis()
        }
    }
    
    val cameraPermission = rememberPermissionState(Manifest.permission.CAMERA)
    
    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            viewModel.onPhotoTaken()
        }
    }
    
    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.uploadDocuments(uris)
        }
    }
    
    val pickDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.uploadDocument(it) }
    }

    var renameTarget by remember { mutableStateOf<DocumentResponse?>(null) }
    var renameText by remember { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf(DocFilter.All) }
    var sort by rememberSaveable { mutableStateOf(DocSort.Newest) }

    val visibleDocuments = remember(documents, displayNames, filter, sort) {
        val filtered = when (filter) {
            DocFilter.All -> documents
            DocFilter.Pdf -> documents.filter { docCategory(it.mime_type) == DocFilter.Pdf }
            DocFilter.Images -> documents.filter { docCategory(it.mime_type) == DocFilter.Images }
            DocFilter.Docs -> documents.filter { docCategory(it.mime_type) == DocFilter.Docs }
            DocFilter.Other -> documents.filter { docCategory(it.mime_type) == DocFilter.Other }
        }
        when (sort) {
            DocSort.Newest -> filtered.sortedByDescending { documentTimestamp(it) }
            DocSort.Oldest -> filtered.sortedBy { documentTimestamp(it) }
            DocSort.Name -> filtered.sortedBy { displayNames[it.id] ?: it.filename }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(GlassTheme.backgroundGradient)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .statusBarsPadding()
    ) {
            Spacer(modifier = Modifier.height(8.dp))
            
        // Header
            GlassCard(
            modifier = Modifier.fillMaxWidth(),
                cornerRadius = 20.dp,
                backgroundColor = GlassTheme.glassWhite,
                borderColor = GlassTheme.glassBorder
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(
                                            GlassTheme.accentSecondary,
                                            GlassTheme.accentTertiary
                                        )
                                    ),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.FolderOpen,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
            Text(
                text = "Документы",
                                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = GlassTheme.textPrimary
            )
                            Text(
                                text = "${documents.size} файлов",
                                style = MaterialTheme.typography.bodySmall,
                                color = GlassTheme.textSecondary
                            )
                        }
                    }
                    
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.1f))
                            .clickable { viewModel.refreshDocuments() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                    contentDescription = "Обновить",
                            tint = GlassTheme.accentPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Filters
            LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
                item {
                    GlassFilterChip(
                        text = "Все",
                        icon = Icons.Default.Apps,
                selected = filter == DocFilter.All,
                        onClick = { filter = DocFilter.All }
                    )
                }
                item {
                    GlassFilterChip(
                        text = "PDF",
                        icon = Icons.Default.PictureAsPdf,
                selected = filter == DocFilter.Pdf,
                onClick = { filter = DocFilter.Pdf },
                        accentColor = Color(0xFFEF5350)
                    )
                }
                item {
                    GlassFilterChip(
                        text = "Фото",
                        icon = Icons.Default.Image,
                selected = filter == DocFilter.Images,
                onClick = { filter = DocFilter.Images },
                        accentColor = Color(0xFF66BB6A)
                    )
                }
                item {
                    GlassFilterChip(
                        text = "DOC",
                        icon = Icons.Default.Description,
                selected = filter == DocFilter.Docs,
                onClick = { filter = DocFilter.Docs },
                        accentColor = Color(0xFF42A5F5)
            )
                }
                item {
            Spacer(Modifier.width(8.dp))
                }
                item {
                    GlassSortChip(
                        text = when (sort) {
                            DocSort.Newest -> "Новые"
                            DocSort.Oldest -> "Старые"
                            DocSort.Name -> "Имя"
                        },
                        onClick = {
                            sort = when (sort) {
                                DocSort.Newest -> DocSort.Oldest
                                DocSort.Oldest -> DocSort.Name
                                DocSort.Name -> DocSort.Newest
                            }
                        }
                    )
                }
            }

                        Spacer(modifier = Modifier.height(12.dp))

            // Content
            Box(modifier = Modifier.weight(1f)) {
                when {
                    uiState is DocumentsUiState.Error && documents.isEmpty() -> {
                        ErrorState(
                            message = (uiState as DocumentsUiState.Error).message,
                            onRetry = { viewModel.refreshDocuments() }
                        )
                        }
                    uiState is DocumentsUiState.Loading && documents.isEmpty() -> {
                LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(5) {
                        DocumentSkeletonItem()
                    }
                }
                    }
                    visibleDocuments.isEmpty() -> {
                        EmptyState(isEmpty = documents.isEmpty())
                    }
                    else -> {
                LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(visibleDocuments, key = { it.id }) { document ->
                        val rawName = displayNames[document.id] ?: document.filename
                        val displayName = rawName.substringBeforeLast('.', rawName)
                                GlassDocumentItem(
                            document = document,
                            displayName = displayName,
                            showReady = viewModel.shouldShowReady(document, now),
                            fileTypeLabel = formatMimeType(document.mime_type),
                            onRename = {
                                renameTarget = document
                                renameText = displayName
                            },
                            onDelete = { viewModel.deleteDocument(document.id) }
                        )
                    }
                            item { Spacer(modifier = Modifier.height(8.dp)) }
                        }
                    }
                }
            }

            // Upload Panel
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp + bottomPadding),
                cornerRadius = 20.dp,
                backgroundColor = GlassTheme.glassWhite,
                borderColor = GlassTheme.glassBorder
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                        GlassActionButton(
                            icon = Icons.Default.CameraAlt,
                            text = "Камера",
                            modifier = Modifier.weight(1f),
                        onClick = {
                            if (cameraPermission.status.isGranted) {
                                viewModel.prepareCamera()?.let { uri ->
                                    takePictureLauncher.launch(uri)
                                }
                            } else {
                                cameraPermission.launchPermissionRequest()
                            }
                            }
                        )
                        GlassActionButton(
                            icon = Icons.Default.Image,
                            text = "Галерея",
                        modifier = Modifier.weight(1f),
                            onClick = { pickImageLauncher.launch("image/*") }
                        )
                    }

                    // Main upload button
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        GlassTheme.accentPrimary,
                                        GlassTheme.accentSecondary
                                    )
                                )
                            )
                            .clickable {
                        pickDocumentLauncher.launch(
                            arrayOf(
                                "application/pdf",
                                "image/*",
                                "application/msword",
                                "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                            )
                        )
                    },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                ) {
                            Icon(
                                Icons.Default.Upload,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                    Spacer(Modifier.width(8.dp))
                            Text(
                                "Загрузить документ",
                                style = MaterialTheme.typography.labelLarge,
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold
                            )
                }
                    }

                    // Upload progress
                if (isUploading) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = GlassTheme.accentPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(10.dp))
                    Text(
                                text = "Загрузка...",
                        style = MaterialTheme.typography.bodySmall,
                                color = GlassTheme.textSecondary
                    )
                        }
                    }
                }
            }
        }
    }

    // Rename Dialog
    if (renameTarget != null) {
        AlertDialog(
            onDismissRequest = { renameTarget = null },
            containerColor = Color(0xFF1A1A2E),
            shape = RoundedCornerShape(20.dp),
            title = {
                Text(
                    "Переименовать",
                    color = GlassTheme.textPrimary,
                    fontWeight = FontWeight.SemiBold
                )
            },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    singleLine = true,
                    placeholder = { Text("Введите имя", color = GlassTheme.textTertiary) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GlassTheme.accentPrimary,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                        focusedTextColor = GlassTheme.textPrimary,
                        unfocusedTextColor = GlassTheme.textPrimary,
                        cursorColor = GlassTheme.accentPrimary
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        renameTarget?.let { viewModel.setDisplayName(it.id, renameText) }
                        renameTarget = null
                    }
                ) {
                    Text("Сохранить", color = GlassTheme.accentPrimary)
                }
            },
            dismissButton = {
                TextButton(onClick = { renameTarget = null }) {
                    Text("Отмена", color = GlassTheme.textSecondary)
                }
            }
        )
    }
}

@Composable
private fun GlassFilterChip(
    text: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    accentColor: Color = GlassTheme.accentPrimary
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (selected) accentColor.copy(alpha = 0.2f)
                else Color.White.copy(alpha = 0.08f)
            )
            .border(
                1.dp,
                if (selected) accentColor.copy(alpha = 0.5f)
                else Color.White.copy(alpha = 0.1f),
                RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (selected) accentColor else GlassTheme.textSecondary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text,
                style = MaterialTheme.typography.labelMedium,
                color = if (selected) accentColor else GlassTheme.textSecondary
            )
        }
    }
}

@Composable
private fun GlassSortChip(
    text: String,
    onClick: () -> Unit
) {
            Box(
                modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                Icons.Default.Sort,
                contentDescription = null,
                tint = GlassTheme.textSecondary,
                modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                text,
                        style = MaterialTheme.typography.labelMedium,
                color = GlassTheme.textSecondary
                    )
                }
            }
        }

@Composable
private fun GlassActionButton(
    icon: ImageVector,
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = GlassTheme.accentPrimary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text,
                style = MaterialTheme.typography.labelMedium,
                color = GlassTheme.textPrimary
            )
        }
    }
}

@Composable
private fun GlassDocumentItem(
    document: DocumentResponse,
    displayName: String,
    showReady: Boolean,
    fileTypeLabel: String,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    val (icon, tint) = getFileTypeIconAndColor(document.mime_type)
    
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 14.dp,
        backgroundColor = GlassTheme.glassWhite,
        borderColor = GlassTheme.glassBorder
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // File icon
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(tint.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                    Icon(
                    icon,
                        contentDescription = null,
                        tint = tint,
                    modifier = Modifier.size(24.dp)
                    )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = GlassTheme.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (showReady || (document.status?.lowercase() != "processed")) {
                        GlassStatusChip(
                            status = document.status ?: "unknown",
                            showReady = showReady
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = fileTypeLabel.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = GlassTheme.textTertiary,
                        letterSpacing = 1.sp
                    )
                }
            }

            // Actions
            Row {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.08f))
                        .clickable { onRename() },
                    contentAlignment = Alignment.Center
                ) {
                Icon(
                        Icons.Default.Edit,
                    contentDescription = "Rename",
                        tint = GlassTheme.accentPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(GlassTheme.statusError.copy(alpha = 0.1f))
                        .clickable { onDelete() },
                    contentAlignment = Alignment.Center
                ) {
                Icon(
                        Icons.Default.Delete,
                    contentDescription = "Delete",
                        tint = GlassTheme.statusError,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun GlassStatusChip(status: String, showReady: Boolean) {
    val statusKey = status.lowercase()
    val (color, text) = when {
        statusKey == "processing" -> GlassTheme.statusWarning to "⏳"
        statusKey == "error" -> GlassTheme.statusError to "❌"
        statusKey == "processed" && showReady -> GlassTheme.statusGood to "✓"
        else -> return
    }

    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            fontSize = 10.sp,
            color = color
        )
    }
}

@Composable
private fun DocumentSkeletonItem() {
    val transition = rememberInfiniteTransition(label = "skeleton")
    val alpha by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
        label = "alpha"
    )
    
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 14.dp,
        backgroundColor = GlassTheme.glassWhite,
        borderColor = GlassTheme.glassBorder
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White.copy(alpha = alpha * 0.15f))
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .height(14.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.White.copy(alpha = alpha * 0.1f))
                )
                Spacer(Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.4f)
                        .height(10.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.White.copy(alpha = alpha * 0.08f))
                )
            }
        }
    }
}

@Composable
private fun EmptyState(isEmpty: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "empty")
    
    val float by infiniteTransition.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "float"
    )
    
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            cornerRadius = 24.dp,
            backgroundColor = GlassTheme.glassWhite,
            borderColor = GlassTheme.glassBorder
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .offset(y = float.dp)
                        .graphicsLayer { scaleX = scale; scaleY = scale }
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    GlassTheme.accentSecondary.copy(alpha = 0.3f),
                                    GlassTheme.accentTertiary.copy(alpha = 0.2f)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "📂", fontSize = 36.sp)
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                Text(
                    text = if (isEmpty) "Пока пусто" else "Не найдено",
                    style = MaterialTheme.typography.titleMedium,
                    color = GlassTheme.textPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = if (isEmpty) 
                        "Загрузите документы\nдля анализа и поиска" 
                    else 
                        "Попробуйте другой фильтр",
                    style = MaterialTheme.typography.bodyMedium,
                    color = GlassTheme.textSecondary,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            cornerRadius = 24.dp,
            backgroundColor = GlassTheme.statusError.copy(alpha = 0.1f),
            borderColor = GlassTheme.statusError.copy(alpha = 0.3f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(GlassTheme.statusError.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = GlassTheme.statusError,
                        modifier = Modifier.size(32.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Ошибка загрузки",
                    style = MaterialTheme.typography.titleMedium,
                    color = GlassTheme.textPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = GlassTheme.textSecondary,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(20.dp))
                
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(GlassTheme.accentPrimary)
                        .clickable { onRetry() }
                        .padding(horizontal = 24.dp, vertical = 12.dp)
                ) {
            Text(
                        "Повторить",
                        color = Color.White,
                        fontWeight = FontWeight.Medium
            )
                }
            }
        }
    }
}

private fun formatMimeType(mimeType: String?): String {
    if (mimeType.isNullOrBlank()) return "файл"
    return when {
        mimeType.startsWith("image/") -> mimeType.substringAfter("image/").lowercase()
        mimeType == "application/pdf" -> "pdf"
        mimeType.contains("msword") -> "doc"
        mimeType.contains("officedocument.wordprocessingml") -> "docx"
        else -> mimeType.substringAfter("/").lowercase()
    }
}

private fun getFileTypeIconAndColor(mimeType: String?): Pair<ImageVector, Color> {
    val type = formatMimeType(mimeType)
    return when (type) {
        "pdf" -> Icons.Default.PictureAsPdf to Color(0xFFEF5350)
        "doc", "docx" -> Icons.Default.Description to Color(0xFF42A5F5)
        "png", "jpg", "jpeg", "webp", "gif" -> Icons.Default.Image to Color(0xFF66BB6A)
        else -> Icons.Default.InsertDriveFile to Color(0xFF78909C)
    }
}

private enum class DocFilter { All, Pdf, Images, Docs, Other }
private enum class DocSort { Newest, Oldest, Name }

private fun docCategory(mimeType: String?): DocFilter {
    val type = formatMimeType(mimeType)
    return when {
        type == "pdf" -> DocFilter.Pdf
        type == "doc" || type == "docx" -> DocFilter.Docs
        type in listOf("png", "jpg", "jpeg", "webp", "gif") -> DocFilter.Images
        else -> DocFilter.Other
    }
}

private fun documentTimestamp(document: DocumentResponse): Long {
    val raw = document.uploaded_at ?: return 0L
    return try {
        Instant.parse(raw).toEpochMilli()
    } catch (_: DateTimeParseException) {
        0L
    }
}
