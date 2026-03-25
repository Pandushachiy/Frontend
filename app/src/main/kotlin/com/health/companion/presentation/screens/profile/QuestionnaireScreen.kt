package com.health.companion.presentation.screens.profile

import android.content.Context
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.health.companion.presentation.screens.settings.SettingsViewModel
import com.health.companion.presentation.theme.LocalAppTheme
import com.health.companion.presentation.theme.LocalChatBackground

private val AVATAR_EMOJIS = listOf(
    "🦊", "🐻", "🐺", "🦁", "🐯", "🦋",
    "🌺", "⚡", "🎯", "🔥", "💎", "🌙",
    "🚀", "👽", "🎸", "🌊", "🍀", "🪐"
)

private const val PREF_AVATAR = "profile_avatar_emoji"

@Composable
fun QuestionnaireScreen(
    onComplete: () -> Unit = {},
    onBack: () -> Unit = {},
    profileViewModel: ProfileViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {
    val currentTheme = LocalAppTheme.current
    val chatBg = LocalChatBackground.current
    val context = LocalContext.current
    val isSaving by profileViewModel.isSaving.collectAsStateWithLifecycle()
    val error by profileViewModel.error.collectAsStateWithLifecycle()

    val prefs = remember { context.getSharedPreferences("profile_prefs", Context.MODE_PRIVATE) }
    val initialName by settingsViewModel.userName.collectAsStateWithLifecycle()
    var name by remember(initialName) {
        mutableStateOf(if (initialName == "Пользователь") "" else initialName)
    }
    var selectedEmoji by remember {
        mutableStateOf(prefs.getString(PREF_AVATAR, null))
    }
    var saved by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(chatBg.gradient)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // ── Top Bar ──────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Назад",
                        tint = Color.White
                    )
                }
                Text(
                    text = "Редактировать",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    modifier = Modifier.weight(1f)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(28.dp)
            ) {
                Spacer(Modifier.height(8.dp))

                // ── Avatar preview + section label ───────────────────
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Current avatar preview
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .background(currentTheme.accentGradient, CircleShape)
                            .border(
                                2.dp,
                                Color.White.copy(alpha = 0.25f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        val display = selectedEmoji
                        if (display != null) {
                            Text(display, fontSize = 34.sp)
                        } else {
                            Text(
                                name.firstOrNull()?.uppercase()?.toString() ?: "?",
                                fontSize = 30.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Выберите аватар",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.45f)
                    )
                }

                // ── Emoji grid ───────────────────────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color.White.copy(alpha = 0.06f))
                        .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(18.dp))
                        .padding(12.dp)
                ) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(6),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.heightIn(max = 160.dp)
                    ) {
                        // "no emoji" option — use first letter
                        item {
                            EmojiCell(
                                emoji = null,
                                label = name.firstOrNull()?.uppercase()?.toString() ?: "A",
                                isSelected = selectedEmoji == null,
                                accentColor = currentTheme.primary,
                                onClick = { selectedEmoji = null }
                            )
                        }
                        items(AVATAR_EMOJIS) { emoji ->
                            EmojiCell(
                                emoji = emoji,
                                isSelected = selectedEmoji == emoji,
                                accentColor = currentTheme.primary,
                                onClick = { selectedEmoji = emoji }
                            )
                        }
                    }
                }

                // ── Name field ───────────────────────────────────────
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Имя",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White.copy(alpha = 0.55f),
                        letterSpacing = 0.8.sp
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.White.copy(alpha = 0.07f))
                            .border(
                                1.dp,
                                if (name.isNotBlank()) currentTheme.primary.copy(alpha = 0.4f)
                                else Color.White.copy(alpha = 0.10f),
                                RoundedCornerShape(14.dp)
                            )
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                    ) {
                        BasicTextField(
                            value = name,
                            onValueChange = {
                                name = it
                                saved = false
                            },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = MaterialTheme.typography.bodyLarge.copy(
                                color = Color.White,
                                fontWeight = FontWeight.Medium
                            ),
                            singleLine = true,
                            cursorBrush = SolidColor(currentTheme.primary),
                            decorationBox = { inner ->
                                Box {
                                    if (name.isEmpty()) {
                                        Text(
                                            "Ваше имя...",
                                            style = MaterialTheme.typography.bodyLarge.copy(
                                                color = Color.White.copy(alpha = 0.3f)
                                            )
                                        )
                                    }
                                    inner()
                                }
                            }
                        )
                    }
                }

                // ── Error ────────────────────────────────────────────
                if (error != null) {
                    Text(
                        error!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFF87171)
                    )
                }
            }

            // ── Save button ──────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 20.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .let { mod ->
                            if (saved) mod.background(Color.White.copy(alpha = 0.1f))
                            else mod.background(currentTheme.accentGradient)
                        }
                        .clickable(enabled = !isSaving && name.isNotBlank()) {
                            // Save avatar to prefs
                            if (selectedEmoji != null) {
                                prefs.edit().putString(PREF_AVATAR, selectedEmoji).apply()
                            } else {
                                prefs.edit().remove(PREF_AVATAR).apply()
                            }
                            // Save name to server
                            profileViewModel.setAnswer("preferredName", name.trim())
                            profileViewModel.saveAnswers {
                                saved = true
                                onComplete()
                            }
                        }
                        .padding(vertical = 15.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                    } else {
                        Text(
                            text = if (saved) "✓ Сохранено" else "Сохранить",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmojiCell(
    emoji: String?,
    label: String = "",
    isSelected: Boolean,
    accentColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(
                if (isSelected) accentColor.copy(alpha = 0.22f)
                else Color.White.copy(alpha = 0.05f)
            )
            .border(
                width = if (isSelected) 1.5.dp else 0.5.dp,
                color = if (isSelected) accentColor.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.08f),
                shape = CircleShape
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (emoji != null) {
            Text(emoji, fontSize = 20.sp)
        } else {
            Text(
                label,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) accentColor else Color.White.copy(alpha = 0.5f)
            )
        }
    }
}
