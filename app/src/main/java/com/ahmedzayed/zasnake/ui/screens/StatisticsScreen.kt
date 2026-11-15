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
    // The old screen entry point is preserved for callers and saved navigation state.
    if (achievementsOnly) {
        AchievementsScreen(state, onBack, onClaimReward)
        return
    }
    val lang = state.settings.language
    val ar = lang == "ar"
    val stats = state.stats
    LazyColumn(Modifier.fillMaxSize().background(Playform.Cream)
        .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(11.dp)) {
        item { ScreenHeader(tx(lang, "statistics"), onBack) }
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
        item { Spacer(Modifier.height(8.dp)) }
    }
}
