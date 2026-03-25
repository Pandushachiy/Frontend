package com.health.companion.presentation.screens.marketplace

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import com.health.companion.data.remote.api.MarketplaceCategory
import com.health.companion.data.remote.api.MarketplaceSkill
import com.health.companion.presentation.components.*
import com.health.companion.presentation.theme.LocalAppTheme
import com.health.companion.presentation.theme.LocalChatBackground
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

@Composable
fun MarketplaceScreen(
    viewModel: MarketplaceViewModel = hiltViewModel(),
    onBack: () -> Unit = {}
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()
    val allSkills by viewModel.allSkills.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val selectedSort by viewModel.selectedSort.collectAsState()
    val totalResults by viewModel.totalResults.collectAsState()
    val selectedSkill by viewModel.selectedSkill.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isLoadingMore by viewModel.isLoadingMore.collectAsState()
    val error by viewModel.error.collectAsState()
    val installMessage by viewModel.installMessage.collectAsState()

    selectedSkill?.let { skill ->
        SkillDetailsDialog(
            skill = skill,
            onDismiss = { viewModel.hideSkillDetails() },
            onInstall = { viewModel.installSkill(skill) }
        )
    }

    installMessage?.let { msg ->
        LaunchedEffect(msg) {
            kotlinx.coroutines.delay(3000)
            viewModel.clearInstallMessage()
        }
    }

    val currentTheme = LocalAppTheme.current
    val chatBg = LocalChatBackground.current
    val listState = rememberLazyListState()
    val isSearchMode = searchQuery.isNotBlank()
    val displaySkills = if (isSearchMode) searchResults else allSkills

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
            // ── Top Bar ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад", tint = GlassColors.textPrimary)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Маркетплейс", style = GlassTypography.titleSmall)
                    Text(
                        "ClawHub — навыки для AI",
                        style = GlassTypography.timestamp
                    )
                }
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = currentTheme.primary
                    )
                } else {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Default.Refresh, "Обновить", tint = GlassColors.textSecondary)
                    }
                }
            }

            // ── Search Bar ──
            MarketplaceSearchBar(
                query = searchQuery,
                onQueryChange = { viewModel.setSearchQuery(it) },
                isSearching = isSearching,
                onClear = { viewModel.clearSearch() },
                accentColor = currentTheme.primary
            )

            // ── Messages ──
            AnimatedVisibility(visible = error != null) {
                error?.let { msg ->
                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                            .clickable { viewModel.clearError() },
                        shape = RoundedCornerShape(12.dp),
                        backgroundColor = GlassColors.error.copy(alpha = 0.12f),
                        borderColor = GlassColors.error.copy(alpha = 0.25f)
                    ) {
                        Text(
                            msg,
                            style = GlassTypography.labelSmall.copy(color = GlassColors.error),
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }

            AnimatedVisibility(visible = installMessage != null) {
                installMessage?.let { msg ->
                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(12.dp),
                        backgroundColor = GlassColors.mint.copy(alpha = 0.12f),
                        borderColor = GlassColors.mint.copy(alpha = 0.25f)
                    ) {
                        Text(
                            msg,
                            style = GlassTypography.labelSmall.copy(color = GlassColors.mint),
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }

            // ── Content ──
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .navigationBarsPadding(),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (!isSearchMode) {
                    // Sort + Category filters
                    item(key = "filters") {
                        Column {
                            SortChips(
                                selected = selectedSort,
                                onSelect = { viewModel.selectSort(it) },
                                modifier = Modifier.padding(top = 8.dp)
                            )
                            if (categories.isNotEmpty()) {
                                CategoriesChips(
                                    categories = categories,
                                    selected = selectedCategory,
                                    onSelect = { viewModel.selectCategory(it) },
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }
                        }
                    }

                    // Header with total count
                    item(key = "all_header") {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                selectedCategory?.let { cat ->
                                    categories.find { it.id == cat }?.name ?: "Категория"
                                } ?: sortLabel(selectedSort),
                                style = GlassTypography.labelMedium.copy(
                                    color = currentTheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                            Spacer(Modifier.weight(1f))
                            if (isLoadingMore) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(12.dp),
                                    strokeWidth = 1.5.dp,
                                    color = currentTheme.primary
                                )
                                Spacer(Modifier.width(6.dp))
                            }
                            if (totalResults > 0) {
                                Text(
                                    formatCount(totalResults),
                                    style = GlassTypography.timestamp.copy(color = GlassColors.textMuted)
                                )
                            }
                        }
                    }
                } else {
                    // Search results header
                    item(key = "search_header") {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Результаты поиска",
                                style = GlassTypography.labelMedium.copy(
                                    color = currentTheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                            Spacer(Modifier.weight(1f))
                            Text(
                                "Найдено: ${formatCount(totalResults)}",
                                style = GlassTypography.timestamp
                            )
                        }
                    }
                }

                // Skills list
                if (displaySkills.isEmpty() && !isLoading && !isSearching) {
                    item(key = "empty") {
                        EmptyState(
                            icon = if (isSearchMode) "search" else "package",
                            title = if (isSearchMode) "Ничего не найдено" else "Нет навыков",
                            subtitle = if (isSearchMode) "Попробуйте другой запрос" else "Попробуйте обновить"
                        )
                    }
                } else {
                    items(
                        displaySkills,
                        key = { "skill_${it.id.ifEmpty { it.slug }}_${it.name}" }
                    ) { skill ->
                        SkillListCard(
                            skill = skill,
                            onClick = { viewModel.showSkillDetails(skill) },
                            onInstall = { viewModel.installSkill(skill) },
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                    }
                }

                // Loading states
                if (isLoading && displaySkills.isEmpty()) {
                    item(key = "loading") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(
                                    color = currentTheme.primary,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(Modifier.height(12.dp))
                                Text("Загрузка...", style = GlassTypography.labelSmall)
                            }
                        }
                    }
                }

            }
        }
    }
}

