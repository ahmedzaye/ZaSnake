package com.ahmedzayed.zasnake.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ahmedzayed.zasnake.game.ACHIEVEMENTS
import com.ahmedzayed.zasnake.game.AppUiState
import kotlin.math.min

@Composable
internal fun StatisticsScreen(
    state: AppUiState,
    onBack: () -> Unit,
    onClaimReward: (String) -> Boolean,
    achievementsOnly: Boolean = state.statisticsOpenAchievements
) {
    val lang = state.settings.language
    val ar = lang == "ar"
    val stats = state.stats
    var filter by remember { mutableStateOf("all") }
    val unlockedCount = ACHIEVEMENTS.count {
        it.id in stats.unlockedAchievements || it.progress(stats) >= it.target
    }
    val availableCoins = ACHIEVEMENTS.filter {
        (it.id in stats.unlockedAchievements || it.progress(stats) >= it.target) &&
            it.id !in stats.claimedAchievementRewards
    }.sumOf { it.rewardCoins }
    LazyColumn(Modifier.fillMaxSize().background(Playform.Cream)
        .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(11.dp)) {
        item {
            ScreenHeader(tx(lang, if (achievementsOnly) "achievements" else "statistics"), onBack)
        }
        if (!achievementsOnly) {
            item {
                PlayformDarkCard {
                    PlayformTag("YOUR NUMBERS")
                    Text(if (ar) "رحلتك بالأرقام" else "Your Journey in Numbers",
                        color = Playform.White, fontSize = 23.sp,
                        fontWeight = FontWeight.Black)
                    Text(if (ar) "إحصائياتك المحفوظة عبر الجولات"
                        else "Your saved game progress",
                        color = Playform.LightOnDark, fontSize = 13.sp)
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()) {
                    PlayformCounterTile(tx(lang, "best_score"),
                        stats.bestScore.toString(), Modifier.weight(1f), Playform.Gold)
                    PlayformCounterTile(tx(lang, "best_time"),
                        stats.bestTimeScore.toString(), Modifier.weight(1f), Playform.Lavender)
                }
            }
            item {
                val values = listOf(
                    tx(lang, "games") to stats.games.toString(),
                    tx(lang, "food_eaten") to stats.food.toString(),
                    tx(lang, "play_time") to formatPlayTime(stats.playMs, lang),
                    tx(lang, "golden_apples") to stats.golden.toString(),
                    tx(lang, "scissors") to stats.scissors.toString(),
                    tx(lang, "longest_snake") to stats.maxLength.toString()
                )
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    values.chunked(2).forEach { pair ->
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            pair.forEach { (label, value) ->
                                PlayformCounterTile(label, value, Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        } else {
            item {
                PlayformDarkCard {
                    PlayformTag("ACHIEVEMENT GALLERY")
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("$unlockedCount / ${ACHIEVEMENTS.size}",
                                color = Playform.White, fontWeight = FontWeight.Black,
                                fontSize = 32.sp)
                            Text(if (ar) "إنجازات مفتوحة" else "Achievements unlocked",
                                color = Playform.LightOnDark, fontSize = 13.sp)
                        }
                        Text("🏆", fontSize = 50.sp)
                    }
                    PlayformProgressBar(unlockedCount.toFloat() / ACHIEVEMENTS.size.toFloat(),
                        color = Playform.Gold)
                    Text(if (ar) "$availableCoins عملة جاهزة للاستلام"
                        else "$availableCoins coins ready to claim",
                        color = Playform.LightOnDark, fontSize = 12.sp)
                    Text(if (ar) "إجمالي المكافآت: 1,225 عملة"
                        else "Total achievement rewards: 1,225 coins",
                        color = Playform.LightOnDark, fontSize = 12.sp)
                }
            }
            item {
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(
                        "all" to (if (ar) "الكل" else "All"),
                        "ready" to (if (ar) "جاهزة" else "Ready"),
                        "progress" to (if (ar) "قيد التقدم" else "In progress"),
                        "claimed" to (if (ar) "مستلمة" else "Claimed")
                    ).forEach { (key, label) ->
                        ChoiceChip(label, filter == key) { filter = key }
                    }
                }
            }
            val visible = ACHIEVEMENTS.filter { achievement ->
                val unlocked = achievement.id in stats.unlockedAchievements ||
                    achievement.progress(stats) >= achievement.target
                val claimed = achievement.id in stats.claimedAchievementRewards
                when (filter) {
                    "ready" -> unlocked && !claimed
                    "progress" -> !unlocked
                    "claimed" -> claimed
                    else -> true
                }
            }
            items(visible, key = { it.id }) { achievement ->
                val unlocked = achievement.id in stats.unlockedAchievements ||
                    achievement.progress(stats) >= achievement.target
                val claimed = achievement.id in stats.claimedAchievementRewards
                val progress = achievement.progress(stats).coerceAtLeast(0L)
                val shown = if (unlocked) maxOf(progress, achievement.target) else progress
                val ratio = if (achievement.target <= 0) 1f
                    else (shown.toFloat() / achievement.target.toFloat()).coerceIn(0f, 1f)
                val progressText = if (achievement.id == "dedicated" ||
                    achievement.id == "play60min") {
                    "${formatPlayTime(shown.coerceAtMost(achievement.target), lang)} / ${formatPlayTime(achievement.target, lang)}"
                } else "${shown.coerceAtMost(achievement.target)} / ${achievement.target}"
                PlayformCard(background = if (unlocked) Color(0xFFFFFDF6) else Playform.White) {
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(Modifier.size(49.dp).clip(RoundedCornerShape(15.dp))
                            .background(if (unlocked) Playform.Gold else Playform.PaleLavender),
                            contentAlignment = Alignment.Center) {
                            Text(if (unlocked) achievement.icon else "🔒", fontSize = 23.sp)
                        }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(if (ar) achievement.arDescription else achievement.enDescription,
                                color = Playform.Ink, fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold, lineHeight = 20.sp)
                            PlayformProgressBar(ratio,
                                color = if (unlocked) Playform.GoldText else Playform.Lavender)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(7.dp)) {
                            Text("🪙 ${achievement.rewardCoins}", color = Playform.GoldText,
                                fontWeight = FontWeight.Black, fontSize = 12.sp)
                            when {
                                claimed -> androidx.compose.material3.Icon(
                                    Icons.Rounded.CheckCircle,
                                    contentDescription = if (ar) "تم استلام المكافأة" else "Reward claimed",
                                    tint = Color(0xFF286C5B),
                                    modifier = Modifier.size(24.dp)
                                )
                                unlocked -> Button(
                                    onClick = { onClaimReward(achievement.id) },
                                    modifier = Modifier.heightIn(min = 44.dp),
                                    contentPadding = PaddingValues(horizontal = 11.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Playform.Coral, contentColor = Playform.Ink)
                                ) { Text(tx(lang, "claim_short"), fontSize = 12.sp) }
                                else -> androidx.compose.runtime.CompositionLocalProvider(
                                    LocalLayoutDirection provides LayoutDirection.Ltr
                                ) {
                                    Text(progressText, color = Playform.TextSecondary, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
            if (visible.isEmpty()) item {
                PlayformCard {
                    Text(if (ar) "مافيش إنجازات في التصنيف ده حاليًا"
                        else "No achievements in this category yet",
                        color = Playform.TextSecondary, fontSize = 14.sp)
                }
            }
        }
        item { Spacer(Modifier.height(8.dp)) }
    }
}
