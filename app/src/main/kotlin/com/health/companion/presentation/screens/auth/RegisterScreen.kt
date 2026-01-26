package com.health.companion.presentation.screens.auth

import android.graphics.BlurMaskFilter
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Canvas
import androidx.hilt.navigation.compose.hiltViewModel
import com.health.companion.presentation.components.GlassTheme
import kotlinx.coroutines.launch

private object RegisterColors {
    val core = Color(0xFF9B7BC7)
    val mid = Color(0xFF8B6AAF)
    val outer = Color(0xFF7B5A9F)
    val glow = Color(0xFFB794F6)
    val shine = Color(0xFFD4C4F0)
    val leaf = Color(0xFF5DBE7A)
}

@Composable
fun RegisterScreen(
    viewModel: AuthViewModel = hiltViewModel(),
    onNavigateToLogin: () -> Unit,
    onRegisterSuccess: () -> Unit
) {
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
        if (uiState is AuthUiState.Success) {
            onRegisterSuccess()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(GlassTheme.backgroundGradient)
    ) {
        // Ambient background
        AmbientBackground()
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .imePadding()
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(40.dp))
            
            // Back button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.1f))
                        .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                        .clickable { onNavigateToLogin() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Назад",
                        tint = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Small berry animation
            Box(modifier = Modifier.size(100.dp)) {
                SmallBerryAnimation()
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Title
            Text(
                text = "Создать аккаунт",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Присоединяйся к Feyberry",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.6f)
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Glass Form Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.White.copy(alpha = 0.08f))
                    .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(24.dp))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    // Name Field
                    GlassTextField(
                        value = name,
                        onValueChange = viewModel::updateName,
                        placeholder = "Имя",
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
                        text = "Минимум 2 символа",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.4f),
                        modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Email Field
                    GlassTextField(
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

                    Spacer(modifier = Modifier.height(14.dp))

                    // Password Field
                    GlassTextField(
                        value = password,
                        onValueChange = viewModel::updatePassword,
                        placeholder = "Пароль",
                        leadingIcon = Icons.Rounded.Lock,
                        trailingIcon = if (passwordVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                        onTrailingIconClick = { passwordVisible = !passwordVisible },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
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
                                scrollState.animateScrollTo(400)
                            }
                        }
                    )
                    
                    Text(
                        text = "Минимум 6 символов",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.4f),
                        modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Confirm Password Field
                    val passwordsMatch = confirmPassword.isEmpty() || password == confirmPassword
                    GlassTextField(
                        value = confirmPassword,
                        onValueChange = viewModel::updateConfirmPassword,
                        placeholder = "Подтвердите пароль",
                        leadingIcon = Icons.Rounded.Lock,
                        trailingIcon = if (confirmPasswordVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                        onTrailingIconClick = { confirmPasswordVisible = !confirmPasswordVisible },
                        visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                focusManager.clearFocus()
                                viewModel.register()
                            }
                        ),
                        isError = !passwordsMatch,
                        onFocused = {
                            coroutineScope.launch {
                                kotlinx.coroutines.delay(150)
                                scrollState.animateScrollTo(500)
                            }
                        }
                    )
                    
                    if (!passwordsMatch) {
                        Text(
                            text = "Пароли не совпадают",
                            style = MaterialTheme.typography.labelSmall,
                            color = GlassTheme.statusError,
                            modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Error Message
                    if (uiState is AuthUiState.Error) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(GlassTheme.statusError.copy(alpha = 0.15f))
                                .border(1.dp, GlassTheme.statusError.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = (uiState as AuthUiState.Error).message,
                                style = MaterialTheme.typography.bodySmall,
                                color = GlassTheme.statusError,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Register Button
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
                                if (canRegister)
                                    Brush.linearGradient(
                                        colors = listOf(
                                            GlassTheme.accentPrimary,
                                            GlassTheme.accentSecondary
                                        )
                                    )
                                else
                                    Brush.linearGradient(
                                        colors = listOf(
                                            Color.White.copy(alpha = 0.1f),
                                            Color.White.copy(alpha = 0.05f)
                                        )
                                    )
                            )
                            .then(
                                if (canRegister) Modifier.clickable { viewModel.register() }
                                else Modifier
                            ),
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
                                text = "Создать аккаунт",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = if (canRegister) Color.White else Color.White.copy(alpha = 0.4f)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Login Link
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Уже есть аккаунт?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Войти",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = GlassTheme.accentPrimary,
                    modifier = Modifier.clickable { onNavigateToLogin() }
                )
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
private fun SmallBerryAnimation() {
    val infiniteTransition = rememberInfiniteTransition(label = "berry")
    
    val phase1 by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "p1"
    )
    val phase2 by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "p2"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val centerX = size.width / 2
        val centerY = size.height / 2
        val baseRadius = 28.dp.toPx()
        
        drawIntoCanvas { canvas ->
            val glowPaint = Paint().asFrameworkPaint().apply {
                isAntiAlias = true
                color = RegisterColors.glow.copy(alpha = 0.4f).toArgb()
                maskFilter = BlurMaskFilter(40.dp.toPx(), BlurMaskFilter.Blur.NORMAL)
            }
            canvas.nativeCanvas.drawCircle(centerX, centerY, baseRadius * 1.2f, glowPaint)
            
            val berryPaint = Paint().asFrameworkPaint().apply {
                isAntiAlias = true
                maskFilter = BlurMaskFilter(18.dp.toPx(), BlurMaskFilter.Blur.NORMAL)
            }
            
            berryPaint.color = RegisterColors.core.toArgb()
            val blob1X = centerX + (phase1 - 0.5f) * 8.dp.toPx()
            val blob1Y = centerY + (phase2 - 0.5f) * 10.dp.toPx()
            val blob1R = baseRadius * (0.85f + phase1 * 0.1f)
            canvas.nativeCanvas.drawCircle(blob1X, blob1Y, blob1R, berryPaint)
            
            berryPaint.color = RegisterColors.mid.toArgb()
            val blob2X = centerX + (phase2 - 0.5f) * 12.dp.toPx()
            val blob2Y = centerY - 5.dp.toPx() + (phase1 - 0.5f) * 15.dp.toPx()
            val blob2R = baseRadius * (0.35f + phase2 * 0.15f)
            canvas.nativeCanvas.drawCircle(blob2X, blob2Y, blob2R, berryPaint)
            
            val highlightPaint = Paint().asFrameworkPaint().apply {
                isAntiAlias = true
                color = Color.White.copy(alpha = 0.5f + phase1 * 0.15f).toArgb()
                maskFilter = BlurMaskFilter(8.dp.toPx(), BlurMaskFilter.Blur.NORMAL)
            }
            canvas.nativeCanvas.drawCircle(
                blob1X - baseRadius * 0.3f,
                blob1Y - baseRadius * 0.35f,
                baseRadius * 0.18f,
                highlightPaint
            )
            
            // Leaf
            val leafPaint = Paint().asFrameworkPaint().apply {
                isAntiAlias = true
                color = RegisterColors.leaf.toArgb()
                maskFilter = BlurMaskFilter(2.dp.toPx(), BlurMaskFilter.Blur.NORMAL)
            }
            val leafBaseY = blob1Y - blob1R - 2.dp.toPx()
            canvas.nativeCanvas.save()
            canvas.nativeCanvas.rotate((phase1 - 0.5f) * 6f, blob1X, leafBaseY)
            canvas.nativeCanvas.drawOval(
                blob1X - 3.dp.toPx(),
                leafBaseY - 10.dp.toPx(),
                blob1X + 3.dp.toPx(),
                leafBaseY + 1.dp.toPx(),
                leafPaint
            )
            canvas.nativeCanvas.restore()
        }
    }
}

