package com.health.companion.presentation.screens.settings

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.health.companion.presentation.components.GlassColors
import com.health.companion.presentation.theme.AppFontFamily
import com.health.companion.presentation.theme.AppThemeOption
import com.health.companion.presentation.theme.ChatBackgroundOption
import com.health.companion.presentation.theme.LocalAppTheme
import com.health.companion.presentation.theme.LocalChatBackground
import com.health.companion.utils.ThemeManager
import kotlin.math.abs

@Composable
fun AppearanceScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onBack: () -> Unit = {}
) {
    val selectedTheme  by viewModel.selectedTheme.collectAsStateWithLifecycle()
    val selectedBg     by viewModel.selectedBackground.collectAsStateWithLifecycle()
    val textScale      by viewModel.textScale.collectAsStateWithLifecycle()
    val selectedFont   by viewModel.fontFamily.collectAsStateWithLifecycle()
    val chatBg = LocalChatBackground.current
    val appTheme = LocalAppTheme.current

    val allThemes = AppThemeOption.entries.toList()
    val allBgs    = ChatBackgroundOption.entries.toList()
    val themeRow1 = allThemes.take((allThemes.size + 1) / 2)
    val themeRow2 = allThemes.drop((allThemes.size + 1) / 2)
    val bgRow1    = allBgs.take((allBgs.size + 1) / 2)
    val bgRow2    = allBgs.drop((allBgs.size + 1) / 2)

    Box(modifier = Modifier.fillMaxSize().background(chatBg.gradient)) {
        Column(modifier = Modifier.fillMaxSize().systemBarsPadding()) {

            // ── Top bar ───────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = Color.White)
                }
                Text(
                    text = "Внешний вид",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }

            // ── Content ───────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(top = 4.dp, bottom = 40.dp),
                verticalArrangement = Arrangement.spacedBy(28.dp)
            ) {

                // ── 1. Цвет акцента ──────────────────────────────
                AppSection(title = "Цвет акцента", badge = selectedTheme.label, badgeColor = selectedTheme.primary) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        AccentLazyRow(themeRow1, selectedTheme) { viewModel.setTheme(it) }
                        AccentLazyRow(themeRow2, selectedTheme) { viewModel.setTheme(it) }
                    }
                }

                AppDivider()

                // ── 2. Фон ────────────────────────────────────────
                AppSection(title = "Фон", badge = selectedBg.label, badgeColor = Color.White.copy(0.5f)) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        BgLazyRow(bgRow1, selectedBg, selectedTheme) { viewModel.setBackground(it) }
                        BgLazyRow(bgRow2, selectedBg, selectedTheme) { viewModel.setBackground(it) }
                    }
                }

                AppDivider()

                // ── 3. Масштаб текста ─────────────────────────────
                AppSection(
                    title = "Масштаб текста",
                    badge = "${(textScale * 100).toInt()}%",
                    badgeColor = appTheme.primary
                ) {
                    TextScaleSection(
                        scale = textScale,
                        accent = appTheme,
                        onScaleChange = { viewModel.setTextScale(it) }
                    )
                }

                AppDivider()

                // ── 4. Шрифт ─────────────────────────────────────
                AppSection(title = "Шрифт", badge = selectedFont.label, badgeColor = Color.White.copy(0.5f)) {
                    FontSection(
                        selected = selectedFont,
                        accent = appTheme,
                        onSelect = { viewModel.setFontFamily(it) }
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Text Scale
// ─────────────────────────────────────────────────────────────────────────────

private val SCALE_PRESETS = listOf(0.80f, 0.90f, 1.00f, 1.10f, 1.20f, 1.30f, 1.40f)

@Composable
private fun TextScaleSection(
    scale: Float,
    accent: AppThemeOption,
    onScaleChange: (Float) -> Unit
) {
    val chatBg = LocalChatBackground.current

    Column(
        modifier = Modifier.padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // ── Live preview ─────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(chatBg.surfaceColor.copy(alpha = 0.6f))
                .border(0.5.dp, accent.primary.copy(alpha = 0.15f), RoundedCornerShape(14.dp))
                .padding(horizontal = 20.dp, vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Scale factor displayed as "Аа" at relative size
                Text(
                    text = "Аа",
                    fontSize = (28 * scale).sp,
                    fontWeight = FontWeight.SemiBold,
                    color = accent.primary
                )
                Text(
                    text = "Привет! Как прошёл твой день?",
                    fontSize = (13.5f * scale).sp,
                    lineHeight = (18f * scale).sp,
                    color = Color.White.copy(alpha = 0.85f),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Сегодня мне нужно напомнить о встрече в 18:00.",
                    fontSize = (11f * scale).sp,
                    lineHeight = (15f * scale).sp,
                    color = Color.White.copy(alpha = 0.45f),
                    textAlign = TextAlign.Center
                )
            }
        }

        // ── Preset chips ─────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            SCALE_PRESETS.forEach { preset ->
                val isActive = abs(preset - scale) < 0.005f
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (isActive) accent.accentGradient
                            else Brush.linearGradient(
                                listOf(chatBg.surfaceColor.copy(0.7f), chatBg.surfaceColor.copy(0.7f))
                            )
                        )
                        .border(
                            0.5.dp,
                            if (isActive) Color.Transparent else accent.primary.copy(alpha = 0.18f),
                            RoundedCornerShape(8.dp)
                        )
                        .clickable { onScaleChange(preset) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${(preset * 100).toInt()}",
                        fontSize = 11.sp,
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                        color = if (isActive) Color.White else GlassColors.textSecondary
                    )
                }
            }
        }

        // ── Continuous slider ─────────────────────────────────────
        Slider(
            value = scale,
            onValueChange = onScaleChange,
            valueRange = ThemeManager.TEXT_SCALE_MIN..ThemeManager.TEXT_SCALE_MAX,
            steps = 0,
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(
                thumbColor = accent.primary,
                activeTrackColor = accent.primary,
                inactiveTrackColor = accent.primary.copy(alpha = 0.18f)
            )
        )

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("80%", fontSize = 10.sp, color = GlassColors.textMuted)
            Text("140%", fontSize = 10.sp, color = GlassColors.textMuted)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Font
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun FontSection(
    selected: AppFontFamily,
    accent: AppThemeOption,
    onSelect: (AppFontFamily) -> Unit
) {
    val chatBg = LocalChatBackground.current
    val fonts = AppFontFamily.entries.toList()

    // 2 × 2 grid
    val rows = fonts.chunked(2)
    Column(
        modifier = Modifier.padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                row.forEach { font ->
                    val isActive = font == selected
                    val cardScale by animateFloatAsState(
                        targetValue = if (isActive) 1.03f else 1f,
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        label = "font_card_${font.id}"
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .scale(cardScale)
                            .height(88.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                if (isActive) accent.accentGradient
                                else Brush.linearGradient(
                                    listOf(chatBg.surfaceColor.copy(0.75f), chatBg.surfaceColor.copy(0.75f))
                                )
                            )
                            .border(
                                width = if (isActive) 1.5.dp else 0.5.dp,
                                color = if (isActive) accent.primary.copy(0.6f) else accent.primary.copy(0.12f),
                                shape = RoundedCornerShape(14.dp)
                            )
                            .clickable { onSelect(font) }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "Аа",
                                style = TextStyle(
                                    fontFamily = font.fontFamily,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isActive) Color.White else GlassColors.textPrimary
                                )
                            )
                            Text(
                                text = font.label,
                                style = TextStyle(
                                    fontFamily = font.fontFamily,
                                    fontSize = 11.sp,
                                    color = if (isActive) Color.White.copy(0.85f) else GlassColors.textSecondary
                                )
                            )
                            Text(
                                text = font.subtitle,
                                style = TextStyle(
                                    fontFamily = font.fontFamily,
                                    fontSize = 9.sp,
                                    color = if (isActive) Color.White.copy(0.55f) else GlassColors.textMuted
                                )
                            )
                        }
                        if (isActive) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(18.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(0.25f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(11.dp))
                            }
                        }
                    }
                }
                // If row has only 1 item — add empty spacer
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Common widgets
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun AppSection(
    title: String,
    badge: String,
    badgeColor: Color,
    content: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title.uppercase(),
                fontSize = 11.sp,
                letterSpacing = 1.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White.copy(alpha = 0.4f)
            )
            Text(text = badge, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = badgeColor)
        }
        content()
    }
}

