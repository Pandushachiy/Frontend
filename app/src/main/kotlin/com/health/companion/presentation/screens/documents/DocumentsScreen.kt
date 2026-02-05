package com.health.companion.presentation.screens.documents

import android.Manifest
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.ui.viewinterop.AndroidView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.layout.onSizeChanged
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.health.companion.BuildConfig
import com.health.companion.data.remote.api.DocumentResponse
import com.health.companion.presentation.components.GlassCard
import com.health.companion.presentation.components.GlassTheme
import com.health.companion.presentation.components.GlassGradients
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.format.DateTimeParseException

// Base host for constructing full URLs from relative paths
private val API_HOST = BuildConfig.API_BASE_URL.substringBefore("/api/")  // http://46.17.99.76:8000

// Helper functions to construct document URLs
// Backend returns relative paths like "/api/v1/documents/{id}/thumbnail"
private fun DocumentResponse.getThumbnailUrl(): String {
    return when {
        thumbnailUrl != null -> "$API_HOST$thumbnailUrl"
        else -> "${BuildConfig.API_BASE_URL}/documents/$id/thumbnail"
    }
}

private fun DocumentResponse.getPreviewUrl(): String {
    return when {
        previewUrl != null -> "$API_HOST$previewUrl"
        else -> "${BuildConfig.API_BASE_URL}/documents/$id/preview"
    }
}

