package com.health.companion.presentation.screens.canvas

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import com.health.companion.data.canvas.CanvasEdge
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.atan2

// ──────────────────────────────────────────────────────────────────────────────
// Category → colour
// ──────────────────────────────────────────────────────────────────────────────

val CLUSTER_COLORS = mapOf(
    "documents"  to Color(0xFF4A9EFF),
    "document"   to Color(0xFF4A9EFF),
    "документы"  to Color(0xFF4A9EFF),
    "health"     to Color(0xFF50D8A4),
    "здоровье"   to Color(0xFF50D8A4),
    "finance"    to Color(0xFFFFAA2C),
    "финансы"    to Color(0xFFFFAA2C),
    "contacts"   to Color(0xFFB57CF6),
    "контакты"   to Color(0xFFB57CF6),
    "skills"     to Color(0xFF40D9F1),
    "навыки"     to Color(0xFF40D9F1),
    "work"       to Color(0xFFFF7A6E),
    "работа"     to Color(0xFFFF7A6E),
    "personal"   to Color(0xFFFF9ECA),
    "личное"     to Color(0xFFFF9ECA),
    "lifestyle"  to Color(0xFF75D272),
    "interests"  to Color(0xFFFFD166),
    "интересы"   to Color(0xFFFFD166),
    "goals"      to Color(0xFFFF6B9D),
    "цели"       to Color(0xFFFF6B9D),
    "general"    to Color(0xFF8899BB),
    "facts"      to Color(0xFF8899BB),
)

fun clusterColor(cluster: String): Color =
    CLUSTER_COLORS[cluster.lowercase().trim()] ?: Color(0xFF8899BB)

// ──────────────────────────────────────────────────────────────────────────────
// Star field — pre-generated random stars for the background
// ──────────────────────────────────────────────────────────────────────────────

data class Star(val x: Float, val y: Float, val size: Float, val baseAlpha: Float, val speed: Float, val phase: Float)

// kept for API compat — no longer the primary star source
fun generateStarField(count: Int, worldW: Float, worldH: Float): List<Star> = emptyList()

// ──────────────────────────────────────────────────────────────────────────────
// Ambient dust particles — floating slowly in space
// ──────────────────────────────────────────────────────────────────────────────

data class DustMote(val baseX: Float, val baseY: Float, val size: Float, val alpha: Float,
                    val driftAmpX: Float, val driftAmpY: Float, val speedX: Float, val speedY: Float, val phase: Float)

