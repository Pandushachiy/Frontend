package com.health.companion.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ============================================================================
// 🎨 GLASS DESIGN SYSTEM — Единый источник правды
// Основано на: chat_design_prompt.md (Glassmorphism + ChatGPT Style)
// ============================================================================

/**
 * Цветовая палитра — ТОЧНО по спецификации
 */
object GlassColors {
    // === BACKGROUNDS ===
    val background = Color(0xFF0A0E27)           // Глубокий тёмный
    val surface = Color(0xFF1A1F3A)              // Матовое стекло база
    val surfaceAlt = Color(0xFF252D45)           // Чуть светлее для вложений
    
    // === ACCENTS ===
    val accent = Color(0xFF6366F1)               // Индиго (primary)
    val accentLight = Color(0xFF818CF8)          // Индиго lighter
    val accentSecondary = Color(0xFF8B5CF6)      // Фиолетовый
    val mint = Color(0xFF00D9A5)                 // Мятный (для успеха)
    val orange = Color(0xFFFF9F43)               // Оранжевый
    val coral = Color(0xFFFF6B6B)                // Коралловый (ошибки)
    val teal = Color(0xFF4ECDC4)                 // Бирюзовый
    
    // === CHAT BUBBLES ===
    val userBubble = Color(0xFF2563EB)           // Синий (Telegram style)
    val userBubbleDark = Color(0xFF1E40AF)       // Тёмный синий для градиента
    val assistantBubble = Color(0xFF1A1F3A)      // Стеклянный
    
    // === TEXT ===
    val textPrimary = Color(0xFFFFFFFF)          // Белый
    val textSecondary = Color(0xFFB0B0C0)        // Серебристо-серый
    val textTertiary = Color(0xFF8B8B9A)         // Приглушённый
    val textMuted = Color(0xFF6B7280)            // Очень приглушённый
    
    // === OVERLAYS ===
    val whiteOverlay20 = Color(0x33FFFFFF)       // 20% белый
    val whiteOverlay10 = Color(0x1AFFFFFF)       // 10% белый
    val whiteOverlay05 = Color(0x0DFFFFFF)       // 5% белый
    val blackOverlay30 = Color(0x4D000000)       // 30% чёрный
    
    // === STATUS ===
    val success = Color(0xFF4ADE80)
    val warning = Color(0xFFFBBF24)
    val error = Color(0xFFF87171)
    val info = Color(0xFF60A5FA)
}

/**
 * Градиенты
 */
object GlassGradients {
    // Основной фон приложения
    val background = Brush.linearGradient(
        colors = listOf(
            Color(0xFF0A0E27),
            Color(0xFF1A1F3A),
            Color(0xFF0F1B2E)
        ),
        start = Offset(0f, 0f),
        end = Offset(1000f, 1000f)
    )
    
    // Альтернативный вертикальный
    val backgroundVertical = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF0A0E27),
            Color(0xFF1A1F3A),
            Color(0xFF0F1B2E)
        )
    )
    
    // User bubble gradient
    val userBubble = Brush.linearGradient(
        colors = listOf(
            GlassColors.userBubble,
            GlassColors.userBubbleDark
        ),
        start = Offset(0f, 0f),
        end = Offset(500f, 500f)
    )
    
    // Accent gradient
    val accent = Brush.linearGradient(
        colors = listOf(
            GlassColors.accent,
            GlassColors.accentSecondary
        )
    )
    
    // Purple gradient
    val purple = Brush.linearGradient(
        colors = listOf(
            Color(0xFF667eea),
            Color(0xFF764ba2)
        )
    )
    
    // Warm gradient
    val warm = Brush.linearGradient(
        colors = listOf(
            Color(0xFFf093fb),
            Color(0xFFf5576c)
        )
    )
    
    // Cool gradient
    val cool = Brush.linearGradient(
        colors = listOf(
            Color(0xFF4facfe),
            Color(0xFF00f2fe)
        )
    )
}

/**
 * Типографика — по спецификации
 */
object GlassTypography {
    // Heading (TopAppBar title)
    val heading = TextStyle(
        fontSize = 18.sp,
        fontWeight = FontWeight.SemiBold,
        color = GlassColors.textPrimary,
        lineHeight = 21.6.sp  // 1.2
    )
    
    // Message text (primary)
    val messageText = TextStyle(
        fontSize = 15.sp,
        fontWeight = FontWeight.Normal,
        color = GlassColors.textPrimary,
        lineHeight = 23.sp,   // 1.4 ← ВАЖНО для читаемости
        letterSpacing = 0.25.sp
    )
    
