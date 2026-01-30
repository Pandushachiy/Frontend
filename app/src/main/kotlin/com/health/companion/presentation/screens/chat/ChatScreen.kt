package com.health.companion.presentation.screens.chat

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Environment
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.HapticFeedbackConstantsCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.health.companion.data.remote.api.MessageDTO
import com.health.companion.presentation.components.*
import com.health.companion.utils.VoiceEventLogger
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Pattern
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.layout.ContentScale
import android.graphics.Color as AndroidColor

@Composable
fun ChatScreen(
    viewModel: ChatViewModel = hiltViewModel(),
    bottomBarPadding: PaddingValues = PaddingValues(0.dp),
    onMessageSent: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val messages by viewModel.messages.collectAsState()
    
    // Debug: log when messages change
    LaunchedEffect(messages.size, messages.lastOrNull()?.content?.length) {
        android.util.Log.d("UI_DEBUG", "Messages recompose: size=${messages.size}, lastLen=${messages.lastOrNull()?.content?.length}")
    }
    val currentMessage by viewModel.currentMessage.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val isRecording by viewModel.isRecording.collectAsState()
    val partialVoiceResult by viewModel.partialVoiceResult.collectAsState()
    val isUploading by viewModel.isUploading.collectAsState()
    val conversations by viewModel.conversations.collectAsState()
    val streamStatus by viewModel.streamStatus.collectAsState()
    val isStreaming by viewModel.isStreaming.collectAsState()
    val currentConversationId by viewModel.currentConversationId.collectAsState()
    val messageSendStatus by viewModel.messageSendStatus.collectAsState()
    val authToken by viewModel.authToken.collectAsState()
    val attachedImageUri by viewModel.attachedImageUri.collectAsState()
    val sessionAttachments by viewModel.sessionAttachments.collectAsState()
    val attachmentsLoading by viewModel.attachmentsLoading.collectAsState()

    // Telegram-style: reverseLayout = true, scroll to 0 = bottom
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current
    val context = LocalContext.current
    val density = LocalDensity.current
    val haptics = LocalHapticFeedback.current
    val view = LocalView.current
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        viewModel.voiceEvents.collect { event ->
            when (event) {
                is VoiceUiEvent.RecordingStarted -> performVoiceHaptic(context, haptics, view, VoiceHaptic.Start)
                is VoiceUiEvent.RecordingStopped -> performVoiceHaptic(context, haptics, view, VoiceHaptic.Stop)
                is VoiceUiEvent.Error -> performVoiceHaptic(context, haptics, view, VoiceHaptic.Error)
            }
        }
    }

    var showAttachMenu by remember { mutableStateOf(false) }
    var photoUri by remember { mutableStateOf<Uri?>(null) }
    var showChatsSheet by remember { mutableStateOf(false) }
    var showAttachmentsSheet by remember { mutableStateOf(false) }
    val chatSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Permissions
    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED
        )
    }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED
        )
    }

    // Permission launchers
    var pendingVoiceAfterPermission by remember { mutableStateOf(false) }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasAudioPermission = isGranted
        VoiceEventLogger.log(context, "audio_permission=$isGranted")
        if (isGranted) {
            pendingVoiceAfterPermission = true
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    // Camera launcher
    val takePictureLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && photoUri != null) {
            viewModel.uploadFile(photoUri!!)
            showAttachMenu = false
        }
    }

    // Gallery picker (for document upload)
    val pickImageLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.uploadFile(it)
            showAttachMenu = false
        }
    }
    
    // Image picker for Image-to-Image editing (attaches to message)
    val attachImageLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.attachImage(it)
            showAttachMenu = false
        }
    }

    // File picker
    val pickFileLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.uploadFile(it)
            showAttachMenu = false
        }
    }

    // Create photo Uri
    fun createPhotoUri(): Uri {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val imageFile = File.createTempFile("IMG_${timeStamp}_", ".jpg", storageDir)
        return FileProvider.getUriForFile(context, "${context.packageName}.provider", imageFile)
    }

    // Reversed messages for reverseLayout (newest at index 0)
    val reversedMessages = remember(messages) { messages.reversed() }

    // Auto-scroll to bottom (index 0 in reverseLayout) when new message arrives
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            delay(100)
            listState.animateScrollToItem(0)
        }
    }

    // Chats bottom sheet - Feyberry Style
    if (showChatsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showChatsSheet = false },
            sheetState = chatSheetState,
            containerColor = Color.Transparent,
            dragHandle = null,
            scrimColor = Color.Black.copy(alpha = 0.6f)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                    .background(GlassTheme.backgroundGradient)
            ) {
                // Glass overlay
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .matchParentSize()
                        .background(Color.White.copy(alpha = 0.03f))
                )
                
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 40.dp)
                ) {
                    // Drag handle
                    Box(
                        modifier = Modifier
                            .padding(vertical = 14.dp)
                            .align(Alignment.CenterHorizontally)
                            .size(48.dp, 4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        GlassTheme.accentPrimary.copy(alpha = 0.3f),
                                        GlassTheme.accentSecondary.copy(alpha = 0.5f),
                                        GlassTheme.accentPrimary.copy(alpha = 0.3f)
                                    )
                                )
                            )
                    )
                    
                    Spacer(Modifier.height(8.dp))
                    
                    // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Berry icon
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(
                                        Brush.linearGradient(
                                            colors = listOf(
                                                GlassTheme.accentPrimary.copy(alpha = 0.2f),
                                                GlassTheme.accentSecondary.copy(alpha = 0.15f)
                                            )
                                        )
                                    )
                                    .border(
                                        1.dp,
                                        Brush.linearGradient(
                                            colors = listOf(
                                                GlassTheme.accentPrimary.copy(alpha = 0.4f),
                                                GlassTheme.accentSecondary.copy(alpha = 0.2f)
                                            )
                                        ),
                                        RoundedCornerShape(14.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("🫐", fontSize = 24.sp)
                            }
                            Spacer(Modifier.width(14.dp))
                            Column {
                    Text(
                                    text = "Диалоги",
                        style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = if (conversations.isEmpty()) "Нет сессий" 
                                           else "${conversations.size} ${if (conversations.size == 1) "сессия" else "сессий"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.5f)
                                )
                            }
                        }
                        
                        // New chat button
                        Box(
                            modifier = Modifier
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
                        showChatsSheet = false
                                    coroutineScope.launch {
                                        delay(200)
                                        viewModel.createNewConversation()
                                    }
                                }
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    "Новый",
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                    }
                }

                    Spacer(Modifier.height(24.dp))

                if (conversations.isEmpty()) {
                        // Empty state - Feyberry style
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color.White.copy(alpha = 0.05f))
                                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(20.dp))
                                .padding(vertical = 48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                // Animated berry
                                Box(
                                    modifier = Modifier
                                        .size(72.dp)
                                        .clip(CircleShape)
                                        .background(
                                            Brush.radialGradient(
                                                colors = listOf(
                                                    GlassTheme.accentPrimary.copy(alpha = 0.2f),
                                                    Color.Transparent
                                                )
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("🫐", fontSize = 40.sp)
                                }
                                Spacer(Modifier.height(20.dp))
                    Text(
                                    text = "Начни первый диалог",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White.copy(alpha = 0.9f)
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = "Нажми «Новый» чтобы создать чат",
                        style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(alpha = 0.5f)
                    )
                            }
                        }
                } else {
                        // Conversations list
                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                    conversations.forEach { convo ->
                                val isSelected = convo.id == currentConversationId
                                val dateFormat = remember { SimpleDateFormat("dd MMM, HH:mm", Locale("ru")) }
                                
                                Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                        .clip(RoundedCornerShape(18.dp))
                                        .background(
                                            if (isSelected)
                                                Brush.linearGradient(
                                                    colors = listOf(
                                                        GlassTheme.accentPrimary.copy(alpha = 0.15f),
                                                        GlassTheme.accentSecondary.copy(alpha = 0.1f)
                                                    )
                                                )
                                            else
                                                Brush.linearGradient(
                                                    colors = listOf(
                                                        Color.White.copy(alpha = 0.06f),
                                                        Color.White.copy(alpha = 0.03f)
                                                    )
                                                )
                                        )
                                        .border(
                                            1.dp,
                                            if (isSelected)
                                                Brush.linearGradient(
                                                    colors = listOf(
                                                        GlassTheme.accentPrimary.copy(alpha = 0.4f),
                                                        GlassTheme.accentSecondary.copy(alpha = 0.2f)
                                                    )
                                                )
                                            else
                                                Brush.linearGradient(
                                                    colors = listOf(
                                                        Color.White.copy(alpha = 0.1f),
                                                        Color.White.copy(alpha = 0.05f)
                                                    )
                                                ),
                                            RoundedCornerShape(18.dp)
                                        )
                                    .clickable {
                                        val selectedId = convo.id
                                        showChatsSheet = false
                                        // Delay selection to let sheet close smoothly first
                                        coroutineScope.launch {
                                            delay(200)
                                            viewModel.selectConversation(selectedId)
                                        }
                                    }
                                        .padding(16.dp)
                        ) {
                            Row(
                                        modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                        // Chat avatar
                                        Box(
                                            modifier = Modifier
                                                .size(46.dp)
                                                .clip(RoundedCornerShape(13.dp))
                                                .background(
                                                    if (isSelected)
                                                        Brush.linearGradient(
                                                            colors = listOf(
                                                                GlassTheme.accentPrimary.copy(alpha = 0.25f),
                                                                GlassTheme.accentSecondary.copy(alpha = 0.15f)
                                                            )
                                                        )
                                                    else
                                                        Brush.linearGradient(
                                                            colors = listOf(
                                                                Color.White.copy(alpha = 0.08f),
                                                                Color.White.copy(alpha = 0.04f)
                                                            )
                                                        )
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(text = "🫐", fontSize = 22.sp)
                                        }
                                        
                                        Spacer(Modifier.width(14.dp))
                                        
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                                text = convo.title.ifEmpty { "Новый диалог" },
                                        style = MaterialTheme.typography.bodyLarge,
                                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                                color = Color.White.copy(alpha = if (isSelected) 1f else 0.9f),
                                        maxLines = 1
                                    )
                                            Spacer(Modifier.height(4.dp))
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(6.dp)
                                                        .clip(CircleShape)
                                                        .background(
                                                            if (isSelected) GlassTheme.accentPrimary
                                                            else Color.White.copy(alpha = 0.3f)
                                                        )
                                                )
                                                Spacer(Modifier.width(8.dp))
                                    Text(
                                                    text = dateFormat.format(Date(convo.updatedAt)),
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = Color.White.copy(alpha = 0.5f)
                                    )
                                }
                                        }
                                        
                                        // Actions
                                        if (isSelected) {
                                            Box(
                                                modifier = Modifier
                                                    .size(32.dp)
                                                    .clip(CircleShape)
                                                    .background(GlassTheme.accentPrimary.copy(alpha = 0.2f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                    Icon(
                                                    Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = GlassTheme.accentPrimary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        } else {
                                            Box(
                                                modifier = Modifier
                                                    .size(32.dp)
                                                    .clip(CircleShape)
                                                    .background(Color.White.copy(alpha = 0.05f))
                                                    .clickable { viewModel.deleteConversation(convo.id) },
                                                contentAlignment = Alignment.Center
                                            ) {
                                    Icon(
                                                    Icons.Default.Close,
                                                    contentDescription = "Удалить",
                                                    tint = Color.White.copy(alpha = 0.4f),
                                                    modifier = Modifier.size(16.dp)
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
        }
    }

    // Get bottom padding from Scaffold
    val navBarPadding = bottomBarPadding.calculateBottomPadding()
    
    // Calculate ime insets directly
    val imeInsets = WindowInsets.ime
    val imeDensity = LocalDensity.current
    val imeBottom = imeInsets.getBottom(imeDensity)
    val imeBottomDp = with(imeDensity) { imeBottom.toDp() }
    
    // Use MAX of (ime height) or (navbar padding) - never both!
    val effectiveBottom = maxOf(imeBottomDp, navBarPadding)
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(GlassGradients.backgroundVertical)
            .padding(bottom = effectiveBottom)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Compact header с кнопкой сессий
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Session selector button
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(GlassColors.surface.copy(alpha = 0.6f))
                        .border(1.dp, GlassColors.whiteOverlay10, RoundedCornerShape(20.dp))
                        .clickable { showChatsSheet = true }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Chat,
                        contentDescription = "Чаты",
                        tint = GlassColors.accent,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Чаты",
                        style = MaterialTheme.typography.labelMedium,
                        color = GlassColors.textPrimary,
                        fontWeight = FontWeight.Medium
                    )
                    if (conversations.isNotEmpty()) {
                        Spacer(Modifier.width(4.dp))
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .background(GlassColors.accent, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (conversations.size > 9) "9+" else conversations.size.toString(),
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                
                // New chat button
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(GlassColors.accent, GlassColors.accentSecondary)
                            )
                        )
                        .clickable { viewModel.createNewConversation() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Новый чат",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            
            // Messages list - takes all available space
            // Клик на список закрывает панель прикрепления
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null // Без визуального эффекта
                    ) { 
                        if (showAttachMenu) showAttachMenu = false 
                    },
                reverseLayout = true, // TELEGRAM-STYLE: newest messages at bottom
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 8.dp,
                    bottom = 12.dp
                ),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Image generation animation - показываем при генерации картинки
                // Детектим запрос на генерацию по ключевым словам в последнем сообщении юзера
                val lastUserMessage = messages.lastOrNull { it.role == "user" }
                val lastUserMsg = lastUserMessage?.content?.lowercase() ?: ""
                val lastUserHadImage = !lastUserMessage?.images.isNullOrEmpty() // Image-to-Image!
                
                // Ключевые слова для генерации изображений
                val isTextToImageRequest = lastUserMsg.contains("сгенерируй") ||
                                           lastUserMsg.contains("нарисуй") ||
                                           lastUserMsg.contains("создай картинку") ||
                                           lastUserMsg.contains("создай изображение") ||
                                           lastUserMsg.contains("generate") ||
                                           lastUserMsg.contains("draw")
                
                // Ключевые слова для Image-to-Image (редактирование)
                val isEditKeyword = lastUserMsg.contains("сделай") ||
                                    lastUserMsg.contains("измени") ||
                                    lastUserMsg.contains("добавь") ||
                                    lastUserMsg.contains("убери") ||
                                    lastUserMsg.contains("удали") ||
                                    lastUserMsg.contains("замени") ||
                                    lastUserMsg.contains("поменяй") ||
                                    lastUserMsg.contains("фон") ||
                                    lastUserMsg.contains("edit") ||
                                    lastUserMsg.contains("change") ||
                                    lastUserMsg.contains("remove") ||
                                    lastUserMsg.contains("add")
                
                // Image-to-Image: если прикреплено фото + ключевое слово редактирования
                val isImageToImageRequest = lastUserHadImage && isEditKeyword
                val isImageRequest = isTextToImageRequest || isImageToImageRequest
                
                val lastMsg = messages.lastOrNull()
                val lastMsgImageUrl = lastMsg?.imageUrl
                val lastMsgAgent = lastMsg?.agent_name
                val lastMsgContent = lastMsg?.content ?: ""
                
                // Проверяем есть ли уже результат (текст или изображение)
                val hasResult = lastMsg?.role == "assistant" && 
                               (lastMsgContent.length > 10 || (lastMsgImageUrl != null && lastMsgAgent != "streaming"))
                
                // Показываем анимацию генерации если:
                // 1. Бэк прислал статус генерации И нет результата
                // 2. ИЛИ идёт загрузка для image request
                // НЕ показываем если уже есть результат!
                val isGeneratingImage = !hasResult && (
                                        streamStatus == "generating_image" ||
                                        streamStatus == "generating" ||
                                        streamStatus.contains("image", ignoreCase = true) ||
                                        streamStatus.contains("generat", ignoreCase = true) ||
                                        // Сразу показываем анимацию для запросов генерации
                                        (isLoading && isImageRequest) ||
                                        // Image-to-Image: если отправлено фото - сразу анимация
                                        (isLoading && lastUserHadImage)
                                        )
                
                // Показываем анимацию генерации
                if (isGeneratingImage) {
                    item(key = "image_generation_animation") {
                        // Запоминаем тип анимации при первом показе чтобы не переключалась
                        val rememberedImages = remember { lastUserMessage?.images }
                        val useImageToImage = remember { !rememberedImages.isNullOrEmpty() }
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.Start
                        ) {
                            // AI Avatar
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(
                                        Brush.linearGradient(
                                            colors = listOf(
                                                Color(0xFF6366F1),
                                                Color(0xFF8B5CF6)
                                            )
                                        ),
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("✨", fontSize = 16.sp)
                            }
                            
                            Spacer(Modifier.width(8.dp))
                            
                            // Выбираем анимацию: Image-to-Image или обычная генерация
                            // Используем запомненные значения для стабильности
                            if (useImageToImage && rememberedImages != null) {
                                // Image-to-Image: показываем исходные фото с эффектами трансформации
                                ImageToImageAnimation(
                                    sourceImageUris = rememberedImages,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                            } else {
                                // Text-to-Image: обычная анимация генерации
                                ImageGeneratingAnimation(
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                            }
                        }
                    }
                }
                
                // Loading indicator - only show when NOT generating image
                if ((isLoading || isUploading) && !isGeneratingImage) {
                    item { 
                        TypingIndicatorV2(
                            modifier = Modifier.padding(vertical = GlassSpacing.betweenBubbleGroups),
                            isUploading = isUploading
                        )
                    }
                }

                // Messages (reversed order for reverseLayout) with proper grouping
                itemsIndexed(reversedMessages, key = { _, message -> message.id }) { index, message ->
                    val prev = reversedMessages.getOrNull(index - 1) // визуально выше (т.к. reversed)
                    val next = reversedMessages.getOrNull(index + 1) // визуально ниже
                    
                    // Группировка: первый в группе = нет предыдущего с тем же role
                    // Последний в группе = нет следующего с тем же role
                    val isFirstInGroup = prev?.role != message.role
                    val isLastInGroup = next?.role != message.role
                    
                    // Spacing по спеке: 2dp внутри группы, 12dp между группами
                    val topPadding = when {
                        index == 0 -> 0.dp  // Первое сообщение
                        isFirstInGroup -> GlassSpacing.betweenBubbleGroups  // Начало новой группы
                        else -> GlassSpacing.betweenBubblesInGroup  // Внутри группы
                    }
                    
                    // Animate streaming messages only
                    val shouldAnimate = message.agent_name == "streaming"
                    
                    ChatBubbleV2(
                        message = message,
                        status = messageSendStatus[message.id],
                        isFirstInGroup = isFirstInGroup,
                        isLastInGroup = isLastInGroup,
                        modifier = Modifier.padding(top = topPadding),
                        animate = shouldAnimate,
                        authToken = authToken,
                        onRetry = {
                            viewModel.retrySendMessage(message.id, message.content)
                        },
                        onDelete = {
                            viewModel.deleteMessage(message.id)
                        }
                    )
                }

                // Empty state or skeleton at "bottom" (top visually)
                if (reversedMessages.isEmpty() && isSyncing) {
                    items(4) {
                        ChatSkeletonBubble()
                    }
                } else if (reversedMessages.isEmpty()) {
                    item { ChatEmptyState() }
                }
            }

            // Error banner above input
            if (uiState is ChatUiState.Error) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.errorContainer
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = (uiState as? ChatUiState.Error)?.message ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { viewModel.clearError() }, Modifier.size(24.dp)) {
                            Icon(Icons.Default.Close, "Закрыть", Modifier.size(16.dp))
                        }
                    }
                }
            }

            // Attached image preview for Image-to-Image
            AnimatedVisibility(
                visible = attachedImageUri != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                attachedImageUri?.let { uri ->
                    Box(
                            modifier = Modifier
                                .fillMaxWidth()
                            .padding(horizontal = GlassSpacing.screenEdge, vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(GlassColors.surface.copy(alpha = 0.8f))
                                .border(1.dp, GlassColors.whiteOverlay10, RoundedCornerShape(12.dp))
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Thumbnail
                            coil.compose.AsyncImage(
                                model = uri,
                                contentDescription = "Прикреплённое фото",
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                            )
                            
                            Spacer(Modifier.width(12.dp))
                            
                            // Text
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Фото для редактирования",
                                    style = GlassTypography.labelSmall,
                                    color = GlassColors.textPrimary
                                )
                                Text(
                                    text = "Опишите что изменить",
                                    style = GlassTypography.timestamp,
                                    color = GlassColors.textTertiary
                                )
                            }
                            
                            // Remove button
                            IconButton(
                                onClick = { viewModel.removeAttachedImage() },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Удалить",
                                    tint = GlassColors.textTertiary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                            }
                        }
                    }

            // INPUT AREA - чистый, без кнопки чатов (перенесена в header)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                    .padding(horizontal = GlassSpacing.screenEdge, vertical = 8.dp),
                verticalAlignment = Alignment.Bottom  // Bottom align для multi-line
            ) {
                // Main input container — по спеке с max 4 lines
                    Row(
                        modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)  // Минимальная высота
                        .shadow(
                            elevation = GlassElevation.inputField,
                            shape = GlassShapes.inputField,
                            spotColor = Color.Black.copy(alpha = 0.2f)
                        )
                        .clip(GlassShapes.inputField)
                        .background(GlassColors.surface.copy(alpha = 0.9f), GlassShapes.inputField)
                        .border(1.dp, GlassColors.whiteOverlay10, GlassShapes.inputField)
                        .padding(start = 4.dp, end = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                    // Attach button inside
                    Box(
                        modifier = Modifier
                            .size(GlassSpacing.buttonSize)
                            .clip(CircleShape)
                            .clickable { showAttachMenu = !showAttachMenu },
                        contentAlignment = Alignment.Center
                    ) {
                            Icon(
                            imageVector = if (showAttachMenu) Icons.Default.Close else Icons.Default.AttachFile,
                            contentDescription = "Прикрепить",
                            tint = GlassColors.textTertiary,
                            modifier = Modifier.size(20.dp)
                            )
                        }

                    // Recording indicator inline
                    if (isRecording) {
                        val transition = rememberInfiniteTransition(label = "rec")
                        val recAlpha by transition.animateFloat(
                            0.5f, 1f,
                            infiniteRepeatable(tween(500), RepeatMode.Reverse),
                            label = "rec_alpha"
                        )
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(GlassColors.error.copy(alpha = recAlpha), CircleShape)
                        )
                        Spacer(Modifier.width(8.dp))
                        }

                    // TextField — max 4 lines по спеке
                    BasicTextField(
                            value = currentMessage,
                            onValueChange = viewModel::updateCurrentMessage,
                            modifier = Modifier
                                .weight(1f)
                            .heightIn(min = 36.dp, max = 120.dp)  // ~4 lines
                                .focusRequester(focusRequester)
                                .onFocusChanged { focusState ->
                                    if (focusState.isFocused && reversedMessages.isNotEmpty()) {
                                        coroutineScope.launch {
                                        delay(200)
                                            listState.animateScrollToItem(0)
                                        }
                                    }
                                },
                        textStyle = GlassTypography.messageText,
                        cursorBrush = SolidColor(GlassColors.accent),
                        maxLines = 4,  // MAX 4 LINES по спеке!
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Sentences,
                                imeAction = ImeAction.Send
                            ),
                            keyboardActions = KeyboardActions(
                                onSend = {
                                    if (currentMessage.isNotBlank() && !isLoading) {
                                        viewModel.sendMessage(currentMessage)
                                    onMessageSent()
                                        coroutineScope.launch {
                                            delay(100)
                                            listState.animateScrollToItem(0)
                                        }
                                    }
                                }
                            ),
                        decorationBox = { innerTextField ->
                            Box(
                                modifier = Modifier.padding(vertical = 10.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                if (currentMessage.isEmpty()) {
                                    Text(
                                        text = if (isRecording) "Говорите..." else "Сообщение",
                                        style = GlassTypography.placeholder
                                    )
                                }
                                innerTextField()
                            }
                        }
                    )
                }

                Spacer(Modifier.width(GlassSpacing.buttonSpacing))

                // Mic/Send button — по спеке
                val micPulseTransition = rememberInfiniteTransition(label = "mic_pulse")
                val micPulse by micPulseTransition.animateFloat(
                    initialValue = 1f,
                    targetValue = 1.15f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(600, easing = EaseInOutCubic),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "mic_pulse"
                )

                Box(
                    modifier = Modifier.size(GlassSpacing.buttonSize + 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Pulse effect for recording
                                if (isRecording) {
                                    Box(
                                        modifier = Modifier
                                .size(GlassSpacing.buttonSize + 8.dp)
                                            .scale(micPulse)
                                            .background(
                                    GlassColors.error.copy(alpha = 0.3f),
                                                CircleShape
                                            )
                                    )
                                }
                    
                    // Main button с градиентом
                    Box(
                        modifier = Modifier
                            .size(GlassSpacing.buttonSize + 8.dp)
                            .shadow(
                                elevation = 4.dp,
                                shape = CircleShape,
                                spotColor = if (isRecording) GlassColors.error else GlassColors.accent
                            )
                            .clip(CircleShape)
                            .background(
                                when {
                                    isRecording -> Brush.linearGradient(
                                        colors = listOf(GlassColors.error, GlassColors.coral)
                                    )
                                    currentMessage.isNotBlank() -> GlassGradients.accent
                                    else -> Brush.linearGradient(
                                        colors = listOf(GlassColors.accent, GlassColors.accentSecondary)
                                    )
                                },
                                CircleShape
                            )
                            .clickable(enabled = !isLoading || currentMessage.isBlank()) {
                                if (currentMessage.isNotBlank()) {
                                    viewModel.sendMessage(currentMessage)
                                    onMessageSent()
                                    coroutineScope.launch {
                                        delay(100)
                                        listState.animateScrollToItem(0)
                                    }
                                } else {
                                        view.isHapticFeedbackEnabled = true
                                        view.performHapticFeedback(HapticFeedbackConstantsCompat.KEYBOARD_PRESS)
                                        if (hasAudioPermission) {
                                            if (pendingVoiceAfterPermission) {
                                                pendingVoiceAfterPermission = false
                                            }
                                            val prefs = context.getSharedPreferences("voice_prefs", android.content.Context.MODE_PRIVATE)
                                            val autoSend = prefs.getBoolean("auto_send_voice", true)
                                            VoiceEventLogger.log(context, "mic_click record_toggle")
                                            viewModel.toggleVoiceInput(autoSend)
                                        } else {
                                            VoiceEventLogger.log(context, "mic_click request_permission")
                                            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                            view.performHapticFeedback(HapticFeedbackConstantsCompat.LONG_PRESS)
                                        }
                                }
                                    },
                        contentAlignment = Alignment.Center
                    ) {
                        when {
                            isLoading && currentMessage.isNotBlank() -> {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = GlassColors.textPrimary,
                                    strokeWidth = 2.dp
                                )
                            }
                            isRecording -> {
                                    Icon(
                                    Icons.Default.Stop,
                                    "Стоп",
                                    tint = GlassColors.textPrimary,
                                    modifier = Modifier.size(22.dp)
                                    )
                                }
                            currentMessage.isNotBlank() -> {
                                    Icon(
                                        Icons.AutoMirrored.Filled.Send,
                                        "Отправить",
                                    tint = GlassColors.textPrimary,
                                    modifier = Modifier.size(20.dp)
                                    )
                                }
                            else -> {
                                Icon(
                                    Icons.Default.Mic,
                                    "Голос",
                                    tint = GlassColors.textPrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }
            }
            
            // Attach menu — горизонтальная панель над вводом
            AnimatedVisibility(
                visible = showAttachMenu,
                enter = fadeIn(animationSpec = tween(150)) + 
                        expandVertically(animationSpec = tween(150)),
                exit = fadeOut(animationSpec = tween(100)) + 
                       shrinkVertically(animationSpec = tween(100))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = GlassSpacing.screenEdge)
                        .padding(bottom = 8.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(GlassColors.surfaceAlt.copy(alpha = 0.95f))
                        .border(1.dp, GlassColors.whiteOverlay10, RoundedCornerShape(16.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Image-to-Image (Edit)
                    AttachButton(
                        icon = Icons.Default.AutoFixHigh, 
                        label = "Edit",
                        color = Color(0xFF9C27B0)
                    ) { 
                        attachImageLauncher.launch("image/*")
                        showAttachMenu = false
                    }
                    // Camera
                    AttachButton(
                        icon = Icons.Default.CameraAlt, 
                        label = "Камера",
                        color = Color(0xFF00897B)
                    ) {
                        if (hasCameraPermission) {
                            photoUri = createPhotoUri()
                            takePictureLauncher.launch(photoUri!!)
                        } else {
                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                        showAttachMenu = false
                    }
                    // Gallery
                    AttachButton(
                        icon = Icons.Default.Image, 
                        label = "Фото",
                        color = Color(0xFF3949AB)
                    ) { 
                        pickImageLauncher.launch("image/*")
                        showAttachMenu = false
                    }
                    // File
                    AttachButton(
                        icon = Icons.Default.Description, 
                        label = "Файл",
                        color = Color(0xFFE64A19)
                    ) {
                        pickFileLauncher.launch(arrayOf("application/pdf", "application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "image/*"))
                        showAttachMenu = false
                    }
                }
            }
        }

        // Scroll to bottom FAB
        val showScrollFab by remember {
            derivedStateOf {
                listState.firstVisibleItemIndex > 2
            }
        }

        if (showScrollFab) {
            SmallFloatingActionButton(
                onClick = {
                    coroutineScope.launch {
                        listState.animateScrollToItem(0)
                    }
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 100.dp)
            ) {
                Icon(Icons.Default.KeyboardArrowDown, "Вниз")
            }
        }
    }
    
    // Session Attachments Sheet
    if (showAttachmentsSheet) {
        SessionAttachmentsSheet(
            attachments = sessionAttachments,
            isLoading = attachmentsLoading,
            authToken = authToken,
            onUpload = { uri ->
                viewModel.uploadSessionAttachment(uri)
            },
            onDelete = { attachmentId ->
                viewModel.deleteSessionAttachment(attachmentId)
            },
            onDismiss = { showAttachmentsSheet = false }
        )
    }
}

/**
 * Ultra-compact attach icon - 36dp
 */
/**
 * Кнопка для панели прикрепления - с иконкой и подписью
 */
@Composable
private fun AttachButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = color,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            color = GlassColors.textSecondary,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun AttachOptionV2(
    icon: androidx.compose.ui.graphics.vector.ImageVector, 
    label: String, 
    color: Color, 
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(GlassShapes.medium)
                .background(color.copy(alpha = 0.15f))
                .border(1.dp, color.copy(alpha = 0.3f), GlassShapes.medium)
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon, 
                contentDescription = label, 
                tint = color, 
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = label, 
            style = GlassTypography.labelSmall
        )
    }
}

@Composable
private fun ChatEmptyState() {
    val infiniteTransition = rememberInfiniteTransition(label = "blueberry")
    
    // Плавное дыхание основного круга
    val breathe by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathe"
    )
    
    // Вращение внешнего кольца
    val outerRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(30000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "outer_rotation"
    )
    
    // Обратное вращение среднего кольца
    val middleRotation by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "middle_rotation"
    )
    
    // Пульсация свечения
    val glowPulse by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )
    
    // Плавающие ягоды вокруг
    val float1 by infiniteTransition.animateFloat(
        initialValue = -8f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "float1"
    )
    
    val float2 by infiniteTransition.animateFloat(
        initialValue = 6f,
        targetValue = -6f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "float2"
    )
    
    val float3 by infiniteTransition.animateFloat(
        initialValue = -5f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(
            animation = tween(3500, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "float3"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp),
        contentAlignment = Alignment.Center
    ) {
        // Внешнее вращающееся кольцо с точками
        Box(
            modifier = Modifier
                .size(180.dp)
                .graphicsLayer { rotationZ = outerRotation }
        ) {
            // Орбитальные точки
            listOf(0f, 60f, 120f, 180f, 240f, 300f).forEachIndexed { index, angle ->
                val dotPulse by infiniteTransition.animateFloat(
                    initialValue = 0.4f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1200 + index * 150, easing = EaseInOutCubic),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "dot_$index"
                )
            Box(
                modifier = Modifier
                        .size(180.dp)
                        .graphicsLayer { rotationZ = angle }
                ) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .offset(y = 4.dp)
                            .size((3 + index % 3).dp)
                            .background(
                                GlassTheme.accentPrimary.copy(alpha = dotPulse * 0.8f),
                                CircleShape
                            )
                    )
                }
            }
        }
        
        // Среднее вращающееся кольцо
        Box(
            modifier = Modifier
                .size(130.dp)
                .graphicsLayer { rotationZ = middleRotation }
                .border(
                    1.dp,
                    Brush.sweepGradient(
                        colors = listOf(
                            Color(0xFF6366F1).copy(alpha = 0.4f),
                            Color(0xFF8B5CF6).copy(alpha = 0.1f),
                            Color(0xFFA855F7).copy(alpha = 0.4f),
                            Color(0xFF6366F1).copy(alpha = 0.1f)
                        )
                    ),
                    CircleShape
                )
        )
        
        // Свечение фона
        Box(
            modifier = Modifier
                .size(110.dp)
                .scale(breathe)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF6366F1).copy(alpha = glowPulse),
                            Color(0xFF8B5CF6).copy(alpha = glowPulse * 0.5f),
                            Color.Transparent
                        )
                    ),
                    CircleShape
                )
        )
        
        // Основной круг с градиентом
        Box(
            modifier = Modifier
                .size(80.dp)
                .scale(breathe)
                    .clip(CircleShape)
                    .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF6366F1),  // Indigo
                            Color(0xFF8B5CF6),  // Purple  
                            Color(0xFF4F46E5)   // Deep indigo
                        )
                    )
                )
                .border(
                    2.dp,
                    Color.White.copy(alpha = 0.2f),
                    CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
            // Черника emoji
            Text(
                text = "🫐",
                fontSize = 36.sp
            )
        }
        
        // Плавающие маленькие черники
        Box(
            modifier = Modifier
                .offset(x = (-55).dp, y = float1.dp)
        ) {
            Text(text = "🫐", fontSize = 18.sp, modifier = Modifier.graphicsLayer { alpha = 0.7f })
        }
        
        Box(
            modifier = Modifier
                .offset(x = 60.dp, y = float2.dp)
        ) {
            Text(text = "🫐", fontSize = 14.sp, modifier = Modifier.graphicsLayer { alpha = 0.6f })
        }
        
        Box(
            modifier = Modifier
                .offset(x = 30.dp, y = (50 + float3).dp)
        ) {
            Text(text = "🫐", fontSize = 12.sp, modifier = Modifier.graphicsLayer { alpha = 0.5f })
        }
        
        Box(
            modifier = Modifier
                .offset(x = (-40).dp, y = (-45 + float2).dp)
        ) {
            Text(text = "🫐", fontSize = 16.sp, modifier = Modifier.graphicsLayer { alpha = 0.65f })
        }
        
        // Искрящиеся точки
        listOf(
            -70f to -20f, 75f to 15f, -30f to 60f, 50f to -55f, -60f to 40f, 65f to 50f
        ).forEachIndexed { index, (x, y) ->
            val sparkle by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(800 + index * 200, easing = EaseInOutCubic),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "sparkle_$index"
            )
            
            Box(
                modifier = Modifier
                    .offset(x = x.dp, y = y.dp)
                    .size(3.dp)
                    .graphicsLayer { alpha = sparkle }
                    .background(
                        Color.White.copy(alpha = 0.8f),
                        CircleShape
                    )
            )
        }
    }
}

