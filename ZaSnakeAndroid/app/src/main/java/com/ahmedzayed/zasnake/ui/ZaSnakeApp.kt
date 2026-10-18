package com.ahmedzayed.zasnake.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.weight
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
import androidx.compose.material.icons.rounded.Gamepad
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalContext
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.os.SystemClock
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.runtime.withFrameNanos
import com.ahmedzayed.zasnake.GameViewModel
import com.ahmedzayed.zasnake.R
import com.ahmedzayed.zasnake.game.ACHIEVEMENTS
import com.ahmedzayed.zasnake.game.AppScreen
import com.ahmedzayed.zasnake.game.AppSettings
import com.ahmedzayed.zasnake.game.AppUiState
import com.ahmedzayed.zasnake.game.Cell
import com.ahmedzayed.zasnake.game.ConfirmationAction
import com.ahmedzayed.zasnake.game.Difficulty
import com.ahmedzayed.zasnake.game.Direction
import com.ahmedzayed.zasnake.game.GameMode
import com.ahmedzayed.zasnake.game.SnakeSkin
import com.ahmedzayed.zasnake.game.SpecialType
import com.ahmedzayed.zasnake.platform.PlayGamesBridge
import kotlinx.coroutines.isActive
import kotlin.math.abs
import kotlin.math.min

private val Bg = Color(0xFF050A0E)
private val Panel = Color(0xFF0A1520)
private val Panel2 = Color(0xFF0C1914)
private val Neon = Color(0xFF00FF88)
private val Muted = Color(0xFF91A99D)
private val Gold = Color(0xFFFFD34E)
private val Blue = Color(0xFF9DC9FF)

private val ar = mapOf(
    "app_tagline" to "الثعبان الكلاسيكي بروح جديدة",
    "ready" to "جاهز للتحدي؟", "home_description" to "كُل التفاح، كبّر ثعبانك، وحطّم رقمك القياسي!",
    "new_game" to "لعبة جديدة", "resume_game" to "استكمال الجولة المحفوظة",
    "settings" to "الإعدادات", "stats_achievements" to "الإحصائيات والإنجازات",
    "choose_mode" to "اختر وضع اللعب", "choose_mode_sub" to "كل وضع بتجربة مختلفة",
    "classic" to "كلاسيكي", "classic_desc" to "اجمع التفاح وتجنب الاصطدام.",
    "time_attack" to "هجوم الوقت", "time_desc" to "60 ثانية في البداية؛ كل تفاحة تضيف ثانيتين.",
    "zen" to "هادئ Zen", "zen_desc" to "استمتع باللعب دون خسارة بسبب الاصطدام.",
    "start_classic" to "ابدأ الكلاسيكي", "start_time" to "ابدأ هجوم الوقت", "start_zen" to "ابدأ Zen",
    "back" to "رجوع", "general" to "عام", "language" to "لغة التطبيق", "arabic" to "العربية", "english" to "English",
    "sound_haptics" to "الصوت والاهتزاز", "sound" to "المؤثرات الصوتية", "haptics" to "الاهتزاز",
    "gameplay" to "اللعب والتخصيص", "difficulty" to "الصعوبة", "easy" to "سهل", "medium" to "متوسط", "hard" to "صعب",
    "walls" to "الجدران", "walls_desc" to "الاصطدام بحدود اللوحة ينهي الجولة في Classic وTime Attack.",
    "enabled" to "مفعّلة", "disabled" to "معطّلة", "skin" to "شكل الثعبان", "green" to "أخضر نيون",
    "purple" to "بنفسجي نيون", "yellow" to "أصفر نيون", "egypt" to "علم مصر",
    "controls" to "التحكم", "landscape_side" to "مكان التحكم في الوضع الأفقي", "left" to "اليسار", "right" to "اليمين",
    "account_sync" to "الحساب والمزامنة", "play_games" to "Google Play Games",
    "cloud_not_configured" to "اللعب المحلي متاح؛ إعداد Play Console مطلوب لتفعيل المزامنة.",
    "cloud_connected" to "الحساب متصل؛ المزامنة جاهزة.", "cloud_sync_error" to "المزامنة لم تكتمل؛ سنحاول مجددًا.",
    "cloud_checking" to "جارٍ التحقق من الاتصال…", "cloud_syncing" to "جارٍ مزامنة التقدم…", "cloud_synced" to "التقدم متزامن مع السحابة.", "cloud_conflict" to "تحتاج الجولة إلى اختيارك قبل إكمال المزامنة.", "cloud_offline" to "اللعب المحلي — المزامنة غير متاحة الآن.",
    "statistics" to "الإحصائيات", "achievements" to "الإنجازات", "best_score" to "أفضل نتيجة",
    "games" to "المباريات", "food_eaten" to "التفاح المأكول", "play_time" to "وقت اللعب",
    "golden_apples" to "التفاح الذهبي", "scissors" to "المقصات المستخدمة", "longest_snake" to "أطول ثعبان",
    "unlocked" to "مفتوح", "locked" to "مقفل", "no_achievements" to "واصل اللعب لفتح إنجازات جديدة.",
    "pause_title" to "اللعبة متوقفة", "continue" to "متابعة اللعب", "restart" to "إعادة الجولة",
    "main_menu" to "القائمة الرئيسية", "game_over" to "انتهت الجولة!", "time_up" to "انتهى الوقت!",
    "your_score" to "نتيجتك", "level" to "المستوى", "time" to "الوقت", "score" to "النقاط",
    "new_round_confirm_title" to "بدء لعبة جديدة؟", "new_round_confirm" to "ستُستبدل الجولة غير المكتملة. ستظل الإحصائيات والإنجازات محفوظة.",
    "restart_confirm_title" to "إعادة الجولة؟", "restart_confirm" to "سيتم استبدال تقدم الجولة الحالية والبدء من جديد.",
    "exit_confirm_title" to "الخروج من اللعبة؟", "exit_confirm" to "ستظل الجولة محفوظة ويمكنك استكمالها عند فتح التطبيق مجددًا.",
    "cancel" to "إلغاء", "confirm" to "متابعة", "close" to "إغلاق", "best_time" to "أفضل نتيجة في هجوم الوقت",
    "select_mode" to "حدد وضعًا للبدء", "tap_to_steer" to "اسحب لتوجيه الثعبان", "paused_save" to "تم حفظ الجولة محليًا",
    "sync_pending" to "المزامنة قيد الإعداد", "record" to "رقم قياسي جديد!", "resume_hint" to "ستعود إلى نفس الوضع والنقاط والوقت المتبقي."
)
private val en = mapOf(
    "app_tagline" to "Classic Snake, reimagined", "ready" to "Ready for a challenge?",
    "home_description" to "Eat apples, grow your snake, and beat your best!",
    "new_game" to "New game", "resume_game" to "Resume saved round", "settings" to "Settings",
    "stats_achievements" to "Stats & achievements", "choose_mode" to "Choose a game mode", "choose_mode_sub" to "Three modes, three experiences",
    "classic" to "Classic", "classic_desc" to "Collect apples and avoid collisions.",
    "time_attack" to "Time Attack", "time_desc" to "Start with 60 seconds; each apple adds two seconds.",
    "zen" to "Zen", "zen_desc" to "Relax without losing from collisions.", "start_classic" to "Start Classic",
    "start_time" to "Start Time Attack", "start_zen" to "Start Zen", "back" to "Back", "general" to "General",
    "language" to "App language", "arabic" to "العربية", "english" to "English", "sound_haptics" to "Sound & haptics",
    "sound" to "Sound effects", "haptics" to "Vibration", "gameplay" to "Gameplay & appearance", "difficulty" to "Difficulty",
    "easy" to "Easy", "medium" to "Medium", "hard" to "Hard", "walls" to "Walls",
    "walls_desc" to "Board edges end the run in Classic and Time Attack.", "enabled" to "On", "disabled" to "Off",
    "skin" to "Snake skin", "green" to "Neon green", "purple" to "Neon purple", "yellow" to "Neon yellow", "egypt" to "Egypt flag",
    "controls" to "Controls", "landscape_side" to "Landscape joystick side", "left" to "Left", "right" to "Right",
    "account_sync" to "Account & sync", "play_games" to "Google Play Games",
    "cloud_not_configured" to "Local play is ready; Play Console setup is required for cloud sync.",
    "cloud_connected" to "Account connected; sync is ready.", "cloud_sync_error" to "Sync did not finish; we'll retry.",
    "cloud_checking" to "Checking connection…", "cloud_syncing" to "Syncing progress…", "cloud_synced" to "Progress synced to cloud.", "cloud_conflict" to "Choose a round before sync can continue.", "cloud_offline" to "Local play — sync is unavailable right now.",
    "statistics" to "Statistics", "achievements" to "Achievements", "best_score" to "Best score", "games" to "Games",
    "food_eaten" to "Apples eaten", "play_time" to "Play time", "golden_apples" to "Golden apples",
    "scissors" to "Scissors used", "longest_snake" to "Longest snake", "unlocked" to "Unlocked", "locked" to "Locked",
    "no_achievements" to "Keep playing to unlock achievements.", "pause_title" to "Game paused", "continue" to "Resume game",
    "restart" to "Restart round", "main_menu" to "Main menu", "game_over" to "Game over!", "time_up" to "Time's up!",
    "your_score" to "Your score", "level" to "Level", "time" to "Time", "score" to "Score",
    "new_round_confirm_title" to "Start a new game?", "new_round_confirm" to "The unfinished round will be replaced. Your stats and achievements will remain.",
    "restart_confirm_title" to "Restart this round?", "restart_confirm" to "The current round progress will be replaced.",
    "exit_confirm_title" to "Exit the game?", "exit_confirm" to "Your round will remain saved and can be resumed when you reopen the app.",
    "cancel" to "Cancel", "confirm" to "Continue", "close" to "Close", "best_time" to "Best Time Attack score",
    "select_mode" to "Select a mode to begin", "tap_to_steer" to "Drag to steer the snake", "paused_save" to "Round saved locally",
    "sync_pending" to "Sync setup pending", "record" to "New record!", "resume_hint" to "Your mode, score and remaining time will be restored."
)

