package com.health.companion.presentation.screens.skills

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import com.health.companion.data.remote.api.ConfigKeyInfoDTO
import com.health.companion.data.remote.api.SkillDTO
import com.health.companion.presentation.components.*
import com.health.companion.presentation.theme.LocalAppTheme
import com.health.companion.presentation.theme.LocalChatBackground

@Composable
fun SkillsScreen(
    viewModel: SkillsViewModel = hiltViewModel(),
    onBack: () -> Unit = {},
    onOpenMarketplace: () -> Unit = {}
) {
    val skills by viewModel.skills.collectAsState()
    val enabledCount by viewModel.enabledCount.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val configSkill by viewModel.configSkill.collectAsState()
    val revealedKeys by viewModel.revealedKeys.collectAsState()
    val revealingKey by viewModel.revealingKey.collectAsState()
    val revealError by viewModel.revealError.collectAsState()
    val editingConfigKey by viewModel.editingConfigKey.collectAsState()
    val currentTheme = LocalAppTheme.current
    val chatBg = LocalChatBackground.current

    val sortedSkills = remember(skills) {
        skills.sortedWith(compareByDescending<SkillDTO> { it.enabled }.thenBy { it.name })
    }

    configSkill?.let { skill ->
        ConfigDialog(
            skill = skill,
            accentColor = currentTheme.primary,
            onDismiss = { viewModel.hideConfig() },
            onSave = { key, value -> viewModel.setConfigKey(skill.name, key, value) }
        )
    }

    editingConfigKey?.let { (skillName, key) ->
        val skill = skills.find { it.name == skillName }
        if (skill != null) {
            EditSingleKeyDialog(
                skillName = skill.resolvedDisplayName(),
                keyName = key,
                currentMasked = skill.configSet[key]?.maskedValue,
                accentColor = currentTheme.primary,
                onDismiss = { viewModel.cancelEditingKey() },
                onSave = { value -> viewModel.setConfigKey(skillName, key, value) }
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(chatBg.gradient)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад", tint = Color.White)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Навыки",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    if (skills.isNotEmpty()) {
                        Text(
                            "$enabledCount из ${skills.size} активно",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.45f),
                            fontSize = 10.sp
                        )
                    }
                }
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(20.dp)
                            .padding(end = 8.dp),
                        strokeWidth = 2.dp,
                        color = currentTheme.primary
                    )
                } else {
                    IconButton(onClick = { viewModel.loadAll() }) {
                        Icon(Icons.Default.Refresh, "Обновить", tint = Color.White.copy(alpha = 0.45f))
                    }
                }
            }

            AnimatedVisibility(visible = error != null) {
                error?.let { msg ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFF87171).copy(alpha = 0.10f))
                            .border(1.dp, Color(0xFFF87171).copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                            .clickable { viewModel.clearError() }
                            .padding(12.dp)
                    ) {
                        Text(msg, style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFF87171)))
                    }
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .navigationBarsPadding(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                item(key = "marketplace_promo") {
                    MarketplacePromoCard(
                        accentColor = currentTheme.primary,
                        secondaryColor = currentTheme.secondary,
                        onClick = onOpenMarketplace
                    )
                }

                if (skills.isEmpty() && !isLoading) {
                    item(key = "empty") {
                        EmptySkillsCard(
                            accentGradient = currentTheme.accentGradient,
                            onOpenMarketplace = onOpenMarketplace
                        )
                    }
                } else {
                    if (skills.isNotEmpty()) {
                        item(key = "skills_header") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 6.dp, bottom = 2.dp, start = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    "Установленные",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White.copy(alpha = 0.55f)
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    CountBadge("$enabledCount активно", currentTheme.primary)
                                    if (skills.size - enabledCount > 0) {
                                        CountBadge("${skills.size - enabledCount} выкл", Color.White.copy(alpha = 0.3f))
                                    }
                                }
                            }
                        }
                    }

                    items(sortedSkills, key = { "skill_${it.name}" }) { skill ->
                        SkillCard(
                            skill = skill,
                            accentColor = currentTheme.primary,
                            secondaryColor = currentTheme.secondary,
                            revealedKeys = revealedKeys,
                            revealingKey = revealingKey,
                            revealError = revealError,
                            onToggle = { viewModel.toggle(skill.name) },
                            onDelete = { viewModel.delete(skill.name) },
                            onConfig = { viewModel.showConfig(skill) },
                            onRevealKey = { key -> viewModel.revealConfigKey(skill.name, key) },
                            onEditKey = { key -> viewModel.startEditingKey(skill.name, key) }
                        )
                    }
                }

                item(key = "bottom_spacer") { Spacer(Modifier.height(16.dp)) }
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════════════════════
// Marketplace Promo
// ════════════════════════════════════════════════════════════════════════════════

