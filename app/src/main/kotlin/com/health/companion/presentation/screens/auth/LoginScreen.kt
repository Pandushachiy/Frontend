package com.health.companion.presentation.screens.auth

import android.graphics.BlurMaskFilter
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
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
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import com.health.companion.R
import com.health.companion.presentation.components.GlassColors
import com.health.companion.presentation.theme.LocalAppTheme
import com.health.companion.presentation.theme.LocalChatBackground
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    viewModel: AuthViewModel = hiltViewModel(),
    onNavigateToRegister: () -> Unit,
    onLoginSuccess: () -> Unit
) {
    val appTheme = LocalAppTheme.current
    val chatBg = LocalChatBackground.current

    val uiState by viewModel.uiState.collectAsState()
    val email by viewModel.email.collectAsState()
    val password by viewModel.password.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var passwordVisible by remember { mutableStateOf(false) }
    var showForgotDialog by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(uiState) {
        if (uiState is AuthUiState.Success) onLoginSuccess()
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
            Spacer(modifier = Modifier.height(52.dp))

            Box(modifier = Modifier.size(130.dp)) {
                AuthBerryCanvas(primary = appTheme.primary, secondary = appTheme.secondary)
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "FairyBerry",
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = GlassColors.textPrimary,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.login_subtitle),
                fontSize = 14.sp,
                color = GlassColors.textSecondary
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Glass card
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
                        .padding(24.dp)
                ) {
                    Text(
                        text = stringResource(R.string.login_welcome_back),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = GlassColors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.login_sign_in_prompt),
                        fontSize = 13.sp,
                        color = GlassColors.textSecondary
                    )

                    Spacer(modifier = Modifier.height(22.dp))

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
                                scrollState.animateScrollTo(600)
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
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                focusManager.clearFocus()
                                viewModel.login()
                            }
                        ),
                        onFocused = {
                            coroutineScope.launch {
                                kotlinx.coroutines.delay(150)
                                scrollState.animateScrollTo(600)
                            }
                        }
                    )

                    TextButton(
                        onClick = { showForgotDialog = true },
                        modifier = Modifier.align(Alignment.End),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text(
                            stringResource(R.string.forgot_password),
                            fontSize = 13.sp,
                            color = appTheme.primary.copy(alpha = 0.85f)
                        )
                    }

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
                                    text = (uiState as? AuthUiState.Error)?.message ?: "",
                                    fontSize = 13.sp,
                                    color = GlassColors.error,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }

                    val canLogin = !isLoading && email.isNotBlank() && password.isNotBlank()
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                if (canLogin) appTheme.accentGradient
                                else Brush.linearGradient(
                                    listOf(Color.White.copy(alpha = 0.09f), Color.White.copy(alpha = 0.04f))
                                )
                            )
                            .then(if (canLogin) Modifier.clickable { viewModel.login() } else Modifier),
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
                                stringResource(R.string.sign_in),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (canLogin) Color.White else Color.White.copy(alpha = 0.35f)
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
                Text(stringResource(R.string.no_account), fontSize = 14.sp, color = GlassColors.textSecondary)
                TextButton(onClick = onNavigateToRegister) {
                    Text(
                        stringResource(R.string.create_account_link),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = appTheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(48.dp))
        }
    }

    if (showForgotDialog) {
        ForgotPasswordDialog(
            accentGradient = appTheme.accentGradient,
            accentColor = appTheme.primary,
            surfaceColor = chatBg.surfaceColor,
            onDismiss = { showForgotDialog = false }
        )
    }
}

@Composable
private fun ForgotPasswordDialog(
    accentGradient: Brush,
    accentColor: Color,
    surfaceColor: Color,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(surfaceColor)
                .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(24.dp))
                .padding(24.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(accentColor.copy(alpha = 0.15f))
                        .border(1.dp, accentColor.copy(alpha = 0.3f), RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.Lock,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    stringResource(R.string.reset_password_title),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = GlassColors.textPrimary
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    stringResource(R.string.reset_password_message),
                    fontSize = 14.sp,
                    color = GlassColors.textSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(20.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(accentGradient)
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center
                ) {
                    Text(stringResource(R.string.understood), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                }
            }
        }
    }
}

