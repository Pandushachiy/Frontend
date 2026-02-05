package com.health.companion.presentation.screens.chat

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * SSE Status Indicator — Glassmorphism + Rich Animations
 * 
 * Показывает текущий статус обработки запроса с уникальной анимацией
 */
@Composable
fun SSEStatusIndicator(
    status: String,
    modifier: Modifier = Modifier
) {
    val config = remember(status) { getStatusConfig(status) }
    
    AnimatedContent(
        targetState = config,
        transitionSpec = {
            (fadeIn(tween(300)) + scaleIn(tween(300), initialScale = 0.9f))
                .togetherWith(fadeOut(tween(200)) + scaleOut(tween(200), targetScale = 0.9f))
        },
        modifier = modifier,
        label = "status_transition"
    ) { currentConfig ->
        GlassStatusBubble(currentConfig)
    }
}

@Composable
private fun GlassStatusBubble(config: StatusConfig) {
    // Компактный bubble — только анимация, без текста
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        config.color.copy(alpha = 0.15f),
                        config.color.copy(alpha = 0.05f)
                    )
                )
            )
            .border(
                width = 1.dp,
                color = config.color.copy(alpha = 0.3f),
                shape = RoundedCornerShape(14.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        // Только анимированная иконка
        Box(
            modifier = Modifier.size(32.dp),
            contentAlignment = Alignment.Center
        ) {
            when (config.animationType) {
                AnimationType.PULSE_ORBIT -> ThinkingAnimation(config.color)
                AnimationType.EQUALIZER -> AnalyzingAnimation(config.color)
                AnimationType.RADAR -> SearchingAnimation(config.color)
                AnimationType.GLOBE -> WebSearchAnimation(config.color)
                AnimationType.TYPING -> GeneratingAnimation(config.color)
                AnimationType.PIXELS -> ImageGeneratingAnimation(config.color)
                AnimationType.SPARKLES -> EnhancingAnimation(config.color)
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════
// 🧠 THINKING — Пульсирующий мозг с орбитальными частицами
// ═══════════════════════════════════════════════════════════
@Composable
private fun ThinkingAnimation(color: Color) {
    val infiniteTransition = rememberInfiniteTransition(label = "thinking")
    
    // Пульсация мозга
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )
    
    // Вращение орбиты
    val orbitAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing)
        ),
        label = "orbit"
    )
    
    Box(
        modifier = Modifier.size(32.dp),
        contentAlignment = Alignment.Center
    ) {
        // Orbital particles
        Canvas(modifier = Modifier.size(32.dp)) {
            val center = Offset(size.width / 2, size.height / 2)
            val orbitRadius = size.width / 2 - 2.dp.toPx()
            
            // 3 orbital dots
            for (i in 0..2) {
                val angle = Math.toRadians((orbitAngle + i * 120).toDouble())
                val x = center.x + orbitRadius * cos(angle).toFloat()
                val y = center.y + orbitRadius * sin(angle).toFloat()
                
                drawCircle(
                    color = color.copy(alpha = 0.9f - i * 0.2f),
                    radius = 3.dp.toPx() - i * 0.5f,
                    center = Offset(x, y)
                )
            }
        }
        
        // Brain emoji
        Text(
            text = "🧠",
            fontSize = 16.sp,
            modifier = Modifier.scale(pulse)
        )
    }
}

