package com.health.companion.presentation.screens.chat

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.health.companion.data.remote.api.ThinkingChainStep
import com.health.companion.presentation.components.*
import kotlinx.coroutines.delay

// ─── Цвета фаз ───────────────────────────────────────────────────────────────

private fun phaseColorFor(phase: String): Color = when (phase) {
    "analyze"  -> Color(0xFF8B5CF6)  // фиолетовый — мышление
    "decision" -> Color(0xFF6366F1)  // индиго     — решение
    "execute"  -> Color(0xFFF59E0B)  // янтарный   — действие
    "observe"  -> Color(0xFF14B8A6)  // бирюзовый  — наблюдение
    "answer"   -> Color(0xFF10B981)  // изумрудный — ответ
    else       -> Color(0xFF818CF8)  // дефолт     — светлый индиго
}

// detail-текст уже содержит эмодзи от бэка — не дублируем иконку

// ─── Иконки инструментов для pipeline-плана ──────────────────────────────────

private fun toolIcon(tool: String) = when {
    tool.contains("web_search", true)    -> "🌐"
    tool.contains("excel", true) ||
    tool.contains("spreadsheet", true)   -> "📊"
    tool.contains("word", true) ||
    tool.contains("document", true)      -> "📝"
    tool.contains("pdf", true)           -> "📄"
    tool.contains("image", true) ||
    tool.contains("dalle", true)         -> "🎨"
    tool.contains("memory", true)        -> "🧠"
    tool.contains("reminder", true)      -> "⏰"
    tool.contains("code", true)          -> "💻"
    tool.contains("telegram", true)      -> "✈️"
    tool.contains("email", true)         -> "📧"
    tool.contains("calendar", true)      -> "📅"
    else                                 -> "⚙️"
}

// ─── Главный компонент ────────────────────────────────────────────────────────

/**
 * ThinkingChainCard — визуализация reasoning chain.
 * Показывает фазы analyze/execute/observe, pipeline-план и шаги с таймингом.
 * Авто-сворачивается когда контент начинает стримиться.
 */