private fun DocumentResponse.getDownloadUrl(): String {
    return when {
        downloadUrl != null -> "$API_HOST$downloadUrl"
        else -> "${BuildConfig.API_BASE_URL}/documents/$id/download"
    }
}

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
    val authToken by viewModel.authToken.collectAsState()
    val pendingUploads by viewModel.pendingUploads.collectAsState()
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
    
    // Preview state
    var previewDocument by remember { mutableStateOf<DocumentResponse?>(null) }
    var previewPdfDocument by remember { mutableStateOf<DocumentResponse?>(null) }

    val visibleDocuments = remember(documents, displayNames, filter, sort) {
        val filtered = when (filter) {
            DocFilter.All -> documents
            DocFilter.Pdf -> documents.filter { docCategory(it.mimeType) == DocFilter.Pdf }
            DocFilter.Images -> documents.filter { docCategory(it.mimeType) == DocFilter.Images }
            DocFilter.Docs -> documents.filter { docCategory(it.mimeType) == DocFilter.Docs }
            DocFilter.Other -> documents.filter { docCategory(it.mimeType) == DocFilter.Other }
        }
        when (sort) {
            DocSort.Newest -> filtered.sortedByDescending { documentTimestamp(it) }
            DocSort.Oldest -> filtered.sortedBy { documentTimestamp(it) }
            DocSort.Name -> filtered.sortedBy { displayNames[it.id] ?: it.filename }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // 🎨 КАК В WELLNESS — ПРОСТАЯ СТРУКТУРА
    // ═══════════════════════════════════════════════════════════════
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(GlassGradients.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(bottom = bottomPadding)  // Отступ для BottomNav
        ) {
            // Фильтры — компактные
            LazyRow(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                item { CompactChip("Все", Icons.Default.Apps, filter == DocFilter.All) { filter = DocFilter.All } }
                item { CompactChip("Фото", Icons.Default.Image, filter == DocFilter.Images, Color(0xFF66BB6A)) { filter = DocFilter.Images } }
                item { CompactChip("PDF", Icons.Default.PictureAsPdf, filter == DocFilter.Pdf, Color(0xFFEF5350)) { filter = DocFilter.Pdf } }
                item { CompactChip("DOC", Icons.Default.Description, filter == DocFilter.Docs, Color(0xFF42A5F5)) { filter = DocFilter.Docs } }
                item { Spacer(Modifier.width(4.dp)) }
                item { 
                    CompactChip(
                        when (sort) { DocSort.Newest -> "↓ Нов"; DocSort.Oldest -> "↑ Стар"; DocSort.Name -> "A-Z" },
                        Icons.Default.Sort, false
                    ) { sort = when (sort) { DocSort.Newest -> DocSort.Oldest; DocSort.Oldest -> DocSort.Name; DocSort.Name -> DocSort.Newest } }
                }
            }

            // Список документов
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
                            modifier = Modifier.padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) { items(5) { DocumentSkeletonItem() } }
                    }
                    visibleDocuments.isEmpty() -> {
                        EmptyState(isEmpty = documents.isEmpty())
                    }
                    else -> {
                        val context = LocalContext.current
                        val listState = rememberLazyListState()
                        var currentRevealedId by remember { mutableStateOf<String?>(null) }
                        
                        LaunchedEffect(pendingUploads.size) {
                            if (pendingUploads.isNotEmpty()) listState.animateScrollToItem(0)
                        }
                        LaunchedEffect(listState.isScrollInProgress) {
                            if (listState.isScrollInProgress) currentRevealedId = null
                        }
                        
                        // Автообновление пока есть документы без smartTitle
                        val hasProcessingDocs = visibleDocuments.any { viewModel.isAwaitingAiProcessing(it, now) }
                        LaunchedEffect(hasProcessingDocs) {
                            if (hasProcessingDocs) {
                                while (true) {
                                    delay(5000)  // Обновляем каждые 5 сек
                                    viewModel.refreshDocuments()
                                }
                            }
                        }
                        
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)  // Компактно!
                        ) {
                            items(pendingUploads, key = { "p_${it.id}" }) { pending ->
                                Box(Modifier.animateItemPlacement(
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessLow
                                    )
                                )) {
                                    UploadingDocumentItem(pending, { viewModel.retryUpload(pending.id) }, { viewModel.cancelPendingUpload(pending.id) })
                                }
                            }
                            
                            items(visibleDocuments, key = { it.id }) { document ->
                                val displayName = document.smartTitle ?: (displayNames[document.id] ?: document.filename).substringBeforeLast('.', document.filename)
                                val thumbUrl = document.getThumbnailUrl()
                                val prevUrl = document.getPreviewUrl()
                                
                                // 🎬 Spring-анимация — как в сессиях!
                                Box(Modifier.animateItemPlacement(
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessLow
                                    )
                                )) {
                                    SwipeableDocumentItem(
                                        onDelete = { viewModel.deleteDocument(document.id) },
                                        currentRevealedId = currentRevealedId,
                                        itemId = document.id,
                                        onReveal = { currentRevealedId = it }
                                    ) {
                                        CompactDocCard(
                                            document = document,
                                            name = displayName,
                                            type = formatMimeType(document.mimeType),
                                            thumbUrl = thumbUrl,
                                            token = authToken,
                                            isProcessing = viewModel.isAwaitingAiProcessing(document, now),
                                            onTap = {
                                                val fn = document.filename.lowercase()
                                                val mt = document.mimeType?.lowercase() ?: ""
                                                when {
                                                    mt.startsWith("image/") || fn.endsWith(".jpg") || fn.endsWith(".png") -> previewDocument = document
                                                    mt == "application/pdf" || fn.endsWith(".pdf") -> previewPdfDocument = document
                                                    mt.contains("word") || fn.endsWith(".doc") -> previewPdfDocument = document
                                                    else -> context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(prevUrl)))
                                                }
                                            },
                                            onEdit = { renameTarget = document; renameText = displayName }
                                        )
                                    }
                                }
                            }
                            item { Spacer(Modifier.height(70.dp)) }  // Место для панели
                        }
                    }
                }
            }
            
            // Панель действий — прижата к низу
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 4.dp),  // Минимальный отступ от BottomNav
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFF1A1F35),
                border = BorderStroke(1.dp, Color(0xFF8B5CF6).copy(alpha = 0.3f))  // Фиолетовая рамка
            ) {
                Row(
                    modifier = Modifier.padding(6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ActionBtn(Icons.Default.CameraAlt, "Камера", Color(0xFF4CAF50), Modifier.weight(1f)) {
                        if (cameraPermission.status.isGranted) viewModel.prepareCamera()?.let { takePictureLauncher.launch(it) }
                        else cameraPermission.launchPermissionRequest()
                    }
                    ActionBtn(Icons.Default.Image, "Галерея", Color(0xFF2196F3), Modifier.weight(1f)) { pickImageLauncher.launch("image/*") }
                    ActionBtn(Icons.Default.AttachFile, "Файл", Color(0xFFFF9800), Modifier.weight(1f)) {
                        pickDocumentLauncher.launch(arrayOf("application/pdf", "application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                    }
                }
            }
        }
    }

    // Image Preview Dialog - fullscreen with zoom/pan
    val previewContext = LocalContext.current
    previewDocument?.let { doc ->
        ImagePreviewDialog(
            imageUrl = doc.getPreviewUrl(),
            title = doc.smartTitle ?: doc.filename,
            authToken = authToken,
            onDismiss = { previewDocument = null },
            onDownload = {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(doc.getDownloadUrl()))
                previewContext.startActivity(intent)
            }
        )
    }
    
    // PDF/DOC Preview Dialog - встроенный просмотр через WebView
    previewPdfDocument?.let { doc ->
        PdfPreviewDialog(
            documentUrl = doc.getPreviewUrl(),
            downloadUrl = doc.getDownloadUrl(),
            title = doc.smartTitle ?: doc.filename,
            authToken = authToken,
            onDismiss = { previewPdfDocument = null }
        )
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

// ═══════════════════════════════════════════════════════════════
// 🎨 КОМПАКТНЫЕ КОМПОНЕНТЫ — КАК В WELLNESS
// ═══════════════════════════════════════════════════════════════

@Composable
private fun CompactChip(
    text: String,
    icon: ImageVector,
    selected: Boolean,
    color: Color = Color(0xFF8B5CF6),
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.clickable { onClick() },
        shape = RoundedCornerShape(8.dp),
        color = if (selected) color.copy(alpha = 0.2f) else Color(0xFF1A1F35)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = if (selected) color else Color.White.copy(alpha = 0.5f), modifier = Modifier.size(12.dp))
            Spacer(Modifier.width(4.dp))
            Text(text, color = if (selected) color else Color.White.copy(alpha = 0.7f), fontSize = 11.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
        }
    }
}

