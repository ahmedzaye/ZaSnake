package com.ahmedzayed.zasnake.platform

import android.app.Activity
import android.content.Context
import com.ahmedzayed.zasnake.R
import com.google.android.gms.games.PlayGames
import com.google.android.gms.games.SnapshotsClient
import com.google.android.gms.games.snapshot.Snapshot
import com.google.android.gms.games.snapshot.SnapshotMetadataChange
import java.nio.charset.StandardCharsets

sealed class CloudLoadResult {
    data class Payload(val json: String) : CloudLoadResult()
    data class Conflict(val serverJson: String, val deviceJson: String) : CloudLoadResult()
    data object Unavailable : CloudLoadResult()
}

/** Activity-scoped Play Games v2 adapter. All game data is independently saved locally. */
class PlayGamesBridge(private val activity: Activity) {
    private var pendingConflict: SnapshotsClient.SnapshotConflict? = null
    private val snapshotName = "za_snake_progress_v1"

    fun isConfigured(): Boolean {
        val projectId = activity.getString(R.string.game_services_project_id).trim()
        return projectId.isNotBlank() && projectId != "0" && projectId.all(Char::isDigit)
    }

    fun authenticate(onResult: (Boolean) -> Unit) {
        if (!isConfigured()) {
            onResult(false)
            return
        }
        runCatching {
            PlayGames.getGamesSignInClient(activity).isAuthenticated
                .addOnCompleteListener { task ->
                    onResult(task.isSuccessful && task.result?.isAuthenticated == true)
                }
        }.onFailure { onResult(false) }
    }

    fun unlockAchievement(localAchievementId: String) {
        if (!isConfigured()) return
        val resourceName = when (localAchievementId) {
            "first" -> R.string.pgs_achievement_first
            "apples50" -> R.string.pgs_achievement_apples50
            "apples250" -> R.string.pgs_achievement_apples250
            "score10" -> R.string.pgs_achievement_score10
            "score50" -> R.string.pgs_achievement_score50
            "golden" -> R.string.pgs_achievement_golden
            "barber" -> R.string.pgs_achievement_barber
            "long30" -> R.string.pgs_achievement_long30
            "games10" -> R.string.pgs_achievement_games10
            "clock15" -> R.string.pgs_achievement_clock15
            "dedicated" -> R.string.pgs_achievement_dedicated
            else -> return
        }
        val achievementId = activity.getString(resourceName).trim()
        if (achievementId.isBlank() || achievementId.startsWith("REPLACE_")) return
        runCatching { PlayGames.getAchievementsClient(activity).unlock(achievementId) }
    }

    /** Opens the single Saved Games snapshot with manual conflict detection. */
    fun loadBundle(onResult: (CloudLoadResult) -> Unit) {
        if (!isConfigured()) {
            onResult(CloudLoadResult.Unavailable)
            return
        }
        val client = PlayGames.getSnapshotsClient(activity)
        client.open(snapshotName, true, SnapshotsClient.RESOLUTION_POLICY_MANUAL)
            .addOnSuccessListener { result ->
                if (result.isConflict) {
                    val conflict = result.conflict
                    pendingConflict = conflict
                    val server = readSnapshot(conflict.snapshot)
                    val device = readSnapshot(conflict.conflictingSnapshot)
                    if (server == null || device == null) {
                        onResult(CloudLoadResult.Unavailable)
                    } else {
                        onResult(CloudLoadResult.Conflict(server, device))
                    }
                } else {
                    val snapshot = result.data
                    val payload = readSnapshot(snapshot)
                    client.discardAndClose(snapshot).addOnCompleteListener {
                        onResult(if (payload == null) CloudLoadResult.Unavailable else CloudLoadResult.Payload(payload))
                    }
                }
            }
            .addOnFailureListener { onResult(CloudLoadResult.Unavailable) }
    }

    /** Writes a bundle. A detected conflict is returned rather than silently overwriting a round. */
    fun saveBundle(json: String, onResult: (Boolean) -> Unit) {
        if (!isConfigured()) {
            onResult(false)
            return
        }
        val client = PlayGames.getSnapshotsClient(activity)
        client.open(snapshotName, true, SnapshotsClient.RESOLUTION_POLICY_MANUAL)
            .addOnSuccessListener { result ->
                if (result.isConflict) {
                    pendingConflict = result.conflict
                    onResult(false)
                    return@addOnSuccessListener
                }
                val snapshot = result.data
                val writeOk = runCatching { snapshot.snapshotContents.writeBytes(json.toByteArray(StandardCharsets.UTF_8)) }.getOrDefault(false)
                if (!writeOk) {
                    client.discardAndClose(snapshot).addOnCompleteListener { onResult(false) }
                    return@addOnSuccessListener
                }
                val metadata = SnapshotMetadataChange.Builder()
                    .setDescription("Za Snake progress")
                    .build()
                client.commitAndClose(snapshot, metadata)
                    .addOnSuccessListener { onResult(true) }
                    .addOnFailureListener { onResult(false) }
            }
            .addOnFailureListener { onResult(false) }
    }

    /** Resolves the platform-level snapshot conflict using the selected base, then commits the
     * app-level merged payload. The game-level UI decides which active round is retained. */
    fun resolvePendingConflict(json: String, chooseServer: Boolean, onResult: (Boolean) -> Unit) {
        val conflict = pendingConflict
        if (conflict == null) {
            saveBundle(json, onResult)
            return
        }
        val selected = if (chooseServer) conflict.snapshot else conflict.conflictingSnapshot
        val client = PlayGames.getSnapshotsClient(activity)
        client.resolveConflict(conflict.conflictId, selected)
            .addOnSuccessListener {
                pendingConflict = null
                saveBundle(json, onResult)
            }
            .addOnFailureListener { onResult(false) }
    }

    private fun readSnapshot(snapshot: Snapshot): String? = runCatching {
        String(snapshot.snapshotContents.readFully(), StandardCharsets.UTF_8)
    }.getOrNull()

    companion object {
        fun localOnlyStatus(context: Context) =
            if (context.getString(R.string.game_services_project_id).trim() == "0") "not_configured" else "checking"
    }
}
