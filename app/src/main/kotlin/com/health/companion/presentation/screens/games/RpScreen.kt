package com.health.companion.presentation.screens.games

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.core.view.HapticFeedbackConstantsCompat
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.defaultMinSize
import androidx.core.content.ContextCompat
import com.health.companion.data.remote.api.RpSessionCard
import com.health.companion.presentation.components.GlassColors
import com.health.companion.presentation.components.GlassElevation
import com.health.companion.presentation.components.GlassShapes
import com.health.companion.presentation.components.GlassSpacing
import com.health.companion.presentation.components.GlassTypography
import com.health.companion.presentation.theme.LocalAppTheme
import com.health.companion.presentation.theme.LocalChatBackground
import dev.chrisbanes.haze.HazeProgressive
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials

// ═══════════════════════════════════════════════════════════════
// ROOT
// ═══════════════════════════════════════════════════════════════

@Composable
fun RpScreen(
    viewModel: RpViewModel,
    bottomPadding: Dp = 0.dp,
    onBack: () -> Unit = {}
) {
    val phase by viewModel.rpPhase.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val loaderText by viewModel.loaderText.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        when (phase) {
            RpPhase.GALLERY -> RpGalleryContent(
                viewModel = viewModel,
                bottomPadding = bottomPadding,
                onBack = onBack
            )
            RpPhase.SETUP   -> RpSetupContent(viewModel = viewModel, bottomPadding = bottomPadding)
            RpPhase.CHAT    -> RpChatContent(viewModel = viewModel, bottomPadding = bottomPadding)
        }

        if (isLoading) {
            if (loaderText.contains("Создаём") || loaderText.contains("Создаем")) {
                RpCreationAnimation()
            } else {
                Box(
                    modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.55f)),
                    contentAlignment = Alignment.Center
                ) { RpLoaderCard(text = loaderText) }
            }
        }

        errorMessage?.let { msg ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 16.dp, vertical = 12.dp + bottomPadding)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(GlassColors.error.copy(alpha = 0.15f))
                        .border(1.dp, GlassColors.error.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.ErrorOutline, null, tint = GlassColors.error, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(msg, style = TextStyle(fontSize = 13.sp, color = Color.White.copy(alpha = 0.9f)), modifier = Modifier.weight(1f))
                    IconButton(onClick = { viewModel.clearError() }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, null, tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(14.dp))
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// GALLERY
// ═══════════════════════════════════════════════════════════════

@Composable
private fun RpGalleryContent(
    viewModel: RpViewModel,
    bottomPadding: Dp,
    onBack: () -> Unit
) {
    val theme = LocalAppTheme.current
    val chatBg = LocalChatBackground.current
    val sessions by viewModel.sessions.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize().statusBarsPadding().padding(bottom = bottomPadding)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack, null,
                    tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(2.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Ролевые игры",
                    style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            item(key = "new") {
                NewRpCard(theme = theme, onClick = { viewModel.openSetup() })
            }

            items(sessions, key = { it.sessionId }) { card ->
                RpSessionCardItem(
                    card = card,
                    theme = theme,
                    chatBg = chatBg,
                    onClick = { viewModel.openSession(card) },
                    onDelete = { viewModel.deleteSession(card.sessionId) }
                )
            }
        }
    }
}

@Composable
private fun NewRpCard(
    theme: com.health.companion.presentation.theme.AppThemeOption,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(GlassShapes.medium)
            .background(theme.primary.copy(alpha = 0.10f), GlassShapes.medium)
            .border(0.5.dp, theme.primary.copy(alpha = 0.30f), GlassShapes.medium)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(28.dp).clip(CircleShape)
                .background(theme.primary.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Add, null, tint = theme.primary, modifier = Modifier.size(16.dp))
        }
        Spacer(Modifier.width(10.dp))
        Text(
            "Новая история",
            style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = theme.primary)
        )
    }
}

