package com.health.companion.presentation.components

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import android.widget.Toast
import com.health.companion.presentation.screens.chat.FileDownloadEntryPoint
import com.health.companion.presentation.screens.chat.buildDownloadClient
import com.health.companion.presentation.screens.chat.downloadFile
import com.health.companion.presentation.screens.chat.mimeTypeFor
import com.health.companion.presentation.screens.chat.resolveFileUrl
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.launch
import timber.log.Timber
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.health.companion.BuildConfig
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownColor
import com.mikepenz.markdown.m3.markdownTypography
import com.mikepenz.markdown.model.MarkdownColors
import com.mikepenz.markdown.model.MarkdownTypography
import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor
import org.intellij.markdown.parser.MarkdownParser

// CompositionLocal для передачи authToken в кастомные компоненты
val LocalAuthToken = compositionLocalOf<String?> { null }

// ============================================================================
// MARKDOWN COMPONENTS — Адаптировано под мобильные экраны и Glass-тему
// ============================================================================

/**
 * Glass-тема для Markdown рендеринга
 * Интегрирована с GlassColors и GlassTypography
 */
@Composable
fun glassMarkdownColors(): MarkdownColors = markdownColor(
    text = Color.White,
    codeText = Color(0xFF9ECE6A),          // Зелёный для кода
    codeBackground = Color(0xFF1A1B26),    // Тёмный фон кода
    inlineCodeText = Color(0xFF7AA2F7),    // Синий для inline кода
    inlineCodeBackground = Color(0xFF2D2D3D),
    linkText = GlassColors.accent,         // Индиго для ссылок
    dividerColor = Color.White.copy(alpha = 0.2f)
)

/**
 * Компактная типографика для чата
 * Оптимизирована под мобильные экраны
 */
@Composable
fun glassMarkdownTypography(): MarkdownTypography = markdownTypography(
    text = GlassTypography.messageText,

    h1 = TextStyle(
        fontSize = 15.sp,
        fontWeight = FontWeight.SemiBold,
        color = Color.White,
        lineHeight = 19.sp
    ),
    h2 = TextStyle(
        fontSize = 14.5.sp,
        fontWeight = FontWeight.SemiBold,
        color = Color.White.copy(alpha = 0.95f),
        lineHeight = 18.sp
    ),
    h3 = TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        color = Color.White.copy(alpha = 0.9f),
        lineHeight = 18.sp
    ),
    h4 = TextStyle(
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
        color = Color.White.copy(alpha = 0.85f),
        lineHeight = 17.sp
    ),
    h5 = TextStyle(
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
        color = Color.White.copy(alpha = 0.85f),
        lineHeight = 17.sp
    ),
    h6 = TextStyle(
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
        color = Color.White.copy(alpha = 0.85f),
        lineHeight = 17.sp
    ),

    code = TextStyle(
        fontSize = 11.sp,
        fontFamily = FontFamily.Monospace,
        color = Color(0xFF9ECE6A),
        lineHeight = 15.sp
    ),

    quote = TextStyle(
        fontSize = 12.5.sp,
        fontStyle = FontStyle.Italic,
        color = Color.White.copy(alpha = 0.8f),
        lineHeight = 16.sp
    ),

    bullet = TextStyle(
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        color = GlassColors.accent
    ),
    list = TextStyle(
        fontSize = 13.sp,
        color = Color.White,
        lineHeight = 17.sp
    ),
    ordered = TextStyle(
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        color = GlassColors.accent
    ),

    paragraph = TextStyle(
        fontSize = 13.sp,
        color = Color.White,
        lineHeight = 17.sp
    )
)

/**
 * Готовый Markdown composable для чата
 * Использует Glass-тему и оптимизирован для мобильных
 * 
 * @param content Markdown текст для рендеринга
 * @param authToken Токен авторизации для загрузки изображений (опционально)
 */
/**
 * Парсит контент и разделяет на блоки: текст, таблицы, код
 */
