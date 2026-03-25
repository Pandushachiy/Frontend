package com.health.companion.presentation.screens.canvas

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.res.stringResource
import com.health.companion.R
import com.health.companion.data.canvas.RebuildGraphResponse
import com.health.companion.presentation.screens.chat.ChatViewModel

// Deep cosmic black — the canvas background
private val SPACE_BLACK = Color(0xFF030608)

@Composable
fun LivingMapScreen(
    canvasViewModel: CanvasViewModel,
    chatViewModel: ChatViewModel,
    onNodeAskAgent: (String) -> Unit = {},
    bottomPadding: Dp = 0.dp,
) {
    val renderInfos    by canvasViewModel.renderInfos.collectAsStateWithLifecycle()
    val edges          by canvasViewModel.edges.collectAsStateWithLifecycle()
    val clusters       by canvasViewModel.clusters.collectAsStateWithLifecycle()
    val activePaths    by canvasViewModel.activePaths.collectAsStateWithLifecycle()
    val focusedCluster by canvasViewModel.focusedCluster.collectAsStateWithLifecycle()
    val isLoading      by canvasViewModel.isLoading.collectAsStateWithLifecycle()
    val error          by canvasViewModel.error.collectAsStateWithLifecycle()
    val particleTime   by canvasViewModel.particleTime.collectAsStateWithLifecycle()

    val rawScale      by canvasViewModel.viewportScale.collectAsStateWithLifecycle()
    val rawOffset     by canvasViewModel.viewportOffset.collectAsStateWithLifecycle()
    val tapPulses     by canvasViewModel.tapPulses.collectAsStateWithLifecycle()
    val isRebuilding  by canvasViewModel.isRebuilding.collectAsStateWithLifecycle()
    val rebuildResult: RebuildGraphResponse? by canvasViewModel.rebuildResult.collectAsStateWithLifecycle()

    // Smooth camera animation — spring-based so programmatic viewport changes
    // (cluster focus, zoom-out) animate beautifully; manual gestures set target every frame
    val scale by animateFloatAsState(
        targetValue = rawScale,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = 120f),
        label = "camScale",
    )
    val offsetX by animateFloatAsState(
        targetValue = rawOffset.x,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = 120f),
        label = "camOX",
    )
    val offsetY by animateFloatAsState(
        targetValue = rawOffset.y,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = 120f),
        label = "camOY",
    )
    val offset = Offset(offsetX, offsetY)

    var draggingId by remember { mutableStateOf<String?>(null) }

    // Slow nebula drift — integer sin/cos multipliers guarantee f(0) = f(2π·N), no seam
    val nebulaInf = rememberInfiniteTransition("nebula")
    val nebulaT by nebulaInf.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(55000, easing = LinearEasing)),
        label = "nebulaT",
    )

    val config  = LocalConfiguration.current
    val density = LocalDensity.current
    val screenW = with(density) { config.screenWidthDp.dp.toPx() }
    val screenH = with(density) { config.screenHeightDp.dp.toPx() }

    val labelPaint = remember {
        android.graphics.Paint().apply {
            isAntiAlias = true
            textAlign = android.graphics.Paint.Align.CENTER
            typeface  = android.graphics.Typeface.DEFAULT
        }
    }

    // World-space dust particles (stars are now procedural/infinite)
    val stars = remember { emptyList<Star>() }
    val dust  = remember { generateDustField(70, 3000f, 3000f) }

    // Tilt-responsive glow position from accelerometer
    val tilt = rememberDeviceTilt()

    // Parallax: important nodes shift slightly more when device is tilted (3-D depth illusion)
    val parallaxNodes = remember(renderInfos, tilt) {
        if (tilt == Offset.Zero) renderInfos
        else renderInfos.map { n ->
            n.copy(
                x = n.x + n.importance * tilt.x * 14f,
                y = n.y - n.importance * tilt.y * 14f,
            )
        }
    }

    LaunchedEffect(Unit) {
        chatViewModel.canvasEvents.collect { (action, payload) ->
            canvasViewModel.handleCanvasUpdate(action, payload)
        }
    }

    LaunchedEffect(Unit) {
        canvasViewModel.setScreenSize(screenW, screenH)
        if (renderInfos.isEmpty()) canvasViewModel.loadGraph()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SPACE_BLACK)
    ) {
        // ── Background layer: nebulae + sun glow ─────────────────────────────
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            val tw = nebulaT * 2f * Math.PI.toFloat()

            // — Nebula 1: cool blue, top-right quadrant —
            val n1x = size.width  * (0.68f + sin(tw)          * 0.11f)
            val n1y = size.height * (0.22f + cos(tw + 1.5f)   * 0.09f)
            val n1r = size.width * (0.55f + sin(tw * 3f + 0.8f) * 0.04f)
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(Color(0xFF1A4080).copy(alpha = 0.13f), Color(0xFF2255BB).copy(alpha = 0.05f), Color.Transparent),
                    center = Offset(n1x, n1y), radius = n1r,
                ),
                radius = n1r, center = Offset(n1x, n1y),
            )

            // — Nebula 2: purple-violet, bottom-left —
            val n2x = size.width  * (0.22f + cos(tw + 1.1f)          * 0.10f)
            val n2y = size.height * (0.72f + sin(tw * 2f + 0.4f)     * 0.08f)
            val n2r = size.width * (0.50f + sin(tw * 2f + 2.0f) * 0.035f)
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(Color(0xFF4A1A80).copy(alpha = 0.11f), Color(0xFF7730BB).copy(alpha = 0.04f), Color.Transparent),
                    center = Offset(n2x, n2y), radius = n2r,
                ),
                radius = n2r, center = Offset(n2x, n2y),
            )

            // — Nebula 3: teal-cyan, center-right —
            val n3x = size.width  * (0.55f + sin(tw * 2f + 2.3f)    * 0.13f)
            val n3y = size.height * (0.50f + cos(tw + 1.7f)          * 0.07f)
            val n3r = size.width * (0.40f + sin(tw + 4.0f) * 0.03f)
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(Color(0xFF0A3040).copy(alpha = 0.10f), Color(0xFF106080).copy(alpha = 0.04f), Color.Transparent),
                    center = Offset(n3x, n3y), radius = n3r,
                ),
                radius = n3r, center = Offset(n3x, n3y),
            )

            // — Sun glow: center ALWAYS outside screen, follows device tilt —
            val cx = size.width / 2f
            val cy = size.height / 2f
            // Смещение по умолчанию -0.70 / -0.65 (верхний левый угол) плюс вклад наклона.
            // Нет порога — переход всегда плавный: при нейтральном положении тильт ≈ 0
            // и формула даёт точно то же значение что раньше давал hardcoded дефолт.
            val rawDx = tilt.x - 0.70f
            val rawDy = -tilt.y - 0.65f
            val norm  = sqrt(rawDx * rawDx + rawDy * rawDy).coerceAtLeast(0.01f)
            val ndx = rawDx / norm; val ndy = rawDy / norm
            // Ray from screen centre → edge of screen in tilt direction
            val tEdgeX = if (abs(ndx) > 0.001f) cx / abs(ndx) else Float.MAX_VALUE
            val tEdgeY = if (abs(ndy) > 0.001f) cy / abs(ndy) else Float.MAX_VALUE
            val edgeDist = min(tEdgeX, tEdgeY)
            // Place sun centre 1.65× past the edge — always off-screen
            val sunX = cx + ndx * edgeDist * 1.65f
            val sunY = cy + ndy * edgeDist * 1.65f
            val glowR = maxOf(size.width, size.height) * 1.85f

            drawRect(
                brush = Brush.radialGradient(
                    colorStops = arrayOf(
                        0.00f to Color(0xFFFFFFCC).copy(alpha = 0.90f),
                        0.04f to Color(0xFFFFEA80).copy(alpha = 0.72f),
                        0.10f to Color(0xFFFFCC40).copy(alpha = 0.48f),
                        0.18f to Color(0xFFFF9030).copy(alpha = 0.30f),
                        0.28f to Color(0xFFDD6020).copy(alpha = 0.16f),
                        0.42f to Color(0xFF4466BB).copy(alpha = 0.09f),
                        0.58f to Color(0xFF1A2840).copy(alpha = 0.04f),
                        0.75f to Color(0xFF0A1020).copy(alpha = 0.02f),
                        1.00f to Color.Transparent,
                    ),
                    center = Offset(sunX, sunY),
                    radius = glowR,
                ),
            )
        }

        when {
            isLoading && renderInfos.isEmpty() -> LoadingState()
            error != null && renderInfos.isEmpty() -> ErrorState(error ?: "Ошибка загрузки") { canvasViewModel.loadGraph() }

            else -> {
                // ── Graph canvas ──────────────────────────────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        // 1 finger = tap / node-drag only; 2 fingers = pan + zoom
                        .pointerInput(renderInfos) {
                            awaitPointerEventScope {
                                var pressNodeId: String? = null
                                var pressScreen = Offset.Zero
                                var prevT1 = Offset.Zero
                                var prevT2 = Offset.Zero
                                var inMultiTouch = false

                                while (true) {
                                    val ev     = awaitPointerEvent()
                                    val active = ev.changes.filter { it.pressed }

                                    when (ev.type) {
                                        PointerEventType.Press -> {
                                            if (active.size == 1 && !inMultiTouch) {
                                                pressScreen = active[0].position
                                                val s  = canvasViewModel.viewportScale.value
                                                val o  = canvasViewModel.viewportOffset.value
                                                val wp = screenToWorld(active[0].position, o, s)
                                                pressNodeId = hitTestNode(renderInfos, wp)
                                                if (pressNodeId != null) { draggingId = pressNodeId; active[0].consume() }
                                            } else if (active.size >= 2) {
                                                inMultiTouch = true
                                                draggingId = null; pressNodeId = null
                                                prevT1 = active[0].position; prevT2 = active[1].position
                                                active.forEach { it.consume() }
                                            }
                                        }
                                        PointerEventType.Move -> {
                                            if (active.size >= 2) {
                                                if (!inMultiTouch) {
                                                    inMultiTouch = true; draggingId = null
                                                    prevT1 = active[0].position; prevT2 = active[1].position
                                                }
                                                val t1 = active[0].position; val t2 = active[1].position
                                                val prevDist  = (prevT2 - prevT1).getDistance().coerceAtLeast(1f)
                                                val currDist  = (t2 - t1).getDistance().coerceAtLeast(1f)
                                                val zoom      = currDist / prevDist
                                                val currC     = (t1 + t2) / 2f
                                                val pan       = currC - (prevT1 + prevT2) / 2f
                                                val oldS      = canvasViewModel.viewportScale.value
                                                val oldO      = canvasViewModel.viewportOffset.value
                                                val newS      = (oldS * zoom).coerceIn(0.12f, 3f)
                                                val newO      = (oldO - currC) * (newS / oldS) + currC + pan
                                                canvasViewModel.onUserViewportChange(newS, newO)
                                                prevT1 = t1; prevT2 = t2
                                                active.forEach { it.consume() }
                                            } else if (active.size == 1 && draggingId != null && !inMultiTouch) {
                                                val s  = canvasViewModel.viewportScale.value
                                                val o  = canvasViewModel.viewportOffset.value
                                                val wp = screenToWorld(active[0].position, o, s)
                                                canvasViewModel.onNodeDragEnd(draggingId!!, wp.x, wp.y)
                                                active[0].consume()
                                            }
                                        }
                                        PointerEventType.Release -> {
                                            val stillPressed = ev.changes.count { it.pressed }
                                            if (stillPressed == 0) {
                                                if (!inMultiTouch) {
                                                    val ch = ev.changes.firstOrNull()
                                                    draggingId = null
                                                    if (ch != null && (ch.position - pressScreen).getDistance() < 14f) {
                                                        val s  = canvasViewModel.viewportScale.value
                                                        val o  = canvasViewModel.viewportOffset.value
                                                        val wp = screenToWorld(ch.position, o, s)
                                                        val hit = pressNodeId ?: hitTestNode(renderInfos, wp)
                                                        if (hit != null) canvasViewModel.onNodeClick(hit)
                                                        else canvasViewModel.dismissSelectedNode()
                                                    }
                                                } else {
                                                    draggingId = null
                                                }
                                                pressNodeId = null; inMultiTouch = false
                                            } else if (stillPressed == 1) {
                                                val rem = ev.changes.filter { it.pressed }
                                                if (rem.isNotEmpty()) prevT1 = rem[0].position
                                                inMultiTouch = false
                                            }
                                        }
                                        else -> {}
                                    }
                                }
                            }
                        }
                ) {
                    androidx.compose.foundation.Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(
                                scaleX       = scale, scaleY = scale,
                                translationX = offset.x, translationY = offset.y,
                                transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 0f),
                            )
                    ) {
                        drawGraphCanvas(
                            nodes        = parallaxNodes,
                            edges        = edges,
                            activePaths  = activePaths,
                            particleTime = particleTime,
                            scale        = scale,
                            offset       = offset,
                            labelPaint   = labelPaint,
                            tapPulses    = tapPulses,
                            stars        = stars,
                            dust         = dust,
                        )
                    }
                }

                // ── HUD ───────────────────────────────────────────────────────

                // Node count — tiny, top-right
                Text(
                    text     = "${renderInfos.size} nodes",
                    color    = Color.White.copy(alpha = 0.15f),
                    fontSize = 10.sp,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .statusBarsPadding()
                        .padding(end = 14.dp, top = 8.dp),
                )

                // Bottom area: filter bar (bottom-aligned) + rebuild button (slightly higher)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(bottom = bottomPadding),
                ) {
                    // Фильтр-бар прилипает к низу блока
                    ClusterFilterBar(
                        clusters       = clusters,
                        activeCluster  = focusedCluster,
                        onClusterClick = { id ->
                            canvasViewModel.handleCanvasUpdate("focus_cluster", """{"cluster":"$id"}""")
                        },
                        onResetClick   = {
                            canvasViewModel.handleCanvasUpdate("zoom_out", "{}")
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomStart),
                    )
                    // Кнопка чуть выше строки фильтров
                    RebuildGraphButton(
                        isRebuilding = isRebuilding,
                        rebuildResult = rebuildResult,
                        onRebuild = { canvasViewModel.rebuildGraph() },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 10.dp, bottom = 52.dp),
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Rebuild Graph Button — космическая кнопка обновления канваса
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun RebuildGraphButton(
    isRebuilding: Boolean,
    rebuildResult: com.health.companion.data.canvas.RebuildGraphResponse?,
    onRebuild: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val inf = rememberInfiniteTransition(label = "rebuild")

    // Вращение иконки во время пересчёта
    val iconRotation by inf.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(1800, easing = LinearEasing), RepeatMode.Restart),
        label = "rot"
    )
    // Пульсация свечения кнопки в ожидании
    val glowPulse by inf.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2200, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "glow"
    )

    // Черная дыра — цвета
    val diskColor    = Color(0xFFFF7030)
    val diskPeak     = Color(0xFFFFCC60)
    val outerGlow    = Color(0xFFDD5520)

    Column(horizontalAlignment = Alignment.End, modifier = modifier) {
        // Canvas-кнопка — настоящая чёрная дыра с аккреционным диском
        Box(
            modifier = Modifier
                .size(46.dp)
                .shadow(
                    elevation = if (isRebuilding) 16.dp else 10.dp,
                    shape = CircleShape,
                    ambientColor = outerGlow.copy(alpha = if (isRebuilding) 0.7f else 0.35f),
                    spotColor   = outerGlow.copy(alpha = if (isRebuilding) 0.9f else 0.55f),
                )
                .clip(CircleShape)
                .graphicsLayer { alpha = if (isRebuilding) 1f else 0.92f + glowPulse * 0.08f }
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    enabled = !isRebuilding,
                ) { onRebuild() },
            contentAlignment = Alignment.Center
        ) {
            // Canvas — рисуем чёрную дыру
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cx = size.width / 2f
                val cy = size.height / 2f
                val outerR = size.minDimension / 2f

                // Внешнее свечение (аккреционный диск - широкое размытое кольцо)
                drawCircle(
                    brush = Brush.radialGradient(
                        colorStops = arrayOf(
                            0.00f to Color.Black,
                            0.30f to Color.Black,
                            0.40f to Color(0xFF3A1000),
                            0.50f to diskColor.copy(alpha = 0.8f),
                            0.58f to diskPeak.copy(alpha = 0.9f),
                            0.66f to diskColor.copy(alpha = 0.7f),
                            0.76f to outerGlow.copy(alpha = 0.4f),
                            0.88f to outerGlow.copy(alpha = 0.15f),
                            1.00f to Color.Transparent,
                        ),
                        center = Offset(cx, cy), radius = outerR,
                    ),
                    radius = outerR, center = Offset(cx, cy),
                )

                // Фотонное кольцо — тонкое яркое
                drawCircle(
                    color = diskPeak.copy(alpha = 0.55f),
                    radius = outerR * 0.40f,
                    center = Offset(cx, cy),
                    style = Stroke(width = outerR * 0.05f),
                )

                // Горизонт событий — абсолютно чёрный круг
                drawCircle(Color.Black, radius = outerR * 0.33f, center = Offset(cx, cy))
            }

            if (isRebuilding) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = diskPeak,
                    strokeWidth = 2.dp,
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────

