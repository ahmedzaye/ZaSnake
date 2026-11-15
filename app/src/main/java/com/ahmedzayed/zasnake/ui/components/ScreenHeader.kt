package com.ahmedzayed.zasnake.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.KeyboardArrowRight
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun ScreenHeader(title: String, onBack: () -> Unit, trailing: (@Composable () -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().padding(bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButtonFrame(onClick = onBack) {
            androidx.compose.material3.Icon(
                if (LocalLayoutDirection.current == LayoutDirection.Rtl) Icons.Rounded.KeyboardArrowRight
                else Icons.Rounded.ArrowBack,
                contentDescription = if (LocalLayoutDirection.current == LayoutDirection.Rtl) "رجوع" else "Back",
                tint = Playform.Ink
            )
        }
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(title, color = Playform.Ink, fontSize = 22.sp, fontWeight = FontWeight.Black)
        }
        if (trailing != null) trailing() else Spacer(Modifier.size(44.dp))
    }
}