// ════════════════════════════════════════════════════════════════════════════════
// Search Bar
// ════════════════════════════════════════════════════════════════════════════════

@Composable
private fun MarketplaceSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    isSearching: Boolean,
    onClear: () -> Unit,
    accentColor: Color = GlassColors.accent
) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        shape = RoundedCornerShape(14.dp),
        borderColor = if (query.isNotBlank()) accentColor.copy(alpha = 0.35f)
        else GlassColors.whiteOverlay10
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Search,
                contentDescription = null,
                tint = if (query.isNotBlank()) accentColor else GlassColors.textMuted,
                modifier = Modifier.size(20.dp)
            )

            Spacer(Modifier.width(10.dp))

            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.weight(1f),
                textStyle = GlassTypography.messageText.copy(color = GlassColors.textPrimary),
                singleLine = true,
                cursorBrush = SolidColor(accentColor),
                decorationBox = { innerTextField ->
                    Box {
                        if (query.isEmpty()) {
                            Text(
                                "Поиск навыков...",
                                style = GlassTypography.messageText.copy(color = GlassColors.textMuted)
                            )
                        }
                        innerTextField()
                    }
                }
            )

            if (isSearching) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = accentColor
                )
            } else if (query.isNotBlank()) {
                IconButton(
                    onClick = onClear,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Очистить",
                        tint = GlassColors.textMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════════════════════
// Section Title
// ════════════════════════════════════════════════════════════════════════════════

@Composable
private fun SectionTitle(
    title: String,
    icon: String,
    modifier: Modifier = Modifier
) {
    val emoji = when (icon) {
        "fire" -> "\uD83D\uDD25"
        "list" -> "\uD83D\uDCCB"
        "search" -> "\uD83D\uDD0D"
        "package" -> "\uD83D\uDCE6"
        else -> ""
    }
    Text(
        "$emoji $title",
        style = GlassTypography.labelMedium.copy(
            color = GlassColors.accent,
            fontWeight = FontWeight.SemiBold
        ),
        modifier = modifier
    )
}

// ════════════════════════════════════════════════════════════════════════════════
// Sort Chips
// ════════════════════════════════════════════════════════════════════════════════

private val sortOptions = listOf(
    "downloads" to "\u2B07 По скачиваниям",
    "stars" to "\u2B50 По звёздам",
    "recent" to "\uD83C\uDD95 Новые"
)

private fun sortLabel(sort: String): String =
    sortOptions.find { it.first == sort }?.second ?: "\u2B07 По скачиваниям"

@Composable
private fun SortChips(
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        sortOptions.forEach { (key, label) ->
            FilterChip(
                label = label,
                isSelected = selected == key,
                onClick = { onSelect(key) }
            )
        }
    }
}

// ════════════════════════════════════════════════════════════════════════════════
// Categories Chips
// ════════════════════════════════════════════════════════════════════════════════

@Composable
private fun CategoriesChips(
    categories: List<MergedCategory>,
    selected: String?,
    onSelect: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        categories.filter { it.id != "all" }.forEach { category ->
            FilterChip(
                label = category.name,
                isSelected = selected == category.id,
                onClick = { onSelect(if (selected == category.id) null else category.id) }
            )
        }
    }
}

