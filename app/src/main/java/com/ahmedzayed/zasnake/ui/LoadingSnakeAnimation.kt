package com.ahmedzayed.zasnake.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.sin

/**
 * Displays a moving snake only while the existing app state is not yet ready.
 * No artificial delay is introduced: navigation proceeds as soon as state.ready becomes true.
 */
@Composable
internal fun SnakeLoadingScreen(
    background: Color,
    snakeColor: Color,
    motionEnabled: Boolean
) {
    // Respect the system's "Remove animations" / animator-duration accessibility setting.
    // When animations are disabled we draw a static snake instead of running a loop.
    val travel = if (motionEnabled) {
        val transition = rememberInfiniteTransition(label = "snake-loading")
        val animatedTravel by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1600, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "snake-moves-left-to-right"
        )
        animatedTravel
    } else {
        0.55f
    }

    Box(
        modifier = Modifier.fillMaxSize().background(background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Za Snake",
                color = snakeColor,
                fontSize = 28.sp,
                fontWeight = FontWeight.Black
            )
            Spacer(Modifier.height(20.dp))
            Canvas(modifier = Modifier.fillMaxWidth().height(92.dp)) {
                val spacing = 15.dp.toPx()
                val radius = 13.dp.toPx()
                val bodyCount = 10
                val headX = radius + travel * (size.width + bodyCount * spacing + radius)
                val centerY = size.height / 2f
                val wave = travel * 6.2831855f
                val waveHeight = 5.dp.toPx()

                // Draw the tail first, so each section tucks behind the next one.
                for (segment in bodyCount downTo 1) {
                    val x = headX - segment * spacing
                    val y = centerY + sin(segment * 0.65f + wave) * waveHeight
                    val bodyRadius = (11f - segment * 0.52f).dp.toPx()
                    drawCircle(
                        color = snakeColor.copy(alpha = 0.78f + (bodyCount - segment) * 0.018f),
                        radius = bodyRadius,
                        center = Offset(x, y)
                    )
                }

                // Subtle halo and the snake's head.
                drawCircle(snakeColor.copy(alpha = 0.16f), radius * 1.6f, Offset(headX, centerY))
                drawCircle(snakeColor, radius, Offset(headX, centerY))

                val eye = Offset(headX + 4.dp.toPx(), centerY - 5.dp.toPx())
                drawCircle(Color.White, radius = 3.6.dp.toPx(), center = eye)
                drawCircle(Color(0xFF07150E), radius = 1.7.dp.toPx(), center = eye.copy(x = eye.x + 1.dp.toPx()))
                drawLine(
                    color = Color(0xFFFF6F91),
                    start = Offset(headX + radius * 0.85f, centerY + 2.dp.toPx()),
                    end = Offset(headX + radius + 7.dp.toPx(), centerY + 3.dp.toPx()),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }
    }
}