private sealed class ContentBlock {
    data class TextBlock(val text: String) : ContentBlock()
    data class TableBlock(val headers: List<String>, val rows: List<List<String>>) : ContentBlock()
    data class CodeBlock(val code: String, val language: String) : ContentBlock()
    data class ImageBlock(val url: String, val alt: String) : ContentBlock()
    data class DownloadLinkBlock(val label: String, val url: String) : ContentBlock()
}

private val markdownImagePattern = Regex("""!\[([^\]]*)\]\(([^)]+)\)""")

private val FILE_EXTS_MD = "xlsx|xls|csv|pdf|doc|docx|ppt|pptx|zip|rar|7z|tar|txt|mp3|wav|ogg|m4a|mp4|mov|avi|mkv|webp"

// Matches a standalone file download link on its own line.
// Catches: any /files/ path OR any URL ending in a file extension OR label ending in a file extension.
private val standaloneFileLinkPattern = Regex(
    """^[^\[]*\[([^\]]+)\]\(([^)]+(?:/files/[^)]+|\.(?:$FILE_EXTS_MD)(?:\?[^)]*)?|/api/[^)]+))\)\s*$""",
    RegexOption.IGNORE_CASE
)

/** Replaces relative /api/... hrefs with absolute URLs so mikepenz can open them. */
private fun resolveRelativeLinks(content: String): String {
    val apiHost = BuildConfig.API_BASE_URL.substringBefore("/api/")
    return content.replace(Regex("""\[([^\]]*)\]\((/api/[^)]+)\)""")) { match ->
        "[${match.groupValues[1]}]($apiHost${match.groupValues[2]})"
    }
}

private fun isTableLine(line: String): Boolean {
    val trimmed = line.trim()
    return trimmed.count { it == '|' } >= 2
}

private fun isSeparatorLine(line: String): Boolean {
    val trimmed = line.trim()
    return trimmed.contains("|") && trimmed.contains("-") &&
        trimmed.replace(Regex("[|\\-: ]"), "").isEmpty()
}

private fun parsePipeRow(line: String): List<String> =
    line.split("|").map { it.trim() }.filter { it.isNotEmpty() }

private fun parseContentWithTables(content: String): List<ContentBlock> {
    val blocks = mutableListOf<ContentBlock>()
    val lines = content.lines()
    var i = 0
    val currentText = StringBuilder()

    fun flushText() {
        if (currentText.isNotBlank()) {
            splitTextAndImages(currentText.toString().trim(), blocks)
            currentText.clear()
        } else {
            currentText.clear()
        }
    }

    while (i < lines.size) {
        val line = lines[i]

        // ── Code block ──
        if (line.trimStart().startsWith("```")) {
            flushText()
            val language = line.trimStart().removePrefix("```").trim()
            i++
            val codeBuilder = StringBuilder()
            while (i < lines.size && !lines[i].trimStart().startsWith("```")) {
                codeBuilder.appendLine(lines[i])
                i++
            }
            if (i < lines.size) i++
            val code = codeBuilder.toString().trimEnd()
            if (code.isNotBlank()) blocks.add(ContentBlock.CodeBlock(code, language))
            continue
        }

        // ── Table detection: collect consecutive pipe-lines ──
        if (isTableLine(line)) {
            val pipeLines = mutableListOf<String>()
            var j = i
            while (j < lines.size && (isTableLine(lines[j]) || isSeparatorLine(lines[j]))) {
                pipeLines.add(lines[j])
                j++
            }

            if (pipeLines.size >= 2) {
                flushText()

                val sepIdx = pipeLines.indexOfFirst { isSeparatorLine(it) }
                val headers: List<String>
                val dataLines: List<String>

                when {
                    sepIdx == 1 -> {
                        headers = parsePipeRow(pipeLines[0])
                        dataLines = pipeLines.drop(2)
                    }
                    sepIdx == 0 -> {
                        headers = parsePipeRow(pipeLines.getOrElse(1) { "" })
                        dataLines = pipeLines.drop(2)
                    }
                    sepIdx > 1 -> {
                        headers = parsePipeRow(pipeLines[sepIdx - 1])
                        dataLines = pipeLines.filterIndexed { idx, _ -> idx != sepIdx - 1 && idx != sepIdx }
                    }
                    else -> {
                        headers = parsePipeRow(pipeLines[0])
                        dataLines = pipeLines.drop(1)
                    }
                }

                val rows = dataLines
                    .filter { !isSeparatorLine(it) }
                    .map { parsePipeRow(it) }
                    .filter { it.isNotEmpty() }

                if (headers.isNotEmpty() && rows.isNotEmpty()) {
                    blocks.add(ContentBlock.TableBlock(headers, rows))
                }
                i = j
                continue
            }
        }

        // ── Standalone file-download link ──
        val fileLinkMatch = standaloneFileLinkPattern.find(line.trim())
        if (fileLinkMatch != null) {
            flushText()
            blocks.add(ContentBlock.DownloadLinkBlock(
                label = fileLinkMatch.groupValues[1],
                url   = fileLinkMatch.groupValues[2]
            ))
            i++
            continue
        }

        // ── Regular text ──
        currentText.appendLine(line)
        i++
    }

    flushText()
    return blocks
}

