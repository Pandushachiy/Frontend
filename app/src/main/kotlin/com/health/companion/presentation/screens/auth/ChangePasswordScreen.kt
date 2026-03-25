package com.health.companion.presentation.screens.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.health.companion.R
import com.health.companion.presentation.components.GlassColors
import com.health.companion.presentation.theme.LocalAppTheme
import com.health.companion.presentation.theme.LocalChatBackground

@Composable
fun ChangePasswordScreen(
    viewModel: AuthViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val appTheme = LocalAppTheme.current
    val chatBg = LocalChatBackground.current

    val state by viewModel.changePasswordState.collectAsState()
    val currentPassword by viewModel.currentPassword.collectAsState()
    val newPassword by viewModel.newPassword.collectAsState()
    val confirmNewPassword by viewModel.confirmNewPassword.collectAsState()

    var currentVisible by remember { mutableStateOf(false) }
    var newVisible by remember { mutableStateOf(false) }
    var confirmVisible by remember { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()

    LaunchedEffect(state) {
        if (state is ChangePasswordUiState.Success) {
            kotlinx.coroutines.delay(1500)
            viewModel.resetChangePasswordState()
            onBack()
        }
    }

    DisposableEffect(Unit) {
        onDispose { viewModel.resetChangePasswordState() }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(chatBg.gradient)
    ) {
        AuthAmbientCanvas(primary = appTheme.primary, secondary = appTheme.secondary)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .imePadding()
                .statusBarsPadding()
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.08f))
                        .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape)
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back),
                        tint = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    stringResource(R.string.change_password_title),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = GlassColors.textPrimary
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Icon
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(appTheme.primary.copy(alpha = 0.15f))
                    .border(1.dp, appTheme.primary.copy(alpha = 0.30f), RoundedCornerShape(18.dp))
                    .align(Alignment.CenterHorizontally),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.Lock,
                    contentDescription = null,
                    tint = appTheme.primary,
                    modifier = Modifier.size(30.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                stringResource(R.string.create_strong_password),
                fontSize = 14.sp,
                color = GlassColors.textSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Form card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(chatBg.surfaceColor.copy(alpha = 0.85f))
                    .background(appTheme.surfaceTint.copy(alpha = 0.05f))
                    .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(24.dp))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AuthGlassTextField(
                        value = currentPassword,
                        onValueChange = viewModel::updateCurrentPassword,
                        placeholder = stringResource(R.string.current_password),
                        leadingIcon = Icons.Rounded.LockOpen,
                        trailingIcon = if (currentVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                        onTrailingIconClick = { currentVisible = !currentVisible },
                        visualTransformation = if (currentVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        )
                    )

                    HorizontalDivider(color = Color.White.copy(alpha = 0.06f))

                    AuthGlassTextField(
                        value = newPassword,
                        onValueChange = viewModel::updateNewPassword,
                        placeholder = stringResource(R.string.new_password),
                        leadingIcon = Icons.Rounded.Lock,
                        trailingIcon = if (newVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                        onTrailingIconClick = { newVisible = !newVisible },
                        visualTransformation = if (newVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        )
                    )

                    val passwordsMatch = confirmNewPassword.isEmpty() || newPassword == confirmNewPassword
                    AuthGlassTextField(
                        value = confirmNewPassword,
                        onValueChange = viewModel::updateConfirmNewPassword,
                        placeholder = stringResource(R.string.confirm_new_password),
                        leadingIcon = Icons.Rounded.Lock,
                        trailingIcon = if (confirmVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                        onTrailingIconClick = { confirmVisible = !confirmVisible },
                        visualTransformation = if (confirmVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        isError = !passwordsMatch,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                focusManager.clearFocus()
                                viewModel.changePassword()
                            }
                        )
                    )

                    if (!passwordsMatch) {
                        Text(
                            stringResource(R.string.passwords_dont_match),
                            fontSize = 12.sp,
                            color = GlassColors.error,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Hint
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(appTheme.primary.copy(alpha = 0.08f))
                    .border(1.dp, appTheme.primary.copy(alpha = 0.18f), RoundedCornerShape(14.dp))
                    .padding(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Rounded.Info,
                        contentDescription = null,
                        tint = appTheme.primary.copy(alpha = 0.70f),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        stringResource(R.string.password_requirements),
                        fontSize = 12.sp,
                        color = GlassColors.textSecondary,
                        lineHeight = 18.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Error
            AnimatedVisibility(
                visible = state is ChangePasswordUiState.Error,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(GlassColors.error.copy(alpha = 0.12f))
                            .border(1.dp, GlassColors.error.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            (state as? ChangePasswordUiState.Error)?.message ?: "",
                            fontSize = 13.sp,
                            color = GlassColors.error,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            // Success
            AnimatedVisibility(
                visible = state is ChangePasswordUiState.Success,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(GlassColors.success.copy(alpha = 0.12f))
                            .border(1.dp, GlassColors.success.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                Icons.Rounded.CheckCircle,
                                contentDescription = null,
                                tint = GlassColors.success,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                stringResource(R.string.password_changed_success),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = GlassColors.success
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            // Submit button
            val isLoading = state is ChangePasswordUiState.Loading
            val canSubmit = !isLoading &&
                    currentPassword.isNotBlank() &&
                    newPassword.length >= 6 &&
                    newPassword == confirmNewPassword

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        if (canSubmit) appTheme.accentGradient
                        else androidx.compose.ui.graphics.Brush.linearGradient(
                            listOf(Color.White.copy(alpha = 0.09f), Color.White.copy(alpha = 0.04f))
                        )
                    )
                    .then(if (canSubmit) Modifier.clickable { viewModel.changePassword() } else Modifier),
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        stringResource(R.string.change_password_button),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (canSubmit) Color.White else Color.White.copy(alpha = 0.35f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}