private fun tx(language: String, key: String): String = if (language == "ar") ar[key] ?: key else en[key] ?: key
private fun GameMode.title(language: String) = tx(language, when (this) {
    GameMode.CLASSIC -> "classic"; GameMode.TIME_ATTACK -> "time_attack"; GameMode.ZEN -> "zen"
})
private fun GameMode.description(language: String) = tx(language, when (this) {
    GameMode.CLASSIC -> "classic_desc"; GameMode.TIME_ATTACK -> "time_desc"; GameMode.ZEN -> "zen_desc"
})
private fun GameMode.startLabel(language: String) = tx(language, when (this) {
    GameMode.CLASSIC -> "start_classic"; GameMode.TIME_ATTACK -> "start_time"; GameMode.ZEN -> "start_zen"
})

@Composable
fun ZaSnakeApp(
    viewModel: GameViewModel,
    playGamesBridge: PlayGamesBridge,
    onExitApp: () -> Unit
) {
    val state by viewModel.uiState
    val language = state.settings.language
    val layoutDirection = if (language == "ar") LayoutDirection.Rtl else LayoutDirection.Ltr
    val lifecycleOwner = LocalLifecycleOwner.current
    val appContext = LocalContext.current.applicationContext

    BackHandler(enabled = true) {
        if (state.pendingConfirmation != null) viewModel.dismissConfirmation() else viewModel.handleBack()
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) viewModel.onAppBackgrounded()
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

    androidx.compose.runtime.CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        Surface(modifier = Modifier.fillMaxSize().safeDrawingPadding(), color = Bg, contentColor = Color.White) {
            if (!state.ready) {
                Box(Modifier.fillMaxSize().background(Bg), contentAlignment = Alignment.Center) {
                    Text("Za Snake", color = Neon, fontSize = 28.sp, fontWeight = FontWeight.Black)
                }
            } else {
                when (state.screen) {
                    AppScreen.HOME -> HomeScreen(state, onNewGame = viewModel::goToModeSelect, onResume = viewModel::resumeSavedGame,
                        onToggleLanguage = { viewModel.changeSettings(state.settings.copy(language = if (language == "ar") "en" else "ar")) },
                        onSettings = viewModel::showSettings, onStats = viewModel::showStatistics)
                    AppScreen.MODE_SELECT -> ModeSelectScreen(state, onBack = viewModel::navigateHomeFromSubscreen,
                        onSelect = viewModel::selectMode, onStart = viewModel::startSelectedMode)
                    AppScreen.SETTINGS -> SettingsScreen(state, onBack = viewModel::navigateHomeFromSubscreen,
                        onSettingsChange = viewModel::changeSettings)
                    AppScreen.STATISTICS -> StatisticsScreen(state, onBack = viewModel::navigateHomeFromSubscreen)
                    AppScreen.GAME -> GameScreen(state, viewModel, showOverlay = false)
                    AppScreen.PAUSE -> GameScreen(state, viewModel, showOverlay = true)
                    AppScreen.GAME_OVER -> GameScreen(state, viewModel, showOverlay = true)
                }
            }
        }
    }

    val roundConflict = state.conflict
    if (roundConflict != null) {
        androidx.compose.runtime.CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text(if (language == "ar") "وجدنا جولتين مختلفتين" else "Two different rounds found", color = Neon, fontWeight = FontWeight.Bold) },
            text = { Text(if (language == "ar") "هنحتفظ بإحصائياتك وإنجازاتك، لكن محتاجين نعرف أي جولة تريد استكمالها." else "Your stats and achievements will merge. Choose which round you want to continue.", color = Color(0xFFCCDAD1)) },
            confirmButton = {
                TextButton(onClick = {
                    val resolution = viewModel.resolveRoundConflict(true)
                    if (resolution != null) {
                        val done: (Boolean) -> Unit = { viewModel.cloudWriteFinished(it) }
                        if (resolution.fromPgsConflict) playGamesBridge.resolvePendingConflict(resolution.payload, resolution.chooseServerSnapshot, done)
                        else playGamesBridge.saveBundle(resolution.payload, done)
                    }
                }) { Text(if (language == "ar") "استخدام الجولة السحابية" else "Use cloud round", color = Neon) }
            },
            dismissButton = {
                TextButton(onClick = {
                    val resolution = viewModel.resolveRoundConflict(false)
                    if (resolution != null) {
                        val done: (Boolean) -> Unit = { viewModel.cloudWriteFinished(it) }
                        if (resolution.fromPgsConflict) playGamesBridge.resolvePendingConflict(resolution.payload, resolution.chooseServerSnapshot, done)
                        else playGamesBridge.saveBundle(resolution.payload, done)
                    }
                }) { Text(if (language == "ar") "الاحتفاظ بجولة هذا الجهاز" else "Keep this device's round", color = Muted) }
            },
            containerColor = Panel,
            titleContentColor = Neon,
            textContentColor = Color.White
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
            title = { Text(title, color = Neon, fontWeight = FontWeight.Bold) },
            text = { Text(message, color = Color(0xFFCCDAD1)) },
            confirmButton = {
                TextButton(onClick = {
                    if (pending == ConfirmationAction.EXIT_APP) {
                        viewModel.dismissConfirmation()
                        onExitApp()
                    } else viewModel.confirmPendingAction()
                }) { Text(tx(language, "confirm"), color = Neon) }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissConfirmation) { Text(tx(language, "cancel"), color = Muted) }
            },
            containerColor = Panel,
            titleContentColor = Neon,
            textContentColor = Color.White
        )
        }
    }
}

