package com.ahmedzayed.zasnake.platform

import android.app.Activity
import android.content.Context
import com.ahmedzayed.zasnake.R
import com.google.android.gms.games.PlayGames
import com.google.android.gms.common.api.ApiException
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
class PlayGamesBridge(private val activity: Activity, private val enabled: Boolean = true) {
    private var pendingConflict: SnapshotsClient.SnapshotConflict? = null
    private val snapshotName = "za_snake_progress_v1"
    // LOCAL guard only. Player IDs from Android are never trusted by the billing
    // backend. Reject an account change before mixing legacy device data into
    // another Play Games Saved Games profile.
    private val accountBinding = activity.getSharedPreferences(
        "za_snake_pgs_account_binding_v1", Context.MODE_PRIVATE
    )
    private var changedAccount = false
    /** A non-sensitive failure category. No auth tokens or player IDs are logged. */
    var serverAuthFailureReason = "not_attempted"
        private set
    private var accountValidationFailureReason = "not_attempted"

    private fun apiFailure(e: Exception?): String {
        val status = (e as? ApiException)?.statusCode
        return if (status == null) "api_error" else "google_status_$status"
    }

    fun hasChangedAccount(): Boolean = changedAccount

    private fun validateAccountBeforeSync(onResult: (Boolean) -> Unit) {
        runCatching {
            PlayGames.getPlayersClient(activity).currentPlayer.addOnCompleteListener { task ->
                val playerId = if (task.isSuccessful)
                    task.result?.playerId?.takeIf { it.isNotBlank() } else null
                if (playerId == null) {
                    accountValidationFailureReason = "player_profile_" + apiFailure(task.exception)
                    onResult(false)
                    return@addOnCompleteListener
                }
                val existing = accountBinding.getString("bound_player_id", null)
                if (existing != null && existing != playerId) {
                    changedAccount = true
                    accountValidationFailureReason = "different_google_player"
                    onResult(false)
                    return@addOnCompleteListener
                }
                if (existing == null &&
                    !accountBinding.edit().putString("bound_player_id", playerId).commit()) {
                    accountValidationFailureReason = "local_binding_failed"
                    onResult(false)
                    return@addOnCompleteListener
                }
                changedAccount = false
                accountValidationFailureReason = "none"
                onResult(true)
            }
        }.onFailure {
            accountValidationFailureReason = "player_profile_" + apiFailure(it as? Exception)
            onResult(false)
        }
    }

    fun isAllowedByAge(): Boolean = enabled

    fun isConfigured(): Boolean {
        if (!enabled) return false
        val projectId = activity.getString(R.string.game_services_project_id).trim()
        return projectId.isNotBlank() && projectId != "0" && projectId.all(Char::isDigit)
    }

    /** Request a one-time server auth code. This is NOT the Android OAuth client ID. */
    fun requestServerAuthCode(webClientId: String, onResult: (String?) -> Unit) {
        if (!isConfigured() || !webClientId.endsWith(".apps.googleusercontent.com")) {
            serverAuthFailureReason = "play_games_configuration_missing"
            onResult(null)
            return
        }
        validateAccountBeforeSync { safe ->
            if (!safe) {
                serverAuthFailureReason = accountValidationFailureReason
                onResult(null)
                return@validateAccountBeforeSync
            }
            runCatching {
                PlayGames.getGamesSignInClient(activity)
                    .requestServerSideAccess(webClientId, false)
                    .addOnCompleteListener { task ->
                        val code = if (task.isSuccessful)
                            task.result?.takeIf(String::isNotBlank) else null
                        serverAuthFailureReason = when {
                            code != null -> "none"
                            task.isSuccessful -> "empty_server_auth_code"
                            else -> "server_access_" + apiFailure(task.exception)
                        }
                        onResult(code)
                    }
            }.onFailure {
                serverAuthFailureReason = "server_access_" + apiFailure(it as? Exception)
                onResult(null)
            }
        }
    }

