package com.health.companion.presentation.screens.chat

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.text.drawText
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.health.companion.BuildConfig
import com.health.companion.data.remote.api.FilesApi
import com.health.companion.data.remote.api.GeneratedFile
import com.health.companion.presentation.components.GlassColors
import com.health.companion.presentation.components.GlassTypography
import com.health.companion.utils.TokenManager
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

@EntryPoint
@InstallIn(SingletonComponent::class)
internal interface FileDownloadEntryPoint {
    fun tokenManager(): TokenManager
    fun filesApi(): FilesApi
    fun okHttpClient(): OkHttpClient
}

private enum class DownloadState { Idle, InProgress, Done, Error }

private data class FileTypeInfo(
    val label: String,
    val accentColor: Color
)

private fun fileTypeInfo(ext: String): FileTypeInfo = when (ext.lowercase()) {
    "xlsx", "xls" -> FileTypeInfo("XLS",  Color(0xFF21A366))
    "csv"         -> FileTypeInfo("CSV",  Color(0xFF21A366))
    "pdf"         -> FileTypeInfo("PDF",  Color(0xFFE5252A))
    "doc", "docx" -> FileTypeInfo("DOC",  Color(0xFF2B7CD3))
    "ppt", "pptx" -> FileTypeInfo("PPT",  Color(0xFFE04627))
    "zip", "rar",
    "7z", "tar"   -> FileTypeInfo("ZIP",  Color(0xFF8D6E63))
    "png", "jpg",
    "jpeg", "gif",
    "webp"        -> FileTypeInfo("IMG",  Color(0xFF9C27B0))
    "mp3", "wav",
    "ogg", "m4a"  -> FileTypeInfo("MP3",  Color(0xFF00ACC1))
    "mp4", "mov",
    "avi", "mkv"  -> FileTypeInfo("MP4",  Color(0xFF00897B))
    "txt"         -> FileTypeInfo("TXT",  Color(0xFF78909C))
    else          -> FileTypeInfo("FILE", Color(0xFF78909C))
}

internal fun mimeTypeFor(ext: String): String = when (ext.lowercase()) {
    "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    "xls"  -> "application/vnd.ms-excel"
    "csv"  -> "text/csv"
    "pdf"  -> "application/pdf"
    "doc"  -> "application/msword"
    "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    "ppt"  -> "application/vnd.ms-powerpoint"
    "pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation"
    "zip"  -> "application/zip"
    "rar"  -> "application/x-rar-compressed"
    "txt"  -> "text/plain"
    "png"  -> "image/png"
    "jpg", "jpeg" -> "image/jpeg"
    "gif"  -> "image/gif"
    "mp3"  -> "audio/mpeg"
    "mp4"  -> "video/mp4"
    else   -> "application/octet-stream"
}

/**
 * Resolves a file URL to always use the app's API server.
 *
 * Cases:
 * - Absolute URL on our server  → pass through unchanged
 * - Absolute URL on other host  → rewrite host, keep path (handles LLM-generated pand-ai.ru etc.)
 * - Relative path (/api/...)    → prepend our server host
 * - Bare filename               → construct via /api/v1/files/generated/
 */
internal fun resolveFileUrl(serverUrl: String, apiBaseUrl: String): String {
    val apiHost = apiBaseUrl.substringBefore("/api/")   // e.g. http://46.17.99.76:8000
    return when {
        serverUrl.startsWith("http://") || serverUrl.startsWith("https://") -> {
            if (serverUrl.startsWith(apiHost)) {
                serverUrl  // already on our server
            } else {
                // Different host (e.g. pand-ai.ru from LLM content) — rewrite to our server
                try {
                    val path = serverUrl.substringAfter("://").substringAfter("/", "")
                    "$apiHost/$path"
                } catch (_: Exception) { serverUrl }
            }
        }
        serverUrl.startsWith("/") -> "$apiHost$serverUrl"
        else -> "$apiBaseUrl/files/generated/$serverUrl"
    }
}