@Composable
private fun ChatBubble(
    message: MessageDTO,
    status: MessageSendStatus?,
    showAvatar: Boolean,
    modifier: Modifier = Modifier,
    animate: Boolean = false,
    onRetry: () -> Unit = {}
) {
    val isUser = message.role == "user"
    val formattedText = remember(message.content) { formatMessageText(message.content) }

    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start) {
        if (!isUser) {
            if (showAvatar) {
                // Animated blueberry avatar
                BlueberryAvatar()
                Spacer(Modifier.width(8.dp))
                ProviderDot(
                    provider = message.provider,
                    providerColor = message.provider_color,
                    modelUsed = message.model_used
                )
                Spacer(Modifier.width(6.dp))
            } else {
                Spacer(Modifier.width(50.dp)) // Space for missing avatar
            }
        }

        Column(horizontalAlignment = if (isUser) Alignment.End else Alignment.Start, modifier = Modifier.widthIn(max = 340.dp)) {
            // Show agent name label (but not for streaming or standard chat)
            if (!isUser && message.agent_name != null && message.agent_name != "chat" && message.agent_name != "offline" && message.agent_name != "streaming") {
                Text(message.agent_name, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium, modifier = Modifier.padding(bottom = 4.dp))
            }
            
            // User attached images (Image-to-Image) - миниатюры
            if (isUser && !message.images.isNullOrEmpty()) {
                message.images.forEach { imageUriString ->
                    // Парсим URI из строки
                    val parsedUri = try { 
                        Uri.parse(imageUriString) 
                    } catch (e: Exception) { 
                        null 
                    }
                    
                    if (parsedUri != null) {
                        Box(
                            modifier = Modifier
                                .padding(bottom = 8.dp)
                                .size(100.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(GlassColors.surface.copy(alpha = 0.3f))
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(parsedUri)
                                    .crossfade(true)
                                    .memoryCacheKey(imageUriString)
                                    .diskCacheKey(imageUriString)
                                    .build(),
                                contentDescription = "Прикреплённое фото",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                                onError = { 
                                    android.util.Log.e("IMAGE_LOAD", "Failed to load: $imageUriString")
                                }
                            )
                            // Edit icon badge
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(4.dp)
                                    .size(20.dp)
                                    .background(GlassTheme.accentPrimary.copy(alpha = 0.8f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Glassmorphism message bubble
            Box(
                modifier = Modifier
                    .clip(
                        RoundedCornerShape(
                            topStart = 20.dp, 
                            topEnd = 20.dp, 
                            bottomStart = if (isUser) 20.dp else 4.dp, 
                            bottomEnd = if (isUser) 4.dp else 20.dp
                        )
                    )
                    .background(
                        if (isUser) 
                            Brush.linearGradient(
                                colors = listOf(
                                    GlassTheme.accentPrimary,
                                    GlassTheme.accentPrimary.copy(alpha = 0.8f)
                                )
                            )
                        else 
                            Brush.linearGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.1f),
                                    Color.White.copy(alpha = 0.05f)
                                )
                            )
                    )
            ) {
                if (isUser) {
                Text(
                    formattedText,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    style = MaterialTheme.typography.bodySmall.copy(lineHeight = 20.sp),
                        color = Color.White
                    )
                } else {
                    TypewriterText(
                        fullText = formattedText,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = GlassTheme.textPrimary,
                        animationEnabled = animate
                )
                }
            }

            if (isUser && status == MessageSendStatus.Failed) {
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Не отправлено",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(Modifier.width(8.dp))
                    TextButton(onClick = onRetry, contentPadding = PaddingValues(0.dp)) {
                        Text("Повторить", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        if (isUser) {
            if (showAvatar) {
                Spacer(Modifier.width(8.dp))
                Box(modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(GlassTheme.accentPrimary), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Person, null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
            } else {
                Spacer(Modifier.width(44.dp)) // Space for missing avatar
            }
        }
    }
}

/**
 * Typewriter animation for chat messages
 */
@Composable
private fun TypewriterText(
    fullText: String,
    modifier: Modifier = Modifier,
    style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.bodySmall,
    color: Color = GlassTheme.textPrimary,
    animationEnabled: Boolean = true
) {
    val composeTime = remember { System.currentTimeMillis() }
    android.util.Log.d("STREAM_DIAG", "🎨 COMPOSE @${System.currentTimeMillis()} fullText.len=${fullText.length}, animate=$animationEnabled, created@$composeTime")
    
    // Track visible chars - starts at 0, never resets
    var visibleChars by remember { mutableStateOf(0) }
    
    // Single animation loop that keeps running
    LaunchedEffect(Unit) {
        android.util.Log.d("STREAM_DIAG", "🚀 ANIM START @${System.currentTimeMillis()}")
        while (true) {
            delay(25)
            visibleChars++
        }
    }
    
    val displayText = if (animationEnabled && visibleChars < fullText.length) {
        fullText.take(visibleChars)
    } else {
        fullText
    }
    
    // Log only when something interesting happens
    if (visibleChars <= 5 || visibleChars == fullText.length) {
        android.util.Log.d("STREAM_DIAG", "📝 RENDER visible=$visibleChars, fullLen=${fullText.length}, showing=${displayText.length}")
    }
    
    Text(
        text = displayText,
        modifier = modifier,
        style = style.copy(lineHeight = 20.sp),
        color = color
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProviderDot(
    provider: String?,
    providerColor: String?,
    modelUsed: String?
) {
    val color = remember(provider, providerColor) { resolveProviderColor(provider, providerColor) }
    val tooltipText = modelUsed ?: provider ?: "AI"
    TooltipBox(
        state = rememberTooltipState(),
        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        tooltip = { PlainTooltip { Text(tooltipText) } }
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
    }
}

private fun resolveProviderColor(provider: String?, providerColor: String?): Color {
    providerColor?.let { hex ->
        val parsed = parseColor(hex)
        if (parsed != null) return parsed
    }
    return when (provider?.lowercase()) {
        "openai" -> Color(0xFF10A37F)
        "perplexity" -> Color(0xFF1FB8CD)
        "gemini" -> Color(0xFF4285F4)
        else -> Color(0xFF9E9E9E)
    }
}

private fun parseColor(hex: String): Color? {
    return try {
        val normalized = if (hex.startsWith("#")) hex else "#$hex"
        Color(AndroidColor.parseColor(normalized))
    } catch (_: IllegalArgumentException) {
        null
    }
}

private enum class VoiceHaptic {
    Start,
    Stop,
    Error
}

private fun performVoiceHaptic(
    context: Context,
    haptics: HapticFeedback,
    view: android.view.View,
    type: VoiceHaptic
) {
    view.isHapticFeedbackEnabled = true
    val fallback = when (type) {
        VoiceHaptic.Start -> HapticFeedbackConstantsCompat.LONG_PRESS
        VoiceHaptic.Stop -> HapticFeedbackConstantsCompat.KEYBOARD_RELEASE
        VoiceHaptic.Error -> HapticFeedbackConstantsCompat.REJECT
    }
    if (view.performHapticFeedback(fallback)) return

    val hasVibratePermission = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.VIBRATE
    ) == PackageManager.PERMISSION_GRANTED
    if (!hasVibratePermission) {
        val composeFallback = when (type) {
            VoiceHaptic.Start -> HapticFeedbackType.LongPress
            VoiceHaptic.Stop -> HapticFeedbackType.TextHandleMove
            VoiceHaptic.Error -> HapticFeedbackType.LongPress
        }
        haptics.performHapticFeedback(composeFallback)
        return
    }

    try {
        val vibrator = getVibrator(context)
        if (vibrator?.hasVibrator() == true) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val effect = when (type) {
                    VoiceHaptic.Start -> VibrationEffect.createOneShot(120, VibrationEffect.DEFAULT_AMPLITUDE)
                    VoiceHaptic.Stop -> VibrationEffect.createOneShot(60, VibrationEffect.DEFAULT_AMPLITUDE)
                    VoiceHaptic.Error -> VibrationEffect.createWaveform(longArrayOf(0, 25, 40, 25), -1)
                }
                vibrator.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                when (type) {
                    VoiceHaptic.Start -> vibrator.vibrate(120)
                    VoiceHaptic.Stop -> vibrator.vibrate(60)
                    VoiceHaptic.Error -> vibrator.vibrate(longArrayOf(0, 25, 40, 25), -1)
                }
            }
        } else {
            val composeFallback = when (type) {
                VoiceHaptic.Start -> HapticFeedbackType.LongPress
                VoiceHaptic.Stop -> HapticFeedbackType.TextHandleMove
                VoiceHaptic.Error -> HapticFeedbackType.LongPress
            }
            haptics.performHapticFeedback(composeFallback)
        }
    } catch (_: SecurityException) {
        val composeFallback = when (type) {
            VoiceHaptic.Start -> HapticFeedbackType.LongPress
            VoiceHaptic.Stop -> HapticFeedbackType.TextHandleMove
            VoiceHaptic.Error -> HapticFeedbackType.LongPress
        }
        haptics.performHapticFeedback(composeFallback)
    }
}

private fun getVibrator(context: Context): Vibrator? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val manager = context.getSystemService(VibratorManager::class.java)
        manager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Vibrator::class.java)
    }
}

@Composable
private fun BlueberryAvatar(size: Dp = 36.dp) {
    val infiniteTransition = rememberInfiniteTransition(label = "blueberry_avatar")
    
    // Subtle breathing animation
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "avatar_scale"
    )
    
    // Gentle rotation wobble
    val rotation by infiniteTransition.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "avatar_rotation"
    )
    
        Box(
            modifier = Modifier
            .size(size)
            .scale(scale)
            .graphicsLayer { rotationZ = rotation }
                .clip(CircleShape)
                .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF6366F1),
                        Color(0xFF8B5CF6)
                    )
                )
                ),
            contentAlignment = Alignment.Center
        ) {
        Text(
            text = "🫐",
            fontSize = (size.value * 0.5f).sp
        )
    }
}

