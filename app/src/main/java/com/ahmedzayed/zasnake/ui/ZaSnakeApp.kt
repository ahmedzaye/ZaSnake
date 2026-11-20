package com.ahmedzayed.zasnake.ui

import com.ahmedzayed.zasnake.ads.RewardedAds
import com.ahmedzayed.zasnake.ads.RewardPlacement
import com.ahmedzayed.zasnake.billing.CoinPriceCatalog
import com.ahmedzayed.zasnake.BuildConfig
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.Gamepad
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.net.ConnectivityManager
import android.net.Network
import android.os.SystemClock
import android.provider.Settings
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.runtime.withFrameNanos
import com.ahmedzayed.zasnake.GameViewModel
import com.ahmedzayed.zasnake.language.DeviceLanguage
import com.ahmedzayed.zasnake.R
import com.ahmedzayed.zasnake.game.ACHIEVEMENTS
import com.ahmedzayed.zasnake.game.DailyChallenge
import com.ahmedzayed.zasnake.game.DAILY_LOGIN_COIN_SEQUENCE
import com.ahmedzayed.zasnake.game.dailyProgress
import com.ahmedzayed.zasnake.game.nextDailyLoginDay
import com.ahmedzayed.zasnake.game.AppScreen
import com.ahmedzayed.zasnake.game.AppSettings
import com.ahmedzayed.zasnake.game.PlayerNamePolicy
import com.ahmedzayed.zasnake.game.COIN_PACKAGES
import com.ahmedzayed.zasnake.game.AppUiState
import com.ahmedzayed.zasnake.game.Cell
import com.ahmedzayed.zasnake.game.ConfirmationAction
import com.ahmedzayed.zasnake.game.ControlStyle
import com.ahmedzayed.zasnake.game.Difficulty
import com.ahmedzayed.zasnake.game.Direction
import com.ahmedzayed.zasnake.game.GameMode
import com.ahmedzayed.zasnake.game.GameTheme
import com.ahmedzayed.zasnake.game.SnakeSkin
import com.ahmedzayed.zasnake.game.SnakeHeadStyle
import com.ahmedzayed.zasnake.game.SnakeBodyType
import com.ahmedzayed.zasnake.game.ARAB_COLLECTION
import com.ahmedzayed.zasnake.game.SpecialType
import com.ahmedzayed.zasnake.game.EngineEvent
import com.ahmedzayed.zasnake.game.VisualCue
import com.ahmedzayed.zasnake.platform.PlayGamesBridge
import kotlinx.coroutines.isActive
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.min


internal data class RewardPop(val id: Long, val coins: Int)


internal fun Context.findActivity(): Activity? {
    var current: Context = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        val next = current.baseContext
        if (next === current) return null
        current = next
    }
    return current as? Activity
}