// ═══════════════════════════════════════════════════════════
// 📊 ANALYZING — Эквалайзер
// ═══════════════════════════════════════════════════════════
@Composable
private fun AnalyzingAnimation(color: Color) {
    val infiniteTransition = rememberInfiniteTransition(label = "analyzing")
    
    // 5 баров с разными фазами
    val bars = List(5) { index ->
        infiniteTransition.animateFloat(
            initialValue = 0.3f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = 400 + index * 100,
                    easing = EaseInOutSine
                ),
                repeatMode = RepeatMode.Reverse
            ),
            label = "bar_$index"
        )
    }
    
    Canvas(modifier = Modifier.size(32.dp)) {
        val barWidth = 4.dp.toPx()
        val gap = 2.dp.toPx()
        val totalWidth = 5 * barWidth + 4 * gap
        val startX = (size.width - totalWidth) / 2
        
        bars.forEachIndexed { index, heightState ->
            val height = size.height * 0.7f * heightState.value
            val x = startX + index * (barWidth + gap)
            val y = (size.height - height) / 2
            
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(color, color.copy(alpha = 0.5f))
                ),
                topLeft = Offset(x, y),
                size = Size(barWidth, height),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx())
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════
// 🔍 SEARCHING — Радар
// ═══════════════════════════════════════════════════════════
@Composable
private fun SearchingAnimation(color: Color) {
    val infiniteTransition = rememberInfiniteTransition(label = "searching")
    
    val sweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing)
        ),
        label = "sweep"
    )
    
    val pingAlpha by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseOut)
        ),
        label = "ping"
    )
    
    val pingScale by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseOut)
        ),
        label = "ping_scale"
    )
    
    Canvas(modifier = Modifier.size(32.dp)) {
        val center = Offset(size.width / 2, size.height / 2)
        val radius = size.width / 2 - 2.dp.toPx()
        
        // Outer circle
        drawCircle(
            color = color.copy(alpha = 0.3f),
            radius = radius,
            center = center,
            style = Stroke(width = 1.5.dp.toPx())
        )
        
        // Inner circle
        drawCircle(
            color = color.copy(alpha = 0.2f),
            radius = radius * 0.5f,
            center = center,
            style = Stroke(width = 1.dp.toPx())
        )
        
        // Ping wave
        drawCircle(
            color = color.copy(alpha = pingAlpha * 0.5f),
            radius = radius * pingScale,
            center = center,
            style = Stroke(width = 1.5.dp.toPx())
        )
        
        // Sweep line
        rotate(sweepAngle, center) {
            drawLine(
                brush = Brush.linearGradient(
                    colors = listOf(color, Color.Transparent),
                    start = center,
                    end = Offset(center.x, center.y - radius)
                ),
                start = center,
                end = Offset(center.x, center.y - radius),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
        
        // Center dot
        drawCircle(
            color = color,
            radius = 3.dp.toPx(),
            center = center
        )
    }
}

// ═══════════════════════════════════════════════════════════
// 🌐 WEB_SEARCH — Вращающийся глобус
// ═══════════════════════════════════════════════════════════
@Composable
private fun WebSearchAnimation(color: Color) {
    val infiniteTransition = rememberInfiniteTransition(label = "web_search")
    
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing)
        ),
        label = "rotation"
    )
    
    val orbitAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing)
        ),
        label = "orbit"
    )
    
    Box(
        modifier = Modifier.size(32.dp),
        contentAlignment = Alignment.Center
    ) {
        // Globe + orbiting dots
        Canvas(modifier = Modifier.size(32.dp)) {
            val center = Offset(size.width / 2, size.height / 2)
            val radius = size.width / 2 - 4.dp.toPx()
            
            // Globe circle
            drawCircle(
                color = color.copy(alpha = 0.4f),
                radius = radius,
                center = center,
                style = Stroke(width = 1.5.dp.toPx())
            )
            
            // Equator
            drawArc(
                color = color.copy(alpha = 0.4f),
                startAngle = 0f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius * 0.25f),
                size = Size(radius * 2, radius * 0.5f),
                style = Stroke(width = 1.dp.toPx())
            )
            
            // Orbiting dots
            val orbitRadius = size.width / 2 - 2.dp.toPx()
            for (i in 0..2) {
                val angle = Math.toRadians((orbitAngle + i * 120).toDouble())
                val x = center.x + orbitRadius * cos(angle).toFloat()
                val y = center.y + orbitRadius * sin(angle).toFloat() * 0.4f
                
                drawCircle(
                    color = color,
                    radius = 2.5.dp.toPx(),
                    center = Offset(x, y)
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════
// ✍️ GENERATING — Печатающий курсор
// ═══════════════════════════════════════════════════════════
@Composable
private fun GeneratingAnimation(color: Color) {
    val infiniteTransition = rememberInfiniteTransition(label = "generating")
    
    val cursorAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cursor"
    )
    
    // Анимированные "буквы"
    val letterProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing)
        ),
        label = "letters"
    )
    
    Canvas(modifier = Modifier.size(32.dp)) {
        val lineHeight = 4.dp.toPx()
        val gap = 3.dp.toPx()
        val startY = (size.height - 3 * lineHeight - 2 * gap) / 2
        
        for (i in 0..2) {
            val y = startY + i * (lineHeight + gap)
            val width = when {
                i < letterProgress.toInt() -> size.width * 0.8f
                i == letterProgress.toInt() -> size.width * 0.8f * (letterProgress - i)
                else -> 0f
            }
            
            if (width > 0) {
                drawRoundRect(
                    color = color.copy(alpha = 0.6f),
                    topLeft = Offset(2.dp.toPx(), y),
                    size = Size(width.coerceAtMost(size.width * 0.75f), lineHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx())
                )
            }
        }
        
        // Cursor
        val cursorLine = letterProgress.toInt().coerceAtMost(2)
        val cursorX = when {
            cursorLine < letterProgress.toInt() -> size.width * 0.78f
            else -> 2.dp.toPx() + size.width * 0.8f * (letterProgress - cursorLine)
        }.coerceAtMost(size.width * 0.78f)
        val cursorY = startY + cursorLine * (lineHeight + gap)
        
        drawRoundRect(
            color = color.copy(alpha = cursorAlpha),
            topLeft = Offset(cursorX, cursorY - 1.dp.toPx()),
            size = Size(2.dp.toPx(), lineHeight + 2.dp.toPx()),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(1.dp.toPx())
        )
    }
}

