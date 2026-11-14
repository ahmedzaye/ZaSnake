package com.ahmedzayed.zasnake.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * PLAYFORM visual tokens. These are used by the application chrome; the playfield
 * keeps the selected GameTheme palette and the snake retains its equipped skin,
 * head and body renderer. No persistence or game-rule values live here.
 */
internal object Playform {
    val Cream = Color(0xFFF7F3EB)
    val Ink = Color(0xFF262641)
    val Coral = Color(0xFFFF7768)
    val Lavender = Color(0xFFC8B9F4)
    val Gold = Color(0xFFFFDB80)
    val Mint = Color(0xFF81D7D0)
    val White = Color.White
    val TextSecondary = Color(0xFF625D70) // 5.72:1 on Cream
    val CoralText = Color(0xFFB4474C) // >4.5:1 on Cream
    val GoldText = Color(0xFF8B5D13)
    val Border = Color(0xFFE1DBE5)
    val PaleLavender = Color(0xFFECE5F3)
    val PaleCoral = Color(0xFFFFDFD6)
    val PaleMint = Color(0xFFDDF5F0)
    val LightOnDark = Color(0xFFD8D3E8)
}

@Composable
internal fun PlayformPageIntro(title: String, description: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(title, color = Playform.Ink, fontSize = 25.sp, lineHeight = 32.sp, fontWeight = FontWeight.Black)
        Text(description, color = Playform.TextSecondary, fontSize = 13.sp, lineHeight = 19.sp)
    }
}

@Composable
internal fun PlayformCard(
    modifier: Modifier = Modifier,
    background: Color = Playform.White,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
            .background(background).border(1.dp, Playform.Border, RoundedCornerShape(20.dp))
            .padding(15.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        content = content
    )
}

@Composable
internal fun PlayformDarkCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp))
            .background(Playform.Ink).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(11.dp),
        content = content
    )
}

@Composable
internal fun PlayformTag(label: String, color: Color = Playform.Gold) {
    Text(label, color = color, fontSize = 11.sp, fontWeight = FontWeight.Bold, lineHeight = 15.sp)
}

@Composable
internal fun PlayformSectionTitle(title: String, subtitle: String? = null) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(title, fontSize = 18.sp, lineHeight = 24.sp, color = Playform.Ink,
            fontWeight = FontWeight.Black)
        if (subtitle != null) Text(subtitle, fontSize = 13.sp, lineHeight = 18.sp,
            color = Playform.TextSecondary)
    }
}

@Composable
internal fun PlayformCounterTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    background: Color = Playform.White
) {
    Column(
        modifier = modifier.clip(RoundedCornerShape(16.dp)).background(background)
            .padding(horizontal = 9.dp, vertical = 13.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(value, color = Playform.Ink, fontWeight = FontWeight.Black, fontSize = 23.sp,
            maxLines = 1, textAlign = TextAlign.Center)
        Text(label, color = Playform.TextSecondary, fontSize = 12.sp, lineHeight = 16.sp,
            textAlign = TextAlign.Center)
    }
}

@Composable
internal fun PlayformProgressBar(progress: Float, color: Color = Playform.Coral, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(8.dp))
        .background(Playform.PaleLavender)) {
        Box(Modifier.fillMaxWidth(progress.coerceIn(0f, 1f)).height(7.dp)
            .clip(RoundedCornerShape(8.dp)).background(color))
    }
}

@Composable
internal fun PlayformStatusPill(
    text: String,
    color: Color = Playform.Ink,
    background: Color = Playform.PaleLavender
) {
    Box(Modifier.clip(RoundedCornerShape(12.dp)).background(background)
        .padding(horizontal = 11.dp, vertical = 7.dp), contentAlignment = Alignment.Center) {
        Text(text, color = color, fontWeight = FontWeight.Bold, fontSize = 12.sp,
            lineHeight = 17.sp, textAlign = TextAlign.Center)
    }
}
