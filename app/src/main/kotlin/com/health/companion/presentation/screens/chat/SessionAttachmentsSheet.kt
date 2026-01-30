package com.health.companion.presentation.screens.chat

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.health.companion.BuildConfig
import com.health.companion.data.remote.api.AttachmentDTO
import com.health.companion.data.remote.api.AttachmentMode
import com.health.companion.presentation.components.*
import java.text.SimpleDateFormat
import java.util.*

private val API_HOST = BuildConfig.API_BASE_URL.removeSuffix("/api/v1")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionAttachmentsSheet(
    attachments: List<AttachmentDTO>,
    isLoading: Boolean,
    authToken: String?,
    onUpload: (Uri) -> Unit,
    onDelete: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    
    // File picker
    val filePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { onUpload(it) }
    }
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.Transparent,
        dragHandle = null,
        scrimColor = Color.Black.copy(alpha = 0.6f)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.6f)
                .background(
                    brush = GlassGradients.backgroundVertical,
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                )
                .border(
                    width = 1.dp,
                    color = GlassColors.whiteOverlay10,
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 8.dp)
            ) {
                // Handle
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .width(40.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(GlassColors.whiteOverlay20)
                )
                
                Spacer(Modifier.height(16.dp))
                
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Контекст сессии",
                            style = GlassTypography.heading,
                            color = GlassColors.textPrimary
                        )
                        Text(
                            text = "${attachments.size} файл(ов)",
                            style = GlassTypography.labelSmall,
                            color = GlassColors.textTertiary
                        )
                    }
                    
                    // Upload button
                    FilledTonalButton(
                        onClick = {
                            filePicker.launch(arrayOf(
                                "image/*",
                                "application/pdf",
                                "application/msword",
                                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                                "text/*"
                            ))
                        },
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = GlassColors.accent.copy(alpha = 0.2f),
                            contentColor = GlassColors.accent
                        )
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Добавить")
                    }
                }
                
                Spacer(Modifier.height(16.dp))
                
                // Content
                if (isLoading && attachments.isEmpty()) {
                    // Loading skeleton
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = GlassColors.accent,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                } else if (attachments.isEmpty()) {
                    // Empty state
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.FolderOpen,
                                contentDescription = null,
                                tint = GlassColors.textTertiary,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = "Нет вложений",
                                style = GlassTypography.messageText,
                                color = GlassColors.textSecondary
                            )
                            Text(
                                text = "Добавьте файлы для контекста чата",
                                style = GlassTypography.labelSmall,
                                color = GlassColors.textTertiary
                            )
                        }
                    }
                } else {
                    // Attachments list
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(attachments, key = { it.id }) { attachment ->
                            AttachmentItem(
                                attachment = attachment,
                                authToken = authToken,
                                onDelete = { onDelete(attachment.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AttachmentItem(
    attachment: AttachmentDTO,
    authToken: String?,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(GlassColors.surface.copy(alpha = 0.6f))
            .border(1.dp, GlassColors.whiteOverlay10, RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Thumbnail or icon
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(getTypeColor(attachment.type).copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            if (attachment.type == "image" && attachment.url != null) {
                val fullUrl = remember(attachment.url) {
                    if (attachment.url.startsWith("http")) attachment.url
                    else "$API_HOST${attachment.url}"
                }
                
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(fullUrl)
                        .crossfade(true)
                        .apply {
                            if (!authToken.isNullOrBlank()) {
                                addHeader("Authorization", "Bearer $authToken")
                            }
                        }
                        .build(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    imageVector = getTypeIcon(attachment.type),
                    contentDescription = null,
                    tint = getTypeColor(attachment.type),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        
        Spacer(Modifier.width(12.dp))
        
        // Info
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = attachment.filename,
                style = GlassTypography.messageText,
                color = GlassColors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Status badge
                val statusColor = when (attachment.status) {
                    "ready" -> GlassColors.mint
                    "processing" -> GlassColors.orange
                    "error" -> GlassColors.coral
                    else -> GlassColors.textTertiary
                }
                val statusText = when (attachment.status) {
                    "ready" -> "Готов"
                    "processing" -> "Обработка..."
                    "error" -> "Ошибка"
                    else -> ""
                }
                
                if (statusText.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(statusColor)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = statusText,
                        style = GlassTypography.timestamp,
                        color = statusColor
                    )
                    Spacer(Modifier.width(8.dp))
                }
                
                // File size
                attachment.fileSize?.let { size ->
                    Text(
                        text = formatFileSize(size),
                        style = GlassTypography.timestamp,
                        color = GlassColors.textTertiary
                    )
                }
            }
            
            // Extracted text preview
            attachment.extractedText?.let { text ->
                if (text.isNotBlank()) {
                    Text(
                        text = text.take(100) + if (text.length > 100) "..." else "",
                        style = GlassTypography.timestamp.copy(fontSize = 10.sp),
                        color = GlassColors.textTertiary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
        
        // Delete button
        IconButton(
            onClick = onDelete,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                Icons.Default.Delete,
                contentDescription = "Удалить",
                tint = GlassColors.textTertiary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

private fun getTypeIcon(type: String) = when (type) {
    "image" -> Icons.Default.Image
    "document" -> Icons.Default.Description
    "audio" -> Icons.Default.AudioFile
    else -> Icons.Default.InsertDriveFile
}

private fun getTypeColor(type: String) = when (type) {
    "image" -> GlassColors.accent
    "document" -> GlassColors.orange
    "audio" -> GlassColors.teal
    else -> GlassColors.textTertiary
}

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        else -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
    }
}