// ═══════════════════════════════════════════════════════════
// 🎨 GENERATING_IMAGE — Пиксельное заполнение
// ═══════════════════════════════════════════════════════════
@Composable
private fun ImageGeneratingAnimation(color: Color) {
    val infiniteTransition = rememberInfiniteTransition(label = "image_gen")
    
    val progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = EaseInOutSine)
        ),
        label = "progress"
    )
    
    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing)
        ),
        label = "shimmer"
    )
    
    Canvas(modifier = Modifier.size(32.dp)) {
        val pixelSize = 4.5.dp.toPx()
        val gap = 1.dp.toPx()
        val cols = 5
        val rows = 5
        val totalPixels = cols * rows
        val filledPixels = (totalPixels * progress).toInt()
        
        val startX = (size.width - cols * (pixelSize + gap)) / 2
        val startY = (size.height - rows * (pixelSize + gap)) / 2
        
        val spiralOrder = listOf(
            12, 7, 8, 13, 18, 17, 16, 11, 6, 1, 2, 3, 4, 9, 14, 19, 24, 23, 22, 21, 20, 15, 10, 5, 0
        )
        
        for (i in 0 until totalPixels) {
            val pixelIndex = spiralOrder.getOrElse(i) { i }
            val col = pixelIndex % cols
            val row = pixelIndex / cols
            
            val x = startX + col * (pixelSize + gap)
            val y = startY + row * (pixelSize + gap)
            
            val alpha = if (i < filledPixels) {
                val shimmerPos = (x / size.width + shimmerOffset).coerceIn(0f, 1f)
                0.4f + 0.4f * if (shimmerPos in 0.4f..0.6f) 1f else 0.5f
            } else {
                0.15f
            }
            
            drawRoundRect(
                color = color.copy(alpha = alpha),
                topLeft = Offset(x, y),
                size = Size(pixelSize, pixelSize),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(1.dp.toPx())
            )
        }
        
        // Frame
        drawRoundRect(
            color = color.copy(alpha = 0.4f),
            topLeft = Offset(startX - 1.dp.toPx(), startY - 1.dp.toPx()),
            size = Size(
                cols * (pixelSize + gap) + 1.dp.toPx(),
                rows * (pixelSize + gap) + 1.dp.toPx()
            ),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx()),
            style = Stroke(width = 1.dp.toPx())
        )
    }
}