@Composable
private fun ScreenHeader(title: String, onBack: () -> Unit, trailing: (@Composable () -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().padding(bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButtonFrame(onClick = onBack) { androidx.compose.material3.Icon(Icons.Rounded.ArrowBack, null, tint = Muted) }
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(title, color = Neon, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }
        if (trailing != null) trailing() else Spacer(Modifier.size(44.dp))
    }
}

@Composable
private fun HomeScreen(
    state: AppUiState,
    onNewGame: () -> Unit,
    onResume: () -> Unit,
    onToggleLanguage: () -> Unit,
    onSettings: () -> Unit,
    onStats: () -> Unit
) {
    val lang = state.settings.language
    Column(
        Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF07120F), Bg, Color(0xFF061117)))).padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Za Snake", color = Neon, fontSize = 28.sp, fontWeight = FontWeight.Black, letterSpacing = 0.5.sp)
                Text(tx(lang, "app_tagline"), color = Muted, fontSize = 12.sp)
            }
            IconButtonFrame(onClick = onToggleLanguage, modifier = Modifier.height(40.dp).widthIn(min = 76.dp)) {
                Text(if (lang == "ar") "English" else "العربية", color = Neon, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        HorizontalDivider(color = Color(0xFF183527))
        Spacer(Modifier.height(2.dp))
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            val heroSize = min(min(maxWidth.value * 0.70f, maxHeight.value * 0.52f), 230f).dp.coerceAtLeast(80.dp)
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Image(
                    painter = painterResource(R.drawable.za_snake_mascot),
                    contentDescription = "Za Snake mascot",
                    modifier = Modifier.size(heroSize).clip(RoundedCornerShape(28.dp)).border(1.dp, Color(0xFF1D4D36), RoundedCornerShape(28.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(Modifier.height(14.dp))
                Text(tx(lang, "ready"), color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text(tx(lang, "home_description"), color = Muted, fontSize = 13.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp))
            }
        }
        PrimaryActionButton(tx(lang, "new_game"), Icons.Rounded.PlayArrow, onNewGame)
        if (state.hasSavedGame) {
            OutlinedActionButton(tx(lang, "resume_game"), Icons.Rounded.Replay, onResume)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SecondaryTile(tx(lang, "stats_achievements"), Icons.Rounded.EmojiEvents, Modifier.weight(1f), onStats)
            SecondaryTile(tx(lang, "settings"), Icons.Rounded.Settings, Modifier.weight(1f), onSettings)
        }
        CloudStatusLine(state, lang)
    }
}

@Composable
private fun ModeSelectScreen(state: AppUiState, onBack: () -> Unit, onSelect: (GameMode) -> Unit, onStart: () -> Unit) {
    val lang = state.settings.language
    Column(Modifier.fillMaxSize().background(Bg).padding(16.dp)) {
        ScreenHeader(tx(lang, "choose_mode"), onBack)
        Text(tx(lang, "choose_mode_sub"), color = Muted, fontSize = 14.sp, modifier = Modifier.padding(bottom = 16.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ModeCard(GameMode.CLASSIC, state.selectedMode == GameMode.CLASSIC, lang, onSelect)
            ModeCard(GameMode.TIME_ATTACK, state.selectedMode == GameMode.TIME_ATTACK, lang, onSelect)
            ModeCard(GameMode.ZEN, state.selectedMode == GameMode.ZEN, lang, onSelect)
        }
        PrimaryActionButton(state.selectedMode.startLabel(lang), Icons.Rounded.PlayArrow, onStart)
    }
}

@Composable
private fun ModeCard(mode: GameMode, selected: Boolean, language: String, onSelect: (GameMode) -> Unit) {
    val accent = when (mode) { GameMode.CLASSIC -> Neon; GameMode.TIME_ATTACK -> Gold; GameMode.ZEN -> Blue }
    val icon = when (mode) { GameMode.CLASSIC -> Icons.Rounded.Gamepad; GameMode.TIME_ATTACK -> Icons.Rounded.Timer; GameMode.ZEN -> Icons.Rounded.Spa }
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onSelect(mode) }.border(if (selected) 1.5.dp else 1.dp, if (selected) accent else Color(0xFF263C32), RoundedCornerShape(18.dp)),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = if (selected) Color(0xFF10251D) else Panel)
    ) {
        Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(13.dp)) {
            Box(Modifier.size(52.dp).clip(RoundedCornerShape(14.dp)).background(accent.copy(alpha = 0.13f)), contentAlignment = Alignment.Center) {
                androidx.compose.material3.Icon(icon, null, tint = accent, modifier = Modifier.size(29.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(mode.title(language), color = accent, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text(mode.description(language), color = Color(0xFFC4D6CA), fontSize = 12.sp, lineHeight = 17.sp)
            }
            if (selected) androidx.compose.material3.Icon(Icons.Rounded.CheckCircle, null, tint = accent, modifier = Modifier.size(24.dp))
        }
    }
}

@Composable
private fun SettingsScreen(state: AppUiState, onBack: () -> Unit, onSettingsChange: (AppSettings) -> Unit) {
    val lang = state.settings.language
    val settings = state.settings
    LazyColumn(Modifier.fillMaxSize().background(Bg).padding(horizontal = 16.dp, vertical = 12.dp)) {
        item { ScreenHeader(tx(lang, "settings"), onBack) }
        item { SectionLabel(tx(lang, "general")) }
        item {
            SettingsPanel {
                SettingHeading(tx(lang, "language"), Icons.Rounded.Language)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ChoiceChip(tx(lang, "arabic"), settings.language == "ar") { onSettingsChange(settings.copy(language = "ar")) }
                    ChoiceChip(tx(lang, "english"), settings.language == "en") { onSettingsChange(settings.copy(language = "en")) }
                }
                HorizontalDivider(color = Color(0xFF23392D))
                SettingSwitch(tx(lang, "sound"), Icons.Rounded.VolumeUp, settings.soundEnabled) { onSettingsChange(settings.copy(soundEnabled = it)) }
                SettingSwitch(tx(lang, "haptics"), Icons.Rounded.Vibration, settings.hapticsEnabled) { onSettingsChange(settings.copy(hapticsEnabled = it)) }
            }
        }
        item { SectionLabel(tx(lang, "gameplay")) }
        item {
            SettingsPanel {
                SettingHeading(tx(lang, "difficulty"), Icons.Rounded.Gamepad)
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    Difficulty.entries.forEach { d ->
                        ChoiceChip(tx(lang, d.key), settings.difficulty == d) { onSettingsChange(settings.copy(difficulty = d)) }
                    }
                }
                HorizontalDivider(color = Color(0xFF23392D))
                SettingSwitch(tx(lang, "walls"), Icons.Rounded.Lock, settings.wallsEnabled) { onSettingsChange(settings.copy(wallsEnabled = it)) }
                Text(tx(lang, "walls_desc"), color = Muted, fontSize = 11.sp, lineHeight = 15.sp)
                HorizontalDivider(color = Color(0xFF23392D))
                SettingHeading(tx(lang, "skin"), Icons.Rounded.Star)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    SnakeSkin.entries.forEach { skin ->
                        SkinChip(skin, settings.skin == skin, lang) { onSettingsChange(settings.copy(skin = skin)) }
                    }
                }
                HorizontalDivider(color = Color(0xFF23392D))
                SettingHeading(tx(lang, "landscape_side"), Icons.Rounded.TouchApp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ChoiceChip(tx(lang, "right"), !settings.joystickLeftLandscape) { onSettingsChange(settings.copy(joystickLeftLandscape = false)) }
                    ChoiceChip(tx(lang, "left"), settings.joystickLeftLandscape) { onSettingsChange(settings.copy(joystickLeftLandscape = true)) }
                }
            }
        }
        item { SectionLabel(tx(lang, "account_sync")) }
        item {
            SettingsPanel {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    androidx.compose.material3.Icon(
                        if (state.cloudStatus in listOf("connected", "sync_error", "synced")) Icons.Rounded.CloudDone else Icons.Rounded.CloudOff,
                        contentDescription = null,
                        tint = if (state.cloudStatus in listOf("connected", "sync_error", "synced")) Neon else Gold,
                        modifier = Modifier.size(28.dp)
                    )
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(tx(lang, "play_games"), color = Color.White, fontWeight = FontWeight.SemiBold)
                        Text(cloudStatusLabel(state.cloudStatus, lang), color = Muted, fontSize = 12.sp, lineHeight = 16.sp)
                    }
                }
            }
        }
        item { Spacer(Modifier.height(14.dp)) }
    }
}

