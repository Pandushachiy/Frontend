package com.health.companion.presentation.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.health.companion.R
import com.health.companion.data.remote.api.CostPeriod
import com.health.companion.presentation.components.GlassColors
import com.health.companion.presentation.theme.LocalAppTheme
import com.health.companion.presentation.theme.LocalChatBackground

@Composable
fun SettingsDetailScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onBack: () -> Unit = {},
    onOpenAppearance: () -> Unit = {}
) {
    val selectedTheme by viewModel.selectedTheme.collectAsStateWithLifecycle()
    val selectedBackground by viewModel.selectedBackground.collectAsStateWithLifecycle()
    val profileStats by viewModel.profileStats.collectAsStateWithLifecycle()
    val currentTheme = LocalAppTheme.current
    val chatBg = LocalChatBackground.current
    val context = LocalContext.current
    val voicePrefs = remember { context.getSharedPreferences("voice_prefs", android.content.Context.MODE_PRIVATE) }
    var autoSendVoice by remember { mutableStateOf(voicePrefs.getBoolean("auto_send_voice", true)) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(chatBg.gradient)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // ── Header ───────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                    tint = GlassColors.textPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.settings_title),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = GlassColors.textPrimary
            )
        }

        Spacer(Modifier.height(6.dp))

        // ── Внешний вид ──────────────────────────────────────────
        DetailCard {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpenAppearance)
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(
                            Brush.linearGradient(listOf(selectedTheme.primary, selectedTheme.secondary)),
                            RoundedCornerShape(7.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.25f))
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.appearance),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = GlassColors.textPrimary
                    )
                    Text(
                    text = "${selectedTheme.label} · ${selectedBackground.label}",
                    style = MaterialTheme.typography.labelSmall,
                    color = GlassColors.textSecondary,
                    fontSize = 10.sp,
                    lineHeight = 13.sp
                    )
                }
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = GlassColors.textMuted,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(Modifier.height(6.dp))

        // ── Стоимость ─────────────────────────────────────────────
        CostCard(stats = profileStats, onRefresh = { viewModel.refreshProfileStats() })

        Spacer(Modifier.height(6.dp))

        // ── Голосовой ввод ────────────────────────────────────────
        DetailCard {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { autoSendVoice = !autoSendVoice; voicePrefs.edit().putBoolean("auto_send_voice", autoSendVoice).apply() }
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(currentTheme.primary.copy(alpha = 0.14f), RoundedCornerShape(7.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = null,
                        tint = currentTheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.voice_auto_send),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = GlassColors.textPrimary
                    )
                    Text(
                    text = stringResource(R.string.voice_auto_send_subtitle),
                    style = MaterialTheme.typography.labelSmall,
                    color = GlassColors.textTertiary,
                    fontSize = 10.sp,
                    lineHeight = 13.sp
                    )
                }
                Switch(
                    checked = autoSendVoice,
                    onCheckedChange = {
                        autoSendVoice = it
                        voicePrefs.edit().putBoolean("auto_send_voice", it).apply()
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = currentTheme.primary,
                        checkedTrackColor = currentTheme.primary.copy(alpha = 0.35f),
                        uncheckedThumbColor = Color.White.copy(alpha = 0.55f),
                        uncheckedTrackColor = Color.White.copy(alpha = 0.09f)
                    ),
                    modifier = Modifier.height(20.dp)
                )
            }
        }
    }
}

@Composable
private fun CostCard(
    stats: com.health.companion.data.remote.api.ProfileStatsResponse?,
    onRefresh: () -> Unit
) {
    val currentTheme = LocalAppTheme.current
    var expanded by remember { mutableStateOf(false) }
    val costs = stats?.costs

    DetailCard {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(
                            Brush.linearGradient(listOf(Color(0xFF11998e), Color(0xFF38ef7d))),
                            RoundedCornerShape(7.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text("💰", fontSize = 13.sp)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Стоимость",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = GlassColors.textPrimary
                    )
                    Text(
                        text = if (costs != null)
                            "За всё время: ${"%.4f".format(costs.allTime?.usd ?: 0.0)} USD"
                        else "Загрузка...",
                        style = MaterialTheme.typography.labelSmall,
                        color = GlassColors.textSecondary,
                        fontSize = 10.sp,
                        lineHeight = 13.sp
                    )
                }
                Icon(
                    if (expanded) Icons.Default.KeyboardArrowUp
                    else Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = GlassColors.textMuted,
                    modifier = Modifier.size(16.dp)
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                if (costs == null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = currentTheme.primary
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 10.dp, end = 10.dp, bottom = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        HorizontalDivider(
                            color = currentTheme.primary.copy(alpha = 0.08f),
                            thickness = 0.5.dp
                        )
                        costs.allTime?.let { CostPeriodRow("За всё время", it, currentTheme.primary) }
                        costs.last7d?.let { CostPeriodRow("За 7 дней", it, currentTheme.primary) }
                        costs.last24h?.let { CostPeriodRow("За 24 часа", it, currentTheme.primary) }

                        stats.messages?.let { msg ->
                            HorizontalDivider(color = currentTheme.primary.copy(alpha = 0.08f), thickness = 0.5.dp)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Сообщений", style = MaterialTheme.typography.labelSmall, color = GlassColors.textSecondary, fontSize = 10.sp)
                                Text(
                                    "${msg.total} (↑${msg.user} / ↓${msg.assistant})",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Medium,
                                    color = GlassColors.textPrimary,
                                    fontSize = 10.sp
                                )
                            }
                        }
                        stats.conversations?.let { conv ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Диалогов", style = MaterialTheme.typography.labelSmall, color = GlassColors.textSecondary, fontSize = 10.sp)
                                Text(
                                    "${conv.total}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Medium,
                                    color = GlassColors.textPrimary,
                                    fontSize = 10.sp
                                )
                            }
                        }

                        TextButton(
                            onClick = onRefresh,
                            modifier = Modifier.align(Alignment.End).height(24.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                        ) {
                            Text(
                                "Обновить",
                                fontSize = 10.sp,
                                color = currentTheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CostPeriodRow(label: String, period: CostPeriod, accent: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = GlassColors.textSecondary,
                fontSize = 10.sp
            )
            Text(
                text = "${"%.4f".format(period.usd)} USD",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = accent,
                fontSize = 11.sp
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "${formatTokens(period.tokens)} токенов",
                style = MaterialTheme.typography.labelSmall,
                color = GlassColors.textMuted,
                fontSize = 9.sp
            )
            Text(
                text = "${period.requests} запросов",
                style = MaterialTheme.typography.labelSmall,
                color = GlassColors.textMuted,
                fontSize = 9.sp
            )
        }
    }
}

private fun formatTokens(tokens: Long): String = when {
    tokens >= 1_000_000 -> "${"%.1f".format(tokens / 1_000_000.0)}M"
    tokens >= 1_000 -> "${"%.1f".format(tokens / 1_000.0)}K"
    else -> tokens.toString()
}

@Composable
private fun DetailCard(content: @Composable () -> Unit) {
    val chatBg = LocalChatBackground.current
    val theme = LocalAppTheme.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(chatBg.surfaceColor.copy(alpha = 0.85f))
            .background(theme.surfaceTint.copy(alpha = 0.04f))
            .border(0.5.dp, theme.primary.copy(alpha = 0.10f), RoundedCornerShape(10.dp))
    ) {
        content()
    }
}
