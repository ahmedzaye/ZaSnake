package com.ahmedzayed.zasnake.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import com.ahmedzayed.zasnake.game.SnakeBodyType
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.max

/** All five geometries share the same head-to-tail centers and skin palette.
 * Only visuals change: grid physics, collision, and rewards stay in SnakeEngine.
 */
internal fun DrawScope.drawSnakeBodyByType(
    body: SnakeBodyType, centers: List<Offset>, cell: Float, colors: List<Color>
) {
    if (centers.size < 2 || cell <= 0f || colors.isEmpty()) return
    when (body) {
        SnakeBodyType.SMOOTH -> drawSmoothSnakeBody(centers, cell, colors)
        SnakeBodyType.RETRO_PIXEL -> drawRetroPixelBody(centers, cell, colors)
        SnakeBodyType.ARMORED -> drawArmoredBody(centers, cell, colors)
        SnakeBodyType.CRYSTAL -> drawCrystalBody(centers, cell, colors)
        SnakeBodyType.SEGMENTED -> Unit // The classic segmented renderer remains in ZaSnakeApp.
    }
}

private fun segmentColor(index: Int, colors: List<Color>): Color = when (colors.size) {
    1 -> colors[0]
    2 -> colors[1]
    else -> colors[(index % (colors.size - 1)) + 1]
}

private fun closeEnough(a: Offset, b: Offset, cell: Float): Boolean =
    hypot(a.x - b.x, a.y - b.y) <= cell * 1.65f

private fun segmentDirection(centers: List<Offset>, index: Int, cell: Float): Float {
    val here = centers[index]
    val front = if (index > 0 && closeEnough(centers[index - 1], here, cell)) centers[index - 1] else null
    val back = if (index < centers.lastIndex && closeEnough(centers[index + 1], here, cell)) centers[index + 1] else null
    val dx = when {
        front != null && back != null -> front.x - back.x
        front != null -> front.x - here.x
        back != null -> here.x - back.x
        else -> 1f
    }
    val dy = when {
        front != null && back != null -> front.y - back.y
        front != null -> front.y - here.y
        back != null -> here.y - back.y
        else -> 0f
    }
    return Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
}

private fun DrawScope.drawRetroPixelBody(centers: List<Offset>, cell: Float, colors: List<Color>) {
    val pixel = max(1f, cell / 9f)
    // Screen-axis-aligned blocks, deliberately unlike smooth or rounded Segmented tiles.
    for (i in centers.lastIndex downTo 1) {
        val c = centers[i]
        val color = segmentColor(i, colors)
        val tailScale = if (i == centers.lastIndex) .70f else if (i == centers.lastIndex - 1) .85f else 1f
        val width = pixel * (if (tailScale < .8f) 6f else if (tailScale < 1f) 7f else 8f)
        val height = width
        // Join neighboring cells with square 8-bit bridges, never over wrap teleports.
        if (i > 0 && closeEnough(c, centers[i - 1], cell)) {
            val next = centers[i - 1]
            val dx = next.x - c.x
            val dy = next.y - c.y
            if (kotlin.math.abs(dx) >= kotlin.math.abs(dy)) {
                val left = minOf(c.x, next.x)
                drawRect(color.copy(alpha = .94f), Offset(left, c.y - pixel * 2f),
                    Size(kotlin.math.abs(dx), pixel * 4f))
            } else {
                val top = minOf(c.y, next.y)
                drawRect(color.copy(alpha = .94f), Offset(c.x - pixel * 2f, top),
                    Size(pixel * 4f, kotlin.math.abs(dy)))
            }
        }
        // The 8-bit core intentionally has squared corners and visible pixel highlights.
        drawRect(color, topLeft = Offset(c.x - width / 2f, c.y - height / 2f), size = Size(width, height))
        drawRect(Color.Black.copy(alpha = .22f),
            topLeft = Offset(c.x + width / 2f - pixel, c.y - height / 2f), size = Size(pixel, height))
        drawRect(Color.White.copy(alpha = .34f),
            topLeft = Offset(c.x - width / 2f, c.y - height / 2f), size = Size(width - pixel, pixel))
        drawRect(Color.White.copy(alpha = .17f),
            topLeft = Offset(c.x - width / 2f + pixel, c.y - height / 2f + pixel * 2f),
            size = Size(pixel * 2f, pixel))
        // Tiny, square edge pixels add a deliberate retro silhouette.
        if (i % 2 == 0 && i != centers.lastIndex) {
            drawRect(color.copy(alpha = .85f),
                topLeft = Offset(c.x - width / 2f - pixel, c.y - pixel / 2f), size = Size(pixel, pixel))
        }
    }
}

