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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.animation.core.Animatable
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt

@Composable
fun ChatScreen(
    viewModel: ChatViewModel = hiltViewModel(),
    bottomBarPadding: PaddingValues = PaddingValues(0.dp),
    onMessageSent: () -> Unit = {},
    onNavigate: (String) -> Unit = {} // Для навигации без bottom bar
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
    // chatSheetState больше не нужен — используем Dialog с fadeIn

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
    // Filter out empty messages (no content, no images, no imageUrl)
    val reversedMessages = remember(messages) { 
        messages.filter { msg ->
            msg.content.isNotBlank() || 
            !msg.images.isNullOrEmpty() || 
            msg.imageUrl != null
        }.reversed() 
    }

    // Auto-scroll to bottom (index 0 in reverseLayout) when new message arrives
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            delay(100)
            listState.animateScrollToItem(0)
        }
    }

    // Chats Dialog - Плавное появление fadeIn (без выезжания)
    if (showChatsSheet) {
        Dialog(
            onDismissRequest = { showChatsSheet = false },
            properties = DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = true,
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false  // НЕ реагировать на клавиатуру!
            )
        ) {
            // Анимированное появление — мягкий fadeIn + легкий scaleIn
            var visible by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) { visible = true }
            
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { showChatsSheet = false },
                contentAlignment = Alignment.TopStart  // Сверху слева, рядом с иконкой
            ) {
                AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn(tween(200)) + scaleIn(
                        initialScale = 0.95f,
                        animationSpec = tween(200, easing = FastOutSlowInEasing)
                    ),
                    exit = fadeOut(tween(150)) + scaleOut(
                        targetScale = 0.95f,
                        animationSpec = tween(150)
                    )
                ) {
                    // Компактное окно диалогов — без затемнения, плавающее
                    Box(
                        modifier = Modifier
                            .padding(start = 10.dp, top = 55.dp)  // Под иконкой сессий
                            .width(280.dp)
                            .heightIn(max = 400.dp)
                            .shadow(
                                elevation = 16.dp,
                                shape = RoundedCornerShape(16.dp),
                                ambientColor = Color.Black.copy(alpha = 0.3f),
                                spotColor = Color.Black.copy(alpha = 0.3f)
                            )
                            .clip(RoundedCornerShape(16.dp))
                            .background(GlassColors.surface)
                            .border(
                                width = 1.dp,
                                color = GlassColors.accentSecondary.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(16.dp)
                            )
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            ) { /* блокируем клик наружу */ }
                    ) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            // Header — компактный
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.Chat,
                                    contentDescription = null,
                                    tint = GlassColors.accentSecondary,
                                    modifier = Modifier.size(22.dp)
                                )
                                
                                // Кнопка "+"
                                Box(
                                    modifier = Modifier
                                        .size(29.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            Brush.linearGradient(
                                                colors = listOf(GlassColors.mint, GlassColors.mint.copy(alpha = 0.8f))
                                            )
                                        )
                                        .clickable {
                                            showChatsSheet = false
                                            viewModel.createNewConversation()
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Add,
                                        contentDescription = "Новый",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            // Автозакрытие при пустом списке
                            LaunchedEffect(conversations.isEmpty()) {
                                if (conversations.isEmpty()) {
                                    showChatsSheet = false
                                }
                            }
                            
                            if (conversations.isNotEmpty()) {
                                var currentRevealedSessionId by remember { mutableStateOf<String?>(null) }
                                
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 12.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                    contentPadding = PaddingValues(bottom = 16.dp)
                                ) {
                                    items(conversations.size, key = { conversations[it].id }) { index ->
                                        val convo = conversations[index]
                                        Box(modifier = Modifier.animateItemPlacement(
                                            animationSpec = spring(
                                                dampingRatio = 0.7f,
                                                stiffness = 400f
                                            )
                                        )) {
                                            SwipeableSessionItem(
                                                itemId = convo.id,
                                                currentRevealedId = currentRevealedSessionId,
                                                onReveal = { currentRevealedSessionId = it },
                                                onDelete = { viewModel.deleteConversation(convo.id) }
                                            ) {
                                                SimpleSessionCard(
                                                    convo = convo,
                                                    isSelected = convo.id == currentConversationId,
                                                    onClick = {
                                                        if (currentRevealedSessionId != null) {
                                                            currentRevealedSessionId = null
                                                        } else {
                                                            android.util.Log.d("SESSION_CLICK", "🔄 Selecting: ${convo.id}")
                                                            showChatsSheet = false
                                                            viewModel.selectConversation(convo.id)
                                                        }
                                                    }
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
            // Компактный header — только анимированная иконка сессий с бейджиком
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Панель кнопок — показываем только если есть сессии
                AnimatedVisibility(
                    visible = conversations.isNotEmpty(),
                    enter = fadeIn() + scaleIn(initialScale = 0.8f),
                    exit = fadeOut() + scaleOut(targetScale = 0.8f)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Session selector
                        SessionsIconWithBadge(
                            sessionCount = conversations.size,
                            onClick = { showChatsSheet = true }
                        )
                        
                        // New chat button
                        Box(
                            modifier = Modifier
                                .size(29.dp)
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
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
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
                
                // Ключевые слова для генерации изображений — расширенный список!
                val isTextToImageRequest = lastUserMsg.contains("сгенерируй", ignoreCase = true) ||
                                           lastUserMsg.contains("генерируй", ignoreCase = true) ||
                                           lastUserMsg.contains("сгенерир", ignoreCase = true) ||
                                           lastUserMsg.contains("генерир", ignoreCase = true) ||
                                           lastUserMsg.contains("нарисуй", ignoreCase = true) ||
                                           lastUserMsg.contains("рисуй", ignoreCase = true) ||
                                           lastUserMsg.contains("изобрази", ignoreCase = true) ||
                                           lastUserMsg.contains("покажи картин", ignoreCase = true) ||
                                           lastUserMsg.contains("покажи изображ", ignoreCase = true) ||
                                           lastUserMsg.contains("создай картин", ignoreCase = true) ||
                                           lastUserMsg.contains("создай изображ", ignoreCase = true) ||
                                           lastUserMsg.contains("создай фото", ignoreCase = true) ||
                                           lastUserMsg.contains("сделай картин", ignoreCase = true) ||
                                           lastUserMsg.contains("сделай изображ", ignoreCase = true) ||
                                           lastUserMsg.contains("визуализируй", ignoreCase = true) ||
                                           lastUserMsg.contains("generate", ignoreCase = true) ||
                                           lastUserMsg.contains("draw", ignoreCase = true) ||
                                           lastUserMsg.contains("image", ignoreCase = true) ||
                                           lastUserMsg.contains("picture", ignoreCase = true)
                
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
                                                GlassColors.accent,
                                                GlassColors.accentSecondary
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
                
                // SSE Status Indicator - показывает текущий статус обработки
                // Показываем только когда НЕ генерируется картинка (у картинок своя анимация)
                if ((isLoading || isUploading || streamStatus.isNotEmpty()) && !isGeneratingImage) {
                    item(key = "sse_status_indicator") { 
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = GlassSpacing.betweenBubbleGroups),
                            horizontalArrangement = Arrangement.Start
                        ) {
                            // Используем SSE статус или дефолтный
                            val displayStatus = when {
                                isUploading -> "uploading"
                                streamStatus.isNotEmpty() -> streamStatus
                                isLoading -> "thinking"
                                else -> "thinking"
                            }
                            SSEStatusIndicator(status = displayStatus)
                        }
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
                containerColor = GlassColors.accent,
                contentColor = GlassColors.textPrimary,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = GlassSpacing.screenEdge, bottom = 100.dp)
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

// AttachOptionV2 удалён — не используется

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
                                GlassColors.accent.copy(alpha = dotPulse * 0.8f),
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
                            GlassColors.accent.copy(alpha = 0.4f),
                            GlassColors.accentSecondary.copy(alpha = 0.1f),
                            Color(0xFFA855F7).copy(alpha = 0.4f),
                            GlassColors.accent.copy(alpha = 0.1f)
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
                            GlassColors.accent.copy(alpha = glowPulse),
                            GlassColors.accentSecondary.copy(alpha = glowPulse * 0.5f),
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
                            GlassColors.accent,  // Indigo
                            GlassColors.accentSecondary,  // Purple  
                            GlassColors.accent.copy(alpha = 0.9f)   // Deep indigo
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

// ChatBubble и TypewriterText удалены — используется ChatBubbleV2

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

/**
 * Компактная кнопка сессий — 29dp как кнопка плюса
 * Мягкая анимация, без квадратного фона
 */
@Composable
private fun SessionsIconWithBadge(
    sessionCount: Int,
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "sessions")
    
    // Мягкая медленная пульсация
    val pulse by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )
    
    Box(contentAlignment = Alignment.Center) {
        // Основной круг — 29dp как кнопка плюса
        Box(
            modifier = Modifier
                .size(29.dp)
                .scale(pulse)
                .clip(CircleShape)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(GlassColors.accent, GlassColors.accentSecondary)
                    )
                )
                .clickable(
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                    indication = null // Без ripple эффекта!
                ) { onClick() },
            contentAlignment = Alignment.Center
        ) {
            // Три линии — hamburger menu
            Column(
                verticalArrangement = Arrangement.spacedBy(2.5.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                repeat(3) {
                    Box(
                        modifier = Modifier
                            .width(if (it == 1) 9.dp else 11.dp)
                            .height(1.5.dp)
                            .clip(RoundedCornerShape(1.dp))
                            .background(Color.White)
                    )
                }
            }
        }
        
        // Бейджик — справа сверху
        if (sessionCount > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 4.dp, y = (-4).dp)
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(GlassColors.mint)
                    .border(1.dp, GlassColors.background, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (sessionCount > 9) "+" else sessionCount.toString(),
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    lineHeight = 8.sp
                )
            }
        }
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
                        GlassColors.accent,
                        GlassColors.accentSecondary
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

// TypingIndicator удалён — используется SSEStatusIndicator

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

    // Clean ONLY markdown bold/italic patterns, NOT single asterisks in emojis
    // **bold** → bold, __underline__ → underline
    text = text.replace(Regex("\\*\\*(.+?)\\*\\*"), "$1")  // **bold**
    text = text.replace(Regex("__(.+?)__"), "$1")          // __underline__
    // Не трогаем одиночные * и _ — они могут быть частью эмодзи

    return text.trim()
}

/**
 * Simple Session Card — единый фиолетовый стиль
 */
@Composable
private fun SimpleSessionCard(
    convo: com.health.companion.data.local.database.ConversationEntity,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd.MM HH:mm", Locale("ru")) }
    val purpleColor = GlassColors.accentSecondary
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (isSelected) purpleColor.copy(alpha = 0.15f)
                else Color.White.copy(alpha = 0.05f)
            )
            .border(
                width = 1.dp,
                color = if (isSelected) purpleColor.copy(alpha = 0.5f)
                        else purpleColor.copy(alpha = 0.2f),
                shape = RoundedCornerShape(14.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Фиолетовая точка
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(purpleColor)
            )
            
            Spacer(Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = convo.title.ifEmpty { "Новый диалог" },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    
                    Spacer(Modifier.width(8.dp))
                    
                    // Время последнего сообщения
                    Text(
                        text = dateFormat.format(Date(convo.lastMessageAt ?: convo.updatedAt)),
                        style = MaterialTheme.typography.labelSmall,
                        color = purpleColor.copy(alpha = 0.7f),
                        fontSize = 10.sp
                    )
                }
                
                Spacer(Modifier.height(2.dp))
                
                Text(
                    text = convo.summary?.take(40) ?: "Нажми чтобы продолжить…",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.45f),
                    maxLines = 1,
                    fontSize = 11.sp
                )
            }
            // Убрана галочка — она смещала дату
        }
    }
}

