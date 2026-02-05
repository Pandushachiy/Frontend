package com.health.companion.presentation.screens.chat

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.health.companion.BuildConfig
import com.health.companion.data.remote.api.MessageDTO
import com.health.companion.presentation.components.*
import kotlinx.coroutines.launch
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

// API Host for constructing full URLs
private val API_HOST = BuildConfig.API_BASE_URL.substringBefore("/api/")

// ============================================================================
// ChatBubble V2 — по спецификации chat_design_prompt.md
// Telegram-style с асимметричными углами и timestamp снаружи
// ============================================================================

/**
 * Message bubble V2 — компактный дизайн
 * 
 * - Скруглённые углы (16dp/20dp)
 * - БЕЗ аватара юзера
 * - Салатовый/мятный градиент для user
 * - Цветной полупрозрачный фон для AI
 * - Компактные размеры
 * - Long press для удаления сообщения
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatBubbleV2(
    message: MessageDTO,
    status: MessageSendStatus?,
    isFirstInGroup: Boolean,
    isLastInGroup: Boolean,
    modifier: Modifier = Modifier,
    animate: Boolean = false,
    authToken: String? = null,
    onRetry: () -> Unit = {},
    onDelete: () -> Unit = {}
) {
    val isUser = message.role == "user"
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val maxBubbleWidth = screenWidth * 0.92f // Больше ширина для контента
    val haptic = LocalHapticFeedback.current
    
    // Режим удаления — сообщение выделено красным
    var isInDeleteMode by remember { mutableStateOf(false) }
    
    // Анимация исчезновения при удалении
    var isDeleting by remember { mutableStateOf(false) }
    val deleteAlpha by animateFloatAsState(
        targetValue = if (isDeleting) 0f else 1f,
        animationSpec = tween(350, easing = EaseOutCubic),
        finishedListener = { if (isDeleting) onDelete() },
        label = "deleteAlpha"
    )
    val deleteScale by animateFloatAsState(
        targetValue = if (isDeleting) 0.85f else 1f,
        animationSpec = tween(350, easing = EaseOutCubic),
        label = "deleteScale"
    )
    val deleteOffsetY by animateFloatAsState(
        targetValue = if (isDeleting) -30f else 0f,
        animationSpec = tween(350, easing = EaseOutCubic),
        label = "deleteOffsetY"
    )
    
    // Анимация красного контура
    val borderAlpha by animateFloatAsState(
        targetValue = if (isInDeleteMode) 1f else 0f,
        animationSpec = tween(200),
        label = "borderAlpha"
    )
    
    val formattedText = remember(message.content) { formatMessageTextV2(message.content) }
    
    val timestamp = remember(message.createdAt) {
        try {
            val dateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            dateFormat.format(Date(message.createdAt?.toLongOrNull() ?: System.currentTimeMillis()))
        } catch (e: Exception) { "" }
    }
    
    // Более скруглённые формы
    val bubbleShape = RoundedCornerShape(
        topStart = if (!isUser && isFirstInGroup) 6.dp else 18.dp,
        topEnd = if (isUser && isFirstInGroup) 6.dp else 18.dp,
        bottomStart = 18.dp,
        bottomEnd = 18.dp
    )
    
    // Основной контейнер
    Box(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                alpha = deleteAlpha
                scaleX = deleteScale
                scaleY = deleteScale
                translationY = deleteOffsetY
            }
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { if (isInDeleteMode) isInDeleteMode = false },
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    isInDeleteMode = true
                }
            )
    ) {
        // Основной контент
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
                verticalAlignment = Alignment.Top
            ) {
            // === ASSISTANT SIDE ===
            if (!isUser) {
                // Avatar (только для первого в группе) — выравнивание TOP
                if (isFirstInGroup) {
                    BlueberryAvatarV2(size = 24.dp)
                    Spacer(Modifier.width(5.dp))
                } else {
                    Spacer(Modifier.width(29.dp))
                }
                
                // Проверяем есть ли картинка (сгенерированная или загруженная)
                val hasGeneratedImage = message.imageUrl != null
                val hasUploadedImages = !message.images.isNullOrEmpty()
                // Скрываем текст-заглушку "[Изображение: ...]" если есть реальная картинка
                val isImagePlaceholder = message.content.startsWith("[Изображение:") || 
                                         message.content.startsWith("[Image:")
                val hasText = message.content.isNotBlank() && !(hasGeneratedImage && isImagePlaceholder)
                val context = LocalContext.current
                
                Column(modifier = Modifier.widthIn(max = maxBubbleWidth)) {
                    // Загруженные изображения (превью) — для system сообщений
                    if (hasUploadedImages) {
                        message.images?.forEach { imageUri ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                            ) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(Uri.parse(imageUri))
                                        .crossfade(200)
                                        .build(),
                                    contentDescription = "Загруженное фото",
                                    contentScale = ContentScale.FillWidth,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                )
                            }
                        }
                        if (hasText) Spacer(Modifier.height(6.dp))
                    }
                    
                    // Сгенерированная картинка
                    if (hasGeneratedImage) {
                        message.imageUrl?.let { imageUrl ->
                            Timber.d("ChatBubbleV2: displaying image $imageUrl")
                            android.util.Log.d("IMAGE_DEBUG", "🖼️ ChatBubbleV2 showing imageUrl=$imageUrl")
                            GeneratedImageCard(
                                imageUrl = imageUrl,
                                authToken = authToken,
                                modifier = Modifier
                            )
                        }
                        if (hasText) Spacer(Modifier.height(6.dp))
                    }
                    
                    // Текст в bubble (если есть)
                    if (hasText) {
                        Box(
                            modifier = Modifier
                                .shadow(2.dp, bubbleShape, spotColor = Color.Black.copy(alpha = 0.2f))
                                .clip(bubbleShape)
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(
                                            GlassColors.surfaceAlt.copy(alpha = 0.95f),
                                            GlassColors.surface.copy(alpha = 0.9f)
                                        )
                                    ),
                                    bubbleShape
                                )
                                .border(1.dp, Color(0xFF4A5580).copy(alpha = 0.4f), bubbleShape)
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Column {
                                if (message.agentName != null && 
                                    message.agentName !in listOf("chat", "offline", "streaming", "system") &&
                                    isFirstInGroup && !hasGeneratedImage && !hasUploadedImages) {
                                    Text(
                                        text = message.agentName,
                                        style = GlassTypography.timestamp.copy(
                                            color = GlassColors.accent,
                                            fontWeight = FontWeight.Medium
                                        ),
                                        modifier = Modifier.padding(bottom = 2.dp)
                                    )
                                }
                                
                                MarkdownTextV2(
                                    content = message.content,
                                    animate = animate
                                )
                            }
                        }
                    }
                }
            }
            
            // === USER SIDE (БЕЗ аватара!) ===
            if (isUser) {
                val context = LocalContext.current
                val hasUserImages = !message.images.isNullOrEmpty()
                
                Column(
                    modifier = Modifier.widthIn(max = maxBubbleWidth),
                    horizontalAlignment = Alignment.End
                ) {
                    // Прикреплённые изображения юзера (Image-to-Image)
                    if (hasUserImages) {
                        message.images?.forEach { imageUri ->
                            Box(
                                modifier = Modifier
                                    .padding(bottom = 6.dp)
                                    .size(120.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(GlassColors.surface.copy(alpha = 0.3f))
                            ) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(Uri.parse(imageUri))
                                        .crossfade(200)
                                        .memoryCacheKey(imageUri)
                                        .diskCacheKey(imageUri)
                                        .build(),
                                    contentDescription = "Прикреплённое фото",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                                // Edit badge
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .padding(4.dp)
                                        .size(20.dp)
                                        .background(GlassColors.accent.copy(alpha = 0.9f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("✏️", fontSize = 10.sp)
                                }
                            }
                        }
                    }
                    
                    // Текстовый bubble с мятным градиентом
                    if (message.content.isNotBlank()) {
                        Box(
                            modifier = Modifier
                                .shadow(2.dp, bubbleShape, spotColor = Color.Black.copy(alpha = 0.25f))
                                .clip(bubbleShape)
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(
                                            GlassColors.mint,
                                            GlassColors.mint.copy(alpha = 0.8f)
                                        )
                                    ),
                                    bubbleShape
                                )
                                .border(1.dp, GlassColors.mint.copy(alpha = 0.3f), bubbleShape)
                                .padding(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = formattedText,
                                style = GlassTypography.messageText.copy(fontSize = 13.sp, lineHeight = 18.sp),
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
        
        // === TIMESTAMP ===
        if (isLastInGroup) {
            Spacer(Modifier.height(2.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = if (!isUser) 34.dp else 0.dp),
                horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = timestamp,
                    style = GlassTypography.timestamp.copy(fontSize = 10.sp)
                )
                
                // Галочки убраны — только время
            }
        }
        
        // === ERROR + RETRY ===
        if (isUser && status == MessageSendStatus.Failed) {
            Spacer(Modifier.height(2.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Не отправлено", style = GlassTypography.timestamp.copy(color = GlassColors.error))
                Spacer(Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(GlassColors.error.copy(alpha = 0.15f))
                        .clickable { onRetry() }
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text("Повторить", style = GlassTypography.timestamp.copy(color = GlassColors.error))
                }
            }
        }
        }  // Close Column
        
        // === КРАСНЫЙ КОНТУР И КНОПКИ УДАЛЕНИЯ ===
        if (isInDeleteMode) {
            // Контур вокруг всего сообщения
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .padding(horizontal = if (!isUser) 30.dp else 0.dp)
                    .border(
                        width = 2.dp,
                        color = Color(0xFFE53935).copy(alpha = borderAlpha),
                        shape = RoundedCornerShape(20.dp)
                    )
            )
            
            // Кнопки удаления/отмены
            Row(
                modifier = Modifier
                    .align(if (isUser) Alignment.BottomEnd else Alignment.BottomStart)
                    .padding(
                        start = if (!isUser) 38.dp else 0.dp,
                        end = if (isUser) 4.dp else 0.dp,
                        bottom = 4.dp
                    )
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF1A1A2E).copy(alpha = 0.98f))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Кнопка УДАЛИТЬ
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFE53935))
                        .clickable { 
                            isInDeleteMode = false
                            isDeleting = true
                        }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Удалить",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                
                // Кнопка ОТМЕНА
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF3A3F5C))
                        .clickable { isInDeleteMode = false }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "Отмена",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }  // Close Box wrapper
}  // Close ChatBubbleV2

/**
 * Blueberry Avatar V2 — компактный
 */