fun generateDustField(count: Int, worldW: Float, worldH: Float): List<DustMote> {
    val rng = java.util.Random(137)
    val offX = 500f - worldW / 2f
    val offY = 500f - worldH / 2f
    return List(count) {
        DustMote(
            baseX = rng.nextFloat() * worldW + offX,
            baseY = rng.nextFloat() * worldH + offY,

            size = 0.5f + rng.nextFloat() * 1.2f,
            alpha = 0.06f + rng.nextFloat() * 0.12f,
            driftAmpX = 8f + rng.nextFloat() * 25f,
            driftAmpY = 8f + rng.nextFloat() * 25f,
            speedX = 0.3f + rng.nextFloat() * 0.7f,
            speedY = 0.3f + rng.nextFloat() * 0.7f,
            phase = rng.nextFloat() * 6.2832f,
        )
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Main entry — all layers in order
// ──────────────────────────────────────────────────────────────────────────────

fun DrawScope.drawGraphCanvas(
    nodes: List<NodeRenderInfo>,
    edges: List<CanvasEdge>,
    activePaths: List<PathAnimation>,
    particleTime: Float,
    scale: Float,
    offset: Offset,
    labelPaint: android.graphics.Paint,
    tapPulses: List<TapPulse> = emptyList(),
    stars: List<Star> = emptyList(),
    dust: List<DustMote> = emptyList(),
) {
    if (nodes.isEmpty() && stars.isEmpty()) return

    val margin  = 80f
    val vpLeft   = (-offset.x) / scale - margin
    val vpTop    = (-offset.y) / scale - margin
    val vpRight  = (size.width  - offset.x) / scale + margin
    val vpBottom = (size.height - offset.y) / scale + margin

    // Layer 0 — infinite procedural star field
    drawProceduralStars(particleTime, scale, vpLeft, vpTop, vpRight, vpBottom)

    // Layer 0b — ambient dust
    if (dust.isNotEmpty() && scale > 0.15f) {
        drawDustLayer(dust, particleTime, scale, vpLeft, vpTop, vpRight, vpBottom)
    }

    if (nodes.isEmpty()) return

    val nodeMap = HashMap<String, NodeRenderInfo>(nodes.size * 2)
    nodes.forEach { nodeMap[it.id] = it }

    val edgeStroke = (1.5f / scale).coerceIn(0.4f, 5f)
    val iconStroke = (1.8f / scale).coerceIn(0.5f, 6f)

    // Layer 1 — constellation lines (thin, inside clusters)
    if (scale > 0.16f) {
        drawConstellations(nodes, scale, vpLeft, vpTop, vpRight, vpBottom)
    }

    // Layer 2 — cluster soft halos
    drawClusterBackgrounds(nodes, scale)

    // Layer 3 — gradient Bezier edges
    drawEdgesLayer(nodes, edges, nodeMap, scale, particleTime, edgeStroke, vpLeft, vpTop, vpRight, vpBottom)

    // Layer 4 — SSE path animations
    drawActivePathsLayer(nodes, activePaths, nodeMap)

    // Layer 5 — particle trails on edges
    if (scale > 0.28f) {
        drawEdgeParticleTrails(nodes, edges, nodeMap, particleTime, scale)
    }

    // Layer 6 — glow halos
    drawGlowsLayer(nodes, scale, vpLeft, vpTop, vpRight, vpBottom)

    // Layer 6b — connection glow
    drawConnectionGlowLayer(edges, nodeMap, scale, particleTime, vpLeft, vpTop, vpRight, vpBottom)

    // Layer 6c — hot-node ripple rings
    if (scale > 0.22f) {
        drawHotNodeRipples(nodes, particleTime, scale, vpLeft, vpTop, vpRight, vpBottom)
    }

    // Layer 6d — tap pulse rings
    if (tapPulses.isNotEmpty()) drawTapPulsesLayer(tapPulses, scale)

    // Layer 7 — node circles
    drawNodesLayer(nodes, scale, vpLeft, vpTop, vpRight, vpBottom)

    // Layer 8 — category icons
    if (scale > 0.55f) {
        drawIconsLayer(nodes, scale, iconStroke, vpLeft, vpTop, vpRight, vpBottom)
    }

    // Layer 9 — labels with adaptive opacity
    if (scale > 0.20f) {
        drawLabelsLayer(nodes, scale, labelPaint, vpLeft, vpTop, vpRight, vpBottom)
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Layer: star field — twinkling background stars
// ──────────────────────────────────────────────────────────────────────────────

// Deterministic hash for procedural generation — fast, no allocations
private fun starHash(cx: Int, cy: Int, idx: Int): Int {
    var h = cx * 374761393 + cy * 668265263 + idx * 1274126177
    h = (h xor (h ushr 13)) * 1274126177
    h = h xor (h ushr 16)
    return h
}

private fun DrawScope.drawProceduralStars(
    particleTime: Float,
    scale: Float,
    vpLeft: Float, vpTop: Float, vpRight: Float, vpBottom: Float,
) {
    val cellSize = 300f
    val twoPi = 2f * Math.PI.toFloat()

    val cxMin = kotlin.math.floor(vpLeft / cellSize).toInt()
    val cxMax = kotlin.math.floor(vpRight / cellSize).toInt()
    val cyMin = kotlin.math.floor(vpTop / cellSize).toInt()
    val cyMax = kotlin.math.floor(vpBottom / cellSize).toInt()

    val totalCells = (cxMax - cxMin + 1).toLong() * (cyMax - cyMin + 1).toLong()
    val starsPerCell = when {
        totalCells > 2000 -> 1
        totalCells > 800  -> 2
        totalCells > 300  -> 3
        else              -> 4
    }

    for (cx in cxMin..cxMax) {
        for (cy in cyMin..cyMax) {
            for (i in 0 until starsPerCell) {
                val h = starHash(cx, cy, i)
                val fx = ((h and 0xFFFF).toFloat() / 65535f)
                val fy = (((h ushr 8) and 0xFFFF).toFloat() / 65535f)
                val x = cx * cellSize + fx * cellSize
                val y = cy * cellSize + fy * cellSize

                val sizeRaw = 0.4f + ((h ushr 16) and 0xFF).toFloat() / 255f * 1.4f
                val baseAlpha = 0.12f + ((h ushr 4) and 0xFF).toFloat() / 255f * 0.45f
                val speed = 0.4f + ((h ushr 12) and 0xFF).toFloat() / 255f * 1.6f
                val phase = ((h ushr 20) and 0xFF).toFloat() / 255f * twoPi

                val twinkle = 0.5f + 0.5f * sin(particleTime * twoPi * speed + phase)
                val alpha = baseAlpha * twinkle
                if (alpha < 0.02f) continue

                val r = (sizeRaw / scale.coerceAtLeast(0.12f)).coerceIn(0.3f, 3.5f)
                drawCircle(
                    color = Color.White.copy(alpha = alpha),
                    radius = r,
                    center = Offset(x, y),
                )
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Layer: ambient dust motes
// ──────────────────────────────────────────────────────────────────────────────

private fun DrawScope.drawDustLayer(
    dust: List<DustMote>,
    particleTime: Float,
    scale: Float,
    vpLeft: Float, vpTop: Float, vpRight: Float, vpBottom: Float,
) {
    val twoPi = 2f * Math.PI.toFloat()
    dust.forEach { d ->
        val x = d.baseX + sin(particleTime * twoPi * d.speedX + d.phase) * d.driftAmpX
        val y = d.baseY + cos(particleTime * twoPi * d.speedY + d.phase + 1.3f) * d.driftAmpY
        if (!isVis(x, y, 4f, vpLeft, vpTop, vpRight, vpBottom)) return@forEach
        val r = d.size / scale.coerceAtLeast(0.1f)
        drawCircle(
            color = Color(0xFFAABBDD).copy(alpha = d.alpha),
            radius = r.coerceIn(0.3f, 3.5f),
            center = Offset(x, y),
        )
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Layer: constellation lines — thin lines between nodes in the same cluster
// ──────────────────────────────────────────────────────────────────────────────

private fun DrawScope.drawConstellations(
    nodes: List<NodeRenderInfo>,
    scale: Float,
    vpLeft: Float, vpTop: Float, vpRight: Float, vpBottom: Float,
) {
    val groups = nodes.filter { it.isFocused }.groupBy { it.cluster }
    val strokeW = (0.6f / scale).coerceIn(0.15f, 2f)

    groups.forEach { (cluster, ns) ->
        if (ns.size < 2) return@forEach
        val col = clusterColor(cluster).copy(alpha = 0.07f)

        // MST-like: connect each node to its nearest already-connected neighbor
        // (greedy nearest-neighbor chain → visually clean "constellation" shape)
        val connected = mutableListOf(ns[0])
        val remaining = ns.drop(1).toMutableList()
        while (remaining.isNotEmpty()) {
            var bestDist = Float.MAX_VALUE
            var bestFrom: NodeRenderInfo? = null
            var bestTo: NodeRenderInfo? = null
            for (c in connected) {
                for (r in remaining) {
                    val d = dist(c.x, c.y, r.x, r.y)
                    if (d < bestDist) { bestDist = d; bestFrom = c; bestTo = r }
                }
            }
            if (bestTo == null) break
            remaining.remove(bestTo!!)
            connected.add(bestTo!!)
            val from = bestFrom!!; val to = bestTo!!
            if (anyVisible(from.x, from.y, to.x, to.y, 10f, vpLeft, vpTop, vpRight, vpBottom)) {
                drawLine(col, Offset(from.x, from.y), Offset(to.x, to.y), strokeWidth = strokeW)
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Layer: cluster soft backgrounds
// ──────────────────────────────────────────────────────────────────────────────

private fun DrawScope.drawClusterBackgrounds(nodes: List<NodeRenderInfo>, scale: Float) {
    if (scale < 0.18f) return
    val groups = nodes.filter { it.isFocused }.groupBy { it.cluster }
    groups.forEach { (cluster, ns) ->
        if (ns.size < 3) return@forEach
        val cx = ns.map { it.x }.average().toFloat()
        val cy = ns.map { it.y }.average().toFloat()
        val maxD = ns.maxOf { dist(it.x, it.y, cx, cy) }
        val r = (maxD + 55f).coerceIn(70f, 700f)
        val col = clusterColor(cluster)
        drawCircle(
            brush = Brush.radialGradient(
                listOf(col.copy(alpha = 0.048f), col.copy(alpha = 0.014f), Color.Transparent),
                center = Offset(cx, cy), radius = r,
            ),
            radius = r, center = Offset(cx, cy),
        )
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Layer: gradient Bezier edges — color flows from source to target
// ──────────────────────────────────────────────────────────────────────────────

private fun DrawScope.drawEdgesLayer(
    nodes: List<NodeRenderInfo>,
    edges: List<CanvasEdge>,
    nodeMap: Map<String, NodeRenderInfo>,
    scale: Float,
    particleTime: Float,
    baseStroke: Float,
    vpLeft: Float, vpTop: Float, vpRight: Float, vpBottom: Float,
) {
    edges.forEachIndexed { idx, edge ->
        val src = nodeMap[edge.sourceId] ?: return@forEachIndexed
        val tgt = nodeMap[edge.targetId] ?: return@forEachIndexed

        if (!anyVisible(src.x, src.y, tgt.x, tgt.y, 20f, vpLeft, vpTop, vpRight, vpBottom)) return@forEachIndexed

        val focusMul = if (src.isFocused && tgt.isFocused) 1f else 0.15f

        val flashBoost = if (edge.strength > 0.7f) {
            val flashPhase = (particleTime * 2.8f + idx * 0.41f) % 1f
            1f + (1f - abs(flashPhase * 2f - 1f)) * 0.65f
        } else 1f

        val alpha = (0.40f + edge.strength * 0.35f) * focusMul * flashBoost
        val strokeW = 1f + edge.strength * 3f

        val srcCol = Color(src.colorInt)
        val tgtCol = Color(tgt.colorInt)

        val mx = (src.x + tgt.x) / 2f
        val my = (src.y + tgt.y) / 2f
        val dx = tgt.x - src.x; val dy = tgt.y - src.y
        val len = sqrt(dx * dx + dy * dy).coerceAtLeast(1f)
        val perpFactor = (len * 0.12f).coerceAtMost(120f)
        val cpX = mx - dy / len * perpFactor
        val cpY = my + dx / len * perpFactor

        val tStart = findExitT(src.x, src.y, cpX, cpY, tgt.x, tgt.y, src.x, src.y, src.radius + 2f)
        val tEnd   = 1f - findExitT(tgt.x, tgt.y, cpX, cpY, src.x, src.y, tgt.x, tgt.y, tgt.radius + 2f)

        if (tStart >= tEnd) return@forEachIndexed

        // Draw gradient edge: subdivide Bezier into segments with interpolated color
        val segments = 8
        val clampedAlpha = alpha.coerceIn(0.1f, 0.75f)
        var prevX = qBez(src.x, cpX, tgt.x, tStart)
        var prevY = qBez(src.y, cpY, tgt.y, tStart)
        for (seg in 1..segments) {
            val frac = seg.toFloat() / segments
            val t = tStart + (tEnd - tStart) * frac
            val curX = qBez(src.x, cpX, tgt.x, t)
            val curY = qBez(src.y, cpY, tgt.y, t)
            val segColor = lerp(srcCol, tgtCol, t).let { lerp(it, Color.White, 0.30f) }
                .copy(alpha = clampedAlpha)
            drawLine(
                color = segColor,
                start = Offset(prevX, prevY),
                end = Offset(curX, curY),
                strokeWidth = strokeW,
                cap = StrokeCap.Round,
            )
            prevX = curX; prevY = curY
        }
    }
}

// Quadratic Bezier evaluation
private fun qBez(p0: Float, cp: Float, p1: Float, t: Float): Float {
    val u = 1f - t; return u * u * p0 + 2f * u * t * cp + t * t * p1
}

/** Binary search for t where quadratic Bezier exits a circle (cx,cy,r). */
private fun findExitT(
    p0x: Float, p0y: Float, cpx: Float, cpy: Float, p1x: Float, p1y: Float,
    cx: Float, cy: Float, r: Float
): Float {
    var lo = 0f; var hi = 0.5f
    repeat(12) {
        val mid = (lo + hi) / 2f
        val d = dist(qBez(p0x, cpx, p1x, mid), qBez(p0y, cpy, p1y, mid), cx, cy)
        if (d < r) lo = mid else hi = mid
    }
    return hi
}

/** Extract sub-Bezier (a..b) using de Casteljau and return as Path. */
private fun buildSubBezierPath(
    p0x: Float, p0y: Float, cpx: Float, cpy: Float, p1x: Float, p1y: Float,
    a: Float, b: Float
): Path {
    val rp0x = qBez(p0x, cpx, p1x, a)
    val rp0y = qBez(p0y, cpy, p1y, a)
    val rcpx = (1f - a) * cpx + a * p1x
    val rcpy = (1f - a) * cpy + a * p1y
    val rp1x = p1x; val rp1y = p1y

    val tp = if (a < 1f) ((b - a) / (1f - a)).coerceIn(0f, 1f) else 1f

    val newP0x = rp0x; val newP0y = rp0y
    val newCPx = (1f - tp) * rp0x + tp * rcpx
    val newCPy = (1f - tp) * rp0y + tp * rcpy
    val newP1x = qBez(rp0x, rcpx, rp1x, tp)
    val newP1y = qBez(rp0y, rcpy, rp1y, tp)

    return Path().apply {
        moveTo(newP0x, newP0y)
        quadraticTo(newCPx, newCPy, newP1x, newP1y)
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Layer: SSE active path animations
// ──────────────────────────────────────────────────────────────────────────────

private fun DrawScope.drawActivePathsLayer(
    nodes: List<NodeRenderInfo>,
    activePaths: List<PathAnimation>,
    nodeMap: Map<String, NodeRenderInfo>,
) {
    activePaths.forEach { anim ->
        val ids = anim.nodeNames.mapNotNull { name ->
            nodes.find { it.label.equals(name, ignoreCase = true) }?.id
        }
        if (ids.size < 2) return@forEach
        val toShow = (anim.progress * (ids.size - 1)).toInt()
        for (i in 0 until toShow) {
            val s = nodeMap[ids[i]] ?: continue
            val t = nodeMap[ids[i + 1]] ?: continue
            drawLine(
                color = anim.color.copy(alpha = 0.85f),
                start = Offset(s.x, s.y), end = Offset(t.x, t.y),
                strokeWidth = 3.5f, cap = StrokeCap.Round,
                pathEffect = PathEffect.dashPathEffect(
                    floatArrayOf(14f, 8f), phase = anim.progress * 40f
                ),
            )
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Layer: particle trails on edges — 4-dot trail with fading alpha
// ──────────────────────────────────────────────────────────────────────────────

private fun DrawScope.drawEdgeParticleTrails(
    nodes: List<NodeRenderInfo>,
    edges: List<CanvasEdge>,
    nodeMap: Map<String, NodeRenderInfo>,
    particleTime: Float,
    scale: Float,
) {
    val headR = (2.8f / scale).coerceIn(1.5f, 7f)
    val trailCount = 4
    val trailGap = 0.018f
    edges.forEachIndexed { idx, edge ->
        if (edge.strength < 0.5f) return@forEachIndexed
        val src = nodeMap[edge.sourceId] ?: return@forEachIndexed
        val tgt = nodeMap[edge.targetId] ?: return@forEachIndexed
        if (!src.isFocused || !tgt.isFocused) return@forEachIndexed

        val srcCol = Color(src.colorInt)
        val tgtCol = Color(tgt.colorInt)

        repeat(2) { k ->
            val headT = (particleTime + idx * 0.14f + k * 0.5f) % 1f
            for (trail in 0 until trailCount) {
                val t = (headT - trail * trailGap).let { if (it < 0f) it + 1f else it }
                val px = lerp(src.x, tgt.x, t)
                val py = lerp(src.y, tgt.y, t)
                if (dist(px, py, src.x, src.y) < src.radius) continue
                if (dist(px, py, tgt.x, tgt.y) < tgt.radius) continue
                val col = lerp(srcCol, tgtCol, t)
                val alphaFade = 1f - trail.toFloat() / trailCount
                val dotR = headR * (1f - trail * 0.15f)
                drawCircle(col.copy(alpha = 0.72f * alphaFade), dotR.coerceAtLeast(0.5f), Offset(px, py))
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Layer: connection glow — pulsing energy dots where edges meet node surfaces
// ──────────────────────────────────────────────────────────────────────────────

private fun DrawScope.drawConnectionGlowLayer(
    edges: List<CanvasEdge>,
    nodeMap: Map<String, NodeRenderInfo>,
    scale: Float,
    particleTime: Float,
    vpLeft: Float, vpTop: Float, vpRight: Float, vpBottom: Float,
) {
    if (scale < 0.22f) return

    edges.forEachIndexed { idx, edge ->
        val src = nodeMap[edge.sourceId] ?: return@forEachIndexed
        val tgt = nodeMap[edge.targetId] ?: return@forEachIndexed
        if (!src.isFocused && !tgt.isFocused) return@forEachIndexed
        if (!anyVisible(src.x, src.y, tgt.x, tgt.y, 20f, vpLeft, vpTop, vpRight, vpBottom)) return@forEachIndexed

        val srcCol = Color(src.colorInt)
        val tgtCol = Color(tgt.colorInt)
        val midCol = lerp(srcCol, tgtCol, 0.5f)
        val glowCol = lerp(midCol, Color.White, 0.5f)

        val phase = (particleTime * 1.8f + idx * 0.37f) % 1f
        val pulse = 0.5f + 0.5f * sin(phase * 2f * Math.PI.toFloat())

        val mx = (src.x + tgt.x) / 2f
        val my = (src.y + tgt.y) / 2f
        val dx = tgt.x - src.x; val dy = tgt.y - src.y
        val len = sqrt(dx * dx + dy * dy).coerceAtLeast(1f)
        val perpFactor = (len * 0.12f).coerceAtMost(120f)
        val cpX = mx - dy / len * perpFactor
        val cpY = my + dx / len * perpFactor

        val tSrc = findExitT(src.x, src.y, cpX, cpY, tgt.x, tgt.y, src.x, src.y, src.radius)
        val srcGlowX = qBez(src.x, cpX, tgt.x, tSrc)
        val srcGlowY = qBez(src.y, cpY, tgt.y, tSrc)

        val tTgt = findExitT(tgt.x, tgt.y, cpX, cpY, src.x, src.y, tgt.x, tgt.y, tgt.radius)
        val tgtGlowX = qBez(tgt.x, cpX, src.x, tTgt)
        val tgtGlowY = qBez(tgt.y, cpY, src.y, tTgt)

        val baseGlowR = (3.5f + edge.strength * 2.5f)
        val glowR = baseGlowR * (0.8f + pulse * 0.5f)
        val coreR = baseGlowR * 0.4f

        val focusAlpha = if (src.isFocused && tgt.isFocused) 1f else 0.3f

        if (isVis(srcGlowX, srcGlowY, glowR + 4f, vpLeft, vpTop, vpRight, vpBottom)) {
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(
                        glowCol.copy(alpha = 0.55f * pulse * focusAlpha),
                        glowCol.copy(alpha = 0.15f * pulse * focusAlpha),
                        Color.Transparent
                    ),
                    center = Offset(srcGlowX, srcGlowY), radius = glowR
                ),
                radius = glowR, center = Offset(srcGlowX, srcGlowY)
            )
            drawCircle(
                color = glowCol.copy(alpha = (0.7f + pulse * 0.3f) * focusAlpha),
                radius = coreR, center = Offset(srcGlowX, srcGlowY)
            )
        }

        if (isVis(tgtGlowX, tgtGlowY, glowR + 4f, vpLeft, vpTop, vpRight, vpBottom)) {
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(
                        glowCol.copy(alpha = 0.55f * pulse * focusAlpha),
                        glowCol.copy(alpha = 0.15f * pulse * focusAlpha),
                        Color.Transparent
                    ),
                    center = Offset(tgtGlowX, tgtGlowY), radius = glowR
                ),
                radius = glowR, center = Offset(tgtGlowX, tgtGlowY)
            )
            drawCircle(
                color = glowCol.copy(alpha = (0.7f + pulse * 0.3f) * focusAlpha),
                radius = coreR, center = Offset(tgtGlowX, tgtGlowY)
            )
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Layer: hot-node ripple rings — periodic expanding rings from hot nodes
// ──────────────────────────────────────────────────────────────────────────────

private fun DrawScope.drawHotNodeRipples(
    nodes: List<NodeRenderInfo>,
    particleTime: Float,
    scale: Float,
    vpLeft: Float, vpTop: Float, vpRight: Float, vpBottom: Float,
) {
    val strokeW = (1.2f / scale).coerceIn(0.4f, 3f)
    val twoPi = 2f * Math.PI.toFloat()
    nodes.forEach { n ->
        if (!n.isHot || !n.isFocused) return@forEach
        if (n.radius <= 0.5f) return@forEach
        if (!isVis(n.x, n.y, n.radius * 5f, vpLeft, vpTop, vpRight, vpBottom)) return@forEach

        val col = Color(n.colorInt)
        val nodeHash = (n.id.hashCode() and 0xFFFF) * 0.0001f

        repeat(2) { ring ->
            val phase = (particleTime * 0.4f + nodeHash + ring * 0.5f) % 1f
            val r = n.radius * (1f + phase * 3.5f)
            val alpha = (1f - phase).pow(2.2f) * 0.3f
            if (alpha < 0.01f) return@repeat
            drawCircle(
                color = col.copy(alpha = alpha),
                radius = r,
                center = Offset(n.x, n.y),
                style = Stroke(width = strokeW),
            )
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Layer: glow halos behind nodes
// ──────────────────────────────────────────────────────────────────────────────

private fun DrawScope.drawGlowsLayer(
    nodes: List<NodeRenderInfo>,
    scale: Float,
    vpLeft: Float, vpTop: Float, vpRight: Float, vpBottom: Float,
) {
    nodes.forEach { n ->
        if (!n.isFocused) return@forEach
        if (n.radius <= 0.5f) return@forEach
        if (!isVis(n.x, n.y, n.radius * 3.5f, vpLeft, vpTop, vpRight, vpBottom)) return@forEach
        val col = Color(n.colorInt)
        val hotMul  = if (n.isHot) 2.0f else 1f
        val impMul  = 1f + n.importance * 0.4f
        val hlMul   = 1f + n.highlightAlpha * 1.6f
        val glowR   = n.radius * 3.5f * impMul + n.glowRadius
        val peak = 0.22f * hotMul * hlMul
        drawCircle(
            brush = Brush.radialGradient(
                colorStops = arrayOf(
                    0.00f to col.copy(alpha = peak),
                    0.20f to col.copy(alpha = peak * 0.75f),
                    0.40f to col.copy(alpha = peak * 0.45f),
                    0.60f to col.copy(alpha = peak * 0.22f),
                    0.80f to col.copy(alpha = peak * 0.07f),
                    1.00f to Color.Transparent,
                ),
                center = Offset(n.x, n.y), radius = glowR,
            ),
            radius = glowR, center = Offset(n.x, n.y),
        )
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Layer: node circles
// ──────────────────────────────────────────────────────────────────────────────

private fun DrawScope.drawNodesLayer(
    nodes: List<NodeRenderInfo>,
    scale: Float,
    vpLeft: Float, vpTop: Float, vpRight: Float, vpBottom: Float,
) {
    val isLOD = scale < 0.28f
    nodes.forEach { n ->
        if (n.radius <= 0.5f) return@forEach
        if (!isVis(n.x, n.y, n.radius + 2f, vpLeft, vpTop, vpRight, vpBottom)) return@forEach
        val ctr = Offset(n.x, n.y)
        val r   = n.radius
        val col = Color(n.colorInt)
        val fa  = if (n.isFocused) 1f else 0.2f

        if (isLOD) {
            val dotR = (5f / scale).coerceAtLeast(2f)
            drawCircle(col.copy(alpha = 0.75f * fa), dotR, ctr)
            return@forEach
        }

        drawCircle(
            brush = Brush.radialGradient(
                listOf(
                    col.copy(alpha = 0.88f * fa),
                    col.copy(alpha = 0.58f * fa),
                    col.copy(alpha = 0.32f * fa),
                ),
                center = ctr, radius = r,
            ),
            radius = r, center = ctr,
        )

        val borderAlpha = if (n.isSelected) 1f else (0.55f + n.highlightAlpha * 0.45f)
        drawCircle(
            color = if (n.isSelected) Color.White.copy(alpha = 0.95f)
                    else col.copy(alpha = borderAlpha * fa),
            radius = r, center = ctr,
            style = Stroke(width = if (n.isSelected) (2.8f / scale).coerceIn(1f, 4f)
                                   else (1.2f / scale).coerceIn(0.4f, 3f)),
        )

        if (n.annotationCount > 0 && scale > 0.42f) {
            val bR = (5f / scale).coerceIn(2f, 9f)
            drawCircle(Color(0xFFFFD166), bR, Offset(n.x + r * 0.78f, n.y - r * 0.78f))
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Layer: category icons
// ──────────────────────────────────────────────────────────────────────────────

private fun DrawScope.drawIconsLayer(
    nodes: List<NodeRenderInfo>,
    scale: Float,
    iconStroke: Float,
    vpLeft: Float, vpTop: Float, vpRight: Float, vpBottom: Float,
) {
    nodes.forEach { n ->
        if (!n.isFocused) return@forEach
        if (n.radius <= 0.5f) return@forEach
        if (!isVis(n.x, n.y, n.radius + 2f, vpLeft, vpTop, vpRight, vpBottom)) return@forEach
        val iconR = (n.radius * 0.52f).coerceIn(4f, 20f)
        drawCategoryIcon(n.cluster, Offset(n.x, n.y), iconR, iconStroke)
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Layer: labels — adaptive opacity based on zoom & importance
// ──────────────────────────────────────────────────────────────────────────────

private fun DrawScope.drawLabelsLayer(
    nodes: List<NodeRenderInfo>,
    scale: Float,
    paint: android.graphics.Paint,
    vpLeft: Float, vpTop: Float, vpRight: Float, vpBottom: Float,
) {
    val baseWorldSize = 16f

    // Adaptive: at low zoom only important/hot/selected labels show;
    // as zoom increases, progressively more labels fade in.
    val importanceThreshold = when {
        scale > 0.7f  -> -1f     // show all
        scale > 0.45f -> 0.2f    // most labels
        scale > 0.30f -> 0.5f    // only important
        else          -> 0.7f    // only very important
    }

    drawIntoCanvas { composCanvas ->
        val nc = composCanvas.nativeCanvas
        nodes.forEach { n ->
            if (!n.isFocused) return@forEach
            if (n.radius <= 0.5f) return@forEach
            if (!isVis(n.x, n.y, n.radius + baseWorldSize * 2f, vpLeft, vpTop, vpRight, vpBottom)) return@forEach

            val alwaysShow = n.isHot || n.isSelected || n.highlightAlpha > 0.1f
            if (!alwaysShow && n.importance < importanceThreshold) return@forEach

            // Smooth fade: alpha ramps up as zoom increases or importance is higher
            val zoomAlpha = when {
                alwaysShow -> 1f
                scale > 0.7f -> 1f
                scale > 0.45f -> 0.5f + (scale - 0.45f) / 0.25f * 0.5f
                else -> 0.3f + n.importance * 0.4f
            }

            paint.textSize = if (n.isHot || n.isSelected) baseWorldSize * 1.15f else baseWorldSize
            val baseAlpha = if (n.isSelected) 255 else (210 * zoomAlpha).toInt().coerceIn(30, 210)
            paint.color = android.graphics.Color.argb(baseAlpha, 255, 255, 255)
            paint.isFakeBoldText = n.isHot || n.isSelected

            val labelY = n.y + n.radius + 10f
            nc.drawText(n.label.take(44), n.x, labelY, paint)
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Helpers
// ──────────────────────────────────────────────────────────────────────────────

private fun isVis(
    x: Float, y: Float, margin: Float,
    l: Float, t: Float, r: Float, b: Float,
): Boolean = x + margin > l && x - margin < r && y + margin > t && y - margin < b

private fun anyVisible(
    ax: Float, ay: Float, bx: Float, by: Float, margin: Float,
    l: Float, t: Float, r: Float, b: Float,
): Boolean = isVis(ax, ay, margin, l, t, r, b) || isVis(bx, by, margin, l, t, r, b)

private fun dist(ax: Float, ay: Float, bx: Float, by: Float): Float {
    val dx = ax - bx; val dy = ay - by
    return sqrt(dx * dx + dy * dy)
}

private fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t

private fun lerp(a: Color, b: Color, t: Float) = Color(
    red   = lerp(a.red,   b.red,   t),
    green = lerp(a.green, b.green, t),
    blue  = lerp(a.blue,  b.blue,  t),
    alpha = lerp(a.alpha, b.alpha, t),
)

// ──────────────────────────────────────────────────────────────────────────────
// Layer: tap-pulse shock rings
// ──────────────────────────────────────────────────────────────────────────────

private fun DrawScope.drawTapPulsesLayer(pulses: List<TapPulse>, scale: Float) {
    pulses.forEach { pulse ->
        repeat(3) { ring ->
            val delay = ring * 0.28f
            val t = (pulse.progress - delay).coerceIn(0f, 1f)
            if (t <= 0f) return@repeat
            val r = pulse.baseRadius * (1f + t * 3.5f)
            val alpha = (1f - t).pow(1.8f) * 0.65f
            val strokeW = (2.2f / scale).coerceIn(0.7f, 5f)
            val nodeColor = Color(pulse.colorInt)
            val col = lerp(nodeColor, Color.White, t * 0.35f).copy(alpha = alpha)
            drawCircle(
                color = col,
                radius = r,
                center = Offset(pulse.x, pulse.y),
                style = Stroke(width = strokeW),
            )
        }
    }
}