    fun authenticate(onResult: (Boolean) -> Unit) {
        if (!isConfigured()) {
            onResult(false)
            return
        }
        runCatching {
            PlayGames.getGamesSignInClient(activity).isAuthenticated
                .addOnCompleteListener { task ->
                    if (task.isSuccessful && task.result?.isAuthenticated == true) {
                        validateAccountBeforeSync(onResult)
                    } else onResult(false)
                }
        }.onFailure { onResult(false) }
    }

    /** Explicit user action; only invoked from the Account screen for allowed age bands. */
    fun requestSignIn(onResult: (Boolean) -> Unit) {
        if (!isConfigured()) {
            onResult(false)
            return
        }
        runCatching {
            PlayGames.getGamesSignInClient(activity).signIn()
                .addOnCompleteListener { task ->
                    if (task.isSuccessful && task.result?.isAuthenticated == true) {
                        validateAccountBeforeSync(onResult)
                    } else onResult(false)
                }
        }.onFailure { onResult(false) }
    }

    fun unlockAchievement(localAchievementId: String) {
        if (!isConfigured() || changedAccount) return
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
            "apples500" -> R.string.pgs_achievement_apples500
            "apples1000" -> R.string.pgs_achievement_apples1000
            "games50" -> R.string.pgs_achievement_games50
            "games100" -> R.string.pgs_achievement_games100
            "golden10" -> R.string.pgs_achievement_golden10
            "golden50" -> R.string.pgs_achievement_golden50
            "scissors25" -> R.string.pgs_achievement_scissors25
            "play60min" -> R.string.pgs_achievement_play60min
            "score100" -> R.string.pgs_achievement_score100
            else -> return
        }
        val achievementId = activity.getString(resourceName).trim()
        if (achievementId.isBlank() || achievementId.startsWith("REPLACE_")) return
        runCatching { PlayGames.getAchievementsClient(activity).unlock(achievementId) }
    }

    /** Opens the single Saved Games snapshot with manual conflict detection. */
    fun loadBundle(onResult: (CloudLoadResult) -> Unit) {
        if (!isConfigured() || changedAccount) {
            onResult(CloudLoadResult.Unavailable)
            return
        }
        val client = PlayGames.getSnapshotsClient(activity)
        client.open(snapshotName, true, SnapshotsClient.RESOLUTION_POLICY_MANUAL)
            .addOnSuccessListener { result ->
                if (result.isConflict) {
                    val conflict = result.conflict
                    if (conflict == null) {
                        onResult(CloudLoadResult.Unavailable)
                        return@addOnSuccessListener
                    }
                    val serverSnapshot = conflict.snapshot
                    val deviceSnapshot = conflict.conflictingSnapshot
                    if (serverSnapshot == null || deviceSnapshot == null) {
                        onResult(CloudLoadResult.Unavailable)
                        return@addOnSuccessListener
                    }
                    pendingConflict = conflict
                    val server = readSnapshot(serverSnapshot)
                    val device = readSnapshot(deviceSnapshot)
                    if (server == null || device == null) {
                        onResult(CloudLoadResult.Unavailable)
                    } else {
                        onResult(CloudLoadResult.Conflict(server, device))
                    }
                } else {
                    val snapshot = result.data
                    if (snapshot == null) {
                        onResult(CloudLoadResult.Unavailable)
                        return@addOnSuccessListener
                    }
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
        if (!isConfigured() || changedAccount) {
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
                if (snapshot == null) {
                    onResult(false)
                    return@addOnSuccessListener
                }
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
        if (changedAccount) {
            onResult(false)
            return
        }
        val conflict = pendingConflict
        if (conflict == null) {
            saveBundle(json, onResult)
            return
        }
        val selected = if (chooseServer) conflict.snapshot else conflict.conflictingSnapshot
        if (selected == null) {
            onResult(false)
            return
        }
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