@Composable
private fun TypingIndicator(isUploading: Boolean, streamStatus: String = "") {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar
            if (isUploading) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(listOf(GlassTheme.accentPrimary, GlassTheme.accentSecondary))
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.CloudUpload, null, tint = Color.White, modifier = Modifier.size(12.dp))
            }
            } else {
            BlueberryAvatar(size = 24.dp)
        }

        Spacer(Modifier.width(6.dp))

        // Compact glass bubble with dots
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp, 12.dp, 12.dp, 4.dp))
                .background(Color.White.copy(alpha = 0.1f))
                .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(12.dp, 12.dp, 12.dp, 4.dp))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp), 
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isUploading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(10.dp), 
                        strokeWidth = 1.5.dp, 
                        color = GlassTheme.accentPrimary
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "Загрузка", 
                        style = MaterialTheme.typography.labelSmall, 
                        color = GlassTheme.textSecondary
                    )
                } else {
                    // Animated dots only
                    val transition = rememberInfiniteTransition(label = "dots")
                    repeat(3) { index ->
                        val alpha by transition.animateFloat(
                            0.3f, 1f, 
                            infiniteRepeatable(
                                tween(400, delayMillis = index * 120), 
                                RepeatMode.Reverse
                            ), 
                            label = "dot_alpha_$index"
                        )
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                            .clip(CircleShape)
                                .background(GlassTheme.accentPrimary.copy(alpha = alpha))
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatSkeletonBubble() {
    val transition = rememberInfiniteTransition(label = "skeleton")
    val alpha by transition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
        label = "skeleton_alpha"
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha))
        )
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(16.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha))
            )
            Spacer(Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha))
            )
        }
    }
}

private fun formatMessageText(raw: String): String {
    var text = raw.trim()
    if (text.isEmpty()) return text

    // Normalize special list markers and collapse excessive symbols
    val bulletRegex = Pattern.compile("(?m)^\\s*[-*•]\\s+")
    text = bulletRegex.matcher(text).replaceAll("• ")

    // Remove markdown headings like ## Title
    val headingRegex = Pattern.compile("(?m)^\\s*#{1,6}\\s+")
    text = headingRegex.matcher(text).replaceAll("")

    // Ensure numbered lists have a space after the dot
    val numberedRegex = Pattern.compile("(?m)^(\\d+)\\.(\\S)")
    text = numberedRegex.matcher(text).replaceAll("$1. $2")

    // Replace multiple blank lines with a single blank line
    val multiBlank = Pattern.compile("(\\n\\s*){3,}")
    text = multiBlank.matcher(text).replaceAll("\n\n")

    // Clean excessive asterisks/underscores used for markdown emphasis
    text = text.replace("*", "").replace("_", "")

    return text.trim()
}