    // Message text code/special
    val codeText = TextStyle(
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
        color = GlassColors.textPrimary,
        lineHeight = 19.5.sp  // 1.5
    )
    
    // Timestamp
    val timestamp = TextStyle(
        fontSize = 11.sp,
        fontWeight = FontWeight.Normal,
        color = GlassColors.textTertiary
    )
    
    // Input placeholder
    val placeholder = TextStyle(
        fontSize = 15.sp,
        fontWeight = FontWeight.Normal,
        color = GlassColors.textTertiary
    )
    
    // Labels
    val labelSmall = TextStyle(
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        color = GlassColors.textSecondary
    )
    
    val labelMedium = TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        color = GlassColors.textPrimary
    )
    
    // Title
    val titleSmall = TextStyle(
        fontSize = 16.sp,
        fontWeight = FontWeight.SemiBold,
        color = GlassColors.textPrimary
    )
    
    val titleMedium = TextStyle(
        fontSize = 18.sp,
        fontWeight = FontWeight.SemiBold,
        color = GlassColors.textPrimary
    )
    
    val titleLarge = TextStyle(
        fontSize = 22.sp,
        fontWeight = FontWeight.Bold,
        color = GlassColors.textPrimary
    )
}

/**
 * Отступы и размеры — по спецификации
 */
object GlassSpacing {
    val screenEdge = 12.dp              // От края экрана
    val betweenBubblesInGroup = 2.dp    // Между bubble одного автора
    val betweenBubbleGroups = 12.dp     // Между группами
    val betweenSections = 16.dp         // Между секциями
    val inputBottom = 16.dp             // Отступ input снизу
    val bubbleHorizontal = 12.dp        // Padding внутри bubble
    val bubbleVertical = 10.dp          // Padding внутри bubble
    
    // Icon/Button sizes
    val iconSize = 24.dp                // Визуальный размер иконки
    val buttonSize = 36.dp              // Pressable area
    val buttonSpacing = 8.dp            // Между кнопками
}

/**
 * Скругления углов — по спецификации
 */
object GlassShapes {
    // Стандартные
    val small = RoundedCornerShape(6.dp)
    val medium = RoundedCornerShape(12.dp)
    val large = RoundedCornerShape(16.dp)
    val extraLarge = RoundedCornerShape(20.dp)
    val card = RoundedCornerShape(24.dp)
    
    // === CHAT BUBBLES (Telegram-style, асимметричные!) ===
    
    // Assistant message (левая сторона) — маленький угол сверху-слева
    val assistantBubble = RoundedCornerShape(
        topStart = 4.dp,
        topEnd = 12.dp,
        bottomStart = 12.dp,
        bottomEnd = 12.dp
    )
    
    // User message (правая сторона) — маленький угол сверху-справа
    val userBubble = RoundedCornerShape(
        topStart = 12.dp,
        topEnd = 4.dp,
        bottomStart = 12.dp,
        bottomEnd = 12.dp
    )
    
    // Продолжение сообщения (все углы скруглены)
    val continuedBubble = RoundedCornerShape(12.dp)
    
    // Input field
    val inputField = RoundedCornerShape(12.dp)
    
    // Chip/Tag
    val chip = RoundedCornerShape(8.dp)
    
    // Circle
    val circle = CircleShape
}

/**
 * Тени — по спецификации
 */
object GlassElevation {
    val assistantBubble = 2.dp
    val userBubble = 3.dp
    val inputField = 2.dp
    val topBar = 2.dp
    val modal = 4.dp
    val fab = 6.dp
}

// ============================================================================
// 🧩 КОМПОНЕНТЫ
// ============================================================================

/**
 * Glass Card — универсальная карточка
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = GlassShapes.medium,
    backgroundColor: Color = GlassColors.surface.copy(alpha = 0.9f),
    borderColor: Color = GlassColors.whiteOverlay10,
    elevation: Dp = GlassElevation.assistantBubble,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .shadow(
                elevation = elevation,
                shape = shape,
                spotColor = Color.Black.copy(alpha = 0.3f)
            )
            .clip(shape)
            .background(backgroundColor, shape)
            .border(1.dp, borderColor, shape)
    ) {
        content()
    }
}

/**
 * Glass Card с градиентом
 */