@Composable
private fun RpSessionCardItem(
    card: RpSessionCard,
    theme: com.health.companion.presentation.theme.AppThemeOption,
    chatBg: com.health.companion.presentation.theme.ChatBackgroundOption,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(chatBg.surfaceColor.copy(alpha = 0.55f))
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp).clip(CircleShape)
                .background(Brush.linearGradient(listOf(theme.primary.copy(alpha = 0.25f), theme.secondary.copy(alpha = 0.15f))))
                .border(1.dp, theme.primary.copy(alpha = 0.25f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                card.charName.firstOrNull()?.uppercase() ?: "?",
                style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold, color = theme.primary)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(card.charName, style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.White))
                if (!card.modelName.isNullOrBlank()) {
                    RpModelBadge(name = card.modelName, theme = theme)
                }
            }
            // Показываем только сюжет: убираем "RP: {имя} — " из начала title
            val scenarioText = card.title
                ?.substringAfter(" — ", missingDelimiterValue = "")
                ?.takeIf { it.isNotBlank() }
                ?: card.title?.removePrefix("RP: ")?.trim()
            if (!scenarioText.isNullOrBlank()) {
                Text(
                    scenarioText,
                    style = TextStyle(fontSize = 12.sp, color = Color.White.copy(alpha = 0.5f)),
                    maxLines = 1
                )
            }
        }

        if (showDeleteConfirm) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(
                    modifier = Modifier
                        .size(34.dp).clip(RoundedCornerShape(8.dp))
                        .background(GlassColors.error.copy(alpha = 0.25f))
                        .border(1.dp, GlassColors.error.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .clickable { onDelete(); showDeleteConfirm = false },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Check, null, tint = GlassColors.error, modifier = Modifier.size(18.dp))
                }
                Box(
                    modifier = Modifier
                        .size(34.dp).clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                        .clickable { showDeleteConfirm = false },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Close, null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .size(34.dp).clip(RoundedCornerShape(8.dp))
                    .background(GlassColors.error.copy(alpha = 0.12f))
                    .border(1.dp, GlassColors.error.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                    .clickable { showDeleteConfirm = true },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.DeleteOutline, null, tint = GlassColors.error.copy(alpha = 0.85f), modifier = Modifier.size(18.dp))
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// MODEL CARD
// ═══════════════════════════════════════════════════════════════

@Composable
// Цвет бейджа по base-архитектуре
private fun baseColor(base: String?): Color = when (base?.lowercase()) {
    "qwen" -> Color(0xFF26C6DA)   // бирюзовый
    "llama3", "llama" -> Color(0xFFFF8C42) // оранжевый
    else -> Color(0xFF9E9E9E)
}

@Composable
private fun RpModelDropdown(
    models: List<com.health.companion.data.remote.api.RpModel>,
    selectedKey: String?,
    isLoading: Boolean,
    theme: com.health.companion.presentation.theme.AppThemeOption,
    chatBg: com.health.companion.presentation.theme.ChatBackgroundOption,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = models.firstOrNull { it.modelKey == selectedKey } ?: models.firstOrNull()
    val shape = RoundedCornerShape(10.dp)

    Column {
        // ── Закрытый селектор ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(chatBg.surfaceColor.copy(alpha = 0.55f))
                .border(1.dp,
                    if (expanded) theme.primary.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.10f),
                    shape)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { if (!isLoading) expanded = !expanded }
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = theme.primary, strokeWidth = 2.dp)
                Spacer(Modifier.width(10.dp))
                Text("Загружаем модели...", style = TextStyle(fontSize = 13.sp, color = Color.White.copy(alpha = 0.35f)))
            } else if (selected != null) {
                // Бейдж base
                val bColor = baseColor(selected.base)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(bColor.copy(alpha = 0.15f))
                        .border(0.5.dp, bColor.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Text(
                        selected.base?.uppercase() ?: "AI",
                        style = TextStyle(fontSize = 9.sp, color = bColor, fontWeight = FontWeight.Bold)
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    selected.displayName,
                    modifier = Modifier.weight(1f),
                    style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                )
            } else {
                Text("Выбрать модель", modifier = Modifier.weight(1f),
                    style = TextStyle(fontSize = 13.sp, color = Color.White.copy(alpha = 0.4f)))
            }
            Spacer(Modifier.width(6.dp))
            Icon(
                if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                null,
                tint = Color.White.copy(alpha = 0.4f),
                modifier = Modifier.size(18.dp)
            )
        }

        // ── Выпадающий список ──
        AnimatedVisibility(
            visible = expanded && models.isNotEmpty(),
            enter = expandVertically(animationSpec = tween(200)) + fadeIn(tween(150)),
            exit = shrinkVertically(animationSpec = tween(200)) + fadeOut(tween(150))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
                    .clip(shape)
                    .background(chatBg.surfaceColor.copy(alpha = 0.85f))
                    .border(1.dp, Color.White.copy(alpha = 0.07f), shape),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                models.forEachIndexed { index, model ->
                    val isSelected = model.modelKey == selectedKey
                    val bColor = baseColor(model.base)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(
                                if (index < models.lastIndex)
                                    Modifier.border(
                                        width = 0.5.dp,
                                        color = Color.White.copy(alpha = 0.05f),
                                        shape = RoundedCornerShape(0.dp)
                                    )
                                else Modifier
                            )
                            .background(if (isSelected) theme.primary.copy(alpha = 0.10f) else Color.Transparent)
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            ) {
                                onSelect(model.modelKey)
                                expanded = false
                            }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Радио-точка
                        Box(
                            modifier = Modifier.size(16.dp).clip(CircleShape)
                                .border(1.5.dp,
                                    if (isSelected) theme.primary else Color.White.copy(alpha = 0.2f),
                                    CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) Box(Modifier.size(8.dp).clip(CircleShape).background(theme.primary))
                        }
                        Spacer(Modifier.width(10.dp))
                        // Бейдж base
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(bColor.copy(alpha = 0.12f))
                                .border(0.5.dp, bColor.copy(alpha = 0.35f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 5.dp, vertical = 2.dp)
                        ) {
                            Text(model.base?.uppercase() ?: "AI",
                                style = TextStyle(fontSize = 9.sp, color = bColor, fontWeight = FontWeight.Bold))
                        }
                        Spacer(Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                model.displayName,
                                style = TextStyle(
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isSelected) theme.primary else Color.White.copy(alpha = 0.85f)
                                )
                            )
                            if (!model.description.isNullOrBlank()) {
                                Text(
                                    model.description,
                                    style = TextStyle(fontSize = 11.sp, color = Color.White.copy(alpha = 0.38f)),
                                    maxLines = 2
                                )
                            }
                        }
                    }
                    if (index < models.lastIndex) {
                        Box(Modifier.fillMaxWidth().height(0.5.dp).background(Color.White.copy(alpha = 0.05f)))
                    }
                }
            }
        }
    }
}

