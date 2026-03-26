package com.health.companion.presentation.screens.chat

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.rememberScrollState

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.Canvas
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntSize
import androidx.compose.runtime.State

import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.health.companion.BuildConfig
import com.health.companion.data.remote.api.AgentStep
import com.health.companion.data.remote.api.EmotionEvent
import com.health.companion.data.remote.api.MessageDTO
import com.health.companion.presentation.components.*
import com.health.companion.presentation.theme.LocalAppTheme
import com.health.companion.presentation.theme.LocalChatBackground
import kotlinx.coroutines.launch
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

// API Host for constructing full URLs
private val API_HOST = BuildConfig.API_BASE_URL.substringBefore("/api/")

// Константный State<Float> = 0f для default-параметров ChatBubbleV2
private object ZeroFloatState : State<Float> {
    override val value: Float = 0f
}

// Compiled once at file-level — not recreated per composable
private val IMAGE_URL_REGEX = Regex("""!\[([^\]]*)\]\(([^)]+)\)""")

private val FILE_EXTENSIONS = "xlsx|xls|csv|pdf|doc|docx|ppt|pptx|zip|rar|7z|tar|txt|mp3|wav|ogg|m4a|mp4|mov|avi|mkv|webp"

// Matches markdown file links by LABEL extension: [report.xlsx](url)
private val FILE_LINK_BY_LABEL = Regex(
    """(?<!!)\[([^\]]+\.(?:$FILE_EXTENSIONS))\]\(([^)]+)\)""",
    RegexOption.IGNORE_CASE
)

// Matches markdown file links by URL extension: [любой текст](http://…/report.xlsx)
private val FILE_LINK_BY_URL = Regex(
    """(?<!!)\[([^\]]*)\]\(([^)]+\.(?:$FILE_EXTENSIONS)(?:\?[^)]*)?)\)""",
    RegexOption.IGNORE_CASE
)

// Matches links to /api/v1/files/... paths by any label (label may have no extension)
private val FILE_LINK_BY_PATH = Regex(
    """(?<!!)\[([^\]]+)\]\((/(?:api/v1/)?files/[^)]+)\)""",
    RegexOption.IGNORE_CASE
)

private val TOOL_CALL_BLOCK = Regex("""\[TOOL_CALL:[^\]]*\][\s\S]*?(?=\n\n|\z)""")
private val CODE_LINE = Regex("""^(?:\w+\.\w+[\s(=]|---\s.+\s---|print\(|output_path\s*=|file_path:|file_name:|wb\.|ws\.|import |from |def |class ).*$""", RegexOption.MULTILINE)

private fun stripToolOutput(content: String): String {
    var result = TOOL_CALL_BLOCK.replace(content, "")
    val lines = result.lines()
    val cleaned = mutableListOf<String>()
    var codeStreak = 0
    for (line in lines) {
        if (CODE_LINE.containsMatchIn(line)) {
            codeStreak++
            continue
        }
        if (codeStreak >= 2 && line.isBlank()) continue
        codeStreak = 0
        cleaned.add(line)
    }
    return cleaned.joinToString("\n")
        .replace(Regex("""\n{3,}"""), "\n\n")
        .trim()
}

/**
 * Strips a trailing incomplete markdown link from streaming content.
 * Handles: `[text`, `[text]`, `[text](`, `[text](url-partial`
 * Does NOT strip complete links `[text](url)` — those are handled by extractFileLinksFromContent.
 */
private fun stripTrailingPartialLink(content: String): String {
    val lastBracket = content.lastIndexOf('[')
    if (lastBracket == -1) return content
    val tail = content.substring(lastBracket)
    if (tail.contains(Regex("""\[[^\]]*\]\([^)]*\)"""))) return content
    val stripFrom = if (lastBracket > 0 && content[lastBracket - 1] == '!') lastBracket - 1 else lastBracket
    return content.substring(0, stripFrom).trimEnd()
}

/**
 * Extracts ALL file markdown links from content:
 *  - by label extension (e.g. [report.xlsx](url))
 *  - by URL extension  (e.g. [Скачать файл](http://…/data.xlsx))
 *  - by /files/ path   (e.g. [Excel](​/api/v1/files/generated/…))
 * Returns extracted GeneratedFile list and content with those links stripped.
 */
private fun extractFileLinksFromContent(
    content: String,
    baseUrl: String
): Pair<List<com.health.companion.data.remote.api.GeneratedFile>, String> {
    val files = mutableListOf<com.health.companion.data.remote.api.GeneratedFile>()
    val seen = mutableSetOf<String>()

    fun addFile(name: String, rawUrl: String) {
        val fullUrl = if (rawUrl.startsWith("http")) rawUrl else "$baseUrl$rawUrl"
        if (seen.add(fullUrl)) {
            // Derive a good filename: prefer label if it has extension, else use URL filename
            val displayName = if (name.contains(".")) name else {
                val urlName = fullUrl.substringAfterLast("/").substringBefore("?")
                if (urlName.contains(".")) urlName else name
            }
            files.add(com.health.companion.data.remote.api.GeneratedFile(url = fullUrl, name = displayName))
        }
    }

    // Collect all matches from all patterns, track positions to avoid double-replacing
    data class Match(val range: IntRange, val name: String, val url: String)
    val allMatches = mutableListOf<Match>()

    FILE_LINK_BY_LABEL.findAll(content).forEach { m ->
        allMatches.add(Match(m.range, m.groupValues[1], m.groupValues[2]))
    }
    FILE_LINK_BY_URL.findAll(content).forEach { m ->
        if (allMatches.none { it.range.first == m.range.first }) {
            allMatches.add(Match(m.range, m.groupValues[1], m.groupValues[2]))
        }
    }
    FILE_LINK_BY_PATH.findAll(content).forEach { m ->
        if (allMatches.none { it.range.first == m.range.first }) {
            allMatches.add(Match(m.range, m.groupValues[1], m.groupValues[2]))
        }
    }

    allMatches.sortedByDescending { it.range.first }.forEach { addFile(it.name, it.url) }

    // Strip all matched ranges from content (replace with empty, back-to-front)
    val sb = StringBuilder(content)
    allMatches.sortedByDescending { it.range.first }.forEach { m ->
        sb.replace(m.range.first, m.range.last + 1, "")
    }

    return Pair(files, sb.toString().trim())
}