@Composable
private fun BlueberryAvatarV2(size: Dp = 28.dp) {
    Box(
        modifier = Modifier
            .size(size)
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

/**
 * Markdown Text V2 — простой рендерер без внешних библиотек
 * Поддерживает: жирный, курсив, код, списки, таблицы (базово)
 * 
 * Анимация: плавное появление символов по мере стриминга
 * visibleChars догоняет content.length, не сбрасывается при обновлении
 */
@Composable
private fun MarkdownTextV2(
    content: String,
    animate: Boolean = false
) {
    // visibleChars плавно догоняет content.length
    // При обновлении content — visibleChars НЕ сбрасывается, а продолжает расти
    var visibleChars by remember { mutableStateOf(if (animate) 0 else content.length) }
    
    // Если анимация выключена — показываем всё сразу
    LaunchedEffect(animate, content.length) {
        if (!animate) {
            visibleChars = content.length
            return@LaunchedEffect
        }
        
        // Плавно догоняем content.length
        while (visibleChars < content.length) {
            kotlinx.coroutines.delay(12) // Быстрее для плавности
            // Добавляем по 2-3 символа за раз для естественности
            visibleChars = (visibleChars + 2).coerceAtMost(content.length)
        }
    }
    
    val displayText = content.take(visibleChars)
    
    // Парсим и рендерим markdown
    val blocks = remember(displayText) { parseMarkdownBlocks(displayText) }
    
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        blocks.forEach { block ->
            when (block) {
                is MarkdownBlock.CodeBlock -> {
                    // Код блок со скроллом — компактный
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF1A1B26))
                            .horizontalScroll(rememberScrollState())
                            .padding(8.dp)
                    ) {
                        Text(
                            text = block.code,
                            style = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = Color(0xFF9ECE6A),
                                lineHeight = 14.sp
                            )
                        )
                    }
                }
                is MarkdownBlock.Table -> {
                    // Таблица
                    TableRenderer(block.rows)
                }
                
                is MarkdownBlock.Heading -> {
                    // Заголовок с акцентом
                    Text(
                        text = parseInlineMarkdown(block.text),
                        style = TextStyle(
                            fontSize = when (block.level) {
                                1 -> 18.sp
                                2 -> 16.sp
                                3 -> 14.sp
                                else -> 13.sp  // #### и более
                            },
                            fontWeight = FontWeight.Bold,
                            color = when (block.level) {
                                1 -> Color(0xFF7AA2F7)  // Синий
                                2 -> Color(0xFF9ECE6A)  // Зелёный
                                3 -> Color(0xFFBB9AF7)  // Фиолетовый
                                else -> Color(0xFFE0AF68)  // Оранжевый для ####
                            },
                            lineHeight = 22.sp
                        ),
                        modifier = Modifier.padding(top = if (block.level == 1) 8.dp else 4.dp)
                    )
                }
                
                is MarkdownBlock.ListItem -> {
                    // Элемент списка с буллетом
                    Row(
                        modifier = Modifier.padding(start = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "•",
                            color = Color(0xFF9ECE6A),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = parseInlineMarkdown(block.text),
                            style = GlassTypography.messageText.copy(
                                fontSize = 13.sp,
                                lineHeight = 18.sp
                            ),
                            color = Color.White
                        )
                    }
                }
                
                is MarkdownBlock.Quote -> {
                    // Цитата с левой полосой
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp)
                            .background(
                                Color(0xFFFFC107).copy(alpha = 0.1f),
                                RoundedCornerShape(4.dp)
                            )
                            .border(
                                width = 3.dp,
                                color = Color(0xFFFFC107),
                                shape = RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp)
                            )
                            .padding(start = 10.dp, top = 6.dp, bottom = 6.dp, end = 8.dp)
                    ) {
                        Text(
                            text = parseInlineMarkdown(block.text),
                            style = GlassTypography.messageText.copy(
                                fontSize = 13.sp,
                                lineHeight = 18.sp,
                                fontStyle = FontStyle.Italic
                            ),
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }
                
                is MarkdownBlock.Divider -> {
                    // Разделитель
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 6.dp),
                        thickness = 1.dp,
                        color = Color.White.copy(alpha = 0.2f)
                    )
                }
                
                is MarkdownBlock.Text -> {
                    // Обычный текст с inline форматированием
                    Text(
                        text = parseInlineMarkdown(block.text),
                        style = GlassTypography.messageText.copy(
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        ),
                        color = Color.White
                    )
                }
            }
        }
    }
}