// ─── Shared field component ───────────────────────────────────────────────────

@Composable
internal fun AuthGlassTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector,
    trailingIcon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    onTrailingIconClick: (() -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    isError: Boolean = false,
    onFocused: () -> Unit = {}
) {
    val appTheme = LocalAppTheme.current
    var isFocused by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(
                when {
                    isError -> GlassColors.error.copy(alpha = 0.10f)
                    isFocused -> Color.White.copy(alpha = 0.12f)
                    else -> Color.White.copy(alpha = 0.06f)
                }
            )
            .border(
                width = 1.dp,
                color = when {
                    isError -> GlassColors.error.copy(alpha = 0.5f)
                    isFocused -> appTheme.primary.copy(alpha = 0.6f)
                    else -> Color.White.copy(alpha = 0.10f)
                },
                shape = RoundedCornerShape(14.dp)
            )
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                tint = when {
                    isError -> GlassColors.error
                    isFocused -> appTheme.primary
                    else -> Color.White.copy(alpha = 0.40f)
                },
                modifier = Modifier.size(20.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .weight(1f)
                    .onFocusEvent { fs ->
                        val was = isFocused
                        isFocused = fs.isFocused
                        if (fs.isFocused && !was) onFocused()
                    },
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color.White),
                cursorBrush = SolidColor(appTheme.primary),
                singleLine = true,
                visualTransformation = visualTransformation,
                keyboardOptions = keyboardOptions,
                keyboardActions = keyboardActions,
                decorationBox = { inner ->
                    Box {
                        if (value.isEmpty()) {
                            Text(
                                placeholder,
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.White.copy(alpha = 0.35f)
                            )
                        }
                        inner()
                    }
                }
            )

            if (trailingIcon != null) {
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = trailingIcon,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.45f),
                    modifier = Modifier
                        .size(22.dp)
                        .clickable { onTrailingIconClick?.invoke() }
                )
            }
        }
    }
}

// ─── Berry animation using theme colors ──────────────────────────────────────

