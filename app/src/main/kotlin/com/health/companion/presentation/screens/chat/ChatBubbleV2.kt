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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    val maxBubbleWidth = screenWidth * 0.82f
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
    
    val timestamp = remember(message.created_at) {
        try {
            val dateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            dateFormat.format(Date(message.created_at?.toLongOrNull() ?: System.currentTimeMillis()))
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
                    BlueberryAvatarV2(size = 28.dp)
                    Spacer(Modifier.width(6.dp))
                } else {
                    Spacer(Modifier.width(34.dp))
                }
                
                // Проверяем есть ли картинка (сгенерированная или загруженная)
                val hasGeneratedImage = message.imageUrl != null
                val hasUploadedImages = !message.images.isNullOrEmpty()
                val hasText = message.content.isNotBlank()
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
                                            Color(0xFF2A3352).copy(alpha = 0.95f),
                                            Color(0xFF1E2744).copy(alpha = 0.9f)
                                        )
                                    ),
                                    bubbleShape
                                )
                                .border(1.dp, Color(0xFF4A5580).copy(alpha = 0.4f), bubbleShape)
                                .padding(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            Column {
                                if (message.agent_name != null && 
                                    message.agent_name !in listOf("chat", "offline", "streaming", "system") &&
                                    isFirstInGroup && !hasGeneratedImage && !hasUploadedImages) {
                                    Text(
                                        text = message.agent_name,
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
                // Bubble с салатовым/мятным градиентом
                Box(
                    modifier = Modifier
                        .widthIn(max = maxBubbleWidth)
                        .shadow(2.dp, bubbleShape, spotColor = Color.Black.copy(alpha = 0.25f))
                        .clip(bubbleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF00C896),  // Мятный/салатовый
                                    Color(0xFF00A67E)   // Тёмнее
                                )
                            ),
                            bubbleShape
                        )
                        .border(1.dp, Color(0xFF00E0A8).copy(alpha = 0.3f), bubbleShape)
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = formattedText,
                        style = GlassTypography.messageText.copy(fontSize = 14.sp),
                        color = Color.White
                    )
                }
                // БЕЗ аватара юзера!
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
                
                if (isUser && status != null) {
                    Spacer(Modifier.width(3.dp))
                    when (status) {
                        MessageSendStatus.Sending -> Text("○", style = GlassTypography.timestamp.copy(fontSize = 10.sp))
                        MessageSendStatus.Sent -> Text("✓✓", style = GlassTypography.timestamp.copy(fontSize = 10.sp, color = GlassColors.mint))
                        MessageSendStatus.Failed -> Text("✗", style = GlassTypography.timestamp.copy(fontSize = 10.sp, color = GlassColors.error))
                    }
                }
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
                    // Код блок со скроллом
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF1A1B26))
                            .horizontalScroll(rememberScrollState())
                            .padding(10.dp)
                    ) {
                        Text(
                            text = block.code,
                            style = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                color = Color(0xFF9ECE6A),
                                lineHeight = 16.sp
                            )
                        )
                    }
                }
                is MarkdownBlock.Table -> {
                    // Таблица
                    TableRenderer(block.rows)
                }
                is MarkdownBlock.Text -> {
                    // Обычный текст с inline форматированием
                    Text(
                        text = parseInlineMarkdown(block.text),
                        style = GlassTypography.messageText.copy(
                            fontSize = 14.sp,
                            lineHeight = 20.sp
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
}

private fun parseMarkdownBlocks(text: String): List<MarkdownBlock> {
    val blocks = mutableListOf<MarkdownBlock>()
    var remaining = text.trim()
    
    while (remaining.isNotEmpty()) {
        // Код блок
        val codeMatch = Regex("```(\\w*)\\n([\\s\\S]*?)```").find(remaining)
        if (codeMatch != null && codeMatch.range.first == 0) {
            blocks.add(MarkdownBlock.CodeBlock(
                codeMatch.groupValues[1],
                codeMatch.groupValues[2].trim()
            ))
            remaining = remaining.substring(codeMatch.range.last + 1).trim()
            continue
        }
        
        // Таблица (строки с |)
        val tableMatch = Regex("^(\\|[^\\n]+\\|\\n)+").find(remaining)
        if (tableMatch != null && tableMatch.range.first == 0) {
            val tableText = tableMatch.value
            val rows = tableText.lines()
                .filter { it.isNotBlank() && !it.contains("---") }
                .map { row ->
                    row.split("|")
                        .map { it.trim() }
                        .filter { it.isNotEmpty() }
                }
            if (rows.isNotEmpty()) {
                blocks.add(MarkdownBlock.Table(rows))
            }
            remaining = remaining.substring(tableMatch.range.last + 1).trim()
            continue
        }
        
        // Обычный текст до следующего блока
        val nextBlockStart = listOf(
            remaining.indexOf("```"),
            remaining.indexOf("\n|")
        ).filter { it > 0 }.minOrNull() ?: remaining.length
        
        val textPart = remaining.substring(0, nextBlockStart).trim()
        if (textPart.isNotEmpty()) {
            blocks.add(MarkdownBlock.Text(textPart))
        }
        remaining = remaining.substring(nextBlockStart).trim()
    }
    
    return blocks.ifEmpty { listOf(MarkdownBlock.Text(text)) }
}

private fun parseInlineMarkdown(text: String): AnnotatedString {
    return buildAnnotatedString {
        var current = text
            .replace("**", "⬛BOLD⬛")
            .replace("*", "⬛ITALIC⬛")
            .replace("`", "⬛CODE⬛")
            .replace("• ", "  • ")
            .replace(Regex("^#{1,3}\\s+", RegexOption.MULTILINE), "")
        
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

@Composable
private fun TableRenderer(rows: List<List<String>>) {
    if (rows.isEmpty()) return
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF1E2030))
            .border(1.dp, Color(0xFF3D4560), RoundedCornerShape(8.dp))
    ) {
        rows.forEachIndexed { rowIndex, row ->
            val isHeader = rowIndex == 0
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (isHeader) Modifier.background(Color(0xFF2A3050))
                        else Modifier
                    )
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                row.forEach { cell ->
                    Text(
                        text = cell,
                        modifier = Modifier.weight(1f),
                        style = TextStyle(
                            fontSize = 12.sp,
                            fontWeight = if (isHeader) FontWeight.Bold else FontWeight.Normal,
                            color = if (isHeader) GlassColors.accent else Color.White.copy(alpha = 0.9f)
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (rowIndex < rows.size - 1) {
                Divider(color = Color(0xFF3D4560).copy(alpha = 0.5f), thickness = 0.5.dp)
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
                            Color(0xFF2A3352).copy(alpha = 0.95f),
                            Color(0xFF1E2744).copy(alpha = 0.9f)
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
 * Без подкладки - края фото = края блока
 * С кнопкой скачивания
 */
@Composable
private fun GeneratedImageCard(
    imageUrl: String,
    authToken: String? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showDownloadButton by remember { mutableStateOf(false) }
    
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
            .clickable { showDownloadButton = !showDownloadButton }
    ) {
        // Картинка со скруглёнными углами, без фона
        SubcomposeAsyncImage(
            model = ImageRequest.Builder(context)
                .data(fullUrl)
                .crossfade(400)
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
                // Простой shimmer placeholder вместо анимации генерации
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF1A1F3A))
                ) {
                    // Минималистичный индикатор загрузки
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center).size(32.dp),
                        color = Color(0xFF6366F1),
                        strokeWidth = 2.dp
                    )
                }
            },
            error = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF1A1A2E)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("⚠️", fontSize = 28.sp)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Не удалось загрузить",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 12.sp
                        )
                    }
                }
            }
        )
        
        // Кнопка скачивания — появляется по тапу
        AnimatedVisibility(
            visible = showDownloadButton,
            modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.6f))
                    .clickable {
                        downloadImage(context, fullUrl, authToken)
                        showDownloadButton = false
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Download,
                    contentDescription = "Скачать",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
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
                                Color(0xFF8B5CF6).copy(alpha = 0.18f),
                                Color(0xFF6366F1).copy(alpha = 0.15f),
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
                                    Color(0xFF8B5CF6).copy(alpha = glowPulse)
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
                                    Color(0xFF6366F1).copy(alpha = borderPulse),
                                    Color(0xFF8B5CF6).copy(alpha = borderPulse * 0.6f),
                                    Color(0xFFEC4899).copy(alpha = borderPulse * 0.8f),
                                    Color(0xFF8B5CF6).copy(alpha = borderPulse * 0.6f),
                                    Color(0xFF6366F1).copy(alpha = borderPulse)
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
                                                color = Color(0xFF8B5CF6).copy(alpha = borderPulse * 0.7f),
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
                                Color(0xFF8B5CF6).copy(alpha = dotAlpha),
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
                            Color(0xFF6366F1).copy(alpha = 0.6f),
                            Color(0xFF8B5CF6).copy(alpha = 0.1f),
                            Color(0xFFEC4899).copy(alpha = 0.4f),
                            Color(0xFF6366F1).copy(alpha = 0.1f),
                            Color(0xFF6366F1).copy(alpha = 0.6f)
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
                                Color(0xFF8B5CF6).copy(alpha = dotAlpha),
                                CircleShape
                            )
                    )
                }
            }
        }
    }
}