private fun splitTextAndImages(text: String, blocks: MutableList<ContentBlock>) {
    var remaining = text
    while (remaining.isNotEmpty()) {
        val match = markdownImagePattern.find(remaining)
        if (match != null) {
            val before = remaining.substring(0, match.range.first).trim()
            if (before.isNotBlank()) blocks.add(ContentBlock.TextBlock(before))
            blocks.add(ContentBlock.ImageBlock(url = match.groupValues[2], alt = match.groupValues[1]))
            remaining = remaining.substring(match.range.last + 1).trim()
        } else {
            if (remaining.isNotBlank()) blocks.add(ContentBlock.TextBlock(remaining))
            break
        }
    }
}

/** Returns true if [url] points to one of our backend file endpoints that require auth. */
private fun isApiFileUrl(url: String): Boolean =
    url.contains("/api/v1/chat/v3/files/") ||
    url.contains("/api/v1/files/generated") ||
    url.contains("/api/v1/files/download")

@Composable
fun GlassMarkdown(
    content: String,
    modifier: Modifier = Modifier,
    authToken: String? = null
) {
    // GFM flavour для поддержки strikethrough и других расширений
    val flavour = remember { GFMFlavourDescriptor() }
    
    // Парсим контент на блоки
    val blocks = remember(content) { parseContentWithTables(content) }
    
    val context = LocalContext.current
    // Явно предоставляем UriHandler — mikepenz вызывает openUri() по клику на ссылку.
    // Для API-файлов добавляем ?token= чтобы браузер/download-manager мог загрузить без заголовка.
    val uriHandler = remember(context, authToken) {
        object : UriHandler {
            override fun openUri(uri: String) {
                val finalUri = if (!authToken.isNullOrBlank() && isApiFileUrl(uri)) {
                    val sep = if (uri.contains('?')) "&" else "?"
                    "$uri${sep}token=$authToken"
                } else uri
                runCatching {
                    context.startActivity(
                        android.content.Intent(
                            android.content.Intent.ACTION_VIEW,
                            android.net.Uri.parse(finalUri)
                        )
                    )
                }
            }
        }
    }

    CompositionLocalProvider(
        LocalAuthToken provides authToken,
        LocalUriHandler provides uriHandler
    ) {
        Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(3.dp)) {
            blocks.forEach { block ->
                when (block) {
                    is ContentBlock.TextBlock -> {
                        if (block.text.isNotBlank()) {
                            Markdown(
                                content = resolveRelativeLinks(block.text),
                                colors = glassMarkdownColors(),
                                typography = glassMarkdownTypography(),
                                flavour = flavour
                            )
                        }
                    }
                    is ContentBlock.CodeBlock -> {
                        CodeBlockWithCopy(
                            code = block.code,
                            language = block.language
                        )
                    }
                    is ContentBlock.TableBlock -> {
                        if (block.headers.size <= 2) {
                            val md = block.rows.joinToString("\n") { row ->
                                val key = row.getOrElse(0) { "" }
                                val value = row.getOrElse(1) { "" }
                                if (value.isNotBlank()) "**$key:** $value" else "**$key**"
                            }
                            Markdown(
                                content = md,
                                colors = glassMarkdownColors(),
                                typography = glassMarkdownTypography(),
                                flavour = flavour
                            )
                        } else {
                            MobileScrollableTable(
                                headers = block.headers,
                                rows = block.rows
                            )
                        }
                    }
                    is ContentBlock.ImageBlock -> {
                        MarkdownImageCard(
                            imageUrl = block.url,
                            alt = block.alt,
                            authToken = authToken
                        )
                    }
                    is ContentBlock.DownloadLinkBlock -> {
                        FileDownloadCard(
                            label = block.label,
                            url = block.url
                        )
                    }
                }
            }
        }
    }
}