@Composable
internal fun AuthBerryCanvas(primary: Color, secondary: Color) {
    val t = rememberInfiniteTransition(label = "berry")
    val p1 by t.animateFloat(0f, 1f, infiniteRepeatable(tween(4000, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "p1")
    val p2 by t.animateFloat(0f, 1f, infiniteRepeatable(tween(5200, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "p2")
    val p3 by t.animateFloat(0f, 1f, infiniteRepeatable(tween(3600, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "p3")
    val p4 by t.animateFloat(0f, 1f, infiniteRepeatable(tween(4700, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "p4")

    val leafColor = GlassColors.mint

    Canvas(modifier = Modifier.fillMaxSize()) {
        val cx = size.width / 2f
        val cy = size.height / 2f + 10.dp.toPx()
        val base = 36.dp.toPx()

        drawIntoCanvas { canvas ->
            val glowPaint = Paint().asFrameworkPaint().apply {
                isAntiAlias = true
                color = primary.copy(alpha = 0.30f).toArgb()
                maskFilter = BlurMaskFilter(50.dp.toPx(), BlurMaskFilter.Blur.NORMAL)
            }
            canvas.nativeCanvas.drawCircle(cx, cy, base * 1.3f, glowPaint)

            val bp = Paint().asFrameworkPaint().apply {
                isAntiAlias = true
                maskFilter = BlurMaskFilter(20.dp.toPx(), BlurMaskFilter.Blur.NORMAL)
            }

            bp.color = primary.toArgb()
            val b1x = cx + (p1 - 0.5f) * 14.dp.toPx()
            val b1y = cy + (p2 - 0.5f) * 18.dp.toPx()
            val b1r = base * (0.82f + p3 * 0.14f)
            canvas.nativeCanvas.drawCircle(b1x, b1y, b1r, bp)

            bp.color = secondary.toArgb()
            val b2x = cx + (p2 - 0.5f) * 22.dp.toPx()
            val b2y = cy - 10.dp.toPx() + (p1 - 0.5f) * 24.dp.toPx()
            val b2r = base * (0.38f + p4 * 0.16f)
            canvas.nativeCanvas.drawCircle(b2x, b2y, b2r, bp)

            bp.color = primary.copy(alpha = 0.70f).toArgb()
            val b3x = cx + 13.dp.toPx() + (p3 - 0.5f) * 10.dp.toPx()
            val b3y = cy + (p4 - 0.5f) * 16.dp.toPx()
            val b3r = base * (0.26f + p1 * 0.12f)
            canvas.nativeCanvas.drawCircle(b3x, b3y, b3r, bp)

            val hlp = Paint().asFrameworkPaint().apply {
                isAntiAlias = true
                color = Color.White.copy(alpha = 0.45f + p1 * 0.12f).toArgb()
                maskFilter = BlurMaskFilter(9.dp.toPx(), BlurMaskFilter.Blur.NORMAL)
            }
            canvas.nativeCanvas.drawCircle(b1x - base * 0.3f, b1y - base * 0.35f, base * 0.2f, hlp)

            val lp = Paint().asFrameworkPaint().apply {
                isAntiAlias = true
                color = leafColor.toArgb()
                maskFilter = BlurMaskFilter(2.dp.toPx(), BlurMaskFilter.Blur.NORMAL)
            }
            val leafY = b1y - b1r - 3.dp.toPx()
            canvas.nativeCanvas.save()
            canvas.nativeCanvas.rotate((p1 - 0.5f) * 7f, b1x, leafY)
            canvas.nativeCanvas.drawOval(b1x - 4.dp.toPx(), leafY - 13.dp.toPx(), b1x + 4.dp.toPx(), leafY + 1.dp.toPx(), lp)
            canvas.nativeCanvas.restore()

            canvas.nativeCanvas.save()
            canvas.nativeCanvas.rotate(-26f + (p2 - 0.5f) * 5f, b1x - 6.dp.toPx(), leafY)
            canvas.nativeCanvas.drawOval(b1x - 10.dp.toPx(), leafY - 11.dp.toPx(), b1x - 2.dp.toPx(), leafY + 1.dp.toPx(), lp)
            canvas.nativeCanvas.restore()

            canvas.nativeCanvas.save()
            canvas.nativeCanvas.rotate(26f - (p2 - 0.5f) * 5f, b1x + 6.dp.toPx(), leafY)
            canvas.nativeCanvas.drawOval(b1x + 2.dp.toPx(), leafY - 11.dp.toPx(), b1x + 10.dp.toPx(), leafY + 1.dp.toPx(), lp)
            canvas.nativeCanvas.restore()
        }

        drawCircle(primary.copy(alpha = 0.25f + p1 * 0.30f), 3.dp.toPx(), Offset(cx + 48.dp.toPx(), cy - 26.dp.toPx()))
        drawCircle(secondary.copy(alpha = 0.30f + p3 * 0.25f), 2.5f.dp.toPx(), Offset(cx - 44.dp.toPx(), cy + 18.dp.toPx()))
    }
}

@Composable
internal fun AuthAmbientCanvas(primary: Color, secondary: Color) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        drawIntoCanvas { canvas ->
            val p = Paint().asFrameworkPaint().apply {
                isAntiAlias = true
                maskFilter = BlurMaskFilter(90.dp.toPx(), BlurMaskFilter.Blur.NORMAL)
            }
            p.color = primary.copy(alpha = 0.15f).toArgb()
            canvas.nativeCanvas.drawCircle(size.width * 0.85f, 100.dp.toPx(), 130.dp.toPx(), p)

            p.color = secondary.copy(alpha = 0.11f).toArgb()
            canvas.nativeCanvas.drawCircle(size.width * 0.1f, size.height - 150.dp.toPx(), 150.dp.toPx(), p)
        }
    }
}