/**
 * Returns true if the text contains markdown syntax that requires rich rendering.
 * Plain messages ("Привет! Как дела?") use Text which wraps to content width like Telegram.
 * Complex messages (headers, code, bold, lists, links) use GlassMarkdown.
 */
private fun String.hasMarkdownSyntax(): Boolean =
    contains("```") || contains("# ") || contains("## ") ||
    contains("**") || contains("__") || contains("`") ||
    contains("](") || contains("> ") || contains("|") ||
    startsWith("- ") || contains("\n- ") ||
    startsWith("* ") || contains("\n* ") ||
    startsWith("• ") || contains("\n• ") ||
    (length >= 3 && this[0].isDigit() && this[1] == '.' && this[2] == ' ')

// ============================================================================
// 💜 EMOTION SYSTEM — Color glow + Badge on avatar
// ============================================================================

/**
 * Maps emotion name → glow Color (soft tint around AI bubble)
 */
private fun emotionColor(emotion: String): Color = when (emotion) {
    "happy"      -> Color(0xFF4ADE80)  // Green
    "sad"        -> Color(0xFF60A5FA)  // Blue
    "anxious"    -> Color(0xFFFBBF24)  // Yellow
    "frustrated" -> Color(0xFFFF9F43)  // Orange
    "excited"    -> Color(0xFFC084FC)  // Purple
    "stressed"   -> Color(0xFFF87171)  // Red
    "curious"    -> Color(0xFF22D3EE)  // Cyan
    else         -> Color.Transparent   // neutral — no glow
}

/**
 * Maps emotion name → small emoji for avatar badge
 */
private fun emotionEmoji(emotion: String): String? = when (emotion) {
    "happy"      -> "😊"
    "sad"        -> "😢"
    "anxious"    -> "😰"
    "frustrated" -> "😤"
    "excited"    -> "🤩"
    "stressed"   -> "😣"
    "curious"    -> "🤔"
    else         -> null  // neutral — no badge
}

// ============================================================================
// Message segment types for splitting content with inline images
// ============================================================================

private sealed class MessageSegment {
    data class Text(val content: String) : MessageSegment()
    data class Image(val url: String, val alt: String) : MessageSegment()
}

private val IMAGE_KEYWORDS = listOf(
    "изображение", "картинк", "фото", "рисун", "генерир", "сгенерир",
    "image", "picture", "photo", "generated", "drawing", "illustration"
)

private fun isImageDescription(section: String): Boolean {
    val lower = section.lowercase()
    return IMAGE_KEYWORDS.any { lower.contains(it) }
}

private fun splitTextBySections(text: String): List<String> {
    val headerRegex = Regex("""(?=\n#{1,3}\s)""")
    val parts = text.split(headerRegex).map { it.trim() }.filter { it.isNotBlank() }
    return if (parts.isEmpty() && text.isNotBlank()) listOf(text) else parts
}

private fun splitIntoSegments(content: String, imageRegex: Regex): List<MessageSegment> {
    val segments = mutableListOf<MessageSegment>()
    var remaining = content

    while (remaining.isNotEmpty()) {
        val match = imageRegex.find(remaining)
        if (match != null) {
            val before = remaining.substring(0, match.range.first).trim()
            if (before.isNotBlank()) {
                // Keep all text before the image as ONE segment — no header-based splitting
                segments.add(MessageSegment.Text(before))
            }

            val after = remaining.substring(match.range.last + 1).trim()
            // Filter out image-description sections but keep the rest as a single text block
            val keptText = splitTextBySections(after)
                .filter { !isImageDescription(it) }
                .joinToString("\n\n")

            segments.add(MessageSegment.Image(
                url = match.groupValues[2],
                alt = match.groupValues[1]
            ))
            if (keptText.isNotBlank()) segments.add(MessageSegment.Text(keptText))
            remaining = ""
        } else {
            // No inline images — entire remaining content is one text segment
            if (remaining.isNotBlank()) segments.add(MessageSegment.Text(remaining))
            break
        }
    }
    if (segments.isEmpty() && content.isNotBlank()) {
        segments.add(MessageSegment.Text(content))
    }
    return segments
}

// ============================================================================
// Telegram-style bubble layout: timestamp floats at bottom-right
// ============================================================================

@Composable
private fun ChatFlexBubble(
    modifier: Modifier = Modifier,
    timestamp: @Composable () -> Unit,
    content: @Composable () -> Unit
) {
    Layout(
        contents = listOf(content, timestamp),
        modifier = modifier
    ) { (contentMeasurables, tsMeasurables), constraints ->
        val tsPlaceable = tsMeasurables.firstOrNull()?.measure(Constraints())
        val gap = (6 * density).toInt()
        val tsW = tsPlaceable?.width ?: 0
        val tsH = tsPlaceable?.height ?: 0
        val hasTimestamp = tsW > 0
        val reservedForTs = if (hasTimestamp) tsW + gap else 0

        val contentConstraints = constraints.copy(
            minWidth = 0, minHeight = 0,
            maxWidth = (constraints.maxWidth - reservedForTs).coerceAtLeast(0)
        )
        val contentPlaceable = contentMeasurables.first().measure(contentConstraints)

        if (!hasTimestamp) {
            layout(contentPlaceable.width, contentPlaceable.height) {
                contentPlaceable.place(0, 0)
            }
        } else {
            val w = minOf(contentPlaceable.width + reservedForTs, constraints.maxWidth)
            val h = maxOf(contentPlaceable.height, tsH)
            layout(w, h) {
                contentPlaceable.place(0, 0)
                tsPlaceable!!.place(w - tsW, h - tsH)
            }
        }
    }
}

