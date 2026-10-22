package com.ahmedzayed.zasnake.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

/**
 * Draw the cosmetic smooth body as a continuous tapered tube following a smoothed centerline.
 * Input is head-to-tail. Geometry does not affect collision, scoring or game speed.
 * Skips teleports over a wrap boundary so no diagonal line crosses the playfield.
 */
internal fun DrawScope.drawSmoothSnakeBody(
    centers: List<Offset>, cell: Float, colors: List<Color>
) {
    if (centers.size < 2 || cell <= 0f || colors.isEmpty()) return

    val chains = splitContinuousChains(centers, maxJump = cell * 1.65f)
    if (chains.isEmpty()) return

    val neckRadius = cell * 0.34f
    val tailRadius = cell * 0.085f
    val spacing = max(1.5f, cell * 0.08f)
    val extraTail = cell * 0.18f

    for (chain in chains) {
        if (chain.size < 2) continue
        val smoothed = chaikinSmooth(chain, iterations = 3)
        if (smoothed.size < 2) continue

        val cumulative = cumulativeDistances(smoothed)
        val totalLength = cumulative.last()
        if (totalLength <= 0.01f) continue

        val segmentCount = max(1, chain.lastIndex)
        var distance = totalLength

        // Draw from tail to neck so the independent head can overlap the front cleanly.
        while (distance >= 0f) {
            val point = pointAtDistance(smoothed, cumulative, distance)
            val positionFromHead = (distance / totalLength).coerceIn(0f, 1f)
            val thicknessFromHead = 1f - positionFromHead
            val eased = thicknessFromHead * thicknessFromHead * (3f - 2f * thicknessFromHead) // smoothstep
            val radius = lerp(tailRadius, neckRadius, eased)
            val logicalIndex = (positionFromHead * segmentCount).toInt().coerceIn(0, segmentCount)
            drawCircle(
                color = bodyColor(logicalIndex, colors).copy(alpha = 0.985f),
                radius = radius,
                center = point
            )
            distance -= spacing
        }

        // Extend the very tail slightly to create a smoother, thinner finish.
        val tailEnd = smoothed.last()
        val beforeTail = smoothed[smoothed.lastIndex - 1]
        val tailDir = normalize(tailEnd - beforeTail)
        val tailLength = extraTail
        var tailStep = spacing
        while (tailStep <= tailLength) {
            val t = 1f - (tailStep / tailLength).coerceIn(0f, 1f)
            val radius = max(0.8f, tailRadius * t)
            drawCircle(
                color = bodyColor(segmentCount, colors).copy(alpha = 0.985f),
                radius = radius,
                center = tailEnd + tailDir * tailStep
            )
            tailStep += spacing * 0.85f
        }
    }
    // Head rendering overlaps the neck; do not draw over the independent head style.
}

private fun bodyColor(index: Int, colors: List<Color>): Color = when {
    colors.isEmpty() -> Color.White
    colors.size == 1 -> colors.first()
    colors.size == 2 -> colors[1]
    else -> colors[(index % (colors.size - 1)) + 1]
}

private fun splitContinuousChains(points: List<Offset>, maxJump: Float): List<List<Offset>> {
    if (points.isEmpty()) return emptyList()
    val chains = mutableListOf<MutableList<Offset>>()
    var current = mutableListOf(points.first())
    for (i in 1 until points.size) {
        val previous = points[i - 1]
        val point = points[i]
        if (distance(previous, point) <= maxJump) {
            current += point
        } else {
            if (current.size >= 2) chains += current
            current = mutableListOf(point)
        }
    }
    if (current.size >= 2) chains += current
    return chains
}

private fun chaikinSmooth(points: List<Offset>, iterations: Int): List<Offset> {
    var current = points
    repeat(iterations) {
        if (current.size < 3) return@repeat
        val next = ArrayList<Offset>(current.size * 2)
        next += current.first()
        for (i in 0 until current.lastIndex) {
            val p0 = current[i]
            val p1 = current[i + 1]
            val q = Offset(
                x = p0.x * 0.75f + p1.x * 0.25f,
                y = p0.y * 0.75f + p1.y * 0.25f
            )
            val r = Offset(
                x = p0.x * 0.25f + p1.x * 0.75f,
                y = p0.y * 0.25f + p1.y * 0.75f
            )
            next += q
            next += r
        }
        next += current.last()
        current = next
    }
    return current
}

private fun cumulativeDistances(points: List<Offset>): List<Float> {
    val cumulative = ArrayList<Float>(points.size)
    var total = 0f
    cumulative += 0f
    for (i in 1 until points.size) {
        total += distance(points[i - 1], points[i])
        cumulative += total
    }
    return cumulative
}

private fun pointAtDistance(points: List<Offset>, cumulative: List<Float>, distance: Float): Offset {
    if (distance <= 0f) return points.first()
    val total = cumulative.last()
    if (distance >= total) return points.last()
    var low = 0
    var high = cumulative.lastIndex
    while (low <= high) {
        val mid = (low + high) ushr 1
        when {
            cumulative[mid] < distance -> low = mid + 1
            cumulative[mid] > distance -> high = mid - 1
            else -> return points[mid]
        }
    }
    val upperIndex = low.coerceIn(1, cumulative.lastIndex)
    val lowerIndex = upperIndex - 1
    val start = points[lowerIndex]
    val end = points[upperIndex]
    val segmentLength = cumulative[upperIndex] - cumulative[lowerIndex]
    if (segmentLength <= 0.001f) return start
    val t = ((distance - cumulative[lowerIndex]) / segmentLength).coerceIn(0f, 1f)
    return Offset(
        x = lerp(start.x, end.x, t),
        y = lerp(start.y, end.y, t)
    )
}

private fun distance(a: Offset, b: Offset): Float = hypot(b.x - a.x, b.y - a.y)

private fun normalize(vector: Offset): Offset {
    val length = hypot(vector.x, vector.y)
    return if (length <= 0.0001f) Offset(0f, 0f) else Offset(vector.x / length, vector.y / length)
}

private operator fun Offset.minus(other: Offset): Offset = Offset(x - other.x, y - other.y)
private operator fun Offset.plus(other: Offset): Offset = Offset(x + other.x, y + other.y)
private operator fun Offset.times(scale: Float): Offset = Offset(x * scale, y * scale)

private fun lerp(start: Float, end: Float, t: Float): Float = start + (end - start) * t
