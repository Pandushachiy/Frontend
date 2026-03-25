package com.health.companion.presentation.screens.canvas

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.health.companion.R
import com.health.companion.data.canvas.CanvasCluster

// Круглые кнопки — pill shape
private val CHIP_SHAPE = RoundedCornerShape(20.dp)

// Muted blue-gray — harmonises with dark canvas, avoids harsh white
private val INACTIVE_TEXT = Color(0xFF7E8FA3)
private val INACTIVE_ICON = Color(0xFF5E6E80)
private val CHIP_BG_DARK  = Color(0xFF0B1018)        // near-black chip base
private val CHIP_BORDER   = Color(0xFF1C2A3A)

/** Strip emoji and punctuation, leaving only letters, digits, spaces and dashes. */
private fun String.cleanLabel(): String =
    replace(Regex("[^\\p{L}\\p{N}\\s\\-]"), "").trim()

@Composable
fun ClusterFilterBar(
    clusters: List<CanvasCluster>,
    activeCluster: String?,
    onClusterClick: (String) -> Unit,
    onResetClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Панель: вровень с левым краем табов (12dp), ширина ≈ 3/4 экрана (до начала «Ещё»)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 12.dp, end = 12.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(end = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            item {
                Chip(
                    label      = stringResource(R.string.filter_all),
                    count      = 0,
                    color      = Color(0xFF6366F1),
                    isActive   = activeCluster == null,
                    categoryId = "all",
                    onClick    = onResetClick,
                )
            }
            items(clusters) { cluster ->
                val col = remember(cluster.color) {
                    runCatching { Color(android.graphics.Color.parseColor(cluster.color)) }
                        .getOrElse { Color(0xFF4A9EFF) }
                }
                Chip(
                    label      = cluster.label.cleanLabel(),
                    count      = cluster.nodeCount,
                    color      = col,
                    isActive   = activeCluster == cluster.id,
                    categoryId = cluster.id,
                    onClick    = { onClusterClick(cluster.id) },
                )
            }
        }
    }
}

@Composable
private fun Chip(
    label: String,
    count: Int,
    color: Color,
    isActive: Boolean,
    categoryId: String,
    onClick: () -> Unit,
) {
    val bg by animateColorAsState(
        if (isActive) color.copy(alpha = 0.20f) else CHIP_BG_DARK.copy(alpha = 0.82f),
        tween(160), label = "bg",
    )
    val borderCol by animateColorAsState(
        if (isActive) color.copy(alpha = 0.60f) else CHIP_BORDER,
        tween(160), label = "bd",
    )
    val iconTint by animateColorAsState(
        if (isActive) color else INACTIVE_ICON,
        tween(160), label = "ic",
    )
    val textCol by animateColorAsState(
        if (isActive) color.copy(alpha = 0.95f) else INACTIVE_TEXT,
        tween(160), label = "tc",
    )

    Box(
        modifier = Modifier
            .clip(CHIP_SHAPE)
            .background(bg)
            .border(Dp(0.5f), borderCol, CHIP_SHAPE)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CategoryIconChip(categoryId = categoryId, tint = iconTint)

            Text(
                text       = label,
                color      = textCol,
                fontSize   = 11.sp,
                fontWeight = if (isActive) FontWeight.Medium else FontWeight.Normal,
                maxLines   = 1,
            )
            if (count > 0) {
                Text(
                    text     = count.toString(),
                    color    = textCol.copy(alpha = 0.50f),
                    fontSize = 9.sp,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun CategoryIconChip(categoryId: String, tint: Color) {
    val iconSizeDp = 13.dp
    Canvas(modifier = Modifier.size(iconSizeDp)) {
        val r = size.minDimension * 0.42f
        val cx = size.width  / 2f
        val cy = size.height / 2f
        drawCategoryIcon(
            cluster = categoryId,
            center  = Offset(cx, cy),
            iconR   = r,
            strokeW = size.minDimension * 0.13f,
            color   = tint,
        )
    }
}