@Composable
private fun RpModelCard(
    model: com.health.companion.data.remote.api.RpModel,
    isSelected: Boolean,
    theme: com.health.companion.presentation.theme.AppThemeOption,
    chatBg: com.health.companion.presentation.theme.ChatBackgroundOption,
    onClick: () -> Unit
) {
    val bColor = baseColor(model.base)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) theme.primary.copy(alpha = 0.14f) else chatBg.surfaceColor.copy(alpha = 0.4f))
            .border(1.dp, if (isSelected) theme.primary.copy(alpha = 0.55f) else Color.White.copy(alpha = 0.07f), RoundedCornerShape(10.dp))
            .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(16.dp).clip(CircleShape)
            .border(1.5.dp, if (isSelected) theme.primary else Color.White.copy(alpha = 0.25f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) Box(Modifier.size(8.dp).clip(CircleShape).background(theme.primary))
        }
        Spacer(Modifier.width(10.dp))
        if (model.base != null) {
            Box(modifier = Modifier.clip(RoundedCornerShape(4.dp))
                .background(bColor.copy(alpha = 0.12f))
                .border(0.5.dp, bColor.copy(alpha = 0.35f), RoundedCornerShape(4.dp))
                .padding(horizontal = 5.dp, vertical = 2.dp)
            ) {
                Text(model.base.uppercase(), style = TextStyle(fontSize = 9.sp, color = bColor, fontWeight = FontWeight.Bold))
            }
            Spacer(Modifier.width(8.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(model.displayName, style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                color = if (isSelected) theme.primary else Color.White.copy(alpha = 0.85f)))
            if (!model.description.isNullOrBlank()) {
                Text(model.description, style = TextStyle(fontSize = 11.sp, color = Color.White.copy(alpha = 0.4f)), maxLines = 2)
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// SETUP
// ═══════════════════════════════════════════════════════════════

@Composable
private fun RpSetupContent(viewModel: RpViewModel, bottomPadding: Dp) {
    val theme = LocalAppTheme.current
    val chatBg = LocalChatBackground.current

    var theme_text by remember { mutableStateOf("") }
    var charName by remember { mutableStateOf("") }
    var charDesc by remember { mutableStateOf("") }
    var userName by remember { mutableStateOf("") }
    var userDesc by remember { mutableStateOf("") }

    val models by viewModel.models.collectAsState()
    val selectedModelKey by viewModel.selectedModelKey.collectAsState()
    val isLoadingModels by viewModel.isLoadingModels.collectAsState()

    val canCreate = theme_text.isNotBlank() && charName.isNotBlank() &&
            charDesc.isNotBlank() && userName.isNotBlank() && userDesc.isNotBlank()

    // Единый монолитный Column — шапка фиксирована, всё остальное скроллируется
    Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
        // Шапка (не двигается)
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.navigateBack() }, modifier = Modifier.size(36.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(4.dp))
            Column {
                Text("Новая история", style = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = Color.White))
                Text("Настрой персонажей", style = TextStyle(fontSize = 11.sp, color = Color.White.copy(alpha = 0.4f)))
            }
        }

        // Весь контент включая кнопку — единый скролл
        // imePadding здесь добавляет паддинг снизу = высота клавиатуры,
        // поэтому контент скроллируется вверх, а не фрагментируется
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 14.dp)
                .padding(top = 4.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            RpSectionLabel("Сценарий")
            RpTextField(
                value = theme_text,
                onValueChange = { theme_text = it },
                placeholder = "Готический замок, детектив 1890е...",
                minLines = 4,
                theme = theme,
                chatBg = chatBg
            )

            RpSectionCard(theme = theme, chatBg = chatBg) {
                Text("Персонаж ИИ", style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = theme.primary))
                Spacer(Modifier.height(8.dp))
                RpTextField(value = charName, onValueChange = { charName = it }, placeholder = "Имя персонажа", theme = theme, chatBg = chatBg)
                Spacer(Modifier.height(6.dp))
                RpTextField(value = charDesc, onValueChange = { charDesc = it }, placeholder = "Таинственная хозяйка замка, скрывает тёмный секрет...", minLines = 3, theme = theme, chatBg = chatBg)
                Spacer(Modifier.height(3.dp))
                if (charDesc.isNotBlank() && charDesc.length < 10) {
                    Text("минимум 10 символов", style = TextStyle(fontSize = 10.sp, color = GlassColors.error.copy(alpha = 0.7f)))
                } else {
                    Text("минимум 10 символов", style = TextStyle(fontSize = 10.sp, color = Color.White.copy(alpha = 0.25f)))
                }
            }

            RpSectionCard(theme = theme, chatBg = chatBg) {
                Text("Твой персонаж", style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = theme.primary))
                Spacer(Modifier.height(8.dp))
                RpTextField(value = userName, onValueChange = { userName = it }, placeholder = "Твоё имя в истории", theme = theme, chatBg = chatBg)
                Spacer(Modifier.height(6.dp))
                RpTextField(value = userDesc, onValueChange = { userDesc = it }, placeholder = "Молодой следователь, тёмные волосы, всегда в пальто...", minLines = 3, theme = theme, chatBg = chatBg)
                Spacer(Modifier.height(3.dp))
                if (userDesc.isNotBlank() && userDesc.length < 10) {
                    Text("минимум 10 символов", style = TextStyle(fontSize = 10.sp, color = GlassColors.error.copy(alpha = 0.7f)))
                } else {
                    Text("минимум 10 символов", style = TextStyle(fontSize = 10.sp, color = Color.White.copy(alpha = 0.25f)))
                }
            }

            // ── Выбор модели — выпадающий список ──
            RpSectionLabel("Модель ИИ")
            RpModelDropdown(
                models = models,
                selectedKey = selectedModelKey,
                isLoading = isLoadingModels,
                theme = theme,
                chatBg = chatBg,
                onSelect = { viewModel.selectModel(it) }
            )

            // Кнопка — часть скролла, не отдельный блок
            Spacer(Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth().height(50.dp).clip(RoundedCornerShape(14.dp))
                    .then(
                        if (canCreate)
                            Modifier.background(Brush.linearGradient(listOf(theme.primary, theme.secondary)))
                        else
                            Modifier.background(Color.White.copy(alpha = 0.08f))
                    )
                    .clickable(enabled = canCreate) {
                        viewModel.createSession(theme_text, charName, charDesc, userName, userDesc)
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Создать персонажа",
                    style = TextStyle(
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (canCreate) Color.White else Color.White.copy(alpha = 0.3f)
                    )
                )
            }
            Text(
                "ИИ создаст Character Card для персонажа (~3-5 сек)",
                style = TextStyle(fontSize = 10.sp, color = Color.White.copy(alpha = 0.22f)),
                modifier = Modifier.fillMaxWidth()
            )
            // Отступ внизу для навигационной панели
            Spacer(Modifier.height(bottomPadding + 8.dp))
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// CHAT
// ═══════════════════════════════════════════════════════════════

