package com.ahmedzayed.zasnake.ui

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import com.ahmedzayed.zasnake.game.Direction
import com.ahmedzayed.zasnake.game.SnakeHeadStyle

/** The shop and live board use the same drawing code. Shapes inherit the equipped skin's head color. */
internal fun DrawScope.drawStyledSnakeHead(
    center: Offset,
    cell: Float,
    color: Color,
    style: SnakeHeadStyle,
    direction: Direction,
    silhouetteOnly: Boolean = false
) {
    val degrees = when (direction) {
        Direction.RIGHT -> 0f
        Direction.DOWN -> 90f
        Direction.LEFT -> 180f
        Direction.UP -> 270f
    }
    fun pt(x: Float, y: Float): Offset = Offset(center.x + x * cell, center.y + y * cell)
    fun polygon(vararg xy: Float, fill: Color) {
        val path = Path().apply {
            moveTo(center.x + xy[0] * cell, center.y + xy[1] * cell)
            for (i in 2 until xy.size step 2) lineTo(center.x + xy[i] * cell, center.y + xy[i + 1] * cell)
            close()
        }
        drawPath(path, fill)
    }
    rotate(degrees, pivot = center) {
        if (silhouetteOnly) {
            when (style) {
                SnakeHeadStyle.CLASSIC -> drawRoundRect(color, pt(-.41f, -.41f), Size(cell * .82f, cell * .82f), CornerRadius(cell * .24f))
                SnakeHeadStyle.SHARP -> polygon(-.43f, -.39f, .15f, -.39f, .47f, -.15f,
                    .47f, .15f, .15f, .39f, -.43f, .39f, fill = color)
                SnakeHeadStyle.CUTE -> drawCircle(color, radius = cell * .45f, center = center)
                SnakeHeadStyle.ROYAL -> {
                    drawRoundRect(color, pt(-.41f, -.34f), Size(cell * .82f, cell * .74f), CornerRadius(cell * .24f))
                    polygon(-.36f, -.30f, -.43f, -.51f, -.12f, -.41f, .03f, -.54f,
                        .19f, -.40f, .41f, -.50f, .35f, -.27f, fill = color)
                }
                SnakeHeadStyle.CYBER -> polygon(-.41f, -.28f, -.24f, -.43f, .22f, -.43f,
                    .45f, -.23f, .45f, .23f, .22f, .43f, -.24f, .43f, -.41f, .28f, fill = color)
            }
            return@rotate
        }
        when (style) {
            SnakeHeadStyle.CLASSIC -> {
                drawRoundRect(color, pt(-.40f, -.40f), Size(cell * .80f, cell * .80f), CornerRadius(cell * .23f))
            }
            SnakeHeadStyle.SHARP -> {
                polygon(-.42f, -.37f, .14f, -.37f, .45f, -.14f, .45f, .14f,
                    .14f, .37f, -.42f, .37f, fill = color)
                drawLine(Color.White.copy(alpha = .22f), pt(-.27f, -.31f), pt(.20f, -.31f), cell * .035f)
            }
            SnakeHeadStyle.CUTE -> {
                drawCircle(color, radius = cell * .43f, center = center)
                drawCircle(Color.White.copy(alpha = .15f), radius = cell * .24f, center = pt(-.12f, -.15f))
                drawCircle(Color(0xFFFFA9BB).copy(alpha = .7f), radius = cell * .065f, center = pt(.21f, .27f))
            }
            SnakeHeadStyle.ROYAL -> {
                drawRoundRect(color, pt(-.39f, -.31f), Size(cell * .78f, cell * .69f), CornerRadius(cell * .24f))
                polygon(-.33f, -.27f, -.40f, -.49f, -.12f, -.38f, .03f, -.52f,
                    .19f, -.37f, .39f, -.48f, .33f, -.24f, fill = Color(0xFFFFD166))
                drawLine(Color(0xFFFFE9A6), pt(-.29f, -.25f), pt(.29f, -.25f), cell * .04f)
                drawCircle(Color(0xFFFF6D91), cell * .044f, pt(.02f, -.38f))
            }
            SnakeHeadStyle.CYBER -> {
                polygon(-.39f, -.26f, -.22f, -.41f, .20f, -.41f, .42f, -.22f,
                    .42f, .22f, .20f, .41f, -.22f, .41f, -.39f, .26f, fill = color)
                drawRoundRect(Color(0xFF09252D), pt(.00f, -.29f), Size(cell * .34f, cell * .58f), CornerRadius(cell * .075f))
                drawLine(Color(0xFF8CFFF5), pt(.19f, -.23f), pt(.19f, .23f), cell * .045f)
                drawLine(Color.White.copy(alpha = .56f), pt(-.29f, -.33f), pt(.04f, -.33f), cell * .03f)
            }
        }
        if (style == SnakeHeadStyle.CYBER) {
            drawLine(Color(0xFF8CFFF5), pt(.28f, -.20f), pt(.28f, -.055f), cell * .045f)
            drawLine(Color(0xFF8CFFF5), pt(.28f, .055f), pt(.28f, .20f), cell * .045f)
        } else {
            val eyeX = if (style == SnakeHeadStyle.CUTE) .095f else .10f
            val y = if (style == SnakeHeadStyle.ROYAL) .20f else .195f
            val radius = if (style == SnakeHeadStyle.CUTE) .106f else .079f
            for (eyeY in listOf(-y, y)) {
                drawCircle(Color.White, cell * radius, pt(eyeX, eyeY))
                drawCircle(Color(0xFF09252D), cell * radius * .43f, pt(eyeX + .035f, eyeY))
            }
        }
    }
}
