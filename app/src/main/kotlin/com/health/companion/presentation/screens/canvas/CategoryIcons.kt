package com.health.companion.presentation.screens.canvas

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.cos
import kotlin.math.sin

/**
 * Draw a category-specific line-art icon inside a node circle.
 * [center]  — world-space centre of the node
 * [iconR]   — icon radius in world units (~0.5 × node radius)
 * [strokeW] — stroke width in world units (caller scales for screen-constant width)
 */
fun DrawScope.drawCategoryIcon(
    cluster: String,
    center: Offset,
    iconR: Float,
    strokeW: Float,
    color: Color = Color.White.copy(alpha = 0.92f),
) {
    val white = color
    val st = Stroke(width = strokeW, cap = StrokeCap.Round, join = StrokeJoin.Round)
    when (cluster.lowercase().trim()) {
        "documents", "document", "docs",
        "документы", "документ"              -> iconDocument(center, iconR, white, st)

        "health", "medical", "здоровье",
        "медицина"                            -> iconCross(center, iconR, white, strokeW)

        "finance", "financial", "money",
        "финансы", "деньги"                  -> iconCoin(center, iconR, white, st)

        "contacts", "contact", "people",
        "контакты", "люди", "человек"        -> iconPerson(center, iconR, white, st)

        "skills", "skill", "knowledge",
        "навыки", "знания"                   -> iconLightning(center, iconR, white, st)

        "work", "career", "job",
        "работа", "карьера"                  -> iconBriefcase(center, iconR, white, st)

        "personal", "lifestyle",
        "личное", "жизнь"                    -> iconDiamond(center, iconR, white, st)

        "interests", "интересы",
        "хобби"                              -> iconStar(center, iconR, white, strokeW)

        "goals", "цели", "goal",
        "цель"                               -> iconTarget(center, iconR, white, st)

        "general", "facts", "факты",
        "общее"                              -> iconList(center, iconR, white, strokeW)

        else                                  -> iconDots(center, iconR, white, strokeW)
    }
}

// ── Document (page outline with fold + 3 lines) ───────────────────────────────

private fun DrawScope.iconDocument(c: Offset, r: Float, col: Color, st: Stroke) {
    val f = r * 0.28f
    val path = Path().apply {
        moveTo(c.x - r * 0.62f, c.y - r)
        lineTo(c.x + r * 0.35f, c.y - r)
        lineTo(c.x + r * 0.62f, c.y - r + f)
        lineTo(c.x + r * 0.62f, c.y + r)
        lineTo(c.x - r * 0.62f, c.y + r)
        close()
        // Fold corner
        moveTo(c.x + r * 0.35f, c.y - r)
        lineTo(c.x + r * 0.35f, c.y - r + f)
        lineTo(c.x + r * 0.62f, c.y - r + f)
    }
    drawPath(path, col, style = st)
    val lw = st.width * 0.85f
    val la = col.copy(alpha = 0.5f)
    drawLine(la, Offset(c.x - r*0.38f, c.y - r*0.32f), Offset(c.x + r*0.35f, c.y - r*0.32f), lw)
    drawLine(la, Offset(c.x - r*0.38f, c.y + r*0.08f), Offset(c.x + r*0.35f, c.y + r*0.08f), lw)
    drawLine(la, Offset(c.x - r*0.38f, c.y + r*0.48f), Offset(c.x + r*0.05f, c.y + r*0.48f), lw)
}

// ── Health — cross / plus ─────────────────────────────────────────────────────

private fun DrawScope.iconCross(c: Offset, r: Float, col: Color, sw: Float) {
    val thick = sw * 2.4f
    drawLine(col, Offset(c.x, c.y - r), Offset(c.x, c.y + r), thick, StrokeCap.Round)
    drawLine(col, Offset(c.x - r, c.y), Offset(c.x + r, c.y), thick, StrokeCap.Round)
}

// ── Finance — coin with $ stroke ──────────────────────────────────────────────

private fun DrawScope.iconCoin(c: Offset, r: Float, col: Color, st: Stroke) {
    drawCircle(col, radius = r, center = c, style = st)
    val lw = st.width * 1.8f
    drawLine(col, Offset(c.x, c.y - r * 0.52f), Offset(c.x, c.y + r * 0.52f), lw, StrokeCap.Round)
    val ha = col.copy(alpha = 0.55f)
    drawLine(ha, Offset(c.x - r*0.32f, c.y - r*0.18f), Offset(c.x + r*0.32f, c.y - r*0.18f), st.width)
    drawLine(ha, Offset(c.x - r*0.32f, c.y + r*0.22f), Offset(c.x + r*0.32f, c.y + r*0.22f), st.width)
}