@Composable
private fun StatisticsScreen(state: AppUiState, onBack: () -> Unit) {
    val lang = state.settings.language
    var selectedTab by remember { mutableStateOf(0) }
    val stats = state.stats
    LazyColumn(Modifier.fillMaxSize().background(Bg).padding(horizontal = 16.dp, vertical = 12.dp)) {
        item { ScreenHeader(tx(lang, "stats_achievements"), onBack) }
        item {
            Row(Modifier.fillMaxWidth().background(Panel, RoundedCornerShape(13.dp)).padding(4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ChoiceChip(tx(lang, "statistics"), selectedTab == 0, Modifier.weight(1f)) { selectedTab = 0 }
                ChoiceChip(tx(lang, "achievements"), selectedTab == 1, Modifier.weight(1f)) { selectedTab = 1 }
            }
            Spacer(Modifier.height(14.dp))
        }
        if (selectedTab == 0) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF10251D)), shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        androidx.compose.material3.Icon(Icons.Rounded.EmojiEvents, null, tint = Gold, modifier = Modifier.size(31.dp))
                        Text(tx(lang, "best_score"), color = Muted, fontSize = 13.sp)
                        Text(stats.bestScore.toString(), color = Color.White, fontSize = 38.sp, fontWeight = FontWeight.Black)
                        Text("${tx(lang, "best_time")}: ${stats.bestTimeScore}", color = Gold, fontSize = 12.sp)
                    }
                }
                Spacer(Modifier.height(12.dp))
                val values = listOf(
                    tx(lang, "games") to stats.games.toString(),
                    tx(lang, "food_eaten") to stats.food.toString(),
                    tx(lang, "play_time") to formatPlayTime(stats.playMs, lang),
                    tx(lang, "golden_apples") to stats.golden.toString(),
                    tx(lang, "scissors") to stats.scissors.toString(),
                    tx(lang, "longest_snake") to stats.maxLength.toString()
                )
                values.chunked(2).forEach { rowItems ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        rowItems.forEach { (label, value) -> StatTile(label, value, Modifier.weight(1f)) }
                        if (rowItems.size == 1) Spacer(Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(10.dp))
                }
            }
        } else {
            item {
                val unlockedCount = ACHIEVEMENTS.count { it.id in stats.unlockedAchievements }
                Text("$unlockedCount / ${ACHIEVEMENTS.size} ${tx(lang, "achievements").lowercase()}", color = Neon, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(9.dp))
            }
            items(ACHIEVEMENTS, key = { it.id }) { achievement ->
                val unlocked = achievement.id in stats.unlockedAchievements
                val progress = achievement.progress(stats).coerceAtLeast(0L)
                val ratio = if (achievement.target <= 0) 1f else (progress.toFloat() / achievement.target.toFloat()).coerceIn(0f, 1f)
                Card(
                    Modifier.fillMaxWidth().padding(bottom = 9.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = if (unlocked) Color(0xFF10251D) else Panel)
                ) {
                    Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(Modifier.size(46.dp).clip(RoundedCornerShape(12.dp)).background(if (unlocked) Color(0xFF3B3118) else Color(0xFF17242C)), contentAlignment = Alignment.Center) {
                            Text(if (unlocked) achievement.icon else "🔒", fontSize = 22.sp)
                        }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(if (lang == "ar") achievement.arName else achievement.enName, color = if (unlocked) Gold else Color.White, fontWeight = FontWeight.SemiBold)
                            Text(if (lang == "ar") achievement.arDescription else achievement.enDescription, color = Muted, fontSize = 11.sp)
                            if (!unlocked) {
                                LinearProgressIndicator(progress = { ratio }, modifier = Modifier.fillMaxWidth().height(5.dp).clip(CircleShape), color = Neon, trackColor = Color(0xFF24382D))
                                Text("${progress.coerceAtMost(achievement.target)} / ${achievement.target}", color = Muted, fontSize = 10.sp)
                            }
                        }
                        if (unlocked) androidx.compose.material3.Icon(Icons.Rounded.CheckCircle, null, tint = Neon)
                    }
                }
            }
        }
        item { Spacer(Modifier.height(12.dp)) }
    }
}

