package com.health.companion.presentation.components

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// ============================================================================
// LEGACY COMPATIBILITY LAYER
// Для обратной совместимости со старым кодом
// Новый код должен использовать GlassDesignSystem.kt напрямую
// ============================================================================

/**
 * @deprecated Используй GlassColors, GlassGradients из GlassDesignSystem.kt
 */
object GlassTheme {
    // Background gradients — теперь используют GlassGradients
    val backgroundGradient = GlassGradients.backgroundVertical
    val backgroundGradientAlt = GlassGradients.background
    val warmBackgroundGradient = GlassGradients.warm

    // Glass card colors — более плотный фон (меньше прозрачности)
    val glassWhite = Color(0xFF1A1F2E).copy(alpha = 0.85f) // Тёмный фон 85%
    val glassWhiteHover = Color(0xFF252B3D).copy(alpha = 0.90f)
    val glassBorder = Color.White.copy(alpha = 0.15f)
    val glassBorderLight = Color.White.copy(alpha = 0.08f)

    // Accent colors
    val accentPrimary = GlassColors.mint
    val accentSecondary = GlassColors.accent
    val accentTertiary = Color(0xFFf093fb)
    val accentWarm = GlassColors.orange
    val accentCool = Color(0xFF4facfe)

    // Text colors
    val textPrimary = GlassColors.textPrimary
    val textSecondary = GlassColors.textSecondary
    val textTertiary = GlassColors.textTertiary
    val textMuted = GlassColors.textMuted

    // Status colors
    val statusGood = GlassColors.success
    val statusWarning = GlassColors.warning
    val statusError = GlassColors.error
    val statusInfo = GlassColors.info

    // Gradients for accents
    val accentGradient = GlassGradients.accent
    val warmGradient = GlassGradients.warm
    val coolGradient = GlassGradients.cool
    val purpleGradient = GlassGradients.purple
}

/**
 * @deprecated Используй GlassCard из GlassDesignSystem.kt
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 20.dp,
    blurRadius: Dp = 0.dp,
    backgroundColor: Color = GlassColors.surface.copy(alpha = 0.5f),
    borderColor: Color = GlassColors.whiteOverlay10,
    borderWidth: Dp = 1.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    
    Box(
        modifier = modifier
            .clip(shape)
            .background(backgroundColor, shape)
            .border(borderWidth, borderColor, shape)
    ) {
        content()
    }
}

/**
 * @deprecated Используй GlassCardGradient из GlassDesignSystem.kt
 */
@Composable
fun GlassCardGradient(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 20.dp,
    gradientColors: List<Color> = listOf(
        Color.White.copy(alpha = 0.1f),
        Color.White.copy(alpha = 0.05f)
    ),
    borderColor: Color = GlassColors.whiteOverlay10,
    content: @Composable BoxScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    
    Box(
        modifier = modifier
            .clip(shape)
            .background(Brush.linearGradient(gradientColors), shape)
            .border(1.dp, borderColor, shape)
    ) {
        content()
    }
}

/**
 * @deprecated Используй GlassCard из GlassDesignSystem.kt с accent цветом
 */
@Composable
fun AccentCard(
    modifier: Modifier = Modifier,
    accentColor: Color = GlassColors.mint,
    cornerRadius: Dp = 20.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    
    Box(
        modifier = modifier
            .clip(shape)
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        accentColor.copy(alpha = 0.2f),
                        accentColor.copy(alpha = 0.05f)
                    )
                ),
                shape
            )
            .border(1.dp, accentColor.copy(alpha = 0.3f), shape)
    ) {
        content()
    }
}

// GlassBackground перенесён в GlassDesignSystem.kt

/**
 * @deprecated
 */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    alpha: Float = 0.5f,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(GlassColors.background.copy(alpha = alpha))
    ) {
        content()
    }
}

// ============================================================================
// 🍎 APPLE-STYLE GLASS ЭФФЕКТ
// Chromatic aberration + Edge glow + Blur (как в macOS/iOS)
// Android 13+ — настоящая аберрация через RuntimeShader
// Android 12 — blur + RGB рамки
// Android < 12 — RGB рамки + градиенты
// ============================================================================

// AGSL Shader для хроматической аберрации (Android 13+)
private const val CHROMATIC_ABERRATION_SHADER = """
    uniform shader content;
    uniform float2 size;
    uniform float aberrationStrength;
    
    half4 main(float2 coord) {
        float2 uv = coord / size;
        float2 center = float2(0.5, 0.5);
        float2 direction = uv - center;
        float distance = length(direction);
        
        // Увеличиваем эффект на краях
        float edgeFactor = smoothstep(0.3, 0.7, distance);
        float offset = aberrationStrength * edgeFactor;
        
        // Разделяем RGB каналы
        float2 offsetVec = direction * offset;
        half4 colorR = content.eval(coord + offsetVec * float2(1.5, 1.5));
        half4 colorG = content.eval(coord);
        half4 colorB = content.eval(coord - offsetVec * float2(1.5, 1.5));
        
        return half4(colorR.r, colorG.g, colorB.b, colorG.a);
    }
"""

