package com.health.companion.presentation.screens.auth

import android.graphics.BlurMaskFilter
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
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
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.health.companion.presentation.components.GlassTheme
import kotlin.math.sin
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.IntOffset
import kotlinx.coroutines.isActive
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import kotlinx.coroutines.launch
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.runtime.rememberCoroutineScope

private object BerryColors {
    val core = Color(0xFF9B7BC7)
    val mid = Color(0xFF8B6AAF)
    val outer = Color(0xFF7B5A9F)
    val glow = Color(0xFFB794F6)
    val shine = Color(0xFFD4C4F0)
    val leaf = Color(0xFF5DBE7A)
}

@Composable
fun LoginScreen(
    viewModel: AuthViewModel = hiltViewModel(),
    onNavigateToRegister: () -> Unit,
    onLoginSuccess: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val email by viewModel.email.collectAsState()
    val password by viewModel.password.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    
    var passwordVisible by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    

    LaunchedEffect(uiState) {
        if (uiState is AuthUiState.Success) {
            onLoginSuccess()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(GlassTheme.backgroundGradient)
    ) {
        // Ambient canvas background
        AmbientCanvas()
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .imePadding()
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(60.dp))

            // Lava Berry
            Box(modifier = Modifier.size(160.dp)) {
                LavaBerryCanvas()
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "Feyberry",
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
                color = GlassTheme.textPrimary,
                letterSpacing = 1.sp
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = "Твой магический помощник",
                fontSize = 14.sp,
                color = GlassTheme.textSecondary
            )

            Spacer(modifier = Modifier.height(40.dp))

            // Glass card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(GlassTheme.glassWhite)
                    .border(
                        width = 1.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.2f),
                                Color.White.copy(alpha = 0.05f)
                            )
                        ),
                        shape = RoundedCornerShape(24.dp)
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    Text(
                        text = "С возвращением!",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = GlassTheme.textPrimary
                    )

                    Spacer(modifier = Modifier.height(28.dp))

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
                                scrollState.animateScrollTo(500)
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(14.dp))

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
                                scrollState.animateScrollTo(500)
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    TextButton(
                        onClick = { },
                        modifier = Modifier.align(Alignment.End),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("Забыли пароль?", fontSize = 13.sp, color = BerryColors.glow)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

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
                                fontSize = 13.sp,
                                color = GlassTheme.statusError,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    Button(
                        onClick = { viewModel.login() },
                        modifier = Modifier.fillMaxWidth().height(58.dp),
                        enabled = !isLoading && email.isNotBlank() && password.isNotBlank(),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BerryColors.mid,
                            disabledContainerColor = BerryColors.outer.copy(alpha = 0.5f)
                        )
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(Modifier.size(24.dp), Color.White, 2.dp)
                        } else {
                            Text("Войти", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Нет аккаунта?", fontSize = 14.sp, color = GlassTheme.textSecondary)
                TextButton(onClick = onNavigateToRegister) {
                    Text("Создать", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = BerryColors.shine)
                }
            }

            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

/**
 * Лава-ягода через чистый Canvas с BlurMaskFilter
 */
@Composable
private fun LavaBerryCanvas() {
    val infiniteTransition = rememberInfiniteTransition(label = "lava")
    
    // Несколько фаз с разными периодами для органичного движения
    // Все используют Reverse чтобы плавно возвращаться
    val phase1 by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "p1"
    )
    val phase2 by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(5000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "p2"
    )
    val phase3 by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "p3"
    )
    val phase4 by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(4500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "p4"
    )
    val phase5 by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "p5"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val centerX = size.width / 2
        val centerY = size.height / 2 + 10.dp.toPx()
        val baseRadius = 42.dp.toPx()
        
        drawIntoCanvas { canvas ->
            // Краска с размытием для мягких краёв
            val glowPaint = Paint().asFrameworkPaint().apply {
                isAntiAlias = true
                color = BerryColors.glow.copy(alpha = 0.4f).toArgb()
                maskFilter = BlurMaskFilter(60.dp.toPx(), BlurMaskFilter.Blur.NORMAL)
            }
            
            // Внешнее свечение
            canvas.nativeCanvas.drawCircle(
                centerX, centerY, baseRadius * 1.3f, glowPaint
            )
            
            // Основная краска ягоды с blur
            val berryPaint = Paint().asFrameworkPaint().apply {
                isAntiAlias = true
                maskFilter = BlurMaskFilter(25.dp.toPx(), BlurMaskFilter.Blur.NORMAL)
            }
            
            // === ЛАВА КАПЛИ с плавными фазами ===
            
            // Большая центральная
            berryPaint.color = BerryColors.core.toArgb()
            val blob1X = centerX + (phase1 - 0.5f) * 16.dp.toPx()
            val blob1Y = centerY + (phase2 - 0.5f) * 20.dp.toPx()
            val blob1R = baseRadius * (0.8f + phase3 * 0.15f)
            canvas.nativeCanvas.drawCircle(blob1X, blob1Y, blob1R, berryPaint)
            
            // Верхняя капля
            berryPaint.color = BerryColors.mid.toArgb()
            val blob2X = centerX + (phase2 - 0.5f) * 25.dp.toPx()
            val blob2Y = centerY - 10.dp.toPx() + (phase1 - 0.5f) * 30.dp.toPx()
            val blob2R = baseRadius * (0.4f + phase4 * 0.2f)
            canvas.nativeCanvas.drawCircle(blob2X, blob2Y, blob2R, berryPaint)
            
            // Правая капля
            berryPaint.color = BerryColors.glow.copy(alpha = 0.8f).toArgb()
            val blob3X = centerX + 15.dp.toPx() + (phase3 - 0.5f) * 15.dp.toPx()
            val blob3Y = centerY + (phase4 - 0.5f) * 20.dp.toPx()
            val blob3R = baseRadius * (0.3f + phase1 * 0.15f)
            canvas.nativeCanvas.drawCircle(blob3X, blob3Y, blob3R, berryPaint)
            
            // Левая нижняя
            berryPaint.color = BerryColors.core.copy(alpha = 0.9f).toArgb()
            val blob4X = centerX - 12.dp.toPx() + (phase4 - 0.5f) * 18.dp.toPx()
            val blob4Y = centerY + 10.dp.toPx() + (phase5 - 0.5f) * 20.dp.toPx()
            val blob4R = baseRadius * (0.35f + phase2 * 0.15f)
            canvas.nativeCanvas.drawCircle(blob4X, blob4Y, blob4R, berryPaint)
            
            // Маленькая всплывающая
            berryPaint.color = BerryColors.shine.copy(alpha = 0.6f).toArgb()
            val blob5X = centerX + (phase5 - 0.5f) * 12.dp.toPx()
            val blob5Y = centerY + 15.dp.toPx() - phase5 * 30.dp.toPx()
            val blob5R = baseRadius * (0.2f + phase3 * 0.12f)
            canvas.nativeCanvas.drawCircle(blob5X, blob5Y, blob5R, berryPaint)
            
            // Блик
            val highlightPaint = Paint().asFrameworkPaint().apply {
                isAntiAlias = true
                color = Color.White.copy(alpha = 0.45f + phase1 * 0.15f).toArgb()
                maskFilter = BlurMaskFilter(12.dp.toPx(), BlurMaskFilter.Blur.NORMAL)
            }
            canvas.nativeCanvas.drawCircle(
                blob1X - baseRadius * 0.3f,
                blob1Y - baseRadius * 0.35f,
                baseRadius * 0.22f,
                highlightPaint
            )
            
            // Маленький яркий блик
            highlightPaint.color = Color.White.copy(alpha = 0.8f).toArgb()
            highlightPaint.maskFilter = BlurMaskFilter(5.dp.toPx(), BlurMaskFilter.Blur.NORMAL)
            canvas.nativeCanvas.drawCircle(
                blob1X - baseRadius * 0.4f,
                blob1Y - baseRadius * 0.45f,
                baseRadius * 0.08f,
                highlightPaint
            )
            
            // === ЛИСТОЧКИ (привязаны к главной капле) ===
            val leafPaint = Paint().asFrameworkPaint().apply {
                isAntiAlias = true
                maskFilter = BlurMaskFilter(3.dp.toPx(), BlurMaskFilter.Blur.NORMAL)
            }
            
            val leafBaseX = blob1X
            val leafBaseY = blob1Y - blob1R - 4.dp.toPx()
            
            // Центральный листик
            leafPaint.color = BerryColors.leaf.toArgb()
            canvas.nativeCanvas.save()
            canvas.nativeCanvas.rotate((phase1 - 0.5f) * 8f, leafBaseX, leafBaseY)
            canvas.nativeCanvas.drawOval(
                leafBaseX - 5.dp.toPx(),
                leafBaseY - 16.dp.toPx(),
                leafBaseX + 5.dp.toPx(),
                leafBaseY + 2.dp.toPx(),
                leafPaint
            )
            canvas.nativeCanvas.restore()
            
            // Левый листик
            canvas.nativeCanvas.save()
            canvas.nativeCanvas.rotate(-28f + (phase2 - 0.5f) * 6f, leafBaseX - 7.dp.toPx(), leafBaseY)
            canvas.nativeCanvas.drawOval(
                leafBaseX - 11.dp.toPx(),
                leafBaseY - 13.dp.toPx(),
                leafBaseX - 3.dp.toPx(),
                leafBaseY + 2.dp.toPx(),
                leafPaint
            )
            canvas.nativeCanvas.restore()
            
            // Правый листик
            canvas.nativeCanvas.save()
            canvas.nativeCanvas.rotate(28f - (phase2 - 0.5f) * 6f, leafBaseX + 7.dp.toPx(), leafBaseY)
            canvas.nativeCanvas.drawOval(
                leafBaseX + 3.dp.toPx(),
                leafBaseY - 13.dp.toPx(),
                leafBaseX + 11.dp.toPx(),
                leafBaseY + 2.dp.toPx(),
                leafPaint
            )
            canvas.nativeCanvas.restore()
        }
        
        // Искорки
        drawCircle(
            color = BerryColors.shine.copy(alpha = 0.3f + phase1 * 0.4f),
            radius = 4.dp.toPx(),
            center = Offset(centerX + 55.dp.toPx(), centerY - 30.dp.toPx())
        )
        drawCircle(
            color = BerryColors.shine.copy(alpha = 0.4f + phase3 * 0.3f),
            radius = 3.dp.toPx(),
            center = Offset(centerX - 50.dp.toPx(), centerY + 20.dp.toPx())
        )
        drawCircle(
            color = BerryColors.glow.copy(alpha = 0.25f + phase5 * 0.35f),
            radius = 3.5f.dp.toPx(),
            center = Offset(centerX + 45.dp.toPx(), centerY + 35.dp.toPx())
        )
    }
}

