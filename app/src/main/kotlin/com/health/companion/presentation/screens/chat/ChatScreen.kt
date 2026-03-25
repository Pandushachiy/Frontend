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
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.BackHandler
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.HapticFeedbackConstantsCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.health.companion.data.remote.api.MessageDTO
import com.health.companion.presentation.components.*
import com.health.companion.presentation.theme.LocalAppTheme
import com.health.companion.presentation.theme.LocalChatBackground
import com.health.companion.utils.VoiceEventLogger
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import androidx.compose.runtime.snapshotFlow
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Pattern
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.layout.ContentScale
import android.graphics.Color as AndroidColor
import dev.chrisbanes.haze.HazeDefaults
import dev.chrisbanes.haze.HazeProgressive
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.rememberHazeState
import dev.chrisbanes.haze.materials.HazeMaterials
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.onGloballyPositioned
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
    val appTheme = LocalAppTheme.current
    val chatBackground = LocalChatBackground.current
    val uiState by viewModel.uiState.collectAsState()
    val messages by viewModel.messages.collectAsState()
    
    val currentMessage by viewModel.currentMessage.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val isRecording by viewModel.isRecording.collectAsState()
    val partialVoiceResult by viewModel.partialVoiceResult.collectAsState()
    val isUploading by viewModel.isUploading.collectAsState()
    val conversations by viewModel.conversations.collectAsState()
    val streamStatus by viewModel.streamStatus.collectAsState()
    val streamingContent by viewModel.streamingContent.collectAsState()
    val streamingMessageId by viewModel.streamingMessageId.collectAsState()
    val contentStarted by viewModel.contentStarted.collectAsState()
    val webSearchActive by viewModel.webSearchActive.collectAsState()
    val isStreaming by viewModel.isStreaming.collectAsState()
    val imageExpectedInStream by viewModel.imageExpectedInStream.collectAsState()

    val generatedFiles by viewModel.generatedFiles.collectAsState()
    val currentEmotion by viewModel.currentEmotion.collectAsState()
    val streamProgress by viewModel.streamProgress.collectAsState()
    val streamStatusLabel by viewModel.streamStatusLabel.collectAsState()
    val pendingConfirmation by viewModel.pendingConfirmation.collectAsState()
    val confirmationProcessing by viewModel.confirmationProcessing.collectAsState()
    val currentConversationId by viewModel.currentConversationId.collectAsState()
    val messageSendStatus by viewModel.messageSendStatus.collectAsState()
    val authToken by viewModel.authToken.collectAsState()
    val attachedImageUris by viewModel.attachedImageUris.collectAsState()
    val sessionAttachments by viewModel.sessionAttachments.collectAsState()
    val attachmentsLoading by viewModel.attachmentsLoading.collectAsState()

    // При возврате из фона — синхронизировать текущий разговор с сервером.
    // Забирает ответ, который сервер завершил пока приложение было заморожено.
    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                viewModel.syncOnResume()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Telegram-style: reverseLayout = true, scroll to 0 = bottom
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Плавная смена сессии: fade out → scroll → fade in
    // prevConvId инициализируется текущим ID — если приложение восстановлено из
    // состояния (SavedStateHandle), ID уже известен и fade не нужен при старте
    var messagesVisible by remember { mutableStateOf(true) }
    val prevConvId = remember { mutableStateOf(currentConversationId) }
    if (currentConversationId != prevConvId.value) {
        val wasNull = prevConvId.value == null  // первичная загрузка — не fade
        prevConvId.value = currentConversationId
        if (!wasNull) {
            // Fade только при реальной смене сессии пользователем
            messagesVisible = false
        }
    }
    val contentAlpha by animateFloatAsState(
        targetValue = if (messagesVisible) 1f else 0f,
        animationSpec = tween(if (messagesVisible) 280 else 100, easing = FastOutSlowInEasing),
        label = "msgAlpha"
    )
    LaunchedEffect(currentConversationId) {
        // Скроллим к последнему сообщению
        listState.scrollToItem(0)
        if (!messagesVisible) {
            // Пауза только если шёл fade-out (реальная смена сессии)
            delay(60)
            messagesVisible = true
        }
    }
    val keyboardController = LocalSoftwareKeyboardController.current
    val context = LocalContext.current
    val density = LocalDensity.current
    val haptics = LocalHapticFeedback.current
    val view = LocalView.current
    val focusRequester = remember { FocusRequester() }
    val hazeState = rememberHazeState()

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
    var inputAreaHeightPx by remember { mutableStateOf(0) }
    var photoUri by remember { mutableStateOf<Uri?>(null) }
    var showChatsSheet by remember { mutableStateOf(false) }
    var chatsSheetAnimVisible by remember { mutableStateOf(false) }
    var showAttachmentsSheet by remember { mutableStateOf(false) }

    // Telegram-style multi-selection
    var selectedMessages by remember { mutableStateOf<Set<String>>(emptySet()) }
    val isSelectionMode = selectedMessages.isNotEmpty()
    val clipboardManager = LocalClipboardManager.current

    // Анимации режима выбора — используем State<> объекты (без 'by') чтобы читать
    // значения только внутри graphicsLayer { } и избежать перекомпозиции всех баблов
    val selectionStartPadState = animateDpAsState(
        targetValue = if (isSelectionMode) 40.dp else 0.dp,
        animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
        label = "selectionPad"
    )
    val selectionCircleAlphaState = animateFloatAsState(
        targetValue = if (isSelectionMode) 1f else 0f,
        animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
        label = "selectionCircle"
    )
    // Derived state: Dp → Float(px) — вычисляется лениво, не вызывает перекомпозицию
    val selectionOffsetPxState = remember {
        derivedStateOf { with(density) { selectionStartPadState.value.toPx() } }
    }
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
    val audioPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasAudioPermission = isGranted
        VoiceEventLogger.log(context, "audio_permission=$isGranted")
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
            viewModel.attachImage(photoUri!!)
            showAttachMenu = false
        }
    }

    // Gallery picker — прикрепляет НЕСКОЛЬКО фото к сообщению (превью с бейджиками)
    val pickImageLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            viewModel.attachImages(uris)
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

    // Attachment processing state — drives both bubble animation AND input area indicator
    val hasProcessingAttachments = sessionAttachments.any { it.status?.lowercase() == "processing" }
    val hasReadyAttachments = sessionAttachments.any { it.status?.lowercase() == "ready" }
    val analyzingMessageId = if (hasProcessingAttachments) {
        messages.lastOrNull { it.role == "user" && (!it.images.isNullOrEmpty() || it.content.contains("📎")) }?.id
    } else null
    // Track "just became ready" for showing transient success indicator
    var showReadyIndicator by remember { mutableStateOf(false) }
    LaunchedEffect(hasProcessingAttachments) {
        if (!hasProcessingAttachments && hasReadyAttachments) {
            showReadyIndicator = true
            delay(3000)
            showReadyIndicator = false
        }
    }

    // Состояние для полноэкранного просмотра фото из чата
    var previewPhotoAllUris by remember { mutableStateOf<List<String>>(emptyList()) }
    var previewPhotoInitialIndex by remember { mutableIntStateOf(0) }
    var showPhotoPreview by remember { mutableStateOf(false) }

    // Reversed messages for reverseLayout (newest at index 0)
    val reversedMessages = remember(messages) {
        messages
            .distinctBy { it.id }
            .filter { msg ->
                msg.content.isNotBlank() ||
                !msg.images.isNullOrEmpty() ||
                msg.imageUrl != null ||
                !msg.files.isNullOrEmpty() ||
                msg.agentName == "streaming" // streaming placeholder с пустым content
            }
            .reversed()
    }

    // Auto-scroll только когда добавляется новое сообщение (не при обновлении контента)
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(0)
        }
    }

    // Close session window automatically when the last conversation is deleted
    LaunchedEffect(conversations.size) {
        if (conversations.isEmpty() && showChatsSheet) {
            chatsSheetAnimVisible = false
            delay(120)
            showChatsSheet = false
        }
    }

    // Chats overlay — moved out of Dialog so hazeEffect can blur the chat behind it

    // Полноэкранный просмотр фото из чата
    if (showPhotoPreview && previewPhotoAllUris.isNotEmpty()) {
        UserPhotoViewerDialog(
            allUris = previewPhotoAllUris,
            initialIndex = previewPhotoInitialIndex,
            onDismiss = { showPhotoPreview = false }
        )
    }

    val tabBarHeight = bottomBarPadding.calculateBottomPadding()
    val densityLocal = LocalDensity.current
    val imeBottomDp = with(densityLocal) {
        WindowInsets.ime.getBottom(this).toDp()
    }
    // Extra offset caused by keyboard above the tab bar
    val imeOffset = if (imeBottomDp > tabBarHeight) imeBottomDp - tabBarHeight + 5.dp else 0.dp
    val effectiveBottom = tabBarHeight + imeOffset

    val headerBlurStyle = HazeMaterials.thin(containerColor = chatBackground.topColor)
    val inputBlurStyle = HazeMaterials.regular(containerColor = chatBackground.inputColor)
    var headerHeightPx by remember { mutableStateOf(0) }
    val headerHeightDp = with(densityLocal) { headerHeightPx.toDp() }
    var bottomAreaHeightPx by remember { mutableStateOf(0) }
    val bottomAreaHeightDp = with(densityLocal) { bottomAreaHeightPx.toDp() }

    val currentConversation = remember(currentConversationId, conversations) {
        conversations.find { it.id == currentConversationId }
    }
    val headerTitle = currentConversation?.title?.takeIf { it.isNotBlank() }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Full-screen hazeSource: gradient + messages (content to blur)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(chatBackground.gradient)
                .hazeSource(state = hazeState)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = contentAlpha }
            ) {

            Box(modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(bottom = imeOffset)
            ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null
                    ) { 
                        if (showAttachMenu) showAttachMenu = false 
                    },
                reverseLayout = true,
                contentPadding = PaddingValues(
                    start = 6.dp,
                    end = 6.dp,
                    top = headerHeightDp + 8.dp,
                    bottom = bottomAreaHeightDp + tabBarHeight + 8.dp
                ),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                // ═══════════════════════════════════════════════════════════
                // Messages + thinking indicator
                reversedMessages.forEachIndexed { index, message ->
                    val prev = reversedMessages.getOrNull(index - 1) // newer
                    val next = reversedMessages.getOrNull(index + 1) // older
                    val isFirstInGroup = prev?.role != message.role
                    val isLastInGroup = next?.role != message.role

                    // ⚠️ REVERSED: topPadding[i] = gap к item выше (к более старому)
                    val topPadding = when {
                        index == reversedMessages.lastIndex -> 0.dp
                        isLastInGroup -> 10.dp  // Граница между группами разных ролей
                        else -> 1.dp            // Внутри одной группы — минимальный зазор
                    }

                    val msgTime = message.createdAt?.toLongOrNull() ?: 0L
                    val prevTime = prev?.createdAt?.toLongOrNull() ?: Long.MAX_VALUE
                    val showTimestamp = prev == null || (prevTime - msgTime) > 5 * 60 * 1000L

                    val shouldAnimate = message.agentName == "streaming"
                    val showEmotion = currentEmotion.takeIf {
                        message.role == "assistant" && index == 0
                    }

                    item(key = message.id, contentType = message.role) {
                        // liveContent внутри item{} — реактивно обновляется только этот item
                        // когда streamingContent меняется, а не весь список
                        val liveContent = if (message.id == streamingMessageId) streamingContent else null
                        ChatBubbleV2(
                            message = if (liveContent != null) message.copy(content = liveContent) else message,
                            status = messageSendStatus[message.id],
                            isFirstInGroup = isFirstInGroup,
                            isLastInGroup = isLastInGroup,
                            modifier = Modifier
                                .then(
                                    if (shouldAnimate) Modifier
                                    else Modifier.animateItem(
                                        fadeInSpec = null,
                                        fadeOutSpec = tween(durationMillis = 180),
                                        placementSpec = null
                                    )
                                )
                                .padding(top = topPadding),
                            animate = shouldAnimate,
                            authToken = authToken,
                            emotion = showEmotion,
                            showTimestamp = showTimestamp,
                            onRetry = {
                                viewModel.retrySendMessage(message.id, message.content)
                            },
                            onDelete = {
                                viewModel.deleteMessage(message.id)
                            },
                            isSelectionMode = isSelectionMode,
                            isSelected = message.id in selectedMessages,
                            onToggleSelect = {
                                selectedMessages = if (message.id in selectedMessages) {
                                    selectedMessages - message.id
                                } else {
                                    selectedMessages + message.id
                                }
                            },
                            onLongPress = {
                                selectedMessages = setOf(message.id)
                            },
                            selectionOffsetPxState = selectionOffsetPxState,
                            selectionCircleAlphaState = selectionCircleAlphaState,
                            isAnalyzing = message.id == analyzingMessageId,
                            onPhotoClick = { uri ->
                                previewPhotoAllUris = message.images ?: listOf(uri)
                                previewPhotoInitialIndex = (message.images ?: listOf(uri)).indexOf(uri).coerceAtLeast(0)
                                showPhotoPreview = true
                            }
                        )
                    }

                    // Разделитель по дням — появляется над сообщением если следующее
                    // (более старое) сообщение из другого дня, либо это самое первое сообщение
                    // Date separators and timestamps removed per user request

                }

                // Skeleton loading
                if (reversedMessages.isEmpty() && isSyncing) {
                    items(4) {
                        ChatSkeletonBubble()
                    }
                }
            }

            // Empty state — центрированный оверлей (не в reverseLayout списке)
            if (reversedMessages.isEmpty() && !isSyncing) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    ChatEmptyState()
                }
            }
            } // end chat area Box
            } // end padded messages Column
        } // end hazeSource Box

        // Overlays: share same padded coordinate space
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = effectiveBottom)
        ) {

        // ═══ Bottom overlay: error + status + preview + input ═══
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomStart)
                .onSizeChanged { bottomAreaHeightPx = it.height }
        ) {
            // Error banner above input
            if (uiState is ChatUiState.Error) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(GlassColors.error.copy(alpha = 0.12f))
                        .border(
                            width = 0.5.dp,
                            color = GlassColors.error.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(0.dp)
                        )
                        .padding(horizontal = 14.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = GlassColors.error,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = (uiState as? ChatUiState.Error)?.message ?: "",
                        style = GlassTypography.labelSmall,
                        color = GlassColors.textSecondary,
                        modifier = Modifier.weight(1f)
                    )
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .clickable { viewModel.clearError() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Закрыть",
                            tint = GlassColors.textTertiary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            // ═══════════════════════════════════════════════════════════
            // 💭 ЕДИНЫЙ СТАТУС-ИНДИКАТОР — ВНЕ LazyColumn
            // ВСЕ статусы здесь: thinking, web_search, generating_image и т.д.
            // "Стойкие" статусы НЕ пропадают от contentStarted
            // ═══════════════════════════════════════════════════════════
            run {
                // "Стойкие" статусы — НЕ пропадают когда контент начинает стримиться!
                // Всё что агент "делает" (файлы, код, память, напоминания, навыки) — стойкое
                val showThinking = (
                    isUploading ||
                    webSearchActive ||
                    (isLoading && !contentStarted) ||
                    (streamStatus.isNotEmpty() && !contentStarted && streamStatus != "done")
                )

                var lastStatus by remember { mutableStateOf("thinking") }
                if (showThinking) {
                    lastStatus = when {
                        isUploading -> "uploading"
                        streamStatus.isNotEmpty() -> streamStatus
                        isLoading -> "thinking"
                        else -> "thinking"
                    }
                }
                
                // ThinkingProcessCard — аккуратная пилюля статуса
                AnimatedVisibility(
                    visible = showThinking,
                    enter = fadeIn(tween(200)),
                    exit = fadeOut(tween(300))
                ) {
                    ThinkingProcessCard(
                        currentStatus = lastStatus,
                        statusLabel = streamStatusLabel,
                        modifier = Modifier.padding(
                            start = GlassSpacing.screenEdge,
                            top = 4.dp,
                            bottom = 2.dp
                        )
                    )
                }
            }

            AnimatedVisibility(
                visible = pendingConfirmation != null,
                enter = fadeIn(tween(200)) + expandVertically(tween(250), expandFrom = Alignment.Top),
                exit = fadeOut(tween(200)) + shrinkVertically(tween(200))
            ) {
                pendingConfirmation?.let { event ->
                    ConfirmationCard(
                        event = event,
                        onApprove = { viewModel.approveConfirmation() },
                        onReject = { viewModel.rejectConfirmation() },
                        isProcessing = confirmationProcessing,
                        modifier = Modifier.padding(top = 6.dp, bottom = 4.dp)
                    )
                }
            }

            // 👁 Индикатор распознавания фото — пульсирующий, над превью
            AnimatedVisibility(
                visible = hasProcessingAttachments,
                enter = fadeIn(tween(200)) + expandVertically(tween(200), expandFrom = Alignment.Bottom),
                exit = fadeOut(tween(300)) + shrinkVertically(tween(250), shrinkTowards = Alignment.Bottom)
            ) {
                val infiniteTransition = rememberInfiniteTransition(label = "analyzing")
                val pulseAlpha by infiniteTransition.animateFloat(
                    initialValue = 0.4f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(800, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "pulse"
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = GlassSpacing.screenEdge)
                        .padding(bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.RemoveRedEye,
                        contentDescription = null,
                        tint = appTheme.primary.copy(alpha = pulseAlpha),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Распознаю изображение...",
                        style = GlassTypography.labelSmall,
                        color = appTheme.primary.copy(alpha = pulseAlpha)
                    )
                }
            }

            // ✅ Индикатор "Готово" — кратковременный
            AnimatedVisibility(
                visible = showReadyIndicator && !hasProcessingAttachments,
                enter = fadeIn(tween(200)) + expandVertically(tween(200), expandFrom = Alignment.Bottom),
                exit = fadeOut(tween(400)) + shrinkVertically(tween(300), shrinkTowards = Alignment.Bottom)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = GlassSpacing.screenEdge)
                        .padding(bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = GlassColors.success,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Готово — можно задавать вопросы",
                        style = GlassTypography.labelSmall,
                        color = GlassColors.success
                    )
                }
            }

            // 📷 Превью прикреплённых фото — НАД полем ввода (поддержка нескольких)
            AnimatedVisibility(
                visible = attachedImageUris.isNotEmpty(),
                enter = fadeIn(tween(200)) + expandVertically(tween(250), expandFrom = Alignment.Bottom),
                exit = fadeOut(tween(150)) + shrinkVertically(tween(200), shrinkTowards = Alignment.Bottom)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = GlassSpacing.screenEdge)
                        .padding(bottom = 8.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    attachedImageUris.forEachIndexed { index, uri ->
                        // Фото с бейджем удаления
                        Box(modifier = Modifier.size(64.dp)) {
                            // Фото
                            coil.compose.AsyncImage(
                                model = uri,
                                contentDescription = "Фото ${index + 1}",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(12.dp)),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                            )
                            
                            // Бейдж удаления — маленький в углу
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(2.dp)
                                    .size(18.dp)
                                    .shadow(2.dp, CircleShape)
                                    .background(GlassColors.error, CircleShape)
                                    .clickable { viewModel.removeAttachedImage(index) },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Удалить",
                                    tint = Color.White,
                                    modifier = Modifier.size(10.dp)
                                )
                            }
                        }
                    }
                    
                    // Счётчик если много фото
                    if (attachedImageUris.size > 1) {
                        Text(
                            text = "${attachedImageUris.size} фото",
                            style = GlassTypography.labelSmall,
                            color = GlassColors.textSecondary,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }
            }
            
            // INPUT AREA — compact
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                    .padding(horizontal = GlassSpacing.screenEdge)
                    .padding(top = 4.dp, bottom = 2.dp)
                    .graphicsLayer { clip = false }
                    .onGloballyPositioned { inputAreaHeightPx = it.size.height },
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Main input container — frosted glass only here
                    Row(
                        modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 40.dp)
                        .clip(GlassShapes.inputField)
                        .hazeEffect(state = hazeState, style = inputBlurStyle)
                        .background(Color.White.copy(alpha = 0.06f), GlassShapes.inputField)
                        .border(
                            0.5.dp,
                            Brush.horizontalGradient(listOf(
                                appTheme.primary.copy(alpha = 0.15f),
                                appTheme.secondary.copy(alpha = 0.08f)
                            )),
                            GlassShapes.inputField
                        )
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
                        cursorBrush = SolidColor(appTheme.primary),
                        maxLines = 4,  // MAX 4 LINES по спеке!
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Sentences,
                                imeAction = ImeAction.Send
                            ),
                            keyboardActions = KeyboardActions(
                                onSend = {
                                    // Keyboard Enter: only send if there's actual text
                                    // (prevents auto-trigger when returning from gallery picker)
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
                                modifier = Modifier.padding(vertical = 6.dp),
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

                // === Mic/Send button — press-and-hold for voice (Telegram-style) ===

                // Glow animation values (purely visual, zero layout impact)
                val glowAlpha by animateFloatAsState(
                    targetValue = if (isRecording) 1f else 0f,
                    animationSpec = tween(250, easing = FastOutSlowInEasing),
                    label = "glow_alpha"
                )
                val infiniteGlow = rememberInfiniteTransition(label = "voice_glow")
                val glowPulse by infiniteGlow.animateFloat(
                    initialValue = 0.3f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(700, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "glow_pulse"
                )
                val glowRotation by infiniteGlow.animateFloat(
                    initialValue = 0f,
                    targetValue = 360f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(2500, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "glow_rotation"
                )

                // Haptic vibrator helper
                fun performHaptic(strong: Boolean = false) {
                    view.isHapticFeedbackEnabled = true
                    if (strong) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                                vm?.defaultVibrator
                            } else {
                                @Suppress("DEPRECATION")
                                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                            }
                            vibrator?.vibrate(VibrationEffect.createOneShot(40, VibrationEffect.DEFAULT_AMPLITUDE))
                        } else {
                            view.performHapticFeedback(HapticFeedbackConstantsCompat.LONG_PRESS)
                        }
                    } else {
                        view.performHapticFeedback(HapticFeedbackConstantsCompat.KEYBOARD_PRESS)
                    }
                }

                // Colors for glow
                val glowColorError = GlassColors.error
                val glowColorCoral = GlassColors.coral
                val glowColorPink = Color(0xFFFF6B9D)

                // Glass backdrop circle for mic/send button — transparent until content behind
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(GlassSpacing.buttonSize + 6.dp)
                        .clip(CircleShape)
                        .hazeEffect(state = hazeState, style = HazeMaterials.thin(containerColor = Color.Transparent))
                ) {
                Box(
                    modifier = Modifier
                        .size(GlassSpacing.buttonSize)
                        // === GLOW drawn behind the button — NO layout shift ===
                        .drawBehind {
                            if (glowAlpha > 0.01f) {
                                val center = this.center
                                val btnRadius = size.minDimension / 2f

                                // Outer soft glow — large radius
                                drawCircle(
                                    color = glowColorError.copy(alpha = 0.15f * glowPulse * glowAlpha),
                                    radius = btnRadius * (3.5f + 0.8f * glowPulse),
                                    center = center
                                )
                                // Middle glow
                                drawCircle(
                                    color = glowColorCoral.copy(alpha = 0.2f * glowPulse * glowAlpha),
                                    radius = btnRadius * (2.5f + 0.5f * glowPulse),
                                    center = center
                                )
                                // Inner bright ring
                                drawCircle(
                                    color = glowColorPink.copy(alpha = 0.3f * glowPulse * glowAlpha),
                                    radius = btnRadius * (1.8f + 0.3f * glowPulse),
                                    center = center
                                )
                                // Core bright circle right around button
                                drawCircle(
                                    color = glowColorError.copy(alpha = 0.35f * glowAlpha),
                                    radius = btnRadius * 1.35f,
                                    center = center
                                )
                            }
                        }
                        .pointerInput(currentMessage, isLoading, hasAudioPermission) {
                            awaitEachGesture {
                                awaitFirstDown(requireUnconsumed = false)
                                
                                val hasPhotos = viewModel.attachedImageUris.value.isNotEmpty()
                                val canSend = currentMessage.isNotBlank() || hasPhotos

                                if (canSend) {
                                    waitForUpOrCancellation()
                                    if (!isLoading) {
                                        viewModel.sendMessage(currentMessage)
                                        onMessageSent()
                                        coroutineScope.launch {
                                            delay(100)
                                            listState.animateScrollToItem(0)
                                        }
                                    }
                                } else {
                                    // Voice mode — press-and-hold (Telegram-style)
                                    if (!hasAudioPermission) {
                                        waitForUpOrCancellation()
                                        VoiceEventLogger.log(context, "mic_hold request_permission")
                                        audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                        performHaptic(strong = true)
                                        return@awaitEachGesture
                                    }

                                    // PRESS DOWN → start recording + strong haptic
                                    val pressTime = System.currentTimeMillis()
                                    performHaptic(strong = true)
                                    VoiceEventLogger.log(context, "mic_hold start")
                                    viewModel.startVoiceInput()

                                    // Wait for finger UP or cancel
                                    waitForUpOrCancellation()

                                    val holdMs = System.currentTimeMillis() - pressTime

                                    if (holdMs < 300L) {
                                        // Too short — cancel (like Telegram tooltip)
                                        VoiceEventLogger.log(context, "mic_tap_too_short ${holdMs}ms")
                                        viewModel.cancelVoiceInput()
                                    } else {
                                        // Valid hold — stop recording & send
                                        performHaptic(strong = false)
                                        VoiceEventLogger.log(context, "mic_release stop ${holdMs}ms")
                                        viewModel.stopVoiceInput(autoSend = true)
                                    }
                                }
                            }
                        }
                        .shadow(
                            elevation = if (isRecording) 8.dp else 0.dp,
                            shape = CircleShape,
                            spotColor = if (isRecording) GlassColors.error else Color.Transparent
                        )
                        .clip(CircleShape)
                        .background(
                            run {
                                val canSend = currentMessage.isNotBlank() || attachedImageUris.isNotEmpty()
                                when {
                                    isRecording -> Brush.linearGradient(
                                        colors = listOf(GlassColors.error, GlassColors.coral)
                                    )
                                    canSend -> Brush.linearGradient(
                                        colors = listOf(appTheme.primary, appTheme.secondary)
                                    )
                                    else -> Brush.linearGradient(
                                        colors = listOf(
                                            appTheme.primary.copy(alpha = 0.55f),
                                            appTheme.secondary.copy(alpha = 0.55f)
                                        )
                                    )
                                }
                            },
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    val canSend = currentMessage.isNotBlank() || attachedImageUris.isNotEmpty()
                    when {
                        isLoading && canSend -> {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = GlassColors.textPrimary,
                                strokeWidth = 2.dp
                            )
                        }
                        isRecording -> {
                            Icon(
                                Icons.Default.Mic,
                                "Запись...",
                                tint = GlassColors.textPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        canSend -> {
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
                } // end glass backdrop
            }
        } // end bottom overlay Column

        // Scroll to bottom FAB
        val showScrollFab by remember {
            derivedStateOf {
                listState.firstVisibleItemIndex > 2
            }
        }

        AnimatedVisibility(
            visible = showScrollFab,
            enter = fadeIn(tween(200)) + scaleIn(tween(200, easing = FastOutSlowInEasing), initialScale = 0.75f),
            exit = fadeOut(tween(160)) + scaleOut(tween(160, easing = FastOutSlowInEasing), targetScale = 0.75f),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = GlassSpacing.screenEdge, bottom = 72.dp)
        ) {
            SmallFloatingActionButton(
                onClick = {
                    coroutineScope.launch {
                        // Если далеко — сначала прыгаем поближе, потом плавно докатываемся
                        if (listState.firstVisibleItemIndex > 8) {
                            listState.scrollToItem(4)
                        }
                        listState.animateScrollToItem(
                            index = 0,
                            scrollOffset = 0
                        )
                    }
                },
                containerColor = appTheme.primary,
                contentColor = GlassColors.textPrimary,
            ) {
                Icon(Icons.Default.KeyboardArrowDown, "Вниз")
            }
        }

        // Невидимый перехватчик тапов — закрывает attach меню при тапе мимо
        if (showAttachMenu) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null
                    ) { showAttachMenu = false }
            )
        }

        // Attach menu overlay — матовое стекло
        val inputAreaHeightDp = with(density) { inputAreaHeightPx.toDp() }
        AnimatedVisibility(
            visible = showAttachMenu,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = GlassSpacing.screenEdge, bottom = inputAreaHeightDp + 4.dp),
            enter = fadeIn(tween(120)) + slideInVertically(tween(160)) { it },
            exit = fadeOut(tween(80)) + slideOutVertically(tween(110)) { it }
        ) {
            Row(
                modifier = Modifier
                    .shadow(
                        elevation = GlassElevation.modal,
                        shape = GlassShapes.medium,
                        spotColor = appTheme.primary.copy(alpha = 0.25f)
                    )
                    .clip(GlassShapes.medium)
                    .hazeEffect(state = hazeState, style = HazeMaterials.ultraThin(containerColor = chatBackground.inputColor))
                    .border(
                        0.5.dp,
                        Brush.horizontalGradient(listOf(
                            Color.White.copy(alpha = 0.20f),
                            appTheme.primary.copy(alpha = 0.12f)
                        )),
                        GlassShapes.medium
                    )
                    .background(Color.White.copy(alpha = 0.04f)),
                horizontalArrangement = Arrangement.spacedBy(0.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AttachIconButton(
                    icon = Icons.Default.Image,
                    label = "Фото",
                    tint = GlassColors.accentLight
                ) {
                    pickImageLauncher.launch("image/*")
                    showAttachMenu = false
                }
                Box(
                    modifier = Modifier
                        .width(0.5.dp)
                        .height(24.dp)
                        .background(appTheme.primary.copy(alpha = 0.20f))
                )
                AttachIconButton(
                    icon = Icons.Default.Description,
                    label = "Файл",
                    tint = GlassColors.warning
                ) {
                    pickFileLauncher.launch(arrayOf("application/pdf", "application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                    showAttachMenu = false
                }
                Box(
                    modifier = Modifier
                        .width(0.5.dp)
                        .height(24.dp)
                        .background(appTheme.primary.copy(alpha = 0.20f))
                )
                AttachIconButton(
                    icon = Icons.Default.CameraAlt,
                    label = "Камера",
                    tint = GlassColors.mint
                ) {
                    if (hasCameraPermission) {
                        photoUri = createPhotoUri()
                        takePictureLauncher.launch(photoUri!!)
                    } else {
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                    showAttachMenu = false
                }
            }
        }

        } // end overlays Box

        // ═══ Frosted glass header overlay (independent of keyboard) ═══
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopStart)
                .onSizeChanged { headerHeightPx = it.height }
                .padding(bottom = 12.dp)
                .hazeEffect(state = hazeState, style = headerBlurStyle) {
                    progressive = HazeProgressive.verticalGradient(
                        startIntensity = 1f,
                        endIntensity = 0f,
                        easing = Easing { f -> f * f * f }
                    )
                }
        ) {
            AnimatedContent(
                targetState = isSelectionMode,
                transitionSpec = {
                    fadeIn(tween(200)) togetherWith fadeOut(tween(200))
                },
                label = "chatHeader"
            ) { inSelectionMode ->
                if (inSelectionMode) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 4.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { selectedMessages = emptySet() }) {
                            Icon(Icons.Default.Close, contentDescription = "Отменить", tint = Color.White)
                        }
                        Text(
                            text = "${selectedMessages.size}",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.weight(1f))
                        IconButton(onClick = {
                            val text = messages
                                .filter { it.id in selectedMessages }
                                .sortedBy { it.createdAt?.toLongOrNull() ?: 0L }
                                .joinToString("\n\n") { it.content }
                            clipboardManager.setText(AnnotatedString(text))
                            selectedMessages = emptySet()
                        }) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Копировать", tint = Color.White)
                        }
                        IconButton(onClick = {
                            viewModel.deleteMessages(selectedMessages)
                            selectedMessages = emptySet()
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Удалить", tint = GlassColors.error)
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.size(36.dp), contentAlignment = Alignment.Center) {
                            if (conversations.isNotEmpty()) {
                                SessionsIconWithBadge(
                                    sessionCount = conversations.size,
                                    onClick = {
                                        chatsSheetAnimVisible = false
                                        showChatsSheet = true
                                    }
                                )
                            }
                        }
                        if (headerTitle != null) {
                            Text(
                                text = headerTitle,
                                style = GlassTypography.labelMedium,
                                color = GlassColors.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(start = 6.dp)
                            )
                        }
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
        // ── Chats overlay (inside root Box so hazeEffect works) ──
        if (showChatsSheet) {
            BackHandler {
                chatsSheetAnimVisible = false
                coroutineScope.launch { delay(120); showChatsSheet = false }
            }
            LaunchedEffect(Unit) { chatsSheetAnimVisible = true }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.35f))
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) {
                        chatsSheetAnimVisible = false
                        coroutineScope.launch { delay(120); showChatsSheet = false }
                    },
                contentAlignment = Alignment.TopStart
            ) {
                AnimatedVisibility(
                    visible = chatsSheetAnimVisible,
                    enter = fadeIn(tween(200)) + scaleIn(
                        initialScale = 0.95f,
                        animationSpec = tween(200, easing = FastOutSlowInEasing)
                    ),
                    exit = fadeOut(tween(100, easing = FastOutLinearInEasing)) + scaleOut(
                        targetScale = 0.97f,
                        animationSpec = tween(100, easing = FastOutLinearInEasing)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .padding(horizontal = 12.dp, vertical = 55.dp)
                            .statusBarsPadding()
                            .fillMaxWidth()
                            .wrapContentHeight()
                            .heightIn(max = 520.dp)
                            .shadow(
                                elevation = 24.dp,
                                shape = RoundedCornerShape(20.dp),
                                ambientColor = Color.Black.copy(alpha = 0.4f),
                                spotColor = Color.Black.copy(alpha = 0.4f)
                            )
                            .clip(RoundedCornerShape(20.dp))
                            .hazeEffect(
                                state = hazeState,
                                style = HazeStyle(
                                    blurRadius = 70.dp,
                                    tints = listOf(
                                        HazeTint(chatBackground.surfaceColor.copy(alpha = 0.85f)),
                                        HazeTint(Color.White.copy(alpha = 0.04f))
                                    ),
                                    noiseFactor = 0.15f
                                )
                            )
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            ) { }
                    ) {
                        // Строка с "+" наверху справа
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 14.dp, end = 8.dp, top = 8.dp, bottom = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "Диалоги",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 11.sp
                            )
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(appTheme.primary.copy(alpha = 0.18f))
                                    .clickable {
                                        viewModel.createNewConversation()
                                        chatsSheetAnimVisible = false
                                        coroutineScope.launch { delay(120); showChatsSheet = false }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = appTheme.primary, modifier = Modifier.size(16.dp))
                            }
                        }

                        var currentRevealedSessionId by remember { mutableStateOf<String?>(null) }

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f, fill = false)
                                .heightIn(max = 460.dp)
                                .padding(horizontal = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            contentPadding = PaddingValues(bottom = 10.dp)
                        ) {
                            items(conversations.size, key = { conversations[it].id }) { index ->
                                val convo = conversations[index]
                                Box(modifier = Modifier.animateItem(
                                    placementSpec = spring(dampingRatio = 0.7f, stiffness = 400f)
                                )) {
                                    SwipeableSessionItem(
                                        itemId = convo.id,
                                        currentRevealedId = currentRevealedSessionId,
                                        onReveal = { currentRevealedSessionId = it },
                                        onDeleteStart = {},
                                        onDelete = { viewModel.deleteConversation(convo.id) }
                                    ) {
                                        SimpleSessionCard(
                                            convo = convo,
                                            isSelected = convo.id == currentConversationId,
                                            onClick = {
                                                if (currentRevealedSessionId != null) {
                                                    currentRevealedSessionId = null
                                                } else {
                                                    viewModel.selectConversation(convo.id)
                                                    chatsSheetAnimVisible = false
                                                    coroutineScope.launch {
                                                        delay(120)
                                                        showChatsSheet = false
                                                    }
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
    } // end root Box
    
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
 * Compact pill-shaped attach chip — icon + label in a single row
 */
@Composable
private fun CompactAttachChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(color.copy(alpha = 0.12f))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = color,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = color,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.sp
        )
    }
}

/**
 * Кнопка панели прикрепления в стиле glassmorphism —
 * иконка с цветной подложкой + метка
 */
@Composable
private fun AttachIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clickable(
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = null
            ) { onClick() }
            .padding(horizontal = 13.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(tint.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(14.dp)
            )
        }
        Text(
            text = label,
            fontSize = 12.sp,
            color = GlassColors.textSecondary,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.sp
        )
    }
}

@Composable
private fun ChatEmptyState() {
    val theme = LocalAppTheme.current
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
                                theme.primary.copy(alpha = dotPulse * 0.8f),
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
                            theme.primary.copy(alpha = 0.4f),
                            theme.secondary.copy(alpha = 0.1f),
                            theme.secondary.copy(alpha = 0.4f),
                            theme.primary.copy(alpha = 0.1f)
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
                            theme.primary.copy(alpha = glowPulse),
                            theme.secondary.copy(alpha = glowPulse * 0.5f),
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
                            theme.primary,
                            theme.secondary,
                            theme.primary.copy(alpha = 0.9f)
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
    val theme = LocalAppTheme.current
    val chatBg = LocalChatBackground.current
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
                        colors = listOf(theme.primary, theme.secondary)
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
                    .offset(x = 5.dp, y = (-5).dp)
                    .defaultMinSize(minWidth = 15.dp, minHeight = 15.dp)
                    .clip(CircleShape)
                    .background(GlassColors.error)
                    .border(1.5.dp, chatBg.topColor, CircleShape)
                    .padding(horizontal = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (sessionCount > 99) "99+" else sessionCount.toString(),
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
private fun BlueberryAvatarCompact(size: Dp = 22.dp) {
    val theme = LocalAppTheme.current
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(
                Brush.linearGradient(
                    colors = listOf(theme.primary, theme.secondary)
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
private fun BlueberryAvatar(size: Dp = 36.dp) {
    val theme = LocalAppTheme.current
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
                    colors = listOf(theme.primary, theme.secondary)
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

@Composable
private fun SimpleSessionCard(
    convo: com.health.companion.data.local.database.ConversationEntity,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd.MM HH:mm", Locale("ru")) }
    val accent = LocalAppTheme.current.primary

    val cleanSummary = remember(convo.summary) {
        convo.summary?.replace(Regex("\\*{1,2}"), "")?.trim()?.takeIf { it.isNotBlank() }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isSelected) accent.copy(alpha = 0.14f)
                else Color.White.copy(alpha = 0.04f)
            )
            .border(
                0.5.dp,
                if (isSelected) accent.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.08f),
                RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .padding(top = 5.dp)
                .size(6.dp)
                .clip(CircleShape)
                .background(if (isSelected) accent else accent.copy(alpha = 0.5f))
        )

        Spacer(Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = convo.title.ifEmpty { "Новый диалог" },
                style = MaterialTheme.typography.bodySmall,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontSize = 13.sp
            )
            if (cleanSummary != null) {
                Text(
                    text = cleanSummary,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.40f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 10.sp
                )
            }
        }

        Spacer(Modifier.width(6.dp))

        Text(
            text = dateFormat.format(Date(convo.lastMessageAt ?: convo.updatedAt)),
            style = MaterialTheme.typography.labelSmall,
            color = accent.copy(alpha = 0.55f),
            fontSize = 9.sp,
            modifier = Modifier.padding(top = 2.dp)
        )
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
    onDeleteStart: () -> Unit = {},
    onDelete: () -> Unit,
    content: @Composable () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val offsetX = remember { Animatable(0f) }
    val deleteButtonWidth = 44.dp
    val density = LocalDensity.current
    val deleteButtonWidthPx = with(density) { deleteButtonWidth.toPx() }
    
    var isRevealed by remember { mutableStateOf(false) }
    var itemHeightPx by remember { mutableIntStateOf(0) }
    
    val gap = 4.dp
    val gapPx = with(density) { gap.toPx() }
    val totalSwipeDistance = deleteButtonWidthPx + gapPx
    
    LaunchedEffect(currentRevealedId) {
        if (currentRevealedId != itemId && isRevealed) {
            offsetX.animateTo(0f, tween(180, easing = FastOutSlowInEasing))
            isRevealed = false
        }
    }
    
    val revealProgress = (-offsetX.value / totalSwipeDistance).coerceIn(0f, 1f)
    
    Box(modifier = Modifier.fillMaxWidth()) {
        if (revealProgress > 0.01f && itemHeightPx > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .width(deleteButtonWidth)
                    .height(with(density) { itemHeightPx.toDp() })
                    .graphicsLayer { 
                        alpha = revealProgress
                        scaleX = 0.9f + (revealProgress * 0.1f)
                        scaleY = 0.9f + (revealProgress * 0.1f)
                    }
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFD32F2F))
                    .clickable {
                        onDeleteStart()
                        onDelete()
                        onReveal(null)
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .onSizeChanged { itemHeightPx = it.height }
                .graphicsLayer { translationX = offsetX.value }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            coroutineScope.launch {
                                if (offsetX.value < -totalSwipeDistance / 2) {
                                    offsetX.animateTo(
                                        -totalSwipeDistance, 
                                        spring(dampingRatio = 0.7f, stiffness = 500f)
                                    )
                                    isRevealed = true
                                    onReveal(itemId)
                                } else {
                                    offsetX.animateTo(0f, tween(120, easing = FastOutSlowInEasing))
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

// ─────────────────────────────────────────────────────────────────────────────
// Date separator helpers
// ─────────────────────────────────────────────────────────────────────────────

private fun chatSameDay(a: Long, b: Long): Boolean {
    val ca = java.util.Calendar.getInstance().apply { timeInMillis = a }
    val cb = java.util.Calendar.getInstance().apply { timeInMillis = b }
    return ca.get(java.util.Calendar.YEAR) == cb.get(java.util.Calendar.YEAR) &&
           ca.get(java.util.Calendar.DAY_OF_YEAR) == cb.get(java.util.Calendar.DAY_OF_YEAR)
}

private fun chatDayKey(timestampMs: Long): String {
    val cal = java.util.Calendar.getInstance().apply { timeInMillis = timestampMs }
    return "${cal.get(java.util.Calendar.YEAR)}_${cal.get(java.util.Calendar.DAY_OF_YEAR)}"
}

@Composable
private fun ChatDateSeparator(timestampMs: Long) {
    val today = java.util.Calendar.getInstance()
    val yesterday = java.util.Calendar.getInstance().apply {
        add(java.util.Calendar.DAY_OF_YEAR, -1)
    }

    val label = when {
        chatSameDay(timestampMs, today.timeInMillis) -> "Сегодня"
        chatSameDay(timestampMs, yesterday.timeInMillis) -> "Вчера"
        today.get(java.util.Calendar.YEAR) == java.util.Calendar.getInstance()
            .apply { timeInMillis = timestampMs }.get(java.util.Calendar.YEAR) ->
            java.text.SimpleDateFormat("d MMMM", java.util.Locale("ru")).format(java.util.Date(timestampMs))
        else ->
            java.text.SimpleDateFormat("d MMMM yyyy", java.util.Locale("ru")).format(java.util.Date(timestampMs))
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(androidx.compose.ui.graphics.Color.White.copy(alpha = 0.08f))
                .padding(horizontal = 10.dp, vertical = 3.dp)
        ) {
            Text(
                text = label,
                style = GlassTypography.timestamp.copy(color = GlassColors.textSecondary)
            )
        }
    }
}