// ============================================================================
// ChatBubble V2
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
    emotion: EmotionEvent? = null,
    onRetry: () -> Unit = {},
    onDelete: () -> Unit = {},
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    onToggleSelect: () -> Unit = {},
    onLongPress: () -> Unit = {},
    // State-объекты для анимаций выбора — читаются только в graphicsLayer,
    // не вызывают перекомпозицию всех баблов при каждом кадре анимации
    selectionOffsetPxState: State<Float> = ZeroFloatState,
    selectionCircleAlphaState: State<Float> = ZeroFloatState,
    // Показывать timestamp (false = скрыть, если недавнее сообщение из той же группы)
    showTimestamp: Boolean = true,
    // Анимация распознавания на фото (пока AI анализирует последнее сообщение)
    isAnalyzing: Boolean = false,
    // Клик по фото для открытия просмотра
    onPhotoClick: ((String) -> Unit)? = null
) {
    val isUser = message.role == "user"
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val maxBubbleWidth = screenWidth * 0.88f
    // Компактный padding для коротких однострочных сообщений
    val isShortMessage = remember(message.content) {
        message.content.trim().length <= 80 && !message.content.contains('\n')
    }
    val bubbleVerticalPad = if (isShortMessage) 5.dp else 8.dp
    val bubbleVerticalPadBottom = if (isShortMessage) 5.dp else 7.dp
    val haptic = LocalHapticFeedback.current
    
    val formattedText = remember(message.content) { formatMessageTextV2(message.content) }
    
    val timestamp = remember(message.createdAt) {
        try {
            val dateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            dateFormat.format(Date(message.createdAt?.toLongOrNull() ?: System.currentTimeMillis()))
        } catch (e: Exception) { "" }
    }
    
    // Telegram-style shapes from GlassShapes
    val bubbleShape = when {
        isUser && isFirstInGroup -> GlassShapes.userBubble
        !isUser && isFirstInGroup -> GlassShapes.assistantBubble
        else -> GlassShapes.continuedBubble
    }

    val chatBg = LocalChatBackground.current
    val assistantBubbleBrush = remember(chatBg) {
        Brush.linearGradient(listOf(
            chatBg.surfaceColor,
            chatBg.surfaceColor.copy(alpha = 0.92f)
        ))
    }
    val appTheme = LocalAppTheme.current
    val userBubbleBrush = remember(appTheme) {
        Brush.linearGradient(listOf(
            appTheme.userBubble.copy(alpha = 0.95f),
            appTheme.userBubbleDark.copy(alpha = 0.88f)
        ))
    }

    // Анимация подсветки выбранного сообщения
    val selectedBgAlpha by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0f,
        animationSpec = tween(180),
        label = "selectedBg"
    )

    // Per-bubble анимация галочки (bounce только для checkmark — это OK, значение 0..1)
    val checkScale by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium, dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "checkScale"
    )

    // Основной контейнер
    Box(
        modifier = modifier
            .fillMaxWidth()
            .let { mod ->
                if (isSelectionMode) {
                    mod.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onToggleSelect() }
                } else {
                    mod.combinedClickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {},
                        onLongClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onLongPress()
                        }
                    )
                }
            }
    ) {
        // Подсветка выбранного сообщения
        if (selectedBgAlpha > 0f) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(appTheme.primary.copy(alpha = 0.14f * selectedBgAlpha))
            )
        }
            // Основной контент
        Box(modifier = Modifier.fillMaxWidth()) {
            // Сообщение — плавно съезжает вправо при входе в режим выбора
            // graphicsLayer читает State.value без перекомпозиции (только redraw)
            Column(modifier = Modifier.fillMaxWidth().graphicsLayer {
                translationX = selectionOffsetPxState.value
            }) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
                verticalAlignment = Alignment.Top
            ) {
            // === ASSISTANT SIDE ===
            if (!isUser) {
                
                val hasGeneratedImage = message.imageUrl != null
                val hasUploadedImages = !message.images.isNullOrEmpty()
                val contentLower = message.content.lowercase()
                val isImagePlaceholder = 
                    message.content.startsWith("[Изображение:") || 
                    message.content.startsWith("[Image:") ||
                    message.content.startsWith("Создаю изображение") ||
                    message.content.startsWith("Генерирую изображение") ||
                    message.content.startsWith("Рисую") ||
                    contentLower.startsWith("generating image") ||
                    contentLower.startsWith("creating image")
                val context = LocalContext.current
                val baseUrl = remember { com.health.companion.BuildConfig.API_BASE_URL.substringBefore("/api/") }

                val isStreaming = animate
                val (contentFileLinks, cleanedContent) = remember(message.content, isStreaming) {
                    val stripped = stripToolOutput(message.content)
                    val (files, text) = extractFileLinksFromContent(stripped, baseUrl)
                    if (isStreaming) files to stripTrailingPartialLink(text) else files to text
                }
                // Merge: files from DB/SSE + files found in content text.
                // Deduplicate by URL (stripped of query params) AND by name — guards against
                // the case where message.files stores the label ("Скачать файл") while
                // extractFileLinksFromContent derives the filename from the URL ("report.xlsx"),
                // which caused two download buttons after app restart.
                val allFiles = remember(message.files, contentFileLinks) {
                    val base = message.files.orEmpty()
                    fun normUrl(url: String) = url.substringBefore("?").trimEnd('/')
                    val baseUrls  = base.map { normUrl(it.url) }.toSet()
                    val baseNames = base.map { it.name }.toSet()
                    val extra = contentFileLinks.filter { cf ->
                        normUrl(cf.url) !in baseUrls && cf.name !in baseNames
                    }
                    base + extra
                }

                // When imageUrl is present (from metadata), strip markdown image
                // references from the text to prevent double-rendering.
                val contentForSegments = remember(cleanedContent, hasGeneratedImage) {
                    if (hasGeneratedImage) {
                        IMAGE_URL_REGEX.replace(cleanedContent, "").trim()
                    } else {
                        cleanedContent
                    }
                }

                val segments = remember(contentForSegments) {
                    splitIntoSegments(contentForSegments, IMAGE_URL_REGEX)
                }
                val hasAnyText = segments.any { it is MessageSegment.Text }
                val showBubbleContent = contentForSegments.isNotBlank() && !isImagePlaceholder
                
                // Emotion glow for text bubbles (no animation — emotion changes are rare)
                val eColor = emotion?.let { emotionColor(it.emotion) } ?: Color.Transparent
                val borderColor = if (eColor != Color.Transparent) eColor.copy(alpha = 0.55f)
                                  else Color(0xFF4A5580).copy(alpha = 0.4f)
                
                Column(
                    modifier = Modifier.widthIn(max = maxBubbleWidth),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Uploaded images (system/context photos)
                    if (hasUploadedImages) {
                        message.images?.forEach { imageUri ->
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(Uri.parse(imageUri))
                                    .memoryCachePolicy(CachePolicy.ENABLED)
                                    .diskCachePolicy(CachePolicy.ENABLED)
                                    .build(),
                                contentDescription = null,
                                contentScale = ContentScale.FillWidth,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                            )
                        }
                    }
                    
                    // Generation animation while waiting
                    if (isImagePlaceholder && !hasGeneratedImage) {
                        ImageGeneratingAnimationCompact(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(16.dp))
                        )
                    }
                    
                    // Generated image from SSE imageUrl field
                    if (hasGeneratedImage) {
                        message.imageUrl?.let { imageUrl ->
                            GeneratedImageCard(
                                imageUrl = imageUrl,
                                authToken = authToken,
                                modifier = Modifier
                            )
                        }
                    }
                    
                    // Render content segments in order (skip Image segments when already rendered above)
                    if (showBubbleContent) {
                        val textSegments = segments.filter { it is MessageSegment.Text }
                        val renderSegments = if (hasGeneratedImage) textSegments else segments
                        val lastTextIndex = renderSegments.indexOfLast { it is MessageSegment.Text }
                        
                        renderSegments.forEachIndexed { idx, segment ->
                            when (segment) {
                                is MessageSegment.Image -> {
                                    GeneratedImageCard(
                                        imageUrl = segment.url,
                                        authToken = authToken,
                                        modifier = Modifier
                                    )
                                }
                                is MessageSegment.Text -> {
                                    val isLastText = idx == lastTextIndex
                                    ChatFlexBubble(
                                        modifier = Modifier
                                            .clip(bubbleShape)
                                            .background(assistantBubbleBrush, bubbleShape)
                                            .border(1.dp, borderColor, bubbleShape)
                                            .padding(start = 12.dp, end = 10.dp, top = bubbleVerticalPad, bottom = bubbleVerticalPadBottom),
                                        timestamp = {
                                            if (isLastText && showTimestamp && timestamp.isNotBlank()) {
                                                Text(
                                                    text = timestamp,
                                                    style = TextStyle(fontSize = 10.sp, color = GlassColors.textMuted)
                                                )
                                            }
                                        }
                                    ) {
                                        val isPlainText = remember(segment.content) {
                                            !segment.content.hasMarkdownSyntax()
                                        }
                                        Column {
                                            if (idx == 0 && message.agentName != null && 
                                                message.agentName !in listOf("chat", "offline", "streaming", "system") &&
                                                isFirstInGroup && !hasGeneratedImage && !hasUploadedImages) {
                                                Text(
                                                    text = message.agentName,
                                                    style = GlassTypography.timestamp.copy(
                                                        color = GlassColors.accent,
                                                        fontWeight = FontWeight.Medium
                                                    ),
                                                    modifier = Modifier.padding(bottom = 1.dp)
                                                )
                                            }
                                            if (isPlainText) {
                                                // Plain text → wraps to content width like Telegram
                                                Text(
                                                    text = segment.content,
                                                    style = GlassTypography.messageText,
                                                    color = GlassColors.textPrimary
                                                )
                                            } else {
                                                // Rich content → allow full bubble width
                                                GlassMarkdown(
                                                    content = segment.content,
                                                    authToken = authToken
                                                )
                                            }
                                        }
                                    }
                                }
                        }
                    }
                    }
                    val showFiles = !isStreaming && allFiles.isNotEmpty()
                    AnimatedVisibility(
                        visible = showFiles,
                        enter = fadeIn(tween(300)) + expandVertically(
                            animationSpec = tween(250),
                            expandFrom = Alignment.Top
                        ),
                    ) {
                        Column {
                            allFiles.forEach { file ->
                                FileDownloadButton(
                                    file = file,
                                    authToken = authToken,
                                    modifier = Modifier
                                        .widthIn(min = 200.dp, max = maxBubbleWidth)
                                )
                            }
                        }
                    }
                }
            }
            
            // === USER SIDE (с аватаром справа) ===
            if (isUser) {
                val context = LocalContext.current
                val hasUserImages = !message.images.isNullOrEmpty()
                
                val isPhotoPlaceholder = hasUserImages && (
                    message.content == "📷" ||
                    message.content.isBlank() ||
                    (message.content.startsWith("📷") && message.content.contains("фото") && message.content.contains("контекст"))
                )
                val hasCaption = message.content.isNotBlank() && !isPhotoPlaceholder

                Column(
                    modifier = Modifier.widthIn(max = maxBubbleWidth),
                    horizontalAlignment = Alignment.End
                ) {
                    when {
                        // ═══ ФОТО + ПОДПИСЬ — единый пузырь как у Telegram ═══
                        hasUserImages && hasCaption -> {
                            Column(
                                modifier = Modifier
                                    .widthIn(max = 268.dp)
                                    .clip(bubbleShape)
                                    .background(userBubbleBrush, bubbleShape)
                            ) {
                                // Фото edge-to-edge, скругления от внешнего clip
                                TelegramPhotoGrid(
                                    imageUris = message.images!!,
                                    modifier = Modifier.fillMaxWidth(),
                                    captionMode = true,
                                    isAnalyzing = isAnalyzing,
                                    onPhotoClick = onPhotoClick
                                )
                                // Подпись: текст слева, timestamp справа — как на скрине
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 12.dp, end = 10.dp, top = 7.dp, bottom = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Bottom
                                ) {
                                    Text(
                                        text = formattedText,
                                        style = GlassTypography.messageText,
                                        color = Color.White,
                                        modifier = Modifier
                                            .weight(1f, fill = false)
                                            .padding(end = 6.dp)
                                    )
                                    if (showTimestamp && timestamp.isNotBlank()) {
                                        Text(
                                            text = timestamp,
                                            style = TextStyle(
                                                fontSize = 10.sp,
                                                color = Color.White.copy(alpha = 0.6f)
                                            )
                                        )
                                    }
                                }
                            }
                        }

                        // ═══ ТОЛЬКО ФОТО — тайм-стамп поверх фото ═══
                        hasUserImages -> {
                            Box(contentAlignment = Alignment.BottomEnd) {
                                TelegramPhotoGrid(
                                    imageUris = message.images!!,
                                    isAnalyzing = isAnalyzing,
                                    onPhotoClick = onPhotoClick
                                )
                                if (isLastInGroup && showTimestamp && timestamp.isNotBlank()) {
                                    Box(
                                        modifier = Modifier
                                            .padding(6.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color.Black.copy(alpha = 0.45f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = timestamp,
                                            style = TextStyle(fontSize = 9.sp, color = Color.White)
                                        )
                                    }
                                }
                            }
                        }

                        // ═══ ТОЛЬКО ТЕКСТ ═══
                        hasCaption -> {
                            ChatFlexBubble(
                                modifier = Modifier
                                    .clip(bubbleShape)
                                    .background(userBubbleBrush, bubbleShape)
                                    .padding(start = 12.dp, end = 10.dp, top = bubbleVerticalPad, bottom = bubbleVerticalPadBottom),
                                timestamp = {
                                    if (showTimestamp && timestamp.isNotBlank()) {
                                        Text(
                                            text = timestamp,
                                            style = TextStyle(fontSize = 10.sp, color = Color.White.copy(alpha = 0.5f))
                                        )
                                    }
                                }
                            ) {
                                Text(
                                    text = formattedText,
                                    style = GlassTypography.messageText,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }

            }
        }
        
        // Timestamp теперь INLINE внутри bubble (Telegram-style)
        
        // === CITATIONS — скрыты по запросу ===
        
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
            }  // Close inner Column (sliding content)

            // Кружок выбора — всегда у верхнего края сообщения, чтобы не надо было
            // искать его в середине длинного блока
            if (isSelectionMode || checkScale > 0f) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = 5.dp, top = 6.dp)
                        .size(26.dp)
                        .graphicsLayer {
                            val circleAlpha = selectionCircleAlphaState.value
                            alpha = circleAlpha
                            scaleX = 0.5f + circleAlpha * 0.5f
                            scaleY = 0.5f + circleAlpha * 0.5f
                            transformOrigin = TransformOrigin(0f, 0f)
                        }
                        .clip(CircleShape)
                        .border(
                            2.dp,
                            if (isSelected) appTheme.primary else Color.White.copy(alpha = 0.55f),
                            CircleShape
                        )
                        .background(
                            if (isSelected) appTheme.primary else Color.White.copy(alpha = 0.08f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (checkScale > 0f) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier
                                .size(15.dp)
                                .graphicsLayer {
                                    scaleX = checkScale
                                    scaleY = checkScale
                                }
                        )
                    }
                }
            }
        }  // Close Box (selection)

    }  // Close Box wrapper
}  // Close ChatBubbleV2