// === Markdown Parser ===

private sealed class MarkdownBlock {
    data class Text(val text: String) : MarkdownBlock()
    data class CodeBlock(val language: String, val code: String) : MarkdownBlock()
    data class Table(val rows: List<List<String>>) : MarkdownBlock()
    data class Heading(val level: Int, val text: String) : MarkdownBlock()  // ## или ###
    data class ListItem(val text: String) : MarkdownBlock()                  // - item
    data class Quote(val text: String) : MarkdownBlock()                     // > quote
    object Divider : MarkdownBlock()                                          // ---
}

private fun parseMarkdownBlocks(text: String): List<MarkdownBlock> {
    val blocks = mutableListOf<MarkdownBlock>()
    val lines = text.lines()
    var i = 0
    var textBuffer = StringBuilder()
    
    fun flushTextBuffer() {
        val txt = textBuffer.toString().trim()
        if (txt.isNotEmpty()) {
            blocks.add(MarkdownBlock.Text(txt))
        }
        textBuffer = StringBuilder()
    }
    
    while (i < lines.size) {
        val line = lines[i]
        val trimmedLine = line.trim()
        
        // Код блок начало
        if (trimmedLine.startsWith("```")) {
            flushTextBuffer()
            val lang = trimmedLine.removePrefix("```").trim()
            val codeLines = mutableListOf<String>()
            i++
            while (i < lines.size && !lines[i].trim().startsWith("```")) {
                codeLines.add(lines[i])
                i++
            }
            blocks.add(MarkdownBlock.CodeBlock(lang, codeLines.joinToString("\n")))
            i++ // пропускаем закрывающий ```
            continue
        }
        
        // Разделитель ---
        if (trimmedLine.matches(Regex("^-{3,}$")) || trimmedLine.matches(Regex("^\\*{3,}$"))) {
            flushTextBuffer()
            blocks.add(MarkdownBlock.Divider)
            i++
            continue
        }
        
        // Заголовок # ## ### #### (проверяем от большего к меньшему!)
        if (trimmedLine.startsWith("#### ")) {
            flushTextBuffer()
            blocks.add(MarkdownBlock.Heading(4, trimmedLine.removePrefix("#### ").trim()))
            i++
            continue
        }
        if (trimmedLine.startsWith("### ")) {
            flushTextBuffer()
            blocks.add(MarkdownBlock.Heading(3, trimmedLine.removePrefix("### ").trim()))
            i++
            continue
        }
        if (trimmedLine.startsWith("## ")) {
            flushTextBuffer()
            blocks.add(MarkdownBlock.Heading(2, trimmedLine.removePrefix("## ").trim()))
            i++
            continue
        }
        if (trimmedLine.startsWith("# ")) {
            flushTextBuffer()
            blocks.add(MarkdownBlock.Heading(1, trimmedLine.removePrefix("# ").trim()))
            i++
            continue
        }
        
        // Цитата > text
        if (trimmedLine.startsWith("> ")) {
            flushTextBuffer()
            val quoteText = StringBuilder(trimmedLine.removePrefix("> "))
            i++
            // Собираем многострочные цитаты
            while (i < lines.size && lines[i].trim().startsWith("> ")) {
                quoteText.append("\n").append(lines[i].trim().removePrefix("> "))
                i++
            }
            blocks.add(MarkdownBlock.Quote(quoteText.toString()))
            continue
        }
        
        // Список - item или * item
        if (trimmedLine.matches(Regex("^[-*•]\\s+.+"))) {
            flushTextBuffer()
            val itemText = trimmedLine.replaceFirst(Regex("^[-*•]\\s+"), "")
            blocks.add(MarkdownBlock.ListItem(itemText))
            i++
            continue
        }
        
        // Проверка на таблицу: строка содержит | и следующая тоже (или это заголовок с ---)
        if (line.contains("|") && trimmedLine.let { it.startsWith("|") || it.count { c -> c == '|' } >= 2 }) {
            flushTextBuffer()
            val tableLines = mutableListOf<String>()
            while (i < lines.size && lines[i].contains("|")) {
                val tableLine = lines[i].trim()
                // Пропускаем разделители типа |---|---|
                if (!tableLine.replace("|", "").replace("-", "").replace(":", "").replace(" ", "").isEmpty() ||
                    tableLine.contains("---").not()) {
                    if (!tableLine.matches(Regex("^[\\|\\s\\-:]+$"))) {
                        tableLines.add(tableLine)
                    }
                }
                i++
            }
            
            if (tableLines.isNotEmpty()) {
                val rows = tableLines.map { row ->
                    row.split("|")
                        .map { it.trim() }
                        .filter { it.isNotEmpty() }
                }.filter { it.isNotEmpty() }
                
                if (rows.isNotEmpty()) {
                    blocks.add(MarkdownBlock.Table(rows))
                }
            }
            continue
        }
        
        // Обычный текст
        textBuffer.appendLine(line)
        i++
    }
    
    flushTextBuffer()
    return blocks.ifEmpty { listOf(MarkdownBlock.Text(text)) }
}