@Composable
fun ZaSnakeApp(
    viewModel: GameViewModel,
    playGamesBridge: PlayGamesBridge,
    rewardedAds: RewardedAds?,
    coinPriceCatalog: CoinPriceCatalog?,
    privacyOptionsRequired: Boolean,
    onPrivacyOptions: () -> Unit,
    onExitApp: () -> Unit,
    onEditAge: () -> Unit
) {
    val modelState by viewModel.uiState
    // Automatic tracks Android changes; manual choices take priority without a restart.
    val deviceLanguage = DeviceLanguage.fromLocale(LocalConfiguration.current.locales[0])
    val language = DeviceLanguage.resolve(modelState.settings.languageMode, deviceLanguage)
    val state = if (modelState.settings.language == language) modelState
        else modelState.copy(settings = modelState.settings.copy(language = language))
    LaunchedEffect(deviceLanguage, modelState.settings.languageMode, modelState.ready) {
        if (modelState.ready && modelState.settings.languageMode == DeviceLanguage.SYSTEM) {
            viewModel.followDeviceLanguage(deviceLanguage)
        }
    }
    val layoutDirection = if (language == "ar") LayoutDirection.Rtl else LayoutDirection.Ltr
    val selectedPalette = playformPalette()
    val selectedBoardPalette = paletteFor(state.settings.theme)
    val lifecycleOwner = LocalLifecycleOwner.current
    val localContext = LocalContext.current
    val activity = localContext.findActivity()
    val rootView = LocalView.current
    val appContext = localContext.applicationContext
    val motionEnabled = remember(localContext) {
        Settings.Global.getFloat(localContext.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f
    }
    var rewardPop by remember { mutableStateOf<RewardPop?>(null) }
    var nextRewardPopId by remember { mutableLongStateOf(0L) }
    fun celebrateClaim(coins: Int) {
        if (coins <= 0) return
        nextRewardPopId += 1L
        rewardPop = RewardPop(nextRewardPopId, coins)
    }

    SideEffect {
        activity?.window?.let { window ->
            window.statusBarColor = selectedPalette.bg.toArgb()
            window.navigationBarColor = selectedPalette.bg.toArgb()
            WindowCompat.getInsetsController(window, rootView).apply {
                isAppearanceLightStatusBars = true
                isAppearanceLightNavigationBars = true
            }
        }
    }

    BackHandler(enabled = true) {
        if (state.pendingConfirmation != null) viewModel.dismissConfirmation() else viewModel.handleBack()
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> viewModel.onAppBackgrounded()
                Lifecycle.Event.ON_START -> viewModel.onAppForegrounded()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Silent auth on launch and again when network connectivity returns. Sync is deferred
    // while a live round is moving; a conflict is never surfaced over active gameplay.
    DisposableEffect(state.ready, appContext, playGamesBridge) {
        if (!state.ready) {
            onDispose { }
        } else if (!playGamesBridge.isConfigured()) {
            viewModel.setCloudStatus("not_configured")
            onDispose { }
        } else {
            val connectivity = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            var authInProgress = false
            var lastAttemptAt = 0L
            fun authenticateAndMaybeSync() {
                val now = SystemClock.elapsedRealtime()
                if (authInProgress || now - lastAttemptAt < 3_000L) return
                authInProgress = true
                lastAttemptAt = now
                playGamesBridge.authenticate { authenticated ->
                    authInProgress = false
                    if (authenticated) {
                        viewModel.setCloudStatus("connected")
                        when (viewModel.uiState.value.screen) {
                            AppScreen.GAME -> Unit // Defer cloud round replacement during active play.
                            AppScreen.GAME_OVER -> viewModel.uploadCloudProgress(playGamesBridge)
                            else -> viewModel.syncWithCloud(playGamesBridge)
                        }
                        viewModel.uiState.value.stats.unlockedAchievements.forEach(playGamesBridge::unlockAchievement)
                    } else {
                        viewModel.setCloudStatus("offline")
                    }
                }
            }
            val callback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) = authenticateAndMaybeSync()
                override fun onLost(network: Network) {
                    if (connectivity?.activeNetwork == null) viewModel.setCloudStatus("offline")
                }
            }
            authenticateAndMaybeSync()
            var registered = false
            if (connectivity != null) {
                registered = runCatching {
                    connectivity.registerDefaultNetworkCallback(callback)
                    true
                }.getOrDefault(false)
            }
            onDispose {
                if (registered && connectivity != null) {
                    runCatching { connectivity.unregisterNetworkCallback(callback) }
                }
            }
        }
    }

    LaunchedEffect(state.screen, state.cloudStatus) {
        if (!state.ready || state.screen == AppScreen.GAME) return@LaunchedEffect
        if (state.screen == AppScreen.GAME_OVER) {
            // A completed round must never be resurrected by loading an old cloud snapshot.
            if (state.cloudStatus in listOf("synced", "connected", "sync_error")) {
                viewModel.uploadCloudProgress(playGamesBridge)
            }
        } else if (state.cloudStatus in listOf("connected", "sync_error")) {
            viewModel.syncWithCloud(playGamesBridge)
        } else if (state.cloudStatus == "synced" && state.screen in listOf(AppScreen.PAUSE, AppScreen.HOME)) {
            viewModel.uploadCloudProgress(playGamesBridge)
        }
    }

    LaunchedEffect(state.newAchievementIds) {
        if (state.newAchievementIds.isNotEmpty()) {
            state.newAchievementIds.distinct().forEach(playGamesBridge::unlockAchievement)
            viewModel.consumeAchievementSignals()
        }
    }

    LaunchedEffect(state.screen, state.settings.musicEnabled, state.settings.musicVolume) {
        if (state.ready) viewModel.updateMusic()
    }

    LaunchedEffect(state.screen) {
        if (state.screen == AppScreen.GAME) {
            var lastFrame = 0L
            while (kotlinx.coroutines.currentCoroutineContext().isActive) {
                val now = withFrameNanos { it }
                if (lastFrame != 0L) viewModel.onFrame(((now - lastFrame) / 1_000_000L).coerceAtMost(250L))
                lastFrame = now
            }
        }
    }

    androidx.compose.runtime.CompositionLocalProvider(
        LocalLayoutDirection provides layoutDirection,
        LocalZaPalette provides selectedPalette,
        LocalBoardPalette provides selectedBoardPalette,
        LocalVisualMotionEnabled provides motionEnabled
    ) {
        Surface(modifier = Modifier.fillMaxSize().safeDrawingPadding(), color = Bg, contentColor = PrimaryText) {
            if (!state.ready) {
                SnakeLoadingScreen(background = Bg, snakeColor = Neon, motionEnabled = motionEnabled)
            } else {
                Box(Modifier.fillMaxSize()) {
                    // Never double-compose a running board while switching pause/game-over states.
                    // Only menu screens use the lightweight fade/scale transition.
                    if (state.screen == AppScreen.GAME || state.screen == AppScreen.PAUSE || state.screen == AppScreen.GAME_OVER) {
                        GameScreen(state, viewModel, rewardedAds, showOverlay = state.screen != AppScreen.GAME)
                    } else {
                        AnimatedContent(
                            targetState = state.screen,
                            modifier = Modifier.fillMaxSize(),
                            transitionSpec = {
                                if (motionEnabled) {
                                    (fadeIn(animationSpec = tween(170)) +
                                        scaleIn(initialScale = 0.985f, animationSpec = tween(170))) togetherWith
                                        fadeOut(animationSpec = tween(110))
                                } else {
                                    fadeIn(animationSpec = tween(0)) togetherWith fadeOut(animationSpec = tween(0))
                                }
                            },
                            label = "ZaSnakeMenuTransition"
                        ) { screen ->
                            when (screen) {
                                AppScreen.HOME -> HomeScreen(state, onNewGame = viewModel::goToModeSelect,
                                    onResume = viewModel::resumeSavedGame,
                                    onSettings = viewModel::showSettings, onStats = { viewModel.showStatistics() },
                                    onShop = viewModel::showShop, onDaily = viewModel::showDailyRewards,
                                    onClaimAchievements = { viewModel.showStatistics(openAchievements = true) })
                                AppScreen.MODE_SELECT -> ModeSelectScreen(state, onBack = viewModel::navigateHomeFromSubscreen,
                                    onSelect = viewModel::selectMode, onStart = viewModel::startSelectedMode)
                                AppScreen.SETTINGS -> SettingsScreen(state, onBack = viewModel::navigateHomeFromSubscreen,
                                    onSettingsChange = viewModel::changeSettings,
                                    onUpdatePlayerName = viewModel::updatePlayerName,
                                    onEditAge = onEditAge,
                                    onChangePlayGamesAccount = {
                                        // PGS v2 owns sign-out/account choice in Android's settings.
                                        // This action never fakes an SDK sign-out.
                                        activity?.startActivity(android.content.Intent(
                                            android.provider.Settings.ACTION_SETTINGS
                                        ))
                                    },
                                    privacyOptionsRequired = privacyOptionsRequired,
                                    onPrivacyOptions = onPrivacyOptions,
                                    playGamesAllowed = playGamesBridge.isAllowedByAge(),
                                    playGamesConfigured = playGamesBridge.isConfigured(),
                                    onRequestPlayGamesSignIn = {
                                        if (playGamesBridge.isConfigured()) {
                                            viewModel.setCloudStatus("checking")
                                            playGamesBridge.requestSignIn { authenticated ->
                                                if (authenticated) {
                                                    viewModel.setCloudStatus("connected")
                                                    if (viewModel.uiState.value.screen != AppScreen.GAME) {
                                                        viewModel.syncWithCloud(playGamesBridge)
                                                    }
                                                    viewModel.uiState.value.stats.unlockedAchievements.forEach(playGamesBridge::unlockAchievement)
                                                } else {
                                                    viewModel.setCloudStatus("offline")
                                                }
                                            }
                                        }
                                    },
                                    onTestGameServerAuth = if (BuildConfig.DEBUG && coinPriceCatalog != null) {
                                        { onResult: (Boolean) -> Unit ->
                                            // Adult Debug only: never store, log, or transmit the code.
                                            if (!playGamesBridge.isConfigured() ||
                                                !BuildConfig.PGS_WEB_CLIENT_ID.endsWith(".apps.googleusercontent.com")) {
                                                onResult(false)
                                            } else {
                                                playGamesBridge.requestServerAuthCode(
                                                    BuildConfig.PGS_WEB_CLIENT_ID
                                                ) { code ->
                                                    onResult(!code.isNullOrBlank())
                                                }
                                            }
                                        }
                                    } else null)
                                AppScreen.STATISTICS -> StatisticsScreen(state, onBack = viewModel::navigateHomeFromSubscreen,
                                    onClaimReward = { id ->
                                        val rewarded = viewModel.claimAchievementReward(id)
                                        if (rewarded) celebrateClaim(ACHIEVEMENTS.firstOrNull { it.id == id }?.rewardCoins ?: 0)
                                        rewarded
                                    })
                                AppScreen.SHOP -> ShopScreen(state, coinPriceCatalog = coinPriceCatalog, onBack = viewModel::navigateHomeFromSubscreen,
                                    onPurchase = viewModel::purchaseSkin, onEquip = viewModel::equipSkin,
                                    onPurchaseHead = viewModel::purchaseHead, onEquipHead = viewModel::equipHead,
                                    onPurchaseBody = viewModel::purchaseBody, onEquipBody = viewModel::equipBody,
                                    onPurchaseTheme = viewModel::purchaseTheme, onApplyTheme = { theme ->
                                        viewModel.changeSettings(viewModel.uiState.value.settings.copy(theme = theme))
                                    })
                                AppScreen.DAILY_REWARDS -> DailyRewardsScreen(state, onBack = viewModel::navigateHomeFromSubscreen,
                                    onClaimChallenge = { id ->
                                        val rewarded = viewModel.claimDailyChallengeReward(id)
                                        if (rewarded) celebrateClaim(DailyChallenge.entries.firstOrNull { it.id == id }?.coins ?: 0)
                                        rewarded
                                    },
                                    onClaimLogin = {
                                        val day = state.stats.nextDailyLoginDay(state.dailyDate)
                                        val rewarded = viewModel.claimDailyLoginReward()
                                        if (rewarded) celebrateClaim(DAILY_LOGIN_COIN_SEQUENCE.getOrElse((day - 1).coerceAtLeast(0)) { 0 })
                                        rewarded
                                    },
                                    onRefresh = viewModel::refreshDailyRewards)
                                // Gameplay is deliberately handled outside AnimatedContent.
                                else -> Unit
                            }
                        }
                    }
                    rewardPop?.let { pop ->
                        RewardPopOverlay(pop, balance = state.stats.coins, language = language, onFinished = { id ->
                            if (rewardPop?.id == id) rewardPop = null
                        }, modifier = Modifier.align(Alignment.TopCenter))
                    }
                }
            }
        }
    }

    val roundConflict = state.conflict
    if (roundConflict != null) {
        androidx.compose.runtime.CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text(if (language == "ar") "وجدنا جولتين مختلفتين" else "Two different rounds found", color = Playform.Ink, fontWeight = FontWeight.Bold) },
            text = { Text(if (language == "ar") "هنحتفظ بإحصائياتك وإنجازاتك، لكن محتاجين نعرف أي جولة تريد استكمالها." else "Your stats and achievements will merge. Choose which round you want to continue.", color = Playform.TextSecondary) },
            confirmButton = {
                TextButton(onClick = {
                    val resolution = viewModel.resolveRoundConflict(true)
                    if (resolution != null) {
                        val done: (Boolean) -> Unit = { viewModel.cloudWriteFinished(it) }
                        if (resolution.fromPgsConflict) playGamesBridge.resolvePendingConflict(resolution.payload, resolution.chooseServerSnapshot, done)
                        else playGamesBridge.saveBundle(resolution.payload, done)
                    }
                }) { Text(if (language == "ar") "استخدام الجولة السحابية" else "Use cloud round", color = Playform.Ink) }
            },
            dismissButton = {
                TextButton(onClick = {
                    val resolution = viewModel.resolveRoundConflict(false)
                    if (resolution != null) {
                        val done: (Boolean) -> Unit = { viewModel.cloudWriteFinished(it) }
                        if (resolution.fromPgsConflict) playGamesBridge.resolvePendingConflict(resolution.payload, resolution.chooseServerSnapshot, done)
                        else playGamesBridge.saveBundle(resolution.payload, done)
                    }
                }) { Text(if (language == "ar") "الاحتفاظ بجولة هذا الجهاز" else "Keep this device's round", color = Playform.TextSecondary) }
            },
            containerColor = Playform.Cream,
            titleContentColor = Playform.Ink,
            textContentColor = Playform.Ink
        )
        }
    }

    val pending = state.pendingConfirmation
    if (pending != null) {
        androidx.compose.runtime.CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        val title = when (pending) {
            ConfirmationAction.START_NEW -> tx(language, "new_round_confirm_title")
            ConfirmationAction.RESTART -> tx(language, "restart_confirm_title")
            ConfirmationAction.EXIT_APP -> tx(language, "exit_confirm_title")
        }
        val message = when (pending) {
            ConfirmationAction.START_NEW -> tx(language, "new_round_confirm")
            ConfirmationAction.RESTART -> tx(language, "restart_confirm")
            ConfirmationAction.EXIT_APP -> tx(language, "exit_confirm")
        }
        AlertDialog(
            onDismissRequest = viewModel::dismissConfirmation,
            title = { Text(title, color = Playform.Ink, fontWeight = FontWeight.Bold) },
            text = { Text(message, color = Playform.TextSecondary) },
            confirmButton = {
                TextButton(onClick = {
                    if (pending == ConfirmationAction.EXIT_APP) {
                        viewModel.dismissConfirmation()
                        onExitApp()
                    } else viewModel.confirmPendingAction()
                }) { Text(tx(language, "confirm"), color = Playform.Ink) }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissConfirmation) { Text(tx(language, "cancel"), color = Playform.TextSecondary) }
            },
            containerColor = Playform.Cream,
            titleContentColor = Playform.Ink,
            textContentColor = Playform.Ink
        )
        }
    }
}