@Composable
fun GlassCardGradient(
    modifier: Modifier = Modifier,
    shape: Shape = GlassShapes.medium,
    gradient: Brush = Brush.linearGradient(
        colors = listOf(
            GlassColors.surface.copy(alpha = 0.9f),
            GlassColors.surface.copy(alpha = 0.7f)
        )
    ),
    borderColor: Color = GlassColors.whiteOverlay10,
    elevation: Dp = GlassElevation.assistantBubble,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .shadow(
                elevation = elevation,
                shape = shape,
                spotColor = Color.Black.copy(alpha = 0.3f)
            )
            .clip(shape)
            .background(gradient, shape)
            .border(1.dp, borderColor, shape)
    ) {
        content()
    }
}

/**
 * Glass Button — кнопка
 */
@Composable
fun GlassButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isPrimary: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    val backgroundColor = if (isPrimary) {
        GlassGradients.accent
    } else {
        Brush.solidColor(GlassColors.surface)
    }
    
    val borderColor = if (isPrimary) {
        GlassColors.accent.copy(alpha = 0.5f)
    } else {
        GlassColors.whiteOverlay10
    }
    
    Row(
        modifier = modifier
            .clip(GlassShapes.medium)
            .background(backgroundColor, GlassShapes.medium)
            .border(1.dp, borderColor, GlassShapes.medium)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}

/**
 * Glass TextField — поле ввода
 */
@Composable
fun GlassTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    maxLines: Int = 4,
    singleLine: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingContent: @Composable (() -> Unit)? = null
) {
    var isFocused by remember { mutableStateOf(false) }
    
    val borderColor = if (isFocused) {
        GlassColors.accent
    } else {
        GlassColors.whiteOverlay10
    }
    
    Box(
        modifier = modifier
            .shadow(
                elevation = GlassElevation.inputField,
                shape = GlassShapes.inputField,
                spotColor = Color.Black.copy(alpha = 0.25f)
            )
            .clip(GlassShapes.inputField)
            .background(GlassColors.surface, GlassShapes.inputField)
            .border(1.dp, borderColor, GlassShapes.inputField)
            .padding(horizontal = GlassSpacing.bubbleHorizontal, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.weight(1f)) {
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = GlassTypography.placeholder
                    )
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 24.dp, max = 120.dp),
                    textStyle = GlassTypography.messageText,
                    cursorBrush = SolidColor(GlassColors.accent),
                    maxLines = if (singleLine) 1 else maxLines,
                    singleLine = singleLine,
                    keyboardOptions = keyboardOptions,
                    keyboardActions = keyboardActions,
                    visualTransformation = visualTransformation,
                    onTextLayout = { },
                    interactionSource = remember { MutableInteractionSource() }
                )
            }
            
            trailingContent?.invoke()
        }
    }
}

/**
 * Glass Chip — тег/чип
 */
@Composable
fun GlassChip(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = GlassColors.accent,
    onClick: (() -> Unit)? = null
) {
    Box(
        modifier = modifier
            .clip(GlassShapes.chip)
            .background(color.copy(alpha = 0.15f), GlassShapes.chip)
            .border(1.dp, color.copy(alpha = 0.3f), GlassShapes.chip)
            .then(
                if (onClick != null) Modifier.clickable { onClick() } else Modifier
            )
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            style = GlassTypography.labelSmall.copy(color = color)
        )
    }
}

/**
 * Glass Icon Button — кнопка с иконкой
 */
@Composable
fun GlassIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isActive: Boolean = false,
    content: @Composable () -> Unit
) {
    val color = when {
        !enabled -> GlassColors.textMuted
        isActive -> GlassColors.accent
        else -> GlassColors.textTertiary
    }
    
    Box(
        modifier = modifier
            .size(GlassSpacing.buttonSize)
            .clip(GlassShapes.circle)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

/**
 * Glass Background — фон экрана
 */
@Composable
fun GlassBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(GlassGradients.background)
    ) {
        content()
    }
}

/**
 * Glass Divider — разделитель
 */
@Composable
fun GlassDivider(
    modifier: Modifier = Modifier,
    color: Color = GlassColors.whiteOverlay10
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(color)
    )
}

// ============================================================================
// 🔧 EXTENSIONS
// ============================================================================

/**
 * Brush.solidColor extension
 */
fun Brush.Companion.solidColor(color: Color): Brush {
    return Brush.linearGradient(listOf(color, color))
}

/**
 * Max bubble width (85% screen)
 */
val maxBubbleWidthFraction = 0.85f