// ============================================================================
// CODE BLOCK WITH COPY — Блок кода с кнопкой копирования
// ============================================================================

@Composable
private fun CodeBlockWithCopy(
    code: String,
    language: String,
    modifier: Modifier = Modifier
) {
    val clipboardManager = LocalClipboardManager.current
    val horizontalScrollState = rememberScrollState()
    val verticalScrollState = rememberScrollState()
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF1A1B26))
            .border(1.dp, Color(0xFF3D4A6A).copy(alpha = 0.4f), RoundedCornerShape(8.dp))
    ) {
        Column {
            // Header с языком и кнопкой копирования
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF24283B))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Язык
                Text(
                    text = language.ifBlank { "code" },
                    style = TextStyle(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF7AA2F7)
                    )
                )
                // Кнопка копирования
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .clickable { 
                            clipboardManager.setText(AnnotatedString(code))
                        }
                        .background(Color(0xFF3D4A6A).copy(alpha = 0.3f))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Копировать",
                        tint = Color(0xFF9ECE6A),
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = "Copy",
                        style = TextStyle(
                            fontSize = 10.sp,
                            color = Color(0xFF9ECE6A)
                        )
                    )
                }
            }
            
            // Код с горизонтальным и вертикальным скроллом
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 300.dp)
                    .horizontalScroll(horizontalScrollState)
                    .verticalScroll(verticalScrollState)
                    .padding(10.dp)
            ) {
                Text(
                    text = code,
                    style = TextStyle(
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF9ECE6A),
                        lineHeight = 15.sp
                    )
                )
            }
        }
    }
}

// ============================================================================
// MOBILE-OPTIMIZED TABLE — Кастомная таблица для телефонов
// ============================================================================

/**
 * Таблица, оптимизированная для мобильных экранов
 * 
 * - 1-2 колонки: вертикальные карточки (key: value)
 * - 3+ колонок: горизонтальный скролл с переносом текста
 * - Текст НЕ обрезается — полный перенос
 */
@Composable
fun MobileOptimizedTable(
    headers: List<String>,
    rows: List<List<String>>,
    modifier: Modifier = Modifier
) {
    if (rows.isEmpty() || headers.isEmpty()) return
    
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    
    if (headers.size == 2 && rows.isNotEmpty()) {
        Column(
            modifier = modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            rows.forEach { row ->
                MobileKeyValueCard(
                    key = row.getOrElse(0) { "" },
                    value = row.getOrElse(1) { "" }
                )
            }
        }
    } else if (headers.size == 1 && rows.isNotEmpty()) {
        Column(
            modifier = modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            rows.forEach { row ->
                MobileTableCard(headers, row)
            }
        }
    } else {
        // Для больших таблиц — горизонтальный скролл с wrap text
        MobileScrollableTable(headers, rows, modifier)
    }
}