@Composable
fun ThinkingChainCard(
    steps: List<ThinkingChainStep>,
    isStreaming: Boolean,
    modifier: Modifier = Modifier
) {
    if (steps.isEmpty()) return

    var isExpanded by remember { mutableStateOf(true) }

    // Авто-сворачивание когда ответ пошёл
    LaunchedEffect(isStreaming) {
        if (!isStreaming && steps.isNotEmpty()) {
            delay(1200)
            isExpanded = false
        }
    }

    // Стаггерованное появление шагов
    var visibleCount by remember { mutableStateOf(0) }
    LaunchedEffect(steps.size) {
        while (visibleCount < steps.size) {
            delay(if (visibleCount == 0) 80L else 120L)
            visibleCount++
        }
    }

    val isActive = isStreaming
    // Цвет активной фазы — по последнему шагу
    val lastStep = steps.lastOrNull()
    val activeColor = lastStep?.let { phaseColorFor(it.phase) } ?: GlassColors.accentSecondary

    // Последний план из любого шага
    val latestPlan = steps.lastOrNull { it.plan != null }?.plan

    // Суммарное время
    val totalMs = steps.sumOf { it.elapsedMs }

    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .clickable { isExpanded = !isExpanded },
        shape = GlassShapes.medium,
        borderColor = activeColor.copy(alpha = if (isActive) 0.35f else 0.12f),
        elevation = GlassElevation.assistantBubble
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = GlassSpacing.bubbleHorizontal,
                vertical = GlassSpacing.buttonSpacing
            )
        ) {
            // ── Шапка ─────────────────────────────────────────────────────────
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Пульсирующий индикатор
                if (isActive) {
                    PulsingChainDot(color = activeColor)
                } else {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(activeColor.copy(alpha = 0.45f))
                    )
                }

                Spacer(Modifier.width(6.dp))

                Text(
                    text = if (isActive) "Размышляю..."
                    else "Размышлял · ${steps.size} ${stepsWord2(steps.size)}" +
                            if (totalMs > 0) " · ${formatMs(totalMs)}" else "",
                    style = GlassTypography.timestamp.copy(
                        color = GlassColors.accentLight,
                        fontWeight = FontWeight.Medium
                    ),
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = if (isExpanded) "▾" else "▸",
                    style = GlassTypography.timestamp.copy(color = GlassColors.textMuted)
                )
            }

            // ── Pipeline-план ─────────────────────────────────────────────────
            if (latestPlan != null) {
                Spacer(Modifier.height(6.dp))
                PipelinePlan(plan = latestPlan)
            }

            // ── Список шагов ──────────────────────────────────────────────────
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(tween(220, easing = EaseOutCubic)) + fadeIn(tween(150)),
                exit  = shrinkVertically(tween(150, easing = EaseInCubic)) + fadeOut(tween(120))
            ) {
                Column(
                    modifier = Modifier.padding(
                        top  = GlassSpacing.buttonSpacing,
                        start = 12.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    steps.take(visibleCount).forEachIndexed { idx, step ->
                        key(step.step * 1000 + step.phase.hashCode()) {
                            ChainStepRow(
                                step = step,
                                isActive = idx == steps.lastIndex && isActive
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─── Pipeline-план (web_search → create_spreadsheet) ─────────────────────────

@Composable
private fun PipelinePlan(plan: String) {
    val tools = plan.split("→", "->").map { it.trim() }.filter { it.isNotBlank() }
    if (tools.isEmpty()) return

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White.copy(alpha = 0.04f))
            .padding(horizontal = 8.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        tools.forEachIndexed { idx, tool ->
            // Тул-пилюля
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.White.copy(alpha = 0.06f))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(text = toolIcon(tool), fontSize = 10.sp)
                Text(
                    text = tool.replace("_", " "),
                    style = GlassTypography.timestamp.copy(
                        color = GlassColors.textSecondary,
                        fontWeight = FontWeight.Medium,
                        fontSize = 10.sp
                    ),
                    maxLines = 1
                )
            }
            // Стрелка между шагами
            if (idx < tools.lastIndex) {
                Text(
                    text = "→",
                    style = GlassTypography.timestamp.copy(
                        color = GlassColors.textMuted,
                        fontSize = 9.sp
                    )
                )
            }
        }
    }
}

// ─── Строка одного шага ────────────────────────────────────────────────────────

@Composable
private fun ChainStepRow(step: ThinkingChainStep, isActive: Boolean) {
    val color = phaseColorFor(step.phase)

    // Определяем цвет по содержимому detail
    val textColor = when {
        step.detail.contains("✅") -> Color(0xFF10B981)  // успех — зелёный
        step.detail.contains("⚠️") || step.detail.contains("❌") -> Color(0xFFF97316) // ошибка — оранжевый
        else -> color
    }

    val alpha by if (isActive) {
        rememberInfiniteTransition(label = "chain_active")
            .animateFloat(
                0.6f, 1f,
                infiniteRepeatable(tween(900, easing = EaseInOutSine), RepeatMode.Reverse),
                label = "a"
            )
    } else {
        remember { mutableStateOf(0.8f) }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .alpha(alpha)
    ) {
        // detail уже содержит эмодзи и читабельный текст от бэка — показываем как есть
        Text(
            text = step.detail,
            style = GlassTypography.timestamp.copy(
                color = textColor,
                fontWeight = if (isActive) FontWeight.Medium else FontWeight.Normal,
                lineHeight = GlassTypography.labelSmall.lineHeight
            ),
            modifier = Modifier.weight(1f)
        )

        // Тайминг справа
        if (step.elapsedMs > 0) {
            Text(
                text = formatMs(step.elapsedMs),
                style = GlassTypography.timestamp.copy(
                    color = GlassColors.textMuted,
                    fontSize = 9.sp
                )
            )
        }
    }
}

// ─── Пульсирующая точка ───────────────────────────────────────────────────────

@Composable
private fun PulsingChainDot(color: Color) {
    val t = rememberInfiniteTransition(label = "chain_dot")
    val scale by t.animateFloat(
        0.7f, 1.35f,
        infiniteRepeatable(tween(700, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "s"
    )
    val alpha by t.animateFloat(
        0.4f, 1f,
        infiniteRepeatable(tween(700, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "a"
    )
    Box(
        modifier = Modifier
            .size(6.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(color.copy(alpha = alpha))
    )
}

// ─── Хелперы ─────────────────────────────────────────────────────────────────

private fun formatMs(ms: Long): String = when {
    ms < 1000  -> "${ms}мс"
    ms < 60000 -> "${"%.1f".format(ms / 1000f)}с"
    else       -> "${ms / 60000}м ${(ms % 60000) / 1000}с"
}

private fun stepsWord2(n: Int): String = when {
    n % 10 == 1 && n % 100 != 11 -> "шаг"
    n % 10 in 2..4 && n % 100 !in 12..14 -> "шага"
    else -> "шагов"
}
