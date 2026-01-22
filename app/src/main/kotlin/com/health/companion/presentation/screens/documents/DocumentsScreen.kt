package com.health.companion.presentation.screens.documents

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.health.companion.data.remote.api.DocumentResponse
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.format.DateTimeParseException

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun DocumentsScreen(
    viewModel: DocumentsViewModel = hiltViewModel()
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
    
    // File picker for images
    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.uploadDocument(it) }
    }
    
    // File picker for all documents (PDF, images, etc.)
    val pickDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.uploadDocument(it) }
    }

    var renameTarget by remember { mutableStateOf<DocumentResponse?>(null) }
    var renameText by remember { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf(DocFilter.All) }
    var sort by rememberSaveable { mutableStateOf(DocSort.Newest) }
    val scrollState = rememberScrollState()

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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Документы",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            IconButton(onClick = { viewModel.refreshDocuments() }) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Обновить",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        Text(
            text = "Ваши документы",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = filter == DocFilter.All,
                onClick = { filter = DocFilter.All },
                label = { Text("Все") }
            )
            FilterChip(
                selected = filter == DocFilter.Pdf,
                onClick = { filter = DocFilter.Pdf },
                label = { Text("PDF") }
            )
            FilterChip(
                selected = filter == DocFilter.Images,
                onClick = { filter = DocFilter.Images },
                label = { Text("Фото") }
            )
            FilterChip(
                selected = filter == DocFilter.Docs,
                onClick = { filter = DocFilter.Docs },
                label = { Text("DOC") }
            )
            FilterChip(
                selected = filter == DocFilter.Other,
                onClick = { filter = DocFilter.Other },
                label = { Text("Другое") }
            )

            Spacer(Modifier.width(8.dp))

            SuggestionChip(
                onClick = { sort = DocSort.Newest },
                label = { Text("Новые") }
            )
            SuggestionChip(
                onClick = { sort = DocSort.Oldest },
                label = { Text("Старые") }
            )
            SuggestionChip(
                onClick = { sort = DocSort.Name },
                label = { Text("Имя") }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Box(modifier = Modifier.weight(1f, fill = true)) {
            if (uiState is DocumentsUiState.Loading && documents.isEmpty()) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(5) {
                        DocumentSkeletonItem()
                    }
                }
            } else if (visibleDocuments.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (documents.isEmpty()) "Документов пока нет" else "Ничего не найдено",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (documents.isEmpty()) "Загрузите документы для анализа и поиска" else "Попробуйте другой фильтр",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(visibleDocuments, key = { it.id }) { document ->
                        val displayName = displayNames[document.id] ?: document.filename
                        SwipeDocumentItem(
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
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shape = RoundedCornerShape(20.dp),
            tonalElevation = 2.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FilledTonalButton(
                        onClick = {
                            if (cameraPermission.status.isGranted) {
                                viewModel.prepareCamera()?.let { uri ->
                                    takePictureLauncher.launch(uri)
                                }
                            } else {
                                cameraPermission.launchPermissionRequest()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.CameraAlt, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Камера")
                    }
                    FilledTonalButton(
                        onClick = { pickImageLauncher.launch("image/*") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.Image, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Галерея")
                    }
                }

                Button(
                    onClick = {
                        pickDocumentLauncher.launch(
                            arrayOf(
                                "application/pdf",
                                "image/*",
                                "application/msword",
                                "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                            )
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.Upload, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Загрузить документ")
                }

                if (isUploading) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "Загрузка документа...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }

    if (renameTarget != null) {
        AlertDialog(
            onDismissRequest = { renameTarget = null },
            title = { Text("Переименовать файл") },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    singleLine = true,
                    placeholder = { Text("Введите имя") }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val target = renameTarget
                        if (target != null) {
                            viewModel.setDisplayName(target.id, renameText)
                        }
                        renameTarget = null
                    }
                ) { Text("Сохранить") }
            },
            dismissButton = {
                TextButton(onClick = { renameTarget = null }) { Text("Отмена") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeDocumentItem(
    document: DocumentResponse,
    displayName: String,
    showReady: Boolean,
    fileTypeLabel: String,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
                true
            } else {
                false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 6.dp)
                    .background(MaterialTheme.colorScheme.error.copy(alpha = 0.15f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.CenterEnd
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.padding(end = 16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Удалить",
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Удалить",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    ) {
        DocumentItem(
            document = document,
            displayName = displayName,
            showReady = showReady,
            fileTypeLabel = fileTypeLabel,
            onRename = onRename,
            onDelete = onDelete
        )
    }
}

@Composable
private fun DocumentItem(
    document: DocumentResponse,
    displayName: String,
    showReady: Boolean,
    fileTypeLabel: String,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Document Icon
            val (icon, tint) = getFileTypeIconAndColor(document.mime_type)
            Surface(
                modifier = Modifier.size(40.dp),
                color = tint.copy(alpha = 0.12f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = tint,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Document Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (showReady || (document.status?.lowercase() != "processed")) {
                        StatusChip(status = document.status ?: "unknown", showReady = showReady)
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = fileTypeLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }

            // Actions
            IconButton(onClick = onRename, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Rename",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun DocumentSkeletonItem() {
    val transition = rememberInfiniteTransition(label = "doc_skeleton")
    val alpha by transition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
        label = "doc_alpha"
    )
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha))
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .height(14.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha))
                )
                Spacer(Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.35f)
                        .height(12.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha))
                )
            }
        }
    }
}

@Composable
private fun StatusChip(status: String, showReady: Boolean) {
    val statusKey = status.lowercase()
    val (color, text) = when {
        statusKey == "processing" -> MaterialTheme.colorScheme.tertiary to "Обработка"
        statusKey == "error" -> MaterialTheme.colorScheme.error to "Ошибка"
        statusKey == "processed" && showReady -> MaterialTheme.colorScheme.primary to "Готово"
        else -> MaterialTheme.colorScheme.outline to ""
    }

    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(4.dp)
    ) {
        if (text.isNotEmpty()) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = color,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
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

private fun getFileTypeIconAndColor(mimeType: String?): Pair<androidx.compose.ui.graphics.vector.ImageVector, Color> {
    val type = formatMimeType(mimeType)
    return when (type) {
        "pdf" -> Icons.Default.PictureAsPdf to Color(0xFFD32F2F)
        "doc", "docx" -> Icons.Default.Description to Color(0xFF1976D2)
        "png", "jpg", "jpeg", "webp", "gif" -> Icons.Default.Image to Color(0xFF388E3C)
        else -> Icons.Default.InsertDriveFile to Color(0xFF616161)
    }
}

private enum class DocFilter {
    All,
    Pdf,
    Images,
    Docs,
    Other
}

private enum class DocSort {
    Newest,
    Oldest,
    Name
}

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