// ── Contacts — person silhouette ──────────────────────────────────────────────

private fun DrawScope.iconPerson(c: Offset, r: Float, col: Color, st: Stroke) {
    drawCircle(col, radius = r * 0.36f, center = Offset(c.x, c.y - r * 0.48f), style = st)
    val body = Path().apply {
        moveTo(c.x - r * 0.65f, c.y + r)
        cubicTo(
            c.x - r * 0.65f, c.y + r * 0.05f,
            c.x + r * 0.65f, c.y + r * 0.05f,
            c.x + r * 0.65f, c.y + r,
        )
    }
    drawPath(body, col, style = st)
}

// ── Skills — lightning bolt ───────────────────────────────────────────────────

private fun DrawScope.iconLightning(c: Offset, r: Float, col: Color, st: Stroke) {
    val path = Path().apply {
        moveTo(c.x + r * 0.22f, c.y - r)
        lineTo(c.x - r * 0.18f, c.y - r * 0.04f)
        lineTo(c.x + r * 0.10f, c.y - r * 0.04f)
        lineTo(c.x - r * 0.22f, c.y + r)
        lineTo(c.x + r * 0.18f, c.y + r * 0.04f)
        lineTo(c.x - r * 0.10f, c.y + r * 0.04f)
        close()
    }
    drawPath(path, col, style = st)
}

// ── Work — briefcase ──────────────────────────────────────────────────────────

private fun DrawScope.iconBriefcase(c: Offset, r: Float, col: Color, st: Stroke) {
    drawRoundRect(
        color = col,
        topLeft = Offset(c.x - r * 0.72f, c.y - r * 0.28f),
        size = Size(r * 1.44f, r * 1.28f),
        cornerRadius = CornerRadius(r * 0.14f),
        style = st,
    )
    val handle = Path().apply {
        moveTo(c.x - r * 0.34f, c.y - r * 0.28f)
        lineTo(c.x - r * 0.34f, c.y - r * 0.68f)
        lineTo(c.x + r * 0.34f, c.y - r * 0.68f)
        lineTo(c.x + r * 0.34f, c.y - r * 0.28f)
    }
    drawPath(handle, col, style = st)
    drawLine(col.copy(alpha = 0.45f), Offset(c.x, c.y - r*0.28f), Offset(c.x, c.y + r), st.width)
}

// ── Personal — diamond ────────────────────────────────────────────────────────

private fun DrawScope.iconDiamond(c: Offset, r: Float, col: Color, st: Stroke) {
    val path = Path().apply {
        moveTo(c.x, c.y - r)
        lineTo(c.x + r * 0.72f, c.y)
        lineTo(c.x, c.y + r)
        lineTo(c.x - r * 0.72f, c.y)
        close()
    }
    drawPath(path, col, style = st)
}

// ── Interests — 5-point star ──────────────────────────────────────────────────

private fun DrawScope.iconStar(c: Offset, r: Float, col: Color, sw: Float) {
    val inner = r * 0.42f
    val path = Path()
    for (i in 0 until 10) {
        val angle = (Math.PI * i / 5.0 - Math.PI / 2.0).toFloat()
        val rad = if (i % 2 == 0) r else inner
        val x = c.x + rad * cos(angle)
        val y = c.y + rad * sin(angle)
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    drawPath(path, col, style = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round))
}

// ── Goals — target / bullseye ─────────────────────────────────────────────────

private fun DrawScope.iconTarget(c: Offset, r: Float, col: Color, st: Stroke) {
    drawCircle(col, radius = r, center = c, style = st)
    drawCircle(col, radius = r * 0.54f, center = c, style = st)
    drawCircle(col, radius = r * 0.18f, center = c)
}

// ── General — list lines ──────────────────────────────────────────────────────

private fun DrawScope.iconList(c: Offset, r: Float, col: Color, sw: Float) {
    val la = col.copy(alpha = 0.6f)
    listOf(-0.45f, 0f, 0.45f).forEach { yMul ->
        drawLine(la, Offset(c.x - r, c.y + r * yMul), Offset(c.x + r, c.y + r * yMul), sw * 1.1f, StrokeCap.Round)
    }
}

// ── Fallback — three dots ─────────────────────────────────────────────────────

private fun DrawScope.iconDots(c: Offset, r: Float, col: Color, sw: Float) {
    val dr = sw * 1.4f
    listOf(-0.5f, 0f, 0.5f).forEach { xMul ->
        drawCircle(col, dr, Offset(c.x + r * xMul, c.y))
    }
}
