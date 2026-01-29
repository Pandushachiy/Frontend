package com.health.companion.presentation.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.health.companion.data.remote.api.FactItem
import com.health.companion.presentation.components.*

@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(GlassGradients.backgroundVertical)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = GlassSpacing.screenEdge + 8.dp)
                .statusBarsPadding()
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Header - только кнопки без заголовка
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(GlassColors.surface)
                        .border(1.dp, GlassColors.whiteOverlay10, CircleShape)
                        .clickable { onBack() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = "Назад",
                        tint = GlassColors.textPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = GlassColors.accent,
                        strokeWidth = 2.dp
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(GlassColors.surface)
                            .border(1.dp, GlassColors.whiteOverlay10, CircleShape)
                            .clickable { viewModel.refresh() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Обновить",
                            tint = GlassColors.textSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // User card
            state.profile?.let { profile ->
                UserCardV2(
                    name = profile.user.name,
                    email = profile.user.email
                )

                Spacer(modifier = Modifier.height(16.dp))

                StatsRowV2(
                    factsCount = profile.facts.size,
                    docsCount = profile.documents.size,
                    stats = profile.stats
                )

                Spacer(modifier = Modifier.height(20.dp))
                
                FactsSectionV2(
                    facts = profile.facts,
                    deletingId = state.deletingId,
                    onDelete = { viewModel.deleteFact(it) },
                    onClearAll = { viewModel.clearAllFacts() }
                )
            }

            // Error
            state.error?.let { error ->
                Spacer(modifier = Modifier.height(16.dp))
                ErrorCardV2(message = error)
            }

            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

@Composable
private fun UserCardV2(name: String, email: String) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = GlassShapes.card,
        elevation = 4.dp
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(20.dp)
        ) {
            // Avatar with gradient
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .shadow(4.dp, CircleShape)
                    .clip(CircleShape)
                    .background(GlassGradients.accent),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = name.firstOrNull()?.uppercase() ?: "?",
                    style = GlassTypography.titleLarge.copy(
                        fontSize = 28.sp
                    ),
                    fontWeight = FontWeight.Bold,
                    color = GlassColors.textPrimary
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    text = name,
                    style = GlassTypography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (email.isNotBlank()) {
                    Text(
                        text = email,
                        style = GlassTypography.labelMedium.copy(
                            color = GlassColors.textMuted
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun StatsRowV2(
    factsCount: Int,
    docsCount: Int,
    stats: Map<String, Int>
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        StatChipV2("🧠", factsCount, "фактов", GlassColors.accent, Modifier.weight(1f))
        StatChipV2("📄", docsCount, "документов", GlassColors.mint, Modifier.weight(1f))
    }

    if (stats.isNotEmpty()) {
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            stats.entries.take(2).forEachIndexed { index, (key, value) ->
                val (emoji, color, label) = when (key.lowercase()) {
                    "messages", "messagescount" -> Triple("💬", GlassColors.orange, "сообщений")
                    "conversations", "conversationscount" -> Triple("🗂️", GlassColors.teal, "бесед")
                    "entities", "entitiescount" -> Triple("👤", GlassColors.coral, "сущностей")
                    "factscount" -> Triple("🧠", GlassColors.accent, "фактов")
                    "documentscount" -> Triple("📄", GlassColors.mint, "документов")
                    else -> Triple("📊", GlassColors.textMuted, key)
                }
                StatChipV2(emoji, value, label, color, Modifier.weight(1f))
            }
            if (stats.size == 1) {
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun StatChipV2(
    emoji: String,
    value: Int,
    label: String,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .shadow(2.dp, GlassShapes.medium)
            .clip(GlassShapes.medium)
            .background(color.copy(alpha = 0.12f))
            .border(1.dp, color.copy(alpha = 0.2f), GlassShapes.medium)
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = emoji, fontSize = 18.sp)
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = value.toString(),
                    style = GlassTypography.titleSmall.copy(color = color),
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = label,
                    style = GlassTypography.labelSmall.copy(
                        color = GlassColors.textMuted
                    )
                )
            }
        }
    }
}

@Composable
private fun FactsSectionV2(
    facts: List<FactItem>,
    deletingId: String?,
    onDelete: (String) -> Unit,
    onClearAll: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        SectionHeaderV2(emoji = "🧠", title = "Факты обо мне", count = facts.size)

        if (facts.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .clip(GlassShapes.chip)
                    .background(GlassColors.error.copy(alpha = 0.1f))
                    .clickable { onClearAll() }
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "Очистить",
                    style = GlassTypography.labelSmall.copy(
                        color = GlassColors.error
                    )
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(12.dp))

    if (facts.isEmpty()) {
        EmptyCardV2(
            emoji = "🧠",
            title = "Пока пусто",
            subtitle = "Расскажи о себе в чате"
        )
    } else {
        facts.forEach { fact ->
            FactCardV2(
                fact = fact,
                isDeleting = deletingId == fact.id,
                onDelete = { onDelete(fact.id) }
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun FactCardV2(
    fact: FactItem,
    isDeleting: Boolean,
    onDelete: () -> Unit
) {
    val categoryColor = when (fact.category.lowercase()) {
        "important" -> GlassColors.coral
        "preference" -> GlassColors.teal
        "custom" -> GlassColors.accent
        else -> GlassColors.textMuted
    }

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = GlassShapes.medium
    ) {
        Row(
            verticalAlignment = Alignment.Top,
            modifier = Modifier.padding(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(GlassShapes.small)
                    .background(categoryColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = fact.emoji, fontSize = 18.sp)
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = fact.text,
                    style = GlassTypography.messageText
                )
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .clip(GlassShapes.small)
                        .background(categoryColor.copy(alpha = 0.15f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = fact.category,
                        style = GlassTypography.timestamp.copy(
                            color = categoryColor
                        )
                    )
                }
            }

            if (fact.canDelete) {
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(GlassColors.error.copy(alpha = 0.1f))
                        .clickable(enabled = !isDeleting) { onDelete() },
                    contentAlignment = Alignment.Center
                ) {
                    if (isDeleting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            color = GlassColors.error,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            "×",
                            style = GlassTypography.labelMedium.copy(
                                color = GlassColors.error
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeaderV2(
    emoji: String,
    title: String,
    count: Int? = null
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text = emoji, fontSize = 18.sp)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = GlassTypography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )
        count?.let {
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .clip(GlassShapes.small)
                    .background(GlassColors.whiteOverlay10)
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = it.toString(),
                    style = GlassTypography.labelSmall.copy(
                        color = GlassColors.textSecondary,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }
    }
}

@Composable
private fun EmptyCardV2(emoji: String, title: String, subtitle: String) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = GlassShapes.large,
        backgroundColor = GlassColors.surface.copy(alpha = 0.5f)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Text(text = emoji, fontSize = 32.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                style = GlassTypography.titleSmall.copy(
                    color = GlassColors.textSecondary
                )
            )
            Text(
                text = subtitle,
                style = GlassTypography.labelSmall.copy(
                    color = GlassColors.textMuted
                )
            )
        }
    }
}

@Composable
private fun ErrorCardV2(message: String) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = GlassShapes.medium,
        backgroundColor = GlassColors.error.copy(alpha = 0.1f),
        borderColor = GlassColors.error.copy(alpha = 0.2f)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(12.dp)
        ) {
            Text(text = "⚠️", fontSize = 18.sp)
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = message,
                style = GlassTypography.messageText.copy(
                    color = GlassColors.textSecondary
                )
            )
        }
    }
}