/**
 * Blueberry Avatar V2 — компактный, с Emotion Badge
 */
@Composable
private fun BlueberryAvatarV2(size: Dp = 28.dp, emotion: EmotionEvent? = null) {
    val chatBg = LocalChatBackground.current
    val emoji = emotion?.let { emotionEmoji(it.emotion) }
    val glowColor = emotion?.let { emotionColor(it.emotion) } ?: Color.Transparent
    
    // Animated glow ring around avatar when emotion is present
    val glowAlpha by animateFloatAsState(
        targetValue = if (emoji != null) 0.6f else 0f,
        animationSpec = tween(600, easing = EaseOutCubic),
        label = "avatarGlow"
    )
    
    Box(contentAlignment = Alignment.Center) {
        // Main avatar circle
        Box(
            modifier = Modifier
                .size(size)
                .then(
                    if (glowAlpha > 0f) Modifier.border(
                        width = 1.5.dp,
                        color = glowColor.copy(alpha = glowAlpha),
                        shape = CircleShape
                    ) else Modifier
                )
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
        
        // Emotion badge — tiny emoji at bottom-right
        if (emoji != null) {
            val badgeScale by animateFloatAsState(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                ),
                label = "badgeScale"
            )
            
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 3.dp, y = 3.dp)
                    .scale(badgeScale)
                    .size((size.value * 0.52f).dp)
                    .clip(CircleShape)
                    .background(chatBg.surfaceColor)
                    .border(0.5.dp, glowColor.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = emoji,
                    fontSize = (size.value * 0.28f).sp
                )
            }
        }
    }
}