@Composable
private fun AppDivider() {
    HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp), color = Color.White.copy(alpha = 0.06f))
}

// ─────────────────────────────────────────────────────────────────────────────
// Accent / Background rows (unchanged from before)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun AccentLazyRow(
    themes: List<AppThemeOption>,
    selected: AppThemeOption,
    onSelect: (AppThemeOption) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(themes, key = { it.id }) { theme ->
            AccentItem(theme, selected == theme) { onSelect(theme) }
        }
    }
}

@Composable
private fun AccentItem(theme: AppThemeOption, isSelected: Boolean, onClick: () -> Unit) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.13f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "accent_${theme.id}"
    )
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp),
        modifier = Modifier.width(52.dp)
    ) {
        Box(
            modifier = Modifier
                .scale(scale)
                .size(44.dp)
                .clip(CircleShape)
                .background(theme.accentGradient)
                .then(
                    if (isSelected) Modifier.border(2.dp, Color.White.copy(alpha = 0.88f), CircleShape)
                    else Modifier.border(1.dp, Color.White.copy(alpha = 0.10f), CircleShape)
                )
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(18.dp))
        }
        Text(
            text = theme.label,
            fontSize = 10.sp,
            maxLines = 1,
            color = if (isSelected) theme.primary else Color.White.copy(alpha = 0.38f),
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

@Composable
private fun BgLazyRow(
    bgs: List<ChatBackgroundOption>,
    selected: ChatBackgroundOption,
    accent: AppThemeOption,
    onSelect: (ChatBackgroundOption) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(bgs, key = { it.id }) { bg ->
            BgItem(bg, accent, selected == bg) { onSelect(bg) }
        }
    }
}