private fun parseInlineMarkdown(text: String): AnnotatedString {
    return buildAnnotatedString {
        // Используем regex для корректного парсинга **bold** и *italic*
        var current = text
            // Фикс прилипших чисел: "26февраля" -> "26 февраля"
            .replace(Regex("(\\d)([а-яА-ЯёЁ])")) { "${it.groupValues[1]} ${it.groupValues[2]}" }
            .replace(Regex("([а-яА-ЯёЁ])(\\d)")) { "${it.groupValues[1]} ${it.groupValues[2]}" }
            // Сначала заменяем ** (жирный) - ВАЖНО: до одинарных *
            .replace(Regex("\\*\\*(.+?)\\*\\*")) { "⬛BOLD⬛${it.groupValues[1]}⬛BOLD⬛" }
            .replace(Regex("__(.+?)__")) { "⬛BOLD⬛${it.groupValues[1]}⬛BOLD⬛" }
            // Затем одинарные * и _ (курсив)
            .replace(Regex("(?<!\\*)\\*(?!\\*)(.+?)(?<!\\*)\\*(?!\\*)")) { "⬛ITALIC⬛${it.groupValues[1]}⬛ITALIC⬛" }
            .replace(Regex("(?<!_)_(?!_)(.+?)(?<!_)_(?!_)")) { "⬛ITALIC⬛${it.groupValues[1]}⬛ITALIC⬛" }
            // Inline код
            .replace(Regex("`([^`]+)`")) { "⬛CODE⬛${it.groupValues[1]}⬛CODE⬛" }
            .replace("• ", "  • ")
            .replace(Regex("^#{1,4}\\s+", RegexOption.MULTILINE), "") // Удаляем оставшиеся заголовки
        
        var isBold = false
        var isItalic = false
        var isCode = false
        
        val parts = current.split("⬛")
        parts.forEach { part ->
            when (part) {
                "BOLD" -> isBold = !isBold
                "ITALIC" -> isItalic = !isItalic
                "CODE" -> isCode = !isCode
                else -> {
                    val style = SpanStyle(
                        fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
                        fontStyle = if (isItalic) FontStyle.Italic else FontStyle.Normal,
                        fontFamily = if (isCode) FontFamily.Monospace else FontFamily.Default,
                        background = if (isCode) Color(0xFF2D2D3D) else Color.Transparent,
                        color = if (isCode) Color(0xFF7AA2F7) else Color.Unspecified
                    )
                    withStyle(style) { append(part) }
                }
            }
        }
    }
}