// ═══════════════════════════════════════════════════════════
// ✨ ENHANCING — Искры/Sparkles
// ═══════════════════════════════════════════════════════════
@Composable
private fun EnhancingAnimation(color: Color) {
    val infiniteTransition = rememberInfiniteTransition(label = "enhancing")
    
    // Multiple sparkles with different phases
    val sparkles = List(6) { index ->
        val delay = index * 200
        Triple(
            infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1200, delayMillis = delay, easing = EaseOut)
                ),
                label = "scale_$index"
            ),
            infiniteTransition.animateFloat(
                initialValue = 1f,
                targetValue = 0f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1200, delayMillis = delay, easing = EaseIn)
                ),
                label = "alpha_$index"
            ),
            infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 45f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1200, delayMillis = delay, easing = LinearEasing)
                ),
                label = "rotation_$index"
            )
        )
    }
    
    // Sparkle positions (normalized 0-1)
    val positions = listOf(
        Offset(0.5f, 0.2f),  // top center
        Offset(0.8f, 0.35f), // right
        Offset(0.7f, 0.75f), // bottom right
        Offset(0.3f, 0.8f),  // bottom left
        Offset(0.15f, 0.5f), // left
        Offset(0.35f, 0.25f) // top left
    )
    
    Canvas(modifier = Modifier.size(32.dp)) {
        val center = Offset(size.width / 2, size.height / 2)
        
        // Central glow
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    color.copy(alpha = 0.4f),
                    Color.Transparent
                ),
                center = center,
                radius = size.width / 2
            ),
            center = center,
            radius = size.width / 2
        )
        
        // Draw sparkles
        sparkles.forEachIndexed { index, (scaleState, alphaState, rotationState) ->
            val pos = positions[index]
            val sparkleCenter = Offset(pos.x * size.width, pos.y * size.height)
            val scale = scaleState.value
            val alpha = alphaState.value
            val rotation = rotationState.value
            
            // Draw 4-point star
            rotate(rotation, sparkleCenter) {
                val armLength = 5.dp.toPx() * scale
                val armWidth = 1.5.dp.toPx()
                
                // Vertical arm
                drawRoundRect(
                    color = color.copy(alpha = alpha),
                    topLeft = Offset(sparkleCenter.x - armWidth / 2, sparkleCenter.y - armLength),
                    size = Size(armWidth, armLength * 2),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(armWidth / 2)
                )
                
                // Horizontal arm
                drawRoundRect(
                    color = color.copy(alpha = alpha),
                    topLeft = Offset(sparkleCenter.x - armLength, sparkleCenter.y - armWidth / 2),
                    size = Size(armLength * 2, armWidth),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(armWidth / 2)
                )
            }
        }
        
        // Center star emoji area glow
        drawCircle(
            color = color.copy(alpha = 0.3f),
            radius = 8.dp.toPx(),
            center = center
        )
    }
    
    // Star emoji on top
    Box(
        modifier = Modifier.size(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Text("✨", fontSize = 14.sp)
    }
}

