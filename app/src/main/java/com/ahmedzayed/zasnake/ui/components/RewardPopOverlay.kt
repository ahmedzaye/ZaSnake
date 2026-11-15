package com.ahmedzayed.zasnake.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/** Only called after an idempotent claim has returned success. Purely presentational. */
@Composable
internal fun RewardPopOverlay(pop: RewardPop, balance: Long, language: String, onFinished: (Long) -> Unit, modifier: Modifier = Modifier) {
    val motionEnabled = LocalVisualMotionEnabled.current
    val progress = remember(pop.id) { Animatable(0f) }
    val offsetPx = with(LocalDensity.current) { 45.dp.toPx() }
    LaunchedEffect(pop.id, motionEnabled) {
        if (motionEnabled) progress.animateTo(1f, tween(850)) else delay(650L)
        onFinished(pop.id)
    }
    val t = progress.value
    val alpha = if (!motionEnabled) 1f else when {
        t < 0.12f -> (t / 0.12f).coerceIn(0f, 1f)
        t > 0.76f -> ((1f - t) / 0.24f).coerceIn(0f, 1f)
        else -> 1f
    }
    Box(modifier.fillMaxWidth().padding(top = 58.dp), contentAlignment = Alignment.TopCenter) {
        Box(Modifier.graphicsLayer {
            this.alpha = alpha
            translationY = -offsetPx * t
        }.clip(RoundedCornerShape(25.dp))
            .background(SelectedPanel).border(1.dp, Gold, RoundedCornerShape(25.dp))
            .padding(horizontal = 18.dp, vertical = 9.dp)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("+${pop.coins} 🪙", color = Playform.GoldText, fontWeight = FontWeight.Black, fontSize = 17.sp)
                Text("${tx(language, "coin_balance")}: $balance", color = Playform.Ink, fontSize = 12.sp)
            }
        }
    }
}