/**
 * Красивый рендеринг таблицы — карточки вместо ASCII
 * Горизонтальный скролл для широких таблиц
 */
@Composable
private fun TableRenderer(rows: List<List<String>>) {
    if (rows.isEmpty()) return
    
    val headers = rows.firstOrNull() ?: return
    val dataRows = rows.drop(1)
    
    // Если таблица маленькая (1-2 колонки) — вертикальные карточки
    // Если большая — горизонтальный скролл
    if (headers.size <= 2 && dataRows.isNotEmpty()) {
        // Компактный вид: каждая строка как мини-карточка
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            dataRows.forEach { row ->
                TableRowCard(headers, row)
            }
        }
    } else {
        // Таблица с горизонтальным скроллом и ФИКСИРОВАННЫМИ колонками
        val scrollState = rememberScrollState()
        val showScrollHint = scrollState.value == 0 && headers.size > 3
        
        // Вычисляем ширину каждой колонки по максимальному контенту
        val columnWidths = remember(rows) {
            headers.indices.map { colIndex ->
                val headerLen = headers.getOrNull(colIndex)?.length ?: 0
                val maxDataLen = dataRows.maxOfOrNull { row -> 
                    row.getOrNull(colIndex)?.length ?: 0 
                } ?: 0
                val maxLen = maxOf(headerLen, maxDataLen)
                // Минимум 50dp, ~7dp на символ, максимум 120dp
                (50 + maxLen * 6).coerceIn(50, 120)
            }
        }
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF1E2235),
                            Color(0xFF171B2C)
                        )
                    )
                )
                .border(1.dp, Color(0xFF3D4A6A).copy(alpha = 0.5f), RoundedCornerShape(10.dp))
        ) {
            Column(
                modifier = Modifier
                    .horizontalScroll(scrollState)
                    .padding(4.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    GlassColors.surfaceAlt,
                                    Color(0xFF232945)
                                )
                            ),
                            RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)
                        )
                        .padding(vertical = 8.dp)
                ) {
                    headers.forEachIndexed { index, header ->
                        Text(
                            text = header,
                            modifier = Modifier
                                .width(columnWidths.getOrElse(index) { 60 }.dp)
                                .padding(horizontal = 6.dp),
                            style = TextStyle(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = GlassColors.accent,
                                lineHeight = 14.sp
                            ),
                            maxLines = 2
                        )
                    }
                }
                
                // Data rows
                dataRows.forEachIndexed { rowIndex, row ->
                    val isEven = rowIndex % 2 == 0
                    Row(
                        modifier = Modifier
                            .background(
                                if (isEven) Color.Transparent 
                                else Color(0xFF1A1F30).copy(alpha = 0.5f)
                            )
                            .padding(vertical = 6.dp)
                    ) {
                        // Используем те же ширины что и для header!
                        headers.indices.forEach { colIndex ->
                            val cell = row.getOrElse(colIndex) { "" }
                            Text(
                                text = cell,
                                modifier = Modifier
                                    .width(columnWidths.getOrElse(colIndex) { 60 }.dp)
                                    .padding(horizontal = 6.dp),
                                style = TextStyle(
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = Color.White.copy(alpha = 0.9f),
                                    lineHeight = 14.sp
                                ),
                                maxLines = 2
                            )
                        }
                    }
                }
            }
            
            // Индикатор скролла справа
            if (showScrollHint) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxHeight()
                        .width(24.dp)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color(0xFF171B2C).copy(alpha = 0.9f)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "›",
                        color = GlassColors.accent.copy(alpha = 0.7f),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * Карточка для строки таблицы (компактный вид)
 */