private fun hitTestNode(nodes: List<NodeRenderInfo>, worldPos: Offset): String? {
    for (i in nodes.indices.reversed()) {
        val n   = nodes[i]
        val hit = (n.radius * 1.6f).coerceAtLeast(18f)
        val dx  = n.x - worldPos.x; val dy = n.y - worldPos.y
        if (dx * dx + dy * dy <= hit * hit) return n.id
    }
    return null
}

private fun screenToWorld(screenPos: Offset, offset: Offset, scale: Float) =
    Offset((screenPos.x - offset.x) / scale, (screenPos.y - offset.y) / scale)

// ─────────────────────────────────────────────────────────────────────────────

// ─────────────────────────────────────────────────────────────────────────────
// Sensor: smooth device tilt → Offset(leftRight, forwardBack) both in [-1, 1]
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun rememberDeviceTilt(): Offset {
    val context = LocalContext.current
    val tiltX = remember { mutableStateOf(0f) }
    val tiltY = remember { mutableStateOf(0f) }

    DisposableEffect(Unit) {
        val sm = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val accel = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        var sX = 0f; var sY = 0f
        var frameCount = 0

        var lastTiltUpdateMs = 0L
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                if (event.values.size < 3) return
                // Throttle tilt state updates to ~20fps max to avoid triggering
                // excessive recompositions at 60Hz (SENSOR_DELAY_UI fires at ~15Hz,
                // but we cap even that to reduce Compose invalidation pressure)
                val nowMs = android.os.SystemClock.elapsedRealtime()
                if (nowMs - lastTiltUpdateMs < 50L) return
                lastTiltUpdateMs = nowMs

                val rawX = event.values[0] / 9.8f
                val rawY = event.values[2] / 9.8f
                sX = sX * 0.96f + rawX * 0.04f
                sY = sY * 0.96f + rawY * 0.04f
                frameCount++
                val blend = if (frameCount < 30) 0.92f else 0.96f
                val smoothX = tiltX.value * blend + sX.coerceIn(-1f, 1f) * (1f - blend)
                val smoothY = tiltY.value * blend + sY.coerceIn(-1f, 1f) * (1f - blend)
                tiltX.value = smoothX.coerceIn(-1f, 1f)
                tiltY.value = smoothY.coerceIn(-1f, 1f)
            }
            override fun onAccuracyChanged(s: Sensor?, a: Int) {}
        }
        // SENSOR_DELAY_UI (~60ms between events) is sufficient for a parallax effect
        // and avoids the ~50 recompositions/sec caused by SENSOR_DELAY_GAME at 60Hz
        sm.registerListener(listener, accel, SensorManager.SENSOR_DELAY_UI)
        onDispose { sm.unregisterListener(listener) }
    }
    return Offset(tiltX.value, tiltY.value)
}