@Composable
private fun GameScreen(state: AppUiState, viewModel: GameViewModel, showOverlay: Boolean) {
    val lang = state.settings.language
    val engine = viewModel.engine
    BoxWithConstraints(Modifier.fillMaxSize().background(Bg)) {
        val landscape = maxWidth > maxHeight
        if (landscape) {
            // Keep physical left/right placement independent from Arabic RTL.
            androidx.compose.runtime.CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Row(Modifier.fillMaxSize().padding(10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (state.settings.joystickLeftLandscape) {
                        JoystickArea(
                            modifier = Modifier.weight(0.34f).fillMaxHeight(),
                            language = lang,
                            enabled = !showOverlay,
                            onDirection = engine::requestDirection,
                            leftSidePreference = true
                        )
                    }
                    Column(Modifier.weight(0.66f).fillMaxHeight()) {
                        androidx.compose.runtime.CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                            GameScoreBar(state, engine, Modifier.fillMaxWidth().height(46.dp), onPause = viewModel::requestPause)
                        }
                        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            val side = min(maxWidth.value, maxHeight.value).dp
                            GameBoard(engine, state.frameVersion, state.settings.skin, Modifier.size(side))
                        }
                    }
                    if (!state.settings.joystickLeftLandscape) {
                        JoystickArea(
                            modifier = Modifier.weight(0.34f).fillMaxHeight(),
                            language = lang,
                            enabled = !showOverlay,
                            onDirection = engine::requestDirection,
                            leftSidePreference = false
                        )
                    }
                }
            }
        } else {
            Column(Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 6.dp)) {
                GameScoreBar(state, engine, Modifier.fillMaxWidth().height(48.dp), onPause = viewModel::requestPause)
                BoxWithConstraints(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    val side = min(maxWidth.value, maxHeight.value).dp
                    GameBoard(engine, state.frameVersion, state.settings.skin, Modifier.size(side))
                }
                JoystickArea(
                    modifier = Modifier.fillMaxWidth().heightIn(min = 104.dp, max = 176.dp),
                    language = lang,
                    enabled = !showOverlay,
                    onDirection = engine::requestDirection,
                    leftSidePreference = false
                )
            }
        }
        if (showOverlay) {
            Box(Modifier.fillMaxSize().background(Color(0xD9050A0E)), contentAlignment = Alignment.Center) {
                if (state.screen == AppScreen.PAUSE) PauseCard(state, viewModel)
                else GameOverCard(state, viewModel)
            }
        }
    }
}