/**
 * Key-value карточка для 2-колоночных таблиц.
 * Первый столбец = ключ (лейбл), второй = значение.
 * Заголовки таблицы ("Поле"/"Данные") НЕ показываются — они избыточны.
 */
@Composable
private fun MobileKeyValueCard(
    key: String,
    value: String
) {
    if (key.isBlank() && value.isBlank()) return
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        Color(0xFF1E2540).copy(alpha = 0.8f),
                        Color(0xFF1A2035).copy(alpha = 0.6f)
                    )
                )
            )
            .border(1.dp, Color(0xFF3D4A6A).copy(alpha = 0.3f), RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Text(
                text = key,
                style = TextStyle(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = GlassColors.accent.copy(alpha = 0.9f),
                    lineHeight = 14.sp
                ),
                modifier = Modifier.weight(0.4f)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = value,
                style = TextStyle(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    lineHeight = 16.sp
                ),
                modifier = Modifier.weight(0.6f)
            )
        }
    }
}

/**
 * Карточка для строки таблицы (1 колонка)
 * Формат: Header → Value
 */
@Composable
private fun MobileTableCard(
    headers: List<String>,
    row: List<String>
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        Color(0xFF1E2540).copy(alpha = 0.8f),
                        Color(0xFF1A2035).copy(alpha = 0.6f)
                    )
                )
            )
            .border(1.dp, Color(0xFF3D4A6A).copy(alpha = 0.3f), RoundedCornerShape(10.dp))
            .padding(10.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            row.forEachIndexed { index, cell ->
                if (cell.isNotBlank()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = headers.getOrElse(index) { "" },
                            style = TextStyle(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = GlassColors.accent.copy(alpha = 0.9f),
                                lineHeight = 14.sp
                            ),
                            modifier = Modifier.weight(0.4f)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = cell,
                            style = TextStyle(
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Normal,
                                color = Color.White,
                                lineHeight = 16.sp
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
 * Парсит inline markdown (bold) и возвращает AnnotatedString
 */
@Composable
private fun parseInlineMarkdown(text: String): androidx.compose.ui.text.AnnotatedString {
    return androidx.compose.ui.text.buildAnnotatedString {
        var remaining = text
        val boldPattern = Regex("""\*\*(.+?)\*\*""")
        
        while (remaining.isNotEmpty()) {
            val match = boldPattern.find(remaining)
            if (match != null) {
                // Текст до bold
                if (match.range.first > 0) {
                    append(remaining.substring(0, match.range.first))
                }
                // Bold текст
                pushStyle(androidx.compose.ui.text.SpanStyle(fontWeight = FontWeight.Bold))
                append(match.groupValues[1])
                pop()
                remaining = remaining.substring(match.range.last + 1)
            } else {
                append(remaining)
                break
            }
        }
    }
}

/**
 * Скроллируемая таблица для 3+ колонок
 * С переносом текста в ячейках
 */
@Composable
private fun MobileScrollableTable(
    headers: List<String>,
    rows: List<List<String>>,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    
    // Вычисляем адаптивную ширину колонок — компактно
    val columnWidths = remember(headers, rows) {
        headers.indices.map { colIndex ->
            val headerLen = headers.getOrNull(colIndex)?.length ?: 0
            val maxDataLen = rows.maxOfOrNull { row ->
                row.getOrNull(colIndex)?.replace(Regex("""\*\*(.+?)\*\*"""), "$1")?.length ?: 0
            } ?: 0
            val maxLen = maxOf(headerLen, maxDataLen)
            // Компактно: минимум 50dp, ~4.5dp на символ, максимум 95dp
            (50 + maxLen * 4).coerceIn(50, 95)
        }
    }
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF1A1F2E))
            .border(1.dp, GlassColors.accent.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
    ) {
        Column(
            modifier = Modifier
                .horizontalScroll(scrollState)
                .padding(1.dp)
        ) {
            // Header row — компактный
            Row(
                modifier = Modifier
                    .background(GlassColors.accent.copy(alpha = 0.12f))
                    .padding(vertical = 6.dp, horizontal = 4.dp)
            ) {
                headers.forEachIndexed { index, header ->
                    Text(
                        text = header,
                        modifier = Modifier
                            .width(columnWidths.getOrElse(index) { 60 }.dp)
                            .padding(horizontal = 3.dp),
                        style = TextStyle(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = GlassColors.accent,
                            lineHeight = 12.sp
                        )
                    )
                }
            }
            
            // Разделитель под заголовком
            HorizontalDivider(
                thickness = 1.dp,
                color = GlassColors.accent.copy(alpha = 0.3f)
            )
            
            // Data rows — компактные
            rows.forEachIndexed { rowIndex, row ->
                val isEven = rowIndex % 2 == 0
                Row(
                    modifier = Modifier
                        .background(
                            if (isEven) Color(0xFF252C3F).copy(alpha = 0.5f)
                            else Color.Transparent
                        )
                        .padding(vertical = 5.dp, horizontal = 4.dp)
                ) {
                    headers.indices.forEach { colIndex ->
                        val cell = row.getOrElse(colIndex) { "" }
                        Text(
                            text = parseInlineMarkdown(cell),
                            modifier = Modifier
                                .width(columnWidths.getOrElse(colIndex) { 60 }.dp)
                                .padding(horizontal = 3.dp),
                            style = TextStyle(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Normal,
                                color = Color.White.copy(alpha = 0.95f),
                                lineHeight = 13.sp
                            )
                        )
                    }
                }
                
                // Тонкий разделитель
                if (rowIndex < rows.size - 1) {
                    HorizontalDivider(
                        thickness = 0.5.dp,
                        color = Color(0xFF3D4A6A).copy(alpha = 0.2f)
                    )
                }
            }
        }
    }
}

// ============================================================================
// FILE DOWNLOAD CARD — Tappable card for [label](/api/v1/files/generated/…) links
// ============================================================================

private enum class CardDownloadState { Idle, InProgress, Done, Error }

@Composable
private fun FileDownloadCard(
    label: String,
    url: String,
    modifier: Modifier = Modifier
) {
    val context   = LocalContext.current
    val authToken = LocalAuthToken.current
    val scope     = rememberCoroutineScope()
    val apiHost   = remember { BuildConfig.API_BASE_URL.substringBefore("/api/") }

    val fullUrl = remember(url) {
        resolveFileUrl(url, BuildConfig.API_BASE_URL)
    }
    val fileName = remember(url, label) {
        val urlFile = fullUrl.substringAfterLast("/").substringBefore("?")
        if (label.contains(".")) label else if (urlFile.contains(".")) urlFile else label
    }
    val extension = remember(fullUrl) {
        fullUrl.substringAfterLast(".", "").lowercase().substringBefore("?").take(6)
    }
    val mimeType = remember(extension) { mimeTypeFor(extension) }

    val iconColor = remember(extension) {
        when (extension) {
            "xlsx", "xls", "csv"         -> Color(0xFF4ADE80)
            "docx", "doc"                -> Color(0xFF60A5FA)
            "pptx", "ppt"                -> Color(0xFFFF9F43)
            "pdf"                        -> Color(0xFFF87171)
            "png", "jpg", "jpeg", "webp" -> Color(0xFFC084FC)
            "json"                       -> Color(0xFF7AA2F7)
            "md", "txt"                  -> Color(0xFF9ECE6A)
            else                         -> Color(0xFF7AA2F7)
        }
    }
    val iconEmoji = remember(extension) {
        when (extension) {
            "xlsx", "xls", "csv"         -> "📊"
            "docx", "doc"                -> "📄"
            "pptx", "ppt"                -> "📋"
            "pdf"                        -> "📕"
            "png", "jpg", "jpeg", "webp" -> "🖼️"
            "json"                       -> "📋"
            "md", "txt"                  -> "📝"
            else                         -> "📎"
        }
    }

    val entryPoint = remember {
        EntryPointAccessors.fromApplication(context.applicationContext, FileDownloadEntryPoint::class.java)
    }
    val filesApi       = remember { entryPoint.filesApi() }
    val appClient      = remember { entryPoint.okHttpClient() }
    val downloadClient = remember(appClient) { buildDownloadClient(appClient) }

    var state by remember(fullUrl) { mutableStateOf(CardDownloadState.Idle) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1E2540).copy(alpha = 0.85f))
            .border(1.dp, iconColor.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
            .clickable(enabled = state == CardDownloadState.Idle || state == CardDownloadState.Error) {
                state = CardDownloadState.InProgress
                scope.launch {
                    val (ok, errMsg) = downloadFile(
                        context   = context,
                        client    = downloadClient,
                        filesApi  = filesApi,
                        hintUrl   = fullUrl,
                        fileName  = fileName,
                        mime      = mimeType
                    )
                    state = if (ok) CardDownloadState.Done else CardDownloadState.Error
                    if (!ok) Toast.makeText(context, "Ошибка: $errMsg", Toast.LENGTH_LONG).show()
                }
            }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(iconColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = iconEmoji, style = TextStyle(fontSize = 20.sp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color.White),
                maxLines = 2
            )
            Text(
                text = when (state) {
                    CardDownloadState.Idle       -> extension.uppercase().ifBlank { "FILE" }
                    CardDownloadState.InProgress -> "Скачивание..."
                    CardDownloadState.Done       -> "Сохранено в Загрузки ✓"
                    CardDownloadState.Error      -> "Ошибка — нажми снова"
                },
                style = TextStyle(
                    fontSize = 11.sp,
                    color = when (state) {
                        CardDownloadState.Done  -> iconColor
                        CardDownloadState.Error -> Color(0xFFF87171)
                        else                    -> iconColor.copy(alpha = 0.7f)
                    }
                )
            )
        }
        AnimatedContent(
            targetState = state,
            transitionSpec = { fadeIn(tween(150)) togetherWith fadeOut(tween(100)) },
            label = "card_dl_icon"
        ) { s ->
            when (s) {
                CardDownloadState.Idle -> Icon(Icons.Default.Download, "Скачать",
                    tint = iconColor.copy(alpha = 0.75f), modifier = Modifier.size(20.dp))
                CardDownloadState.InProgress -> CircularProgressIndicator(
                    modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = iconColor)
                CardDownloadState.Done  -> Icon(Icons.Default.CheckCircle, "Готово",
                    tint = iconColor, modifier = Modifier.size(20.dp))
                CardDownloadState.Error -> Icon(Icons.Default.Refresh, "Повторить",
                    tint = Color(0xFFF87171), modifier = Modifier.size(20.dp))
            }
        }
    }
}

// ============================================================================
// MARKDOWN IMAGE — Inline image from ![alt](url) in AI responses
// ============================================================================

@Composable
private fun MarkdownImageCard(
    imageUrl: String,
    alt: String,
    authToken: String?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val apiHost = remember { BuildConfig.API_BASE_URL.substringBefore("/api/") }
    val fullUrl = remember(imageUrl) {
        when {
            imageUrl.startsWith("http") -> try {
                val path = java.net.URL(imageUrl).let { it.path + if (it.query != null) "?${it.query}" else "" }
                "$apiHost$path"
            } catch (e: Exception) { imageUrl }
            imageUrl.startsWith("/") -> "$apiHost$imageUrl"
            else -> "$apiHost/$imageUrl"
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
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
                // Auth handled automatically by CoilAuthInterceptor in the ImageLoader
                .build(),
            contentDescription = alt.ifBlank { "Image" },
            contentScale = ContentScale.FillWidth,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
        )
    }
}