@Composable
private fun MarketplacePromoCard(accentColor: Color, secondaryColor: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Brush.linearGradient(listOf(accentColor.copy(alpha = 0.12f), secondaryColor.copy(alpha = 0.07f))))
            .border(0.5.dp, accentColor.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(Brush.linearGradient(listOf(accentColor, secondaryColor))),
                contentAlignment = Alignment.Center
            ) { Text("🛒", fontSize = 14.sp) }
            Column(Modifier.weight(1f)) {
                Text("Маркетплейс навыков", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = Color.White)
                Text("Найдите и установите навыки из ClawHub", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.5f), fontSize = 10.sp)
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = accentColor, modifier = Modifier.size(16.dp))
        }
    }
}

// ════════════════════════════════════════════════════════════════════════════════
// Skill Card
// ════════════════════════════════════════════════════════════════════════════════

@Composable
private fun SkillCard(
    skill: SkillDTO,
    accentColor: Color,
    secondaryColor: Color,
    revealedKeys: Map<String, String>,
    revealingKey: String?,
    revealError: String?,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    onConfig: () -> Unit,
    onRevealKey: (String) -> Unit,
    onEditKey: (String) -> Unit
) {
    val needsConfig = skill.requiresConfig.isNotEmpty() && !skill.isConfigured
    var expanded by remember { mutableStateOf(false) }
    val arrowAngle by animateFloatAsState(if (expanded) 180f else 0f, tween(200), label = "arrow")
    val hasConfig = skill.configSet.isNotEmpty() || skill.requiresConfig.isNotEmpty()

    val borderColor = when {
        needsConfig -> Color(0xFFFBBF24).copy(alpha = 0.35f)
        skill.enabled -> accentColor.copy(alpha = 0.25f)
        else -> Color.White.copy(alpha = 0.06f)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White.copy(alpha = 0.06f))
            .border(0.5.dp, borderColor, RoundedCornerShape(10.dp))
    ) {
        Column {
            // ── Header ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (skill.enabled) Brush.linearGradient(
                                listOf(accentColor.copy(alpha = 0.20f), secondaryColor.copy(alpha = 0.10f))
                            ) else Brush.linearGradient(
                                listOf(Color.White.copy(alpha = 0.06f), Color.White.copy(alpha = 0.03f))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) { Text(skill.icon ?: "🔌", fontSize = 15.sp) }

                Spacer(Modifier.width(10.dp))

                Column(Modifier.weight(1f)) {
                    Text(
                        skill.resolvedDisplayName(),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        modifier = Modifier.padding(top = 1.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        when {
                            needsConfig -> StatusDot("⚙ Настройка", Color(0xFFFBBF24))
                            skill.enabled -> StatusDot("● Активен", accentColor)
                            else -> StatusDot("○ Выключен", Color.White.copy(alpha = 0.3f))
                        }
                        if (skill.triggerWords.isNotEmpty()) {
                            Text(
                                "${skill.triggerWords.size} триггер${triggerSuffix(skill.triggerWords.size)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.3f),
                                fontSize = 10.sp
                            )
                        }
                    }
                }

                Switch(
                    checked = skill.enabled,
                    onCheckedChange = { if (!needsConfig) onToggle() },
                    enabled = !needsConfig,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = accentColor.copy(alpha = 0.75f),
                        uncheckedThumbColor = Color.White.copy(alpha = 0.4f),
                        uncheckedTrackColor = Color.White.copy(alpha = 0.10f),
                        disabledCheckedTrackColor = Color(0xFFFBBF24).copy(alpha = 0.3f),
                        disabledUncheckedTrackColor = Color.White.copy(alpha = 0.08f)
                    ),
                    modifier = Modifier.height(24.dp).padding(start = 4.dp)
                )

                Icon(
                    Icons.Default.ExpandMore, null,
                    tint = Color.White.copy(alpha = 0.3f),
                    modifier = Modifier.size(16.dp).rotate(arrowAngle)
                )
            }

            // ── Expanded ──
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(tween(150)),
                exit = shrinkVertically() + fadeOut(tween(150))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White.copy(alpha = 0.03f))
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (skill.description.isNotBlank()) {
                        Text(
                            skill.description,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.5f),
                            lineHeight = 14.sp,
                            fontSize = 11.sp
                        )
                    }

                    if (skill.triggerWords.isNotEmpty()) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            items(skill.triggerWords) { word ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(accentColor.copy(alpha = 0.10f))
                                        .border(0.5.dp, accentColor.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(word, style = MaterialTheme.typography.labelSmall, color = accentColor, fontSize = 10.sp)
                                }
                            }
                        }
                    }

                    // ── Config Section ──
                    if (skill.configSet.isNotEmpty()) {
                        // Separator
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
                                .height(0.5.dp)
                                .background(Color.White.copy(alpha = 0.06f))
                        )

                        skill.configSet.forEach { (key, info) ->
                            ConfigKeyRow(
                                skillName = skill.name,
                                keyName = key,
                                info = info,
                                accentColor = accentColor,
                                revealedKeys = revealedKeys,
                                revealingKey = revealingKey,
                                revealError = revealError,
                                onReveal = { onRevealKey(key) },
                                onEdit = { onEditKey(key) }
                            )
                        }
                    } else if (skill.requiresConfig.isNotEmpty()) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
                                .height(0.5.dp)
                                .background(Color.White.copy(alpha = 0.06f))
                        )
                        skill.requiresConfig.forEach { key ->
                            val value = skill.config[key]
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(key, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.5f))
                                Text(
                                    if (value.isNullOrBlank()) "не задано" else "••••••",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (value.isNullOrBlank()) Color(0xFFFBBF24) else accentColor
                                )
                            }
                        }
                    }

                    // ── Actions ──
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (hasConfig) {
                            SmallActionButton("Настроить", accentColor, Modifier.weight(1f), onClick = onConfig)
                        }
                        SmallActionButton("Удалить", Color(0xFFF87171), Modifier.weight(1f), onClick = onDelete)
                    }
                }
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════════════════════
// Config Key Row — clean inline display
// ════════════════════════════════════════════════════════════════════════════════