@Composable
private fun GameScoreBar(state: AppUiState, engine: com.ahmedzayed.zasnake.game.SnakeEngine, modifier: Modifier, onPause: () -> Unit) {
    val lang = state.settings.language
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        ScoreItem(tx(lang, "score"), engine.score.toString(), Modifier.weight(1f))
        ScoreItem(tx(lang, "best_score"), state.stats.bestScore.toString(), Modifier.weight(1f))
        ScoreItem(if (engine.mode == GameMode.TIME_ATTACK) tx(lang, "time") else tx(lang, "level"),
            if (engine.mode == GameMode.TIME_ATTACK) ((engine.timeRemainingMs + 999) / 1000).toString() else engine.level.toString(), Modifier.weight(1f))
        IconButtonFrame(onClick = onPause, modifier = Modifier.size(42.dp)) {
            androidx.compose.material3.Icon(Icons.Rounded.Pause, null, tint = Neon)
        }
    }
}

@Composable
private fun ScoreItem(label: String, value: String, modifier: Modifier) {
    Column(modifier.clip(RoundedCornerShape(8.dp)).background(Panel).border(1.dp, Color(0xFF1B4431), RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = Color(0xFF8DAF9B), fontSize = 9.sp, maxLines = 1)
        Text(value, color = Neon, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

@Composable
private fun GameBoard(engine: com.ahmedzayed.zasnake.game.SnakeEngine, frameVersion: Long, skin: SnakeSkin, modifier: Modifier) {
    Canvas(modifier = modifier.clip(RoundedCornerShape(4.dp)).border(1.dp, Color(0xFF1E6544), RoundedCornerShape(4.dp))) {
        // Capturing the frame marker invalidates the draw closure while the ViewModel ticks.
        if (frameVersion == Long.MIN_VALUE) return@Canvas
        drawRect(Color(0xFF060D12))
        val cell = size.width / com.ahmedzayed.zasnake.game.SnakeEngine.GRID_SIZE
        for (i in 0..com.ahmedzayed.zasnake.game.SnakeEngine.GRID_SIZE) {
            val p = i * cell
            drawLine(Color(0x1200FF88), Offset(p, 0f), Offset(p, size.height), 0.5f)
            drawLine(Color(0x1200FF88), Offset(0f, p), Offset(size.width, p), 0.5f)
        }
        val borderColor = if (engine.isWallsEffective) Color(0x99FF3366) else Color(0x5500CFFF)
        drawRect(borderColor, style = Stroke(width = 2f))

        // Regular red apple.
        val foodX = (engine.food.x + 0.5f) * cell
        val foodY = (engine.food.y + 0.5f) * cell
        drawCircle(Color(0xAAFF3366), cell * 0.42f, Offset(foodX, foodY))
        drawCircle(Color(0xFFFF3366), cell * 0.30f, Offset(foodX, foodY + cell * 0.03f))
        drawLine(Color(0xFF65E99C), Offset(foodX, foodY - cell * 0.23f), Offset(foodX + cell * 0.11f, foodY - cell * 0.39f), cell * 0.07f, cap = StrokeCap.Round)

        engine.specials.forEach { special ->
            val cx = (special.cell.x + 0.5f) * cell
            val cy = (special.cell.y + 0.5f) * cell
            if (special.type == SpecialType.GOLDEN) {
                drawCircle(Color(0x55FFD34E), cell * 0.48f, Offset(cx, cy))
                drawCircle(Gold, cell * 0.31f, Offset(cx, cy))
                drawCircle(Color(0xFFFFF1A8), cell * 0.10f, Offset(cx - cell * .08f, cy - cell * .09f))
            } else {
                drawLine(Color(0xFFBDE8FF), Offset(cx - cell * .22f, cy - cell * .2f), Offset(cx + cell * .2f, cy + cell * .2f), cell * .10f, cap = StrokeCap.Round)
                drawLine(Color(0xFFFFC5D3), Offset(cx + cell * .22f, cy - cell * .2f), Offset(cx - cell * .2f, cy + cell * .2f), cell * .10f, cap = StrokeCap.Round)
            }
        }

        val colors = when (skin) {
            SnakeSkin.GREEN -> listOf(Color(0xFF00FF88), Color(0xFF00C96B))
            SnakeSkin.PURPLE -> listOf(Color(0xFFD28BFF), Color(0xFF9B55E8))
            SnakeSkin.YELLOW -> listOf(Color(0xFFFFE600), Color(0xFFFFB800))
            SnakeSkin.EGYPT -> listOf(Color(0xFFFFCC33), Color(0xFFCE1126), Color(0xFFF2F2F2), Color(0xFF141414))
        }
        val segments = engine.interpolatedSnake()
        for (i in segments.indices.reversed()) {
            val (gx, gy) = segments[i]
            val inset = cell * 0.10f
            val color = if (i == 0 || colors.size == 2) colors[if (i == 0) 0 else 1] else colors[(i % (colors.size - 1)) + 1]
            val left = gx * cell + inset
            val top = gy * cell + inset
            drawRoundRect(color.copy(alpha = if (i == 0) 1f else 0.88f), Offset(left, top), Size(cell - inset * 2, cell - inset * 2), CornerRadius(cell * 0.25f))
            if (i == 0) {
                val eyeOffset = cell * 0.16f
                val eyeR = cell * 0.10f
                val baseX = gx * cell + cell / 2f
                val baseY = gy * cell + cell / 2f
                val (ex, ey) = when (engine.direction) {
                    Direction.RIGHT -> cell * 0.14f to 0f
                    Direction.LEFT -> -cell * 0.14f to 0f
                    Direction.UP -> 0f to -cell * 0.14f
                    Direction.DOWN -> 0f to cell * 0.14f
                }
                drawCircle(Color.White, eyeR, Offset(baseX - eyeOffset + ex, baseY - eyeOffset + ey))
                drawCircle(Color.White, eyeR, Offset(baseX + eyeOffset + ex, baseY + eyeOffset + ey))
                drawCircle(Color(0xFF062014), eyeR * 0.45f, Offset(baseX - eyeOffset + ex + cell * .025f, baseY - eyeOffset + ey))
                drawCircle(Color(0xFF062014), eyeR * 0.45f, Offset(baseX + eyeOffset + ex + cell * .025f, baseY - eyeOffset + ey))
            }
        }
    }
}

@Composable
private fun JoystickArea(
    modifier: Modifier,
    language: String,
    enabled: Boolean,
    onDirection: (Direction) -> Unit,
    leftSidePreference: Boolean
) {
    var active by remember { mutableStateOf(false) }
    var anchor by remember { mutableStateOf(Offset.Zero) }
    var knob by remember { mutableStateOf(Offset.Zero) }
    val density = LocalDensity.current
    val radiusPx = with(density) { 62.dp.toPx() }
    Box(modifier.clip(RoundedCornerShape(16.dp)).background(Brush.radialGradient(listOf(Color(0x0D00FF88), Color(0x0000FF88)))).pointerInput(enabled, leftSidePreference) {
        if (!enabled) return@pointerInput
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            down.consume()
            anchor = down.position
            knob = down.position
            active = true
            val pointerId = down.id
            var keepTracking = true
            while (keepTracking) {
                val event = awaitPointerEvent()
                val change = event.changes.firstOrNull { it.id == pointerId } ?: break
                val p = change.position
                knob = p
                val dx = p.x - anchor.x
                val dy = p.y - anchor.y
                if (maxOf(abs(dx), abs(dy)) > with(density) { 17.dp.toPx() }) {
                    if (abs(dx) > abs(dy)) onDirection(if (dx > 0) Direction.RIGHT else Direction.LEFT)
                    else onDirection(if (dy > 0) Direction.DOWN else Direction.UP)
                }
                change.consume()
                keepTracking = change.pressed
            }
            active = false
        }
    }, contentAlignment = Alignment.Center) {
        if (active) {
            Canvas(Modifier.fillMaxSize()) {
                val r = min(size.width, size.height) * 0.30f
                drawCircle(Color(0x2200FF88), r, anchor, style = Stroke(width = 3f))
                drawCircle(Color(0x1400FF88), r, anchor)
                val offset = knob - anchor
                val maxTravel = r * 0.56f
                val length = kotlin.math.sqrt(offset.x * offset.x + offset.y * offset.y)
                val limited = if (length > maxTravel && length > 0f) offset * (maxTravel / length) else offset
                drawCircle(Color(0xFF00FF88), r * 0.38f, anchor + limited)
                drawCircle(Color(0xFFA7FFD1), r * 0.20f, anchor + limited + Offset(-r * .07f, -r * .08f))
            }
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                androidx.compose.material3.Icon(Icons.Rounded.TouchApp, null, tint = Color(0xFF426E57), modifier = Modifier.size(25.dp))
                Text(tx(language, "tap_to_steer"), color = Color(0xFF668773), fontSize = 10.sp, modifier = Modifier.padding(top = 5.dp))
            }
        }
    }
}

@Composable
private fun PauseCard(state: AppUiState, viewModel: GameViewModel) {
    val lang = state.settings.language
    Column(Modifier.fillMaxWidth(0.86f).widthIn(max = 360.dp).clip(RoundedCornerShape(22.dp)).background(Panel).border(1.dp, Color(0xFF205139), RoundedCornerShape(22.dp)).padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(11.dp)) {
        androidx.compose.material3.Icon(Icons.Rounded.Pause, null, tint = Neon, modifier = Modifier.size(42.dp))
        Text(tx(lang, "pause_title"), color = Neon, fontSize = 23.sp, fontWeight = FontWeight.Bold)
        val detailLabel = if (viewModel.engine.mode == GameMode.TIME_ATTACK) tx(lang, "time") else tx(lang, "level")
        val detailValue = if (viewModel.engine.mode == GameMode.TIME_ATTACK) "${(viewModel.engine.timeRemainingMs + 999L) / 1000L}s" else viewModel.engine.level.toString()
        Text("${tx(lang, "score")}: ${viewModel.engine.score}  •  $detailLabel: $detailValue", color = Muted, fontSize = 13.sp)
        PrimaryActionButton(tx(lang, "continue"), Icons.Rounded.PlayArrow, viewModel::resumeFromPause)
        OutlinedActionButton(tx(lang, "restart"), Icons.Rounded.Replay, viewModel::requestRestart)
        OutlinedActionButton(tx(lang, "main_menu"), Icons.Rounded.Home, viewModel::goHomeKeepingRound)
        Text(tx(lang, "paused_save"), color = Color(0xFF689D7F), fontSize = 11.sp)
    }
}

@Composable
private fun GameOverCard(state: AppUiState, viewModel: GameViewModel) {
    val lang = state.settings.language
    val title = if (viewModel.engine.gameOverReason == "time") tx(lang, "time_up") else tx(lang, "game_over")
    Column(Modifier.fillMaxWidth(0.86f).widthIn(max = 360.dp).clip(RoundedCornerShape(22.dp)).background(Panel).border(1.dp, Color(0xFF205139), RoundedCornerShape(22.dp)).padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(11.dp)) {
        androidx.compose.material3.Icon(Icons.Rounded.EmojiEvents, null, tint = Gold, modifier = Modifier.size(44.dp))
        Text(title, color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.Bold)
        Text(tx(lang, "your_score"), color = Muted, fontSize = 13.sp)
        Text(viewModel.engine.score.toString(), color = Neon, fontSize = 42.sp, fontWeight = FontWeight.Black)
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color(0xFF10251D)).padding(13.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(tx(lang, "best_score"), color = Color(0xFFB9D4C5), fontSize = 12.sp)
            Text(state.stats.bestScore.toString(), color = Gold, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }
        PrimaryActionButton(tx(lang, "restart"), Icons.Rounded.Replay, viewModel::replayAfterGameOver)
        OutlinedActionButton(tx(lang, "main_menu"), Icons.Rounded.Home, viewModel::goHomeAfterGameOver)
    }
}

@Composable
private fun CloudStatusLine(state: AppUiState, language: String) {
    val configuredConnection = state.cloudStatus in listOf("connected", "sync_error", "synced")
    Row(Modifier.fillMaxWidth().padding(top = 1.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        androidx.compose.material3.Icon(if (configuredConnection) Icons.Rounded.CloudDone else Icons.Rounded.CloudOff, null, tint = if (configuredConnection) Color(0xFF62DCA2) else Color(0xFF8C9F94), modifier = Modifier.size(15.dp))
        Text(cloudStatusLabel(state.cloudStatus, language), color = Color(0xFF81988C), fontSize = 10.sp, modifier = Modifier.padding(start = 5.dp), textAlign = TextAlign.Center)
    }
}

private fun cloudStatusLabel(status: String, lang: String) = tx(lang, when (status) {
    "connected" -> "cloud_connected"
    "sync_error" -> "cloud_sync_error"
    "checking" -> "cloud_checking"
    "syncing" -> "cloud_syncing"
    "conflict" -> "cloud_conflict"
    "offline" -> "cloud_offline"
    "synced" -> "cloud_synced"
    else -> "cloud_not_configured"
})

@Composable
private fun SectionLabel(label: String) {
    Text(label, color = Muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 3.dp, top = 9.dp, bottom = 7.dp))
}

@Composable
private fun SettingsPanel(content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(15.dp)).background(Panel2).border(1.dp, Color(0xFF20392D), RoundedCornerShape(15.dp)).padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
}