@Composable
private fun AmbientCanvas() {
    // Статичный фон без анимации - плавный и стабильный
    Canvas(modifier = Modifier.fillMaxSize()) {
        drawIntoCanvas { canvas ->
            val ambientPaint = Paint().asFrameworkPaint().apply {
                isAntiAlias = true
                maskFilter = BlurMaskFilter(100.dp.toPx(), BlurMaskFilter.Blur.NORMAL)
            }
            
            // Верхнее пятно
            ambientPaint.color = BerryColors.mid.copy(alpha = 0.2f).toArgb()
            canvas.nativeCanvas.drawCircle(
                size.width * 0.85f,
                120.dp.toPx(),
                140.dp.toPx(),
                ambientPaint
            )
            
            // Нижнее пятно
            ambientPaint.color = BerryColors.glow.copy(alpha = 0.15f).toArgb()
            canvas.nativeCanvas.drawCircle(
                size.width * 0.15f,
                size.height - 180.dp.toPx(),
                160.dp.toPx(),
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
    onFocused: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.06f))
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(14.dp))
            .onFocusChanged { if (it.isFocused) onFocused() }
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(leadingIcon, null, tint = GlassTheme.textSecondary, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(14.dp))
            Box(Modifier.weight(1f)) {
                if (value.isEmpty()) {
                    Text(placeholder, color = GlassTheme.textTertiary, fontSize = 15.sp)
                }
                androidx.compose.foundation.text.BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    textStyle = androidx.compose.ui.text.TextStyle(color = GlassTheme.textPrimary, fontSize = 15.sp),
                    singleLine = true,
                    visualTransformation = visualTransformation,
                    keyboardOptions = keyboardOptions,
                    keyboardActions = keyboardActions,
                    cursorBrush = Brush.verticalGradient(listOf(BerryColors.glow, BerryColors.glow)),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (trailingIcon != null) {
                Spacer(Modifier.width(8.dp))
                IconButton(onClick = { onTrailingIconClick?.invoke() }, modifier = Modifier.size(44.dp)) {
                    Icon(trailingIcon, null, tint = GlassTheme.textSecondary, modifier = Modifier.size(22.dp))
                }
            }
        }
    }
}