@Composable
private fun RpChatContent(viewModel: RpViewModel, bottomPadding: Dp) {
    val theme = LocalAppTheme.current
    val chatBg = LocalChatBackground.current
    val context = LocalContext.current

    val charName by viewModel.charName.collectAsState()
    val tone by viewModel.tone.collectAsState()
    val currentModelName by viewModel.currentModelName.collectAsState()
    val messages by viewModel.chatMessages.collectAsState()
    val isStreaming by viewModel.isStreaming.collectAsState()
    val streamingText by viewModel.streamingText.collectAsState()
    val isUpdatingRoles by viewModel.isUpdatingRoles.collectAsState()
    val isRecording by viewModel.isRecording.collectAsState()

    var messageInput by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    var showEditRoles by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val reversedMessages = remember(messages) { messages.reversed() }

    // IME offset — точно как в ChatScreen
    val density = LocalDensity.current
    val imeBottomDp = with(density) { WindowInsets.ime.getBottom(density).toDp() }
    val imeOffset = if (imeBottomDp > bottomPadding) imeBottomDp - bottomPadding + 5.dp else 0.dp
    val effectiveBottom = bottomPadding + imeOffset

    var headerHeightPx by remember { mutableStateOf(0) }
    val headerHeightDp = with(density) { headerHeightPx.toDp() }
    var bottomAreaHeightPx by remember { mutableStateOf(0) }
    val bottomAreaHeightDp = with(density) { bottomAreaHeightPx.toDp() }

    val hazeState = remember { HazeState() }

    // Голосовые разрешения
    var hasAudioPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)
    }
    val audioPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasAudioPermission = granted
    }

    LaunchedEffect(messages.size, streamingText) {
        if (messages.isNotEmpty() || streamingText.isNotBlank()) {
            listState.animateScrollToItem(0)
        }
    }
    LaunchedEffect(effectiveBottom) {
        if (messages.isNotEmpty() || streamingText.isNotBlank()) {
            listState.scrollToItem(0)
        }
    }

    // ── Структура точно как в основном чате ──
    // Outer Box — позиционирующий контейнер
    Box(modifier = Modifier.fillMaxSize()) {

        // ── 1. hazeSource Box: ОБЯЗАТЕЛЬНО с фоновым градиентом — источник пикселей для блюра ──
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(chatBg.gradient)
                .hazeSource(state = hazeState)
        ) {
            LazyColumn(
                state = listState,
                reverseLayout = true,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = GlassSpacing.screenEdge,
                    end = GlassSpacing.screenEdge,
                    bottom = bottomAreaHeightDp + effectiveBottom + 8.dp,
                    top = headerHeightDp + 8.dp
                ),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (streamingText.isNotBlank()) {
                    item(key = "streaming") {
                        RpBubble(
                            msg = RpChatMessage("streaming", "assistant", streamingText),
                            charName = charName, isStreaming = true,
                            theme = theme, chatBg = chatBg
                        )
                    }
                }
                if (isStreaming && streamingText.isBlank()) {
                    item(key = "typing") { RpTypingIndicator(charName = charName, theme = theme) }
                }
                items(reversedMessages, key = { it.id }) { msg ->
                    RpBubble(msg = msg, charName = charName, theme = theme, chatBg = chatBg)
                }
            }
        }

        // ── 2. Оверлей ввода — сиблинг hazeSource, поднимается с клавиатурой ──
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomStart)
                .padding(bottom = effectiveBottom)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .onSizeChanged { bottomAreaHeightPx = it.height }
            ) {
                AnimatedVisibility(visible = isUpdatingRoles) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(theme.primary.copy(alpha = 0.08f))
                            .padding(vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Обновляем персонажа...",
                            style = TextStyle(fontSize = 11.sp, color = theme.primary.copy(alpha = 0.7f))
                        )
                    }
                }

                RpChatInput(
                    value = messageInput,
                    onValueChange = { if (!isRecording) messageInput = it },
                    onSend = {
                        if (messageInput.isNotBlank()) {
                            viewModel.sendMessage(messageInput)
                            messageInput = ""
                        }
                    },
                    isStreaming = isStreaming,
                    isRecording = isRecording,
                    hasAudioPermission = hasAudioPermission,
                    theme = theme,
                    chatBg = chatBg,
                    hazeState = hazeState,
                    onMicDown = {
                        if (!hasAudioPermission) {
                            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        } else {
                            viewModel.startVoiceInput()
                        }
                    },
                    onMicUp = { held ->
                        if (held < 300L) viewModel.cancelVoiceInput()
                        else viewModel.stopVoiceInput()
                    }
                )
            }
        }

        // ── 3. Header — сиблинг hazeSource, hazeEffect блюрит контент позади ──
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopStart)
                .onSizeChanged { headerHeightPx = it.height }
                .padding(bottom = 12.dp)
                .hazeEffect(state = hazeState, style = HazeMaterials.thin(containerColor = chatBg.topColor)) {
                    progressive = HazeProgressive.verticalGradient(
                        startIntensity = 1f,
                        endIntensity = 0f,
                        easing = Easing { f -> f * f * f }
                    )
                }
        ) {
            RpChatHeader(
                charName = charName,
                tone = tone,
                modelName = currentModelName,
                theme = theme,
                onBack = { viewModel.navigateBack() },
                onEdit = { showEditRoles = true },
                onDelete = { showDeleteConfirm = true }
            )
        }

        // ── 4. Оверлей редактирования ролей ──
        if (showEditRoles) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.55f))
                    .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { showEditRoles = false },
                contentAlignment = Alignment.BottomCenter
            ) {
                RpEditRolesPanel(
                    charName = charName,
                    theme = theme, chatBg = chatBg,
                    onSave = { cName, cDesc, uName, uDesc ->
                        viewModel.updateRoles(
                            charName = cName.ifBlank { null },
                            charDescription = cDesc.ifBlank { null },
                            userName = uName.ifBlank { null },
                            userDescription = uDesc.ifBlank { null }
                        )
                        showEditRoles = false
                    },
                    onDismiss = { showEditRoles = false }
                )
            }
        }

        // ── 4. Диалог удаления ──
        if (showDeleteConfirm) {
            val chatBgLocal = LocalChatBackground.current
            AlertDialog(
                onDismissRequest = { showDeleteConfirm = false },
                containerColor = chatBgLocal.surfaceColor,
                title = {
                    Text("Удалить историю?", style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White))
                },
                text = {
                    Text(
                        "Вся переписка с «$charName» будет удалена.",
                        style = TextStyle(fontSize = 13.sp, color = Color.White.copy(alpha = 0.65f))
                    )
                },
                confirmButton = {
                    TextButton(onClick = { showDeleteConfirm = false; viewModel.deleteCurrentSession() }) {
                        Text("Удалить", color = GlassColors.error, fontWeight = FontWeight.SemiBold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirm = false }) {
                        Text("Отмена", color = Color.White.copy(alpha = 0.6f))
                    }
                }
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// CHAT HEADER
// ═══════════════════════════════════════════════════════════════

@Composable
private fun RpChatHeader(
    charName: String,
    tone: String?,
    modelName: String? = null,
    theme: com.health.companion.presentation.theme.AppThemeOption,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 4.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack, modifier = Modifier.size(38.dp)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White.copy(alpha = 0.65f), modifier = Modifier.size(20.dp))
        }
        Box(
            modifier = Modifier
                .size(34.dp).clip(CircleShape)
                .background(Brush.linearGradient(listOf(theme.primary.copy(alpha = 0.3f), theme.secondary.copy(alpha = 0.2f))))
                .border(1.dp, theme.primary.copy(alpha = 0.35f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                charName.firstOrNull()?.uppercase() ?: "?",
                style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Bold, color = theme.primary)
            )
        }
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(charName, style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.White))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                if (!tone.isNullOrBlank()) {
                    Text(tone, style = TextStyle(fontSize = 10.sp, color = theme.primary.copy(alpha = 0.6f)))
                }
                if (!modelName.isNullOrBlank()) {
                    RpModelBadge(name = modelName, theme = theme)
                }
            }
        }

        IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Default.Edit, null, tint = Color.White.copy(alpha = 0.55f), modifier = Modifier.size(18.dp))
        }

        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(GlassColors.error.copy(alpha = 0.18f))
                .border(1.dp, GlassColors.error.copy(alpha = 0.45f), RoundedCornerShape(8.dp))
                .clickable(onClick = onDelete),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.DeleteOutline, null, tint = GlassColors.error, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(6.dp))
    }
}