/**
 * User Avatar V2 — компактный аватар пользователя (мятный градиент + иконка)
 */
@Composable
private fun UserAvatarV2(size: Dp = 22.dp) {
    val theme = LocalAppTheme.current
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        theme.userBubble.copy(alpha = 0.9f),
                        theme.userBubbleDark.copy(alpha = 0.7f)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Person,
            contentDescription = "User",
            modifier = Modifier.size(size * 0.6f),
            tint = Color.White
        )
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
    val theme = LocalAppTheme.current
    val chatBg = LocalChatBackground.current
    
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
                .graphicsLayer {
                    shadowElevation = 4f
                    shape = bubbleShape
                    clip = true
                }
                .background(chatBg.surfaceColor, bubbleShape)
                .border(1.dp, Color.White.copy(alpha = 0.08f), bubbleShape)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            if (isUploading) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(10.dp),
                        strokeWidth = 1.5.dp,
                        color = theme.primary
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
    
    // Фиксированный размер контейнера — чтобы не было дёрганья при загрузке
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)  // Фиксированный квадрат — layout не меняется
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF0F1117))
            .clickable { showPreview = true }
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(fullUrl)
                .crossfade(200)
                .memoryCacheKey(fullUrl)
                .diskCacheKey(fullUrl)
                .placeholderMemoryCacheKey(fullUrl)
                .memoryCachePolicy(CachePolicy.ENABLED)
                .diskCachePolicy(CachePolicy.ENABLED)
                // Auth header is added automatically by CoilAuthInterceptor in the ImageLoader
                .build(),
            contentDescription = "Сгенерированное изображение",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
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
                    .placeholderMemoryCacheKey(imageUrl)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    // Auth header added automatically by CoilAuthInterceptor
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
                        .memoryCachePolicy(CachePolicy.ENABLED)
                        .diskCachePolicy(CachePolicy.ENABLED)
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
                                            .memoryCachePolicy(CachePolicy.ENABLED)
                                            .diskCachePolicy(CachePolicy.ENABLED)
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
 * Компактная быстрая анимация для карточки изображения
 * Показывается пока картинка загружается после генерации
 */
