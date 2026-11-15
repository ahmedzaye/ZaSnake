package com.ahmedzayed.zasnake.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ahmedzayed.zasnake.game.AppUiState

internal fun CloudStatusLine(state: AppUiState, language: String) {
    val configuredConnection = state.cloudStatus in listOf("connected", "sync_error", "synced")
    Row(Modifier.fillMaxWidth().padding(top = 1.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        androidx.compose.material3.Icon(if (configuredConnection) Icons.Rounded.CloudDone else Icons.Rounded.CloudOff, null, tint = if (configuredConnection) Neon else Muted, modifier = Modifier.size(15.dp))
        Text(if (state.cloudStatus == "not_configured") tx(language, "sync_pending") else cloudStatusLabel(state.cloudStatus, language),
            color = Muted, fontSize = 10.sp, modifier = Modifier.padding(start = 5.dp), textAlign = TextAlign.Center)
    }
}

internal fun cloudStatusLabel(status: String, lang: String) = tx(lang, when (status) {
    "connected" -> "cloud_connected"
    "sync_error" -> "cloud_sync_error"
    "checking" -> "cloud_checking"
    "syncing" -> "cloud_syncing"
    "conflict" -> "cloud_conflict"
    "offline" -> "cloud_offline"
    "synced" -> "cloud_synced"
    else -> "cloud_not_configured"
})
