package com.health.companion.presentation.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.health.companion.R
import com.health.companion.presentation.components.GlassColors
import com.health.companion.presentation.theme.LocalAppTheme
import com.health.companion.presentation.theme.LocalChatBackground

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onOpenProfile: () -> Unit = {},
    onOpenSkills: () -> Unit = {},
    onOpenSettingsDetail: () -> Unit = {},
    bottomPadding: Dp = 0.dp
) {
    val userName by viewModel.userName.collectAsStateWithLifecycle()
    val userEmail by viewModel.userEmail.collectAsStateWithLifecycle()
    val avatarEmoji by viewModel.avatarEmoji.collectAsStateWithLifecycle()
    val selectedTheme by viewModel.selectedTheme.collectAsStateWithLifecycle()
    val currentTheme = LocalAppTheme.current
    val chatBg = LocalChatBackground.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(chatBg.gradient)
            .statusBarsPadding()
            .padding(bottom = bottomPadding)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // ── Hero: Аватарка ──────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpenProfile)
                .padding(top = 24.dp, bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(contentAlignment = Alignment.BottomEnd) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .shadow(12.dp, CircleShape, spotColor = currentTheme.primary.copy(alpha = 0.5f))
                        .background(currentTheme.accentGradient, CircleShape)
                        .border(2.dp, currentTheme.primary.copy(alpha = 0.6f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (!avatarEmoji.isNullOrBlank()) {
                        Text(
                            text = avatarEmoji!!,
                            fontSize = 36.sp
                        )
                    } else {
                        Text(
                            text = userName.firstOrNull()?.uppercase()?.toString()
                                ?: stringResource(R.string.default_avatar_letter),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(currentTheme.primary, CircleShape)
                        .border(1.5.dp, chatBg.surfaceColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                }
            }
            Text(
                text = userName.ifEmpty { stringResource(R.string.default_user_name) },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = GlassColors.textPrimary
            )
            if (userEmail.isNotEmpty()) {
                Text(
                    text = userEmail,
                    style = MaterialTheme.typography.bodySmall,
                    color = GlassColors.textSecondary,
                    fontSize = 12.sp
                )
            }
        }

        Column(
            modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {

        // ── Возможности ──────────────────────────────────────────
        SectionLabel("Возможности")

        CompactNavCard(
            iconBg = Brush.linearGradient(listOf(Color(0xFF667eea), Color(0xFF764ba2))),
            iconEmoji = "🔌",
            title = stringResource(R.string.ai_skills),
            subtitle = stringResource(R.string.ai_skills_subtitle),
            onClick = onOpenSkills
        )

        // ── Настройки ────────────────────────────────────────────
        CompactNavCard(
            iconContent = {
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
            },
            title = stringResource(R.string.settings_title),
            subtitle = stringResource(R.string.settings_detail_subtitle),
            onClick = onOpenSettingsDetail
        )

        } // end inner Column (padded content)
    }
}

// ──────────────────────────────────────────────────────────────────
// HELPERS
// ──────────────────────────────────────────────────────────────────

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        color = GlassColors.textMuted,
        letterSpacing = 1.sp,
        fontSize = 10.sp,
        modifier = Modifier.padding(start = 4.dp, top = 4.dp)
    )
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
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

@Composable
private fun CompactNavCard(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    icon: ImageVector? = null,
    iconContent: (@Composable () -> Unit)? = null,
    iconBg: Brush? = null,
    iconEmoji: String? = null
) {
    SettingsCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            when {
                iconContent != null -> iconContent()
                iconBg != null && iconEmoji != null -> Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(iconBg, RoundedCornerShape(7.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(iconEmoji, fontSize = 14.sp)
                }
                icon != null -> Icon(icon, contentDescription = null, tint = GlassColors.textSecondary, modifier = Modifier.size(18.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = GlassColors.textPrimary
                )
                Text(
                    text = subtitle,
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
}