@Composable
private fun ImageGeneratingAnimationCompact(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "compact_gen")
    
    // Быстрое вращение кольца (2 секунды вместо 6)
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )
    
    // Shimmer - быстрый (1.5 сек)
    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = -0.5f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer"
    )
    
    // Пульсация - быстрая
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.97f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )
    
    Box(
        modifier = modifier
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
                .size(72.dp)
                .graphicsLayer { 
                    rotationZ = rotation
                    scaleX = pulse
                    scaleY = pulse
                }
                .background(
                    Brush.sweepGradient(
                        colors = listOf(
                            GlassColors.accent.copy(alpha = 0.7f),
                            GlassColors.accentSecondary.copy(alpha = 0.1f),
                            Color(0xFFEC4899).copy(alpha = 0.5f),
                            GlassColors.accent.copy(alpha = 0.1f),
                            GlassColors.accent.copy(alpha = 0.7f)
                        )
                    ),
                    CircleShape
                )
        )
        
        // Внутренний круг с эмодзи
        Box(
            modifier = Modifier
                .size(50.dp)
                .background(Color(0xFF0F1117), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "✨",
                fontSize = 20.sp
            )
        }
        
        // Текст снизу
        Text(
            text = "Создаю...",
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 20.dp)
        )
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

/**
 * Адаптивное фото — правильный авто-масштаб без OOM и без letterbox:
 *
 * - Загружается с ограничением 1280px (избегает OOM для 12МП фото)
 * - aspect ratio вычисляется из реальных размеров фото через onSuccess
 * - Портретные фото: натуральные пропорции без ограничения по высоте (скрины видны целиком)
 * - Ландшафтные фото: натуральные пропорции, max 2.4 (очень широкие обрезаются с боков)
 * - ContentScale.Crop + точный aspectRatio = никаких полос, скругления по рамке фото
 */
@Composable
private fun NaturalAspectPhoto(
    uri: String,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 0.dp,
    captionMode: Boolean = false,
    isAnalyzing: Boolean = false,
    onPhotoClick: (() -> Unit)? = null
) {
    val context = LocalContext.current

    // Плейсхолдер 4:3 (частый формат — скрины телефона, камера)
    var aspectRatio by remember(uri) { mutableFloatStateOf(4f / 3f) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(aspectRatio)
            .let { if (cornerRadius > 0.dp) it.clip(RoundedCornerShape(cornerRadius)) else it }
            .then(
                if (onPhotoClick != null) Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onPhotoClick
                ) else Modifier
            )
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(Uri.parse(uri))
                .memoryCachePolicy(CachePolicy.ENABLED)
                .diskCachePolicy(CachePolicy.ENABLED)
                .size(1280, 1280) // Max 1280px — не OOM, но чёткость сохраняется
                .build(),
            contentDescription = "Фото",
            contentScale = ContentScale.Crop,
            onSuccess = { result ->
                val sz = result.painter.intrinsicSize
                if (sz.width > 0f && sz.height > 0f) {
                    val natural = sz.width / sz.height
                    // Используем натуральные пропорции без нижней границы — иначе Crop
                    // обрезает портретные скриншоты (9:20 ≈ 0.45 < старый minRatio 0.67).
                    // Верхняя граница 2.4 сохраняется для очень широких фото.
                    aspectRatio = natural.coerceAtMost(2.4f)
                }
            },
            modifier = Modifier.fillMaxSize()
        )
        if (isAnalyzing) ScanAnimationOverlay()
    }
}

/**
 * Telegram-style photo grid — красивая сетка фото как в Telegram
 * - 1 фото: полная ширина, ЕСТЕСТВЕННЫЕ пропорции без обрезки (в т.ч. вертикальные)
 * - 2 фото: горизонтально 50/50
 * - 3 фото: 1 большое слева + 2 маленьких справа
 * - 4+ фото: 2x2 сетка
 * - Поддержка анимации распознавания (isAnalyzing) и клика для просмотра (onPhotoClick)
 */
