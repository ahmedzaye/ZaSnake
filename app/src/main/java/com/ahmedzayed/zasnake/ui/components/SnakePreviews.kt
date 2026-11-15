package com.ahmedzayed.zasnake.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ahmedzayed.zasnake.game.Direction
import com.ahmedzayed.zasnake.game.SnakeSkin
import com.ahmedzayed.zasnake.game.SnakeHeadStyle
import com.ahmedzayed.zasnake.game.SnakeBodyType

internal fun snakeSkinColors(skin: SnakeSkin): List<Color> = when (skin) {
    SnakeSkin.GREEN -> listOf(Color(0xFF00FF88), Color(0xFF00C96B))
    SnakeSkin.PURPLE -> listOf(Color(0xFFD28BFF), Color(0xFF9B55E8))
    SnakeSkin.YELLOW -> listOf(Color(0xFFFFE600), Color(0xFFFFB800))
    SnakeSkin.EGYPT -> listOf(Color(0xFFFFD166), Color(0xFFD92738), Color(0xFFF6E9DC), Color(0xFF17191F))
    SnakeSkin.UAE -> listOf(Color(0xFFCE2B40), Color(0xFF06966D), Color(0xFFFAF5EB), Color(0xFF142B31))
    SnakeSkin.JORDAN -> listOf(Color(0xFFDB2745), Color(0xFF172A31), Color(0xFFFBF2E6), Color(0xFF167E57))
    SnakeSkin.PALESTINE -> listOf(Color(0xFF17282E), Color(0xFF199169), Color(0xFFFCF6E8), Color(0xFFDA3851))
    SnakeSkin.KUWAIT -> listOf(Color(0xFF172528), Color(0xFF2BA46B), Color(0xFFFFF2DD), Color(0xFFCE3C47))
    SnakeSkin.SAUDI -> listOf(Color(0xFF066D45), Color(0xFF0B9861), Color(0xFFEDF8EF), Color(0xFFC5A55A))
    SnakeSkin.CYBER -> listOf(Color(0xFF00E5FF), Color(0xFF7C4DFF))
    SnakeSkin.ICE -> listOf(Color(0xFFB3E5FC), Color(0xFF0288D1))
    SnakeSkin.FIRE -> listOf(Color(0xFFFF7043), Color(0xFFD50000))
}

/** Zoomed-in but real head renderer: accents and proportions match the actual game board. */
@Composable
internal fun HeadStylePreview(head: SnakeHeadStyle, skin: SnakeSkin, modifier: Modifier = Modifier,
    bodyType: SnakeBodyType = SnakeBodyType.SEGMENTED) {
    val colors = snakeSkinColors(skin)
    Canvas(modifier) {
        drawRect(Color(0xFF07131B))
        val unit = minOf(size.height * .65f, size.width / 3.45f)
        val y = size.height * .5f
        val headX = size.width * .72f
        if (bodyType != SnakeBodyType.SEGMENTED) {
            drawSnakeBodyByType(bodyType, listOf(Offset(headX, y), Offset(headX - unit * .78f, y),
                Offset(headX - unit * 1.56f, y), Offset(headX - unit * 2.34f, y)), unit, colors)
        } else {
            for (i in 2 downTo 1) {
                val x = headX - unit * i * .78f
                val color = colors[if (colors.size == 2) 1 else (i % (colors.size - 1)) + 1]
                drawRoundRect(color.copy(alpha = .90f),
                    Offset(x - unit * .37f, y - unit * .37f), Size(unit * .74f, unit * .74f),
                    CornerRadius(unit * .22f))
            }
        }
        drawStyledSnakeHead(Offset(headX, y), unit, colors.first(), head, Direction.RIGHT)
    }
}

@Composable
internal fun SkinPreview(skin: SnakeSkin, modifier: Modifier = Modifier,
    headStyle: SnakeHeadStyle = SnakeHeadStyle.CLASSIC,
    bodyType: SnakeBodyType = SnakeBodyType.SEGMENTED) {
    val colors = snakeSkinColors(skin)
    Canvas(modifier = modifier) {
        drawRect(Color(0xFF07131B))
        val step = 16.dp.toPx()
        var x = 0f
        while (x < size.width) {
            drawLine(Color(0xFF17303B), Offset(x, 0f), Offset(x, size.height), 0.6f)
            x += step
        }
        var y = 0f
        while (y < size.height) {
            drawLine(Color(0xFF17303B), Offset(0f, y), Offset(size.width, y), 0.6f)
            y += step
        }
        val unit = minOf(size.width / 7.2f, size.height / 3.2f)
        val segment = unit * 0.82f
        val thickness = unit * 0.78f
        val centerY = size.height * 0.53f
        val centers = listOf(
            Offset(size.width * 0.18f, centerY + unit * 0.85f),
            Offset(size.width * 0.18f + unit, centerY + unit * 0.85f),
            Offset(size.width * 0.18f + unit * 2f, centerY + unit * 0.85f),
            Offset(size.width * 0.18f + unit * 2f, centerY - unit * 0.05f),
            Offset(size.width * 0.18f + unit * 3f, centerY - unit * 0.05f),
            Offset(size.width * 0.18f + unit * 4f, centerY - unit * 0.05f)
        )
        if (bodyType != SnakeBodyType.SEGMENTED) {
            drawSnakeBodyByType(bodyType, centers.reversed(), unit, colors)
            drawStyledSnakeHead(centers.last(), unit, colors.first(), headStyle, Direction.RIGHT)
        } else centers.forEachIndexed { index, center ->
            val color = if (index == centers.lastIndex) colors.first() else colors[(index % (colors.size - 1)) + 1]
            drawRoundRect(color.copy(alpha = 0.20f), Offset(center.x - segment * 0.65f, center.y - thickness * 0.65f), Size(segment * 1.3f, thickness * 1.3f), CornerRadius(thickness * 0.38f))
            if (index == centers.lastIndex) {
                drawStyledSnakeHead(center, unit, color, headStyle, Direction.RIGHT)
            } else {
                drawRoundRect(color, Offset(center.x - segment / 2f, center.y - thickness / 2f),
                    Size(segment, thickness), CornerRadius(thickness * 0.32f))
            }
        }
    }
}

@Composable
internal fun BodyStylePreview(body: SnakeBodyType, skin: SnakeSkin, headStyle: SnakeHeadStyle,
    modifier: Modifier = Modifier) {
    SkinPreview(skin, modifier, headStyle, body)
}
