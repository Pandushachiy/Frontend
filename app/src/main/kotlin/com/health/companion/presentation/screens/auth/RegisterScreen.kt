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
import kotlinx.coroutines.launch

@Composable
fun RegisterScreen(
    viewModel: AuthViewModel = hiltViewModel(),
    onNavigateToLogin: () -> Unit,
    onRegisterSuccess: () -> Unit
) {
    val appTheme = LocalAppTheme.current
    val chatBg = LocalChatBackground.current

    val uiState by viewModel.uiState.collectAsState()
    val name by viewModel.name.collectAsState()
    val email by viewModel.email.collectAsState()
    val password by viewModel.password.collectAsState()
    val confirmPassword by viewModel.confirmPassword.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(uiState) {
        if (uiState is AuthUiState.Success) onRegisterSuccess()
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
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Back button
            Row(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.08f))
                        .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape)
                        .clickable(onClick = onNavigateToLogin),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back),
                        tint = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Box(modifier = Modifier.size(90.dp)) {
                AuthBerryCanvas(primary = appTheme.primary, secondary = appTheme.secondary)
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                stringResource(R.string.create_account),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                stringResource(R.string.join_feyberry),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.55f)
            )

            Spacer(modifier = Modifier.height(24.dp))

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
                        .padding(20.dp)
                ) {
                    AuthGlassTextField(
                        value = name,
                        onValueChange = viewModel::updateName,
                        placeholder = stringResource(R.string.name_placeholder),
                        leadingIcon = Icons.Rounded.Person,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        ),
                        onFocused = {
                            coroutineScope.launch {
                                kotlinx.coroutines.delay(150)
                                scrollState.animateScrollTo(200)
                            }
                        }
                    )
                    Text(
                        stringResource(R.string.min_2_chars),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.38f),
                        modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    AuthGlassTextField(
                        value = email,
                        onValueChange = viewModel::updateEmail,
                        placeholder = "Email",
                        leadingIcon = Icons.Rounded.Email,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        ),
                        onFocused = {
                            coroutineScope.launch {
                                kotlinx.coroutines.delay(150)
                                scrollState.animateScrollTo(300)
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    AuthGlassTextField(
                        value = password,
                        onValueChange = viewModel::updatePassword,
                        placeholder = stringResource(R.string.password_placeholder),
                        leadingIcon = Icons.Rounded.Lock,
                        trailingIcon = if (passwordVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                        onTrailingIconClick = { passwordVisible = !passwordVisible },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        ),
                        onFocused = {
                            coroutineScope.launch {
                                kotlinx.coroutines.delay(150)
                                scrollState.animateScrollTo(400)
                            }
                        }
                    )
                    Text(
                        stringResource(R.string.min_6_chars),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.38f),
                        modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    val passwordsMatch = confirmPassword.isEmpty() || password == confirmPassword
                    AuthGlassTextField(
                        value = confirmPassword,
                        onValueChange = viewModel::updateConfirmPassword,
                        placeholder = stringResource(R.string.confirm_password_placeholder),
                        leadingIcon = Icons.Rounded.Lock,
                        trailingIcon = if (confirmPasswordVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                        onTrailingIconClick = { confirmPasswordVisible = !confirmPasswordVisible },
                        visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        isError = !passwordsMatch,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                focusManager.clearFocus()
                                viewModel.register()
                            }
                        ),
                        onFocused = {
                            coroutineScope.launch {
                                kotlinx.coroutines.delay(150)
                                scrollState.animateScrollTo(500)
                            }
                        }
                    )
                    if (!passwordsMatch) {
                        Text(
                            stringResource(R.string.passwords_dont_match),
                            style = MaterialTheme.typography.labelSmall,
                            color = GlassColors.error,
                            modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    AnimatedVisibility(
                        visible = uiState is AuthUiState.Error,
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
                                    (uiState as? AuthUiState.Error)?.message ?: "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = GlassColors.error,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }

                    val canRegister = !isLoading &&
                            name.length >= 2 &&
                            email.isNotBlank() &&
                            password.length >= 6 &&
                            password == confirmPassword

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                if (canRegister) appTheme.accentGradient
                                else androidx.compose.ui.graphics.Brush.linearGradient(
                                    listOf(Color.White.copy(alpha = 0.09f), Color.White.copy(alpha = 0.04f))
                                )
                            )
                            .then(if (canRegister) Modifier.clickable { viewModel.register() } else Modifier),
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
                                stringResource(R.string.create_account),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = if (canRegister) Color.White else Color.White.copy(alpha = 0.35f)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(R.string.already_have_account),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.55f)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    stringResource(R.string.sign_in),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = appTheme.primary,
                    modifier = Modifier.clickable(onClick = onNavigateToLogin)
                )
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}