@Composable
private fun ActionBtn(icon: ImageVector, text: String, color: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        modifier = modifier.height(36.dp).clickable { onClick() },
        shape = RoundedCornerShape(10.dp),
        color = color.copy(alpha = 0.15f)
    ) {
        Row(Modifier.padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            Icon(icon, null, tint = color, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text(text, color = Color.White, fontSize = 12.sp)
        }
    }
}

/**
 * 🎨 Карточка документа в стиле сессий — минималистичная и красивая!
 * Фиолетовая точка слева, название, тип и дата справа
 */
@Composable
private fun CompactDocCard(
    document: DocumentResponse,
    name: String,
    type: String,
    thumbUrl: String,
    token: String?,
    isProcessing: Boolean = false,
    onTap: () -> Unit,
    onEdit: () -> Unit
) {
    val (icon, tint) = getFileTypeIconAndColor(document.mimeType)
    val purpleColor = Color(0xFF8B5CF6)
    val context = LocalContext.current
    val isImage = document.mimeType?.startsWith("image/") == true
    
    // Анимация для обработки
    val infiniteTransition = rememberInfiniteTransition(label = "proc")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
        label = "pulse"
    )
    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1500, easing = LinearEasing), RepeatMode.Restart),
        label = "shimmer"
    )
    
    // Форматируем дату
    val dateText = remember(document.uploadedAt) {
        try {
            val instant = java.time.Instant.parse(document.uploadedAt)
            val formatter = java.time.format.DateTimeFormatter.ofPattern("dd.MM HH:mm")
                .withZone(java.time.ZoneId.systemDefault())
            formatter.format(instant)
        } catch (_: Exception) { "" }
    }
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (isProcessing) purpleColor.copy(alpha = 0.08f)
                else Color.White.copy(alpha = 0.05f)
            )
            .border(
                width = 1.dp,
                color = if (isProcessing) purpleColor.copy(alpha = pulseAlpha * 0.5f)
                        else tint.copy(alpha = 0.3f),
                shape = RoundedCornerShape(14.dp)
            )
            .clickable { onTap() }
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        // Shimmer при обработке
        if (isProcessing) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                purpleColor.copy(alpha = 0.08f),
                                purpleColor.copy(alpha = 0.12f),
                                purpleColor.copy(alpha = 0.08f),
                                Color.Transparent
                            ),
                            startX = shimmerOffset * 600f - 150f,
                            endX = shimmerOffset * 600f + 150f
                        )
                    )
            )
        }
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Иконка/thumbnail слева — блок маленький, иконка БОЛЬШАЯ внутри!
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(tint.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                if (isImage) {
                    SubcomposeAsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(thumbUrl)
                            .crossfade(100)
                            .setHeader("Authorization", "Bearer ${token ?: ""}")
                            .build(),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(10.dp)),
                        contentScale = ContentScale.Crop,
                        loading = { 
                            Box(
                                Modifier.fillMaxSize().background(tint.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(icon, null, tint = tint.copy(alpha = 0.4f), modifier = Modifier.size(28.dp))
                            }
                        },
                        error = { Icon(icon, null, tint = tint, modifier = Modifier.size(32.dp)) }
                    )
                } else {
                    // Иконка файла — БОЛЬШАЯ, почти до краёв блока!
                    Icon(icon, null, tint = tint, modifier = Modifier.size(34.dp))
                }
            }
            
            Spacer(Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    
                    Spacer(Modifier.width(8.dp))
                    
                    Text(
                        text = dateText,
                        style = MaterialTheme.typography.labelSmall,
                        color = purpleColor.copy(alpha = 0.7f),
                        fontSize = 10.sp
                    )
                }
                
                Spacer(Modifier.height(3.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Тип файла
                    Text(
                        text = type.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = tint.copy(alpha = 0.8f),
                        fontSize = 10.sp,
                        letterSpacing = 0.5.sp
                    )
                    
                    // Статус обработки
                    if (isProcessing) {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "• Распознаётся...",
                            style = MaterialTheme.typography.labelSmall,
                            color = purpleColor.copy(alpha = pulseAlpha),
                            fontSize = 10.sp
                        )
                    }
                }
            }
            
            // Кнопка редактирования
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.06f))
                    .clickable { onEdit() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Edit,
                    null,
                    tint = Color.White.copy(alpha = 0.4f),
                    modifier = Modifier.size(14.dp)
                )
            }
        }
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
    // 🍎 Apple Glass чип
    GlassCard(
        modifier = Modifier.clickable { onClick() },
        cornerRadius = 10.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (selected) accentColor else GlassTheme.textSecondary,
                modifier = Modifier.size(14.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text,
                style = MaterialTheme.typography.labelSmall,
                color = if (selected) accentColor else GlassTheme.textSecondary,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun GlassSortChip(
    text: String,
    onClick: () -> Unit
) {
    // 🍎 Apple Glass чип сортировки
    GlassCard(
        modifier = Modifier.clickable { onClick() },
        cornerRadius = 10.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
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
    // 🍎 Apple Glass кнопка
    GlassCard(
        modifier = modifier
            .height(44.dp)
            .clickable { onClick() },
        cornerRadius = 12.dp
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
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
}

/**
 * 🎬 Swipe-to-delete с анимацией как в сессиях!
 * Плавный свайп, красивая кнопка удаления, bounce-эффект при удалении
 */
@Composable
private fun SwipeableDocumentItem(
    onDelete: () -> Unit,
    currentRevealedId: String?,
    itemId: String,
    onReveal: (String?) -> Unit,
    content: @Composable () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val offsetX = remember { Animatable(0f) }
    val deleteButtonWidth = 60.dp
    val density = LocalDensity.current
    val deleteButtonWidthPx = with(density) { deleteButtonWidth.toPx() }
    
    var isRevealed by remember { mutableStateOf(false) }
    var itemHeight by remember { mutableStateOf(0) }
    
    // 🎬 Telegram-style анимация удаления
    var isDeleting by remember { mutableStateOf(false) }
    val deleteAlpha = remember { Animatable(1f) }
    val deleteScale = remember { Animatable(1f) }
    val deleteHeight = remember { Animatable(1f) }
    
    val gap = 6.dp
    val gapPx = with(density) { gap.toPx() }
    val totalSwipeDistance = deleteButtonWidthPx + gapPx
    
    // Закрыть если другой элемент раскрыт
    LaunchedEffect(currentRevealedId) {
        if (currentRevealedId != itemId && isRevealed) {
            offsetX.animateTo(0f, tween(180, easing = FastOutSlowInEasing))
            isRevealed = false
        }
    }
    
    // Прямой расчёт прогресса - мгновенная реакция!
    val revealProgress = (-offsetX.value / totalSwipeDistance).coerceIn(0f, 1f)
    
    // Если удаление завершено - не показываем
    if (deleteAlpha.value <= 0.01f) return
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                // GPU-accelerated анимация удаления
                alpha = deleteAlpha.value
                scaleX = deleteScale.value
                scaleY = deleteScale.value
            }
            .then(
                if (isDeleting) {
                    Modifier.height(with(density) { (itemHeight * deleteHeight.value).toDp() })
                } else Modifier
            )
    ) {
        // Кнопка удаления — GPU-accelerated
        if (revealProgress > 0.01f && !isDeleting) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .width(deleteButtonWidth)
                    .height(with(density) { (itemHeight - 4).toDp() })
                    .graphicsLayer { 
                        // Всё через graphicsLayer для 120Hz плавности
                        alpha = revealProgress
                        scaleX = 0.85f + (revealProgress * 0.15f)
                        scaleY = 0.85f + (revealProgress * 0.15f)
                    }
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFFE53935),
                                Color(0xFFD32F2F)
                            )
                        )
                    )
                    .clickable {
                        if (isDeleting) return@clickable
                        isDeleting = true
                        
                        // 🎬 Плавная анимация удаления — как в сессиях!
                        coroutineScope.launch {
                            // Параллельные анимации для плавности
                            launch { 
                                deleteAlpha.animateTo(0f, tween(250, easing = FastOutSlowInEasing)) 
                            }
                            launch { 
                                deleteScale.animateTo(0.8f, tween(250, easing = FastOutSlowInEasing)) 
                            }
                            launch { 
                                deleteHeight.animateTo(0f, tween(300, easing = FastOutSlowInEasing)) 
                            }
                            
                            // Ждём завершения самой длинной анимации
                            kotlinx.coroutines.delay(300)
                            
                            onDelete()
                            onReveal(null)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Удалить",
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
        
        // Основной контент — GPU-accelerated
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .onSizeChanged { if (!isDeleting) itemHeight = it.height }
                .graphicsLayer { translationX = offsetX.value }
                .pointerInput(isDeleting) {
                    if (isDeleting) return@pointerInput
                    detectHorizontalDragGestures(
                        onDragStart = { },
                        onDragEnd = {
                            coroutineScope.launch {
                                if (offsetX.value < -totalSwipeDistance / 2) {
                                    offsetX.animateTo(-totalSwipeDistance, tween(180, easing = FastOutSlowInEasing))
                                    isRevealed = true
                                    onReveal(itemId)
                                } else {
                                    offsetX.animateTo(0f, tween(180, easing = FastOutSlowInEasing))
                                    isRevealed = false
                                    if (currentRevealedId == itemId) onReveal(null)
                                }
                            }
                        },
                        onDragCancel = {
                            coroutineScope.launch {
                                offsetX.animateTo(0f, tween(150))
                                isRevealed = false
                            }
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            coroutineScope.launch {
                                val newOffset = (offsetX.value + dragAmount).coerceIn(-totalSwipeDistance, 0f)
                                offsetX.snapTo(newOffset)
                            }
                        }
                    )
                }
                .clickable(enabled = isRevealed) {
                    coroutineScope.launch {
                        offsetX.animateTo(0f, tween(150))
                        isRevealed = false
                        onReveal(null)
                    }
                }
        ) {
            content()
        }
    }
}