@Composable
private fun FilterChip(
    label: String,
    isSelected: Boolean,
    count: Int = 0,
    onClick: () -> Unit
) {
    val accentColor = LocalAppTheme.current.primary
    val chatBg = LocalChatBackground.current
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(
                if (isSelected) accentColor.copy(alpha = 0.18f)
                else chatBg.surfaceColor.copy(alpha = 0.6f)
            )
            .border(
                1.dp,
                if (isSelected) accentColor.copy(alpha = 0.45f)
                else GlassColors.whiteOverlay10,
                RoundedCornerShape(20.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                label,
                style = GlassTypography.labelSmall.copy(
                    color = if (isSelected) accentColor else GlassColors.textSecondary,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                )
            )
            if (count > 0 && !isSelected) {
                Spacer(Modifier.width(4.dp))
                Text(
                    formatCount(count),
                    style = GlassTypography.timestamp.copy(
                        color = GlassColors.textMuted,
                        fontSize = 10.sp
                    )
                )
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════════════════════
// Skill List Card
// ════════════════════════════════════════════════════════════════════════════════

@Composable
private fun SkillListCard(
    skill: MarketplaceSkill,
    onClick: () -> Unit,
    onInstall: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accentColor = LocalAppTheme.current.primary
    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        borderColor = if (skill.isInstalled) GlassColors.mint.copy(alpha = 0.25f)
        else GlassColors.whiteOverlay10
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accentColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Text(skill.icon ?: "\uD83D\uDD0C", fontSize = 22.sp)
            }

            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    skill.name,
                    style = GlassTypography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (skill.author.isNotBlank()) {
                    Text(
                        skill.author,
                        style = GlassTypography.timestamp.copy(color = GlassColors.textMuted),
                        maxLines = 1
                    )
                }

                if (skill.description.isNotBlank()) {
                    Text(
                        skill.description,
                        style = GlassTypography.timestamp.copy(color = GlassColors.textSecondary),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                Row(
                    modifier = Modifier.padding(top = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "\u2B50 ${formatCount(skill.stars)}",
                        style = GlassTypography.timestamp.copy(color = GlassColors.warning)
                    )
                    skill.category?.let { cat ->
                        Text(
                            cat,
                            style = GlassTypography.timestamp.copy(color = GlassColors.textMuted)
                        )
                    }
                }
            }

            Spacer(Modifier.width(8.dp))

            if (skill.isInstalled) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(GlassColors.mint.copy(alpha = 0.12f))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        "\u2713",
                        style = GlassTypography.labelSmall.copy(
                            color = GlassColors.mint,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(accentColor.copy(alpha = 0.12f))
                        .border(
                            1.dp,
                            accentColor.copy(alpha = 0.25f),
                            RoundedCornerShape(8.dp)
                        )
                        .clickable { onInstall() }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        "+",
                        style = GlassTypography.labelMedium.copy(
                            color = accentColor,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════════════════════
// Skill Details Dialog
// ════════════════════════════════════════════════════════════════════════════════

@Composable
private fun SkillDetailsDialog(
    skill: MarketplaceSkill,
    onDismiss: () -> Unit,
    onInstall: () -> Unit
) {
    val uriHandler = LocalUriHandler.current
    val currentTheme = LocalAppTheme.current
    val chatBg = LocalChatBackground.current

    Dialog(onDismissRequest = onDismiss) {
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            shape = RoundedCornerShape(22.dp),
            backgroundColor = chatBg.surfaceColor.copy(alpha = 0.97f)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth()
            ) {
                // Header
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        currentTheme.primary.copy(alpha = 0.2f),
                                        currentTheme.secondary.copy(alpha = 0.15f)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(skill.icon ?: "\uD83D\uDD0C", fontSize = 28.sp)
                    }

                    Spacer(Modifier.width(14.dp))

                    Column(Modifier.weight(1f)) {
                        Text(skill.name, style = GlassTypography.titleSmall)
                        if (skill.author.isNotBlank()) {
                            Text(
                                skill.author,
                                style = GlassTypography.timestamp.copy(color = GlassColors.textMuted)
                            )
                        }
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Закрыть",
                            tint = GlassColors.textMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Stats row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatChip("\u2B50 ${formatCount(skill.stars)}", GlassColors.warning)
                    skill.category?.let { cat ->
                        StatChip(cat, GlassColors.accentSecondary)
                    }
                    if (skill.downloads > 0) {
                        StatChip("${formatCount(skill.downloads)} \u2193", GlassColors.info)
                    }
                }

                Spacer(Modifier.height(14.dp))

                // Description
                if (skill.description.isNotBlank()) {
                    Text(
                        skill.description,
                        style = GlassTypography.messageText.copy(
                            color = GlassColors.textSecondary,
                            lineHeight = 20.sp
                        )
                    )
                    Spacer(Modifier.height(14.dp))
                }

                // Tags
                if (skill.tags.isNotEmpty()) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                    ) {
                        skill.tags.forEach { tag ->
                            GlassChip(text = tag, color = currentTheme.primary)
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                }

                // Links
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    skill.repo?.let { repo ->
                        LinkButton(
                            text = "GitHub",
                            onClick = { uriHandler.openUri(repo) }
                        )
                    }
                    skill.url?.let { url ->
                        LinkButton(
                            text = "Подробнее",
                            onClick = { uriHandler.openUri(url) }
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))

                // Install button
                if (skill.isInstalled) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(GlassColors.mint.copy(alpha = 0.12f))
                            .border(
                                1.dp,
                                GlassColors.mint.copy(alpha = 0.25f),
                                RoundedCornerShape(12.dp)
                            )
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "\u2713 Установлен",
                            style = GlassTypography.labelMedium.copy(
                                color = GlassColors.mint,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(currentTheme.accentGradient)
                            .clickable { onInstall() }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Установить",
                            style = GlassTypography.labelMedium.copy(
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatChip(text: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.1f))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text,
            style = GlassTypography.timestamp.copy(
                color = color,
                fontWeight = FontWeight.Medium
            )
        )
    }
}

@Composable
private fun LinkButton(
    text: String,
    onClick: () -> Unit
) {
    val accentColor = LocalAppTheme.current.primary
    val chatBg = LocalChatBackground.current
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(chatBg.surfaceColor)
            .border(1.dp, GlassColors.whiteOverlay10, RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text,
            style = GlassTypography.timestamp.copy(color = accentColor)
        )
    }
}

// ════════════════════════════════════════════════════════════════════════════════
// Empty State
// ════════════════════════════════════════════════════════════════════════════════

@Composable
private fun EmptyState(icon: String, title: String, subtitle: String) {
    val emoji = when (icon) {
        "search" -> "\uD83D\uDD0D"
        "package" -> "\uD83D\uDCE6"
        else -> "\uD83D\uDD0C"
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 48.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(emoji, fontSize = 40.sp)
            Spacer(Modifier.height(12.dp))
            Text(title, style = GlassTypography.labelMedium)
            Text(subtitle, style = GlassTypography.timestamp)
        }
    }
}

// ════════════════════════════════════════════════════════════════════════════════
// Helpers
// ════════════════════════════════════════════════════════════════════════════════

private fun formatCount(num: Int): String = when {
    num >= 1_000_000 -> String.format("%.1fM", num / 1_000_000.0)
    num >= 1_000 -> String.format("%.1fK", num / 1_000.0)
    else -> num.toString()
}