@Composable
private fun AmbientBackground() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        drawIntoCanvas { canvas ->
            val ambientPaint = Paint().asFrameworkPaint().apply {
                isAntiAlias = true
                maskFilter = BlurMaskFilter(100.dp.toPx(), BlurMaskFilter.Blur.NORMAL)
            }
            
            ambientPaint.color = RegisterColors.mid.copy(alpha = 0.15f).toArgb()
            canvas.nativeCanvas.drawCircle(
                size.width * 0.85f,
                100.dp.toPx(),
                120.dp.toPx(),
                ambientPaint
            )
            
            ambientPaint.color = RegisterColors.glow.copy(alpha = 0.1f).toArgb()
            canvas.nativeCanvas.drawCircle(
                size.width * 0.15f,
                size.height - 150.dp.toPx(),
                140.dp.toPx(),
                ambientPaint
            )
        }
    }
}

@Composable
private fun GlassTextField(
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
    var isFocused by remember { mutableStateOf(false) }
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (isError) GlassTheme.statusError.copy(alpha = 0.1f)
                else Color.White.copy(alpha = if (isFocused) 0.12f else 0.08f)
            )
            .border(
                width = 1.dp,
                color = when {
                    isError -> GlassTheme.statusError.copy(alpha = 0.5f)
                    isFocused -> GlassTheme.accentPrimary.copy(alpha = 0.5f)
                    else -> Color.White.copy(alpha = 0.15f)
                },
                shape = RoundedCornerShape(14.dp)
            )
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                tint = when {
                    isError -> GlassTheme.statusError
                    isFocused -> GlassTheme.accentPrimary
                    else -> Color.White.copy(alpha = 0.5f)
                },
                modifier = Modifier.size(22.dp)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .weight(1f)
                    .onFocusEvent { focusState ->
                        val wasFocused = isFocused
                        isFocused = focusState.isFocused
                        if (focusState.isFocused && !wasFocused) {
                            onFocused()
                        }
                    },
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = Color.White
                ),
                cursorBrush = SolidColor(GlassTheme.accentPrimary),
                singleLine = true,
                visualTransformation = visualTransformation,
                keyboardOptions = keyboardOptions,
                keyboardActions = keyboardActions,
                decorationBox = { innerTextField ->
                    Box {
                        if (value.isEmpty()) {
                            Text(
                                text = placeholder,
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.White.copy(alpha = 0.4f)
                            )
                        }
                        innerTextField()
                    }
                }
            )
            
            if (trailingIcon != null) {
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = trailingIcon,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.5f),
                    modifier = Modifier
                        .size(22.dp)
                        .clickable { onTrailingIconClick?.invoke() }
                )
            }
        }
    }
}