@Composable
private fun SettingHeading(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        androidx.compose.material3.Icon(icon, null, tint = Neon, modifier = Modifier.size(19.dp))
        Text(label, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
    }
}

@Composable
private fun SettingSwitch(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        androidx.compose.material3.Icon(icon, null, tint = Muted, modifier = Modifier.size(19.dp))
        Text(label, color = Color(0xFFE0EBE4), fontSize = 13.sp, modifier = Modifier.weight(1f).padding(horizontal = 9.dp))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun ChoiceChip(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    FilterChip(selected = selected, onClick = onClick, label = { Text(label, fontSize = 12.sp, maxLines = 1) }, modifier = modifier,
        colors = androidx.compose.material3.FilterChipDefaults.filterChipColors(
            selectedContainerColor = Neon,
            selectedLabelColor = Color(0xFF042014),
            containerColor = Color(0xFF101F1A),
            labelColor = Color(0xFFCEDDD3)
        ))
}

@Composable
private fun SkinChip(skin: SnakeSkin, selected: Boolean, language: String, onClick: () -> Unit) {
    val color = when (skin) {
        SnakeSkin.GREEN -> Neon; SnakeSkin.PURPLE -> Color(0xFFD28BFF); SnakeSkin.YELLOW -> Color(0xFFFFE600); SnakeSkin.EGYPT -> Color(0xFFCE1126)
    }
    Column(Modifier.clickable { onClick() }.padding(2.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Box(Modifier.size(31.dp).clip(CircleShape).background(color).border(if (selected) 3.dp else 1.dp, if (selected) Color.White else Color(0xFF53665A), CircleShape))
        Text(tx(language, skin.key), color = if (selected) Neon else Muted, fontSize = 9.sp, textAlign = TextAlign.Center)
    }
}

@Composable
private fun StatTile(label: String, value: String, modifier: Modifier) {
    Column(modifier.clip(RoundedCornerShape(13.dp)).background(Panel).border(1.dp, Color(0xFF20392D), RoundedCornerShape(13.dp)).padding(vertical = 14.dp, horizontal = 9.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(value, color = Neon, fontWeight = FontWeight.Black, fontSize = 21.sp, textAlign = TextAlign.Center)
        Text(label, color = Muted, fontSize = 10.sp, textAlign = TextAlign.Center, lineHeight = 13.sp)
    }
}

@Composable
private fun PrimaryActionButton(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Button(onClick = onClick, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp), shape = RoundedCornerShape(13.dp), colors = ButtonDefaults.buttonColors(containerColor = Neon, contentColor = Color(0xFF042014))) {
        androidx.compose.material3.Icon(icon, null, modifier = Modifier.size(21.dp))
        Text(label, fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 8.dp))
    }
}

@Composable
private fun OutlinedActionButton(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp), shape = RoundedCornerShape(13.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDBE8E0))) {
        androidx.compose.material3.Icon(icon, null, modifier = Modifier.size(19.dp))
        Text(label, fontSize = 13.sp, modifier = Modifier.padding(start = 8.dp))
    }
}