@Composable
private fun BgItem(bg: ChatBackgroundOption, accent: AppThemeOption, isSelected: Boolean, onClick: () -> Unit) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.07f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "bg_${bg.id}"
    )
    val shape = RoundedCornerShape(12.dp)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.width(58.dp)
    ) {
        Box(
            modifier = Modifier
                .scale(scale)
                .width(54.dp)
                .height(72.dp)
                .clip(shape)
                .background(bg.gradient)
                .then(
                    if (isSelected) Modifier.border(2.dp, accent.primary.copy(alpha = 0.85f), shape)
                    else Modifier.border(1.dp, Color.White.copy(alpha = 0.08f), shape)
                )
                .clickable(onClick = onClick)
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 6.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically)
            ) {
                Box(Modifier.fillMaxWidth(0.78f).height(7.dp).clip(RoundedCornerShape(4.dp)).background(bg.surfaceColor.copy(alpha = 0.80f)))
                Box(Modifier.fillMaxWidth(0.62f).height(7.dp).align(Alignment.End).clip(RoundedCornerShape(4.dp)).background(accent.accentGradient))
                Box(Modifier.fillMaxWidth(0.50f).height(5.dp).clip(RoundedCornerShape(4.dp)).background(bg.surfaceColor.copy(alpha = 0.50f)))
            }
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .align(Alignment.TopEnd)
                        .offset(x = (-4).dp, y = 4.dp)
                        .clip(CircleShape)
                        .background(accent.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(10.dp))
                }
            }
        }
        Text(
            text = bg.label,
            fontSize = 10.sp,
            maxLines = 1,
            color = if (isSelected) accent.primary else Color.White.copy(alpha = 0.38f),
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}
