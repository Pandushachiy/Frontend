package com.health.companion.presentation.screens.chat

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.material3.Text
import com.health.companion.data.remote.api.AgentStep
import com.health.companion.data.remote.api.ProgressEvent
import com.health.companion.presentation.components.*
import kotlinx.coroutines.delay

/**
 * AgentThinkingBlock — компактная панель размышлений AI
 *
 * Использует GlassDesignSystem:
 * - GlassColors для цветов
 * - GlassTypography для текста
 * - GlassSpacing для отступов
 * - GlassShapes для скруглений
 * - AnimationDuration для анимаций
 */
@Composable
fun AgentThinkingBlock(
    steps: List<AgentStep>,
    isGenerating: Boolean,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(true) }

    // Авто-сворачивание когда ответ пошёл
    LaunchedEffect(isGenerating) {
        if (!isGenerating && steps.isNotEmpty()) {
            delay(AnimationDuration.SLOW.toLong())
            isExpanded = false
        }
    }

    if (steps.isEmpty()) return

    // Стагеррованное появление шагов
    var visibleStepCount by remember { mutableStateOf(0) }
    LaunchedEffect(steps.size) {
        while (visibleStepCount < steps.size) {
            delay(if (visibleStepCount == 0) AnimationDuration.FAST.toLong() else AnimationDuration.NORMAL.toLong())
            visibleStepCount++
        }
    }

    // ─── Компактная панель ───
    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .clickable { isExpanded = !isExpanded },
        shape = GlassShapes.medium,
        borderColor = GlassColors.accentSecondary.copy(alpha = if (isGenerating) 0.3f else 0.12f),
        elevation = GlassElevation.assistantBubble
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = GlassSpacing.bubbleHorizontal,
                vertical = GlassSpacing.buttonSpacing
            )
        ) {
            // ─── Header — всегда видна ───
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Индикатор
                if (isGenerating) {
                    PulsingDotIndicator()
                } else {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(GlassColors.accentSecondary.copy(alpha = 0.5f))
                    )
                }

                Spacer(Modifier.width(GlassSpacing.buttonSpacing))

                // Заголовок
                Text(
                    text = if (isGenerating) "Размышляю..."
                    else "Размышлял · ${steps.size} ${stepsWord(steps.size)}",
                    style = GlassTypography.timestamp.copy(
                        color = GlassColors.accentLight,
                        fontWeight = FontWeight.Medium
                    ),
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Стрелка
                Text(
                    text = if (isExpanded) "▾" else "▸",
                    style = GlassTypography.timestamp.copy(
                        color = GlassColors.textMuted
                    )
                )
            }

            // ─── Содержимое ───
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(
                    tween(AnimationDuration.NORMAL, easing = EaseOutCubic)
                ) + fadeIn(tween(AnimationDuration.FAST)),
                exit = shrinkVertically(
                    tween(AnimationDuration.FAST, easing = EaseInCubic)
                ) + fadeOut(tween(AnimationDuration.FAST))
            ) {
                Column(
                    modifier = Modifier.padding(
                        top = GlassSpacing.buttonSpacing,
                        start = GlassSpacing.betweenBubbleGroups + 2.dp // Align with header text
                    ),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    steps.take(visibleStepCount).forEachIndexed { index, step ->
                        key(step.detail.hashCode()) {
                            StepLine(
                                step = step,
                                isActive = index == steps.lastIndex && isGenerating
                            )
                        }
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════

@Composable
private fun StepLine(step: AgentStep, isActive: Boolean) {
    val alpha = if (isActive) {
        val t = rememberInfiniteTransition(label = "active")
        val a by t.animateFloat(
            0.5f, 1f,
            infiniteRepeatable(
                tween(AnimationDuration.SLOW, easing = EaseInOutSine),
                RepeatMode.Reverse
            ),
            label = "a"
        )
        a
    } else 0.7f

    val color = when (step.step) {
        "think"            -> GlassColors.accentLight
        "act"              -> GlassColors.info
        "observe"          -> if (step.detail.startsWith("✅")) GlassColors.success else GlassColors.error
        "info"             -> GlassColors.warning
        "auto_fix"         -> GlassColors.warning
        "workspace"        -> Color(0xFF10B981)  // emerald — данные сохранены
        "file_gen"         -> Color(0xFF38BDF8)  // sky — создание документа
        "parallel", "parallel_act" -> Color(0xFF818CF8)  // indigo — параллельное выполнение
        "parallel_subagent" -> if (step.detail.startsWith("✅")) GlassColors.success else if (step.detail.startsWith("❌")) GlassColors.error else GlassColors.info
        else               -> GlassColors.textSecondary
    }

    Text(
        text = step.detail,
        style = GlassTypography.timestamp.copy(
            color = color,
            fontWeight = if (isActive) FontWeight.Medium else FontWeight.Normal,
            lineHeight = GlassTypography.labelSmall.lineHeight
        ),
        modifier = Modifier
            .fillMaxWidth()
            .alpha(alpha)
    )
}

// ═══════════════════════════════════════════════════════════

@Composable
private fun PulsingDotIndicator() {
    val t = rememberInfiniteTransition(label = "dot")
    val scale by t.animateFloat(
        0.7f, 1.3f,
        infiniteRepeatable(
            tween(AnimationDuration.MODERATE, easing = EaseInOutSine),
            RepeatMode.Reverse
        ),
        label = "s"
    )
    val alpha by t.animateFloat(
        0.4f, 1f,
        infiniteRepeatable(
            tween(AnimationDuration.MODERATE, easing = EaseInOutSine),
            RepeatMode.Reverse
        ),
        label = "a"
    )
    Box(
        modifier = Modifier
            .size(6.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(GlassColors.accentSecondary.copy(alpha = alpha))
    )
}

// ═══════════════════════════════════════════════════════════

private fun stepsWord(n: Int): String = when {
    n % 10 == 1 && n % 100 != 11 -> "шаг"
    n % 10 in 2..4 && n % 100 !in 12..14 -> "шага"
    else -> "шагов"
}

// ═══════════════════════════════════════════════════════════
// Прогресс-бар потока
// ═══════════════════════════════════════════════════════════

@Composable
fun StreamProgressBar(
    progress: ProgressEvent,
    currentStatus: String,
    modifier: Modifier = Modifier
) {
    val accentColor = statusDotColor(currentStatus)
    val percent = progress.percent.coerceIn(0, 100)

    val animatedFraction by animateFloatAsState(
        targetValue = percent / 100f,
        animationSpec = tween(durationMillis = 500, easing = EaseOutCubic),
        label = "progress_fraction"
    )

    // Пульсирующий блик на конце полосы
    val glowAlpha by rememberInfiniteTransition(label = "glow")
        .animateFloat(
            initialValue = 0.5f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                tween(800, easing = EaseInOutSine),
                RepeatMode.Reverse
            ),
            label = "glow_alpha"
        )

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        // Верхняя строка: описание шага слева, процент справа
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val leftText: String? = when {
                progress.stepLabel != null -> progress.stepLabel
                progress.step != null && progress.totalSteps != null && progress.totalSteps > 0 ->
                    "Шаг ${progress.step} из ${progress.totalSteps}"
                else -> null
            }
            if (leftText != null) {
                Text(
                    text = leftText,
                    style = GlassTypography.timestamp.copy(
                        color = accentColor.copy(alpha = 0.65f),
                        fontWeight = FontWeight.Normal
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
            } else {
                Spacer(Modifier.weight(1f))
            }
            Text(
                text = "$percent%",
                style = GlassTypography.timestamp.copy(
                    color = accentColor.copy(alpha = 0.9f),
                    fontWeight = FontWeight.SemiBold
                )
            )
        }

        // Трек
        var trackSize by remember { mutableStateOf(IntSize.Zero) }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(accentColor.copy(alpha = 0.13f))
                .onSizeChanged { trackSize = it }
        ) {
            // Заполненная часть
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(animatedFraction)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                accentColor.copy(alpha = 0.5f),
                                accentColor.copy(alpha = 0.9f)
                            )
                        )
                    )
            )
            // Светящийся кружок на конце полосы
            if (animatedFraction > 0.02f && animatedFraction < 0.99f) {
                val tipOffsetPx = (trackSize.width * animatedFraction).toInt()
                val tipOffsetDp = with(LocalDensity.current) { tipOffsetPx.toDp() }
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .offset(x = tipOffsetDp - 3.5.dp, y = (-2).dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = glowAlpha))
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════
// Fallback — если бэк НЕ шлёт agent_step
// ═══════════════════════════════════════════════════════════

@Composable
fun ThinkingProcessCard(
    currentStatus: String,
    statusLabel: String? = null,
    contentStarted: Boolean = false,
    modifier: Modifier = Modifier
) {
    val text = statusLabel ?: when {
        currentStatus.contains("saving_memory", true) ||
        currentStatus.contains("save_memory", true)           -> "Запоминаю это"
        currentStatus.contains("searching_memory", true) ||
        currentStatus.contains("search_memory", true)         -> "Вспоминаю"
        currentStatus.contains("setting_reminder", true) ||
        currentStatus.contains("set_reminder", true)          -> "Напомню тебе"
        currentStatus.contains("checking_reminder", true)     -> "Смотрю напоминания"
        currentStatus.contains("generating_image", true) ||
        currentStatus.contains("editing_image", true) ||
        currentStatus.contains("creating_image", true)        -> "Рисую"
        currentStatus.contains("web_search", true) ||
        currentStatus.contains("searching_web", true)         -> "Смотрю в интернете"
        currentStatus.contains("searching_docs", true)        -> "Читаю твои файлы"
        currentStatus.contains("research", true)              -> "Копаю глубже"
        currentStatus.contains("search", true)                -> "Читаю твои файлы"
        currentStatus.contains("coding", true)                -> "Пишу код"
        currentStatus.contains("creating_excel", true)        -> "Собираю таблицу"
        currentStatus.contains("creating_word", true)         -> "Пишу документ"
        currentStatus.contains("creating_presentation", true) -> "Делаю слайды"
        currentStatus.contains("creating_pdf", true)          -> "Готовлю PDF"
        currentStatus.contains("creating_file", true)         -> "Создаю файл"
        currentStatus.contains("installing", true)            -> "Подключаю"
        currentStatus.contains("configuring", true)           -> "Настраиваю"
        currentStatus.contains("running_skill", true)         -> "Использую навык"
        currentStatus.contains("creating_skill", true)        -> "Пишу навык"
        currentStatus.contains("testing", true)               -> "Тестирую"
        currentStatus.contains("fixing_code", true)           -> "Исправляю ошибку"
        currentStatus.contains("synthesizing", true)          -> "Обрабатываю результаты"
        currentStatus.contains("awaiting_confirmation", true) -> "Жду подтверждения"
        currentStatus.contains("data_saved", true)            -> "Данные подготовлены"
        currentStatus.contains("loading_media", true)         -> "Загружаю медиа"
        currentStatus.contains("planning", true)              -> "Составляю план"
        currentStatus.contains("analyz", true)                -> "Анализирую"
        currentStatus.contains("writing", true)               -> "Составляю текст"
        currentStatus.contains("streaming", true)             -> "Пишу ответ"
        currentStatus.contains("generat", true)               -> "Пишу для тебя"
        currentStatus.contains("upload", true)                -> "Загружаю"
        currentStatus.contains("thinking", true)              -> "Думаю над этим"
        currentStatus.length > 2                               -> currentStatus
        else                                                   -> "Думаю над этим"
    }

    val dotColor = statusDotColor(currentStatus)

    val t = rememberInfiniteTransition(label = "status_dot")
    val dotScale by t.animateFloat(
        0.6f, 1.4f,
        infiniteRepeatable(tween(700, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "s"
    )
    val dotAlpha by t.animateFloat(
        0.5f, 1f,
        infiniteRepeatable(tween(700, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "a"
    )

    val pillShape = RoundedCornerShape(15.dp)
    Row(
        modifier = modifier
            .clip(pillShape)
            .background(Color.Black.copy(alpha = 0.55f), pillShape)
            .border(
                0.5.dp,
                Brush.horizontalGradient(listOf(
                    dotColor.copy(alpha = 0.20f),
                    dotColor.copy(alpha = 0.08f)
                )),
                pillShape
            )
            .padding(horizontal = 11.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .scale(dotScale)
                .clip(CircleShape)
                .background(dotColor.copy(alpha = dotAlpha))
        )
        Text(
            text = text,
            style = GlassTypography.timestamp.copy(
                color = dotColor.copy(alpha = 0.85f),
                fontWeight = FontWeight.Medium
            )
        )
    }
}

internal fun statusDotColor(status: String): Color = when {
    // Поиск
    status.contains("web_search", true) ||
    status.contains("searching_web", true)        -> Color(0xFF7C3AED)  // фиолетовый
    status.contains("searching_docs", true)       -> Color(0xFF6366F1)  // индиго
    status.contains("searching_memory", true) ||
    status.contains("saving_memory", true)        -> Color(0xFF06B6D4)  // циан
    status.contains("research", true)             -> Color(0xFF8B5CF6)  // сиреневый

    // Изображения
    status.contains("generating_image", true) ||
    status.contains("editing_image", true) ||
    status.contains("creating_image", true)       -> Color(0xFFEC4899)  // розовый

    // Файлы — каждый тип свой оттенок синего
    status.contains("creating_excel", true)       -> Color(0xFF22C55E)  // зелёный (Excel)
    status.contains("creating_word", true)        -> Color(0xFF3B82F6)  // синий (Word)
    status.contains("creating_presentation", true)-> Color(0xFFF97316)  // оранжевый (PPT)
    status.contains("creating_pdf", true)         -> Color(0xFFEF4444)  // красный (PDF)
    status.contains("creating_file", true)        -> Color(0xFF38BDF8)  // небесный

    // Код
    status.contains("coding", true) ||
    status.contains("fixing_code", true)          -> Color(0xFFF97316)  // оранжевый

    // Данные / pipeline
    status.contains("data_saved", true)           -> Color(0xFF10B981)  // изумрудный
    status.contains("loading_media", true)        -> Color(0xFF60A5FA)  // голубой

    // Напоминания
    status.contains("reminder", true)             -> Color(0xFFF59E0B)  // янтарный

    // Навыки / установка
    status.contains("installing", true) ||
    status.contains("configuring", true) ||
    status.contains("skill", true) ||
    status.contains("testing", true)              -> Color(0xFF8B5CF6)  // пурпурный

    // Анализ / синтез / план
    status.contains("synthesizing", true) ||
    status.contains("planning", true) ||
    status.contains("analyz", true)               -> Color(0xFF6366F1)  // индиго
    status.contains("writing", true)              -> Color(0xFFA78BFA)  // светло-фиолетовый

    // Подтверждение
    status.contains("awaiting_confirmation", true)-> Color(0xFFF59E0B)  // янтарный

    // Генерация ответа
    status.contains("generat", true) ||
    status.contains("streaming", true)            -> Color(0xFF6366F1)  // индиго

    else                                          -> Color(0xFF9333EA)  // дефолт — пурпурный
}