@Composable
private fun SecondaryTile(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier, onClick: () -> Unit) {
    Column(modifier.clip(RoundedCornerShape(13.dp)).background(Panel).border(1.dp, Color(0xFF234431), RoundedCornerShape(13.dp)).clickable { onClick() }.padding(horizontal = 7.dp, vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(7.dp)) {
        androidx.compose.material3.Icon(icon, null, tint = if (icon == Icons.Rounded.EmojiEvents) Gold else Neon, modifier = Modifier.size(24.dp))
        Text(label, color = Color(0xFFE0ECE4), fontSize = 11.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center, lineHeight = 14.sp)
    }
}

@Composable
private fun IconButtonFrame(onClick: () -> Unit, modifier: Modifier = Modifier.size(44.dp), content: @Composable () -> Unit) {
    Box(modifier.clip(RoundedCornerShape(12.dp)).background(Panel).border(1.dp, Color(0xFF264737), RoundedCornerShape(12.dp)).clickable { onClick() }, contentAlignment = Alignment.Center) { content() }
}

private fun formatPlayTime(millis: Long, language: String): String {
    val totalMinutes = (millis / 60_000L).coerceAtLeast(0)
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (language == "ar") {
        if (hours > 0) "${hours}س ${minutes}د" else "${minutes}د"
    } else {
        if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
    }
}
