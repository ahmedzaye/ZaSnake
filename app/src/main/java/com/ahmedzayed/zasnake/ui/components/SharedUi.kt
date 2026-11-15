package com.ahmedzayed.zasnake.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ahmedzayed.zasnake.game.SnakeSkin
import kotlin.math.min

@Composable
internal fun SectionLabel(label: String) {
    Text(label, color = Muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 3.dp, top = 9.dp, bottom = 7.dp))
}

@Composable
internal fun SettingsPanel(content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(15.dp)).background(Panel2).border(1.dp, BorderColor, RoundedCornerShape(15.dp)).padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
}

@Composable
internal fun SettingHeading(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        androidx.compose.material3.Icon(icon, null, tint = Playform.CoralText, modifier = Modifier.size(19.dp))
        Text(label, color = PrimaryText, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
    }
}

@Composable
internal fun SettingSwitch(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        androidx.compose.material3.Icon(icon, null, tint = Muted, modifier = Modifier.size(19.dp))
        Text(label, color = PrimaryText, fontSize = 13.sp, modifier = Modifier.weight(1f).padding(horizontal = 9.dp))
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = androidx.compose.material3.SwitchDefaults.colors(
                checkedThumbColor = ButtonText,
                checkedTrackColor = Neon,
                checkedBorderColor = Neon,
                uncheckedThumbColor = Muted,
                uncheckedTrackColor = Panel,
                uncheckedBorderColor = BorderColor
            )
        )
    }
}

@Composable
internal fun ChoiceChip(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    FilterChip(selected = selected, onClick = onClick, label = { Text(label, fontSize = 12.sp, maxLines = 1) }, modifier = modifier,
        colors = androidx.compose.material3.FilterChipDefaults.filterChipColors(
            selectedContainerColor = Neon,
            selectedLabelColor = ButtonText,
            containerColor = ChipBackground,
            labelColor = SecondaryText
        ))
}

@Composable
internal fun SkinChip(skin: SnakeSkin, selected: Boolean, language: String, onClick: () -> Unit) {
    val color = when (skin) {
        SnakeSkin.GREEN -> Neon; SnakeSkin.PURPLE -> Color(0xFFD28BFF); SnakeSkin.YELLOW -> Color(0xFFFFE600)
        SnakeSkin.EGYPT -> Color(0xFFCE1126); SnakeSkin.UAE -> Color(0xFF06966D)
        SnakeSkin.JORDAN -> Color(0xFFDB2745); SnakeSkin.PALESTINE -> Color(0xFF199169)
        SnakeSkin.KUWAIT -> Color(0xFF172528); SnakeSkin.SAUDI -> Color(0xFF066D45)
        SnakeSkin.CYBER -> Color(0xFF00E5FF); SnakeSkin.ICE -> Color(0xFF9EDFFF); SnakeSkin.FIRE -> Color(0xFFFF623E)
    }
    Column(Modifier.clickable { onClick() }.padding(2.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Box(Modifier.size(31.dp).clip(CircleShape).background(color).border(if (selected) 3.dp else 1.dp, if (selected) Color.White else Color(0xFF53665A), CircleShape))
        Text(tx(language, skin.key), color = if (selected) Playform.Ink else Playform.TextSecondary, fontSize = 12.sp, textAlign = TextAlign.Center)
    }
}

@Composable
internal fun StatTile(label: String, value: String, modifier: Modifier) {
    Column(modifier.clip(RoundedCornerShape(13.dp)).background(Panel).border(1.dp, BorderColor, RoundedCornerShape(13.dp)).padding(vertical = 14.dp, horizontal = 9.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(value, color = Playform.Ink, fontWeight = FontWeight.Black, fontSize = 21.sp, textAlign = TextAlign.Center)
        Text(label, color = Playform.TextSecondary, fontSize = 12.sp, textAlign = TextAlign.Center, lineHeight = 13.sp)
    }
}

@Composable
internal fun buttonPressScale(interaction: MutableInteractionSource): Float {
    val pressed by interaction.collectIsPressedAsState()
    val target = if (LocalVisualMotionEnabled.current && pressed) 0.97f else 1f
    val scale by animateFloatAsState(target, tween(95), label = "PressScale")
    return scale
}

@Composable
internal fun PrimaryActionButton(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val scale = buttonPressScale(interaction)
    Button(onClick = onClick,
        interactionSource = interaction,
        modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp).graphicsLayer { scaleX = scale; scaleY = scale },
        shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = Playform.Coral, contentColor = Playform.Ink)) {
        androidx.compose.material3.Icon(icon, null, modifier = Modifier.size(21.dp))
        Text(label, fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 8.dp))
    }
}

@Composable
internal fun OutlinedActionButton(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val scale = buttonPressScale(interaction)
    OutlinedButton(onClick = onClick,
        interactionSource = interaction,
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).graphicsLayer { scaleX = scale; scaleY = scale },
        shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = Playform.Ink)) {
        androidx.compose.material3.Icon(icon, null, modifier = Modifier.size(19.dp))
        Text(label, fontSize = 13.sp, modifier = Modifier.padding(start = 8.dp))
    }
}

@Composable
internal fun SecondaryTile(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier, onClick: () -> Unit) {
    Column(modifier.clip(RoundedCornerShape(13.dp)).background(Panel).border(1.dp, BorderColor, RoundedCornerShape(13.dp)).clickable { onClick() }.padding(horizontal = 7.dp, vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(7.dp)) {
        androidx.compose.material3.Icon(icon, null, tint = if (icon == Icons.Rounded.EmojiEvents) Gold else Neon, modifier = Modifier.size(24.dp))
        Text(label, color = PrimaryText, fontSize = 11.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center, lineHeight = 14.sp)
    }
}

@Composable
internal fun IconButtonFrame(onClick: () -> Unit, modifier: Modifier = Modifier.size(44.dp), content: @Composable () -> Unit) {
    Box(modifier.clip(RoundedCornerShape(12.dp)).background(Playform.White).border(1.dp, Playform.Border, RoundedCornerShape(12.dp)).clickable { onClick() }, contentAlignment = Alignment.Center) { content() }
}

internal fun formatPlayTime(millis: Long, language: String): String {
    val totalMinutes = (millis / 60_000L).coerceAtLeast(0)
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (language == "ar") {
        if (hours > 0) "${hours}س ${minutes}د" else "${minutes}د"
    } else {
        if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
    }
}


/**
 * Compact visual claim action shared by Daily Rewards and Achievements.
 * Material3 retains a minimum accessible touch target around the 36dp visual button.
 * The reward callback and eligibility remain with each owning screen.
 */
@Composable
internal fun RewardClaimButton(
    label: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.height(36.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            horizontal = 10.dp, vertical = 0.dp
        ),
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Playform.Coral,
            contentColor = Playform.Ink
        )
    ) {
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}
