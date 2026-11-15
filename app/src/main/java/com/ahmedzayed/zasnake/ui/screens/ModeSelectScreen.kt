package com.ahmedzayed.zasnake.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Gamepad
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ahmedzayed.zasnake.game.AppUiState
import com.ahmedzayed.zasnake.game.GameMode
import kotlin.math.min

@Composable
internal fun ModeSelectScreen(state: AppUiState, onBack: () -> Unit, onSelect: (GameMode) -> Unit, onStart: () -> Unit) {
    val lang = state.settings.language
    Column(Modifier.fillMaxSize().background(Playform.Cream)
        .padding(horizontal = 16.dp, vertical = 12.dp)) {
        ScreenHeader(tx(lang, "choose_mode"), onBack)
        PlayformPageIntro(
            if (lang == "ar") "اختار أسلوب اللعب" else "Choose your mode",
            if (lang == "ar") "كل وضع له تحدٍ مختلف. القرار ليك!" else "Three ways to play. Your choice."
        )
        Spacer(Modifier.height(16.dp))
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ModeCard(GameMode.CLASSIC, state.selectedMode == GameMode.CLASSIC, lang, onSelect)
            ModeCard(GameMode.TIME_ATTACK, state.selectedMode == GameMode.TIME_ATTACK, lang, onSelect)
            ModeCard(GameMode.ZEN, state.selectedMode == GameMode.ZEN, lang, onSelect)
        }
        Spacer(Modifier.height(10.dp))
        PrimaryActionButton(state.selectedMode.startLabel(lang), Icons.Rounded.PlayArrow, onStart)
    }
}

@Composable
internal fun ModeCard(mode: GameMode, selected: Boolean, language: String, onSelect: (GameMode) -> Unit) {
    val color = when (mode) {
        GameMode.CLASSIC -> Playform.PaleCoral
        GameMode.TIME_ATTACK -> Playform.Gold
        GameMode.ZEN -> Playform.PaleMint
    }
    val icon = when (mode) {
        GameMode.CLASSIC -> Icons.Rounded.Gamepad
        GameMode.TIME_ATTACK -> Icons.Rounded.Timer
        GameMode.ZEN -> Icons.Rounded.Spa
    }
    Row(Modifier.fillMaxWidth().heightIn(min = 96.dp)
        .clip(RoundedCornerShape(22.dp)).background(color)
        .border(if (selected) 2.dp else 1.dp,
            if (selected) Playform.Ink else Playform.Border, RoundedCornerShape(22.dp))
        .clickable { onSelect(mode) }.padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.size(49.dp).clip(RoundedCornerShape(16.dp))
            .background(Playform.White.copy(alpha = .8f)), contentAlignment = Alignment.Center) {
            androidx.compose.material3.Icon(icon, contentDescription = null,
                tint = Playform.Ink, modifier = Modifier.size(29.dp))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(mode.title(language), color = Playform.Ink, fontWeight = FontWeight.Black, fontSize = 17.sp)
            Text(mode.description(language), color = Playform.TextSecondary,
                fontSize = 12.sp, lineHeight = 17.sp)
        }
        if (selected) androidx.compose.material3.Icon(Icons.Rounded.CheckCircle,
            contentDescription = if (language == "ar") "محدد" else "Selected",
            tint = Playform.Ink, modifier = Modifier.size(23.dp))
    }
}
