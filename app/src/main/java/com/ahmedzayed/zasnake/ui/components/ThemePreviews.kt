package com.ahmedzayed.zasnake.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ahmedzayed.zasnake.game.GameTheme
import com.ahmedzayed.zasnake.game.SnakeSkin

/** Tiny themed board plus five color swatches; preview does not change the selected theme. */
@Composable
internal fun ThemeMiniPreview(theme: GameTheme, skin: SnakeSkin, modifier: Modifier = Modifier) {
    val p = paletteFor(theme)
    val snakeColor = snakeSkinColors(skin).first()
    Row(modifier.clip(RoundedCornerShape(12.dp)).background(p.bg).border(1.dp, p.border, RoundedCornerShape(12.dp)).padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically) {
        Canvas(Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(7.dp))) {
            drawRect(p.boardBackground)
            val cell = size.height / 5f
            var gx = 0f
            while (gx < size.width) {
                drawLine(p.boardGrid, Offset(gx, 0f), Offset(gx, size.height), 1f)
                gx += cell
            }
            for (y in 0..5) drawLine(p.boardGrid, Offset(0f, y * cell), Offset(size.width, y * cell), 1f)
            val midX = size.width * 0.40f
            for (i in 0..3) drawRoundRect(
                color = if (i == 0) snakeColor else snakeColor.copy(alpha = 0.82f - i * 0.12f),
                topLeft = Offset(midX - i * cell * 0.85f, size.height * 0.43f),
                size = Size(cell * 0.77f, cell * 0.77f),
                cornerRadius = CornerRadius(cell * 0.24f)
            )
            drawCircle(p.gold, radius = cell * 0.33f, center = Offset(size.width * 0.78f, size.height * 0.52f))
        }
        Column(Modifier.width(46.dp).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            listOf(p.bg, p.panel, p.panel2, p.accent, p.gold).forEach { color ->
                Box(Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(3.dp)).background(color))
            }
        }
    }
}

@Composable
internal fun ThemeOptionCard(
    theme: GameTheme,
    selected: Boolean,
    language: String,
    onClick: () -> Unit
) {
    val p = paletteFor(theme)
    val titleKey = "theme_${theme.key}"
    val descriptionKey = "theme_${theme.key}_desc"
    Column(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) Playform.PaleLavender else Playform.White)
            .border(1.dp, if (selected) Playform.Ink else Playform.Border, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(Modifier.fillMaxWidth().height(25.dp).clip(RoundedCornerShape(7.dp))) {
            Box(Modifier.weight(1f).fillMaxHeight().background(p.bg))
            Box(Modifier.weight(1f).fillMaxHeight().background(p.panel))
            Box(Modifier.weight(1f).fillMaxHeight().background(p.panel2))
            Box(Modifier.weight(1f).fillMaxHeight().background(p.accent))
            Box(Modifier.weight(1f).fillMaxHeight().background(p.gold))
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(tx(language, titleKey), color = Playform.Ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text(tx(language, descriptionKey), color = Playform.TextSecondary, fontSize = 12.sp, lineHeight = 15.sp)
            }
            if (selected) {
                androidx.compose.material3.Icon(Icons.Rounded.CheckCircle, contentDescription = tx(language, "theme_active"), tint = p.accent, modifier = Modifier.size(22.dp))
            } else {
                Text(tx(language, "theme_apply"), color = Playform.Ink, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