@Composable
private fun RpModelBadge(name: String, theme: com.health.companion.presentation.theme.AppThemeOption) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(theme.secondary.copy(alpha = 0.15f))
            .border(0.5.dp, theme.secondary.copy(alpha = 0.35f), RoundedCornerShape(4.dp))
            .padding(horizontal = 5.dp, vertical = 2.dp)
    ) {
        Text(
            text = name,
            style = TextStyle(
                fontSize = 9.sp,
                color = theme.secondary.copy(alpha = 0.8f),
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.3.sp
            )
        )
    }
}

// ═══════════════════════════════════════════════════════════════
// RP TEXT PARSER — ACTION / DIALOGUE / PLAIN
// ═══════════════════════════════════════════════════════════════

private enum class RpSegmentType { ACTION, DIALOGUE, PLAIN }
private data class RpSegment(val type: RpSegmentType, val text: String)

private fun parseRpText(raw: String, streaming: Boolean = false): List<RpSegment> {
    val segments = mutableListOf<RpSegment>()
    val pattern = Regex("""(\*[^*]+\*)|("(?:[^"\\]|\\.)*")""")
    var lastIndex = 0
    pattern.findAll(raw).forEach { match ->
        if (match.range.first > lastIndex) {
            val plain = raw.substring(lastIndex, match.range.first).trim()
            if (plain.isNotEmpty()) segments.add(RpSegment(RpSegmentType.PLAIN, plain))
        }
        when {
            match.groupValues[1].isNotEmpty() ->
                segments.add(RpSegment(RpSegmentType.ACTION, match.groupValues[1].drop(1).dropLast(1)))
            match.groupValues[2].isNotEmpty() ->
                segments.add(RpSegment(RpSegmentType.DIALOGUE, match.groupValues[2].drop(1).dropLast(1)))
        }
        lastIndex = match.range.last + 1
    }
    if (lastIndex < raw.length) {
        val tail = raw.substring(lastIndex)
        val trimmed = tail.trimStart()
        if (trimmed.isNotEmpty()) {
            if (streaming) {
                // Во время стриминга определяем тип незакрытого токена по первому символу
                when {
                    trimmed.startsWith("*") -> {
                        val partial = trimmed.drop(1)
                        if (partial.isNotEmpty()) segments.add(RpSegment(RpSegmentType.ACTION, partial))
                    }
                    trimmed.startsWith("\"") -> {
                        val partial = trimmed.drop(1)
                        if (partial.isNotEmpty()) segments.add(RpSegment(RpSegmentType.DIALOGUE, partial))
                    }
                    else -> segments.add(RpSegment(RpSegmentType.PLAIN, trimmed.trim()))
                }
            } else {
                val plain = trimmed.trim()
                if (plain.isNotEmpty()) segments.add(RpSegment(RpSegmentType.PLAIN, plain))
            }
        }
    }
    return segments
}