/**
 * Derives a download-specific OkHttpClient from the app's main client.
 * - Inherits SSL config, auth interceptor, connection pool
 * - Removes Accept: application/json (would break binary file responses)
 * - Extends read timeout for large file transfers
 */
internal fun buildDownloadClient(appClient: OkHttpClient): OkHttpClient =
    appClient.newBuilder()
        .readTimeout(300, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            // Remove Accept: application/json — breaks binary file responses
            chain.proceed(chain.request().newBuilder().removeHeader("Accept").build())
        }
        .build()

@Composable
fun FileDownloadButton(
    file: GeneratedFile,
    authToken: String? = null,
    baseUrl: String = BuildConfig.API_BASE_URL.substringBefore("/api/"),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()
    var downloadState by remember(file.url) { mutableStateOf(DownloadState.Idle) }

    val fullUrl  = remember(file.url) {
        resolveFileUrl(file.url, BuildConfig.API_BASE_URL).also {
            Timber.d("FileDownloadButton: raw url='${file.url}' → resolved='$it'  apiBase='${BuildConfig.API_BASE_URL}'")
        }
    }
    val ext      = remember(file.name) { file.name.substringAfterLast(".", "").lowercase() }
    val typeInfo = remember(ext) { fileTypeInfo(ext) }
    val mime     = remember(ext) { mimeTypeFor(ext) }

    val entryPoint = remember {
        EntryPointAccessors.fromApplication(context.applicationContext, FileDownloadEntryPoint::class.java)
    }
    val filesApi       = remember { entryPoint.filesApi() }
    val appClient      = remember { entryPoint.okHttpClient() }
    val downloadClient = remember(appClient) { buildDownloadClient(appClient) }

    // Subtle pressed scale animation
    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 700f),
        label = "press"
    )

    // Idle state glow pulse on the action button
    val infiniteTransition = rememberInfiniteTransition(label = "dl_idle")
    val idleGlow by infiniteTransition.animateFloat(
        initialValue = 0.30f, targetValue = 0.65f,
        animationSpec = infiniteRepeatable(
            tween(1600, easing = FastOutSlowInEasing), RepeatMode.Reverse
        ), label = "idle_glow"
    )

    val accent = typeInfo.accentColor
    val shape  = RoundedCornerShape(13.dp)
    val isClickable = downloadState == DownloadState.Idle || downloadState == DownloadState.Error

    Box(
        modifier = modifier
            .graphicsLayer { scaleX = pressScale; scaleY = pressScale }
            .clip(shape)
            // Very subtle card background — glass feel
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.045f),
                        accent.copy(alpha = 0.04f)
                    )
                )
            )
            .border(
                width = 0.5.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.12f),
                        accent.copy(alpha = 0.18f)
                    )
                ),
                shape = shape
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = isClickable
            ) {
                downloadState = DownloadState.InProgress
                scope.launch {
                    val (ok, errMsg) = downloadFile(
                        context   = context,
                        client    = downloadClient,
                        filesApi  = filesApi,
                        hintUrl   = fullUrl,
                        fileName  = file.name,
                        mime      = mime
                    )
                    downloadState = if (ok) DownloadState.Done else DownloadState.Error
                    if (!ok) Toast.makeText(context, "Ошибка: $errMsg", Toast.LENGTH_LONG).show()
                }
            }
            .padding(horizontal = 9.dp, vertical = 7.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            FileTypeIcon(
                label = typeInfo.label,
                accent = accent,
                ext = ext,
                modifier = Modifier.size(37.dp)
            )

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = file.name,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.92f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 14.sp
                )
                AnimatedContent(
                    targetState = downloadState,
                    transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(150)) },
                    label = "sub"
                ) { state ->
                    Text(
                        text = when (state) {
                            DownloadState.Idle       -> "Нажмите чтобы сохранить"
                            DownloadState.InProgress -> "Скачивание…"
                            DownloadState.Done       -> "Сохранено в Загрузках"
                            DownloadState.Error      -> "Ошибка — нажмите снова"
                        },
                        fontSize = 10.sp,
                        color = when (state) {
                            DownloadState.Done  -> accent.copy(alpha = 0.85f)
                            DownloadState.Error -> Color(0xFFFF6B6B).copy(alpha = 0.9f)
                            else                -> Color.White.copy(alpha = 0.38f)
                        }
                    )
                }
            }

            // ── Download action button ───────────────────────────────────────
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .drawBehind {
                        // Animated glow ring when idle
                        if (downloadState == DownloadState.Idle) {
                            drawCircle(
                                color = accent.copy(alpha = idleGlow * 0.35f),
                                radius = size.minDimension / 2f + 6f
                            )
                        }
                    }
                    .background(
                        when (downloadState) {
                            DownloadState.Done  -> Brush.linearGradient(listOf(accent, accent.copy(alpha = 0.7f)))
                            DownloadState.Error -> Brush.linearGradient(
                                listOf(Color(0xFFE53935).copy(alpha = 0.8f), Color(0xFFE53935).copy(alpha = 0.5f))
                            )
                            else -> Brush.linearGradient(
                                listOf(accent.copy(alpha = 0.28f), accent.copy(alpha = 0.15f))
                            )
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(
                    targetState = downloadState,
                    transitionSpec = {
                        (fadeIn(tween(180)) + androidx.compose.animation.scaleIn(
                            tween(180), initialScale = 0.75f
                        )) togetherWith (fadeOut(tween(100)) + androidx.compose.animation.scaleOut(
                            tween(100), targetScale = 0.75f
                        ))
                    },
                    label = "action_icon"
                ) { state ->
                    when (state) {
                        DownloadState.Idle -> Icon(
                            Icons.Default.Download, "Скачать",
                            tint = accent, modifier = Modifier.size(16.dp)
                        )
                        DownloadState.InProgress -> CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 1.5.dp,
                            color = accent,
                            trackColor = accent.copy(alpha = 0.15f)
                        )
                        DownloadState.Done -> Icon(
                            Icons.Default.Check, "Готово",
                            tint = Color.White, modifier = Modifier.size(16.dp)
                        )
                        DownloadState.Error -> Icon(
                            Icons.Default.Refresh, "Повтор",
                            tint = Color.White, modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Minimalist document icon drawn via Canvas.
 * Each file type has a unique inner detail.
 */
@Composable
private fun FileTypeIcon(
    label: String,
    accent: Color,
    ext: String,
    modifier: Modifier = Modifier
) {
    val labelText = label.uppercase()
    val textMeasurer = androidx.compose.ui.text.rememberTextMeasurer()
    val labelStyle = androidx.compose.ui.text.TextStyle(
        fontSize = 7.sp,
        fontWeight = FontWeight.ExtraBold,
        color = Color.White,
        letterSpacing = 0.3.sp
    )

    androidx.compose.foundation.Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val fold = w * 0.28f
        val r = w * 0.1f

        // Document body path with folded corner
        val docPath = Path().apply {
            moveTo(r, 0f)
            lineTo(w - fold, 0f)
            lineTo(w, fold)
            lineTo(w, h - r)
            arcTo(
                rect = androidx.compose.ui.geometry.Rect(w - r * 2, h - r * 2, w, h),
                startAngleDegrees = 0f, sweepAngleDegrees = 90f, forceMoveTo = false
            )
            lineTo(r, h)
            arcTo(
                rect = androidx.compose.ui.geometry.Rect(0f, h - r * 2, r * 2, h),
                startAngleDegrees = 90f, sweepAngleDegrees = 90f, forceMoveTo = false
            )
            lineTo(0f, r)
            arcTo(
                rect = androidx.compose.ui.geometry.Rect(0f, 0f, r * 2, r * 2),
                startAngleDegrees = 180f, sweepAngleDegrees = 90f, forceMoveTo = false
            )
            close()
        }

        // Fill
        drawPath(docPath, accent.copy(alpha = 0.15f))
        drawPath(docPath, accent.copy(alpha = 0.40f), style = Stroke(width = 1f))

        // Folded corner triangle
        val foldPath = Path().apply {
            moveTo(w - fold, 0f)
            lineTo(w - fold, fold)
            lineTo(w, fold)
            close()
        }
        drawPath(foldPath, accent.copy(alpha = 0.25f))
        drawLine(accent.copy(alpha = 0.5f), Offset(w - fold, 0f), Offset(w - fold, fold), strokeWidth = 0.5f)
        drawLine(accent.copy(alpha = 0.5f), Offset(w - fold, fold), Offset(w, fold), strokeWidth = 0.5f)

        // Inner detail per file type
        val contentArea = androidx.compose.ui.geometry.Rect(
            w * 0.18f, h * 0.22f, w * 0.72f, h * 0.55f
        )
        drawFileDetail(ext.lowercase(), accent, contentArea)

        // Extension label strip at bottom
        val stripH = h * 0.22f
        val stripY = h - stripH - h * 0.06f
        val stripRect = RoundRect(
            left = w * 0.08f, top = stripY,
            right = w * 0.92f, bottom = stripY + stripH,
            cornerRadius = CornerRadius(stripH * 0.35f)
        )
        val stripPath = Path().apply { addRoundRect(stripRect) }
        drawPath(stripPath, accent.copy(alpha = 0.85f))

        val measured = textMeasurer.measure(labelText, labelStyle)
        drawText(
            textLayoutResult = measured,
            topLeft = Offset(
                x = (w - measured.size.width) / 2f,
                y = stripY + (stripH - measured.size.height) / 2f
            )
        )
    }
}

private fun DrawScope.drawFileDetail(ext: String, accent: Color, area: androidx.compose.ui.geometry.Rect) {
    val lineColor = accent.copy(alpha = 0.45f)
    val lineStroke = 1.2f
    when (ext) {
        "xlsx", "xls", "csv" -> {
            // Grid: 2x3 cells
            val cols = 2; val rows = 3
            val cellW = area.width / cols; val cellH = area.height / rows
            for (r in 0..rows) {
                val y = area.top + r * cellH
                drawLine(lineColor, Offset(area.left, y), Offset(area.right, y), strokeWidth = lineStroke)
            }
            for (c in 0..cols) {
                val x = area.left + c * cellW
                drawLine(lineColor, Offset(x, area.top), Offset(x, area.bottom), strokeWidth = lineStroke)
            }
        }
        "pdf" -> {
            // Horizontal lines + red accent bar on left
            drawLine(accent.copy(alpha = 0.6f), Offset(area.left, area.top), Offset(area.left, area.bottom), strokeWidth = 2.5f, cap = StrokeCap.Round)
            for (i in 0..3) {
                val y = area.top + i * area.height / 3f
                val endX = if (i == 3) area.left + area.width * 0.6f else area.right
                drawLine(lineColor, Offset(area.left + 5f, y), Offset(endX, y), strokeWidth = lineStroke, cap = StrokeCap.Round)
            }
        }
        "doc", "docx" -> {
            // Text lines
            val gaps = 4
            for (i in 0..gaps) {
                val y = area.top + i * area.height / gaps
                val endX = if (i == gaps) area.left + area.width * 0.5f else area.right
                drawLine(lineColor, Offset(area.left, y), Offset(endX, y), strokeWidth = lineStroke, cap = StrokeCap.Round)
            }
        }
        "ppt", "pptx" -> {
            // Slide frame with play triangle
            val pad = 2f
            drawRoundRect(lineColor, Offset(area.left, area.top), Size(area.width, area.height), CornerRadius(3f), style = Stroke(lineStroke))
            val cx = area.center.x; val cy = area.center.y; val tri = area.height * 0.3f
            val triPath = Path().apply {
                moveTo(cx - tri * 0.4f, cy - tri * 0.5f)
                lineTo(cx + tri * 0.6f, cy)
                lineTo(cx - tri * 0.4f, cy + tri * 0.5f)
                close()
            }
            drawPath(triPath, accent.copy(alpha = 0.5f))
        }
        "zip", "rar", "7z", "tar" -> {
            // Zipper pattern — alternating small squares
            val sqSize = area.width * 0.22f
            val cx = area.center.x
            for (i in 0..4) {
                val y = area.top + i * (area.height / 4f)
                val offsetX = if (i % 2 == 0) -sqSize * 0.6f else sqSize * 0.1f
                drawRoundRect(
                    lineColor,
                    Offset(cx + offsetX, y),
                    Size(sqSize, sqSize * 0.7f),
                    CornerRadius(1.5f),
                    style = Stroke(lineStroke)
                )
            }
        }
        "png", "jpg", "jpeg", "gif", "webp" -> {
            // Mountain + sun
            val sun = area.width * 0.14f
            drawCircle(accent.copy(alpha = 0.45f), sun, Offset(area.right - sun * 1.2f, area.top + sun * 1.2f))
            val mtPath = Path().apply {
                moveTo(area.left, area.bottom)
                lineTo(area.left + area.width * 0.35f, area.top + area.height * 0.25f)
                lineTo(area.left + area.width * 0.55f, area.top + area.height * 0.55f)
                lineTo(area.left + area.width * 0.7f, area.top + area.height * 0.4f)
                lineTo(area.right, area.bottom)
                close()
            }
            drawPath(mtPath, accent.copy(alpha = 0.35f))
        }
        "mp3", "wav", "ogg", "m4a" -> {
            // Sound wave
            val midY = area.center.y
            val bars = 5
            val barW = area.width / (bars * 2f)
            val heights = listOf(0.3f, 0.7f, 1f, 0.7f, 0.3f)
            for (i in 0 until bars) {
                val x = area.left + i * (area.width / bars) + barW * 0.5f
                val barH = area.height * heights[i] * 0.5f
                drawLine(lineColor, Offset(x, midY - barH), Offset(x, midY + barH), strokeWidth = barW * 0.7f, cap = StrokeCap.Round)
            }
        }
        "mp4", "mov", "avi", "mkv" -> {
            // Film strip + play
            drawRoundRect(lineColor, Offset(area.left, area.top), Size(area.width, area.height), CornerRadius(2f), style = Stroke(lineStroke))
            // Perforations
            val perfSize = 2f
            for (i in 0..3) {
                val y = area.top + (i + 0.5f) * area.height / 4f
                drawCircle(lineColor, perfSize, Offset(area.left + 3f, y))
                drawCircle(lineColor, perfSize, Offset(area.right - 3f, y))
            }
            val cx = area.center.x; val cy = area.center.y; val tri = area.height * 0.25f
            val triPath = Path().apply {
                moveTo(cx - tri * 0.3f, cy - tri * 0.5f)
                lineTo(cx + tri * 0.5f, cy)
                lineTo(cx - tri * 0.3f, cy + tri * 0.5f)
                close()
            }
            drawPath(triPath, accent.copy(alpha = 0.55f))
        }
        "txt" -> {
            // Simple text lines
            for (i in 0..3) {
                val y = area.top + i * area.height / 3f
                val endX = if (i == 3) area.left + area.width * 0.4f else area.right
                drawLine(lineColor, Offset(area.left, y), Offset(endX, y), strokeWidth = lineStroke, cap = StrokeCap.Round)
            }
        }
        else -> {
            // Generic lines
            for (i in 0..2) {
                val y = area.top + i * area.height / 2f
                drawLine(lineColor, Offset(area.left, y), Offset(area.right, y), strokeWidth = lineStroke, cap = StrokeCap.Round)
            }
        }
    }
}

/**
 * Downloads a file. Strategy:
 * 1. hintUrl — absolute URL from file_ready SSE event (preferred, backend guarantees correct IP)
 * 2. urlFromList — from GET /api/v1/files/generated (download_url field, also absolute)
 * 3. Bare filename fallback via /api/v1/files/generated/{filename}
 *
 * Returns Pair(success, errorMessage).
 */
internal suspend fun downloadFile(
    context: Context,
    client: OkHttpClient,
    filesApi: FilesApi,
    hintUrl: String,
    fileName: String,
    mime: String
): Pair<Boolean, String?> = withContext(Dispatchers.IO) {
    try {
        val apiBase = BuildConfig.API_BASE_URL   // e.g. http://46.17.99.76:8000/api/v1

        // Strategy 2: list endpoint — returns absolute download_url (backend now provides IP-based URLs)
        val cleanName = fileName.replace(Regex("^[\\p{So}\\p{Sm}\\p{P}\\s]+"), "").trim()
        val urlFromList: String? = try {
            val files = filesApi.listGeneratedFiles()
            val match = files.firstOrNull { it.filename == fileName || it.filename == cleanName }
            match?.resolvedUrl?.takeIf { it.isNotBlank() }
                ?.let { resolveFileUrl(it, apiBase) }
                ?.also { Timber.d("FileDownload: list url=$it") }
        } catch (e: Exception) {
            Timber.w(e, "FileDownload: list call failed")
            null
        }

        // Strategy 3: /api/v1/files/download/{filename} — new backend endpoint, finds file by name
        val cleanFileName = fileName.replace(Regex("^[\\p{So}\\p{Sm}\\p{P}\\s]+"), "").trim()
        val filenameUrl = "$apiBase/files/download/${java.net.URLEncoder.encode(cleanFileName, "UTF-8").replace("+", "%20")}"

        val candidates = listOfNotNull(
            hintUrl.takeIf { it.isNotBlank() },               // Strategy 1: file_ready URL (absolute IP)
            urlFromList,                                        // Strategy 2: list endpoint download_url
            filenameUrl.takeIf { it != hintUrl }               // Strategy 3: /files/download/{filename}
        ).distinct()
        Timber.d("FileDownload: hintUrl='$hintUrl' candidates=$candidates")

        var lastError = "Нет доступных URL"
        var succeeded = false

        for (downloadUrl in candidates) {
            if (succeeded) break

            val response = try {
                val request = Request.Builder().url(downloadUrl).build()
                client.newCall(request).execute()
            } catch (e: java.net.UnknownHostException) {
                Timber.w("FileDownload: DNS fail for $downloadUrl — trying next")
                lastError = "DNS error: $downloadUrl"
                continue
            } catch (e: java.io.IOException) {
                Timber.w(e, "FileDownload: IO fail for $downloadUrl — trying next")
                lastError = "IO: ${e.message}"
                continue
            }
            Timber.d("FileDownload HTTP ${response.code} for $downloadUrl")

            if (!response.isSuccessful) {
                lastError = "HTTP ${response.code} ($downloadUrl): ${response.body?.string()?.take(150) ?: ""}"
                Timber.w("FileDownload: $lastError")
                response.close()
            } else {
                val body = response.body
                if (body == null) {
                    lastError = "Пустой ответ: $downloadUrl"
                    response.close()
                } else {
                    // Save to Downloads
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        val cv = ContentValues().apply {
                            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                            put(MediaStore.Downloads.MIME_TYPE, mime)
                            put(MediaStore.Downloads.IS_PENDING, 1)
                        }
                        val resolver = context.contentResolver
                        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, cv)
                        if (uri == null) {
                            body.close()
                            return@withContext Pair(false, "MediaStore: нет места или нет прав")
                        }
                        val stream = resolver.openOutputStream(uri)
                        if (stream == null) {
                            body.close()
                            return@withContext Pair(false, "Не удалось открыть файл для записи")
                        }
                        stream.use { out -> body.byteStream().copyTo(out) }
                        cv.clear()
                        cv.put(MediaStore.Downloads.IS_PENDING, 0)
                        resolver.update(uri, cv, null, null)
                    } else {
                        val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                        dir.mkdirs()
                        FileOutputStream(File(dir, fileName)).use { out -> body.byteStream().copyTo(out) }
                    }
                    body.close()
                    Timber.d("FileDownload ✓ saved $fileName via $downloadUrl")
                    succeeded = true
                }
            }
        }

        if (succeeded) Pair(true, null)
        else {
            Timber.e("FileDownload ✗ all candidates failed. Last: $lastError")
            Pair(false, lastError)
        }
    } catch (e: Exception) {
        Timber.e(e, "FileDownload ✗ $fileName")
        Pair(false, e.message ?: e.javaClass.simpleName)
    }
}
