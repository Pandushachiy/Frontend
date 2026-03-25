package com.health.companion.presentation.screens.profile

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.health.companion.R
import com.health.companion.presentation.components.*
import com.health.companion.presentation.screens.settings.SettingsViewModel
import com.health.companion.presentation.theme.LocalAppTheme
import com.health.companion.presentation.theme.LocalChatBackground

private val AVATAR_EMOJIS = listOf(
    "A", "🦊", "🐻", "🐺", "🐱", "🐯",
    "🦋", "🌺", "⚡", "🎯", "🔥", "💎",
    "🌙", "🚀", "👽", "🎸", "🌊", "🍀",
    "❄️", "🎮", "🎵", "🏀", "✈️", "🌈",
)

private val LANGUAGES = listOf("ru" to "Русский", "en" to "English")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onBack: () -> Unit = {},
    onLogout: () -> Unit = {},
    onChangePassword: () -> Unit = {},
    viewModel: ProfileScreenViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {
    val theme = LocalAppTheme.current
    val chatBg = LocalChatBackground.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    val isDeletingAllData by settingsViewModel.isDeletingAllData.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var showAvatarSheet by remember { mutableStateOf(false) }
    var showDeleteAllConfirm by remember { mutableStateOf(false) }
    var medExpanded by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbar.showSnackbar(it, duration = SnackbarDuration.Short)
            viewModel.consumeMessage()
        }
    }

    LaunchedEffect(state.avatarEmoji) {
        if (!state.isLoading) {
            context.getSharedPreferences("profile_prefs", Context.MODE_PRIVATE)
                .edit().putString("profile_avatar_emoji", state.avatarEmoji).apply()
        }
    }

    Box(Modifier.fillMaxSize().background(chatBg.gradient)) {
        SnackbarHost(
            snackbar,
            Modifier.align(Alignment.BottomCenter).navigationBarsPadding()
                .padding(bottom = 16.dp).zIndex(1f)
        )

        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            // ── TopBar ─────────────────────────────────────────
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = GlassColors.textPrimary)
                }
                Text(stringResource(R.string.profile_title), style = GlassTypography.titleSmall)
            }

            if (state.isLoading) {
                Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator(color = theme.primary)
                }
            } else {
                Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp)
                        .imePadding(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Spacer(Modifier.height(8.dp))

                    // ── Avatar ──────────────────────────────────
                    Box(Modifier.fillMaxWidth(), Alignment.Center) {
                        Box(
                            Modifier
                                .size(56.dp)
                                .background(theme.accentGradient, CircleShape)
                                .border(1.5.dp, theme.primary.copy(alpha = 0.3f), CircleShape)
                                .clip(CircleShape)
                                .clickable { showAvatarSheet = true },
                            Alignment.Center
                        ) {
                            val emoji = state.avatarEmoji
                            if (!emoji.isNullOrBlank()) {
                                Text(emoji, fontSize = 26.sp)
                            } else {
                                Text(
                                    state.name.firstOrNull()?.uppercase()?.toString() ?: "?",
                                    fontSize = 22.sp, fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    if (state.email.isNotBlank()) {
                        Text(
                            state.email,
                            style = GlassTypography.timestamp,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(Modifier.height(4.dp))

                    // ── Основное ────────────────────────────────
                    SectionLabel(stringResource(R.string.profile_section_basic))

                    GlassCard(shape = GlassShapes.medium) {
                        Column(Modifier.fillMaxWidth()) {
                            CompactField(stringResource(R.string.profile_field_name), state.name, viewModel::onName, theme.primary)
                            GlassDivider()
                            CompactField(stringResource(R.string.profile_field_nickname), state.nickname, viewModel::onNickname, theme.primary)
                            GlassDivider()
                            CompactField(stringResource(R.string.profile_field_age), state.age, viewModel::onAge, theme.primary, KeyboardType.Number)
                            GlassDivider()
                            CompactDropdown(stringResource(R.string.profile_field_language), state.language, viewModel::onLanguage)
                        }
                    }

                    // ── Медкарта (expandable) ───────────────────
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(GlassShapes.small)
                            .clickable { medExpanded = !medExpanded }
                            .padding(horizontal = 4.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            stringResource(R.string.profile_section_medcard).uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = GlassColors.textMuted,
                            letterSpacing = 1.sp,
                            fontSize = 10.sp
                        )
                        Spacer(Modifier.weight(1f))
                        Icon(
                            if (medExpanded) Icons.Default.KeyboardArrowUp
                            else Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = GlassColors.textMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    AnimatedVisibility(
                        visible = medExpanded,
                        enter = expandVertically(),
                        exit = shrinkVertically()
                    ) {
                        GlassCard(shape = GlassShapes.medium) {
                            Column(Modifier.fillMaxWidth()) {
                                CompactField(stringResource(R.string.profile_field_height), state.height, viewModel::onHeight, theme.primary, KeyboardType.Number)
                                GlassDivider()
                                CompactField(stringResource(R.string.profile_field_weight), state.weight, viewModel::onWeight, theme.primary, KeyboardType.Number)
                                GlassDivider()
                                CompactField(stringResource(R.string.profile_field_allergies), state.allergies, viewModel::onAllergies, theme.primary, placeholder = stringResource(R.string.profile_comma_hint))
                                GlassDivider()
                                CompactField(stringResource(R.string.profile_field_diseases), state.diseases, viewModel::onDiseases, theme.primary, placeholder = stringResource(R.string.profile_comma_hint))
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    // ── Save ────────────────────────────────────
                    val canSave = !state.isSaving && state.name.isNotBlank()
                    GlassButton(
                        onClick = { viewModel.save() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .alpha(if (canSave) 1f else 0.45f),
                        enabled = canSave
                    ) {
                        if (state.isSaving) {
                            CircularProgressIndicator(
                                Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                stringResource(R.string.save_action),
                                style = GlassTypography.labelMedium,
                                color = Color.White
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // ── Actions ─────────────────────────────────
                    SectionLabel(stringResource(R.string.settings_title))

                    GlassCard(shape = GlassShapes.medium) {
                        Column(Modifier.fillMaxWidth()) {
                            ActionRow(
                                icon = Icons.Default.Lock,
                                iconColor = theme.primary,
                                title = stringResource(R.string.change_password_title),
                                subtitle = stringResource(R.string.change_password_subtitle),
                                onClick = onChangePassword
                            )
                            GlassDivider()
                            ActionRow(
                                icon = Icons.Default.DeleteForever,
                                iconColor = Color(0xFFEF4444),
                                title = stringResource(R.string.delete_all_data),
                                subtitle = stringResource(R.string.delete_all_data_message).take(45) + "…",
                                onClick = { showDeleteAllConfirm = true },
                                isLoading = isDeletingAllData
                            )
                            GlassDivider()
                            ActionRow(
                                icon = Icons.AutoMirrored.Filled.ExitToApp,
                                iconColor = Color(0xFFF59E0B),
                                title = stringResource(R.string.sign_out),
                                onClick = {
                                    settingsViewModel.logout()
                                    onLogout()
                                }
                            )
                        }
                    }

                    Spacer(Modifier.height(32.dp))
                }
            }
        }

        if (showDeleteAllConfirm) {
            AlertDialog(
                onDismissRequest = { showDeleteAllConfirm = false },
                containerColor = chatBg.surfaceColor,
                icon = {
                    Icon(Icons.Default.Warning, null, tint = Color(0xFFEF4444))
                },
                title = {
                    Text(
                        stringResource(R.string.delete_all_data_title),
                        color = GlassColors.textPrimary,
                        style = GlassTypography.labelMedium,
                        textAlign = TextAlign.Center,
                    )
                },
                text = {
                    Text(
                        stringResource(R.string.delete_all_data_message),
                        color = GlassColors.textSecondary,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                    )
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteAllConfirm = false }) {
                        Text(stringResource(R.string.cancel), color = GlassColors.textMuted)
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showDeleteAllConfirm = false
                            settingsViewModel.deleteAllData {
                                settingsViewModel.logout()
                                onLogout()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            stringResource(R.string.delete_all),
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            )
        }

        if (showAvatarSheet) {
            AvatarPickerSheet(
                selected = state.avatarEmoji,
                name = state.name,
                primary = theme.primary,
                onPick = { viewModel.onAvatar(it); showAvatarSheet = false },
                onDismiss = { showAvatarSheet = false }
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════
// Action row (profile actions)
// ═══════════════════════════════════════════════════════════

@Composable
private fun ActionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit,
    isLoading: Boolean = false
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(enabled = !isLoading, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            Modifier
                .size(32.dp)
                .background(iconColor.copy(alpha = 0.12f), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = iconColor, modifier = Modifier.size(16.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = GlassTypography.labelSmall, color = GlassColors.textPrimary)
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = GlassColors.textMuted,
                    fontSize = 10.sp,
                    maxLines = 1
                )
            }
        }
        if (isLoading) {
            CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
        }
    }
}

// ═══════════════════════════════════════════════════════════
// Compact inline field (Settings-style)
// ═══════════════════════════════════════════════════════════

@Composable
private fun CompactField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    accentColor: Color,
    keyboardType: KeyboardType = KeyboardType.Text,
    placeholder: String = ""
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            style = GlassTypography.labelSmall.copy(color = GlassColors.textSecondary),
            modifier = Modifier.widthIn(min = 90.dp)
        )
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = TextStyle(
                fontSize = 14.sp,
                color = GlassColors.textPrimary,
                textAlign = TextAlign.End
            ),
            cursorBrush = SolidColor(accentColor),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            modifier = Modifier.weight(1f),
            decorationBox = { inner ->
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                    if (value.isEmpty() && placeholder.isNotEmpty()) {
                        Text(
                            placeholder,
                            style = TextStyle(
                                fontSize = 14.sp,
                                color = GlassColors.textMuted,
                                textAlign = TextAlign.End
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    inner()
                }
            }
        )
    }
}

@Composable
private fun CompactDropdown(
    label: String,
    selected: String,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val displayLabel = LANGUAGES.firstOrNull { it.first == selected }?.second ?: selected
    val chatBg = LocalChatBackground.current

    Row(
        Modifier
            .fillMaxWidth()
            .clickable { expanded = true }
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            style = GlassTypography.labelSmall.copy(color = GlassColors.textSecondary),
            modifier = Modifier.widthIn(min = 90.dp)
        )
        Spacer(Modifier.weight(1f))

        Box {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(displayLabel, style = GlassTypography.labelSmall.copy(color = GlassColors.textPrimary))
                Spacer(Modifier.width(4.dp))
                Icon(Icons.Default.KeyboardArrowDown, null, Modifier.size(14.dp), tint = GlassColors.textMuted)
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                containerColor = chatBg.surfaceColor
            ) {
                LANGUAGES.forEach { (code, name) ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                name,
                                style = GlassTypography.labelSmall.copy(color = GlassColors.textPrimary)
                            )
                        },
                        onClick = { onSelect(code); expanded = false },
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

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

// ═══════════════════════════════════════════════════════════
// Avatar picker sheet
// ═══════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AvatarPickerSheet(
    selected: String?,
    name: String,
    primary: Color,
    onPick: (String?) -> Unit,
    onDismiss: () -> Unit
) {
    val chatBg = LocalChatBackground.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = chatBg.surfaceColor,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        dragHandle = {
            Box(
                Modifier.padding(top = 10.dp).width(32.dp).height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(GlassColors.whiteOverlay20)
            )
        }
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(6),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .heightIn(max = 180.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(AVATAR_EMOJIS) { emoji ->
                val isLetter = emoji == "A"
                val isSelected = if (isLetter) selected == null || selected == "A" else selected == emoji

                Box(
                    Modifier
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (isSelected) primary.copy(alpha = 0.2f)
                            else GlassColors.whiteOverlay05
                        )
                        .then(
                            if (isSelected) Modifier.border(1.5.dp, primary, RoundedCornerShape(10.dp))
                            else Modifier
                        )
                        .clickable { onPick(if (isLetter) null else emoji) },
                    Alignment.Center
                ) {
                    if (isLetter) {
                        Text(
                            name.firstOrNull()?.uppercase()?.toString() ?: "A",
                            fontSize = 18.sp, fontWeight = FontWeight.Bold,
                            color = if (isSelected) primary else GlassColors.textMuted
                        )
                    } else {
                        Text(emoji, fontSize = 20.sp)
                    }
                }
            }
        }
        Spacer(Modifier.navigationBarsPadding().height(8.dp))
    }
}