/**
 * Компонент загружающегося документа с красивой анимацией
 */
@Composable
private fun UploadingDocumentItem(
    pending: PendingUpload,
    onRetry: () -> Unit,
    onCancel: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "upload")
    
    // Shimmer эффект для загрузки
    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer"
    )
    
    // Пульсация иконки
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )
    
    val isError = pending.status == UploadStatus.ERROR
    val borderColor = if (isError) Color(0xFFE53935) else Color(0xFF6366F1)
    
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        cornerRadius = 14.dp,
        backgroundColor = GlassTheme.glassWhite,
        borderColor = borderColor.copy(alpha = 0.6f)
    ) {
        Box {
            // Shimmer overlay для загрузки
            if (!isError) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color(0xFF6366F1).copy(alpha = 0.08f),
                                    Color(0xFF8B5CF6).copy(alpha = 0.12f),
                                    Color(0xFF6366F1).copy(alpha = 0.08f),
                                    Color.Transparent
                                ),
                                startX = shimmerOffset * 1000f - 200f,
                                endX = shimmerOffset * 1000f + 200f
                            )
                        )
                )
            }
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Иконка с анимацией
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .scale(if (!isError) pulse else 1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (isError) Color(0xFFE53935).copy(alpha = 0.15f)
                            else Color(0xFF6366F1).copy(alpha = 0.15f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isError) {
                        Icon(
                            Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = Color(0xFFE53935),
                            modifier = Modifier.size(26.dp)
                        )
                    } else {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color(0xFF6366F1),
                            strokeWidth = 2.5.dp
                        )
                    }
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                // Название и статус
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = pending.filename.substringBeforeLast('.'),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = GlassTheme.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isError) {
                            Text(
                                text = pending.error?.take(40) ?: "Ошибка загрузки",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFE53935),
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        } else {
                            Text(
                                text = "Загрузка на сервер...",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF6366F1),
                                fontSize = 11.sp
                            )
                        }
                        
                        Spacer(Modifier.width(8.dp))
                        
                        Text(
                            text = pending.filename.substringAfterLast('.', "").uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = GlassTheme.textTertiary,
                            fontSize = 10.sp,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
                
                // Кнопки действий
                if (isError) {
                    // Retry
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF6366F1).copy(alpha = 0.15f))
                            .clickable { onRetry() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Повторить",
                            tint = Color(0xFF6366F1),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                }
                
                // Cancel/Remove
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.08f))
                        .clickable { onCancel() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Отменить",
                        tint = GlassTheme.textSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

/**
 * Компонент документа с thumbnail, smartTitle
 * Показывает анимацию обработки если status = "processing"
 */
@Composable
private fun GlassDocumentItemV2(
    document: DocumentResponse,
    displayName: String,
    isAwaitingAi: Boolean,
    fileTypeLabel: String,
    thumbnailUrl: String,
    authToken: String?,
    onPreview: () -> Unit,
    onRename: () -> Unit
) {
    val (icon, tint) = getFileTypeIconAndColor(document.mimeType)
    val isImage = document.mimeType?.startsWith("image/") == true
    val context = LocalContext.current
    
    // Показываем анимацию обработки если ИИ ещё не дал smartTitle
    val isProcessing = isAwaitingAi
    
    // Анимация для обработки
    val infiniteTransition = rememberInfiniteTransition(label = "processing")
    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )
    
    val purpleColor = Color(0xFF8B5CF6)
    
    // 🍎 Apple Glass эффект
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onPreview() },
        cornerRadius = 12.dp
    ) {
        Box {
            // Shimmer для обработки (поверх glass)
            if (isProcessing) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color(0xFF8B5CF6).copy(alpha = 0.08f),
                                    Color(0xFF6366F1).copy(alpha = 0.15f),
                                    Color(0xFF8B5CF6).copy(alpha = 0.08f),
                                    Color.Transparent
                                ),
                                startX = shimmerOffset * 800f - 200f,
                                endX = shimmerOffset * 800f + 200f
                            )
                        )
                )
            }
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),  // Компактный padding!
                verticalAlignment = Alignment.CenterVertically
            ) {
            // Thumbnail — блок 44dp, иконка БОЛЬШАЯ внутри!
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(tint.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                if (isImage) {
                    SubcomposeAsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(thumbnailUrl)
                            .crossfade(150)
                            .memoryCacheKey(thumbnailUrl)
                            .diskCacheKey(thumbnailUrl)
                            .setHeader("Authorization", "Bearer ${authToken ?: ""}")
                            .build(),
                        contentDescription = displayName,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(10.dp)),
                        contentScale = ContentScale.Crop,
                        loading = {
                            Box(
                                Modifier
                                    .fillMaxSize()
                                    .background(tint.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(icon, null, tint = tint.copy(alpha = 0.3f), modifier = Modifier.size(28.dp))
                            }
                        },
                        error = {
                            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(32.dp))
                        }
                    )
                } else {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = tint,
                        modifier = Modifier.size(34.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Info - с отступом справа чтобы не налезало на кнопку
            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = GlassTheme.textPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 18.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Статус обработки или тип файла
                    if (isProcessing) {
                        Text(
                            text = "Распознаётся...",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF8B5CF6),
                            fontSize = 11.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text(
                        text = fileTypeLabel.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = GlassTheme.textTertiary,
                        letterSpacing = 0.8.sp,
                        fontSize = 10.sp
                    )
                }
            }

            // Индикатор обработки (слева от кнопки редактирования)
            if (isProcessing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = Color(0xFF8B5CF6),
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            
            // Только кнопка редактирования (удаление - через свайп)
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
                    tint = GlassTheme.textSecondary,
                    modifier = Modifier.size(16.dp)
                )
            }
            }  // Row
        }  // Box (for shimmer)
    }  // FrostedGlassCard
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
    // Если по фильтру ничего нет - просто ничего не показываем
    if (!isEmpty) return
    
    // Только когда совсем пусто - лаконичный текст
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 60.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Text(
            text = "Загрузите файлы",
            style = MaterialTheme.typography.bodyMedium,
            color = GlassTheme.textTertiary
        )
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
    val raw = document.uploadedAt ?: return 0L
    return try {
        Instant.parse(raw).toEpochMilli()
    } catch (_: DateTimeParseException) {
        0L
    }
}

