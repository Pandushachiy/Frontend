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
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.HapticFeedbackConstantsCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.health.companion.data.remote.api.MessageDTO
import com.health.companion.utils.VoiceEventLogger
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max
import java.util.regex.Pattern
import android.graphics.Color as AndroidColor

@Composable
fun ChatScreen(
    viewModel: ChatViewModel = hiltViewModel(),
    bottomBarPadding: PaddingValues = PaddingValues(0.dp)
) {
    val uiState by viewModel.uiState.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val currentMessage by viewModel.currentMessage.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val isRecording by viewModel.isRecording.collectAsState()
    val partialVoiceResult by viewModel.partialVoiceResult.collectAsState()
    val isUploading by viewModel.isUploading.collectAsState()
    val conversations by viewModel.conversations.collectAsState()
    val currentConversationId by viewModel.currentConversationId.collectAsState()
    val messageSendStatus by viewModel.messageSendStatus.collectAsState()

    val listState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }
    val coroutineScope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current
    val context = LocalContext.current
    val density = LocalDensity.current
    val haptics = LocalHapticFeedback.current
    val view = LocalView.current

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
    var inputBarHeightPx by remember { mutableStateOf(0) }
    var showChatsSheet by remember { mutableStateOf(false) }
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
            // Avoid launching voice immediately after permission dialog on Oppo.
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

    // Gallery picker
    val pickImageLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.uploadFile(it)
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

    // Insets and padding
    val imeBottomPx = WindowInsets.ime.getBottom(density)
    val navBottomPx = WindowInsets.navigationBars.getBottom(density)
    val bottomBarPx = with(density) { bottomBarPadding.calculateBottomPadding().toPx().toInt() }
    val navBottomDp = with(density) { max(navBottomPx, bottomBarPx).toDp() }
    val listBottomInsetsDp = if (imeBottomPx > 0) 0.dp else navBottomDp
    val topInsetsDp = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val inputBarHeightDp = with(density) { inputBarHeightPx.toDp() }

    // Scroll state
    val isAtBottom by remember {
        derivedStateOf {
            val lastIndex = listState.layoutInfo.totalItemsCount - 1
            lastIndex < 0 || listState.layoutInfo.visibleItemsInfo.any { it.index == lastIndex }
        }
    }

    // Auto-scroll on new messages (only when user is at bottom or sends a message)
    val latestMessage = messages.lastOrNull()
    LaunchedEffect(latestMessage?.id) {
        if (latestMessage != null) {
            val shouldScroll = isAtBottom || latestMessage.role == "user"
            if (shouldScroll) {
                delay(50)
                listState.animateScrollToItem(messages.size - 1)
            }
        }
    }

    LaunchedEffect(imeBottomPx) {
        if (imeBottomPx > 0 && messages.isNotEmpty() && isAtBottom) {
            delay(50)
            listState.scrollToItem(messages.size - 1)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surfaceContainerLow
                    )
                )
            )
    ) {
        if (showChatsSheet) {
            ModalBottomSheet(
                onDismissRequest = { showChatsSheet = false },
                sheetState = chatSheetState
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Чаты",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        TextButton(onClick = {
                            viewModel.createNewConversation()
                            showChatsSheet = false
                        }) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("Новый")
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    if (conversations.isEmpty()) {
                        Text(
                            text = "Чатов пока нет",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(16.dp))
                    } else {
                        conversations.forEach { convo ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                color = if (convo.id == currentConversationId) {
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                } else {
                                    MaterialTheme.colorScheme.surface
                                },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = convo.title,
                                            style = MaterialTheme.typography.bodyLarge,
                                            maxLines = 1
                                        )
                                        Text(
                                            text = "Обновлено: ${Date(convo.updatedAt)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    IconButton(onClick = {
                                        viewModel.deleteConversation(convo.id)
                                    }) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Удалить",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                    IconButton(onClick = {
                                        viewModel.selectConversation(convo.id)
                                        showChatsSheet = false
                                    }) {
                                        Icon(
                                            imageVector = Icons.Default.ChevronRight,
                                            contentDescription = "Открыть"
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = topInsetsDp + 12.dp,
                bottom = inputBarHeightDp + listBottomInsetsDp + 12.dp
            ),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (messages.isEmpty() && isSyncing) {
                items(4) {
                    ChatSkeletonBubble()
                }
            } else if (messages.isEmpty()) {
                item { ChatEmptyState() }
            }

            itemsIndexed(messages, key = { _, message -> message.id }) { index, message ->
                val prev = messages.getOrNull(index - 1)
                val isGrouped = prev?.role == message.role
                ChatBubble(
                    message = message,
                    status = messageSendStatus[message.id],
                    showAvatar = !isGrouped,
                    modifier = Modifier.padding(top = if (isGrouped) 2.dp else 10.dp),
                    onRetry = {
                        viewModel.retrySendMessage(message.id, message.content)
                    }
                )
            }

            if (isLoading || isUploading) {
                item { TypingIndicator(isUploading) }
            }
        }

        if (!isAtBottom && messages.size > 3) {
            SmallFloatingActionButton(
                onClick = {
                    coroutineScope.launch {
                        listState.animateScrollToItem(messages.size - 1)
                    }
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = listBottomInsetsDp + inputBarHeightDp + 16.dp)
            ) {
                Icon(Icons.Default.KeyboardArrowDown, "Вниз")
            }
        }

        if (uiState is ChatUiState.Error) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = topInsetsDp),
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

        // Input bar over content
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = if (imeBottomPx > 0) 0.dp else navBottomDp)
                .imePadding()
                .background(MaterialTheme.colorScheme.surface)
                .onSizeChanged { inputBarHeightPx = it.height }
        ) {
            if (showAttachMenu) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    AttachOption(Icons.Default.CameraAlt, "Камера", MaterialTheme.colorScheme.primary) {
                        if (hasCameraPermission) {
                            photoUri = createPhotoUri()
                            takePictureLauncher.launch(photoUri!!)
                        } else {
                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    }
                    AttachOption(Icons.Default.Image, "Фото", MaterialTheme.colorScheme.secondary) {
                        pickImageLauncher.launch("image/*")
                    }
                    AttachOption(Icons.Default.Description, "Файл", MaterialTheme.colorScheme.tertiary) {
                        pickFileLauncher.launch(arrayOf(
                            "application/pdf",
                            "application/msword",
                            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                            "image/*"
                        ))
                    }
                }
            }

            if (isRecording) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val transition = rememberInfiniteTransition(label = "mic")
                    val scale by transition.animateFloat(1f, 1.3f, infiniteRepeatable(tween(500), RepeatMode.Reverse), label = "scale")
                    Icon(Icons.Default.Mic, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.scale(scale))
                    Spacer(Modifier.width(12.dp))
                    val statusText = if (partialVoiceResult.isNotEmpty()) partialVoiceResult else "Запись..."
                    Text(statusText, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }

            val micPulseTransition = rememberInfiniteTransition(label = "mic_pulse")
            val micPulse by micPulseTransition.animateFloat(
                initialValue = 0.9f,
                targetValue = 1.2f,
                animationSpec = infiniteRepeatable(
                    animation = tween(700, easing = EaseInOutCubic),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "mic_pulse"
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    viewModel.refreshConversations()
                    showChatsSheet = true
                }, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Chat,
                        contentDescription = "Чаты",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(onClick = { showAttachMenu = !showAttachMenu }, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = if (showAttachMenu) Icons.Default.Close else Icons.Default.Add,
                        contentDescription = "Прикрепить",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                OutlinedTextField(
                    value = currentMessage,
                    onValueChange = viewModel::updateCurrentMessage,
                    modifier = Modifier.weight(1f).heightIn(min = 44.dp, max = 110.dp),
                    placeholder = { Text(if (isRecording) "Говорите..." else "Сообщение") },
                    maxLines = 4,
                    singleLine = false,
                    textStyle = MaterialTheme.typography.bodyMedium,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Send
                    ),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            if (currentMessage.isNotBlank() && !isLoading) {
                                viewModel.sendMessage(currentMessage)
                                keyboardController?.hide()
                            }
                        }
                    ),
                    shape = RoundedCornerShape(18.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                    )
                )

                Spacer(Modifier.width(8.dp))

                if (currentMessage.isBlank()) {
                    Box(contentAlignment = Alignment.Center) {
                        if (isRecording) {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .scale(micPulse)
                                    .background(
                                        MaterialTheme.colorScheme.error.copy(alpha = 0.25f),
                                        CircleShape
                                    )
                            )
                        }
                        FilledIconButton(
                            onClick = {
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
                            },
                            modifier = Modifier.size(40.dp),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = if (isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(
                                imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                                contentDescription = if (isRecording) "Стоп" else "Голос",
                                tint = if (isRecording) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                } else {
                    FilledIconButton(
                        onClick = {
                            viewModel.sendMessage(currentMessage)
                            keyboardController?.hide()
                        },
                        enabled = !isLoading,
                        modifier = Modifier.size(40.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                        } else {
                            Icon(
                                Icons.AutoMirrored.Filled.Send,
                                "Отправить",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AttachOption(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, color: Color, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(8.dp)) {
        FilledIconButton(
            onClick = onClick,
            modifier = Modifier.size(56.dp),
            colors = IconButtonDefaults.filledIconButtonColors(containerColor = color.copy(alpha = 0.15f))
        ) {
            Icon(icon, label, tint = color, modifier = Modifier.size(28.dp))
        }
        Spacer(Modifier.height(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ChatEmptyState() {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.size(56.dp).clip(CircleShape).background(
                    Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary))
                ),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Psychology, null, tint = Color.White, modifier = Modifier.size(28.dp))
            }

            Spacer(Modifier.height(12.dp))
            Text(
                "Начните чат — напишите сообщение ниже",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
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
    onRetry: () -> Unit = {}
) {
    val isUser = message.role == "user"
    val formattedText = remember(message.content) { formatMessageText(message.content) }

    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start) {
        if (!isUser) {
            if (showAvatar) {
                Box(
                    modifier = Modifier.size(36.dp).clip(CircleShape).background(
                        Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary))
                    ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.SmartToy, null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(8.dp))
                ProviderDot(
                    provider = message.provider,
                    providerColor = message.provider_color,
                    modelUsed = message.model_used
                )
                Spacer(Modifier.width(6.dp))
            }
        }

        Column(horizontalAlignment = if (isUser) Alignment.End else Alignment.Start, modifier = Modifier.widthIn(max = 340.dp)) {
            if (!isUser && message.agent_name != null && message.agent_name != "chat" && message.agent_name != "offline") {
                Text(message.agent_name, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium, modifier = Modifier.padding(bottom = 4.dp))
            }

            Surface(
                color = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = if (isUser) 20.dp else 4.dp, bottomEnd = if (isUser) 4.dp else 20.dp),
                tonalElevation = if (isUser) 0.dp else 2.dp
            ) {
                Text(
                    formattedText,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    style = MaterialTheme.typography.bodySmall.copy(lineHeight = 19.sp),
                    color = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                )
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
                Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Person, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
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
private fun TypingIndicator(isUploading: Boolean) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
        Box(
            modifier = Modifier.size(36.dp).clip(CircleShape).background(
                Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary))
            ),
            contentAlignment = Alignment.Center
        ) {
            if (isUploading) {
                Icon(Icons.Default.CloudUpload, null, tint = Color.White, modifier = Modifier.size(20.dp))
            } else {
                Icon(Icons.Default.SmartToy, null, tint = Color.White, modifier = Modifier.size(20.dp))
            }
        }

        Spacer(Modifier.width(8.dp))

        Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(20.dp, 20.dp, 20.dp, 4.dp)) {
            Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (isUploading) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text("Загрузка...", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    val transition = rememberInfiniteTransition(label = "dots")
                    repeat(3) { index ->
                        val alpha by transition.animateFloat(0.3f, 1f, infiniteRepeatable(tween(600, delayMillis = index * 200), RepeatMode.Reverse), label = "a$index")
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = alpha)))
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