// ═══════════════════════════════════════════════════════════
// Animated dots "..."
// ═══════════════════════════════════════════════════════════
@Composable
private fun AnimatedDots(color: Color) {
    val infiniteTransition = rememberInfiniteTransition(label = "dots")
    
    val dots = List(3) { index ->
        infiniteTransition.animateFloat(
            initialValue = 0.3f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(600, delayMillis = index * 150, easing = EaseInOutSine),
                repeatMode = RepeatMode.Reverse
            ),
            label = "dot_$index"
        )
    }
    
    Row(
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        modifier = Modifier.padding(start = 2.dp)
    ) {
        dots.forEach { alphaState ->
            Box(
                modifier = Modifier
                    .size(4.dp)
                    .alpha(alphaState.value)
                    .background(color, CircleShape)
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════
// Config
// ═══════════════════════════════════════════════════════════
private data class StatusConfig(
    val icon: String,
    val text: String,
    val color: Color,
    val animationType: AnimationType
)

private enum class AnimationType {
    PULSE_ORBIT,  // thinking
    EQUALIZER,    // analyzing
    RADAR,        // searching
    GLOBE,        // web_search
    TYPING,       // generating
    PIXELS,       // generating_image
    SPARKLES      // enhancing
}

private fun getStatusConfig(status: String): StatusConfig {
    android.util.Log.d("SSE_STATUS", "🎯 Status received: '$status'")
    return when {
        // Uploading file
        status.contains("upload", ignoreCase = true) -> StatusConfig(
            icon = "📤",
            text = "Загружаю",
            color = Color(0xFF14B8A6),
            animationType = AnimationType.EQUALIZER
        )
        // Thinking
        status.contains("think", ignoreCase = true) -> StatusConfig(
            icon = "🧠",
            text = "Думаю",
            color = Color(0xFF9333EA),
            animationType = AnimationType.PULSE_ORBIT
        )
        // Analyzing
        status.contains("analyz", ignoreCase = true) -> StatusConfig(
            icon = "📊",
            text = "Анализирую",
            color = Color(0xFF3B82F6),
            animationType = AnimationType.EQUALIZER
        )
        // Web search (check before generic search!)
        // Handles: "web_search", "searching_web", "web search", etc.
        status.contains("web_search", ignoreCase = true) ||
        status.contains("searching_web", ignoreCase = true) ||
        (status.contains("web", ignoreCase = true) && status.contains("search", ignoreCase = true)) -> StatusConfig(
            icon = "🌐",
            text = "Ищу в интернете",
            color = Color(0xFF1FB8CD),
            animationType = AnimationType.GLOBE
        )
        // Document search
        status.contains("search", ignoreCase = true) -> StatusConfig(
            icon = "🔍",
            text = "Ищу в документах",
            color = Color(0xFF10B981),
            animationType = AnimationType.RADAR
        )
        // Image generation
        status.contains("image", ignoreCase = true) || 
        status.contains("рису", ignoreCase = true) ||
        status.contains("generating_image", ignoreCase = true) -> StatusConfig(
            icon = "🎨",
            text = "Рисую",
            color = Color(0xFFEC4899),
            animationType = AnimationType.PIXELS
        )
        // Text generation
        status.contains("generat", ignoreCase = true) || 
        status.contains("пишу", ignoreCase = true) ||
        status.contains("writing", ignoreCase = true) -> StatusConfig(
            icon = "✍️",
            text = "Пишу ответ",
            color = Color(0xFF6366F1),
            animationType = AnimationType.TYPING
        )
        // Enhancing
        status.contains("enhanc", ignoreCase = true) || 
        status.contains("улучш", ignoreCase = true) ||
        status.contains("improv", ignoreCase = true) -> StatusConfig(
            icon = "✨",
            text = "Улучшаю",
            color = Color(0xFFFBBF24),
            animationType = AnimationType.SPARKLES
        )
        // Processing (generic)
        status.contains("process", ignoreCase = true) -> StatusConfig(
            icon = "⚙️",
            text = "Обрабатываю",
            color = Color(0xFF64748B),
            animationType = AnimationType.EQUALIZER
        )
        // Default - thinking
        else -> StatusConfig(
            icon = "🧠",
            text = "Обрабатываю",
            color = Color(0xFF9333EA),
            animationType = AnimationType.PULSE_ORBIT
        )
    }
}