@Composable
private fun TableRowCard(headers: List<String>, row: List<String>) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        Color(0xFF1E2540).copy(alpha = 0.8f),
                        Color(0xFF1A2035).copy(alpha = 0.6f)
                    )
                )
            )
            .border(1.dp, Color(0xFF3D4A6A).copy(alpha = 0.3f), RoundedCornerShape(8.dp))
            .padding(10.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            row.forEachIndexed { index, cell ->
                if (cell.isNotBlank()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Label
                        Text(
                            text = headers.getOrElse(index) { "" },
                            style = TextStyle(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                color = GlassColors.accent.copy(alpha = 0.8f)
                            ),
                            modifier = Modifier.weight(0.4f)
                        )
                        // Value
                        Text(
                            text = cell,
                            style = TextStyle(
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Normal,
                                color = Color.White
                            ),
                            modifier = Modifier.weight(0.6f)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Typing Indicator V2 — компактный, с выравниванием TOP
 */
@Composable
fun TypingIndicatorV2(
    modifier: Modifier = Modifier,
    isUploading: Boolean = false
) {
    val infiniteTransition = rememberInfiniteTransition(label = "typing")
    
    val bubbleShape = RoundedCornerShape(topStart = 6.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 18.dp)
    
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.Top  // TOP! Как у сообщений
    ) {
        BlueberryAvatarV2(size = 28.dp)
        Spacer(Modifier.width(6.dp))
        
        Box(
            modifier = Modifier
                .shadow(2.dp, bubbleShape, spotColor = Color.Black.copy(alpha = 0.2f))
                .clip(bubbleShape)
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            GlassColors.surfaceAlt.copy(alpha = 0.95f),
                            GlassColors.surface.copy(alpha = 0.9f)
                        )
                    ),
                    bubbleShape
                )
                .border(1.dp, Color(0xFF4A5580).copy(alpha = 0.4f), bubbleShape)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            if (isUploading) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(10.dp),
                        strokeWidth = 1.5.dp,
                        color = GlassColors.mint
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Загрузка...", style = GlassTypography.timestamp.copy(color = GlassColors.textSecondary))
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    repeat(3) { index ->
                        val dotAlpha by infiniteTransition.animateFloat(
                            initialValue = 0.3f, targetValue = 1f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(400, delayMillis = index * 120, easing = EaseInOutCubic),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "dot_$index"
                        )
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(GlassColors.textSecondary.copy(alpha = dotAlpha))
                        )
                    }
                }
            }
        }
    }
}

/**
 * Форматирование текста сообщения
 */
private fun formatMessageTextV2(raw: String): String {
    var text = raw.trim()
    if (text.isEmpty()) return text
    
    // Normalize bullets
    text = text.replace(Regex("(?m)^\\s*[-*•]\\s+"), "• ")
    
    // Remove markdown headings
    text = text.replace(Regex("(?m)^\\s*#{1,6}\\s+"), "")
    
    // Fix numbered lists
    text = text.replace(Regex("(?m)^(\\d+)\\.(\\S)"), "$1. $2")
    
    // Collapse multiple blank lines
    text = text.replace(Regex("(\\n\\s*){3,}"), "\n\n")
    
    // Remove markdown emphasis
    text = text.replace("*", "").replace("_", "")
    
    return text.trim()
}

/**
 * Карточка для сгенерированного изображения в чате
 * По клику открывается fullscreen preview с zoom/pan
 */
@Composable
private fun GeneratedImageCard(
    imageUrl: String,
    authToken: String? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showPreview by remember { mutableStateOf(false) }
    
    // Construct full URL if relative path
    val fullUrl = remember(imageUrl) {
        when {
            imageUrl.startsWith("http://") || imageUrl.startsWith("https://") -> imageUrl
            imageUrl.startsWith("/") -> "$API_HOST$imageUrl"
            else -> "$API_HOST/$imageUrl"
        }.also {
            Timber.d("GeneratedImageCard: Original URL=$imageUrl, Full URL=$it")
        }
    }
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.Transparent)
            .clickable { showPreview = true }
    ) {
        // Картинка со скруглёнными углами — с правильным кэшированием
        SubcomposeAsyncImage(
            model = ImageRequest.Builder(context)
                .data(fullUrl)
                .crossfade(300)
                .memoryCacheKey(fullUrl)
                .diskCacheKey(fullUrl)
                .apply {
                    if (!authToken.isNullOrBlank()) {
                        setHeader("Authorization", "Bearer $authToken")
                    }
                }
                .build(),
            contentDescription = "Сгенерированное изображение",
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp)),
            contentScale = ContentScale.FillWidth,
            loading = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF1A1F3A))
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center).size(32.dp),
                        color = GlassColors.accent,
                        strokeWidth = 2.dp
                    )
                }
            },
            error = {
                // Показываем placeholder вместо ошибки — изображение скорее всего загружается
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF1A1F3A)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.ZoomIn,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Нажми для просмотра",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 12.sp
                        )
                    }
                }
            }
        )
        
        // Иконка zoom — показываем что можно увеличить
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(8.dp)
                .size(32.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.ZoomIn,
                contentDescription = "Увеличить",
                tint = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
    
    // 🔍 Fullscreen Preview Dialog с zoom/pan
    if (showPreview) {
        GeneratedImagePreviewDialog(
            imageUrl = fullUrl,
            authToken = authToken,
            onDismiss = { showPreview = false }
        )
    }
}

/**
 * 🔍 Fullscreen просмотр сгенерированного изображения
 * С zoom/pan и кнопкой скачивания
 */
