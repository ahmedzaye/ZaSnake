package com.ahmedzayed.zasnake

import android.app.Application
import android.os.Build
import android.os.SystemClock
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
import com.ahmedzayed.zasnake.language.DeviceLanguage
import com.ahmedzayed.zasnake.game.ACHIEVEMENTS
import com.ahmedzayed.zasnake.game.AppScreen
import com.ahmedzayed.zasnake.game.AppSettings
import com.ahmedzayed.zasnake.game.PlayerNamePolicy
import com.ahmedzayed.zasnake.game.AppUiState
import com.ahmedzayed.zasnake.game.ConfirmationAction
import com.ahmedzayed.zasnake.game.ConflictResolution
import com.ahmedzayed.zasnake.game.DeviceCounters
import com.ahmedzayed.zasnake.game.DailyChallenge
import com.ahmedzayed.zasnake.game.dailyUtcDate
import com.ahmedzayed.zasnake.game.recordDailyActivity
import com.ahmedzayed.zasnake.game.claimDailyChallenge
import com.ahmedzayed.zasnake.game.claimDailyLogin
import com.ahmedzayed.zasnake.game.touchDailyDate
import com.ahmedzayed.zasnake.game.EngineEvent
import com.ahmedzayed.zasnake.game.GameMode
import com.ahmedzayed.zasnake.game.GameTheme
import com.ahmedzayed.zasnake.game.FREE_THEME_KEYS
import com.ahmedzayed.zasnake.game.GameSnapshot
import com.ahmedzayed.zasnake.game.PendingRoundConflict
import com.ahmedzayed.zasnake.game.PlayerStats
import com.ahmedzayed.zasnake.game.ResumeGraceTimer
import com.ahmedzayed.zasnake.game.RoundTombstone
import com.ahmedzayed.zasnake.game.SnakeEngine
import com.ahmedzayed.zasnake.game.SnakeSkin
import com.ahmedzayed.zasnake.game.SnakeHeadStyle
import com.ahmedzayed.zasnake.game.SnakeBodyType
import com.ahmedzayed.zasnake.game.FREE_BODY_KEYS
import com.ahmedzayed.zasnake.game.FREE_HEAD_KEYS
import com.ahmedzayed.zasnake.game.FREE_SKIN_KEYS
import com.ahmedzayed.zasnake.game.VisualCue
import com.ahmedzayed.zasnake.platform.CloudLoadResult
import com.ahmedzayed.zasnake.audio.GameAudio
import com.ahmedzayed.zasnake.audio.SoundEffect
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
    private var activeDailyDate = dailyUtcDate()
    private var lastDailyClockCheck = 0L
    private var localSaveElapsedMs = 0L
    private var lastStatsSaveElapsedMs = 0L
    private var currentSettings = AppSettings()
    private var currentStats = PlayerStats()
    private var currentTombstone: RoundTombstone? = null
    private var cloudSyncInProgress = false
    private val audio = GameAudio(application)
    private var isForeground = true
    private var celebratedRecordThisRound = false
    private val resumeGraceTimer = ResumeGraceTimer()
    private val graceSecondsMutable = mutableStateOf(0)
    val resumeGraceSeconds: State<Int> = graceSecondsMutable

    /** Keep the board visible and physics frozen until timeout or an intentional direction press. */
    private fun beginResumeGrace() {
        engine.pause()
        resumeGraceTimer.begin(SystemClock.elapsedRealtime())
        graceSecondsMutable.value = 3
        updateMusic()
    }

    private fun clearResumeGrace(resumeEngine: Boolean) {
        resumeGraceTimer.clear()
        graceSecondsMutable.value = 0
        if (resumeEngine && !engine.isGameOver) engine.resume()
        updateMusic()
    }
    private val vibrator: Vibrator? = getApplication<Application>().getSystemService(Vibrator::class.java)

    init {
        viewModelScope.launch {
            currentSettings = settingsRepository.settings.first()
            audio.applySettings(currentSettings)
            deviceId = settingsRepository.installationId()
            currentStats = localStore.loadStats().touchDailyDate(activeDailyDate)
            // Promote ownership from old local settings into the cloud-merged entitlement ledger.
            // On fresh installs only the three free colors are present in settings.
            val inheritedPremiumSkins = currentSettings.ownedSkinKeys - FREE_SKIN_KEYS
            val legacyEntitlementsAdded = inheritedPremiumSkins.any { it !in currentStats.purchasedSkinKeys }
            if (legacyEntitlementsAdded) {
                currentStats = currentStats.copy(unlockedSkins = currentStats.unlockedSkins + inheritedPremiumSkins)
                localStore.saveStats(currentStats)
            }
            // Promote any locally owned premium heads before syncing with Play Games.
            val inheritedHeads = currentSettings.ownedHeadKeys - FREE_HEAD_KEYS
            if (inheritedHeads.any { it !in currentStats.purchasedHeadKeys }) {
                currentStats = currentStats.copy(unlockedHeads = currentStats.unlockedHeads + inheritedHeads)
                localStore.saveStats(currentStats)
            }
            val inheritedBodies = currentSettings.ownedBodyKeys - FREE_BODY_KEYS
            if (inheritedBodies.any { it !in currentStats.purchasedBodyKeys }) {
                currentStats = currentStats.copy(unlockedBodies = currentStats.unlockedBodies + inheritedBodies)
                localStore.saveStats(currentStats)
            }
            currentSettings = currentSettings.copy(
                ownedHeadKeys = currentSettings.ownedHeadKeys + currentStats.purchasedHeadKeys + FREE_HEAD_KEYS,
                ownedBodyKeys = currentSettings.ownedBodyKeys + currentStats.purchasedBodyKeys + FREE_BODY_KEYS,
                ownedSkinKeys = currentSettings.ownedSkinKeys + currentStats.purchasedSkinKeys,
                ownedThemeKeys = currentSettings.ownedThemeKeys + currentStats.purchasedThemeKeys + FREE_THEME_KEYS
            )
            settingsRepository.save(currentSettings)
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
                dailyDate = activeDailyDate,
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

    /** React to Android locale changes only while the player has chosen Automatic. */
    fun followDeviceLanguage(language: String) {
        if (currentSettings.languageMode != DeviceLanguage.SYSTEM) return
        val normalized = DeviceLanguage.resolve(DeviceLanguage.SYSTEM, language)
        if (currentSettings.language == normalized) return
        currentSettings = currentSettings.copy(language = normalized)
        mutableUiState.value = mutableUiState.value.copy(
            settings = mutableUiState.value.settings.copy(language = normalized)
        )
        // Automatic language is derived on launch; no DataStore write is needed here.
    }

    fun changeSettings(settings: AppSettings) {
        if (settings.headStyle.key !in (currentSettings.ownedHeadKeys + currentStats.purchasedHeadKeys + FREE_HEAD_KEYS)) return
        if (settings.bodyType.key !in (currentSettings.ownedBodyKeys + currentStats.purchasedBodyKeys + FREE_BODY_KEYS)) return
        if (settings.theme != currentSettings.theme &&
            settings.theme.key !in (currentSettings.ownedThemeKeys + currentStats.purchasedThemeKeys + FREE_THEME_KEYS)) return
        val mode = DeviceLanguage.normalizePreference(settings.languageMode)
        val deviceLanguage = DeviceLanguage.fromLocale(getApplication<Application>().resources.configuration.locales[0])
        currentSettings = settings.copy(
            languageMode = mode,
            language = DeviceLanguage.resolve(mode, deviceLanguage),
            playerName = currentSettings.playerName,
            ownedThemeKeys = currentSettings.ownedThemeKeys + currentStats.purchasedThemeKeys + FREE_THEME_KEYS,
            ownedHeadKeys = currentSettings.ownedHeadKeys + currentStats.purchasedHeadKeys + FREE_HEAD_KEYS,
            ownedBodyKeys = currentSettings.ownedBodyKeys + currentStats.purchasedBodyKeys + FREE_BODY_KEYS
        )
        audio.applySettings(currentSettings)
        mutableUiState.value = mutableUiState.value.copy(settings = currentSettings)
        viewModelScope.launch { settingsRepository.save(currentSettings) }
    }

    /** Only the local display nickname changes; it is not the Play Games profile name. */
    fun updatePlayerName(candidate: String): Boolean {
        if (!mutableUiState.value.ready) return false
        val normalized = PlayerNamePolicy.normalized(candidate) ?: return false
        if (normalized != currentSettings.playerName) {
            currentSettings = currentSettings.copy(playerName = normalized)
            mutableUiState.value = mutableUiState.value.copy(settings = currentSettings)
            viewModelScope.launch { settingsRepository.save(currentSettings) }
        }
        return true
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
        refreshDailyDate(force = true)
        clearResumeGrace(false)
        engine.startNew(mode, difficulty, walls, deviceId)
        celebratedRecordThisRound = false
        currentStats = currentStats.editDevice(deviceId) { it.copy(games = it.games + 1) }
        val unlocked = evaluateAchievements(currentStats)
        currentStats = unlocked.first
        mutableUiState.value = mutableUiState.value.copy(
            screen = AppScreen.GAME,
            visualCues = emptyList(),
            newRecordThisRound = false,
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
            celebratedRecordThisRound = false
            if (!engine.pendingCollision) beginResumeGrace()
            mutableUiState.value = mutableUiState.value.copy(
                screen = if (engine.pendingCollision) AppScreen.GAME_OVER else AppScreen.GAME,
                visualCues = emptyList(),
                newRecordThisRound = false,
                selectedMode = saved.mode,
                hasSavedGame = true,
                pendingConfirmation = null
            )
        }
    }

    fun requestPause() {
        if (mutableUiState.value.screen != AppScreen.GAME) return
        flushPlayTime()
        clearResumeGrace(false)
        engine.pause()
        mutableUiState.value = mutableUiState.value.copy(screen = AppScreen.PAUSE, hasSavedGame = true)
        persistGame()
        persistStats()
    }

    fun resumeFromPause() {
        if (mutableUiState.value.screen != AppScreen.PAUSE) return
        mutableUiState.value = mutableUiState.value.copy(screen = AppScreen.GAME, pendingConfirmation = null)
        beginResumeGrace()
    }

    fun requestRestart() {
        mutableUiState.value = mutableUiState.value.copy(pendingConfirmation = ConfirmationAction.RESTART)
    }

    fun goHomeKeepingRound() {
        if (!engine.isGameOver && engine.snake.isNotEmpty()) persistGame()
        clearResumeGrace(false)
        engine.pause()
        mutableUiState.value = mutableUiState.value.copy(
            screen = AppScreen.HOME,
            pendingConfirmation = null,
            hasSavedGame = !engine.isGameOver && engine.snake.isNotEmpty()
        )
    }

    fun goHomeAfterGameOver() {
        if (engine.pendingCollision) settlePendingCollision()
        mutableUiState.value = mutableUiState.value.copy(
            screen = AppScreen.HOME,
            pendingConfirmation = null,
            hasSavedGame = false
        )
    }

    fun replayAfterGameOver() {
        if (engine.pendingCollision) settlePendingCollision()
        beginNewGame(engine.mode, engine.difficulty, engine.wallsEnabled)
    }

    fun showSettings() {
        mutableUiState.value = mutableUiState.value.copy(screen = AppScreen.SETTINGS, pendingConfirmation = null)
    }

    fun showStatistics(openAchievements: Boolean = false) {
        mutableUiState.value = mutableUiState.value.copy(
            screen = AppScreen.STATISTICS, pendingConfirmation = null,
            statisticsOpenAchievements = openAchievements
        )
    }

    fun refreshDailyRewards() = refreshDailyDate(force = true)

    fun showDailyRewards() {
        refreshDailyDate(force = true)
        mutableUiState.value = mutableUiState.value.copy(screen = AppScreen.DAILY_REWARDS, pendingConfirmation = null)
    }

    private fun refreshDailyDate(force: Boolean = false) {
        val currentTime = System.currentTimeMillis()
        if (!force && currentTime - lastDailyClockCheck in 0..9_999L) return
        lastDailyClockCheck = currentTime
        val today = dailyUtcDate(currentTime)
        if (today != activeDailyDate) {
            activeDailyDate = today
            if (mutableUiState.value.ready) {
                currentStats = currentStats.touchDailyDate(today)
                mutableUiState.value = mutableUiState.value.copy(dailyDate = today, stats = currentStats)
                persistStats()
            }
        }
    }

    fun claimDailyChallengeReward(challengeId: String): Boolean {
        if (!mutableUiState.value.ready || deviceId.isBlank()) return false
        refreshDailyDate(force = true)
        val challenge = DailyChallenge.entries.firstOrNull { it.id == challengeId } ?: return false
        val updated = currentStats.claimDailyChallenge(activeDailyDate, challenge)
        if (updated == currentStats) return false
        currentStats = updated
        mutableUiState.value = mutableUiState.value.copy(stats = updated, toast =
            if (currentSettings.language == "ar") "استلمت ${challenge.coins} عملة!" else "Claimed ${challenge.coins} coins!")
        audio.play(SoundEffect.CLAIM)
        persistStats()
        return true
    }

    fun claimDailyLoginReward(): Boolean {
        if (!mutableUiState.value.ready || deviceId.isBlank()) return false
        refreshDailyDate(force = true)
        val updated = currentStats.claimDailyLogin(activeDailyDate)
        if (updated == currentStats) return false
        val coins = updated.coins - currentStats.coins
        currentStats = updated
        mutableUiState.value = mutableUiState.value.copy(stats = updated, toast =
            if (currentSettings.language == "ar") "مكافأة الدخول: $coins عملة!" else "Daily login: $coins coins!")
        audio.play(SoundEffect.CLAIM)
        persistStats()
        return true
    }

    fun showShop() {
        mutableUiState.value = mutableUiState.value.copy(screen = AppScreen.SHOP, pendingConfirmation = null)
    }

    fun equipSkin(skin: SnakeSkin) {
        if (skin.key !in currentSettings.ownedSkinKeys) return
        currentSettings = currentSettings.copy(skin = skin)
        mutableUiState.value = mutableUiState.value.copy(
            settings = currentSettings,
            toast = if (currentSettings.language == "ar") "تم اختيار شكل ${skin.key}" else "Skin equipped"
        )
        viewModelScope.launch { settingsRepository.save(currentSettings) }
    }

    fun purchaseSkin(skin: SnakeSkin): Boolean {
        if (!mutableUiState.value.ready || deviceId.isBlank() || skin.isFree || skin.key in currentSettings.ownedSkinKeys) return false
        val updated = currentStats.purchaseSkin(skin)
        if (updated === currentStats) {
            if (skin.key !in currentStats.purchasedSkinKeys && currentStats.coins < skin.priceCoins.toLong()) {
                mutableUiState.value = mutableUiState.value.copy(toast =
                    if (currentSettings.language == "ar") "العملات غير كافية" else "Not enough coins")
            }
            return false
        }
        currentStats = updated
        currentSettings = currentSettings.copy(skin = skin, ownedSkinKeys = currentSettings.ownedSkinKeys + skin.key)
        mutableUiState.value = mutableUiState.value.copy(stats = currentStats, settings = currentSettings,
            toast = if (currentSettings.language == "ar") "تم شراء الشكل بنجاح!" else "Skin purchased!")
        viewModelScope.launch {
            localStore.saveStats(currentStats)
            settingsRepository.save(currentSettings)
        }
        return true
    }

    fun equipHead(head: SnakeHeadStyle) {
        if (head.key !in (currentSettings.ownedHeadKeys + currentStats.purchasedHeadKeys + FREE_HEAD_KEYS)) return
        currentSettings = currentSettings.copy(headStyle = head)
        mutableUiState.value = mutableUiState.value.copy(settings = currentSettings,
            toast = if (currentSettings.language == "ar") "تم تجهيز الرأس" else "Head equipped")
        viewModelScope.launch { settingsRepository.save(currentSettings) }
    }

    fun purchaseHead(head: SnakeHeadStyle): Boolean {
        if (!mutableUiState.value.ready || deviceId.isBlank() || head.isFree ||
            head.key in (currentSettings.ownedHeadKeys + currentStats.purchasedHeadKeys)) return false
        val updated = currentStats.purchaseHead(head)
        if (updated === currentStats) {
            if (currentStats.coins < head.priceCoins.toLong()) {
                mutableUiState.value = mutableUiState.value.copy(toast =
                    if (currentSettings.language == "ar") "العملات غير كافية" else "Not enough coins")
            }
            return false
        }
        currentStats = updated
        currentSettings = currentSettings.copy(
            headStyle = head, ownedHeadKeys = currentSettings.ownedHeadKeys + head.key + FREE_HEAD_KEYS
        )
        mutableUiState.value = mutableUiState.value.copy(stats = currentStats, settings = currentSettings,
            toast = if (currentSettings.language == "ar") "تم شراء الرأس وتجهيزه!" else "Head purchased and equipped!")
        viewModelScope.launch {
            localStore.saveStats(currentStats)
            settingsRepository.save(currentSettings)
        }
        return true
    }

    fun equipBody(body: SnakeBodyType) {
        if (body.key !in (currentSettings.ownedBodyKeys + currentStats.purchasedBodyKeys + FREE_BODY_KEYS)) return
        currentSettings = currentSettings.copy(bodyType = body)
        mutableUiState.value = mutableUiState.value.copy(settings = currentSettings,
            toast = if (currentSettings.language == "ar") "تم تجهيز نوع الجسم" else "Body equipped")
        viewModelScope.launch { settingsRepository.save(currentSettings) }
    }

    fun purchaseBody(body: SnakeBodyType): Boolean {
        if (!mutableUiState.value.ready || deviceId.isBlank() || body.isFree ||
            body.key in (currentSettings.ownedBodyKeys + currentStats.purchasedBodyKeys)) return false
        val updated = currentStats.purchaseBody(body)
        if (updated === currentStats) {
            if (currentStats.coins < body.priceCoins.toLong()) {
                mutableUiState.value = mutableUiState.value.copy(toast =
                    if (currentSettings.language == "ar") "العملات غير كافية" else "Not enough coins")
            }
            return false
        }
        currentStats = updated
        currentSettings = currentSettings.copy(
            bodyType = body, ownedBodyKeys = currentSettings.ownedBodyKeys + body.key + FREE_BODY_KEYS
        )
        mutableUiState.value = mutableUiState.value.copy(stats = currentStats, settings = currentSettings,
            toast = if (currentSettings.language == "ar") "تم شراء الجسم وتجهيزه!" else "Body purchased and equipped!")
        viewModelScope.launch {
            localStore.saveStats(currentStats)
            settingsRepository.save(currentSettings)
        }
        return true
    }

    fun purchaseTheme(theme: GameTheme): Boolean {
        if (!mutableUiState.value.ready || deviceId.isBlank() || theme.isFree) return false
        val updated = currentStats.purchaseTheme(theme)
        if (updated === currentStats) {
            if (theme.key !in currentStats.purchasedThemeKeys && currentStats.coins < theme.priceCoins.toLong()) {
                mutableUiState.value = mutableUiState.value.copy(toast =
                    if (currentSettings.language == "ar") "العملات غير كافية" else "Not enough coins")
            }
            return false
        }
        currentStats = updated
        currentSettings = currentSettings.copy(
            theme = theme,
            ownedThemeKeys = currentSettings.ownedThemeKeys + theme.key + FREE_THEME_KEYS
        )
        mutableUiState.value = mutableUiState.value.copy(
            stats = currentStats,
            settings = currentSettings,
            toast = if (currentSettings.language == "ar") "تم شراء الثيم وتفعيله!" else "Theme purchased and applied!"
        )
        viewModelScope.launch {
            localStore.saveStats(currentStats)
            settingsRepository.save(currentSettings)
        }
        return true
    }

    /** Manual reward claim; no coins are granted merely by completing an achievement. */
    fun claimAchievementReward(achievementId: String): Boolean {
        if (!mutableUiState.value.ready || deviceId.isBlank()) return false
        val achievement = ACHIEVEMENTS.firstOrNull { it.id == achievementId } ?: return false
        val updated = currentStats.claimAchievementReward(achievement)
        if (updated === currentStats) return false
        currentStats = updated
        mutableUiState.value = mutableUiState.value.copy(
            stats = updated,
            toast = if (currentSettings.language == "ar")
                "استلمت ${achievement.rewardCoins} عملة!" else "Claimed ${achievement.rewardCoins} coins!"
        )
        audio.play(SoundEffect.CLAIM)
        persistStats()
        return true
    }

    fun navigateHomeFromSubscreen() {
        mutableUiState.value = mutableUiState.value.copy(screen = AppScreen.HOME, pendingConfirmation = null)
    }

    fun handleBack() {
        when (mutableUiState.value.screen) {
            AppScreen.GAME -> requestPause()
            AppScreen.PAUSE -> goHomeKeepingRound()
            AppScreen.GAME_OVER -> goHomeAfterGameOver()
            AppScreen.MODE_SELECT, AppScreen.SETTINGS, AppScreen.STATISTICS, AppScreen.SHOP, AppScreen.DAILY_REWARDS -> navigateHomeFromSubscreen()
            AppScreen.HOME -> mutableUiState.value = mutableUiState.value.copy(pendingConfirmation = ConfirmationAction.EXIT_APP)
        }
    }

    fun dismissConfirmation() {
        mutableUiState.value = mutableUiState.value.copy(pendingConfirmation = null)
    }

    fun confirmPendingAction() {
        when (mutableUiState.value.pendingConfirmation) {
            ConfirmationAction.START_NEW -> {
                if (engine.pendingCollision) settlePendingCollision()
                val selected = mutableUiState.value.selectedMode
                viewModelScope.launch {
                    localStore.clearGame()
                    beginNewGame(selected, currentSettings.difficulty, currentSettings.wallsEnabled)
                }
            }
            ConfirmationAction.RESTART -> {
                if (engine.pendingCollision) settlePendingCollision()
                beginNewGame(engine.mode, engine.difficulty, engine.wallsEnabled)
            }
            ConfirmationAction.EXIT_APP, null -> dismissConfirmation()
        }
    }

    fun onAppBackgrounded() {
        isForeground = false
        audio.syncMusic(false)
        if (mutableUiState.value.screen == AppScreen.GAME) requestPause()
    }

    fun onAppForegrounded() {
        refreshDailyDate(force = true)
        isForeground = true
        updateMusic()
    }

    /** Called from the Compose screen observer; never starts music outside an active round. */
    fun updateMusic() {
        audio.applySettings(currentSettings)
        audio.syncMusic(isForeground && mutableUiState.value.screen == AppScreen.GAME &&
            engine.isRunning && graceSecondsMutable.value == 0)
    }

    fun requestDirection(direction: com.ahmedzayed.zasnake.game.Direction) {
        if (mutableUiState.value.screen != AppScreen.GAME || engine.isGameOver) return
        if (graceSecondsMutable.value > 0) {
            if (direction.isReverseOf(engine.direction)) return
            clearResumeGrace(true)
        }
        if (!engine.isRunning) return
        if (direction != engine.nextDirection && !direction.isReverseOf(engine.direction)) {
            audio.play(SoundEffect.TURN)
        }
        engine.requestDirection(direction)
    }

    /** Rewarded flow: after ad confirmation for older players, or free age-safe action for children. */
    fun reviveAfterReward(expectedSessionId: String): Boolean {
        if (expectedSessionId != engine.sessionId || mutableUiState.value.screen != AppScreen.GAME_OVER) return false
        if (!engine.reviveFromCollision()) return false
        clearResumeGrace(false)
        mutableUiState.value = mutableUiState.value.copy(
            screen = AppScreen.GAME, hasSavedGame = true, visualCues = emptyList(),
            frameVersion = mutableUiState.value.frameVersion + 1
        )
        persistGame()
        updateMusic()
        return true
    }

    /** If the player declines their sole revive, complete the round exactly once. */
    fun declineRevive() {
        if (mutableUiState.value.screen == AppScreen.GAME_OVER && engine.pendingCollision) settlePendingCollision()
    }

    fun doubleCoinsAfterReward(expectedSessionId: String): Boolean {
        if (mutableUiState.value.screen != AppScreen.GAME_OVER ||
            engine.pendingCollision || expectedSessionId != engine.sessionId ||
            engine.mode != GameMode.CLASSIC) return false
        val updated = currentStats.doubleRoundCoins(expectedSessionId)
        if (updated == currentStats) return false
        currentStats = updated
        mutableUiState.value = mutableUiState.value.copy(
            stats = updated,
            toast = if (currentSettings.language == "ar") "تمت مضاعفة عملات الجولة!" else "Round coins doubled!"
        )
        persistStats()
        return true
    }

    fun rewardAdUnavailable() {
        mutableUiState.value = mutableUiState.value.copy(toast = if (currentSettings.language == "ar")
            "الإعلان غير متاح الآن، حاول لاحقًا" else "Ad unavailable. Please try again later.")
    }

    /** In the collision checkpoint no coin reward or tombstone has been written yet. */
    private fun settlePendingCollision() {
        if (!engine.finalizePendingCollision()) return
        refreshDailyDate(force = true)
        currentStats = currentStats.recordDailyActivity(activeDailyDate, deviceId) { d ->
            d.copy(completedGames = d.completedGames + 1, bestRoundScore = maxOf(d.bestRoundScore, engine.score))
        }
        settleCompletedRound()
    }

    private fun settleCompletedRound() {
        if (!engine.isGameOver || engine.pendingCollision) return
        if (engine.mode == GameMode.CLASSIC) {
            currentStats = currentStats.rewardCoins(engine.sessionId, engine.score)
        }
        engine.pause()
        mutableUiState.value = mutableUiState.value.copy(
            screen = AppScreen.GAME_OVER, hasSavedGame = false,
            stats = currentStats, frameVersion = mutableUiState.value.frameVersion + 1
        )
        persistStats()
        val endedSessionId = engine.sessionId
        val tombstone = RoundTombstone(
            completedSessionId = endedSessionId,
            completedAtEpochMs = maxOf(System.currentTimeMillis(), engine.savedAtEpochMs), writerDeviceId = deviceId
        )
        currentTombstone = newestTombstone(currentTombstone, tombstone)
        viewModelScope.launch {
            localStore.saveRoundTombstone(tombstone)
            localStore.clearGame(expectedSessionId = endedSessionId)
        }
    }

    fun onFrame(frameDeltaMs: Long) {
        if (mutableUiState.value.screen != AppScreen.GAME) return
        if (graceSecondsMutable.value > 0) {
            val seconds = resumeGraceTimer.secondsRemaining(SystemClock.elapsedRealtime())
            if (seconds == 0) clearResumeGrace(true)
            else graceSecondsMutable.value = seconds
            return // No movement, Time Attack countdown, or shield decrement during grace.
        }
        if (!engine.isRunning) return
        refreshDailyDate()
        val previousLevel = engine.level
        val dt = frameDeltaMs.coerceIn(0L, 250L)
        val events = engine.update(dt)
        if (dt > 0) pendingPlayMs += dt

        val previousBestScore = if (engine.mode == GameMode.TIME_ATTACK) currentStats.bestTimeScore
            else currentStats.bestScore
        val applesToday = events.count { it.type == EngineEvent.Type.FOOD || it.type == EngineEvent.Type.GOLDEN }
        val completedToday = events.any { it.type == EngineEvent.Type.GAME_OVER } && !engine.pendingCollision
        if (applesToday > 0 || completedToday) {
            currentStats = currentStats.recordDailyActivity(activeDailyDate, deviceId) { d ->
                d.copy(apples = d.apples + applesToday,
                    completedGames = d.completedGames + if (completedToday) 1 else 0,
                    bestRoundScore = if (applesToday > 0) maxOf(d.bestRoundScore, engine.score) else d.bestRoundScore)
            }
        }
        // Decorative feedback only. These events never enter the save or coin ledger.
        // Retain at most the last few cues to keep Canvas drawing bounded.
        val cueTime = SystemClock.elapsedRealtime()
        val newCues = events.filter { it.type != EngineEvent.Type.GAME_OVER }
            .mapNotNull { event -> (event.cell ?: engine.snake.firstOrNull())?.let { VisualCue(event.type, it, cueTime) } }
        val visualCues = (mutableUiState.value.visualCues + newCues)
            .filter { cueTime - it.startedAtElapsedMs in 0..520L }
            .takeLast(4)
        events.forEach { event ->
            when (event.type) {
                EngineEvent.Type.FOOD -> {
                    currentStats = currentStats.editDevice(deviceId) { it.copy(food = it.food + 1) }
                    playFeedback(SoundEffect.EAT, 55L)
                }
                EngineEvent.Type.GOLDEN -> {
                    currentStats = currentStats.editDevice(deviceId) { it.copy(food = it.food + 1, golden = it.golden + 1) }
                    playFeedback(SoundEffect.GOLDEN, 90L)
                }
                EngineEvent.Type.SCISSORS -> {
                    currentStats = currentStats.editDevice(deviceId) { it.copy(scissors = it.scissors + 1) }
                    playFeedback(SoundEffect.SCISSORS, 85L)
                }
                EngineEvent.Type.GAME_OVER -> playFeedback(SoundEffect.LOSE, 180L)
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

        val beatRecord = !celebratedRecordThisRound && previousBestScore > 0 &&
            engine.score > previousBestScore
        if (beatRecord) {
            celebratedRecordThisRound = true
            audio.announceRecord(currentSettings.language)
        } else if (engine.level > previousLevel) {
            // Audio only; no banner, visual particles, or interruption to physics.
            audio.play(SoundEffect.LEVEL)
        }

        if (engine.isGameOver) {
            flushPlayTime()
            engine.pause()
            mutableUiState.value = mutableUiState.value.copy(
                screen = AppScreen.GAME_OVER,
                visualCues = emptyList(),
                newRecordThisRound = celebratedRecordThisRound,
                frameVersion = mutableUiState.value.frameVersion + 1,
                stats = currentStats,
                hasSavedGame = engine.pendingCollision,
                newAchievementIds = mutableUiState.value.newAchievementIds + unlocked.second
            )
            if (engine.pendingCollision) {
                // Save the last valid snake position so an interrupted ad never loses progress.
                persistGame()
                persistStats()
            } else settleCompletedRound()
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
            visualCues = visualCues,
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
    private fun playFeedback(effect: SoundEffect, durationMs: Long) {
        audio.play(effect)
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
        if (deviceId.isBlank() || (engine.isGameOver && !engine.pendingCollision) || engine.snake.isEmpty()) return
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
        currentSettings = currentSettings.copy(
                ownedHeadKeys = currentSettings.ownedHeadKeys + currentStats.purchasedHeadKeys + FREE_HEAD_KEYS,
                ownedBodyKeys = currentSettings.ownedBodyKeys + currentStats.purchasedBodyKeys + FREE_BODY_KEYS,
                ownedSkinKeys = currentSettings.ownedSkinKeys + currentStats.purchasedSkinKeys,
                ownedThemeKeys = currentSettings.ownedThemeKeys + currentStats.purchasedThemeKeys + FREE_THEME_KEYS
            )
        viewModelScope.launch { settingsRepository.save(currentSettings) }
        val unlocked = evaluateAchievements(currentStats)
        currentStats = unlocked.first
        mutableUiState.value = mutableUiState.value.copy(
            stats = currentStats,
            settings = currentSettings,
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
            a.specials == b.specials && a.score == b.score && a.timeRemainingMs == b.timeRemainingMs &&
            a.pendingCollision == b.pendingCollision && a.reviveUsed == b.reviveUsed &&
            a.shieldRemainingMs == b.shieldRemainingMs

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
        currentSettings = currentSettings.copy(
                ownedHeadKeys = currentSettings.ownedHeadKeys + currentStats.purchasedHeadKeys + FREE_HEAD_KEYS,
                ownedBodyKeys = currentSettings.ownedBodyKeys + currentStats.purchasedBodyKeys + FREE_BODY_KEYS,
                ownedSkinKeys = currentSettings.ownedSkinKeys + currentStats.purchasedSkinKeys,
                ownedThemeKeys = currentSettings.ownedThemeKeys + currentStats.purchasedThemeKeys + FREE_THEME_KEYS
            )
        viewModelScope.launch {
            settingsRepository.save(currentSettings)
            if (chosenRound == null) {
                localStore.clearGame()
                engine.pause()
            } else {
                localStore.saveGame(chosenRound)
                engine.restore(chosenRound)
            }
            localStore.saveStats(currentStats)
            mutableUiState.value = mutableUiState.value.copy(
                screen = when {
                    chosenRound?.pendingCollision == true -> AppScreen.GAME_OVER
                    mutableUiState.value.screen == AppScreen.GAME -> AppScreen.PAUSE
                    else -> mutableUiState.value.screen
                },
                stats = currentStats,
                settings = currentSettings,
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
            val round = if ((!engine.isGameOver || engine.pendingCollision) && mutableUiState.value.hasSavedGame) {
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
        runCatching { audio.release() }
        super.onCleared()
    }
}
