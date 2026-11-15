package com.ahmedzayed.zasnake.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ahmedzayed.zasnake.game.DailyChallenge
import com.ahmedzayed.zasnake.game.DAILY_LOGIN_COIN_SEQUENCE
import com.ahmedzayed.zasnake.game.dailyProgress
import com.ahmedzayed.zasnake.game.nextDailyLoginDay
import com.ahmedzayed.zasnake.game.AppUiState
import kotlinx.coroutines.delay
import kotlin.math.min

@Composable
internal fun DailyRewardsScreen(
    state: AppUiState,
    onBack: () -> Unit,
    onClaimChallenge: (String) -> Boolean,
    onClaimLogin: () -> Boolean,
    onRefresh: () -> Unit
) {
    val lang = state.settings.language
    val ar = lang == "ar"
    val date = state.dailyDate
    val stats = state.stats
    val progress = stats.dailyProgress(date)
    val clockIsValid = date >= stats.latestDailyDate
    val loginClaimed = date in stats.claimedDailyLogins
    val nextDay = if (loginClaimed) stats.claimedDailyLogins[date] ?: 1
        else stats.nextDailyLoginDay(date)
    val currentCoins = DAILY_LOGIN_COIN_SEQUENCE[(nextDay - 1).coerceIn(0, 6)]
    // Same UTC rollover and reward eligibility as the original screen.
    LaunchedEffect(date) {
        while (true) {
            kotlinx.coroutines.delay(30_000)
            onRefresh()
        }
    }
    LazyColumn(
        Modifier.fillMaxSize().background(Playform.Cream)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(13.dp)
    ) {
        item { ScreenHeader(tx(lang, "daily_rewards"), onBack) }
        item {
            PlayformPageIntro(if (ar) "مكافآتك اليومية" else "Daily Rewards",
                if (ar) "كل يوم فرصة جديدة لتجمع مكافآت أكثر"
                else "A new chance to earn rewards every day")
        }
        item {
            PlayformDarkCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        PlayformTag("DAILY LOGIN")
                        Text(tx(lang, "daily_login"), color = Playform.White,
                            fontWeight = FontWeight.Black, fontSize = 20.sp)
                        Text(tx(lang, "daily_login_hint"),
                            color = Playform.LightOnDark, fontSize = 13.sp, lineHeight = 19.sp)
                    }
                    Text("🎁", fontSize = 46.sp)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    DAILY_LOGIN_COIN_SEQUENCE.forEachIndexed { index, coins ->
                        val day = index + 1
                        val current = day == nextDay
                        Column(Modifier.weight(1f).clip(RoundedCornerShape(10.dp))
                            .background(if (current) Playform.Gold else Color(0xFF45415E))
                            .padding(vertical = 10.dp, horizontal = 1.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Text("D$day", color = if (current) Playform.Ink else Playform.LightOnDark,
                                fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            Text("$coins", color = if (current) Playform.Ink else Playform.White,
                                fontWeight = FontWeight.Black, fontSize = 12.sp)
                        }
                    }
                }
                if (loginClaimed) {
                    PlayformStatusPill(tx(lang, "daily_claimed"),
                        color = Playform.Ink, background = Playform.Gold)
                } else {
                    Button(
                        onClick = { onClaimLogin() },
                        enabled = clockIsValid,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp),
                        shape = RoundedCornerShape(15.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Playform.Coral,
                            contentColor = Playform.Ink)
                    ) {
                        Text(if (ar) "استلم $currentCoins عملة" else "CLAIM $currentCoins COINS",
                            fontWeight = FontWeight.Black, fontSize = 15.sp)
                    }
                }
                Text(date, color = Playform.LightOnDark, fontSize = 12.sp)
            }
        }
        if (!clockIsValid) item {
            PlayformCard(background = Color(0xFFFFF0D2)) {
                Text(tx(lang, "daily_clock_warning"), color = Playform.Ink,
                    fontSize = 13.sp, lineHeight = 19.sp)
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    PlayformSectionTitle(tx(lang, "daily_challenges"),
                        if (ar) "أنجز التحديات واستلم مكافآتك" else "Complete missions to collect coins")
                }
                PlayformStatusPill("🪙 ${DailyChallenge.entries.sumOf { it.coins }}",
                    color = Playform.Ink, background = Playform.Gold)
            }
        }
        items(DailyChallenge.entries.toList(), key = { it.id }) { challenge ->
            val current = challenge.progress(progress).coerceAtLeast(0L)
            val finished = current >= challenge.target
            val claimed = challenge.claimId(date) in stats.claimedDailyChallenges
            val ratio = (current.toFloat() / challenge.target.toFloat()).coerceIn(0f, 1f)
            PlayformCard(background = if (finished) Color(0xFFFFFDF6) else Playform.White) {
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.size(46.dp).clip(RoundedCornerShape(14.dp))
                        .background(Playform.PaleCoral), contentAlignment = Alignment.Center) {
                        Text(challenge.icon, fontSize = 24.sp)
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(if (ar) challenge.arDescription else challenge.enDescription,
                            color = Playform.Ink, fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp, lineHeight = 20.sp)
                        PlayformProgressBar(ratio)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        Text("🪙 ${challenge.coins}", color = Playform.GoldText,
                            fontWeight = FontWeight.Black, fontSize = 12.sp)
                        when {
                            claimed -> androidx.compose.material3.Icon(
                                Icons.Rounded.CheckCircle,
                                contentDescription = if (ar) "تم استلام المكافأة" else "Reward claimed",
                                tint = Color(0xFF286C5B),
                                modifier = Modifier.size(24.dp)
                            )
                            finished -> Button(
                                onClick = { onClaimChallenge(challenge.id) },
                                enabled = clockIsValid,
                                modifier = Modifier.heightIn(min = 44.dp),
                                contentPadding = PaddingValues(horizontal = 11.dp),
                                shape = RoundedCornerShape(11.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Playform.Coral, contentColor = Playform.Ink)
                            ) { Text(tx(lang, "claim_short"), fontSize = 12.sp) }
                            else -> androidx.compose.runtime.CompositionLocalProvider(
                                LocalLayoutDirection provides LayoutDirection.Ltr
                            ) {
                                Text("${current.coerceAtMost(challenge.target)}/${challenge.target}",
                                    color = Playform.TextSecondary, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
        item {
            Text(tx(lang, "daily_reset"), color = Playform.TextSecondary,
                fontSize = 12.sp, modifier = Modifier.fillMaxWidth()
                    .padding(vertical = 8.dp), textAlign = TextAlign.Center)
        }
    }
}