private fun DrawScope.drawArmoredBody(centers: List<Offset>, cell: Float, colors: List<Color>) {
    // Matte undercoat connects plates. Plates then overlap, with directional highlights.
    for (i in centers.lastIndex downTo 1) {
        if (closeEnough(centers[i], centers[i - 1], cell)) {
            drawLine(segmentColor(i, colors).copy(alpha = .65f),
                start = centers[i], end = centers[i - 1], strokeWidth = cell * .54f, cap = StrokeCap.Round)
        }
    }
    for (i in centers.lastIndex downTo 1) {
        val center = centers[i]
        val color = segmentColor(i, colors)
        val scale = if (i == centers.lastIndex) .72f else if (i == centers.lastIndex - 1) .88f else 1f
        rotate(segmentDirection(centers, i, cell), pivot = center) {
            fun x(dx: Float) = center.x + dx * cell * scale
            fun y(dy: Float) = center.y + dy * cell * scale
            val outline = Path().apply {
                moveTo(x(-.40f), y(-.26f)); lineTo(x(-.24f), y(-.37f))
                lineTo(x(.24f), y(-.37f)); lineTo(x(.44f), y(-.21f))
                lineTo(x(.44f), y(.21f)); lineTo(x(.24f), y(.37f))
                lineTo(x(-.24f), y(.37f)); lineTo(x(-.40f), y(.26f)); close()
            }
            drawPath(outline, Color.Black.copy(alpha = .48f))
            val insetPlate = Path().apply {
                moveTo(x(-.35f), y(-.22f)); lineTo(x(-.20f), y(-.31f))
                lineTo(x(.19f), y(-.31f)); lineTo(x(.38f), y(-.17f))
                lineTo(x(.38f), y(.17f)); lineTo(x(.19f), y(.31f))
                lineTo(x(-.20f), y(.31f)); lineTo(x(-.35f), y(.22f)); close()
            }
            drawPath(insetPlate, color)
            drawLine(Color.White.copy(alpha = .34f), Offset(x(-.20f), y(-.25f)),
                Offset(x(.17f), y(-.25f)), strokeWidth = max(1f, cell * .055f))
            drawLine(Color.Black.copy(alpha = .28f), Offset(x(.25f), y(-.17f)),
                Offset(x(.25f), y(.17f)), strokeWidth = max(1f, cell * .065f))
            drawCircle(Color.White.copy(alpha = .28f), radius = cell * .035f * scale,
                center = Offset(x(-.25f), y(0f)))
        }
    }
}

private fun DrawScope.drawCrystalBody(centers: List<Offset>, cell: Float, colors: List<Color>) {
    // Glowing, connected core avoids gaps; crystalline facets are deliberately angular.
    for (i in centers.lastIndex downTo 1) {
        if (closeEnough(centers[i], centers[i - 1], cell)) {
            drawLine(segmentColor(i, colors).copy(alpha = .57f),
                start = centers[i], end = centers[i - 1], strokeWidth = cell * .51f, cap = StrokeCap.Round)
        }
    }
    for (i in centers.lastIndex downTo 1) {
        val center = centers[i]
        val color = segmentColor(i, colors)
        val scale = if (i == centers.lastIndex) .69f else if (i == centers.lastIndex - 1) .86f else 1f
        rotate(segmentDirection(centers, i, cell), pivot = center) {
            fun x(dx: Float) = center.x + dx * cell * scale
            fun y(dy: Float) = center.y + dy * cell * scale
            val rim = Path().apply {
                moveTo(x(-.43f), y(0f)); lineTo(x(-.23f), y(-.37f))
                lineTo(x(.21f), y(-.37f)); lineTo(x(.44f), y(0f))
                lineTo(x(.21f), y(.37f)); lineTo(x(-.23f), y(.37f)); close()
            }
            drawPath(rim, Color.White.copy(alpha = .36f))
            val facet = Path().apply {
                moveTo(x(-.37f), y(0f)); lineTo(x(-.19f), y(-.32f))
                lineTo(x(.18f), y(-.32f)); lineTo(x(.38f), y(0f))
                lineTo(x(.18f), y(.32f)); lineTo(x(-.19f), y(.32f)); close()
            }
            drawPath(facet, color)
            val shine = Path().apply {
                moveTo(x(-.37f), y(0f)); lineTo(x(-.19f), y(-.32f))
                lineTo(x(.18f), y(-.32f)); lineTo(x(.04f), y(.04f)); close()
            }
            drawPath(shine, Color.White.copy(alpha = .27f))
            val shadow = Path().apply {
                moveTo(x(.04f), y(.04f)); lineTo(x(.38f), y(0f))
                lineTo(x(.18f), y(.32f)); lineTo(x(-.19f), y(.32f)); close()
            }
            drawPath(shadow, Color.Black.copy(alpha = .18f))
            drawLine(Color.White.copy(alpha = .49f), Offset(x(-.19f), y(-.32f)),
                Offset(x(.18f), y(-.32f)), strokeWidth = max(1f, cell * .036f))
            drawLine(Color.White.copy(alpha = .21f), Offset(x(.04f), y(.04f)),
                Offset(x(.18f), y(.32f)), strokeWidth = max(1f, cell * .033f))
        }
    }
}