@Composable
private fun RpMessageBubble(content: String, isStreaming: Boolean = false, theme: com.health.companion.presentation.theme.AppThemeOption) {
    // Парсим всегда — и во время стриминга, и после.
    // Во время стриминга хвост незакрытого токена окрашивается по первому символу.
    val segments = remember(content, isStreaming) { parseRpText(content, streaming = isStreaming) }
    if (segments.isEmpty()) {
        Text(content, style = TextStyle(fontSize = 15.sp, color = Color.White.copy(alpha = 0.85f), lineHeight = 24.sp))
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        segments.forEach { seg ->
            when (seg.type) {
                RpSegmentType.ACTION -> Text(
                    text = seg.text,
                    style = TextStyle(
                        fontSize = 14.sp,
                        fontStyle = FontStyle.Italic,
                        color = Color(0xFFB4C8E6).copy(alpha = 0.78f),
                        lineHeight = 22.sp
                    )
                )
                RpSegmentType.DIALOGUE -> Row(verticalAlignment = Alignment.Top) {
                    Box(
                        modifier = Modifier
                            .width(2.dp)
                            .defaultMinSize(minHeight = 18.dp)
                            .background(theme.primary.copy(alpha = 0.55f))
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "\"${seg.text}\"",
                        style = TextStyle(
                            fontSize = 15.sp,
                            color = Color(0xFFF0E6C8).copy(alpha = 0.92f),
                            lineHeight = 22.sp,
                            fontWeight = FontWeight.Normal
                        )
                    )
                }
                RpSegmentType.PLAIN -> Text(
                    text = seg.text,
                    style = TextStyle(
                        fontSize = 15.sp,
                        color = Color.White.copy(alpha = 0.82f),
                        lineHeight = 22.sp
                    )
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// CHAT BUBBLE — идентично основному чату
// ═══════════════════════════════════════════════════════════════

@Composable
private fun RpBubble(
    msg: RpChatMessage,
    charName: String,
    isStreaming: Boolean = false,
    theme: com.health.companion.presentation.theme.AppThemeOption,
    chatBg: com.health.companion.presentation.theme.ChatBackgroundOption
) {
    val isUser = msg.role == "user"
    val isSystem = msg.role == "system"

    if (isSystem) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp)
                .clip(GlassShapes.small)
                .background(GlassColors.error.copy(alpha = 0.1f))
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(msg.text, style = TextStyle(fontSize = 12.sp, color = GlassColors.error.copy(alpha = 0.8f), lineHeight = 16.sp))
        }
        return
    }

    val bubbleShape = if (isUser) GlassShapes.userBubble else GlassShapes.assistantBubble
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val maxBubbleWidth = screenWidth * 0.88f

    // Точно как в основном чате: Box(fillMaxWidth) → Row(End/Start) → bubble
    Box(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
        ) {
            if (!isUser) {
                Text(
                    charName,
                    style = TextStyle(
                        fontSize = 10.sp,
                        color = theme.primary.copy(alpha = 0.65f),
                        fontWeight = FontWeight.Medium
                    ),
                    modifier = Modifier.padding(start = GlassSpacing.bubbleHorizontal, bottom = 2.dp)
                )
            }

            Box(
                modifier = Modifier
                    .widthIn(max = maxBubbleWidth)
                    .shadow(
                        elevation = if (isUser) GlassElevation.userBubble else GlassElevation.assistantBubble,
                        shape = bubbleShape,
                        spotColor = if (isUser) theme.userBubble.copy(alpha = 0.3f) else Color.Transparent
                    )
                    .clip(bubbleShape)
                    .background(
                        if (isUser)
                            Brush.linearGradient(listOf(theme.userBubble, theme.userBubbleDark))
                        else
                            Brush.linearGradient(
                                listOf(
                                    chatBg.surfaceColor,
                                    chatBg.surfaceColor.copy(alpha = 0.92f)
                                )
                            )
                    )
                    .then(
                        if (!isUser)
                            Modifier.border(0.5.dp, theme.primary.copy(alpha = 0.08f), bubbleShape)
                        else Modifier
                    )
                    .padding(
                        horizontal = GlassSpacing.bubbleHorizontal,
                        vertical = GlassSpacing.bubbleVertical
                    )
            ) {
                if (isUser) {
                    Text(
                        text = msg.text,
                        style = GlassTypography.messageText.copy(
                            color = Color.White.copy(alpha = 0.95f),
                            fontStyle = FontStyle.Normal
                        )
                    )
                } else {
                    RpMessageBubble(content = msg.text, isStreaming = isStreaming, theme = theme)
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// TYPING INDICATOR
// ═══════════════════════════════════════════════════════════════

@Composable
private fun RpTypingIndicator(charName: String, theme: com.health.companion.presentation.theme.AppThemeOption) {
    val chatBg = LocalChatBackground.current
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
        Text(
            charName,
            style = TextStyle(fontSize = 10.sp, color = theme.primary.copy(alpha = 0.5f)),
            modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
        )
        val infiniteTransition = rememberInfiniteTransition(label = "typing")
        val alpha by infiniteTransition.animateFloat(
            initialValue = 0.3f, targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
            label = "alpha"
        )
        Row(
            modifier = Modifier
                .clip(GlassShapes.assistantBubble)
                .background(chatBg.surfaceColor.copy(alpha = 0.65f))
                .padding(horizontal = GlassSpacing.bubbleHorizontal, vertical = GlassSpacing.bubbleVertical),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(3) { i ->
                Box(
                    Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(theme.primary.copy(alpha = alpha * (0.4f + i * 0.2f)))
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// CHAT INPUT — идентично основному чату
// ═══════════════════════════════════════════════════════════════

@OptIn(ExperimentalHazeMaterialsApi::class)
@Composable
private fun RpChatInput(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    isStreaming: Boolean,
    isRecording: Boolean,
    hasAudioPermission: Boolean,
    theme: com.health.companion.presentation.theme.AppThemeOption,
    chatBg: com.health.companion.presentation.theme.ChatBackgroundOption,
    hazeState: HazeState,
    onMicDown: () -> Unit,
    onMicUp: (Long) -> Unit
) {
    val canSend = value.isNotBlank() && !isRecording
    val context = LocalContext.current
    val view = LocalView.current

    // Glow animation — точно как в основном чате
    val glowAlpha by animateFloatAsState(
        targetValue = if (isRecording) 1f else 0f,
        animationSpec = tween(250, easing = FastOutSlowInEasing),
        label = "glow_alpha"
    )
    val infiniteGlow = rememberInfiniteTransition(label = "voice_glow")
    val glowPulse by infiniteGlow.animateFloat(
        initialValue = 0.3f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(700, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "glow_pulse"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = GlassSpacing.screenEdge)
            .padding(top = 4.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Input container — haze матовое стекло как в основном чате
        Row(
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 40.dp)
                .shadow(GlassElevation.inputField, GlassShapes.inputField)
                .clip(GlassShapes.inputField)
                .hazeEffect(state = hazeState, style = HazeMaterials.regular(containerColor = chatBg.inputColor))
                .border(
                    1.dp,
                    Brush.horizontalGradient(
                        listOf(
                            theme.primary.copy(alpha = 0.22f),
                            theme.secondary.copy(alpha = 0.12f)
                        )
                    ),
                    GlassShapes.inputField
                )
                .padding(start = 14.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Индикатор записи (пульсирующая точка)
            if (isRecording) {
                val recAlpha by rememberInfiniteTransition(label = "rec").animateFloat(
                    initialValue = 0.4f, targetValue = 1f,
                    animationSpec = infiniteRepeatable(tween(500), RepeatMode.Reverse),
                    label = "rec_a"
                )
                Box(
                    Modifier
                        .padding(start = 8.dp)
                        .size(8.dp)
                        .background(GlassColors.error.copy(alpha = recAlpha), CircleShape)
                )
                Spacer(Modifier.width(8.dp))
            }

            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                enabled = !isStreaming && !isRecording,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 36.dp, max = 120.dp),
                textStyle = GlassTypography.messageText,
                cursorBrush = SolidColor(theme.primary),
                maxLines = 4,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Send
                ),
                keyboardActions = KeyboardActions(
                    onSend = { if (canSend && !isStreaming) onSend() }
                ),
                decorationBox = { innerTextField ->
                    Box(
                        modifier = Modifier.padding(vertical = 6.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (value.isEmpty()) {
                            Text(
                                text = if (isRecording) "Говорите..." else "Сообщение",
                                style = GlassTypography.placeholder
                            )
                        }
                        innerTextField()
                    }
                }
            )
        }

        Spacer(Modifier.width(GlassSpacing.buttonSpacing))

        // Mic/Send button — точно как в основном чате
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(GlassSpacing.buttonSize + 6.dp)
                .clip(CircleShape)
        ) {
            Box(
                modifier = Modifier
                    .size(GlassSpacing.buttonSize)
                    .drawBehind {
                        if (glowAlpha > 0.01f) {
                            val center = this.center
                            val btnRadius = size.minDimension / 2f
                            drawCircle(
                                color = GlassColors.error.copy(alpha = 0.15f * glowPulse * glowAlpha),
                                radius = btnRadius * (3.5f + 0.8f * glowPulse),
                                center = center
                            )
                            drawCircle(
                                color = GlassColors.coral.copy(alpha = 0.2f * glowPulse * glowAlpha),
                                radius = btnRadius * (2.5f + 0.5f * glowPulse),
                                center = center
                            )
                            drawCircle(
                                color = GlassColors.error.copy(alpha = 0.35f * glowAlpha),
                                radius = btnRadius * 1.35f,
                                center = center
                            )
                        }
                    }
                    // Точная копия pointerInput из основного чата (press-and-hold)
                    .pointerInput(canSend, isStreaming, hasAudioPermission) {
                        awaitEachGesture {
                            awaitFirstDown(requireUnconsumed = false)
                            if (canSend) {
                                waitForUpOrCancellation()
                                if (!isStreaming) onSend()
                            } else {
                                // Voice press-and-hold
                                val pressTime = System.currentTimeMillis()
                                // Strong haptic on press (точно как в основном чате)
                                view.isHapticFeedbackEnabled = true
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                        (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
                                    } else {
                                        @Suppress("DEPRECATION")
                                        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                                    }
                                    vibrator?.vibrate(VibrationEffect.createOneShot(40, VibrationEffect.DEFAULT_AMPLITUDE))
                                } else {
                                    view.performHapticFeedback(HapticFeedbackConstantsCompat.LONG_PRESS)
                                }
                                onMicDown()
                                waitForUpOrCancellation()
                                val held = System.currentTimeMillis() - pressTime
                                if (held >= 300L) {
                                    // Soft haptic on release
                                    view.performHapticFeedback(HapticFeedbackConstantsCompat.KEYBOARD_PRESS)
                                }
                                onMicUp(held)
                            }
                        }
                    }
                    .shadow(
                        elevation = if (isRecording) 8.dp else 0.dp,
                        shape = CircleShape,
                        spotColor = if (isRecording) GlassColors.error else Color.Transparent
                    )
                    .clip(CircleShape)
                    .background(
                        when {
                            isRecording -> Brush.linearGradient(listOf(GlassColors.error, GlassColors.coral))
                            canSend     -> Brush.linearGradient(listOf(theme.primary, theme.secondary))
                            else        -> Brush.linearGradient(listOf(theme.primary.copy(alpha = 0.55f), theme.secondary.copy(alpha = 0.55f)))
                        },
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                when {
                    isStreaming && canSend -> CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = GlassColors.textPrimary,
                        strokeWidth = 2.dp
                    )
                    isRecording -> Icon(Icons.Default.Mic, null, tint = Color.White, modifier = Modifier.size(22.dp))
                    canSend     -> Icon(Icons.AutoMirrored.Filled.Send, null, tint = Color.White, modifier = Modifier.size(20.dp))
                    else        -> Icon(Icons.Default.Mic, null, tint = Color.White, modifier = Modifier.size(22.dp))
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// EDIT ROLES PANEL
// ═══════════════════════════════════════════════════════════════

@Composable
private fun RpEditRolesPanel(
    charName: String,
    theme: com.health.companion.presentation.theme.AppThemeOption,
    chatBg: com.health.companion.presentation.theme.ChatBackgroundOption,
    onSave: (charName: String, charDesc: String, userName: String, userDesc: String) -> Unit,
    onDismiss: () -> Unit
) {
    var newCharName by remember { mutableStateOf(charName) }
    var newCharDesc by remember { mutableStateOf("") }
    var newUserName by remember { mutableStateOf("") }
    var newUserDesc by remember { mutableStateOf("") }
    val hasChanges = newCharName != charName || newCharDesc.isNotBlank() || newUserName.isNotBlank() || newUserDesc.isNotBlank()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            .background(chatBg.surfaceColor.copy(alpha = 0.97f))
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {}
            .padding(horizontal = 16.dp, vertical = 16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            Modifier
                .width(36.dp).height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color.White.copy(alpha = 0.2f))
                .align(Alignment.CenterHorizontally)
        )

        Text("Изменить роли", style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.White))
        Text(
            "Оставь пустым — оставит прежнее. Если меняешь описание — ИИ перегенерирует персонажа (~3 сек).",
            style = TextStyle(fontSize = 11.sp, color = Color.White.copy(alpha = 0.4f), lineHeight = 15.sp)
        )

        RpSectionLabel("Персонаж ИИ")
        RpTextField(value = newCharName, onValueChange = { newCharName = it }, placeholder = "Имя", theme = theme, chatBg = chatBg)
        RpTextField(value = newCharDesc, onValueChange = { newCharDesc = it }, placeholder = "Новое описание (оставь пустым чтобы не менять)", minLines = 2, theme = theme, chatBg = chatBg)

        RpSectionLabel("Ваш персонаж")
        RpTextField(value = newUserName, onValueChange = { newUserName = it }, placeholder = "Ваше имя", theme = theme, chatBg = chatBg)
        RpTextField(value = newUserDesc, onValueChange = { newUserDesc = it }, placeholder = "Ваше новое описание", minLines = 2, theme = theme, chatBg = chatBg)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier
                    .weight(1f).height(44.dp).clip(RoundedCornerShape(10.dp))
                    .background(Color.White.copy(alpha = 0.06f))
                    .clickable(onClick = onDismiss),
                contentAlignment = Alignment.Center
            ) {
                Text("Отмена", style = TextStyle(fontSize = 14.sp, color = Color.White.copy(alpha = 0.6f)))
            }
            Box(
                modifier = Modifier
                    .weight(1f).height(44.dp).clip(RoundedCornerShape(10.dp))
                    .then(
                        if (hasChanges)
                            Modifier.background(Brush.linearGradient(listOf(theme.primary, theme.secondary)))
                        else
                            Modifier.background(Color.White.copy(alpha = 0.06f))
                    )
                    .clickable(enabled = hasChanges) { onSave(newCharName, newCharDesc, newUserName, newUserDesc) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Сохранить",
                    style = TextStyle(
                        fontSize = 14.sp,
                        color = if (hasChanges) Color.White else Color.White.copy(alpha = 0.25f),
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

// ═══════════════════════════════════════════════════════════════
// LOADER
// ═══════════════════════════════════════════════════════════════

// ═══════════════════════════════════════════════════════════════
// CINEMATIC CREATION ANIMATION
// ═══════════════════════════════════════════════════════════════

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun RpCreationAnimation() {
    val theme = LocalAppTheme.current

    val phases = listOf(
        "Пробуждаем характер...",
        "Прописываем мотивы...",
        "Создаём историю...",
        "Открываем мир...",
        "Финальные штрихи..."
    )
    var currentPhase by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(1400)
            currentPhase = (currentPhase + 1) % phases.size
        }
    }

    val inf = rememberInfiniteTransition(label = "creation")
    val rotation by inf.animateFloat(
        0f, 360f,
        infiniteRepeatable(tween(3600, easing = LinearEasing)),
        label = "rot"
    )
    val rotationSlow by inf.animateFloat(
        360f, 0f,
        infiniteRepeatable(tween(7000, easing = LinearEasing)),
        label = "rotSlow"
    )
    val pulse by inf.animateFloat(
        0.82f, 1.18f,
        infiniteRepeatable(tween(1000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulse"
    )
    val shimmer by inf.animateFloat(
        -0.4f, 1.4f,
        infiniteRepeatable(tween(2000, easing = LinearEasing)),
        label = "shimmer"
    )
    val sparkleAlpha by inf.animateFloat(
        0.2f, 0.85f,
        infiniteRepeatable(tween(600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "spAlpha"
    )

    val primary = theme.primary
    val secondary = theme.secondary
    val sparkleCount = 14

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.78f)),
        contentAlignment = Alignment.Center
    ) {
        // Фоновые частицы
        Canvas(modifier = Modifier.fillMaxSize()) {
            repeat(sparkleCount) { i ->
                val angle = (i * 360f / sparkleCount + rotation * 0.4f) * (PI / 180f)
                val dist = size.minDimension * 0.38f
                val x = center.x + cos(angle).toFloat() * dist
                val y = center.y + sin(angle).toFloat() * dist
                val alpha = if (i % 2 == 0) sparkleAlpha else 1f - sparkleAlpha
                drawCircle(
                    color = primary.copy(alpha = alpha * 0.55f),
                    radius = if (i % 3 == 0) 4.dp.toPx() else 2.5.dp.toPx(),
                    center = Offset(x, y)
                )
            }
            // Ближние мелкие звёздочки
            repeat(8) { i ->
                val angle = (i * 45f + rotationSlow * 0.3f) * (PI / 180f)
                val dist = size.minDimension * 0.22f
                val x = center.x + cos(angle).toFloat() * dist
                val y = center.y + sin(angle).toFloat() * dist
                drawCircle(
                    color = secondary.copy(alpha = sparkleAlpha * 0.4f),
                    radius = 1.8.dp.toPx(),
                    center = Offset(x, y)
                )
            }
        }

        // Центральная карточка
        Column(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFF1C0A35).copy(alpha = 0.97f),
                            Color(0xFF0E0520).copy(alpha = 0.97f)
                        )
                    )
                )
                .border(
                    1.dp,
                    Brush.linearGradient(listOf(primary.copy(alpha = 0.45f), secondary.copy(alpha = 0.2f))),
                    RoundedCornerShape(24.dp)
                )
                .padding(horizontal = 28.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Орб с вращающимися дугами
            Canvas(modifier = Modifier.size(88.dp)) {
                val cx = size.width / 2
                val cy = size.height / 2
                val baseR = size.minDimension * 0.28f * pulse

                // Внешнее свечение
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(primary.copy(alpha = 0.18f), Color.Transparent),
                        center = Offset(cx, cy), radius = baseR * 2.8f
                    ),
                    radius = baseR * 2.8f, center = Offset(cx, cy)
                )

                // Внешняя вращающаяся дуга
                val arcSize1 = baseR * 3.0f
                drawArc(
                    brush = Brush.sweepGradient(listOf(Color.Transparent, primary, Color.Transparent)),
                    startAngle = rotation,
                    sweepAngle = 200f,
                    useCenter = false,
                    topLeft = Offset(cx - arcSize1, cy - arcSize1),
                    size = Size(arcSize1 * 2, arcSize1 * 2),
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                )

                // Внутренняя дуга (против часовой)
                val arcSize2 = baseR * 2.0f
                drawArc(
                    brush = Brush.sweepGradient(listOf(Color.Transparent, secondary, Color.Transparent)),
                    startAngle = rotationSlow,
                    sweepAngle = 140f,
                    useCenter = false,
                    topLeft = Offset(cx - arcSize2, cy - arcSize2),
                    size = Size(arcSize2 * 2, arcSize2 * 2),
                    style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round)
                )

                // Ядро — радиальный градиент
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(Color.White.copy(alpha = 0.95f), primary.copy(alpha = 0.7f), Color.Transparent),
                        center = Offset(cx, cy), radius = baseR
                    ),
                    radius = baseR, center = Offset(cx, cy)
                )
            }

            // Фаза с плавной AnimatedContent
            AnimatedContent(
                targetState = phases[currentPhase],
                transitionSpec = {
                    (fadeIn(tween(380)) + slideInVertically(tween(380, easing = FastOutSlowInEasing)) { it / 4 })
                        .togetherWith(fadeOut(tween(220)) + slideOutVertically(tween(220)) { -it / 4 })
                },
                label = "phase"
            ) { phaseText ->
                Text(
                    text = phaseText,
                    style = TextStyle(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.90f),
                        letterSpacing = 0.2.sp
                    )
                )
            }

            // Shimmer-полоска прогресса
            Canvas(modifier = Modifier.fillMaxWidth().height(2.dp)) {
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.08f),
                    cornerRadius = CornerRadius(1.dp.toPx())
                )
                val barW = size.width
                drawRoundRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(Color.Transparent, primary.copy(alpha = 0.9f), secondary.copy(alpha = 0.7f), Color.Transparent),
                        startX = barW * shimmer - barW * 0.35f,
                        endX = barW * shimmer + barW * 0.35f
                    ),
                    cornerRadius = CornerRadius(1.dp.toPx())
                )
            }
        }
    }
}