@Composable
private fun TelegramPhotoGrid(
    imageUris: List<String>,
    modifier: Modifier = Modifier,
    // captionMode = true: фото edge-to-edge внутри пузыря (без внутреннего clip и max width)
    // captionMode = false: standalone фото с ограничением 260dp и скруглёнными углами
    captionMode: Boolean = false,
    isAnalyzing: Boolean = false,
    onPhotoClick: ((String) -> Unit)? = null
) {
    val cornerRadius = 16.dp
    val gap = 2.dp
    val gridMaxWidth = 260.dp

    Column(
        modifier = modifier
            .let { if (captionMode) it else it.widthIn(max = gridMaxWidth) }
            .let { if (captionMode) it else it.clip(RoundedCornerShape(cornerRadius)) }
    ) {
        when (imageUris.size) {
            // ═══ ОДНО ФОТО — адаптивные пропорции ═══
            1 -> {
                NaturalAspectPhoto(
                    uri = imageUris[0],
                    cornerRadius = if (captionMode) 0.dp else cornerRadius,
                    captionMode = captionMode,
                    isAnalyzing = isAnalyzing,
                    onPhotoClick = onPhotoClick?.let { { it(imageUris[0]) } }
                )
            }
            // ═══ 2 ФОТО — горизонтально 50/50 ═══
            2 -> {
                val photoHeight = if (captionMode) 240.dp else 220.dp
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(gap)
                ) {
                    imageUris.forEachIndexed { index, uri ->
                        GridPhoto(
                            uri = uri,
                            contentDescription = "Фото ${index + 1}",
                            isAnalyzing = isAnalyzing,
                            onClick = onPhotoClick?.let { { it(uri) } },
                            modifier = Modifier
                                .weight(1f)
                                .height(photoHeight)
                                .let {
                                    if (!captionMode) it.clip(RoundedCornerShape(
                                        topStart = if (index == 0) cornerRadius else 4.dp,
                                        bottomStart = if (index == 0) cornerRadius else 4.dp,
                                        topEnd = if (index == 1) cornerRadius else 4.dp,
                                        bottomEnd = if (index == 1) cornerRadius else 4.dp
                                    )) else it
                                }
                        )
                    }
                }
            }
            // ═══ 3 ФОТО: 1 большое слева + 2 справа ═══
            3 -> {
                val totalHeight = if (captionMode) 240.dp else 220.dp
                Row(
                    modifier = Modifier.fillMaxWidth().height(totalHeight),
                    horizontalArrangement = Arrangement.spacedBy(gap)
                ) {
                    GridPhoto(
                        uri = imageUris[0],
                        contentDescription = "Фото 1",
                        isAnalyzing = isAnalyzing,
                        onClick = onPhotoClick?.let { { it(imageUris[0]) } },
                        modifier = Modifier.weight(0.6f).fillMaxHeight()
                            .let {
                                if (!captionMode) it.clip(RoundedCornerShape(
                                    topStart = cornerRadius, bottomStart = cornerRadius,
                                    topEnd = 4.dp, bottomEnd = 4.dp
                                )) else it
                            }
                    )
                    Column(
                        modifier = Modifier.weight(0.4f).fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(gap)
                    ) {
                        listOf(1, 2).forEachIndexed { i, imgIdx ->
                            GridPhoto(
                                uri = imageUris[imgIdx],
                                contentDescription = "Фото ${imgIdx + 1}",
                                isAnalyzing = isAnalyzing,
                                onClick = onPhotoClick?.let { { it(imageUris[imgIdx]) } },
                                modifier = Modifier.fillMaxWidth().weight(1f)
                                    .let {
                                        if (!captionMode) it.clip(RoundedCornerShape(
                                            topStart = 4.dp,
                                            topEnd = if (i == 0) cornerRadius else 4.dp,
                                            bottomStart = 4.dp,
                                            bottomEnd = if (i == 1) cornerRadius else 4.dp
                                        )) else it
                                    }
                            )
                        }
                    }
                }
            }
            // ═══ 4 ФОТО — 2×2 ═══
            4 -> {
                val rowH = if (captionMode) 155.dp else 145.dp
                Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                    for (row in 0..1) {
                        Row(
                            modifier = Modifier.fillMaxWidth().height(rowH),
                            horizontalArrangement = Arrangement.spacedBy(gap)
                        ) {
                            for (col in 0..1) {
                                val idx = row * 2 + col
                                GridPhoto(
                                    uri = imageUris[idx],
                                    contentDescription = "Фото ${idx + 1}",
                                    isAnalyzing = isAnalyzing,
                                    onClick = onPhotoClick?.let { { it(imageUris[idx]) } },
                                    modifier = Modifier.weight(1f).fillMaxHeight()
                                        .let { if (!captionMode) it.clip(RoundedCornerShape(
                                            topStart = if (row == 0 && col == 0) cornerRadius else 4.dp,
                                            topEnd = if (row == 0 && col == 1) cornerRadius else 4.dp,
                                            bottomStart = if (row == 1 && col == 0) cornerRadius else 4.dp,
                                            bottomEnd = if (row == 1 && col == 1) cornerRadius else 4.dp
                                        )) else it }
                                )
                            }
                        }
                    }
                }
            }
            // ═══ 5 ФОТО — 2 сверху + 3 снизу ═══
            5 -> {
                val row1H = if (captionMode) 165.dp else 155.dp
                val row2H = if (captionMode) 130.dp else 120.dp
                Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().height(row1H),
                        horizontalArrangement = Arrangement.spacedBy(gap)
                    ) {
                        imageUris.take(2).forEachIndexed { col, uri ->
                            GridPhoto(
                                uri = uri,
                                contentDescription = "Фото ${col + 1}",
                                isAnalyzing = isAnalyzing,
                                onClick = onPhotoClick?.let { { it(uri) } },
                                modifier = Modifier.weight(1f).fillMaxHeight()
                                    .let { if (!captionMode) it.clip(RoundedCornerShape(
                                        topStart = if (col == 0) cornerRadius else 4.dp,
                                        topEnd = if (col == 1) cornerRadius else 4.dp,
                                        bottomStart = 4.dp, bottomEnd = 4.dp
                                    )) else it }
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().height(row2H),
                        horizontalArrangement = Arrangement.spacedBy(gap)
                    ) {
                        imageUris.drop(2).forEachIndexed { col, uri ->
                            GridPhoto(
                                uri = uri,
                                contentDescription = "Фото ${col + 3}",
                                isAnalyzing = isAnalyzing,
                                onClick = onPhotoClick?.let { { it(uri) } },
                                modifier = Modifier.weight(1f).fillMaxHeight()
                                    .let { if (!captionMode) it.clip(RoundedCornerShape(
                                        topStart = 4.dp, topEnd = 4.dp,
                                        bottomStart = if (col == 0) cornerRadius else 4.dp,
                                        bottomEnd = if (col == 2) cornerRadius else 4.dp
                                    )) else it }
                            )
                        }
                    }
                }
            }
            // ═══ 6 ФОТО — 3+3 ═══
            6 -> {
                val rowH = if (captionMode) 135.dp else 125.dp
                Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                    for (row in 0..1) {
                        Row(
                            modifier = Modifier.fillMaxWidth().height(rowH),
                            horizontalArrangement = Arrangement.spacedBy(gap)
                        ) {
                            for (col in 0..2) {
                                val idx = row * 3 + col
                                GridPhoto(
                                    uri = imageUris[idx],
                                    contentDescription = "Фото ${idx + 1}",
                                    isAnalyzing = isAnalyzing,
                                    onClick = onPhotoClick?.let { { it(imageUris[idx]) } },
                                    modifier = Modifier.weight(1f).fillMaxHeight()
                                        .let { if (!captionMode) it.clip(RoundedCornerShape(
                                            topStart = if (row == 0 && col == 0) cornerRadius else 4.dp,
                                            topEnd = if (row == 0 && col == 2) cornerRadius else 4.dp,
                                            bottomStart = if (row == 1 && col == 0) cornerRadius else 4.dp,
                                            bottomEnd = if (row == 1 && col == 2) cornerRadius else 4.dp
                                        )) else it }
                                )
                            }
                        }
                    }
                }
            }
            // ═══ 7+ ФОТО — ряды по 3 ═══
            else -> {
                val rowH = if (captionMode) 120.dp else 110.dp
                val totalPhotos = imageUris.size
                val rows = (totalPhotos + 2) / 3
                Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                    for (row in 0 until rows) {
                        val rowPhotos = imageUris.subList(row * 3, minOf(row * 3 + 3, totalPhotos))
                        val isFirstRow = row == 0
                        val isLastRow = row == rows - 1
                        Row(
                            modifier = Modifier.fillMaxWidth().height(rowH),
                            horizontalArrangement = Arrangement.spacedBy(gap)
                        ) {
                            rowPhotos.forEachIndexed { col, uri ->
                                val isFirstCol = col == 0
                                val isLastCol = col == rowPhotos.size - 1
                                GridPhoto(
                                    uri = uri,
                                    contentDescription = "Фото ${row * 3 + col + 1}",
                                    isAnalyzing = isAnalyzing,
                                    onClick = onPhotoClick?.let { { it(uri) } },
                                    modifier = Modifier.weight(1f).fillMaxHeight()
                                        .let { if (!captionMode) it.clip(RoundedCornerShape(
                                            topStart = if (isFirstRow && isFirstCol) cornerRadius else 4.dp,
                                            topEnd = if (isFirstRow && isLastCol) cornerRadius else 4.dp,
                                            bottomStart = if (isLastRow && isFirstCol) cornerRadius else 4.dp,
                                            bottomEnd = if (isLastRow && isLastCol) cornerRadius else 4.dp
                                        )) else it }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ============================================================================
// Scan animation overlay — shown on photos while AI is analyzing them
// ============================================================================

@Composable
private fun ScanAnimationOverlay() {
    val infiniteTransition = rememberInfiniteTransition(label = "photoScan")

    val scanProgress by infiniteTransition.animateFloat(
        initialValue = -0.15f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            tween(2200, easing = LinearEasing),
            RepeatMode.Restart
        ),
        label = "scan"
    )

    val edgePulse by infiniteTransition.animateFloat(
        initialValue = 0.12f,
        targetValue = 0.55f,
        animationSpec = infiniteRepeatable(
            tween(900, easing = FastOutSlowInEasing),
            RepeatMode.Reverse
        ),
        label = "edgePulse"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        // Semi-transparent dark overlay
        drawRect(Color.Black.copy(alpha = 0.38f))

        // Scan line — horizontal beam moving top→bottom
        val lineCenter = scanProgress * size.height
        val lineHalf = 36f
        if (lineCenter > -lineHalf && lineCenter < size.height + lineHalf) {
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color(0xFF00D4FF).copy(alpha = 0.28f),
                        Color.White.copy(alpha = 0.70f),
                        Color(0xFF00D4FF).copy(alpha = 0.28f),
                        Color.Transparent,
                    ),
                    startY = (lineCenter - lineHalf).coerceAtLeast(0f),
                    endY = (lineCenter + lineHalf).coerceAtMost(size.height)
                )
            )
        }

        // Left/right edge glow
        val edgeW = size.width * 0.14f
        drawRect(
            brush = Brush.horizontalGradient(
                colors = listOf(Color(0xFF00D4FF).copy(alpha = edgePulse), Color.Transparent),
                startX = 0f, endX = edgeW
            )
        )
        drawRect(
            brush = Brush.horizontalGradient(
                colors = listOf(Color.Transparent, Color(0xFF00D4FF).copy(alpha = edgePulse)),
                startX = size.width - edgeW, endX = size.width
            )
        )
    }
}

// ============================================================================
// GridPhoto — single photo cell for TelegramPhotoGrid
// ============================================================================

@Composable
private fun GridPhoto(
    uri: String,
    modifier: Modifier = Modifier,
    contentDescription: String = "Фото",
    isAnalyzing: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    Box(
        modifier = modifier
            .then(
                if (onClick != null) Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick
                ) else Modifier
            )
    ) {
        val context = LocalContext.current
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(Uri.parse(uri))
                .memoryCachePolicy(CachePolicy.ENABLED)
                .diskCachePolicy(CachePolicy.ENABLED)
                .build(),
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        if (isAnalyzing) ScanAnimationOverlay()
    }
}