@Composable
private fun ConfigKeyRow(
    skillName: String,
    keyName: String,
    info: ConfigKeyInfoDTO,
    accentColor: Color,
    revealedKeys: Map<String, String>,
    revealingKey: String?,
    revealError: String?,
    onReveal: () -> Unit,
    onEdit: () -> Unit
) {
    val compositeKey = "$skillName::$keyName"
    val isRevealing = revealingKey == compositeKey
    val revealedValue = revealedKeys[compositeKey]
    val isRevealed = revealedValue != null
    val hasRevealError = revealError == compositeKey

    val statusColor = if (info.filled) accentColor else Color(0xFFFBBF24)

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        // Key name + status
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Status dot
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(statusColor)
            )
            Spacer(Modifier.width(8.dp))

            Text(
                keyName,
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.65f),
                fontSize = 11.sp,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (info.filled) {
                // Action icons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Reveal/hide
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(alpha = 0.05f))
                            .clickable(enabled = !isRevealing) { onReveal() },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isRevealing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(12.dp),
                                strokeWidth = 1.5.dp,
                                color = Color.White.copy(alpha = 0.4f)
                            )
                        } else {
                            Icon(
                                if (isRevealed) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                contentDescription = if (isRevealed) "Скрыть" else "Показать",
                                tint = Color.White.copy(alpha = 0.4f),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                    // Edit
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(accentColor.copy(alpha = 0.10f))
                            .clickable { onEdit() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Outlined.Edit,
                            contentDescription = "Изменить",
                            tint = accentColor.copy(alpha = 0.7f),
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            } else {
                // Not configured badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFFFBBF24).copy(alpha = 0.10f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text("не задано", fontSize = 10.sp, color = Color(0xFFFBBF24).copy(alpha = 0.8f), fontWeight = FontWeight.Medium)
                }
            }
        }

        // Value line (masked or revealed)
        if (info.filled) {
            if (isRevealed && revealedValue != null) {
                SelectionContainer {
                    Text(
                        revealedValue,
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                        color = accentColor.copy(alpha = 0.85f),
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(start = 14.dp)
                    )
                }
            } else if (hasRevealError) {
                Text(
                    info.maskedValue ?: "••••••••",
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                    color = Color.White.copy(alpha = 0.25f),
                    fontSize = 11.sp,
                    modifier = Modifier.padding(start = 14.dp)
                )
                Text(
                    "Просмотр пока недоступен",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.2f),
                    fontSize = 9.sp,
                    modifier = Modifier.padding(start = 14.dp)
                )
            } else {
                Text(
                    info.maskedValue ?: "••••••••",
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                    color = Color.White.copy(alpha = 0.25f),
                    fontSize = 11.sp,
                    modifier = Modifier.padding(start = 14.dp)
                )
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════════════════════
// Empty State
// ════════════════════════════════════════════════════════════════════════════════

@Composable
private fun EmptySkillsCard(accentGradient: Brush, onOpenMarketplace: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("🔌", fontSize = 32.sp)
            Text("Нет установленных навыков", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = Color.White)
            Text("Навыки расширяют возможности AI", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.4f), fontSize = 10.sp)
            Spacer(Modifier.height(2.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(accentGradient)
                    .clickable { onOpenMarketplace() }
                    .padding(horizontal = 20.dp, vertical = 9.dp)
            ) {
                Text("Открыть маркетплейс", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = Color.White)
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════════════════════
// Config Dialog (multi-key — initial setup)
// ════════════════════════════════════════════════════════════════════════════════

@Composable
private fun ConfigDialog(
    skill: SkillDTO,
    accentColor: Color,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var configValues by remember {
        mutableStateOf(skill.requiresConfig.associateWith { skill.config[it] ?: "" })
    }
    val chatBg = LocalChatBackground.current

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(chatBg.surfaceColor.copy(alpha = 0.97f))
                .border(0.5.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(skill.icon ?: "🔌", fontSize = 16.sp)
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text("Настройка", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.4f), fontSize = 10.sp)
                        Text(
                            skill.resolvedDisplayName(),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                skill.requiresConfig.forEach { key ->
                    OutlinedTextField(
                        value = configValues[key] ?: "",
                        onValueChange = { configValues = configValues + (key to it) },
                        label = { Text(key, fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = MaterialTheme.typography.bodySmall.copy(color = Color.White),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = accentColor,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                            focusedLabelColor = accentColor,
                            unfocusedLabelColor = Color.White.copy(alpha = 0.35f),
                            cursorColor = accentColor,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )
                    Spacer(Modifier.height(8.dp))
                }

                Spacer(Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Отмена", color = Color.White.copy(alpha = 0.35f), fontSize = 12.sp)
                    }
                    Spacer(Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(Brush.linearGradient(listOf(accentColor, accentColor.copy(alpha = 0.8f))))
                            .clickable {
                                configValues.forEach { (key, value) ->
                                    if (value.isNotBlank()) onSave(key, value)
                                }
                            }
                            .padding(horizontal = 18.dp, vertical = 8.dp)
                    ) {
                        Text("Сохранить", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = Color.White)
                    }
                }
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════════════════════
// Edit Single Key Dialog
// ════════════════════════════════════════════════════════════════════════════════

@Composable
private fun EditSingleKeyDialog(
    skillName: String,
    keyName: String,
    currentMasked: String?,
    accentColor: Color,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var newValue by remember { mutableStateOf("") }
    val chatBg = LocalChatBackground.current

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(chatBg.surfaceColor.copy(alpha = 0.97f))
                .border(0.5.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(7.dp))
                            .background(accentColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.Edit, null, tint = accentColor, modifier = Modifier.size(14.dp))
                    }
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(skillName, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.4f), fontSize = 10.sp)
                        Text(
                            keyName,
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                }

                if (currentMasked != null) {
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(alpha = 0.04f))
                            .border(0.5.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Текущее", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.3f), fontSize = 10.sp)
                        Spacer(Modifier.width(6.dp))
                        Text(
                            currentMasked,
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                            color = Color.White.copy(alpha = 0.4f),
                            fontSize = 10.sp
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = newValue,
                    onValueChange = { newValue = it },
                    label = { Text("Новое значение", fontSize = 11.sp) },
                    placeholder = { Text("Вставьте ключ…", color = Color.White.copy(alpha = 0.15f), fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = MaterialTheme.typography.bodySmall.copy(color = Color.White),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = accentColor,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                        focusedLabelColor = accentColor,
                        unfocusedLabelColor = Color.White.copy(alpha = 0.35f),
                        cursorColor = accentColor,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )

                Spacer(Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Отмена", color = Color.White.copy(alpha = 0.35f), fontSize = 12.sp)
                    }
                    Spacer(Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (newValue.isNotBlank()) Brush.linearGradient(listOf(accentColor, accentColor.copy(alpha = 0.8f)))
                                else Brush.linearGradient(listOf(accentColor.copy(alpha = 0.2f), accentColor.copy(alpha = 0.15f)))
                            )
                            .then(if (newValue.isNotBlank()) Modifier.clickable { onSave(newValue) } else Modifier)
                            .padding(horizontal = 18.dp, vertical = 8.dp)
                    ) {
                        Text(
                            "Сохранить",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White.copy(alpha = if (newValue.isNotBlank()) 1f else 0.3f)
                        )
                    }
                }
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════════════════════
// Helpers
// ════════════════════════════════════════════════════════════════════════════════

@Composable
private fun StatusDot(text: String, color: Color) {
    Text(text, style = MaterialTheme.typography.labelSmall, color = color, fontSize = 10.sp, fontWeight = FontWeight.Medium)
}

@Composable
private fun CountBadge(text: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 7.dp, vertical = 3.dp)
    ) {
        Text(text, style = MaterialTheme.typography.labelSmall, color = color, fontWeight = FontWeight.Medium, fontSize = 10.sp)
    }
}

@Composable
private fun SmallActionButton(text: String, color: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.08f))
            .border(0.5.dp, color.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, style = MaterialTheme.typography.labelSmall, color = color, fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
    }
}

private fun triggerSuffix(count: Int): String = when {
    count % 10 == 1 && count % 100 != 11 -> ""
    count % 10 in 2..4 && count % 100 !in 12..14 -> "а"
    else -> "ов"
}