@Composable
private fun RpLoaderCard(text: String) {
    val theme = LocalAppTheme.current
    val chatBg = LocalChatBackground.current
    val infiniteTransition = rememberInfiniteTransition(label = "loader")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulse"
    )
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(chatBg.surfaceColor.copy(alpha = 0.95f))
            .border(1.dp, theme.primary.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
            .padding(horizontal = 32.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(3) { i ->
                Box(
                    Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(theme.primary.copy(alpha = alpha * (0.5f + i * 0.17f)))
                )
            }
        }
        Text(
            text.ifBlank { "Загрузка..." },
            style = TextStyle(fontSize = 13.sp, color = Color.White.copy(alpha = 0.75f), fontWeight = FontWeight.Medium)
        )
    }
}

// ═══════════════════════════════════════════════════════════════
// SHARED HELPERS
// ═══════════════════════════════════════════════════════════════

@Composable
private fun RpSectionLabel(text: String) {
    Text(
        text,
        style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.White.copy(alpha = 0.5f))
    )
}

@Composable
private fun RpSectionCard(
    theme: com.health.companion.presentation.theme.AppThemeOption,
    chatBg: com.health.companion.presentation.theme.ChatBackgroundOption,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(chatBg.surfaceColor.copy(alpha = 0.5f))
            .border(1.dp, theme.primary.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
            .padding(12.dp),
        content = content
    )
}

@Composable
private fun RpTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    minLines: Int = 1,
    theme: com.health.companion.presentation.theme.AppThemeOption,
    chatBg: com.health.companion.presentation.theme.ChatBackgroundOption
) {
    val shape = RoundedCornerShape(8.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(chatBg.inputColor.copy(alpha = 0.6f))
            .border(1.dp, theme.primary.copy(alpha = 0.15f), shape)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        if (value.isEmpty()) {
            Text(placeholder, style = TextStyle(fontSize = 12.sp, color = Color.White.copy(alpha = 0.28f), lineHeight = 16.sp))
        }
        BasicTextField(
            value = value, onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            textStyle = TextStyle(fontSize = 13.sp, color = Color.White.copy(alpha = 0.9f), lineHeight = 18.sp),
            cursorBrush = SolidColor(theme.primary),
            minLines = minLines,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
        )
    }
}