/**
 * 🍎 Apple Glass Card — настоящий эффект как в macOS/iOS
 * 
 * Из apple_glass.md:
 * - Chromatic aberration (RGB разделение на краях)
 * - Edge glow (светящаяся граница сверху)
 * - Inner shadow (тень снизу)
 * - Градиентная прозрачность
 */
@Composable
fun AppleGlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 24.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val cornerRadiusPx = cornerRadius.value
    
    Box(
        modifier = modifier.clip(RoundedCornerShape(cornerRadius))
    ) {
        // Слой 1: Цветные границы (chromatic aberration)
        Box(
            modifier = Modifier
                .matchParentSize()
                .drawWithContent {
                    val radius = cornerRadiusPx * density
                    
                    // Красный канал (сдвиг влево-вверх)
                    drawRoundRect(
                        color = Color(0x25FF0000),
                        topLeft = Offset(-1f, -1f),
                        size = Size(size.width + 2f, size.height + 2f),
                        cornerRadius = CornerRadius(radius),
                        style = Stroke(width = 2f)
                    )
                    
                    // Зелёный канал (без сдвига)
                    drawRoundRect(
                        color = Color(0x2500FF00),
                        size = size,
                        cornerRadius = CornerRadius(radius),
                        style = Stroke(width = 2f)
                    )
                    
                    // Синий канал (сдвиг вправо-вниз)
                    drawRoundRect(
                        color = Color(0x250000FF),
                        topLeft = Offset(1f, 1f),
                        size = Size(size.width - 2f, size.height - 2f),
                        cornerRadius = CornerRadius(radius - 1f),
                        style = Stroke(width = 2f)
                    )
                }
        )
        
        // Слой 2: Основной glass фон (градиент!)
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.20f),
                            Color.White.copy(alpha = 0.10f)
                        )
                    ),
                    shape = RoundedCornerShape(cornerRadius)
                )
        )
        
        // Слой 3: Edge highlights
        Box(
            modifier = Modifier
                .matchParentSize()
                .drawWithContent {
                    val radius = CornerRadius(cornerRadiusPx * density)
                    
                    // Верхний светящийся край
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.6f),
                                Color.Transparent
                            ),
                            startY = 0f,
                            endY = size.height * 0.15f
                        ),
                        size = size,
                        cornerRadius = radius,
                        style = Stroke(width = 1.5f)
                    )
                    
                    // Нижняя внутренняя тень
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.2f)
                            ),
                            startY = size.height * 0.85f,
                            endY = size.height
                        ),
                        size = size,
                        cornerRadius = radius,
                        style = Stroke(width = 1f)
                    )
                }
        )
        
        // Слой 4: Контент
        Box(content = content)
    }
}

/**
 * 🧊 Frosted Glass — стекло это ПОДЛОЖКА, контент чёткий!
 */
@Composable
fun FrostedGlassCard(
    modifier: Modifier = Modifier,
    tintColor: Color = Color.White,
    tintAlpha: Float = 0.12f,
    borderColor: Color = Color.White.copy(alpha = 0.2f),
    borderWidth: Dp = 1.dp,
    cornerRadius: Dp = 16.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    val cornerRadiusPx = cornerRadius.value
    
    Box(modifier = modifier.clip(shape)) {
        // Слой 1: Стеклянный фон
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(tintColor.copy(alpha = tintAlpha), shape)
                .border(borderWidth, borderColor, shape)
                .drawWithContent {
                    drawContent()
                    
                    val radius = CornerRadius(cornerRadiusPx * density)
                    
                    // Верхний highlight
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.3f),
                                Color.Transparent
                            ),
                            startY = 0f,
                            endY = size.height * 0.15f
                        ),
                        size = size,
                        cornerRadius = radius,
                        style = Stroke(width = 1f)
                    )
                }
        )
        
        // Слой 2: Контент — чёткий!
        Box(content = content)
    }
}

/**
 * 🌈 Accent Glass Card — с цветным акцентом
 */
@Composable
fun AccentGlassCard(
    modifier: Modifier = Modifier,
    accentColor: Color = Color(0xFF8B5CF6),
    blurRadius: Float = 20f,
    cornerRadius: Dp = 16.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    
    Box(
        modifier = modifier
            .clip(shape)
            .then(
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    Modifier.graphicsLayer {
                        renderEffect = RenderEffect
                            .createBlurEffect(blurRadius, blurRadius, Shader.TileMode.CLAMP)
                            .asComposeRenderEffect()
                    }
                } else Modifier
            )
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        accentColor.copy(alpha = 0.15f),
                        accentColor.copy(alpha = 0.05f)
                    )
                ),
                shape
            )
            .border(1.dp, accentColor.copy(alpha = 0.3f), shape)
    ) {
        content()
    }
}
