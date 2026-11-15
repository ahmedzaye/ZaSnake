package com.ahmedzayed.zasnake.ui

import com.ahmedzayed.zasnake.ads.RewardedAds
import com.ahmedzayed.zasnake.ads.RewardPlacement
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import com.ahmedzayed.zasnake.GameViewModel
import com.ahmedzayed.zasnake.game.AppUiState
import com.ahmedzayed.zasnake.game.GameMode

@Composable
internal fun PauseCard(state: AppUiState, viewModel: GameViewModel) {
    val lang = state.settings.language
    val ar = lang == "ar"
    val engine = viewModel.engine
    val detailLabel = if (engine.mode == GameMode.TIME_ATTACK) tx(lang, "time")
        else tx(lang, "level")
    val detailValue = if (engine.mode == GameMode.TIME_ATTACK)
        "${(engine.timeRemainingMs + 999L) / 1000L}s"
        else engine.level.toString()
    Column(
        Modifier.fillMaxWidth(0.90f).widthIn(max = 360.dp)
            .clip(RoundedCornerShape(25.dp)).background(Playform.Cream)
            .border(1.dp, Playform.Border, RoundedCornerShape(25.dp))
            .padding(19.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(11.dp)
    ) {
        Box(Modifier.size(58.dp).clip(RoundedCornerShape(19.dp))
            .background(Playform.PaleLavender), contentAlignment = Alignment.Center) {
            androidx.compose.material3.Icon(Icons.Rounded.Pause, tx(lang, "pause_title"),
                tint = Playform.Ink, modifier = Modifier.size(36.dp))
        }
        PlayformTag("TAKE A BREAK", color = Playform.CoralText)
        Text(tx(lang, "pause_title"), color = Playform.Ink, fontSize = 23.sp,
            lineHeight = 30.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
        Text(if (ar) "تقدر تكمل جولتك من نفس المكان" else "Pick up right where you left off",
            color = Playform.TextSecondary, fontSize = 13.sp, textAlign = TextAlign.Center)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PlayformCounterTile(tx(lang, "score"), engine.score.toString(),
                Modifier.weight(1f))
            PlayformCounterTile(detailLabel, detailValue, Modifier.weight(1f))
        }
        PrimaryActionButton(tx(lang, "continue"), Icons.Rounded.PlayArrow, viewModel::resumeFromPause)
        OutlinedActionButton(tx(lang, "restart"), Icons.Rounded.Replay, viewModel::requestRestart)
        OutlinedActionButton(tx(lang, "main_menu"), Icons.Rounded.Home, viewModel::goHomeKeepingRound)
        Text(tx(lang, "paused_save"), color = Playform.TextSecondary, fontSize = 12.sp,
            textAlign = TextAlign.Center)
    }
}

@Composable
internal fun GameOverCard(state: AppUiState, viewModel: GameViewModel, rewardedAds: RewardedAds?) {
    val lang = state.settings.language
    val title = if (viewModel.engine.gameOverReason == "time") tx(lang, "time_up") else tx(lang, "game_over")
    val engine = viewModel.engine
    val motionEnabled = LocalVisualMotionEnabled.current
    val finalScore = viewModel.engine.score
    val scoreCount = remember(viewModel.engine.sessionId) { Animatable(0f) }
    LaunchedEffect(viewModel.engine.sessionId, finalScore, motionEnabled) {
        if (motionEnabled) scoreCount.animateTo(finalScore.toFloat(), tween(620))
        else scoreCount.snapTo(finalScore.toFloat())
    }
    BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
    Column(Modifier.fillMaxWidth(0.90f).widthIn(max = 370.dp)
        .heightIn(max = (maxHeight - 12.dp).coerceAtLeast(1.dp))
        .clip(RoundedCornerShape(25.dp)).background(Playform.Cream)
        .border(1.dp, Playform.Border, RoundedCornerShape(25.dp))
        .verticalScroll(rememberScrollState()).padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.size(58.dp).clip(RoundedCornerShape(18.dp))
            .background(Playform.Gold), contentAlignment = Alignment.Center) {
            androidx.compose.material3.Icon(Icons.Rounded.EmojiEvents, null,
                tint = Playform.Ink, modifier = Modifier.size(34.dp))
        }
        PlayformTag(if (engine.mode == GameMode.TIME_ATTACK) "TIME ATTACK" else "RUN COMPLETE",
            color = Playform.CoralText)
        Text(title, color = Playform.Ink, fontSize = 23.sp, fontWeight = FontWeight.Bold)
        if (state.newRecordThisRound) {
            Text("✦ ${tx(lang, "new_record_visual")} ✦", color = Playform.GoldText,
                fontSize = 14.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        }
        Text(tx(lang, "your_score"), color = Muted, fontSize = 13.sp)
        Text(scoreCount.value.roundToInt().toString(), color = Playform.CoralText,
            fontSize = 42.sp, fontWeight = FontWeight.Black)
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(LocalZaPalette.current.statsTile).padding(13.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(tx(lang, "best_score"), color = SecondaryText, fontSize = 12.sp)
            Text((if (engine.mode == GameMode.TIME_ATTACK) state.stats.bestTimeScore else state.stats.bestScore).toString(), color = Gold, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }
        if (engine.mode == GameMode.CLASSIC) {
            val earned = if (engine.pendingCollision) engine.score else state.stats.coinRewards[engine.sessionId] ?: 0
            val bonus = state.stats.doubledRoundCoins[engine.sessionId] ?: 0
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(LocalZaPalette.current.statsTile).padding(13.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(if (engine.pendingCollision) {
                    if (lang == "ar") "🪙 عملات عند الإنهاء" else "🪙 Coins if you end"
                } else "🪙  ${tx(lang, "coins_earned")}", color = SecondaryText, fontSize = 12.sp)
                Text("+${earned + bonus}", color = Gold, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        }
        if (engine.canRevive) {
            Text(if (lang == "ar") "فرصة واحدة للإكمال مع حماية من الاصطدام لمدة 5 ثوانٍ"
                else "One revive: 5 seconds of collision protection", color = Neon,
                fontSize = 12.sp, textAlign = TextAlign.Center)
            PrimaryActionButton(if (rewardedAds == null) {
                if (lang == "ar") "▶ إكمال مجاني" else "▶ Free revive"
            } else if (lang == "ar") "▶ شاهد إعلانًا للإكمال" else "▶ Watch ad to revive",
                Icons.Rounded.PlayArrow, onClick = {
                    val session = engine.sessionId
                    if (rewardedAds == null) viewModel.reviveAfterReward(session)
                    else rewardedAds.show(RewardPlacement.REVIVE, onEarned = { viewModel.reviveAfterReward(session) },
                        onUnavailable = viewModel::rewardAdUnavailable)
                })
            OutlinedActionButton(if (lang == "ar") "إنهاء الجولة واستلام العملات" else "End round and collect coins",
                Icons.Rounded.CheckCircle, viewModel::declineRevive)
        } else if (engine.mode == GameMode.CLASSIC &&
            (state.stats.coinRewards[engine.sessionId] ?: 0) > 0) {
            if (engine.sessionId !in state.stats.doubledRoundCoins) {
                PrimaryActionButton(if (rewardedAds == null) {
                    if (lang == "ar") "🪙 مضاعفة مجانية ×2" else "🪙 Free double coins ×2"
                } else if (lang == "ar") "🎬 إعلان لمضاعفة العملات ×2" else "🎬 Watch ad: double coins ×2",
                    Icons.Rounded.Star, onClick = {
                        val session = engine.sessionId
                        if (rewardedAds == null) viewModel.doubleCoinsAfterReward(session)
                        else rewardedAds.show(RewardPlacement.DOUBLE_COINS, onEarned = { viewModel.doubleCoinsAfterReward(session) },
                            onUnavailable = viewModel::rewardAdUnavailable)
                    })
            } else {
                Text(if (lang == "ar") "✓ تمت مضاعفة عملات الجولة" else "✓ Round coins doubled",
                    color = Neon, fontSize = 12.sp)
            }
        }
        val missingAd = (engine.canRevive && rewardedAds?.readyFor(RewardPlacement.REVIVE) == false) ||
            (engine.mode == GameMode.CLASSIC && !engine.pendingCollision &&
                engine.sessionId !in state.stats.doubledRoundCoins &&
                rewardedAds?.readyFor(RewardPlacement.DOUBLE_COINS) == false)
        if (missingAd) {
            Text(if (lang == "ar") "جاري تجهيز الإعلان التجريبي، وقد لا يتوفر دون إنترنت"
                else "Loading test ad; internet connection required", color = Muted, fontSize = 10.sp,
                textAlign = TextAlign.Center)
        }
        PrimaryActionButton(tx(lang, "restart"), Icons.Rounded.Replay, viewModel::replayAfterGameOver)
        OutlinedActionButton(tx(lang, "main_menu"), Icons.Rounded.Home, viewModel::goHomeAfterGameOver)
    }
    }
}

@Composable