@Composable
private fun GeneratedImagePreviewDialog(
    imageUrl: String,
    authToken: String?,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    
    // Анимация появления
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
    
    // Zoom/Pan state
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var imageSize by remember { mutableStateOf(IntSize.Zero) }
    
    val transformState = rememberTransformableState { zoomChange, panChange, _ ->
        val newScale = (scale * zoomChange).coerceIn(1f, 5f)
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
                .background(Color.Black.copy(alpha = 0.85f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                )
                .systemBarsPadding(),
            contentAlignment = Alignment.Center
        ) {
            // Изображение с zoom/pan
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(context)
                    .data(imageUrl)
                    .crossfade(150)
                    .memoryCacheKey(imageUrl)
                    .diskCacheKey(imageUrl)
                    .apply {
                        if (!authToken.isNullOrBlank()) {
                            setHeader("Authorization", "Bearer $authToken")
                        }
                    }
                    .build(),
                contentDescription = "Просмотр изображения",
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
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        // Double-tap to reset zoom
                        val now = System.currentTimeMillis()
                        if (now - lastTapTime < 300) {
                            scale = if (scale > 1.5f) 1f else 2.5f
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
            
            // Кнопки внизу: скачать и закрыть
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 40.dp)
                    .graphicsLayer { alpha = animatedAlpha * 0.9f },
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Скачать
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF10B981).copy(alpha = 0.9f))
                        .clickable { downloadImage(context, imageUrl, authToken) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Download,
                        contentDescription = "Скачать",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
                
                // Закрыть
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.15f))
                        .clickable { onDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Закрыть",
                        tint = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            
            // Подсказка zoom
            if (scale == 1f) {
                Text(
                    text = "Двойной тап для увеличения",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 12.sp,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 16.dp)
                )
            }
        }
    }
}

/**
 * Скачивает изображение в галерею
 * Файл сохраняется в Pictures и появляется в галерее Android
 */
private fun downloadImage(context: android.content.Context, url: String, authToken: String?) {
    try {
        val fileName = "AI_Image_${System.currentTimeMillis()}.png"
        
        val request = android.app.DownloadManager.Request(android.net.Uri.parse(url))
            .setTitle("Сгенерированное изображение")
            .setDescription("Сохранение в галерею...")
            .setNotificationVisibility(android.app.DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(
                android.os.Environment.DIRECTORY_PICTURES,
                fileName
            )
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)
            .setMimeType("image/png")  // Важно для галереи!
        
        // Добавляем авторизацию если есть
        if (!authToken.isNullOrBlank()) {
            request.addRequestHeader("Authorization", "Bearer $authToken")
        }
        
        val downloadManager = context.getSystemService(android.content.Context.DOWNLOAD_SERVICE) as android.app.DownloadManager
        downloadManager.enqueue(request)
        
        android.widget.Toast.makeText(context, "💾 Сохраняю в галерею...", android.widget.Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        Timber.e(e, "Failed to download image")
        android.widget.Toast.makeText(context, "Ошибка сохранения", android.widget.Toast.LENGTH_SHORT).show()
    }
}

// ImageLoadingAnimation удалена - используем ImageGeneratingAnimation вместо неё

/**
 * Анимация трансформации для Image-to-Image
 * Показывает исходное(ые) фото с эффектами AI-обработки
 * Плавная и медленная анимация
 */
@Composable
fun ImageToImageAnimation(
    sourceImageUris: List<String>,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "i2i")
    val context = LocalContext.current
    
    // Волна shimmer - плавная туда-обратно (без скачков)
    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse  // Туда-обратно = плавно
        ),
        label = "shimmer"
    )
    
    // Пульсация рамки - плавная (3 секунды)
    val borderPulse by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "borderPulse"
    )
    
    // Мягкое свечение - очень медленное (5 секунд)
    val glowPulse by infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(5000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )
    
    // Dots progress - медленнее (2.5 секунды)
    val dotsProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "dots"
    )
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(if (sourceImageUris.size > 1) 1.5f else 1f)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF0A0E14))
    ) {
        // Одно или несколько изображений
        if (sourceImageUris.size == 1) {
            // Одно изображение - с мягкими эффектами
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                // Исходное изображение - стабильное, без дёрганья
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(Uri.parse(sourceImageUris.first()))
                        .crossfade(300)
                        .build(),
                    contentDescription = "Source image",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                
                // Мягкое затемнение для контраста
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.15f))
                )
                
                // Плавный shimmer overlay - горизонтальная волна света
                androidx.compose.foundation.Canvas(
                    modifier = Modifier.fillMaxSize()
                ) {
                    val width = size.width
                    val height = size.height
                    // Горизонтальная полоса движется слева направо и обратно
                    val stripWidth = width * 0.4f
                    val position = shimmerOffset * (width + stripWidth) - stripWidth / 2
                    
                    drawRect(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.White.copy(alpha = 0.12f),
                                GlassColors.accentSecondary.copy(alpha = 0.18f),
                                GlassColors.accent.copy(alpha = 0.15f),
                                Color.White.copy(alpha = 0.12f),
                                Color.Transparent
                            ),
                            start = androidx.compose.ui.geometry.Offset(position - stripWidth, 0f),
                            end = androidx.compose.ui.geometry.Offset(position + stripWidth, height)
                        ),
                        size = size
                    )
                }
                
                // Мягкое свечение по краям вместо scan line
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    GlassColors.accentSecondary.copy(alpha = glowPulse)
                                ),
                                radius = 800f
                            )
                        )
                )
                
                // Пульсирующая рамка - плавная
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .border(
                            width = 2.dp,
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    GlassColors.accent.copy(alpha = borderPulse),
                                    GlassColors.accentSecondary.copy(alpha = borderPulse * 0.6f),
                                    Color(0xFFEC4899).copy(alpha = borderPulse * 0.8f),
                                    GlassColors.accentSecondary.copy(alpha = borderPulse * 0.6f),
                                    GlassColors.accent.copy(alpha = borderPulse)
                                )
                            ),
                            shape = RoundedCornerShape(16.dp)
                        )
                )
            }
        } else {
            // Несколько изображений - сетка 2xN
            val columns = 2
            val rows = (sourceImageUris.size + 1) / 2
            
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                for (row in 0 until rows) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        for (col in 0 until columns) {
                            val index = row * columns + col
                            if (index < sourceImageUris.size) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(8.dp))
                                ) {
                                    // Изображение
                                    AsyncImage(
                                        model = ImageRequest.Builder(context)
                                            .data(Uri.parse(sourceImageUris[index]))
                                            .crossfade(300)
                                            .build(),
                                        contentDescription = "Source image ${index + 1}",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    
                                    // Мягкое затемнение
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.Black.copy(alpha = 0.1f))
                                    )
                                    
                                    // Individual shimmer - со смещением по времени
                                    val offsetMultiplier = (index * 0.3f)
                                    val adjustedShimmer = ((shimmerOffset + offsetMultiplier) % 1.6f) - 0.3f
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(
                                                Brush.linearGradient(
                                                    colorStops = arrayOf(
                                                        0f to Color.Transparent,
                                                        (adjustedShimmer - 0.1f).coerceIn(0f, 1f) to Color.Transparent,
                                                        adjustedShimmer.coerceIn(0f, 1f) to Color.White.copy(alpha = 0.2f),
                                                        (adjustedShimmer + 0.1f).coerceIn(0f, 1f) to Color.Transparent,
                                                        1f to Color.Transparent
                                                    )
                                                )
                                            )
                                    )
                                    
                                    // Пульсирующая рамка
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .border(
                                                width = 1.5.dp,
                                                color = GlassColors.accentSecondary.copy(alpha = borderPulse * 0.7f),
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                    )
                                }
                            } else {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
        
        // Текст статуса снизу - с плавным градиентом
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0xFF0A0E14).copy(alpha = 0.85f)
                        )
                    )
                )
                .padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "✨",
                    fontSize = 14.sp
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = if (sourceImageUris.size == 1) "Трансформирую изображение" 
                           else "Обрабатываю ${sourceImageUris.size} изображений",
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            
            Spacer(Modifier.height(8.dp))
            
            // Анимированные точки - плавнее
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                repeat(3) { index ->
                    val dotAlpha = when {
                        dotsProgress < 0.33f -> if (index == 0) 0.9f else 0.25f
                        dotsProgress < 0.66f -> if (index <= 1) 0.9f else 0.25f
                        else -> 0.9f
                    }
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(
                                GlassColors.accentSecondary.copy(alpha = dotAlpha),
                                CircleShape
                            )
                    )
                }
            }
        }
    }
}