/**
 * Swipeable wrapper for session cards — swipe left to delete (120Hz smooth)
 * With Telegram-style deletion animation: fade out + shrink + slide up
 */
@Composable
private fun SwipeableSessionItem(
    itemId: String,
    currentRevealedId: String?,
    onReveal: (String?) -> Unit,
    onDelete: () -> Unit,
    content: @Composable () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val offsetX = remember { Animatable(0f) }
    val deleteButtonWidth = 60.dp
    val density = LocalDensity.current
    val deleteButtonWidthPx = with(density) { deleteButtonWidth.toPx() }
    
    var isRevealed by remember { mutableStateOf(false) }
    var itemHeight by remember { mutableStateOf(0) }
    
    // 🎬 Анимация удаления как в Telegram
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
    
    // Прямой расчёт прогресса без доп. анимации - мгновенная реакция!
    val revealProgress = (-offsetX.value / totalSwipeDistance).coerceIn(0f, 1f)
    
    // Если удаление завершено - не показываем
    if (deleteAlpha.value <= 0.01f) return
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                // Анимация удаления - GPU accelerated
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
        // Кнопка удаления - GPU-accelerated через graphicsLayer
        if (revealProgress > 0.01f && !isDeleting) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .width(deleteButtonWidth)
                    .height(with(density) { (itemHeight - 4).toDp() })
                    .graphicsLayer { 
                        // Всё через graphicsLayer для 120Hz
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
                        
                        // 🎬 Плавная анимация удаления
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
                            
                            // Теперь удаляем
                            onDelete()
                            onReveal(null)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "Удалить",
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
        
        // Основной контент - GPU-accelerated
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .onSizeChanged { if (!isDeleting) itemHeight = it.height }
                .graphicsLayer { 
                    // GPU-ускорение для 120Hz
                    translationX = offsetX.value
                }
                .pointerInput(isDeleting) {
                    if (isDeleting) return@pointerInput
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            coroutineScope.launch {
                                if (offsetX.value < -totalSwipeDistance / 2) {
                                    // Открыть
                                    offsetX.animateTo(
                                        -totalSwipeDistance, 
                                        spring(dampingRatio = 0.7f, stiffness = 500f)
                                    )
                                    isRevealed = true
                                    onReveal(itemId)
                                } else {
                                    // Закрыть - быстро!
                                    offsetX.animateTo(
                                        0f, 
                                        tween(120, easing = FastOutSlowInEasing)
                                    )
                                    isRevealed = false
                                    if (currentRevealedId == itemId) onReveal(null)
                                }
                            }
                        },
                        onHorizontalDrag = { _, dragAmount ->
                            coroutineScope.launch {
                                val newOffset = (offsetX.value + dragAmount).coerceIn(-totalSwipeDistance * 1.1f, 0f)
                                offsetX.snapTo(newOffset)
                            }
                        }
                    )
                }
        ) {
            content()
        }
    }
}