// ============================================================================
// UserPhotoViewerDialog — fullscreen viewer for user-sent chat photos
// ============================================================================

@Composable
fun UserPhotoViewerDialog(
    allUris: List<String>,
    initialIndex: Int = 0,
    onDismiss: () -> Unit
) {
    var currentIndex by remember { mutableIntStateOf(initialIndex.coerceIn(0, (allUris.size - 1).coerceAtLeast(0))) }

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

    // Reset zoom/pan when navigating
    LaunchedEffect(currentIndex) {
        scale = 1f
        offset = Offset.Zero
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            decorFitsSystemWindows = false
        )
    ) {
        val context = LocalContext.current
        val currentUri = allUris.getOrElse(currentIndex) { allUris.first() }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = animatedAlpha }
                .background(Color.Black.copy(alpha = 0.93f))
                .systemBarsPadding(),
            contentAlignment = Alignment.Center
        ) {
            // Background tap → dismiss
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDismiss
                    )
            )

            // Photo with zoom/pan
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(context)
                    .data(Uri.parse(currentUri))
                    .crossfade(150)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .build(),
                contentDescription = "Просмотр фото ${currentIndex + 1}",
                modifier = Modifier
                    .graphicsLayer {
                        scaleX = animatedScale * scale
                        scaleY = animatedScale * scale
                        translationX = offset.x
                        translationY = offset.y
                    }
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
                    .onSizeChanged { imageSize = it }
                    .clip(RoundedCornerShape(12.dp))
                    .transformable(state = transformState)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        val now = System.currentTimeMillis()
                        if (now - lastTapTime < 300) {
                            scale = if (scale > 1.5f) 1f else 2.5f
                            offset = Offset.Zero
                        }
                        lastTapTime = now
                    },
                contentScale = ContentScale.Fit,
                loading = {
                    Box(modifier = Modifier.size(80.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(32.dp),
                            strokeWidth = 2.dp
                        )
                    }
                },
                error = {
                    Box(modifier = Modifier.size(80.dp), contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.BrokenImage,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }
            )

            // Left arrow
            if (allUris.size > 1 && currentIndex > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 8.dp)
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.45f))
                        .clickable { currentIndex-- },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "Предыдущее",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            // Right arrow
            if (allUris.size > 1 && currentIndex < allUris.size - 1) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 8.dp)
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.45f))
                        .clickable { currentIndex++ },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Следующее",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            // Counter "2 / 5" for multi-photo
            if (allUris.size > 1) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 10.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.Black.copy(alpha = 0.50f))
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "${currentIndex + 1} / ${allUris.size}",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Close button
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 8.dp, end = 8.dp)
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.45f))
                    .clickable { onDismiss() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Close, contentDescription = "Закрыть", tint = Color.White, modifier = Modifier.size(20.dp))
            }
        }
    }
}