/**
 * Красивая анимация генерации изображения
 * Минималистичный дизайн с вращающимся кольцом
 */
@Composable
fun ImageGeneratingAnimation(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "imageGen")
    
    // Shimmer движение - медленнее
    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer"
    )
    
    // Пульсация - в 3 раза медленнее (800 -> 2400)
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )
    
    // Вращение градиента - медленное
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )
    
    // Прогресс для точек
    val dotsProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "dots"
    )
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(16.dp))
            .graphicsLayer { scaleX = pulse; scaleY = pulse }
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF0F1117),
                        Color(0xFF1A1F2E),
                        Color(0xFF0F1117)
                    ),
                    start = androidx.compose.ui.geometry.Offset(
                        shimmerOffset * 500f,
                        shimmerOffset * 500f
                    ),
                    end = androidx.compose.ui.geometry.Offset(
                        (shimmerOffset + 1f) * 500f,
                        (shimmerOffset + 1f) * 500f
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Вращающееся кольцо градиента
        Box(
            modifier = Modifier
                .size(100.dp)
                .graphicsLayer { rotationZ = rotation }
                .background(
                    Brush.sweepGradient(
                        colors = listOf(
                            GlassColors.accent.copy(alpha = 0.6f),
                            GlassColors.accentSecondary.copy(alpha = 0.1f),
                            Color(0xFFEC4899).copy(alpha = 0.4f),
                            GlassColors.accent.copy(alpha = 0.1f),
                            GlassColors.accent.copy(alpha = 0.6f)
                        )
                    ),
                    CircleShape
                )
        )
        
        // Внутренний круг - чистый, без эмодзи
        Box(
            modifier = Modifier
                .size(70.dp)
                .background(Color(0xFF0F1117), CircleShape)
        )
        
        // Текст снизу
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Создаю изображение",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
            
            Spacer(Modifier.height(8.dp))
            
            // Анимированные точки
            Row(
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                repeat(3) { index ->
                    val dotAlpha = when {
                        dotsProgress < 0.33f -> if (index == 0) 1f else 0.3f
                        dotsProgress < 0.66f -> if (index <= 1) 1f else 0.3f
                        else -> 1f
                    }
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .background(
                                GlassColors.accentSecondary.copy(alpha = dotAlpha),
                                CircleShape
                            )
                    )
                }
            }
        }
    }
}
