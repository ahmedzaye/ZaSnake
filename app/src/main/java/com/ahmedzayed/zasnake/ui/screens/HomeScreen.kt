package com.ahmedzayed.zasnake.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ahmedzayed.zasnake.R
import com.ahmedzayed.zasnake.game.ACHIEVEMENTS
import com.ahmedzayed.zasnake.game.AppUiState
import kotlin.math.min

@Composable
internal fun HomeScreen(
    state: AppUiState,
    onNewGame: () -> Unit,
    onResume: () -> Unit,
    onSettings: () -> Unit,
    onStats: () -> Unit,
    onShop: () -> Unit,
    onDaily: () -> Unit,
    onClaimAchievements: () -> Unit
) {
    val lang = state.settings.language
    val ar = lang == "ar"
    val claimableCount = ACHIEVEMENTS.count { achievement ->
        achievement.id !in state.stats.claimedAchievementRewards &&
            (achievement.id in state.stats.unlockedAchievements ||
                achievement.progress(state.stats) >= achievement.target)
    }
    LazyColumn(
        Modifier.fillMaxSize().background(Playform.Cream)
            .padding(horizontal = 17.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("ZA.SNAKE", color = Playform.Ink, fontSize = 21.sp,
                    fontWeight = FontWeight.Black, modifier = Modifier.weight(1f))
                PlayformStatusPill("🪙 ${state.stats.coins}",
                    color = Playform.Ink, background = Playform.White)
                IconButtonFrame(onClick = onSettings) {
                    androidx.compose.material3.Icon(Icons.Rounded.Settings,
                        contentDescription = tx(lang, "settings"), tint = Playform.Ink)
                }
            }
        }
        item {
            PlayformDarkCard {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        PlayformTag("ZA.SNAKE / PLAYFORM")
                        Text(if (ar) "جاهز للعب؟" else "READY TO PLAY?",
                            color = Playform.White, fontSize = 27.sp,
                            lineHeight = 34.sp, fontWeight = FontWeight.Black)
                        Text(if (ar) "اختار وضعك وحقق رقمًا جديدًا" else "Pick a mode. Beat your best.",
                            color = Playform.LightOnDark, fontSize = 13.sp, lineHeight = 20.sp)
                    }
                    Image(painter = painterResource(R.drawable.za_snake_mascot),
                        contentDescription = null,
                        modifier = Modifier.size(108.dp).clip(RoundedCornerShape(21.dp)),
                        contentScale = ContentScale.Crop)
                }
                Button(onClick = onNewGame,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 55.dp),
                    shape = RoundedCornerShape(17.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Playform.Coral, contentColor = Playform.Ink)) {
                    androidx.compose.material3.Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                    Text(if (ar) "العب الآن" else "PLAY NOW", fontSize = 17.sp,
                        fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 8.dp))
                }
                if (state.hasSavedGame) {
                    OutlinedButton(onClick = onResume, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Playform.White)) {
                        Text(tx(lang, "resume_game"), fontSize = 13.sp)
                    }
                }
            }
        }
        item {
            PlayformSectionTitle(if (ar) "استكشف اللعبة" else "Explore Za Snake",
                if (ar) "مكافآتك وشكلك وإنجازاتك في مكان واحد" else "Rewards, style and achievements")
        }
        item {
            // This is a scroll-content action shelf, NOT a fixed bottom navigation bar.
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PlayformHomeTile(if (ar) "المكافآت" else "Rewards", "🎁",
                    Playform.PaleCoral, Modifier.weight(1f), onDaily)
                PlayformHomeTile(if (ar) "التخصيص" else "Style Lab", "🐍",
                    Playform.PaleLavender, Modifier.weight(1f), onShop)
                PlayformHomeTile(if (ar) "الإنجازات" else "Achievements", "🏆",
                    Playform.PaleMint, Modifier.weight(1f), onClaimAchievements)
            }
        }
        if (claimableCount > 0) item {
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                .background(Playform.Gold).clickable(onClick = onClaimAchievements)
                .padding(horizontal = 14.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Text("🎁", fontSize = 22.sp)
                Text(if (ar) "عندك ${claimableCount} مكافآت جاهزة للاستلام!"
                    else "${claimableCount} achievement rewards ready!",
                    modifier = Modifier.weight(1f).padding(horizontal = 9.dp),
                    color = Playform.Ink, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                androidx.compose.material3.Icon(Icons.Rounded.KeyboardArrowRight,
                    contentDescription = null, tint = Playform.Ink)
            }
        }
        item {
            CloudStatusLine(state, lang)
        }
        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
internal fun PlayformHomeTile(label: String, emoji: String, color: Color, modifier: Modifier, onClick: () -> Unit) {
    Column(modifier.clip(RoundedCornerShape(20.dp)).background(color)
        .clickable(onClick = onClick).padding(horizontal = 5.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Text(emoji, fontSize = 28.sp)
        Text(label, color = Playform.Ink, fontSize = 12.sp, fontWeight = FontWeight.Bold,
            lineHeight = 16.sp, textAlign = TextAlign.Center, minLines = 2)
    }
}