@Composable
private fun LoadingState() {
    val inf = rememberInfiniteTransition(label = "canvas_load")

    // Core orb: slow breathe
    val coreScale by inf.animateFloat(0.85f, 1.15f,
        infiniteRepeatable(tween(2200, easing = EaseInOutSine), RepeatMode.Reverse), "core")

    // Outer ring rotation
    val ringRot by inf.animateFloat(0f, 360f,
        infiniteRepeatable(tween(12000, easing = LinearEasing), RepeatMode.Restart), "ring")

    // Inner ring reverse rotation
    val ringRot2 by inf.animateFloat(360f, 0f,
        infiniteRepeatable(tween(8000, easing = LinearEasing), RepeatMode.Restart), "ring2")

    // Overall glow pulse
    val glowA by inf.animateFloat(0.18f, 0.45f,
        infiniteRepeatable(tween(1800, easing = EaseInOutSine), RepeatMode.Reverse), "glow")

    // Floating node positions (6 orbital nodes)
    val orbits = remember { listOf(90f, 150f, 210f, 270f, 330f, 30f) }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(bottom = 40.dp),
        ) {

        // Orbital graph
        androidx.compose.foundation.Canvas(modifier = Modifier.size(240.dp)) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val outerR = size.width * 0.44f
            val innerR = size.width * 0.28f
            val coreR  = size.width * 0.10f * coreScale

            // Outer glow halo
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(Color(0xFF4A9EFF).copy(alpha = glowA * 0.6f), Color.Transparent),
                    center = Offset(cx, cy), radius = outerR * 1.4f,
                ),
                radius = outerR * 1.4f, center = Offset(cx, cy),
            )

            // Outer ring (dashed orbit path)
            drawCircle(
                color = Color(0xFF2A5080).copy(alpha = 0.35f),
                radius = outerR, center = Offset(cx, cy),
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = 1f,
                    pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(6f, 10f)),
                )
            )
            // Inner ring
            drawCircle(
                color = Color(0xFF1A3A5A).copy(alpha = 0.45f),
                radius = innerR, center = Offset(cx, cy),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 0.8f)
            )

            // 6 orbital nodes — slowly rotating
            orbits.forEachIndexed { i, baseAngle ->
                val angle = Math.toRadians((baseAngle + ringRot).toDouble())
                val nx = cx + outerR * cos(angle).toFloat()
                val ny = cy + outerR * sin(angle).toFloat()
                val nodeColor = listOf(
                    Color(0xFF4A9EFF), Color(0xFF9B6EF3), Color(0xFF00C897),
                    Color(0xFFFF7A50), Color(0xFFFFCC40), Color(0xFF4A9EFF),
                )[i]
                // Edge to centre
                drawLine(
                    color = nodeColor.copy(alpha = 0.18f),
                    start = Offset(cx, cy), end = Offset(nx, ny), strokeWidth = 0.8f
                )
                // Node glow
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(nodeColor.copy(alpha = glowA * 0.7f), Color.Transparent),
                        center = Offset(nx, ny), radius = 18f,
                    ),
                    radius = 18f, center = Offset(nx, ny),
                )
                // Node core
                drawCircle(color = nodeColor.copy(alpha = 0.90f), radius = 5f, center = Offset(nx, ny))
            }

            // 3 inner ring nodes (counter-rotate)
            listOf(0f, 120f, 240f).forEachIndexed { i, baseAngle ->
                val angle = Math.toRadians((baseAngle + ringRot2).toDouble())
                val nx = cx + innerR * cos(angle).toFloat()
                val ny = cy + innerR * sin(angle).toFloat()
                val c = listOf(Color(0xFF6DFFE0), Color(0xFFB06EFF), Color(0xFFFFAA60))[i]
                drawLine(color = c.copy(alpha = 0.14f), start = Offset(cx, cy), end = Offset(nx, ny), strokeWidth = 0.6f)
                drawCircle(color = c.copy(alpha = 0.85f), radius = 3.5f, center = Offset(nx, ny))
            }

            // Central core orb
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(Color(0xFFFFFFFF).copy(alpha = 0.90f), Color(0xFF4A9EFF).copy(alpha = 0.60f), Color.Transparent),
                    center = Offset(cx, cy), radius = coreR * 2f,
                ),
                radius = coreR * 2f, center = Offset(cx, cy),
            )
            drawCircle(color = Color(0xFFCCE8FF), radius = coreR, center = Offset(cx, cy))
        }

        Spacer(Modifier.height(20.dp))
        Text(
            stringResource(R.string.building_knowledge_map),
            color = Color(0xFF4A6A8A),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
        )

        } // end Column
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp),
        ) {
            Text("!", fontSize = 32.sp, color = Color(0xFFFF6B6B), fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            Text("Could not load graph", color = Color.White.copy(alpha = 0.85f), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            Text(message, color = Color.White.copy(alpha = 0.4f), fontSize = 11.sp)
            Spacer(Modifier.height(18.dp))
            Button(onClick = onRetry, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A9EFF))) {
                Text("Retry")
            }
        }
    }
}
