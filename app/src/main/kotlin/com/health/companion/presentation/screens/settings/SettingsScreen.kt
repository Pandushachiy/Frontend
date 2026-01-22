package com.health.companion.presentation.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.health.companion.presentation.components.GlassMorphismCard
import com.health.companion.utils.CrashLogger
import com.health.companion.utils.VoiceEventLogger

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onNavigateToLogin: () -> Unit = {},
    onOpenProfile: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val userName by viewModel.userName.collectAsState()
    val userEmail by viewModel.userEmail.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val notificationsEnabled by viewModel.notificationsEnabled.collectAsState()
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    var showCrashDialog by remember { mutableStateOf(false) }
    var crashLogText by remember { mutableStateOf("") }
    var showVoiceLogDialog by remember { mutableStateOf(false) }
    var voiceLogText by remember { mutableStateOf("") }
    val voicePrefs = remember { context.getSharedPreferences("voice_prefs", android.content.Context.MODE_PRIVATE) }
    var safeVoiceMode by remember { mutableStateOf(voicePrefs.getBoolean("safe_mode", true)) }
    var autoSendVoice by remember { mutableStateOf(voicePrefs.getBoolean("auto_send_voice", true)) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Header
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // Diagnostics quick access
        GlassMorphismCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Column {
                SettingsClickItem(
                    icon = Icons.Default.BugReport,
                    title = "Журнал падений",
                    subtitle = "Показать и скопировать последний краш",
                    onClick = {
                        crashLogText = CrashLogger.readCrash(context)
                        showCrashDialog = true
                    }
                )
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                )
                SettingsClickItem(
                    icon = Icons.Default.Mic,
                    title = "События голоса",
                    subtitle = "Последние шаги перед закрытием",
                    onClick = {
                        voiceLogText = VoiceEventLogger.read(context) + "\n\n" + buildVoiceDiagnostics(context)
                        showVoiceLogDialog = true
                    }
                )
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                )
                SettingsToggleItem(
                    icon = Icons.Default.Security,
                    title = "Безопасный режим голоса",
                    subtitle = "Использовать системное окно распознавания",
                    checked = safeVoiceMode,
                    onCheckedChange = {
                        safeVoiceMode = it
                        voicePrefs.edit().putBoolean("safe_mode", it).apply()
                    }
                )
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                )
                SettingsToggleItem(
                    icon = Icons.Default.Send,
                    title = "Автоотправка голоса",
                    subtitle = "Отправлять текст сразу после распознавания",
                    checked = autoSendVoice,
                    onCheckedChange = {
                        autoSendVoice = it
                        voicePrefs.edit().putBoolean("auto_send_voice", it).apply()
                    }
                )
            }
        }

        // Profile Section
        GlassMorphismCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = userName.firstOrNull()?.uppercase() ?: "U",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = userName.ifEmpty { "Пользователь" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = userEmail,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
                IconButton(onClick = onOpenProfile) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
                }
            }
        }

        // Preferences Section
        SectionTitle(text = "Настройки")
        
        GlassMorphismCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Column {
                SettingsToggleItem(
                    icon = Icons.Default.DarkMode,
                    title = "Тёмная тема",
                    subtitle = "Включить тёмное оформление",
                    checked = isDarkMode,
                    onCheckedChange = { viewModel.setDarkMode(it) }
                )
                
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                )
                
                SettingsToggleItem(
                    icon = Icons.Default.Notifications,
                    title = "Уведомления",
                    subtitle = "Получать напоминания о здоровье",
                    checked = notificationsEnabled,
                    onCheckedChange = { viewModel.setNotificationsEnabled(it) }
                )
            }
        }

        // About Section
        SectionTitle(text = "О приложении")
        
        GlassMorphismCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            SettingsInfoItem(
                icon = Icons.Default.Info,
                title = "Версия",
                value = "1.0.0"
            )
        }

        // Logout Button
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(
            onClick = { 
                viewModel.logout()
                onNavigateToLogin()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Log Out",
                style = MaterialTheme.typography.titleMedium
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
    }

    if (showCrashDialog) {
        AlertDialog(
            onDismissRequest = { showCrashDialog = false },
            title = { Text("Последний краш") },
            text = {
                Text(
                    text = crashLogText,
                    style = MaterialTheme.typography.bodySmall
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    clipboardManager.setText(AnnotatedString(crashLogText))
                }) {
                    Text("Скопировать")
                }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        CrashLogger.clearCrash(context)
                        crashLogText = "Нет данных о падениях"
                    }) {
                        Text("Очистить")
                    }
                    TextButton(onClick = { showCrashDialog = false }) {
                        Text("Закрыть")
                    }
                }
            }
        )
    }

    if (showVoiceLogDialog) {
        AlertDialog(
            onDismissRequest = { showVoiceLogDialog = false },
            title = { Text("События голоса") },
            text = {
                Text(
                    text = voiceLogText,
                    style = MaterialTheme.typography.bodySmall
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    clipboardManager.setText(AnnotatedString(voiceLogText))
                }) {
                    Text("Скопировать")
                }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        VoiceEventLogger.clear(context)
                        voiceLogText = "Нет событий"
                    }) {
                        Text("Очистить")
                    }
                    TextButton(onClick = { showVoiceLogDialog = false }) {
                        Text("Закрыть")
                    }
                }
            }
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
    )
}

@Composable
private fun SettingsToggleItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
        
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.primary,
                checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            )
        )
    }
}

@Composable
private fun SettingsClickItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    isDestructive: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
        
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
        )
    }
}

@Composable
private fun SettingsInfoItem(
    icon: ImageVector,
    title: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}

private fun buildVoiceDiagnostics(context: android.content.Context): String {
    val pm = context.packageManager
    val recIntent = android.content.Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
    val handlers = pm.queryIntentActivities(recIntent, 0)
    val handlerList = if (handlers.isEmpty()) "none" else handlers.joinToString { it.activityInfo.packageName }
    val isAvailable = android.speech.SpeechRecognizer.isRecognitionAvailable(context)
    return buildString {
        appendLine("SpeechRecognizer available: $isAvailable")
        appendLine("RecognizerIntent handlers: $handlerList")
    }
}