/**
 * Безрамочный Preview — прозрачный фон, фото всплывает поверх приложения
 */
@Composable
private fun ImagePreviewDialog(
    imageUrl: String,
    title: String,
    authToken: String?,
    onDismiss: () -> Unit,
    onDownload: () -> Unit
) {
    val context = LocalContext.current
    
    // Animation
    var visible by remember { mutableStateOf(false) }
    val animatedAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(200),
        label = "alpha"
    )
    val animatedScale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.92f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 500f),
        label = "scale"
    )
    
    LaunchedEffect(Unit) { visible = true }
    
    // Zoom/Pan
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var imageSize by remember { mutableStateOf(IntSize.Zero) }
    
    val transformState = rememberTransformableState { zoomChange, panChange, _ ->
        val newScale = (scale * zoomChange).coerceIn(1f, 4f)
        scale = newScale
        
        if (newScale > 1f && imageSize.width > 0) {
            val maxX = imageSize.width * (newScale - 1) / 2
            val maxY = imageSize.height * (newScale - 1) / 2
            offset = Offset(
                x = (offset.x + panChange.x).coerceIn(-maxX, maxX),
                y = (offset.y + panChange.y).coerceIn(-maxY, maxY)
            )
        } else {
            offset = Offset.Zero
        }
    }
    
    var lastTapTime by remember { mutableLongStateOf(0L) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = animatedAlpha }
                // Полупрозрачный фон - видно приложение сзади
                .background(Color.Black.copy(alpha = 0.6f))
                .clickable(
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                )
                .systemBarsPadding(),
            contentAlignment = Alignment.Center
        ) {
            // Изображение строго по размеру, без scroll
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(context)
                    .data(imageUrl)
                    .crossfade(150)
                    .memoryCacheKey(imageUrl)
                    .diskCacheKey(imageUrl)
                    .setHeader("Authorization", "Bearer ${authToken ?: ""}")
                    .build(),
                contentDescription = title,
                modifier = Modifier
                    .graphicsLayer {
                        scaleX = animatedScale * scale
                        scaleY = animatedScale * scale
                        translationX = offset.x
                        translationY = offset.y
                    }
                    .padding(horizontal = 16.dp)
                    .onSizeChanged { imageSize = it }
                    .clip(RoundedCornerShape(16.dp))
                    .transformable(state = transformState)
                    .clickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null
                    ) {
                        val now = System.currentTimeMillis()
                        if (now - lastTapTime < 300) {
                            scale = 1f
                            offset = Offset.Zero
                        }
                        lastTapTime = now
                    },
                contentScale = ContentScale.Fit,
                loading = {
                    Box(
                        modifier = Modifier.size(80.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(32.dp),
                            strokeWidth = 2.dp
                        )
                    }
                },
                error = {
                    Box(
                        modifier = Modifier.size(80.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.BrokenImage,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.4f),
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }
            )
            
            // Крестик снизу
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 40.dp)
                    .graphicsLayer { alpha = animatedAlpha * 0.9f }
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.1f))
                    .clickable { onDismiss() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Закрыть",
                    tint = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/**
 * PDF/DOC Preview — встроенный просмотр через WebView
 */
@Composable
private fun PdfPreviewDialog(
    documentUrl: String,
    downloadUrl: String,
    title: String,
    authToken: String?,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    
    // Animation
    var visible by remember { mutableStateOf(false) }
    val animatedAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(200),
        label = "alpha"
    )
    val animatedOffset by animateFloatAsState(
        targetValue = if (visible) 0f else 100f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f),
        label = "offset"
    )
    
    LaunchedEffect(Unit) { visible = true }
    
    var isLoading by remember { mutableStateOf(true) }
    var hasError by remember { mutableStateOf(false) }
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { 
                    alpha = animatedAlpha
                    translationY = animatedOffset
                }
                .background(Color(0xFF0A0E27))
                .systemBarsPadding()
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF151929))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Назад",
                            tint = Color.White
                        )
                    }
                    
                    Text(
                        text = title,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp)
                    )
                    
                    // Download button
                    IconButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl))
                            context.startActivity(intent)
                        }
                    ) {
                        Icon(
                            Icons.Default.Download,
                            contentDescription = "Скачать",
                            tint = Color.White
                        )
                    }
                    
                    // Open externally
                    IconButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(documentUrl))
                            context.startActivity(intent)
                        }
                    ) {
                        Icon(
                            Icons.Default.OpenInNew,
                            contentDescription = "Открыть",
                            tint = Color.White
                        )
                    }
                }
                
                // WebView для PDF
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                ) {
                    AndroidView(
                        factory = { ctx ->
                            android.webkit.WebView(ctx).apply {
                                settings.apply {
                                    javaScriptEnabled = true
                                    loadWithOverviewMode = true
                                    useWideViewPort = true
                                    builtInZoomControls = true
                                    displayZoomControls = false
                                    setSupportZoom(true)
                                    domStorageEnabled = true
                                    allowFileAccess = true
                                }
                                
                                setBackgroundColor(android.graphics.Color.parseColor("#0A0E27"))
                                
                                webViewClient = object : android.webkit.WebViewClient() {
                                    override fun onPageFinished(view: android.webkit.WebView?, url: String?) {
                                        isLoading = false
                                    }
                                    
                                    override fun onReceivedError(
                                        view: android.webkit.WebView?,
                                        errorCode: Int,
                                        description: String?,
                                        failingUrl: String?
                                    ) {
                                        hasError = true
                                        isLoading = false
                                    }
                                }
                                
                                // Добавляем auth header если есть
                                val headers = mutableMapOf<String, String>()
                                if (!authToken.isNullOrEmpty()) {
                                    headers["Authorization"] = "Bearer $authToken"
                                }
                                
                                // Используем Google Docs Viewer для PDF
                                val encodedUrl = java.net.URLEncoder.encode(documentUrl, "UTF-8")
                                val googleDocsUrl = "https://docs.google.com/gview?embedded=true&url=$encodedUrl"
                                
                                loadUrl(googleDocsUrl, headers)
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                    
                    // Loading indicator
                    if (isLoading) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                CircularProgressIndicator(
                                    color = Color(0xFF8B5CF6),
                                    modifier = Modifier.size(40.dp),
                                    strokeWidth = 3.dp
                                )
                                Text(
                                    "Загрузка документа...",
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                    
                    // Error state
                    if (hasError) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    Icons.Default.ErrorOutline,
                                    contentDescription = null,
                                    tint = Color(0xFFE53935),
                                    modifier = Modifier.size(48.dp)
                                )
                                Text(
                                    "Не удалось загрузить",
                                    color = Color.White.copy(alpha = 0.9f),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    "Попробуйте открыть внешне",
                                    color = Color.White.copy(alpha = 0.6f),
                                    fontSize = 14.sp
                                )
                                
                                Spacer(Modifier.height(8.dp))
                                
                                TextButton(
                                    onClick = {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(documentUrl))
                                        context.startActivity(intent)
                                    }
                                ) {
                                    Icon(
                                        Icons.Default.OpenInNew,
                                        contentDescription = null,
                                        tint = Color(0xFF8B5CF6),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        "Открыть в браузере",
                                        color = Color(0xFF8B5CF6)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
