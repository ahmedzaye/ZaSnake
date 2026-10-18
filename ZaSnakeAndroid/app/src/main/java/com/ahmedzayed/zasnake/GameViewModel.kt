package com.ahmedzayed.zasnake

import android.app.Application
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ahmedzayed.zasnake.data.LocalStore
import com.ahmedzayed.zasnake.data.CloudBundle
import com.ahmedzayed.zasnake.data.decodeCloudBundle
import com.ahmedzayed.zasnake.data.encodeCloudBundle
import com.ahmedzayed.zasnake.data.SettingsRepository
import com.ahmedzayed.zasnake.game.ACHIEVEMENTS
import com.ahmedzayed.zasnake.game.AppScreen
import com.ahmedzayed.zasnake.game.AppSettings
import com.ahmedzayed.zasnake.game.AppUiState
import com.ahmedzayed.zasnake.game.ConfirmationAction
import com.ahmedzayed.zasnake.game.ConflictResolution
import com.ahmedzayed.zasnake.game.DeviceCounters
import com.ahmedzayed.zasnake.game.EngineEvent
import com.ahmedzayed.zasnake.game.GameMode
import com.ahmedzayed.zasnake.game.GameSnapshot
import com.ahmedzayed.zasnake.game.PendingRoundConflict
import com.ahmedzayed.zasnake.game.PlayerStats
import com.ahmedzayed.zasnake.game.RoundTombstone
import com.ahmedzayed.zasnake.game.SnakeEngine
import com.ahmedzayed.zasnake.game.SnakeSkin
import com.ahmedzayed.zasnake.platform.CloudLoadResult
import com.ahmedzayed.zasnake.platform.PlayGamesBridge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GameViewModel(application: Application) : AndroidViewModel(application) {
    private val settingsRepository = SettingsRepository(application)
    private val localStore = LocalStore(application)
    private val mutableUiState = mutableStateOf(AppUiState())
    val uiState: State<AppUiState> = mutableUiState
    val engine = SnakeEngine()

    private var deviceId: String = ""
    private var pendingPlayMs = 0L
    private var localSaveElapsedMs = 0L
    private var lastStatsSaveElapsedMs = 0L
    private var currentSettings = AppSettings()
    private var currentStats = PlayerStats()
    private var currentTombstone: RoundTombstone? = null
    private var cloudSyncInProgress = false
    private val toneGenerator: ToneGenerator? = runCatching { ToneGenerator(AudioManager.STREAM_MUSIC, 55) }.getOrNull()
    private val vibrator: Vibrator? = getApplication<Application>().getSystemService(Vibrator::class.java)

    init {
        viewModelScope.launch {
            currentSettings = settingsRepository.settings.first()
            deviceId = settingsRepository.installationId()
            currentStats = localStore.loadStats()
            currentTombstone = localStore.loadRoundTombstone()
            val rawSaved = localStore.loadGame()
            val saved = effectiveRound(rawSaved, currentTombstone)
            if (rawSaved != null && saved == null) {
                localStore.clearGame(expectedSessionId = rawSaved.sessionId)
            }
            if (saved != null) engine.restore(saved)
            val unlocked = evaluateAchievements(currentStats)
            currentStats = unlocked.first
            mutableUiState.value = mutableUiState.value.copy(
                ready = true,
                settings = currentSettings,
                stats = currentStats,
                selectedMode = currentSettings.lastMode,
                hasSavedGame = saved != null,
                newAchievementIds = unlocked.second
            )
            if (unlocked.second.isNotEmpty()) localStore.saveStats(currentStats)
        }
    }

    fun setCloudStatus(status: String) {
        mutableUiState.value = mutableUiState.value.copy(cloudStatus = status)
    }

    fun consumeAchievementSignals() {
        mutableUiState.value = mutableUiState.value.copy(newAchievementIds = emptyList())
    }

    fun changeSettings(settings: AppSettings) {
        currentSettings = settings
        mutableUiState.value = mutableUiState.value.copy(settings = settings)
        viewModelScope.launch { settingsRepository.save(settings) }
    }

    fun selectMode(mode: GameMode) {
        mutableUiState.value = mutableUiState.value.copy(selectedMode = mode)
        changeSettings(currentSettings.copy(lastMode = mode))
    }

    fun goToModeSelect() {
        mutableUiState.value = mutableUiState.value.copy(
            screen = AppScreen.MODE_SELECT,
            pendingConfirmation = null,
            selectedMode = currentSettings.lastMode
        )
    }

    fun startSelectedMode() {
        val state = mutableUiState.value
        if (state.hasSavedGame) {
            mutableUiState.value = state.copy(pendingConfirmation = ConfirmationAction.START_NEW)
        } else {
            beginNewGame(state.selectedMode, currentSettings.difficulty, currentSettings.wallsEnabled)
        }
    }

    private fun beginNewGame(mode: GameMode, difficulty: com.ahmedzayed.zasnake.game.Difficulty, walls: Boolean) {
        engine.startNew(mode, difficulty, walls, deviceId)
        currentStats = currentStats.editDevice(deviceId) { it.copy(games = it.games + 1) }
        val unlocked = evaluateAchievements(currentStats)
        currentStats = unlocked.first
        mutableUiState.value = mutableUiState.value.copy(
            screen = AppScreen.GAME,
            selectedMode = mode,
            hasSavedGame = true,
            pendingConfirmation = null,
            stats = currentStats,
            newAchievementIds = unlocked.second
        )
        persistStats()
        persistGame()
    }

    fun resumeSavedGame() {
        viewModelScope.launch {
            val rawSaved = withContext(Dispatchers.IO) { localStore.loadGame() }
            val saved = effectiveRound(rawSaved, currentTombstone)
            if (rawSaved != null && saved == null) localStore.clearGame(expectedSessionId = rawSaved.sessionId)
            if (saved == null) {
                mutableUiState.value = mutableUiState.value.copy(hasSavedGame = false)
                return@launch
            }
            engine.restore(saved)
            engine.resume()
            mutableUiState.value = mutableUiState.value.copy(
                screen = AppScreen.GAME,
                selectedMode = saved.mode,
                hasSavedGame = true,
                pendingConfirmation = null
            )
        }
    }

    fun requestPause() {
        if (mutableUiState.value.screen != AppScreen.GAME) return
        flushPlayTime()
        engine.pause()
        mutableUiState.value = mutableUiState.value.copy(screen = AppScreen.PAUSE, hasSavedGame = true)
        persistGame()
        persistStats()
    }

    fun resumeFromPause() {
        if (mutableUiState.value.screen != AppScreen.PAUSE) return
        engine.resume()
        mutableUiState.value = mutableUiState.value.copy(screen = AppScreen.GAME, pendingConfirmation = null)
    }

    fun requestRestart() {
        mutableUiState.value = mutableUiState.value.copy(pendingConfirmation = ConfirmationAction.RESTART)
    }

    fun goHomeKeepingRound() {
        if (!engine.isGameOver && engine.snake.isNotEmpty()) persistGame()
        engine.pause()
        mutableUiState.value = mutableUiState.value.copy(
            screen = AppScreen.HOME,
            pendingConfirmation = null,
            hasSavedGame = !engine.isGameOver && engine.snake.isNotEmpty()
        )
    }

    fun goHomeAfterGameOver() {
        mutableUiState.value = mutableUiState.value.copy(
            screen = AppScreen.HOME,
            pendingConfirmation = null,
            hasSavedGame = false
        )
    }

    fun replayAfterGameOver() {
        beginNewGame(engine.mode, engine.difficulty, engine.wallsEnabled)
    }

    fun showSettings() {
        mutableUiState.value = mutableUiState.value.copy(screen = AppScreen.SETTINGS, pendingConfirmation = null)
    }

    fun showStatistics() {
        mutableUiState.value = mutableUiState.value.copy(screen = AppScreen.STATISTICS, pendingConfirmation = null)
    }

    fun navigateHomeFromSubscreen() {
        mutableUiState.value = mutableUiState.value.copy(screen = AppScreen.HOME, pendingConfirmation = null)
    }

    fun handleBack() {
        when (mutableUiState.value.screen) {
            AppScreen.GAME -> requestPause()
            AppScreen.PAUSE -> goHomeKeepingRound()
            AppScreen.GAME_OVER -> goHomeAfterGameOver()
            AppScreen.MODE_SELECT, AppScreen.SETTINGS, AppScreen.STATISTICS -> navigateHomeFromSubscreen()
            AppScreen.HOME -> mutableUiState.value = mutableUiState.value.copy(pendingConfirmation = ConfirmationAction.EXIT_APP)
        }
    }

    fun dismissConfirmation() {
        mutableUiState.value = mutableUiState.value.copy(pendingConfirmation = null)
    }

    fun confirmPendingAction() {
        when (mutableUiState.value.pendingConfirmation) {
            ConfirmationAction.START_NEW -> {
                val selected = mutableUiState.value.selectedMode
                viewModelScope.launch {
                    localStore.clearGame()
                    beginNewGame(selected, currentSettings.difficulty, currentSettings.wallsEnabled)
                }
            }
            ConfirmationAction.RESTART -> {
                beginNewGame(engine.mode, engine.difficulty, engine.wallsEnabled)
            }
            ConfirmationAction.EXIT_APP, null -> dismissConfirmation()
        }
    }

    fun onAppBackgrounded() {
        if (mutableUiState.value.screen == AppScreen.GAME) requestPause()
    }

    fun onFrame(frameDeltaMs: Long) {
        if (mutableUiState.value.screen != AppScreen.GAME || !engine.isRunning) return
        val dt = frameDeltaMs.coerceIn(0L, 250L)
        val events = engine.update(dt)
        if (dt > 0) pendingPlayMs += dt

        events.forEach { event ->
            when (event.type) {
                EngineEvent.Type.FOOD -> {
                    currentStats = currentStats.editDevice(deviceId) { it.copy(food = it.food + 1) }
                    playFeedback(ToneGenerator.TONE_PROP_BEEP, 55L)
                }
                EngineEvent.Type.GOLDEN -> {
                    currentStats = currentStats.editDevice(deviceId) { it.copy(food = it.food + 1, golden = it.golden + 1) }
                    playFeedback(ToneGenerator.TONE_PROP_ACK, 90L)
                }
                EngineEvent.Type.SCISSORS -> {
                    currentStats = currentStats.editDevice(deviceId) { it.copy(scissors = it.scissors + 1) }
                    playFeedback(ToneGenerator.TONE_PROP_BEEP2, 85L)
                }
                EngineEvent.Type.GAME_OVER -> playFeedback(ToneGenerator.TONE_PROP_NACK, 180L)
            }
        }

        if (pendingPlayMs >= 10_000L) {
            val fullBlocks = pendingPlayMs / 10_000L
            val addMs = fullBlocks * 10_000L
            pendingPlayMs -= addMs
            currentStats = currentStats.editDevice(deviceId) { it.copy(playMs = it.playMs + addMs) }
        }

        currentStats = currentStats.copy(
            bestScore = maxOf(currentStats.bestScore, engine.score),
            bestTimeScore = maxOf(currentStats.bestTimeScore, if (engine.mode == GameMode.TIME_ATTACK) engine.score else 0),
            maxLength = maxOf(currentStats.maxLength, engine.snake.size)
        )
        val unlocked = evaluateAchievements(currentStats)
        currentStats = unlocked.first

        if (engine.isGameOver) {
            flushPlayTime()
            engine.pause()
            mutableUiState.value = mutableUiState.value.copy(
                screen = AppScreen.GAME_OVER,
                frameVersion = mutableUiState.value.frameVersion + 1,
                stats = currentStats,
                hasSavedGame = false,
                newAchievementIds = mutableUiState.value.newAchievementIds + unlocked.second
            )
            persistStats()
            val endedSessionId = engine.sessionId
            val tombstone = RoundTombstone(
                completedSessionId = endedSessionId,
                completedAtEpochMs = System.currentTimeMillis(),
                writerDeviceId = deviceId
            )
            currentTombstone = newestTombstone(currentTombstone, tombstone)
            viewModelScope.launch {
                localStore.saveRoundTombstone(tombstone)
                localStore.clearGame(expectedSessionId = endedSessionId)
            }
            return
        }

        localSaveElapsedMs += dt
        lastStatsSaveElapsedMs += dt
        if (localSaveElapsedMs >= 1_000L) {
            localSaveElapsedMs %= 1_000L
            persistGame()
        }
        if (lastStatsSaveElapsedMs >= 3_000L) {
            lastStatsSaveElapsedMs %= 3_000L
            persistStats()
        }
        mutableUiState.value = mutableUiState.value.copy(
            frameVersion = mutableUiState.value.frameVersion + 1,
            stats = currentStats,
            hasSavedGame = true,
            newAchievementIds = mutableUiState.value.newAchievementIds + unlocked.second
        )
    }

    private fun flushPlayTime() {
        if (pendingPlayMs > 0L && deviceId.isNotBlank()) {
            val amount = pendingPlayMs
            pendingPlayMs = 0L
            currentStats = currentStats.editDevice(deviceId) { it.copy(playMs = it.playMs + amount) }
        }
    }

    @Suppress("DEPRECATION")
    private fun playFeedback(tone: Int, durationMs: Long) {
        if (currentSettings.soundEnabled) runCatching { toneGenerator?.startTone(tone, durationMs.toInt()) }
        if (currentSettings.hapticsEnabled) runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                vibrator?.vibrate(durationMs)
            }
        }
    }

    private fun evaluateAchievements(stats: PlayerStats): Pair<PlayerStats, List<String>> {
        val newlyUnlocked = ACHIEVEMENTS.filter { achievement ->
            achievement.id !in stats.unlockedAchievements && achievement.progress(stats) >= achievement.target
        }.map { it.id }
        return stats.copy(unlockedAchievements = stats.unlockedAchievements + newlyUnlocked) to newlyUnlocked
    }

    private fun persistGame() {
        if (deviceId.isBlank() || engine.isGameOver || engine.snake.isEmpty()) return
        val snapshot = engine.toSnapshot(deviceId)
        viewModelScope.launch { localStore.saveGame(snapshot) }
    }

    private fun persistStats() {
        if (deviceId.isBlank()) return
        val current = currentStats
        mutableUiState.value = mutableUiState.value.copy(stats = current)
        viewModelScope.launch { localStore.saveStats(current) }
    }

    fun updateCloudStats(cloudStats: PlayerStats) {
        // Commutative, idempotent per-install counters; round conflict is handled separately.
        currentStats = currentStats.merge(cloudStats)
        val unlocked = evaluateAchievements(currentStats)
        currentStats = unlocked.first
        mutableUiState.value = mutableUiState.value.copy(
            stats = currentStats,
            newAchievementIds = mutableUiState.value.newAchievementIds + unlocked.second
        )
        persistStats()
    }

    fun syncWithCloud(bridge: PlayGamesBridge) {
        if (!bridge.isConfigured()) {
            setCloudStatus("not_configured")
            return
        }
        if (cloudSyncInProgress) return
        cloudSyncInProgress = true
        setCloudStatus("syncing")
        bridge.loadBundle { result ->
            when (result) {
                CloudLoadResult.Unavailable -> {
                    cloudSyncInProgress = false
                    setCloudStatus("offline")
                }
                is CloudLoadResult.Payload -> {
                    viewModelScope.launch {
                        val rawLocalRound = localStore.loadGame()
                        val localStats = localStore.loadStats()
                        currentStats = currentStats.merge(localStats)
                        val cloudBundle = decodeCloudBundle(result.json)
                        if (cloudBundle != null) {
                            currentStats = currentStats.merge(cloudBundle.stats)
                            currentTombstone = newestTombstone(currentTombstone, cloudBundle.roundTombstone)
                        }
                        localStore.loadRoundTombstone()?.let { currentTombstone = newestTombstone(currentTombstone, it) }
                        currentTombstone?.let { localStore.saveRoundTombstone(it) }
                        val localRound = effectiveRound(rawLocalRound, currentTombstone)
                        val cloudRound = effectiveRound(cloudBundle?.game, currentTombstone)
                        if (rawLocalRound != null && localRound == null) localStore.clearGame(expectedSessionId = rawLocalRound.sessionId)
                        handleRoundCandidates(localRound, cloudRound, currentStats, bridge, fromPgsConflict = false)
                    }
                }
                is CloudLoadResult.Conflict -> {
                    viewModelScope.launch {
                        val rawLocalRound = localStore.loadGame()
                        val localStats = localStore.loadStats()
                        val serverBundle = decodeCloudBundle(result.serverJson) ?: CloudBundle(PlayerStats(), null)
                        val deviceBundle = decodeCloudBundle(result.deviceJson) ?: CloudBundle(PlayerStats(), null)
                        currentStats = currentStats.merge(localStats).merge(serverBundle.stats).merge(deviceBundle.stats)
                        currentTombstone = newestTombstone(currentTombstone, serverBundle.roundTombstone)
                        currentTombstone = newestTombstone(currentTombstone, deviceBundle.roundTombstone)
                        localStore.loadRoundTombstone()?.let { currentTombstone = newestTombstone(currentTombstone, it) }
                        currentTombstone?.let { localStore.saveRoundTombstone(it) }
                        // The user's explicit choice is about the active round. Stats always merge.
                        val localCandidate = effectiveRound(rawLocalRound ?: deviceBundle.game, currentTombstone)
                        val serverCandidate = effectiveRound(serverBundle.game, currentTombstone)
                        handleRoundCandidates(localCandidate, serverCandidate, currentStats, bridge, fromPgsConflict = true)
                    }
                }
            }
        }
    }

    private suspend fun handleRoundCandidates(
        localRound: GameSnapshot?,
        cloudRound: GameSnapshot?,
        mergedStats: PlayerStats,
        bridge: PlayGamesBridge,
        fromPgsConflict: Boolean
    ) {
        currentStats = evaluateAchievements(mergedStats).first
        var safeLocalRound = localRound
        if (mutableUiState.value.screen == AppScreen.GAME) {
            requestPause()
            safeLocalRound = engine.toSnapshot(deviceId)
            localStore.saveGame(safeLocalRound)
        }
        val conflictNeeded = roundsConflict(safeLocalRound, cloudRound)
        if (conflictNeeded) {
            mutableUiState.value = mutableUiState.value.copy(
                conflict = PendingRoundConflict(safeLocalRound, cloudRound, currentStats, fromPgsConflict),
                stats = currentStats,
                cloudStatus = "conflict",
                hasSavedGame = safeLocalRound != null
            )
            persistStats()
            cloudSyncInProgress = false
            return
        }
        val (chosenRound, choseCloud) = chooseRound(safeLocalRound, cloudRound)
        commitMergedBundle(chosenRound, currentStats, bridge, fromPgsConflict, chooseCloudBase = choseCloud)
    }

    private fun newestTombstone(a: RoundTombstone?, b: RoundTombstone?): RoundTombstone? = when {
        a == null -> b
        b == null -> a
        b.completedAtEpochMs > a.completedAtEpochMs -> b
        else -> a
    }

    /** A completion marker only suppresses the same session when it predates that session's
     * latest saved checkpoint. A genuinely newer checkpoint on another device can still win. */
    private fun effectiveRound(snapshot: GameSnapshot?, tombstone: RoundTombstone?): GameSnapshot? {
        if (snapshot == null || tombstone == null) return snapshot
        return if (snapshot.sessionId == tombstone.completedSessionId &&
            tombstone.completedAtEpochMs >= snapshot.savedAtEpochMs) null else snapshot
    }

    private fun roundsConflict(local: GameSnapshot?, cloud: GameSnapshot?): Boolean {
        if (local == null || cloud == null) return false
        if (sameRoundState(local, cloud)) return false
        // A single writer can safely take its newest durable checkpoint. Different writers may
        // have advanced the same session independently, so the player chooses at a safe point.
        return local.writerDeviceId != cloud.writerDeviceId
    }

    private fun sameRoundState(a: GameSnapshot, b: GameSnapshot): Boolean =
        a.sessionId == b.sessionId && a.mode == b.mode && a.snake == b.snake &&
            a.direction == b.direction && a.nextDirection == b.nextDirection && a.food == b.food &&
            a.specials == b.specials && a.score == b.score && a.timeRemainingMs == b.timeRemainingMs

    private fun chooseRound(local: GameSnapshot?, cloud: GameSnapshot?): Pair<GameSnapshot?, Boolean> {
        if (local == null) return cloud to (cloud != null)
        if (cloud == null) return local to false
        if (sameRoundState(local, cloud)) {
            val useCloud = cloud.savedAtEpochMs > local.savedAtEpochMs
            return (if (useCloud) cloud else local) to useCloud
        }
        if (local.writerDeviceId == cloud.writerDeviceId) {
            val useCloud = cloud.revision > local.revision ||
                (cloud.revision == local.revision && cloud.savedAtEpochMs > local.savedAtEpochMs)
            return (if (useCloud) cloud else local) to useCloud
        }
        // This path is called only when there is no user-resolvable active-round conflict.
        val useCloud = cloud.savedAtEpochMs > local.savedAtEpochMs
        return (if (useCloud) cloud else local) to useCloud
    }

    private fun commitMergedBundle(
        chosenRound: GameSnapshot?,
        mergedStats: PlayerStats,
        bridge: PlayGamesBridge,
        fromPgsConflict: Boolean,
        chooseCloudBase: Boolean
    ) {
        currentStats = mergedStats
        viewModelScope.launch {
            if (chosenRound == null) {
                localStore.clearGame()
                engine.pause()
            } else {
                localStore.saveGame(chosenRound)
                engine.restore(chosenRound)
            }
            localStore.saveStats(currentStats)
            mutableUiState.value = mutableUiState.value.copy(
                screen = if (mutableUiState.value.screen == AppScreen.GAME) AppScreen.PAUSE else mutableUiState.value.screen,
                stats = currentStats,
                hasSavedGame = chosenRound != null,
                selectedMode = chosenRound?.mode ?: currentSettings.lastMode,
                conflict = null,
                cloudStatus = "syncing"
            )
            val payload = encodeCloudBundle(CloudBundle(currentStats, chosenRound, currentTombstone))
            val complete: (Boolean) -> Unit = { success ->
                cloudSyncInProgress = false
                setCloudStatus(if (success) "synced" else "sync_error")
            }
            if (fromPgsConflict) bridge.resolvePendingConflict(payload, chooseCloudBase, complete)
            else bridge.saveBundle(payload, complete)
        }
    }

    fun uploadCloudProgress(bridge: PlayGamesBridge) {
        if (!bridge.isConfigured() || cloudSyncInProgress) return
        val status = mutableUiState.value.cloudStatus
        if (status !in listOf("synced", "connected", "sync_error")) return
        cloudSyncInProgress = true
        viewModelScope.launch {
            val round = if (!engine.isGameOver && mutableUiState.value.hasSavedGame) {
                localStore.loadGame() ?: if (engine.snake.isNotEmpty()) engine.toSnapshot(deviceId) else null
            } else null
            val latestStats = localStore.loadStats().merge(currentStats)
            currentStats = latestStats
            val localTombstone = localStore.loadRoundTombstone()
            currentTombstone = newestTombstone(currentTombstone, localTombstone)
            val payload = encodeCloudBundle(CloudBundle(latestStats, effectiveRound(round, currentTombstone), currentTombstone))
            bridge.saveBundle(payload) { success ->
                cloudSyncInProgress = false
                if (success) setCloudStatus("synced") else {
                    // Don't choose an active round silently if Play Games reports a conflict.
                    syncWithCloud(bridge)
                }
            }
        }
    }

    fun resolveRoundConflict(useCloud: Boolean): ConflictResolution? {
        val conflict = mutableUiState.value.conflict ?: return null
        currentStats = evaluateAchievements(conflict.mergedStats.merge(currentStats)).first
        val selected = if (useCloud) conflict.cloudSnapshot else conflict.localSnapshot
        // Stamp the explicit user choice as the latest local checkpoint so delayed writes from
        // before the dialog cannot overwrite the round the player just chose.
        val chosen = selected?.copy(
            revision = selected.revision + 1,
            writerDeviceId = deviceId,
            savedAtEpochMs = System.currentTimeMillis()
        )
        val payload = encodeCloudBundle(CloudBundle(currentStats, chosen, currentTombstone))
        if (chosen == null) {
            viewModelScope.launch { localStore.clearGame() }
        } else {
            engine.restore(chosen)
            viewModelScope.launch { localStore.saveGame(chosen, forceReplace = true) }
        }
        viewModelScope.launch { localStore.saveStats(currentStats) }
        mutableUiState.value = mutableUiState.value.copy(
            conflict = null,
            hasSavedGame = chosen != null,
            selectedMode = chosen?.mode ?: currentSettings.lastMode,
            stats = currentStats,
            cloudStatus = "syncing",
            screen = AppScreen.HOME
        )
        cloudSyncInProgress = true
        return ConflictResolution(payload, conflict.fromPgsConflict, chooseServerSnapshot = useCloud)
    }

    fun cloudWriteFinished(success: Boolean) {
        cloudSyncInProgress = false
        setCloudStatus(if (success) "synced" else "sync_error")
    }

    fun showToast(message: String) {
        mutableUiState.value = mutableUiState.value.copy(toast = message)
    }

    fun clearToast() { mutableUiState.value = mutableUiState.value.copy(toast = null) }

    fun currentSnapshotForCloud(): GameSnapshot? = if (!engine.isGameOver && engine.snake.isNotEmpty() && mutableUiState.value.hasSavedGame) engine.toSnapshot(deviceId) else null

    override fun onCleared() {
        runCatching { toneGenerator?.release() }
        super.onCleared()
    }
}
