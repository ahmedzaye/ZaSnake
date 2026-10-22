package com.ahmedzayed.zasnake.ui

import com.ahmedzayed.zasnake.ads.RewardedAds
import com.ahmedzayed.zasnake.ads.RewardPlacement
import com.ahmedzayed.zasnake.billing.CoinPriceCatalog
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
import androidx.compose.material.icons.rounded.Settings
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

private data class ZaPalette(
    val bg: Color,
    val panel: Color,
    val panel2: Color,
    val accent: Color,
    val muted: Color,
    val gold: Color,
    val blue: Color,
    val primaryText: Color,
    val secondaryText: Color,
    val border: Color,
    val divider: Color,
    val selectedPanel: Color,
    val chipBackground: Color,
    val buttonText: Color,
    val boardBackground: Color,
    val boardGrid: Color,
    val boardBorder: Color,
    val wallBorder: Color,
    val openBorder: Color,
    val joystickGlow: Color,
    val joystickGlowClear: Color,
    val joystickRing: Color,
    val joystickInner: Color,
    val joystickKnob: Color,
    val joystickHighlight: Color,
    val joystickHint: Color,
    val overlay: Color,
    val homeTop: Color,
    val homeBottom: Color,
    val scoreLabel: Color,
    val statsTile: Color
)

private fun paletteFor(theme: GameTheme): ZaPalette = when (theme) {
    GameTheme.NIGHT -> ZaPalette(
        bg = Color(0xFF050A0E), panel = Color(0xFF0A1520), panel2 = Color(0xFF0C1914),
        accent = Color(0xFF00FF88), muted = Color(0xFF91A99D), gold = Color(0xFFFFD34E), blue = Color(0xFF9DC9FF),
        primaryText = Color(0xFFF4FAF6), secondaryText = Color(0xFFCCDAD1), border = Color(0xFF20392D), divider = Color(0xFF23392D),
        selectedPanel = Color(0xFF10251D), chipBackground = Color(0xFF101F1A), buttonText = Color(0xFF042014),
        boardBackground = Color(0xFF060D12), boardGrid = Color(0x1C00FF88), boardBorder = Color(0xFF1E6544),
        wallBorder = Color(0x99FF3366), openBorder = Color(0x5500CFFF),
        joystickGlow = Color(0x0D00FF88), joystickGlowClear = Color(0x0000FF88), joystickRing = Color(0x2200FF88),
        joystickInner = Color(0x1400FF88), joystickKnob = Color(0xFF00FF88), joystickHighlight = Color(0xFFA7FFD1),
        joystickHint = Color(0xFF426E57), overlay = Color(0xD9050A0E), homeTop = Color(0xFF07120F), homeBottom = Color(0xFF061117),
        scoreLabel = Color(0xFF8DAF9B), statsTile = Color(0xFF10251D)
    )
    GameTheme.LIGHT -> ZaPalette(
        bg = Color(0xFFF3F7F4), panel = Color(0xFFFFFFFF), panel2 = Color(0xFFEAF1EC),
        accent = Color(0xFF087F4B), muted = Color(0xFF536B5E), gold = Color(0xFF946000), blue = Color(0xFF245DB4),
        primaryText = Color(0xFF17251D), secondaryText = Color(0xFF42564A), border = Color(0xFFCCDAD1), divider = Color(0xFFD4E1D9),
        selectedPanel = Color(0xFFDFF4E8), chipBackground = Color(0xFFEEF5F0), buttonText = Color(0xFFFFFFFF),
        boardBackground = Color(0xFFFBFDFC), boardGrid = Color(0x1F087F4B), boardBorder = Color(0xFF87AA95),
        wallBorder = Color(0xFFC84654), openBorder = Color(0xFF4C8DB6),
        joystickGlow = Color(0x22087F4B), joystickGlowClear = Color(0x00087F4B), joystickRing = Color(0x33087F4B),
        joystickInner = Color(0x1F087F4B), joystickKnob = Color(0xFF087F4B), joystickHighlight = Color(0xFFB6EFD0),
        joystickHint = Color(0xFF547965), overlay = Color(0xB8F3F7F4), homeTop = Color(0xFFE6F2EA), homeBottom = Color(0xFFF8FBF9),
        scoreLabel = Color(0xFF5C7465), statsTile = Color(0xFFE0F1E6)
    )
    GameTheme.EMERALD -> ZaPalette(
        bg = Color(0xFF04150F), panel = Color(0xFF09291F), panel2 = Color(0xFF0D3628),
        accent = Color(0xFF43E5A0), muted = Color(0xFF9DCBBA), gold = Color(0xFFFFD166), blue = Color(0xFF9DE0FF),
        primaryText = Color(0xFFF1FFF7), secondaryText = Color(0xFFC5E2D5), border = Color(0xFF205A43), divider = Color(0xFF1A4B37),
        selectedPanel = Color(0xFF10422E), chipBackground = Color(0xFF0F3929), buttonText = Color(0xFF05261A),
        boardBackground = Color(0xFF061B13), boardGrid = Color(0x2543E5A0), boardBorder = Color(0xFF2B9268),
        wallBorder = Color(0xFFFF6175), openBorder = Color(0xFF559DD5),
        joystickGlow = Color(0x1943E5A0), joystickGlowClear = Color(0x0043E5A0), joystickRing = Color(0x3343E5A0),
        joystickInner = Color(0x1F43E5A0), joystickKnob = Color(0xFF43E5A0), joystickHighlight = Color(0xFFC5FFE5),
        joystickHint = Color(0xFF63A98A), overlay = Color(0xD904150F), homeTop = Color(0xFF08271C), homeBottom = Color(0xFF04150F),
        scoreLabel = Color(0xFF9BC6B3), statsTile = Color(0xFF10422E)
    )
    GameTheme.OCEAN -> ZaPalette(
        bg = Color(0xFF061627), panel = Color(0xFF0D2A42), panel2 = Color(0xFF123D58),
        accent = Color(0xFF41DDF2), muted = Color(0xFF93BFD4), gold = Color(0xFFFFD277), blue = Color(0xFF8AB8FF),
        primaryText = Color(0xFFF0FBFF), secondaryText = Color(0xFFCBEBF4), border = Color(0xFF26516A), divider = Color(0xFF224A60),
        selectedPanel = Color(0xFF154562), chipBackground = Color(0xFF15364B), buttonText = Color(0xFF06243B),
        boardBackground = Color(0xFF061D33), boardGrid = Color(0x2841DDF2), boardBorder = Color(0xFF248BA6),
        wallBorder = Color(0xFFF97D93), openBorder = Color(0xFF71D0FF),
        joystickGlow = Color(0x1741DDF2), joystickGlowClear = Color(0x0041DDF2), joystickRing = Color(0x3341DDF2),
        joystickInner = Color(0x1E41DDF2), joystickKnob = Color(0xFF41DDF2), joystickHighlight = Color(0xFFD9FAFF),
        joystickHint = Color(0xFF65AABB), overlay = Color(0xD9061627), homeTop = Color(0xFF0F3650), homeBottom = Color(0xFF04101F),
        scoreLabel = Color(0xFF9FCBDA), statsTile = Color(0xFF154562)
    )
    GameTheme.SUNSET -> ZaPalette(
        bg = Color(0xFF211222), panel = Color(0xFF352039), panel2 = Color(0xFF492A43),
        accent = Color(0xFFFF9872), muted = Color(0xFFD1ABC4), gold = Color(0xFFFFD36E), blue = Color(0xFFB7A3FF),
        primaryText = Color(0xFFFFF3EE), secondaryText = Color(0xFFF2D5DF), border = Color(0xFF704459), divider = Color(0xFF58334B),
        selectedPanel = Color(0xFF583048), chipBackground = Color(0xFF462A3E), buttonText = Color(0xFF361622),
        boardBackground = Color(0xFF28162B), boardGrid = Color(0x29FF9872), boardBorder = Color(0xFFA85F73),
        wallBorder = Color(0xFFFC5F79), openBorder = Color(0xFFBCA4FE),
        joystickGlow = Color(0x19FF9872), joystickGlowClear = Color(0x00FF9872), joystickRing = Color(0x35FF9872),
        joystickInner = Color(0x22FF9872), joystickKnob = Color(0xFFFF9872), joystickHighlight = Color(0xFFFFE4BC),
        joystickHint = Color(0xFFAA6E86), overlay = Color(0xD9211222), homeTop = Color(0xFF4A2540), homeBottom = Color(0xFF1A1128),
        scoreLabel = Color(0xFFD6ACC1), statsTile = Color(0xFF533148)
    )
    GameTheme.ROYAL -> ZaPalette(
        bg = Color(0xFF140C27), panel = Color(0xFF281742), panel2 = Color(0xFF362257),
        accent = Color(0xFFC6A0FF), muted = Color(0xFFB5A1D4), gold = Color(0xFFFFD567), blue = Color(0xFFA8B8FF),
        primaryText = Color(0xFFF8F0FF), secondaryText = Color(0xFFE2D4F7), border = Color(0xFF57417E), divider = Color(0xFF46346A),
        selectedPanel = Color(0xFF3B2860), chipBackground = Color(0xFF312149), buttonText = Color(0xFF211339),
        boardBackground = Color(0xFF1D1136), boardGrid = Color(0x28C6A0FF), boardBorder = Color(0xFF805CB1),
        wallBorder = Color(0xFFFF7198), openBorder = Color(0xFF8BB0FF),
        joystickGlow = Color(0x1BC6A0FF), joystickGlowClear = Color(0x00C6A0FF), joystickRing = Color(0x35C6A0FF),
        joystickInner = Color(0x23C6A0FF), joystickKnob = Color(0xFFC6A0FF), joystickHighlight = Color(0xFFF4DBFF),
        joystickHint = Color(0xFF9173B4), overlay = Color(0xDA140C27), homeTop = Color(0xFF352056), homeBottom = Color(0xFF120923),
        scoreLabel = Color(0xFFBDA9DA), statsTile = Color(0xFF3B2860)
    )
    GameTheme.DESERT -> ZaPalette(
        bg = Color(0xFFFFF1D8), panel = Color(0xFFFFF9ED), panel2 = Color(0xFFF5E2BC),
        accent = Color(0xFF9D431F), muted = Color(0xFF765943), gold = Color(0xFF94620B), blue = Color(0xFF3C7390),
        primaryText = Color(0xFF38251C), secondaryText = Color(0xFF674B38), border = Color(0xFFD6BA95), divider = Color(0xFFE0CAA8),
        selectedPanel = Color(0xFFF5D9AC), chipBackground = Color(0xFFF7E7CC), buttonText = Color(0xFFFFFFFF),
        boardBackground = Color(0xFFFFF7E9), boardGrid = Color(0x339D431F), boardBorder = Color(0xFFB98E65),
        wallBorder = Color(0xFFB84846), openBorder = Color(0xFF3C7996),
        joystickGlow = Color(0x229D431F), joystickGlowClear = Color(0x009D431F), joystickRing = Color(0x359D431F),
        joystickInner = Color(0x209D431F), joystickKnob = Color(0xFF9D431F), joystickHighlight = Color(0xFFFFDCA3),
        joystickHint = Color(0xFF977451), overlay = Color(0xC5FFF1D8), homeTop = Color(0xFFF1D6AB), homeBottom = Color(0xFFFFF6E5),
        scoreLabel = Color(0xFF816147), statsTile = Color(0xFFF7E0BB)
    )
    GameTheme.ROSE -> ZaPalette(
        bg = Color(0xFF220F1F), panel = Color(0xFF36172E), panel2 = Color(0xFF4A213C),
        accent = Color(0xFFFF83B9), muted = Color(0xFFD6A6BB), gold = Color(0xFFFFCC87), blue = Color(0xFFA5B7FF),
        primaryText = Color(0xFFFFF2F8), secondaryText = Color(0xFFF3CEDF), border = Color(0xFF704059), divider = Color(0xFF573049),
        selectedPanel = Color(0xFF572641), chipBackground = Color(0xFF46243A), buttonText = Color(0xFF330F28),
        boardBackground = Color(0xFF280F26), boardGrid = Color(0x29FF83B9), boardBorder = Color(0xFFAE5680),
        wallBorder = Color(0xFFFF597F), openBorder = Color(0xFF9EAAFF),
        joystickGlow = Color(0x1BFF83B9), joystickGlowClear = Color(0x00FF83B9), joystickRing = Color(0x36FF83B9),
        joystickInner = Color(0x23FF83B9), joystickKnob = Color(0xFFFF83B9), joystickHighlight = Color(0xFFFFD4EB),
        joystickHint = Color(0xFFB06A91), overlay = Color(0xD9220F1F), homeTop = Color(0xFF4A1D3A), homeBottom = Color(0xFF1B0B1B),
        scoreLabel = Color(0xFFDAB0C3), statsTile = Color(0xFF572641)
    )
}

private val LocalZaPalette = staticCompositionLocalOf { paletteFor(GameTheme.NIGHT) }
// Android's animator-duration scale (including "Remove animations") is respected.
private val LocalVisualMotionEnabled = staticCompositionLocalOf { true }
private data class RewardPop(val id: Long, val coins: Int)

private val Bg: Color @Composable get() = LocalZaPalette.current.bg
private val Panel: Color @Composable get() = LocalZaPalette.current.panel
private val Panel2: Color @Composable get() = LocalZaPalette.current.panel2
private val Neon: Color @Composable get() = LocalZaPalette.current.accent
private val Muted: Color @Composable get() = LocalZaPalette.current.muted
private val Gold: Color @Composable get() = LocalZaPalette.current.gold
private val Blue: Color @Composable get() = LocalZaPalette.current.blue
private val PrimaryText: Color @Composable get() = LocalZaPalette.current.primaryText
private val SecondaryText: Color @Composable get() = LocalZaPalette.current.secondaryText
private val BorderColor: Color @Composable get() = LocalZaPalette.current.border
private val DividerColor: Color @Composable get() = LocalZaPalette.current.divider
private val SelectedPanel: Color @Composable get() = LocalZaPalette.current.selectedPanel
private val ChipBackground: Color @Composable get() = LocalZaPalette.current.chipBackground
private val ButtonText: Color @Composable get() = LocalZaPalette.current.buttonText

private val ar = mapOf(
    "app_tagline" to "الثعبان الكلاسيكي بروح جديدة",
    "ready" to "جاهز للتحدي؟", "home_description" to "كُل التفاح، كبّر ثعبانك، وحطّم رقمك القياسي!",
    "new_game" to "لعبة جديدة", "resume_game" to "استكمال الجولة المحفوظة",
    "coin_balance" to "رصيد العملات", "coins_earned" to "عملات الجولة", "coin_unit" to "عملة",
    "settings" to "الإعدادات", "stats_achievements" to "الإحصائيات والإنجازات",
    "daily_rewards" to "المكافآت اليومية", "daily_challenges" to "تحديات اليوم",
    "daily_login" to "مكافأة الدخول", "daily_reset" to "يتجدد يوميًا الساعة 00:00 بتوقيت UTC",
    "daily_login_hint" to "استلم مكافأتك كل يوم. يوم غياب يعيد التتابع للبداية.",
    "daily_claimed" to "تم الاستلام ✓", "daily_day" to "اليوم", "daily_max" to "إجمالي تحديات اليوم",
    "daily_clock_warning" to "تاريخ الجهاز أقدم من آخر يوم محفوظ. راجع ضبط الساعة لاستلام المكافآت.",
    "choose_mode" to "اختر وضع اللعب", "choose_mode_sub" to "كل وضع بتجربة مختلفة",
    "classic" to "كلاسيكي", "classic_desc" to "اجمع التفاح وتجنب الاصطدام.",
    "time_attack" to "هجوم الوقت", "time_desc" to "60 ثانية في البداية؛ كل تفاحة تضيف ثانيتين.",
    "zen" to "هادئ Zen", "zen_desc" to "استمتع باللعب دون خسارة بسبب الاصطدام.",
    "start_classic" to "ابدأ الكلاسيكي", "start_time" to "ابدأ هجوم الوقت", "start_zen" to "ابدأ Zen",
    "back" to "رجوع", "general" to "الإعدادات العامة", "language" to "اللغة", "device_language_hint" to "تلقائي يتبع لغة جهازك. يمكنك اختيار لغة مختلفة من هنا في أي وقت.", "language_auto" to "تلقائي", "arabic" to "العربية", "english" to "English",
    "general_summary" to "اللغة والموسيقى والمؤثرات والاهتزاز", "gameplay_summary" to "الصعوبة والجدران وأسلوب التحكم",
    "appearance" to "السمات والمظهر", "appearance_summary" to "شكل الثعبان وثيمات واجهة اللعبة",
    "snake_preview_title" to "شكل ثعبانك الحالي", "snake_preview_hint" to "المعاينة بتعرض اللون والرأس والجسم المُجهّزين معًا.",
    "appearance_owned_hint" to "تظهر هنا العناصر التي تمتلكها فقط. لفتح المزيد، زر المتجر.",
    "game_themes" to "ثيمات اللعبة", "theme_summary" to "غيّر ألوان الواجهة ولوحة اللعب كما تحب.",
    "theme_night" to "Night — داكن", "theme_night_desc" to "واجهة داكنة مع أخضر نيون.",
    "theme_light" to "Light — فاتح", "theme_light_desc" to "واجهة فاتحة بتباين هادئ وواضح.",
    "theme_emerald" to "Emerald — زمردي", "theme_emerald_desc" to "أخضر زمردي أعمق ولمسات مضيئة.",
    "theme_ocean" to "محيطي", "theme_ocean_desc" to "أزرق بحري وتوهج سماوي بارد.",
    "theme_sunset" to "غروب", "theme_sunset_desc" to "برتقالي مشمشي فوق درجات بنفسجية دافئة.",
    "theme_royal" to "ملكي", "theme_royal_desc" to "بنفسجي عميق ولمسات ذهبية أنيقة.",
    "theme_desert" to "صحراوي", "theme_desert_desc" to "رمال فاتحة وتفاصيل طينية دافئة.",
    "theme_rose" to "روزي", "theme_rose_desc" to "وردي لامع مع خلفية توتية داكنة.",
    "shop_skins" to "تخصيص الثعبان", "shop_themes" to "ثيمات اللعبة",
    "shop_coins" to "شحن العملات", "coin_shop_title" to "باقات العملات",
    "coin_shop_info" to "تقدر تجمع العملات من اللعب مجانًا. شراء العملات اختياري، وهيتوفر بعد تفعيل Google Play.",
    "coin_shop_price_pending" to "السعر من Google Play عند التفعيل",
    "coin_shop_unavailable" to "الشراء غير متاح حاليًا", "coin_shop_no_payment" to "لن يتم تحصيل أي مبلغ من هذه الشاشة حاليًا.",
    "shop_colors" to "الألوان", "shop_heads" to "الرؤوس", "shop_bodies" to "الجسم",
    "head_title" to "أشكال الرأس", "body_title" to "نوع الجسم",
    "head_classic" to "كلاسيكي", "head_sharp" to "حاد", "head_cute" to "لطيف",
    "head_royal" to "ملكي", "head_cyber" to "سايبر",
    "body_segmented" to "متقطّع", "body_smooth" to "متصل ناعم",
    "body_retro_pixel" to "بيكسل قديم", "body_armored" to "مدرّع", "body_crystal" to "كريستالي",
    "coming_soon" to "قريبًا", "body_soon_hint" to "اختار نوع جسم مختلف؛ كل الأنواع بتستخدم لون السكن وشكل الرأس المجهّزين.",
    "head_hint" to "لون الرأس يتبع السكن المختار، والرأس تتجه مع حركة الثعبان.",
    "theme_shop_hint" to "اشترِ ثيمًا مرة واحدة، ثم اختره من ثيمات الإعدادات.",
    "theme_preview" to "معاينة الألوان", "theme_purchased" to "تم شراء الثيم وتفعيله",
    "theme_free_hint" to "الثيمات الثلاثة الأصلية مجانية دائمًا في الإعدادات.",
    "theme_apply" to "تفعيل", "theme_active" to "مُفعّل", "themes_coming_soon" to "خيارات Night وLight ولوحات ألوان إضافية ستتوفر في المرحلة التالية.",
    "sound_haptics" to "الصوت والاهتزاز", "sound" to "المؤثرات الصوتية", "music" to "الموسيقى الخلفية", "sfx_volume" to "مستوى المؤثرات", "music_volume" to "مستوى الموسيقى", "haptics" to "الاهتزاز",
    "gameplay" to "إعدادات اللعب والتخصيص", "difficulty" to "الصعوبة", "easy" to "سهل", "medium" to "متوسط", "hard" to "صعب",
    "walls" to "الجدران", "walls_desc" to "الاصطدام بحدود اللوحة ينهي الجولة في Classic وTime Attack.",
    "enabled" to "مفعّلة", "disabled" to "معطّلة", "skin" to "شكل الثعبان", "green" to "أخضر نيون",
    "purple" to "بنفسجي نيون", "yellow" to "أصفر نيون", "egypt" to "مصر", "uae" to "الإمارات",
    "jordan" to "الأردن", "palestine" to "فلسطين", "kuwait" to "الكويت", "saudi" to "السعودية — مستوحى",
    "arab_collection" to "مجموعة عربية", "arab_collection_desc" to "ألوان مستوحاة من أعلامنا بتصميم فني — 180 عملة لكل شكل",
    "cyber" to "سايبر", "ice" to "جليدي", "fire" to "ناري", "shop" to "المتجر", "shop_title" to "المتجر", "shop_desc" to "افتح أشكالًا جديدة باستخدام العملات التي تجمعها أثناء اللعب.", "owned" to "مملوك", "buy" to "شراء", "equip" to "اختيار", "shop_price" to "السعر",
    "controls" to "التحكم", "control_style" to "أسلوب التحكم", "floating_joystick" to "عصا عائمة", "d_pad" to "أزرار الاتجاهات", "landscape_side" to "مكان التحكم في الوضع الأفقي", "left" to "اليسار", "right" to "اليمين",
    "account_sync" to "الحساب والمزامنة", "play_games" to "Google Play Games",
    "account_settings" to "إعدادات الحساب", "account_settings_summary" to "اسم اللاعب والاتصال والحفظ السحابي",
    "player_name" to "اسم اللاعب", "player_name_local_hint" to "اسم داخل Za Snake فقط؛ لا يغيّر ملف Google Play Games.",
    "name_edit" to "تعديل", "name_save" to "حفظ الاسم", "name_invalid" to "اكتب اسمًا من 1 إلى 20 حرفًا.",
    "pgs_connect" to "تسجيل الدخول إلى Google Play Games", "pgs_retry" to "إعادة محاولة الاتصال",
    "pgs_config_needed" to "يلزم إعداد Play Games Services في Console قبل تسجيل الدخول.",
    "pgs_child_local" to "الحساب المحلي فقط متاح لهذه الفئة العمرية حاليًا.",
    "cloud_sync_title" to "حالة الحفظ السحابي", "cloud_local_name" to "اسم اللاعب محفوظ على هذا الجهاز.",
    "cloud_not_configured" to "اللعب المحلي متاح؛ إعداد Play Console مطلوب لتفعيل المزامنة.",
    "cloud_connected" to "الحساب متصل؛ المزامنة جاهزة.", "cloud_sync_error" to "المزامنة لم تكتمل؛ سنحاول مجددًا.",
    "cloud_checking" to "جارٍ التحقق من الاتصال…", "cloud_syncing" to "جارٍ مزامنة التقدم…", "cloud_synced" to "التقدم متزامن مع السحابة.", "cloud_conflict" to "تحتاج الجولة إلى اختيارك قبل إكمال المزامنة.", "cloud_offline" to "اللعب المحلي — المزامنة غير متاحة الآن.",
    "claim_reward" to "استلام المكافأة", "claim_short" to "استلام", "claimed_short" to "تم ✓", "reward_claimed" to "تم الاستلام ✓",
    "reward_pending" to "قيد التقدم", "reward_ready" to "جاهز للاستلام",
    "achievement_reward_total" to "إجمالي المكافآت", "achievement_reward_earned" to "عملات مستلمة",
    "achievement_reward_available" to "عملات جاهزة للاستلام",
    "statistics" to "الإحصائيات", "achievements" to "الإنجازات", "best_score" to "أفضل نتيجة",
    "games" to "المباريات", "food_eaten" to "التفاح المأكول", "play_time" to "وقت اللعب",
    "golden_apples" to "التفاح الذهبي", "scissors" to "المقصات المستخدمة", "longest_snake" to "أطول ثعبان",
    "unlocked" to "مفتوح", "locked" to "مقفل", "no_achievements" to "واصل اللعب لفتح إنجازات جديدة.",
    "pause_title" to "اللعبة متوقفة", "continue" to "متابعة اللعب", "restart" to "إعادة الجولة",
    "main_menu" to "القائمة الرئيسية", "game_over" to "انتهت الجولة!", "time_up" to "انتهى الوقت!", "new_record_visual" to "رقم قياسي جديد!",
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
    "coin_balance" to "Coin balance", "coins_earned" to "Round reward", "coin_unit" to "coins",
    "stats_achievements" to "Stats & achievements",
    "daily_rewards" to "Daily rewards", "daily_challenges" to "Today's challenges",
    "daily_login" to "Daily login bonus", "daily_reset" to "Resets daily at 00:00 UTC",
    "daily_login_hint" to "Claim every day. Missing a day resets the streak to day one.",
    "daily_claimed" to "Claimed ✓", "daily_day" to "Day", "daily_max" to "Today's challenge total",
    "daily_clock_warning" to "Your device date is earlier than your saved progress. Check the clock to claim rewards.",
    "choose_mode" to "Choose a game mode", "choose_mode_sub" to "Three modes, three experiences",
    "classic" to "Classic", "classic_desc" to "Collect apples and avoid collisions.",
    "time_attack" to "Time Attack", "time_desc" to "Start with 60 seconds; each apple adds two seconds.",
    "zen" to "Zen", "zen_desc" to "Relax without losing from collisions.", "start_classic" to "Start Classic",
    "start_time" to "Start Time Attack", "start_zen" to "Start Zen", "back" to "Back",
    "language" to "Language", "device_language_hint" to "Automatic follows your device language. You can choose a different language anytime.", "language_auto" to "Automatic", "arabic" to "العربية", "english" to "English", "sound_haptics" to "Sound & haptics",
    "general" to "General settings", "general_summary" to "Language, music, sound effects and vibration",
    "gameplay" to "Game & controls", "gameplay_summary" to "Difficulty, walls and control style",
    "appearance" to "Themes & appearance", "appearance_summary" to "Snake skin and game interface themes",
    "snake_preview_title" to "Your equipped snake", "snake_preview_hint" to "Preview shows your equipped color, head and body together.",
    "appearance_owned_hint" to "Only items you own are shown. Unlock more in the shop.",
    "game_themes" to "Game themes", "theme_summary" to "Change the interface and board colors to match your style.",
    "theme_night" to "Night", "theme_night_desc" to "Dark surfaces with neon green accents.",
    "theme_light" to "Light", "theme_light_desc" to "A bright interface with calm, clear contrast.",
    "theme_emerald" to "Emerald", "theme_emerald_desc" to "Deep emerald greens with luminous accents.",
    "theme_ocean" to "Ocean", "theme_ocean_desc" to "Deep sea blue with cool cyan glow.",
    "theme_sunset" to "Sunset", "theme_sunset_desc" to "Warm apricot over violet twilight.",
    "theme_royal" to "Royal", "theme_royal_desc" to "Deep violet with elegant golden accents.",
    "theme_desert" to "Desert", "theme_desert_desc" to "Bright sand with warm terracotta details.",
    "theme_rose" to "Rose", "theme_rose_desc" to "Vivid pink against dark berry surfaces.",
    "shop_skins" to "Customize snake", "shop_themes" to "Game themes",
    "shop_coins" to "Coins", "coin_shop_title" to "Coin packs",
    "coin_shop_info" to "You can earn coins by playing for free. Buying coins is optional and will be available after Google Play setup.",
    "coin_shop_price_pending" to "Price from Google Play when available",
    "coin_shop_unavailable" to "Purchases not available yet", "coin_shop_no_payment" to "No payment can be made from this screen yet.",
    "shop_colors" to "Colors", "shop_heads" to "Heads", "shop_bodies" to "Body",
    "head_title" to "Head styles", "body_title" to "Body type",
    "head_classic" to "Classic", "head_sharp" to "Sharp", "head_cute" to "Cute",
    "head_royal" to "Royal", "head_cyber" to "Cyber",
    "body_segmented" to "Segmented", "body_smooth" to "Smooth",
    "body_retro_pixel" to "Retro Pixel", "body_armored" to "Armored", "body_crystal" to "Crystal",
    "coming_soon" to "Coming soon", "body_soon_hint" to "Choose a body style. Every style inherits your equipped skin colors and head.",
    "head_hint" to "Head colors follow your equipped skin and face the movement direction.",
    "theme_shop_hint" to "Buy a theme once, then select it in Settings → Themes.",
    "theme_preview" to "Preview colors", "theme_purchased" to "Theme purchased and applied",
    "theme_free_hint" to "The original three themes stay free in Settings.",
    "theme_apply" to "Apply", "theme_active" to "Active", "themes_coming_soon" to "Night, Light and additional color palettes are coming in the next stage.",
    "sound" to "Sound effects", "music" to "Background music", "sfx_volume" to "Effects volume", "music_volume" to "Music volume", "haptics" to "Vibration", "difficulty" to "Difficulty",
    "easy" to "Easy", "medium" to "Medium", "hard" to "Hard", "walls" to "Walls",
    "walls_desc" to "Board edges end the run in Classic and Time Attack.", "enabled" to "On", "disabled" to "Off",
    "skin" to "Snake skin", "green" to "Neon green", "purple" to "Neon purple", "yellow" to "Neon yellow", "egypt" to "Egypt",
    "uae" to "UAE", "jordan" to "Jordan", "palestine" to "Palestine", "kuwait" to "Kuwait", "saudi" to "Saudi-inspired",
    "arab_collection" to "Arab Collection", "arab_collection_desc" to "Artistic flag-inspired colors — 180 coins per skin",
    "cyber" to "Cyber", "ice" to "Ice", "fire" to "Fire", "shop" to "Shop", "shop_title" to "Shop", "shop_desc" to "Unlock new skins with coins earned while playing.", "owned" to "Owned", "buy" to "Buy", "equip" to "Equip", "shop_price" to "Price",
    "controls" to "Controls", "control_style" to "Control style", "floating_joystick" to "Floating joystick", "d_pad" to "D-Pad", "landscape_side" to "Landscape joystick side", "left" to "Left", "right" to "Right",
    "account_sync" to "Account & sync", "play_games" to "Google Play Games",
    "account_settings" to "Account settings", "account_settings_summary" to "Player name, sign-in and cloud save",
    "player_name" to "Player name", "player_name_local_hint" to "Only your Za Snake nickname; does not change your Play Games profile.",
    "name_edit" to "Edit", "name_save" to "Save name", "name_invalid" to "Enter a name between 1 and 20 characters.",
    "pgs_connect" to "Sign in to Google Play Games", "pgs_retry" to "Retry connection",
    "pgs_config_needed" to "Configure Play Games Services in Console before signing in.",
    "pgs_child_local" to "Local-only account for this age group at present.",
    "cloud_sync_title" to "Cloud save status", "cloud_local_name" to "Your player name is saved on this device.",
    "cloud_not_configured" to "Local play is ready; Play Console setup is required for cloud sync.",
    "cloud_connected" to "Account connected; sync is ready.", "cloud_sync_error" to "Sync did not finish; we'll retry.",
    "cloud_checking" to "Checking connection…", "cloud_syncing" to "Syncing progress…", "cloud_synced" to "Progress synced to cloud.", "cloud_conflict" to "Choose a round before sync can continue.", "cloud_offline" to "Local play — sync is unavailable right now.",
    "claim_reward" to "Claim reward", "claim_short" to "Claim", "claimed_short" to "Claimed ✓", "reward_claimed" to "Claimed ✓",
    "reward_pending" to "In progress", "reward_ready" to "Ready to claim",
    "achievement_reward_total" to "Total rewards", "achievement_reward_earned" to "Coins claimed",
    "achievement_reward_available" to "Coins ready to claim",
    "statistics" to "Statistics", "achievements" to "Achievements", "best_score" to "Best score", "games" to "Games",
    "food_eaten" to "Apples eaten", "play_time" to "Play time", "golden_apples" to "Golden apples",
    "scissors" to "Scissors used", "longest_snake" to "Longest snake", "unlocked" to "Unlocked", "locked" to "Locked",
    "no_achievements" to "Keep playing to unlock achievements.", "pause_title" to "Game paused", "continue" to "Resume game",
    "restart" to "Restart round", "main_menu" to "Main menu", "game_over" to "Game over!", "time_up" to "Time's up!", "new_record_visual" to "NEW HIGH SCORE!",
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

private fun Context.findActivity(): Activity? {
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
    onExitApp: () -> Unit
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
    val selectedPalette = paletteFor(state.settings.theme)
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
                isAppearanceLightStatusBars = state.settings.theme == GameTheme.LIGHT || state.settings.theme == GameTheme.DESERT
                isAppearanceLightNavigationBars = state.settings.theme == GameTheme.LIGHT || state.settings.theme == GameTheme.DESERT
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
                                    })
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
            title = { Text(if (language == "ar") "وجدنا جولتين مختلفتين" else "Two different rounds found", color = selectedPalette.accent, fontWeight = FontWeight.Bold) },
            text = { Text(if (language == "ar") "هنحتفظ بإحصائياتك وإنجازاتك، لكن محتاجين نعرف أي جولة تريد استكمالها." else "Your stats and achievements will merge. Choose which round you want to continue.", color = selectedPalette.secondaryText) },
            confirmButton = {
                TextButton(onClick = {
                    val resolution = viewModel.resolveRoundConflict(true)
                    if (resolution != null) {
                        val done: (Boolean) -> Unit = { viewModel.cloudWriteFinished(it) }
                        if (resolution.fromPgsConflict) playGamesBridge.resolvePendingConflict(resolution.payload, resolution.chooseServerSnapshot, done)
                        else playGamesBridge.saveBundle(resolution.payload, done)
                    }
                }) { Text(if (language == "ar") "استخدام الجولة السحابية" else "Use cloud round", color = selectedPalette.accent) }
            },
            dismissButton = {
                TextButton(onClick = {
                    val resolution = viewModel.resolveRoundConflict(false)
                    if (resolution != null) {
                        val done: (Boolean) -> Unit = { viewModel.cloudWriteFinished(it) }
                        if (resolution.fromPgsConflict) playGamesBridge.resolvePendingConflict(resolution.payload, resolution.chooseServerSnapshot, done)
                        else playGamesBridge.saveBundle(resolution.payload, done)
                    }
                }) { Text(if (language == "ar") "الاحتفاظ بجولة هذا الجهاز" else "Keep this device's round", color = selectedPalette.muted) }
            },
            containerColor = selectedPalette.panel,
            titleContentColor = selectedPalette.accent,
            textContentColor = selectedPalette.primaryText
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
            title = { Text(title, color = selectedPalette.accent, fontWeight = FontWeight.Bold) },
            text = { Text(message, color = selectedPalette.secondaryText) },
            confirmButton = {
                TextButton(onClick = {
                    if (pending == ConfirmationAction.EXIT_APP) {
                        viewModel.dismissConfirmation()
                        onExitApp()
                    } else viewModel.confirmPendingAction()
                }) { Text(tx(language, "confirm"), color = selectedPalette.accent) }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissConfirmation) { Text(tx(language, "cancel"), color = selectedPalette.muted) }
            },
            containerColor = selectedPalette.panel,
            titleContentColor = selectedPalette.accent,
            textContentColor = selectedPalette.primaryText
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
    onSettings: () -> Unit,
    onStats: () -> Unit,
    onShop: () -> Unit,
    onDaily: () -> Unit,
    onClaimAchievements: () -> Unit
) {
    val lang = state.settings.language
    Column(
        Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(LocalZaPalette.current.homeTop, Bg, LocalZaPalette.current.homeBottom))).padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Za Snake", color = Neon, fontSize = 28.sp, fontWeight = FontWeight.Black, letterSpacing = 0.5.sp)
                Text(tx(lang, "app_tagline"), color = Muted, fontSize = 12.sp)
            }
        }
        HorizontalDivider(color = DividerColor)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Row(
                modifier = Modifier.clip(RoundedCornerShape(18.dp)).background(LocalZaPalette.current.statsTile)
                    .border(1.dp, BorderColor, RoundedCornerShape(18.dp)).padding(horizontal = 13.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                Text("🪙", fontSize = 17.sp)
                Text(state.stats.coins.toString(), color = Gold, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(tx(lang, "coin_unit"), color = SecondaryText, fontSize = 11.sp)
            }
        }
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            val heroSize = min(min(maxWidth.value * 0.70f, maxHeight.value * 0.52f), 230f).dp.coerceAtLeast(80.dp)
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Image(
                    painter = painterResource(R.drawable.za_snake_mascot),
                    contentDescription = "Za Snake mascot",
                    modifier = Modifier.size(heroSize).clip(RoundedCornerShape(28.dp)).border(1.dp, BorderColor, RoundedCornerShape(28.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(Modifier.height(14.dp))
                Text(tx(lang, "ready"), color = PrimaryText, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text(tx(lang, "home_description"), color = Muted, fontSize = 13.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp))
            }
        }
        PrimaryActionButton(tx(lang, "new_game"), Icons.Rounded.PlayArrow, onNewGame)
        if (state.hasSavedGame) {
            OutlinedActionButton(tx(lang, "resume_game"), Icons.Rounded.Replay, onResume)
        }
        PrimaryActionButton(tx(lang, "shop"), Icons.Rounded.ShoppingBag, onShop)
        OutlinedActionButton(tx(lang, "daily_rewards"), Icons.Rounded.CardGiftcard, onDaily)
        val claimableCount = ACHIEVEMENTS.count { achievement ->
            achievement.id !in state.stats.claimedAchievementRewards &&
                (achievement.id in state.stats.unlockedAchievements ||
                    achievement.progress(state.stats) >= achievement.target)
        }
        if (claimableCount > 0) {
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                .background(Gold.copy(alpha = .12f))
                .border(1.dp, Gold.copy(alpha = .55f), RoundedCornerShape(12.dp))
                .clickable(onClick = onClaimAchievements)
                .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                androidx.compose.material3.Icon(Icons.Rounded.CardGiftcard, null, tint = Gold, modifier = Modifier.size(20.dp))
                Text(if (lang == "ar") "استلم المكافآت!" else "Claim your rewards!",
                    color = PrimaryText, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f))
                Text(claimableCount.toString(), color = Gold, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SecondaryTile(tx(lang, "stats_achievements"), Icons.Rounded.EmojiEvents, Modifier.weight(1f), onStats)
            SecondaryTile(tx(lang, "settings"), Icons.Rounded.Settings, Modifier.weight(1f), onSettings)
        }
        CloudStatusLine(state, lang)
    }
}

@Composable
private fun DailyRewardsScreen(
    state: AppUiState,
    onBack: () -> Unit,
    onClaimChallenge: (String) -> Boolean,
    onClaimLogin: () -> Boolean,
    onRefresh: () -> Unit
) {
    val lang = state.settings.language
    val date = state.dailyDate
    val stats = state.stats
    val progress = stats.dailyProgress(date)
    val clockIsValid = date >= stats.latestDailyDate
    val loginClaimed = date in stats.claimedDailyLogins
    val nextDay = if (loginClaimed) stats.claimedDailyLogins[date] ?: 1 else stats.nextDailyLoginDay(date)
    val currentCoins = DAILY_LOGIN_COIN_SEQUENCE[(nextDay - 1).coerceIn(0, 6)]
    // Periodic date rollover, including when the daily screen is left open at midnight UTC.
    LaunchedEffect(date) {
        while (true) {
            kotlinx.coroutines.delay(30_000)
            onRefresh()
        }
    }
    LazyColumn(
        Modifier.fillMaxSize().background(Bg).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item { ScreenHeader(tx(lang, "daily_rewards"), onBack) }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = SelectedPanel), shape = RoundedCornerShape(14.dp)) {
                Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text("🎁 ${tx(lang, "daily_login")}", color = Neon, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    Text(tx(lang, "daily_login_hint"), color = Muted, fontSize = 11.sp)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        DAILY_LOGIN_COIN_SEQUENCE.forEachIndexed { index, amount ->
                            val day = index + 1
                            val chosen = day == nextDay
                            Column(
                                modifier = Modifier.weight(1f).clip(RoundedCornerShape(8.dp))
                                    .background(if (chosen) Neon.copy(alpha = 0.15f) else Panel)
                                    .border(1.dp, if (chosen) Neon else BorderColor, RoundedCornerShape(8.dp))
                                    .padding(vertical = 7.dp, horizontal = 1.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Text("${tx(lang, "daily_day")} $day", color = Muted, fontSize = 8.sp, maxLines = 1)
                                Text("🪙$amount", color = if (chosen) Gold else PrimaryText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("$date · +$currentCoins 🪙", color = Gold, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        if (loginClaimed) {
                            Text(tx(lang, "daily_claimed"), color = Neon, fontSize = 12.sp)
                        } else {
                            Button(onClick = { onClaimLogin() }, enabled = clockIsValid, colors = ButtonDefaults.buttonColors(containerColor = Neon, contentColor = ButtonText),
                                modifier = Modifier.height(34.dp), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)) {
                                Text(tx(lang, "claim_short"), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
        if (!clockIsValid) {
            item { Text(tx(lang, "daily_clock_warning"), color = Gold, fontSize = 12.sp) }
        }
        item {
            Text("🎯 ${tx(lang, "daily_challenges")}", color = Neon, fontWeight = FontWeight.Bold, fontSize = 17.sp)
            Text("${tx(lang, "daily_max")}: ${DailyChallenge.entries.sumOf { it.coins }} 🪙", color = Gold, fontSize = 11.sp)
        }
        items(DailyChallenge.entries.toList(), key = { it.id }) { challenge ->
            val current = challenge.progress(progress).coerceAtLeast(0L)
            val finished = current >= challenge.target
            val claimed = challenge.claimId(date) in stats.claimedDailyChallenges
            val ratio = (current.toFloat() / challenge.target.toFloat()).coerceIn(0f, 1f)
            Card(colors = CardDefaults.cardColors(containerColor = if (finished) SelectedPanel else Panel),
                shape = RoundedCornerShape(11.dp), modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth().heightIn(min = 72.dp).padding(9.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(challenge.icon, fontSize = 24.sp)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(if (lang == "ar") challenge.arName else challenge.enName, color = PrimaryText,
                            fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(if (lang == "ar") challenge.arDescription else challenge.enDescription,
                            color = Muted, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            LinearProgressIndicator(progress = { ratio }, modifier = Modifier.weight(1f).height(4.dp).clip(CircleShape),
                                color = Neon, trackColor = BorderColor)
                            Text("${current.coerceAtMost(challenge.target)}/${challenge.target}", color = Muted, fontSize = 10.sp)
                        }
                    }
                    Column(Modifier.width(77.dp), horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("🪙 ${challenge.coins}", color = Gold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        when {
                            claimed -> Text(tx(lang, "claimed_short"), color = Neon, fontSize = 10.sp)
                            finished -> Button(onClick = { onClaimChallenge(challenge.id) }, enabled = clockIsValid,
                                modifier = Modifier.fillMaxWidth().height(30.dp),
                                contentPadding = PaddingValues(horizontal = 2.dp, vertical = 0.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Neon, contentColor = ButtonText)) {
                                Text(tx(lang, "claim_short"), fontSize = 11.sp, maxLines = 1)
                            }
                            else -> Text(tx(lang, "reward_pending"), color = Muted, fontSize = 10.sp)
                        }
                    }
                }
            }
        }
        item { Text(tx(lang, "daily_reset"), color = Muted, fontSize = 11.sp, modifier = Modifier.padding(bottom = 12.dp)) }
    }
}

@Composable
private fun ShopScreen(
    state: AppUiState,
    coinPriceCatalog: CoinPriceCatalog?,
    onBack: () -> Unit,
    onPurchase: (SnakeSkin) -> Boolean,
    onEquip: (SnakeSkin) -> Unit,
    onPurchaseHead: (SnakeHeadStyle) -> Boolean,
    onEquipHead: (SnakeHeadStyle) -> Unit,
    onPurchaseBody: (SnakeBodyType) -> Boolean,
    onEquipBody: (SnakeBodyType) -> Unit,
    onPurchaseTheme: (GameTheme) -> Boolean,
    onApplyTheme: (GameTheme) -> Unit
) {
    val lang = state.settings.language
    val coinPriceState = coinPriceCatalog?.state
    val playPrices = coinPriceCatalog?.localizedPrices.orEmpty()
    val prices = SnakeSkin.entries.associateWith { it.priceCoins }
    var previewSkin by remember(state.settings.skin) { mutableStateOf(state.settings.skin) }
    var pendingBuy by remember { mutableStateOf<SnakeSkin?>(null) }
    var tab by remember { mutableStateOf("skins") }
    LaunchedEffect(tab, coinPriceCatalog) {
        if (tab == "coins") coinPriceCatalog?.refresh()
    }
    var snakeTab by remember { mutableStateOf("colors") }
    var previewHead by remember(state.settings.headStyle) { mutableStateOf(state.settings.headStyle) }
    var pendingHeadBuy by remember { mutableStateOf<SnakeHeadStyle?>(null) }
    var previewBody by remember(state.settings.bodyType) { mutableStateOf(state.settings.bodyType) }
    var pendingBodyBuy by remember { mutableStateOf<SnakeBodyType?>(null) }
    var previewTheme by remember(state.settings.theme) {
        mutableStateOf(state.settings.theme.takeUnless { it.isFree } ?: GameTheme.OCEAN)
    }
    val selectedColor = when (tab) {
        "skins" -> snakeSkinColors(previewSkin).first()
        "themes" -> paletteFor(previewTheme).accent
        else -> Gold
    }
    var pendingThemeBuy by remember { mutableStateOf<GameTheme?>(null) }

    Column(Modifier.fillMaxSize().background(Bg).padding(horizontal = 16.dp, vertical = 12.dp)) {
        ScreenHeader(tx(lang, "shop_title"), onBack)
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ChoiceChip(tx(lang, "shop_skins"), tab == "skins") { tab = "skins" }
            ChoiceChip(tx(lang, "shop_themes"), tab == "themes") { tab = "themes" }
            ChoiceChip(tx(lang, "shop_coins"), tab == "coins") { tab = "coins" }
        }
        Spacer(Modifier.height(8.dp))
        // This preview and balance stay pinned while only the inventory scrolls.
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(15.dp))
                .background(Brush.linearGradient(listOf(LocalZaPalette.current.statsTile, Panel)))
                .border(1.dp, selectedColor.copy(alpha = .65f), RoundedCornerShape(15.dp))
                .padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(if (lang == "ar") "رصيد العملات" else "COIN BALANCE", color = Muted,
                    fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Text("🪙 ${state.stats.coins}", color = Gold, fontSize = 20.sp, fontWeight = FontWeight.Black)
            }
            if (tab != "coins") {
                Box(Modifier.size(90.dp, 68.dp).clip(RoundedCornerShape(11.dp)).background(Color(0xFF07131B))) {
                    if (tab == "skins") when (snakeTab) {
                        "heads" -> HeadStylePreview(previewHead, state.settings.skin, Modifier.fillMaxSize(), state.settings.bodyType)
                        "body" -> BodyStylePreview(previewBody, state.settings.skin, state.settings.headStyle, Modifier.fillMaxSize())
                        else -> SkinPreview(previewSkin, Modifier.fillMaxSize(), state.settings.headStyle, state.settings.bodyType)
                    } else ThemeMiniPreview(previewTheme, state.settings.skin, Modifier.fillMaxSize())
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        LazyColumn(Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (tab == "skins") {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ChoiceChip(tx(lang, "shop_colors"), snakeTab == "colors") { snakeTab = "colors" }
                ChoiceChip(tx(lang, "shop_heads"), snakeTab == "heads") { snakeTab = "heads" }
                ChoiceChip(tx(lang, "shop_bodies"), snakeTab == "body") { snakeTab = "body" }
            }
        }
        if (snakeTab == "colors") {
        item {
            Text(if (lang == "ar") "ألوان الثعبان" else "SNAKE COLORS", color = PrimaryText, fontWeight = FontWeight.Black, fontSize = 15.sp)
        }
        items(listOf(SnakeSkin.GREEN, SnakeSkin.PURPLE, SnakeSkin.YELLOW, SnakeSkin.CYBER, SnakeSkin.ICE, SnakeSkin.FIRE) + ARAB_COLLECTION) { skin ->
            if (skin == SnakeSkin.EGYPT) {
                Column(Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(tx(lang, "arab_collection"), color = PrimaryText,
                        fontWeight = FontWeight.Black, fontSize = 17.sp)
                    Text(tx(lang, "arab_collection_desc"), color = Muted, fontSize = 12.sp)
                }
            }
            val owned = skin.key in state.settings.ownedSkinKeys
            val equipped = state.settings.skin == skin
            val price = prices.getValue(skin)
            val color = snakeSkinColors(skin).first()
            val active = previewSkin == skin
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
                    .background(if (active) LocalZaPalette.current.statsTile else Panel)
                    .border(if (active) 1.5.dp else 1.dp, if (active) color else BorderColor, RoundedCornerShape(18.dp))
                    .clickable { previewSkin = skin }.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.size(92.dp, 72.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFF07131B))) {
                        SkinPreview(skin, Modifier.fillMaxSize(), state.settings.headStyle, state.settings.bodyType)
                        if (active) {
                            Box(Modifier.align(Alignment.TopEnd).padding(5.dp).size(19.dp).clip(CircleShape).background(color), contentAlignment = Alignment.Center) {
                                androidx.compose.material3.Icon(Icons.Rounded.CheckCircle, null, tint = Color(0xFF07131B), modifier = Modifier.size(17.dp))
                            }
                        }
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(tx(lang, skin.key), color = PrimaryText, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(
                            when (skin) {
                                SnakeSkin.GREEN -> if (lang == "ar") "أخضر كلاسيكي" else "Classic green"
                                SnakeSkin.PURPLE -> if (lang == "ar") "بنفسجي ناعم" else "Soft purple"
                                SnakeSkin.YELLOW -> if (lang == "ar") "أصفر مشرق" else "Bright yellow"
                                SnakeSkin.CYBER -> if (lang == "ar") "نيون رقمي" else "Digital neon"
                                SnakeSkin.ICE -> if (lang == "ar") "لمسة جليدية" else "Frost finish"
                                SnakeSkin.FIRE -> if (lang == "ar") "توهج ناري" else "Fiery glow"
                                SnakeSkin.EGYPT -> if (lang == "ar") "أحمر وفضي وأسود مع لمسة ذهبية" else "Red, ivory, black & gold"
                                SnakeSkin.UAE -> if (lang == "ar") "أخضر زمردي ولمسة حمراء" else "Emerald and ruby accents"
                                SnakeSkin.JORDAN -> if (lang == "ar") "شرائط داكنة وتفاصيل قرمزية" else "Dark stripes with crimson"
                                SnakeSkin.PALESTINE -> if (lang == "ar") "أخضر زيتوني وتباين أسود وأبيض" else "Olive, ivory and onyx"
                                SnakeSkin.KUWAIT -> if (lang == "ar") "أسود فحمي وألوان مشرقة" else "Charcoal with bright stripes"
                                SnakeSkin.SAUDI -> if (lang == "ar") "أخضر فاخر وأبيض ولمسة ذهبية" else "Royal green, ivory and gold"
                            }, color = Muted, fontSize = 11.sp
                        )
                        Text(if (owned) if (equipped) (if (lang == "ar") "مُجهّز حاليًا" else "Currently equipped") else tx(lang, "owned") else "$price 🪙", color = if (owned) Neon else Gold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(
                        onClick = { previewSkin = skin }, modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.75f))
                    ) { Text(if (lang == "ar") "معاينة" else "Preview", color = color, fontWeight = FontWeight.Bold) }
                    if (owned) {
                        Button(
                            onClick = { onEquip(skin) }, enabled = !equipped, modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Neon, contentColor = ButtonText),
                            shape = RoundedCornerShape(12.dp)
                        ) { Text(if (equipped) (if (lang == "ar") "مُجهّز" else "Equipped") else tx(lang, "equip"), fontWeight = FontWeight.Bold) }
                    } else {
                        Button(
                            onClick = { previewSkin = skin; pendingBuy = skin }, enabled = state.stats.coins >= price,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Neon, contentColor = ButtonText),
                            shape = RoundedCornerShape(12.dp)
                        ) { Text(tx(lang, "buy"), fontWeight = FontWeight.Bold) }
                    }
                }
                if (!owned && state.stats.coins < price) {
                    Text(if (lang == "ar") "ناقصك ${price - state.stats.coins} عملة" else "${price - state.stats.coins} more coins needed", color = Muted, fontSize = 11.sp)
                }
            }
        }
        } else if (snakeTab == "heads") {
            item {
                Text(tx(lang, "head_title"), color = PrimaryText, fontSize = 16.sp, fontWeight = FontWeight.Black)
                Text(tx(lang, "head_hint"), color = Muted, fontSize = 12.sp)
            }
            items(SnakeHeadStyle.entries) { head ->
                val owned = head.isFree || head.key in state.settings.ownedHeadKeys
                val equipped = state.settings.headStyle == head
                val highlighted = previewHead == head
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
                    .background(if (highlighted) SelectedPanel else Panel)
                    .border(if (highlighted) 1.5.dp else 1.dp, if (highlighted) Neon else BorderColor, RoundedCornerShape(18.dp))
                    .clickable { previewHead = head }.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(Modifier.size(92.dp, 72.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFF07131B))) {
                            HeadStylePreview(head, state.settings.skin, Modifier.fillMaxSize(), state.settings.bodyType)
                        }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Text(tx(lang, "head_${head.key}"), color = PrimaryText, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text(if (owned) (if (equipped) (if (lang == "ar") "مُجهّز" else "Equipped") else tx(lang, "owned"))
                                else "${head.priceCoins} 🪙", color = if (owned) Neon else Gold, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { previewHead = head }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) {
                            Text(if (lang == "ar") "معاينة" else "Preview", color = Neon, fontSize = 12.sp)
                        }
                        Button(
                            onClick = { if (owned) onEquipHead(head) else pendingHeadBuy = head },
                            enabled = if (owned) !equipped else state.stats.coins >= head.priceCoins,
                            modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Neon, contentColor = ButtonText)
                        ) {
                            Text(if (owned) (if (equipped) (if (lang == "ar") "مُجهّز" else "Equipped") else tx(lang, "equip"))
                                 else tx(lang, "buy"), fontWeight = FontWeight.Bold)
                        }
                    }
                    if (!owned && state.stats.coins < head.priceCoins) {
                        Text(if (lang == "ar") "ناقصك ${head.priceCoins - state.stats.coins} عملة"
                            else "${head.priceCoins - state.stats.coins} more coins needed", color = Muted, fontSize = 11.sp)
                    }
                }
            }
        } else {
            item {
                Text(tx(lang, "body_title"), color = PrimaryText, fontWeight = FontWeight.Black, fontSize = 16.sp)
                Text(tx(lang, "body_soon_hint"), color = Muted, fontSize = 12.sp)
            }
            items(SnakeBodyType.entries) { body ->
                val owned = body.isFree || body.key in state.settings.ownedBodyKeys
                val equipped = state.settings.bodyType == body
                val highlighted = previewBody == body
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
                    .background(if (highlighted) SelectedPanel else Panel)
                    .border(if (highlighted) 1.5.dp else 1.dp, if (highlighted) Neon else BorderColor, RoundedCornerShape(18.dp))
                    .clickable { previewBody = body }.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Box(Modifier.fillMaxWidth().height(95.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFF07131B))) {
                        BodyStylePreview(body, state.settings.skin, state.settings.headStyle, Modifier.fillMaxSize())
                    }
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(tx(lang, "body_${body.key}"), color = PrimaryText,
                            fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.weight(1f))
                        Text(if (owned) (if (equipped) (if (lang == "ar") "مُجهّز" else "Equipped") else tx(lang, "owned"))
                            else "${body.priceCoins} 🪙", color = if (owned) Neon else Gold,
                            fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { previewBody = body }, modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)) {
                            Text(if (lang == "ar") "معاينة" else "Preview", color = Neon, fontSize = 12.sp)
                        }
                        Button(onClick = { if (owned) onEquipBody(body) else pendingBodyBuy = body },
                            enabled = if (owned) !equipped else state.stats.coins >= body.priceCoins,
                            modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Neon, contentColor = ButtonText)) {
                            Text(if (owned) (if (equipped) (if (lang == "ar") "مُجهّز" else "Equipped") else tx(lang, "equip"))
                                else tx(lang, "buy"), fontWeight = FontWeight.Bold)
                        }
                    }
                    if (!owned && state.stats.coins < body.priceCoins) {
                        Text(if (lang == "ar") "ناقصك ${body.priceCoins - state.stats.coins} عملة"
                            else "${body.priceCoins - state.stats.coins} more coins needed", color = Muted, fontSize = 11.sp)
                    }
                }
            }
        }
        } // snake customization tab
        else if (tab == "themes") {
            item {
                Text(tx(lang, "theme_shop_hint"), color = Muted, fontSize = 12.sp, lineHeight = 18.sp)
            }
            items(GameTheme.entries.filterNot { it.isFree }) { theme ->
                val p = paletteFor(theme)
                val owned = theme.key in state.settings.ownedThemeKeys
                val applied = state.settings.theme == theme
                val focused = previewTheme == theme
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
                        .background(if (focused) SelectedPanel else Panel)
                        .border(if (focused) 1.5.dp else 1.dp, if (focused) p.accent else BorderColor, RoundedCornerShape(18.dp))
                        .clickable { previewTheme = theme }
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ThemeMiniPreview(theme, state.settings.skin, Modifier.fillMaxWidth().height(86.dp))
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(themeTitle(theme, lang), color = PrimaryText, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Text(themeDescription(theme, lang), color = Muted, fontSize = 11.sp)
                        }
                        Text(if (owned) tx(lang, "owned") else "${theme.priceCoins} 🪙", color = if (owned) Neon else Gold,
                            fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { previewTheme = theme }, modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)) {
                            Text(tx(lang, "theme_preview"), fontSize = 12.sp, color = Neon)
                        }
                        if (owned) {
                            Button(onClick = { onApplyTheme(theme) }, enabled = !applied,
                                modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Neon, contentColor = ButtonText)) {
                                Text(if (applied) tx(lang, "theme_active") else tx(lang, "theme_apply"), fontSize = 12.sp)
                            }
                        } else {
                            Button(onClick = { pendingThemeBuy = theme }, enabled = state.stats.coins >= theme.priceCoins,
                                modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Neon, contentColor = ButtonText)) {
                                Text(tx(lang, "buy"), fontSize = 12.sp)
                            }
                        }
                    }
                    if (!owned && state.stats.coins < theme.priceCoins) {
                        Text(if (lang == "ar") "ناقصك ${theme.priceCoins - state.stats.coins} عملة" else
                            "${theme.priceCoins - state.stats.coins} more coins needed", color = Muted, fontSize = 11.sp)
                    }
                }
            }
            item {
                Text(tx(lang, "theme_free_hint"), color = Muted, fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 12.dp))
            }
        } // themes tab
        else if (tab == "coins") {
            item {
                Text(tx(lang, "coin_shop_title"), color = PrimaryText,
                    fontWeight = FontWeight.Black, fontSize = 18.sp)
                Text(tx(lang, "coin_shop_info"), color = Muted,
                    fontSize = 12.sp, lineHeight = 18.sp)
            }
            if (coinPriceCatalog != null && coinPriceState == CoinPriceCatalog.State.UNAVAILABLE) {
                item {
                    OutlinedButton(onClick = { coinPriceCatalog.refresh() }) {
                        Text(if (lang == "ar") "إعادة محاولة تحميل الأسعار" else "Retry loading prices")
                    }
                }
            }
            items(COIN_PACKAGES, key = { it.productId }) { pack ->
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
                        .background(Panel).border(1.dp, BorderColor, RoundedCornerShape(18.dp))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("🪙", fontSize = 28.sp)
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("${pack.coinAmount} ${if (lang == "ar") "عملة" else "coins"}",
                                color = PrimaryText, fontSize = 19.sp, fontWeight = FontWeight.Black)
                            Text(playPrices[pack.productId] ?: when (coinPriceState) {
                                CoinPriceCatalog.State.LOADING -> if (lang == "ar") "جارٍ تحميل السعر…" else "Loading Google Play price…"
                                CoinPriceCatalog.State.UNAVAILABLE -> if (lang == "ar") "السعر غير متاح" else "Price unavailable"
                                else -> tx(lang, "coin_shop_price_pending")
                            }, color = Muted, fontSize = 12.sp)
                        }
                    }
                    Button(
                        onClick = {}, enabled = false,
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(tx(lang, "coin_shop_unavailable"), fontSize = 12.sp)
                    }
                }
            }
            item {
                Text(tx(lang, "coin_shop_no_payment"),
                    color = Muted, fontSize = 11.sp, modifier = Modifier.padding(bottom = 12.dp))
            }
        } // coins tab
        }
    }

    pendingThemeBuy?.let { theme ->
        AlertDialog(
            onDismissRequest = { pendingThemeBuy = null },
            title = { Text(if (lang == "ar") "تأكيد شراء الثيم" else "Confirm theme purchase", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    ThemeMiniPreview(theme, state.settings.skin, Modifier.fillMaxWidth().height(100.dp))
                    Text(if (lang == "ar") "شراء ثيم ${themeTitle(theme, lang)} مقابل ${theme.priceCoins} عملة؟ هيتفعّل مباشرة ويبقى متاح في الإعدادات." else
                        "Buy ${themeTitle(theme, lang)} for ${theme.priceCoins} coins? It will be applied immediately and unlocked in Settings.")
                }
            },
            confirmButton = {
                TextButton(onClick = { onPurchaseTheme(theme); pendingThemeBuy = null }) {
                    Text(if (lang == "ar") "تأكيد الشراء" else "Buy now", color = Neon, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = { TextButton(onClick = { pendingThemeBuy = null }) { Text(tx(lang, "cancel"), color = Muted) } },
            containerColor = Panel, titleContentColor = PrimaryText, textContentColor = PrimaryText
        )
    }

    pendingBodyBuy?.let { body ->
        AlertDialog(
            onDismissRequest = { pendingBodyBuy = null },
            title = { Text(if (lang == "ar") "تأكيد شراء الجسم" else "Confirm body purchase", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    BodyStylePreview(body, state.settings.skin, state.settings.headStyle,
                        Modifier.fillMaxWidth().height(100.dp))
                    Text(if (lang == "ar") "شراء ${tx(lang, "body_${body.key}")} مقابل ${body.priceCoins} عملة؟"
                        else "Buy ${tx(lang, "body_${body.key}")} for ${body.priceCoins} coins?", color = PrimaryText)
                }
            },
            confirmButton = { TextButton(onClick = { onPurchaseBody(body); pendingBodyBuy = null }) {
                Text(if (lang == "ar") "تأكيد الشراء" else "Buy now", color = Neon)
            } },
            dismissButton = { TextButton(onClick = { pendingBodyBuy = null }) {
                Text(tx(lang, "cancel"), color = Muted)
            } },
            containerColor = Panel, titleContentColor = PrimaryText
        )
    }

    pendingHeadBuy?.let { head ->
        AlertDialog(
            onDismissRequest = { pendingHeadBuy = null },
            title = { Text(if (lang == "ar") "تأكيد شراء الرأس" else "Confirm head purchase", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.fillMaxWidth().height(110.dp).clip(RoundedCornerShape(14.dp)).background(Color(0xFF07131B))) {
                        HeadStylePreview(head, state.settings.skin, Modifier.fillMaxSize(), state.settings.bodyType)
                    }
                    Text(if (lang == "ar") "شراء رأس ${tx(lang, "head_${head.key}")} مقابل ${head.priceCoins} عملة؟"
                        else "Buy ${tx(lang, "head_${head.key}")} for ${head.priceCoins} coins?", color = PrimaryText)
                }
            },
            confirmButton = { TextButton(onClick = { onPurchaseHead(head); pendingHeadBuy = null }) {
                Text(if (lang == "ar") "تأكيد الشراء" else "Buy now", color = Neon)
            } },
            dismissButton = { TextButton(onClick = { pendingHeadBuy = null }) {
                Text(tx(lang, "cancel"), color = Muted)
            } },
            containerColor = Panel, titleContentColor = PrimaryText
        )
    }

    pendingBuy?.let { skin ->
        val price = prices.getValue(skin)
        AlertDialog(
            onDismissRequest = { pendingBuy = null },
            title = { Text(if (lang == "ar") "تأكيد الشراء" else "Confirm purchase", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.fillMaxWidth().height(100.dp).clip(RoundedCornerShape(14.dp)).background(Color(0xFF07131B))) {
                        SkinPreview(skin, Modifier.fillMaxSize(), state.settings.headStyle, state.settings.bodyType)
                    }
                    Text(if (lang == "ar") "شراء شكل ${tx(lang, skin.key)} مقابل $price عملة؟ سيتم تجهيز الشكل مباشرة." else "Buy ${tx(lang, skin.key)} for $price coins? It will be equipped right away.")
                }
            },
            confirmButton = {
                TextButton(onClick = { onPurchase(skin); pendingBuy = null }) {
                    Text(if (lang == "ar") "تأكيد الشراء" else "Buy now", color = Neon, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = { TextButton(onClick = { pendingBuy = null }) { Text(if (lang == "ar") "إلغاء" else "Cancel", color = Muted) } },
            containerColor = Panel,
            titleContentColor = PrimaryText,
            textContentColor = PrimaryText
        )
    }
}

private fun snakeSkinColors(skin: SnakeSkin): List<Color> = when (skin) {
    SnakeSkin.GREEN -> listOf(Color(0xFF00FF88), Color(0xFF00C96B))
    SnakeSkin.PURPLE -> listOf(Color(0xFFD28BFF), Color(0xFF9B55E8))
    SnakeSkin.YELLOW -> listOf(Color(0xFFFFE600), Color(0xFFFFB800))
    SnakeSkin.EGYPT -> listOf(Color(0xFFFFD166), Color(0xFFD92738), Color(0xFFF6E9DC), Color(0xFF17191F))
    SnakeSkin.UAE -> listOf(Color(0xFFCE2B40), Color(0xFF06966D), Color(0xFFFAF5EB), Color(0xFF142B31))
    SnakeSkin.JORDAN -> listOf(Color(0xFFDB2745), Color(0xFF172A31), Color(0xFFFBF2E6), Color(0xFF167E57))
    SnakeSkin.PALESTINE -> listOf(Color(0xFF17282E), Color(0xFF199169), Color(0xFFFCF6E8), Color(0xFFDA3851))
    SnakeSkin.KUWAIT -> listOf(Color(0xFF172528), Color(0xFF2BA46B), Color(0xFFFFF2DD), Color(0xFFCE3C47))
    SnakeSkin.SAUDI -> listOf(Color(0xFF066D45), Color(0xFF0B9861), Color(0xFFEDF8EF), Color(0xFFC5A55A))
    SnakeSkin.CYBER -> listOf(Color(0xFF00E5FF), Color(0xFF7C4DFF))
    SnakeSkin.ICE -> listOf(Color(0xFFB3E5FC), Color(0xFF0288D1))
    SnakeSkin.FIRE -> listOf(Color(0xFFFF7043), Color(0xFFD50000))
}

/** Zoomed-in but real head renderer: accents and proportions match the actual game board. */
@Composable
private fun HeadStylePreview(head: SnakeHeadStyle, skin: SnakeSkin, modifier: Modifier = Modifier,
    bodyType: SnakeBodyType = SnakeBodyType.SEGMENTED) {
    val colors = snakeSkinColors(skin)
    Canvas(modifier) {
        drawRect(Color(0xFF07131B))
        val unit = minOf(size.height * .65f, size.width / 3.45f)
        val y = size.height * .5f
        val headX = size.width * .72f
        if (bodyType != SnakeBodyType.SEGMENTED) {
            drawSnakeBodyByType(bodyType, listOf(Offset(headX, y), Offset(headX - unit * .78f, y),
                Offset(headX - unit * 1.56f, y), Offset(headX - unit * 2.34f, y)), unit, colors)
        } else {
            for (i in 2 downTo 1) {
                val x = headX - unit * i * .78f
                val color = colors[if (colors.size == 2) 1 else (i % (colors.size - 1)) + 1]
                drawRoundRect(color.copy(alpha = .90f),
                    Offset(x - unit * .37f, y - unit * .37f), Size(unit * .74f, unit * .74f),
                    CornerRadius(unit * .22f))
            }
        }
        drawStyledSnakeHead(Offset(headX, y), unit, colors.first(), head, Direction.RIGHT)
    }
}

@Composable
private fun SkinPreview(skin: SnakeSkin, modifier: Modifier = Modifier,
    headStyle: SnakeHeadStyle = SnakeHeadStyle.CLASSIC,
    bodyType: SnakeBodyType = SnakeBodyType.SEGMENTED) {
    val colors = snakeSkinColors(skin)
    Canvas(modifier = modifier) {
        drawRect(Color(0xFF07131B))
        val step = 16.dp.toPx()
        var x = 0f
        while (x < size.width) {
            drawLine(Color(0xFF17303B), Offset(x, 0f), Offset(x, size.height), 0.6f)
            x += step
        }
        var y = 0f
        while (y < size.height) {
            drawLine(Color(0xFF17303B), Offset(0f, y), Offset(size.width, y), 0.6f)
            y += step
        }
        val unit = minOf(size.width / 7.2f, size.height / 3.2f)
        val segment = unit * 0.82f
        val thickness = unit * 0.78f
        val centerY = size.height * 0.53f
        val centers = listOf(
            Offset(size.width * 0.18f, centerY + unit * 0.85f),
            Offset(size.width * 0.18f + unit, centerY + unit * 0.85f),
            Offset(size.width * 0.18f + unit * 2f, centerY + unit * 0.85f),
            Offset(size.width * 0.18f + unit * 2f, centerY - unit * 0.05f),
            Offset(size.width * 0.18f + unit * 3f, centerY - unit * 0.05f),
            Offset(size.width * 0.18f + unit * 4f, centerY - unit * 0.05f)
        )
        if (bodyType != SnakeBodyType.SEGMENTED) {
            drawSnakeBodyByType(bodyType, centers.reversed(), unit, colors)
            drawStyledSnakeHead(centers.last(), unit, colors.first(), headStyle, Direction.RIGHT)
        } else centers.forEachIndexed { index, center ->
            val color = if (index == centers.lastIndex) colors.first() else colors[(index % (colors.size - 1)) + 1]
            drawRoundRect(color.copy(alpha = 0.20f), Offset(center.x - segment * 0.65f, center.y - thickness * 0.65f), Size(segment * 1.3f, thickness * 1.3f), CornerRadius(thickness * 0.38f))
            if (index == centers.lastIndex) {
                drawStyledSnakeHead(center, unit, color, headStyle, Direction.RIGHT)
            } else {
                drawRoundRect(color, Offset(center.x - segment / 2f, center.y - thickness / 2f),
                    Size(segment, thickness), CornerRadius(thickness * 0.32f))
            }
        }
    }
}

@Composable
private fun BodyStylePreview(body: SnakeBodyType, skin: SnakeSkin, headStyle: SnakeHeadStyle,
    modifier: Modifier = Modifier) {
    SkinPreview(skin, modifier, headStyle, body)
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
        modifier = Modifier.fillMaxWidth().clickable { onSelect(mode) }.border(if (selected) 1.5.dp else 1.dp, if (selected) accent else BorderColor, RoundedCornerShape(18.dp)),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = if (selected) SelectedPanel else Panel)
    ) {
        Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(13.dp)) {
            Box(Modifier.size(52.dp).clip(RoundedCornerShape(14.dp)).background(accent.copy(alpha = 0.13f)), contentAlignment = Alignment.Center) {
                androidx.compose.material3.Icon(icon, null, tint = accent, modifier = Modifier.size(29.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(mode.title(language), color = accent, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text(mode.description(language), color = SecondaryText, fontSize = 12.sp, lineHeight = 17.sp)
            }
            if (selected) androidx.compose.material3.Icon(Icons.Rounded.CheckCircle, null, tint = accent, modifier = Modifier.size(24.dp))
        }
    }
}

@Composable
private fun VolumeSetting(label: String, percent: Int, enabled: Boolean, onCommit: (Int) -> Unit) {
    var dragging by remember { mutableStateOf(false) }
    var draft by remember { mutableFloatStateOf(percent.toFloat()) }
    // Save only on release: avoids a burst of DataStore writes while dragging a slider.
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(label, color = PrimaryText, fontSize = 13.sp)
            Text("${if (dragging) draft.roundToInt() else percent}%", color = Muted, fontSize = 12.sp)
        }
        Slider(
            value = if (dragging) draft else percent.toFloat(),
            onValueChange = { dragging = true; draft = it },
            onValueChangeFinished = {
                val newValue = draft.roundToInt().coerceIn(0, 100)
                dragging = false
                if (newValue != percent) onCommit(newValue)
            },
            valueRange = 0f..100f,
            enabled = enabled
        )
    }
}

@Composable
private fun SettingsScreen(
    state: AppUiState,
    onBack: () -> Unit,
    onSettingsChange: (AppSettings) -> Unit,
    onUpdatePlayerName: (String) -> Boolean,
    privacyOptionsRequired: Boolean,
    onPrivacyOptions: () -> Unit,
    playGamesAllowed: Boolean,
    playGamesConfigured: Boolean,
    onRequestPlayGamesSignIn: () -> Unit
) {
    val lang = state.settings.language
    val settings = state.settings
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var appearanceSection by remember { mutableStateOf("snake") }
    var appearanceSnakeTab by remember { mutableStateOf("colors") }
    var editingPlayerName by remember { mutableStateOf(false) }
    var nameDraft by remember { mutableStateOf("") }
    var nameInvalid by remember { mutableStateOf(false) }

    if (editingPlayerName) {
        AlertDialog(
            onDismissRequest = { editingPlayerName = false },
            title = { Text(tx(lang, "player_name")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(
                        value = nameDraft,
                        onValueChange = { nameDraft = it.take(PlayerNamePolicy.MAX_LENGTH + 1); nameInvalid = false },
                        label = { Text(tx(lang, "player_name")) },
                        singleLine = true,
                        isError = nameInvalid
                    )
                    Text(tx(lang, if (nameInvalid) "name_invalid" else "player_name_local_hint"),
                        color = if (nameInvalid) MaterialTheme.colorScheme.error else Muted, fontSize = 12.sp)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (onUpdatePlayerName(nameDraft)) editingPlayerName = false
                    else nameInvalid = true
                }) { Text(tx(lang, "name_save")) }
            },
            dismissButton = { TextButton(onClick = { editingPlayerName = false }) { Text(tx(lang, "cancel")) } }
        )
    }

    // Intercept system Back only while a settings subpage is open. From the root settings page,
    // the existing app-level BackHandler continues to return to the main menu.
    BackHandler(enabled = selectedCategory != null) {
        selectedCategory = null
    }

    if (selectedCategory == null) {
        LazyColumn(
            Modifier.fillMaxSize().background(Bg).padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item { ScreenHeader(tx(lang, "settings"), onBack) }
            item {
                SettingsCategoryCard(
                    title = tx(lang, "gameplay"),
                    description = tx(lang, "gameplay_summary"),
                    icon = Icons.Rounded.Gamepad,
                    accent = Neon,
                    onClick = { selectedCategory = "gameplay" }
                )
            }
            item {
                SettingsCategoryCard(
                    title = tx(lang, "general"),
                    description = tx(lang, "general_summary"),
                    icon = Icons.Rounded.Language,
                    accent = Blue,
                    onClick = { selectedCategory = "general" }
                )
            }
            item {
                SettingsCategoryCard(
                    title = tx(lang, "appearance"),
                    description = tx(lang, "appearance_summary"),
                    icon = Icons.Rounded.Star,
                    accent = Gold,
                    onClick = { selectedCategory = "appearance" }
                )
            }
            item {
                SettingsCategoryCard(
                    title = tx(lang, "account_settings"),
                    description = tx(lang, "account_settings_summary"),
                    icon = Icons.Rounded.Person,
                    accent = Blue,
                    onClick = { selectedCategory = "account" }
                )
            }
            item { Spacer(Modifier.height(8.dp)) }
        }
    } else {
        LazyColumn(
            Modifier.fillMaxSize().background(Bg).padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item { ScreenHeader(tx(lang, selectedCategory!!), onBack = { selectedCategory = null }) }
            when (selectedCategory) {
                "account" -> {
                    item {
                        SettingsPanel {
                            SettingHeading(tx(lang, "player_name"), Icons.Rounded.Person)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(settings.playerName, modifier = Modifier.weight(1f),
                                    color = PrimaryText, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                                OutlinedButton(onClick = {
                                    nameDraft = settings.playerName
                                    nameInvalid = false
                                    editingPlayerName = true
                                }) { Text(tx(lang, "name_edit")) }
                            }
                            Text(tx(lang, "player_name_local_hint"), color = Muted, fontSize = 12.sp)
                        }
                    }
                    if (privacyOptionsRequired) {
                        item {
                            SettingsPanel {
                                SettingHeading(
                                    if (lang == "ar") "خصوصية الإعلانات" else "Ad privacy",
                                    Icons.Rounded.Lock
                                )
                                Text(
                                    if (lang == "ar") "مراجعة خيارات الموافقة على الإعلانات أو تعديلها."
                                    else "Review or change your advertising privacy choices.",
                                    color = Muted, fontSize = 12.sp
                                )
                                OutlinedButton(onClick = onPrivacyOptions) {
                                    Text(if (lang == "ar") "خيارات الخصوصية" else "Privacy options")
                                }
                            }
                        }
                    }
                    item {
                        SettingsPanel {
                            SettingHeading(tx(lang, "play_games"), Icons.Rounded.CloudDone)
                            Text(
                                when {
                                    !playGamesAllowed -> tx(lang, "pgs_child_local")
                                    !playGamesConfigured -> tx(lang, "pgs_config_needed")
                                    else -> cloudStatusLabel(state.cloudStatus, lang)
                                }, color = Muted, fontSize = 12.sp, lineHeight = 17.sp
                            )
                            if (playGamesAllowed && playGamesConfigured &&
                                state.cloudStatus !in listOf("checking", "syncing", "conflict")) {
                                OutlinedButton(onClick = onRequestPlayGamesSignIn) {
                                    Text(tx(lang, if (state.cloudStatus in listOf("synced", "connected", "sync_error"))
                                        "pgs_retry" else "pgs_connect"))
                                }
                            }
                            HorizontalDivider(color = DividerColor)
                            SettingHeading(tx(lang, "cloud_sync_title"), Icons.Rounded.CloudOff)
                            Text(if (playGamesAllowed) cloudStatusLabel(state.cloudStatus, lang)
                                else tx(lang, "pgs_child_local"),
                                color = Muted, fontSize = 12.sp, lineHeight = 17.sp)
                            Text(tx(lang, "cloud_local_name"), color = Muted, fontSize = 11.sp)
                        }
                    }
                }
                "general" -> {
                    item {
                        SettingsPanel {
                            SettingHeading(tx(lang, "language"), Icons.Rounded.Language)
                            Row(
                                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(
                                    DeviceLanguage.SYSTEM to tx(lang, "language_auto"),
                                    DeviceLanguage.ARABIC to tx(lang, "arabic"),
                                    DeviceLanguage.ENGLISH to tx(lang, "english")
                                ).forEach { (mode, label) ->
                                    ChoiceChip(label, settings.languageMode == mode) {
                                        onSettingsChange(settings.copy(languageMode = mode))
                                    }
                                }
                            }
                            Text(tx(lang, "device_language_hint"), color = Muted, fontSize = 12.sp)
                            HorizontalDivider(color = DividerColor)
                            SettingHeading(tx(lang, "sound_haptics"), Icons.Rounded.VolumeUp)
                            SettingSwitch(tx(lang, "sound"), Icons.Rounded.VolumeUp, settings.soundEnabled) {
                                onSettingsChange(settings.copy(soundEnabled = it))
                            }
                            VolumeSetting(tx(lang, "sfx_volume"), settings.sfxVolume, settings.soundEnabled) {
                                onSettingsChange(settings.copy(sfxVolume = it))
                            }
                            HorizontalDivider(color = DividerColor)
                            SettingSwitch(tx(lang, "music"), Icons.Rounded.MusicNote, settings.musicEnabled) {
                                onSettingsChange(settings.copy(musicEnabled = it))
                            }
                            VolumeSetting(tx(lang, "music_volume"), settings.musicVolume, settings.musicEnabled) {
                                onSettingsChange(settings.copy(musicVolume = it))
                            }
                            HorizontalDivider(color = DividerColor)
                            SettingSwitch(tx(lang, "haptics"), Icons.Rounded.Vibration, settings.hapticsEnabled) {
                                onSettingsChange(settings.copy(hapticsEnabled = it))
                            }
                        }
                    }
                }
                "gameplay" -> {
                    item {
                        SettingsPanel {
                            SettingHeading(tx(lang, "difficulty"), Icons.Rounded.Gamepad)
                            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                                Difficulty.entries.forEach { d ->
                                    ChoiceChip(tx(lang, d.key), settings.difficulty == d) {
                                        onSettingsChange(settings.copy(difficulty = d))
                                    }
                                }
                            }
                            HorizontalDivider(color = DividerColor)
                            SettingSwitch(tx(lang, "walls"), Icons.Rounded.Lock, settings.wallsEnabled) {
                                onSettingsChange(settings.copy(wallsEnabled = it))
                            }
                            Text(tx(lang, "walls_desc"), color = Muted, fontSize = 11.sp, lineHeight = 15.sp)
                            HorizontalDivider(color = DividerColor)
                            SettingHeading(tx(lang, "controls"), Icons.Rounded.TouchApp)
                            Text(tx(lang, "control_style"), color = PrimaryText, fontSize = 13.sp)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                ChoiceChip(tx(lang, "floating_joystick"), settings.controlStyle == ControlStyle.FLOATING_JOYSTICK) {
                                    onSettingsChange(settings.copy(controlStyle = ControlStyle.FLOATING_JOYSTICK))
                                }
                                ChoiceChip(tx(lang, "d_pad"), settings.controlStyle == ControlStyle.D_PAD) {
                                    onSettingsChange(settings.copy(controlStyle = ControlStyle.D_PAD))
                                }
                            }
                            if (settings.controlStyle == ControlStyle.FLOATING_JOYSTICK) {
                                Text(tx(lang, "landscape_side"), color = PrimaryText, fontSize = 13.sp)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    ChoiceChip(tx(lang, "right"), !settings.joystickLeftLandscape) {
                                        onSettingsChange(settings.copy(joystickLeftLandscape = false))
                                    }
                                    ChoiceChip(tx(lang, "left"), settings.joystickLeftLandscape) {
                                        onSettingsChange(settings.copy(joystickLeftLandscape = true))
                                    }
                                }
                            }
                        }
                    }
                }
                "appearance" -> {
                    item {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ChoiceChip(tx(lang, "shop_skins"), appearanceSection == "snake") {
                                appearanceSection = "snake"
                            }
                            ChoiceChip(tx(lang, "game_themes"), appearanceSection == "themes") {
                                appearanceSection = "themes"
                            }
                        }
                    }
                    if (appearanceSection == "snake") {
                        item {
                            SettingsPanel {
                                SettingHeading(tx(lang, "snake_preview_title"), Icons.Rounded.Star)
                                Box(Modifier.fillMaxWidth().height(124.dp)
                                    .clip(RoundedCornerShape(13.dp)).background(Color(0xFF07131B))) {
                                    SkinPreview(
                                        skin = settings.skin,
                                        modifier = Modifier.fillMaxSize(),
                                        headStyle = settings.headStyle,
                                        bodyType = settings.bodyType
                                    )
                                }
                                Text(
                                    tx(lang, "snake_preview_hint"),
                                    color = Muted, fontSize = 11.sp, lineHeight = 16.sp
                                )
                            }
                        }
                        item {
                            SettingsPanel {
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    ChoiceChip(tx(lang, "shop_colors"), appearanceSnakeTab == "colors") {
                                        appearanceSnakeTab = "colors"
                                    }
                                    ChoiceChip(tx(lang, "shop_heads"), appearanceSnakeTab == "heads") {
                                        appearanceSnakeTab = "heads"
                                    }
                                    ChoiceChip(tx(lang, "shop_bodies"), appearanceSnakeTab == "body") {
                                        appearanceSnakeTab = "body"
                                    }
                                }
                                Text(tx(lang, "appearance_owned_hint"), color = Muted, fontSize = 11.sp)
                                when (appearanceSnakeTab) {
                                    "colors" -> {
                                        SettingHeading(tx(lang, "skin"), Icons.Rounded.Star)
                                        val owned = SnakeSkin.entries.filter { it.isFree || it.key in settings.ownedSkinKeys }
                                        SettingsOwnedChoiceGrid(owned) { skin, modifier ->
                                            SettingsOwnedChoiceCard(
                                                label = tx(lang, skin.key),
                                                selected = settings.skin == skin,
                                                modifier = modifier,
                                                onClick = { onSettingsChange(settings.copy(skin = skin)) }
                                            ) {
                                                SkinPreview(skin, Modifier.fillMaxSize(), settings.headStyle, settings.bodyType)
                                            }
                                        }
                                    }
                                    "heads" -> {
                                        SettingHeading(tx(lang, "head_title"), Icons.Rounded.Star)
                                        val owned = SnakeHeadStyle.entries.filter { it.isFree || it.key in settings.ownedHeadKeys }
                                        SettingsOwnedChoiceGrid(owned) { head, modifier ->
                                            SettingsOwnedChoiceCard(
                                                label = tx(lang, "head_${head.key}"),
                                                selected = settings.headStyle == head,
                                                modifier = modifier,
                                                onClick = { onSettingsChange(settings.copy(headStyle = head)) }
                                            ) {
                                                HeadStylePreview(head, settings.skin, Modifier.fillMaxSize(), settings.bodyType)
                                            }
                                        }
                                    }
                                    else -> {
                                        SettingHeading(tx(lang, "body_title"), Icons.Rounded.Star)
                                        val owned = SnakeBodyType.entries.filter { it.isFree || it.key in settings.ownedBodyKeys }
                                        // The body category uses the same adaptive grid as colors and heads.
                                        // Future body shapes will be displayed automatically without resizing this UI.
                                        SettingsOwnedChoiceGrid(owned) { body, modifier ->
                                            SettingsOwnedChoiceCard(
                                                label = tx(lang, "body_${body.key}"),
                                                selected = settings.bodyType == body,
                                                modifier = modifier,
                                                onClick = { onSettingsChange(settings.copy(bodyType = body)) }
                                            ) {
                                                BodyStylePreview(body, settings.skin, settings.headStyle, Modifier.fillMaxSize())
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        item {
                            SettingsPanel {
                                SettingHeading(tx(lang, "game_themes"), Icons.Rounded.Spa)
                                Text(tx(lang, "theme_summary"), color = Muted, fontSize = 12.sp, lineHeight = 17.sp)
                                GameTheme.entries.filter { it.isFree || it.key in settings.ownedThemeKeys }.forEach { theme ->
                                    ThemeOptionCard(
                                        theme = theme,
                                        selected = settings.theme == theme,
                                        language = lang,
                                        onClick = { onSettingsChange(settings.copy(theme = theme)) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(8.dp)) }
        }
    }
}

/** A responsive grid for every owned customization category, including future body types. */
@Composable
private fun <T> SettingsOwnedChoiceGrid(
    options: List<T>,
    content: @Composable (T, Modifier) -> Unit
) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val columns = when {
            maxWidth >= 610.dp -> 4
            maxWidth >= 420.dp -> 3
            maxWidth >= 255.dp -> 2
            else -> 1
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            options.chunked(columns).forEach { rowOptions ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    rowOptions.forEach { option -> content(option, Modifier.weight(1f)) }
                    repeat(columns - rowOptions.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
private fun SettingsOwnedChoiceCard(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    preview: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(12.dp)
    Column(
        modifier.clip(shape)
            .background(if (selected) SelectedPanel else Panel)
            .border(if (selected) 2.dp else 1.dp, if (selected) Neon else BorderColor, shape)
            .clickable(onClick = onClick)
            .padding(7.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Box(Modifier.fillMaxWidth().height(78.dp).clip(RoundedCornerShape(9.dp))
            .background(Color(0xFF07131B))) {
            preview()
            if (selected) {
                androidx.compose.material3.Icon(
                    Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    tint = Neon,
                    modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(19.dp)
                )
            }
        }
        Text(
            label,
            color = if (selected) Neon else PrimaryText,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            fontSize = 11.sp,
            maxLines = 2,
            minLines = 2,
            lineHeight = 14.sp,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

private fun themeTitle(theme: GameTheme, language: String) = tx(language, "theme_${theme.key}")
private fun themeDescription(theme: GameTheme, language: String) = tx(language, "theme_${theme.key}_desc")

/** Tiny themed board plus five color swatches; preview does not change the selected theme. */
@Composable
private fun ThemeMiniPreview(theme: GameTheme, skin: SnakeSkin, modifier: Modifier = Modifier) {
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
private fun ThemeOptionCard(
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
            .background(if (selected) p.selectedPanel else p.panel)
            .border(1.dp, if (selected) p.accent else p.border, RoundedCornerShape(14.dp))
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
                Text(tx(language, titleKey), color = p.primaryText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text(tx(language, descriptionKey), color = p.secondaryText, fontSize = 11.sp, lineHeight = 15.sp)
            }
            if (selected) {
                androidx.compose.material3.Icon(Icons.Rounded.CheckCircle, contentDescription = tx(language, "theme_active"), tint = p.accent, modifier = Modifier.size(22.dp))
            } else {
                Text(tx(language, "theme_apply"), color = p.accent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun SettingsCategoryCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accent: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
            .clickable(onClick = onClick)
            .border(1.dp, BorderColor, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Panel)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(15.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(13.dp)
        ) {
            Box(
                Modifier.size(46.dp).clip(RoundedCornerShape(13.dp)).background(accent.copy(alpha = 0.13f)),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.material3.Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(25.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, color = PrimaryText, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Text(description, color = Muted, fontSize = 11.sp, lineHeight = 15.sp)
            }
        }
    }
}

@Composable
private fun StatisticsScreen(state: AppUiState, onBack: () -> Unit, onClaimReward: (String) -> Boolean) {
    val lang = state.settings.language
    var selectedTab by remember(state.statisticsOpenAchievements) {
        mutableStateOf(if (state.statisticsOpenAchievements) 1 else 0)
    }
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
                // LTR outer row fixes the physical placement in both languages:
                // Time Attack left, Classic best score right.
                androidx.compose.runtime.CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatTile(tx(lang, "best_time"), stats.bestTimeScore.toString(), Modifier.weight(1f))
                        StatTile(tx(lang, "best_score"), stats.bestScore.toString(), Modifier.weight(1f))
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
                val unlockedCount = ACHIEVEMENTS.count { it.id in stats.unlockedAchievements || it.progress(stats) >= it.target }
                val claimedCount = ACHIEVEMENTS.count { it.id in stats.claimedAchievementRewards }
                val claimedCoins = ACHIEVEMENTS.sumOf { stats.claimedAchievementRewards[it.id] ?: 0 }
                val availableCoins = ACHIEVEMENTS.filter { (it.id in stats.unlockedAchievements || it.progress(stats) >= it.target) && it.id !in stats.claimedAchievementRewards }
                    .sumOf { it.rewardCoins }
                Card(
                    colors = CardDefaults.cardColors(containerColor = SelectedPanel),
                    shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        Text("$unlockedCount / ${ACHIEVEMENTS.size} ${tx(lang, "achievements")}", color = Neon, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Text("${tx(lang, "achievement_reward_total")}: ${ACHIEVEMENTS.sumOf { it.rewardCoins }} 🪙", color = PrimaryText, fontSize = 12.sp)
                        Text("${tx(lang, "achievement_reward_earned")}: $claimedCoins 🪙 ($claimedCount / ${ACHIEVEMENTS.size})", color = SecondaryText, fontSize = 12.sp)
                        Text("${tx(lang, "achievement_reward_available")}: $availableCoins 🪙", color = if (availableCoins > 0) Gold else Muted, fontSize = 12.sp)
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
            items(ACHIEVEMENTS, key = { it.id }) { achievement ->
                val unlocked = achievement.id in stats.unlockedAchievements || achievement.progress(stats) >= achievement.target
                val claimed = achievement.id in stats.claimedAchievementRewards
                val progress = achievement.progress(stats).coerceAtLeast(0L)
                val shownProgress = if (unlocked) maxOf(progress, achievement.target) else progress
                val ratio = if (achievement.target <= 0) 1f else (shownProgress.toFloat() / achievement.target.toFloat()).coerceIn(0f, 1f)
                val progressText = if (achievement.id == "dedicated" || achievement.id == "play60min") {
                    "${formatPlayTime(shownProgress.coerceAtMost(achievement.target), lang)} / ${formatPlayTime(achievement.target, lang)}"
                } else "${shownProgress.coerceAtMost(achievement.target)} / ${achievement.target}"

                // Compact achievement row: keep manual claiming visible without a full-width button.
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                    shape = RoundedCornerShape(11.dp),
                    colors = CardDefaults.cardColors(containerColor = if (unlocked) SelectedPanel else Panel)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().heightIn(min = 72.dp)
                            .padding(horizontal = 8.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier.size(38.dp).clip(RoundedCornerShape(9.dp))
                                .background(if (unlocked) Gold.copy(alpha = 0.18f) else Bg),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(if (unlocked) achievement.icon else "🔒", fontSize = 19.sp)
                        }
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Text(
                                if (lang == "ar") achievement.arName else achievement.enName,
                                color = if (unlocked) Gold else PrimaryText,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                if (lang == "ar") achievement.arDescription else achievement.enDescription,
                                color = Muted,
                                fontSize = 10.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                LinearProgressIndicator(
                                    progress = { ratio },
                                    modifier = Modifier.weight(1f).height(4.dp).clip(CircleShape),
                                    color = if (unlocked) Gold else Neon,
                                    trackColor = BorderColor
                                )
                                Text(progressText, color = Muted, fontSize = 9.sp, maxLines = 1)
                            }
                        }
                        Column(
                            modifier = Modifier.width(76.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Text("🪙 ${achievement.rewardCoins}", color = Gold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            when {
                                claimed -> Text(tx(lang, "claimed_short"), color = Neon, fontSize = 10.sp, maxLines = 1)
                                unlocked -> Button(
                                    onClick = { onClaimReward(achievement.id) },
                                    modifier = Modifier.fillMaxWidth().height(30.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 3.dp, vertical = 0.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Neon, contentColor = ButtonText)
                                ) {
                                    Text(tx(lang, "claim_short"), fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                }
                                else -> Text(tx(lang, "reward_pending"), color = Muted, fontSize = 10.sp, maxLines = 1)
                            }
                        }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(12.dp)) }
    }
}

@Composable
private fun GameScreen(state: AppUiState, viewModel: GameViewModel, rewardedAds: RewardedAds?, showOverlay: Boolean) {
    val lang = state.settings.language
    val layoutDirection = if (lang == "ar") LayoutDirection.Rtl else LayoutDirection.Ltr
    val engine = viewModel.engine
    val dPadSelected = state.settings.controlStyle == ControlStyle.D_PAD
    BoxWithConstraints(Modifier.fillMaxSize().background(Bg)) {
        val landscape = maxWidth > maxHeight
        if (landscape) {
            // Physical controls stay independent of RTL. Each style gets a full-height touch zone.
            // In landscape the D-Pad is centered in a dedicated side panel to preserve board size.
            val controlsOnLeft = !dPadSelected && state.settings.joystickLeftLandscape
            androidx.compose.runtime.CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Row(Modifier.fillMaxSize().padding(10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (controlsOnLeft) {
                        GameControlArea(Modifier.weight(0.34f).fillMaxHeight(), lang, !showOverlay,
                            state.settings.controlStyle, viewModel::requestDirection, true, landscape = true)
                    }
                    Column(Modifier.weight(0.66f).fillMaxHeight()) {
                        androidx.compose.runtime.CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                            GameScoreBar(state, engine, Modifier.fillMaxWidth().height(48.dp), onPause = viewModel::requestPause)
                        }
                        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            val side = min(maxWidth.value, maxHeight.value).dp
                            GameBoard(engine, state.frameVersion, state.settings.skin, state.settings.headStyle, state.settings.bodyType, state.visualCues, Modifier.size(side))
                        }
                    }
                    if (!controlsOnLeft) {
                        GameControlArea(Modifier.weight(0.34f).fillMaxHeight(), lang, !showOverlay,
                            state.settings.controlStyle, viewModel::requestDirection, false, landscape = true)
                    }
                }
            }
        } else {
            // Reserve the same minimum control height as before, then size the board explicitly.
            // Any unused vertical space belongs to the touch region, not a blank board container.
            val horizontalPadding = 8.dp
            val verticalPadding = 6.dp
            val scoreHeight = 48.dp
            val minimumControlHeight = if (dPadSelected) 178.dp else 132.dp
            val boardSide = min(
                (maxWidth - horizontalPadding * 2).value,
                (maxHeight - verticalPadding * 2 - scoreHeight - minimumControlHeight).value
            ).coerceAtLeast(0f).dp
            Column(Modifier.fillMaxSize().padding(horizontal = horizontalPadding, vertical = verticalPadding)) {
                GameScoreBar(state, engine, Modifier.fillMaxWidth().height(scoreHeight), onPause = viewModel::requestPause)
                Box(Modifier.fillMaxWidth().height(boardSide), contentAlignment = Alignment.Center) {
                    GameBoard(engine, state.frameVersion, state.settings.skin, state.settings.headStyle, state.settings.bodyType, state.visualCues, Modifier.size(boardSide))
                }
                GameControlArea(
                    Modifier.fillMaxWidth().weight(1f), lang, !showOverlay,
                    state.settings.controlStyle, viewModel::requestDirection, false, landscape = false
                )
            }
        }
        val graceSeconds = viewModel.resumeGraceSeconds.value
        if (state.screen == AppScreen.GAME && graceSeconds > 0) {
            Box(Modifier.fillMaxSize().padding(top = 62.dp), contentAlignment = Alignment.TopCenter) {
                Column(Modifier.clip(RoundedCornerShape(14.dp))
                    .background(Panel.copy(alpha = .93f))
                    .border(1.dp, Neon.copy(alpha = .6f), RoundedCornerShape(14.dp))
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("$graceSeconds", color = Neon, fontSize = 23.sp, fontWeight = FontWeight.Black)
                    Text(if (lang == "ar") "حرّك الثعبان للبدء" else "Steer to continue",
                        color = SecondaryText, fontSize = 10.sp)
                }
            }
        }
        if (engine.shieldRemainingMs > 0L && state.screen == AppScreen.GAME) {
            Box(Modifier.fillMaxSize().padding(top = 54.dp), contentAlignment = Alignment.TopCenter) {
                Text("🛡 ${(engine.shieldRemainingMs + 999L) / 1000L}s", color = Neon,
                    fontWeight = FontWeight.Bold, fontSize = 16.sp,
                    modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(Panel).padding(horizontal = 12.dp, vertical = 5.dp))
            }
        }
        if (showOverlay) {
            Box(Modifier.fillMaxSize().background(LocalZaPalette.current.overlay), contentAlignment = Alignment.Center) {
                if (state.screen == AppScreen.PAUSE) PauseCard(state, viewModel)
                else GameOverCard(state, viewModel, rewardedAds)
            }
        }
    }
}

@Composable
private fun GameControlArea(
    modifier: Modifier,
    language: String,
    enabled: Boolean,
    style: ControlStyle,
    onDirection: (Direction) -> Unit,
    leftSidePreference: Boolean,
    landscape: Boolean
) {
    if (style == ControlStyle.D_PAD) {
        // Physical direction buttons must not be reversed by Arabic RTL layout.
        // Only the D-Pad is forced LTR; all other Arabic screens remain RTL.
        androidx.compose.runtime.CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            DPadArea(modifier, enabled, onDirection, landscape)
        }
    } else {
        JoystickArea(modifier, language, enabled, onDirection, leftSidePreference)
    }
}

@Composable
private fun DPadArea(modifier: Modifier, enabled: Boolean, onDirection: (Direction) -> Unit, landscape: Boolean) {
    val palette = LocalZaPalette.current
    val buttonSize = if (landscape) 44.dp else 52.dp
    val gap = if (landscape) 2.dp else 4.dp
    Box(
        modifier.clip(RoundedCornerShape(16.dp))
            .background(Brush.radialGradient(listOf(palette.joystickGlow, palette.joystickGlowClear))),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(gap)) {
            DPadButton(Direction.UP, buttonSize, enabled, onDirection)
            Row(horizontalArrangement = Arrangement.spacedBy(gap), verticalAlignment = Alignment.CenterVertically) {
                DPadButton(Direction.LEFT, buttonSize, enabled, onDirection)
                Box(Modifier.size(buttonSize).clip(RoundedCornerShape(10.dp)).background(palette.joystickInner))
                DPadButton(Direction.RIGHT, buttonSize, enabled, onDirection)
            }
            DPadButton(Direction.DOWN, buttonSize, enabled, onDirection)
        }
    }
}

@Composable
private fun DPadButton(direction: Direction, size: Dp, enabled: Boolean, onDirection: (Direction) -> Unit) {
    var pressed by remember(direction) { mutableStateOf(false) }
    val palette = LocalZaPalette.current
    val icon = when (direction) {
        Direction.UP -> Icons.Rounded.KeyboardArrowUp
        Direction.DOWN -> Icons.Rounded.KeyboardArrowDown
        Direction.LEFT -> Icons.Rounded.KeyboardArrowLeft
        Direction.RIGHT -> Icons.Rounded.KeyboardArrowRight
    }
    Box(
        Modifier.size(size)
            .clip(RoundedCornerShape(12.dp))
            .background(if (pressed && enabled) palette.joystickKnob else palette.joystickInner)
            .border(1.dp, if (pressed && enabled) palette.joystickHighlight else palette.joystickRing, RoundedCornerShape(12.dp))
            .pointerInput(enabled, direction) {
                if (enabled) detectTapGestures(onPress = {
                    pressed = true
                    try {
                        onDirection(direction) // Direction change on DOWN, not after lifting a finger.
                        tryAwaitRelease()
                    } finally {
                        pressed = false
                    }
                })
            },
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.material3.Icon(icon, contentDescription = direction.name,
            tint = if (pressed && enabled) palette.bg else palette.joystickKnob,
            modifier = Modifier.size(size * 0.68f))
    }
}

@Composable
private fun GameScoreBar(state: AppUiState, engine: com.ahmedzayed.zasnake.game.SnakeEngine, modifier: Modifier, onPause: () -> Unit) {
    val lang = state.settings.language
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        ScoreItem(tx(lang, "score"), engine.score.toString(), Modifier.weight(1f), highlightOnChange = true)
        ScoreItem(tx(lang, "best_score"), state.stats.bestScore.toString(), Modifier.weight(1f))
        ScoreItem(if (engine.mode == GameMode.TIME_ATTACK) tx(lang, "time") else tx(lang, "level"),
            if (engine.mode == GameMode.TIME_ATTACK) ((engine.timeRemainingMs + 999) / 1000).toString() else engine.level.toString(), Modifier.weight(1f))
        IconButtonFrame(onClick = onPause, modifier = Modifier.size(42.dp)) {
            androidx.compose.material3.Icon(Icons.Rounded.Pause, null, tint = Neon)
        }
    }
}

@Composable
private fun ScoreItem(label: String, value: String, modifier: Modifier, highlightOnChange: Boolean = false) {
    val pulse = remember { Animatable(0f) }
    LaunchedEffect(value, highlightOnChange) {
        if (highlightOnChange && value != "0") {
            pulse.snapTo(1f)
            pulse.animateTo(0f, tween(durationMillis = 280))
        }
    }
    Column(
        modifier.fillMaxHeight().clip(RoundedCornerShape(8.dp)).background(Panel)
            .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
            .padding(horizontal = 3.dp, vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(label, color = LocalZaPalette.current.scoreLabel, fontSize = 9.sp,
            lineHeight = 11.sp, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
        Text(value, color = Neon, fontSize = 16.sp, lineHeight = 18.sp,
            modifier = Modifier.graphicsLayer {
                scaleX = 1f + .10f * pulse.value
                scaleY = 1f + .10f * pulse.value
            },
            fontWeight = FontWeight.Bold, maxLines = 1, textAlign = TextAlign.Center)
    }
}

@Composable
private fun GameBoard(engine: com.ahmedzayed.zasnake.game.SnakeEngine, frameVersion: Long, skin: SnakeSkin, headStyle: SnakeHeadStyle, bodyType: SnakeBodyType, cues: List<VisualCue>, modifier: Modifier) {
    val palette = LocalZaPalette.current
    val motionEnabled = LocalVisualMotionEnabled.current
    Canvas(modifier = modifier.clip(RoundedCornerShape(4.dp)).border(1.dp, palette.boardBorder, RoundedCornerShape(4.dp))) {
        // Capturing the frame marker invalidates the draw closure while the ViewModel ticks.
        if (frameVersion == Long.MIN_VALUE) return@Canvas
        drawRect(palette.boardBackground)
        val cell = size.width / com.ahmedzayed.zasnake.game.SnakeEngine.GRID_SIZE
        for (i in 0..com.ahmedzayed.zasnake.game.SnakeEngine.GRID_SIZE) {
            val p = i * cell
            drawLine(palette.boardGrid, Offset(p, 0f), Offset(p, size.height), 0.5f)
            drawLine(palette.boardGrid, Offset(0f, p), Offset(size.width, p), 0.5f)
        }
        val borderColor = if (engine.isWallsEffective) palette.wallBorder else palette.openBorder
        drawRect(borderColor, style = Stroke(width = 2f))

        // Regular red apple.
        val foodX = (engine.food.x + 0.5f) * cell
        val foodY = (engine.food.y + 0.5f) * cell
        drawCircle(Color(0xAAFF3366), cell * 0.42f, Offset(foodX, foodY))
        drawCircle(Color(0xFFFF3366), cell * 0.30f, Offset(foodX, foodY + cell * 0.03f))
        drawLine(palette.accent, Offset(foodX, foodY - cell * 0.23f), Offset(foodX + cell * 0.11f, foodY - cell * 0.39f), cell * 0.07f, cap = StrokeCap.Round)

        engine.specials.forEach { special ->
            val cx = (special.cell.x + 0.5f) * cell
            val cy = (special.cell.y + 0.5f) * cell
            if (special.type == SpecialType.GOLDEN) {
                drawCircle(palette.gold.copy(alpha = 0.30f), cell * 0.48f, Offset(cx, cy))
                drawCircle(palette.gold, cell * 0.31f, Offset(cx, cy))
                drawCircle(Color(0xFFFFF1A8), cell * 0.10f, Offset(cx - cell * .08f, cy - cell * .09f))
            } else {
                drawLine(Color(0xFFBDE8FF), Offset(cx - cell * .22f, cy - cell * .2f), Offset(cx + cell * .2f, cy + cell * .2f), cell * .10f, cap = StrokeCap.Round)
                drawLine(Color(0xFFFFC5D3), Offset(cx + cell * .22f, cy - cell * .2f), Offset(cx - cell * .2f, cy + cell * .2f), cell * .10f, cap = StrokeCap.Round)
            }
        }

        val colors = snakeSkinColors(skin)
        val segments = engine.interpolatedSnake()
        if (bodyType != SnakeBodyType.SEGMENTED) {
            // Small dark silhouette under each style to keep light skins visible on light themes.
            val centers = segments.map { (gx, gy) -> Offset((gx + .5f) * cell, (gy + .5f) * cell) }
            val outline = Color(0xFF0D1820).copy(alpha = .80f)
            drawSnakeBodyByType(bodyType, centers, cell * 1.075f, listOf(outline))
            drawSnakeBodyByType(bodyType, centers, cell, colors)
            centers.firstOrNull()?.let { center ->
                drawStyledSnakeHead(center, cell * 1.075f, outline, headStyle, engine.direction, silhouetteOnly = true)
                drawStyledSnakeHead(center, cell, colors.first(), headStyle, engine.direction)
            }
        } else {
            for (i in segments.indices.reversed()) {
                val (gx, gy) = segments[i]
                val inset = cell * 0.10f
                val color = if (i == 0 || colors.size == 2) colors[if (i == 0) 0 else 1] else colors[(i % (colors.size - 1)) + 1]
                val left = gx * cell + inset
                val top = gy * cell + inset
                if (i != 0) {
                    val borderInset = cell * .065f
                    drawRoundRect(Color(0xFF0D1820).copy(alpha = .85f),
                        Offset(gx * cell + borderInset, gy * cell + borderInset),
                        Size(cell - borderInset * 2, cell - borderInset * 2), CornerRadius(cell * .27f))
                    drawRoundRect(color.copy(alpha = 0.88f), Offset(left, top),
                        Size(cell - inset * 2, cell - inset * 2), CornerRadius(cell * 0.25f))
                } else {
                    val center = Offset(gx * cell + cell / 2f, gy * cell + cell / 2f)
                    drawStyledSnakeHead(center, cell * 1.075f, Color(0xFF0D1820), headStyle,
                        engine.direction, silhouetteOnly = true)
                    drawStyledSnakeHead(center, cell, color, headStyle, engine.direction)
                }
            }
        }

        // Event-driven neon/arcade highlights. Bounded to four transient cues and
        // rendered on the existing game frame; no additional animation timer or physics.
        if (motionEnabled) {
            val now = SystemClock.elapsedRealtime()
            cues.forEach { cue ->
                val duration = when (cue.kind) {
                    EngineEvent.Type.GOLDEN -> 480f
                    EngineEvent.Type.SCISSORS -> 350f
                    else -> 270f
                }
                val elapsed = (now - cue.startedAtElapsedMs).toFloat()
                if (elapsed >= 0f && elapsed < duration) {
                    val progress = (elapsed / duration).coerceIn(0f, 1f)
                    val fade = (1f - progress) * (1f - progress)
                    val center = Offset((cue.cell.x + 0.5f) * cell, (cue.cell.y + 0.5f) * cell)
                    when (cue.kind) {
                        EngineEvent.Type.FOOD -> {
                            drawCircle(Color(0xFFFF6D90).copy(alpha = fade * .8f),
                                radius = cell * (.22f + progress * .85f), center = center,
                                style = Stroke(width = cell * .10f))
                        }
                        EngineEvent.Type.GOLDEN -> {
                            drawCircle(palette.gold.copy(alpha = fade * .85f),
                                radius = cell * (.38f + progress * 1.5f), center = center,
                                style = Stroke(width = cell * .13f))
                            for (k in 0 until 6) {
                                val angle = k * (kotlin.math.PI.toFloat() / 3f)
                                val inner = cell * (.50f + progress * .9f)
                                val outer = inner + cell * .20f
                                val unit = Offset(kotlin.math.cos(angle), kotlin.math.sin(angle))
                                drawLine(palette.gold.copy(alpha = fade),
                                    center + unit * inner, center + unit * outer,
                                    strokeWidth = cell * .08f, cap = StrokeCap.Round)
                            }
                        }
                        EngineEvent.Type.SCISSORS -> {
                            val span = cell * (.25f + progress * .75f)
                            val alphaColor = palette.blue.copy(alpha = fade)
                            drawLine(alphaColor, center + Offset(-span, -span), center + Offset(span, span),
                                strokeWidth = cell * .09f, cap = StrokeCap.Round)
                            drawLine(alphaColor, center + Offset(-span, span), center + Offset(span, -span),
                                strokeWidth = cell * .09f, cap = StrokeCap.Round)
                        }
                        EngineEvent.Type.GAME_OVER -> Unit
                    }
                }
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
    val palette = LocalZaPalette.current
    val radiusPx = with(density) { 56.dp.toPx() }
    Box(modifier.clip(RoundedCornerShape(16.dp)).background(Brush.radialGradient(listOf(palette.joystickGlow, palette.joystickGlowClear))).pointerInput(enabled, leftSidePreference) {
        if (!enabled) return@pointerInput
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            down.consume()
            // The ring must remain completely within the available control region.
            val marginX = min(radiusPx, size.width / 2f)
            val marginY = min(radiusPx, size.height / 2f)
            anchor = Offset(
                down.position.x.coerceIn(marginX, size.width - marginX),
                down.position.y.coerceIn(marginY, size.height - marginY)
            )
            knob = anchor
            active = true
            val pointerId = down.id
            var keepTracking = true
            while (keepTracking) {
                val event = awaitPointerEvent()
                val change = event.changes.firstOrNull { it.id == pointerId } ?: break
                val p = change.position
                knob = Offset(p.x.coerceIn(0f, size.width.toFloat()), p.y.coerceIn(0f, size.height.toFloat()))
                val dx = p.x - down.position.x
                val dy = p.y - down.position.y
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
            // Overlay only: the floating joystick never affects parent measurement.
            Canvas(Modifier.matchParentSize()) {
                val r = min(radiusPx, min(size.width, size.height) * 0.30f)
                drawCircle(palette.joystickRing, r, anchor, style = Stroke(width = 3f))
                drawCircle(palette.joystickInner, r, anchor)
                val offset = knob - anchor
                val maxTravel = r * 0.56f
                val length = kotlin.math.sqrt(offset.x * offset.x + offset.y * offset.y)
                val limited = if (length > maxTravel && length > 0f) offset * (maxTravel / length) else offset
                drawCircle(palette.joystickKnob, r * 0.38f, anchor + limited)
                drawCircle(palette.joystickHighlight, r * 0.20f, anchor + limited + Offset(-r * .07f, -r * .08f))
            }
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                androidx.compose.material3.Icon(Icons.Rounded.TouchApp, null, tint = palette.joystickHint, modifier = Modifier.size(25.dp))
                Text(tx(language, "tap_to_steer"), color = palette.muted, fontSize = 10.sp, modifier = Modifier.padding(top = 5.dp))
            }
        }
    }
}

/** Only called after an idempotent claim has returned success. Purely presentational. */
@Composable
private fun RewardPopOverlay(pop: RewardPop, balance: Long, language: String, onFinished: (Long) -> Unit, modifier: Modifier = Modifier) {
    val motionEnabled = LocalVisualMotionEnabled.current
    val progress = remember(pop.id) { Animatable(0f) }
    val offsetPx = with(LocalDensity.current) { 45.dp.toPx() }
    LaunchedEffect(pop.id, motionEnabled) {
        if (motionEnabled) progress.animateTo(1f, tween(850)) else delay(650L)
        onFinished(pop.id)
    }
    val t = progress.value
    val alpha = if (!motionEnabled) 1f else when {
        t < 0.12f -> (t / 0.12f).coerceIn(0f, 1f)
        t > 0.76f -> ((1f - t) / 0.24f).coerceIn(0f, 1f)
        else -> 1f
    }
    Box(modifier.fillMaxWidth().padding(top = 58.dp), contentAlignment = Alignment.TopCenter) {
        Box(Modifier.graphicsLayer {
            this.alpha = alpha
            translationY = -offsetPx * t
        }.clip(RoundedCornerShape(25.dp))
            .background(SelectedPanel).border(1.dp, Gold, RoundedCornerShape(25.dp))
            .padding(horizontal = 18.dp, vertical = 9.dp)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("+${pop.coins} 🪙", color = Gold, fontWeight = FontWeight.Black, fontSize = 17.sp)
                Text("${tx(language, "coin_balance")}: $balance", color = PrimaryText, fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun PauseCard(state: AppUiState, viewModel: GameViewModel) {
    val lang = state.settings.language
    Column(Modifier.fillMaxWidth(0.86f).widthIn(max = 360.dp).clip(RoundedCornerShape(22.dp)).background(Panel).border(1.dp, BorderColor, RoundedCornerShape(22.dp)).padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(11.dp)) {
        androidx.compose.material3.Icon(Icons.Rounded.Pause, null, tint = Neon, modifier = Modifier.size(42.dp))
        Text(tx(lang, "pause_title"), color = Neon, fontSize = 23.sp, fontWeight = FontWeight.Bold)
        val detailLabel = if (viewModel.engine.mode == GameMode.TIME_ATTACK) tx(lang, "time") else tx(lang, "level")
        val detailValue = if (viewModel.engine.mode == GameMode.TIME_ATTACK) "${(viewModel.engine.timeRemainingMs + 999L) / 1000L}s" else viewModel.engine.level.toString()
        Text("${tx(lang, "score")}: ${viewModel.engine.score}  •  $detailLabel: $detailValue", color = Muted, fontSize = 13.sp)
        PrimaryActionButton(tx(lang, "continue"), Icons.Rounded.PlayArrow, viewModel::resumeFromPause)
        OutlinedActionButton(tx(lang, "restart"), Icons.Rounded.Replay, viewModel::requestRestart)
        OutlinedActionButton(tx(lang, "main_menu"), Icons.Rounded.Home, viewModel::goHomeKeepingRound)
        Text(tx(lang, "paused_save"), color = Muted, fontSize = 11.sp)
    }
}

@Composable
private fun GameOverCard(state: AppUiState, viewModel: GameViewModel, rewardedAds: RewardedAds?) {
    val lang = state.settings.language
    val title = if (viewModel.engine.gameOverReason == "time") tx(lang, "time_up") else tx(lang, "game_over")
    val engine = viewModel.engine
    val motionEnabled = LocalVisualMotionEnabled.current
    val finalScore = viewModel.engine.score
    val scoreCount = remember(viewModel.engine.sessionId) { Animatable(0f) }
    LaunchedEffect(viewModel.engine.sessionId, finalScore, motionEnabled) {
        if (motionEnabled) scoreCount.animateTo(finalScore.toFloat(), tween(620))
        else scoreCount.snapTo(finalScore.toFloat())
    }
    BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
    Column(Modifier.fillMaxWidth(0.90f).widthIn(max = 370.dp)
        .heightIn(max = (maxHeight - 12.dp).coerceAtLeast(1.dp))
        .clip(RoundedCornerShape(22.dp)).background(Panel)
        .border(1.dp, BorderColor, RoundedCornerShape(22.dp))
        .verticalScroll(rememberScrollState()).padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        androidx.compose.material3.Icon(Icons.Rounded.EmojiEvents, null, tint = Gold, modifier = Modifier.size(44.dp))
        Text(title, color = PrimaryText, fontSize = 23.sp, fontWeight = FontWeight.Bold)
        if (state.newRecordThisRound) {
            Text("✦ ${tx(lang, "new_record_visual")} ✦", color = Gold,
                fontSize = 14.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        }
        Text(tx(lang, "your_score"), color = Muted, fontSize = 13.sp)
        Text(scoreCount.value.roundToInt().toString(), color = Neon,
            fontSize = 42.sp, fontWeight = FontWeight.Black)
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(LocalZaPalette.current.statsTile).padding(13.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(tx(lang, "best_score"), color = SecondaryText, fontSize = 12.sp)
            Text(state.stats.bestScore.toString(), color = Gold, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }
        if (engine.mode == GameMode.CLASSIC) {
            val earned = if (engine.pendingCollision) engine.score else state.stats.coinRewards[engine.sessionId] ?: 0
            val bonus = state.stats.doubledRoundCoins[engine.sessionId] ?: 0
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(LocalZaPalette.current.statsTile).padding(13.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(if (engine.pendingCollision) {
                    if (lang == "ar") "🪙 عملات عند الإنهاء" else "🪙 Coins if you end"
                } else "🪙  ${tx(lang, "coins_earned")}", color = SecondaryText, fontSize = 12.sp)
                Text("+${earned + bonus}", color = Gold, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        }
        if (engine.canRevive) {
            Text(if (lang == "ar") "فرصة واحدة للإكمال مع حماية من الاصطدام لمدة 5 ثوانٍ"
                else "One revive: 5 seconds of collision protection", color = Neon,
                fontSize = 12.sp, textAlign = TextAlign.Center)
            PrimaryActionButton(if (rewardedAds == null) {
                if (lang == "ar") "▶ إكمال مجاني" else "▶ Free revive"
            } else if (lang == "ar") "▶ شاهد إعلانًا للإكمال" else "▶ Watch ad to revive",
                Icons.Rounded.PlayArrow, onClick = {
                    val session = engine.sessionId
                    if (rewardedAds == null) viewModel.reviveAfterReward(session)
                    else rewardedAds.show(RewardPlacement.REVIVE, onEarned = { viewModel.reviveAfterReward(session) },
                        onUnavailable = viewModel::rewardAdUnavailable)
                })
            OutlinedActionButton(if (lang == "ar") "إنهاء الجولة واستلام العملات" else "End round and collect coins",
                Icons.Rounded.CheckCircle, viewModel::declineRevive)
        } else if (engine.mode == GameMode.CLASSIC &&
            (state.stats.coinRewards[engine.sessionId] ?: 0) > 0) {
            if (engine.sessionId !in state.stats.doubledRoundCoins) {
                PrimaryActionButton(if (rewardedAds == null) {
                    if (lang == "ar") "🪙 مضاعفة مجانية ×2" else "🪙 Free double coins ×2"
                } else if (lang == "ar") "🎬 إعلان لمضاعفة العملات ×2" else "🎬 Watch ad: double coins ×2",
                    Icons.Rounded.Star, onClick = {
                        val session = engine.sessionId
                        if (rewardedAds == null) viewModel.doubleCoinsAfterReward(session)
                        else rewardedAds.show(RewardPlacement.DOUBLE_COINS, onEarned = { viewModel.doubleCoinsAfterReward(session) },
                            onUnavailable = viewModel::rewardAdUnavailable)
                    })
            } else {
                Text(if (lang == "ar") "✓ تمت مضاعفة عملات الجولة" else "✓ Round coins doubled",
                    color = Neon, fontSize = 12.sp)
            }
        }
        val missingAd = (engine.canRevive && rewardedAds?.readyFor(RewardPlacement.REVIVE) == false) ||
            (engine.mode == GameMode.CLASSIC && !engine.pendingCollision &&
                engine.sessionId !in state.stats.doubledRoundCoins &&
                rewardedAds?.readyFor(RewardPlacement.DOUBLE_COINS) == false)
        if (missingAd) {
            Text(if (lang == "ar") "جاري تجهيز الإعلان التجريبي، وقد لا يتوفر دون إنترنت"
                else "Loading test ad; internet connection required", color = Muted, fontSize = 10.sp,
                textAlign = TextAlign.Center)
        }
        PrimaryActionButton(tx(lang, "restart"), Icons.Rounded.Replay, viewModel::replayAfterGameOver)
        OutlinedActionButton(tx(lang, "main_menu"), Icons.Rounded.Home, viewModel::goHomeAfterGameOver)
    }
    }
}

@Composable
private fun CloudStatusLine(state: AppUiState, language: String) {
    val configuredConnection = state.cloudStatus in listOf("connected", "sync_error", "synced")
    Row(Modifier.fillMaxWidth().padding(top = 1.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        androidx.compose.material3.Icon(if (configuredConnection) Icons.Rounded.CloudDone else Icons.Rounded.CloudOff, null, tint = if (configuredConnection) Neon else Muted, modifier = Modifier.size(15.dp))
        Text(if (state.cloudStatus == "not_configured") tx(language, "sync_pending") else cloudStatusLabel(state.cloudStatus, language),
            color = Muted, fontSize = 10.sp, modifier = Modifier.padding(start = 5.dp), textAlign = TextAlign.Center)
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
private fun SettingsPanel(content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(15.dp)).background(Panel2).border(1.dp, BorderColor, RoundedCornerShape(15.dp)).padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
}

@Composable
private fun SettingHeading(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        androidx.compose.material3.Icon(icon, null, tint = Neon, modifier = Modifier.size(19.dp))
        Text(label, color = PrimaryText, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
    }
}

@Composable
private fun SettingSwitch(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, checked: Boolean, onChange: (Boolean) -> Unit) {
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
private fun ChoiceChip(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    FilterChip(selected = selected, onClick = onClick, label = { Text(label, fontSize = 12.sp, maxLines = 1) }, modifier = modifier,
        colors = androidx.compose.material3.FilterChipDefaults.filterChipColors(
            selectedContainerColor = Neon,
            selectedLabelColor = ButtonText,
            containerColor = ChipBackground,
            labelColor = SecondaryText
        ))
}

@Composable
private fun SkinChip(skin: SnakeSkin, selected: Boolean, language: String, onClick: () -> Unit) {
    val color = when (skin) {
        SnakeSkin.GREEN -> Neon; SnakeSkin.PURPLE -> Color(0xFFD28BFF); SnakeSkin.YELLOW -> Color(0xFFFFE600)
        SnakeSkin.EGYPT -> Color(0xFFCE1126); SnakeSkin.UAE -> Color(0xFF06966D)
        SnakeSkin.JORDAN -> Color(0xFFDB2745); SnakeSkin.PALESTINE -> Color(0xFF199169)
        SnakeSkin.KUWAIT -> Color(0xFF172528); SnakeSkin.SAUDI -> Color(0xFF066D45)
        SnakeSkin.CYBER -> Color(0xFF00E5FF); SnakeSkin.ICE -> Color(0xFF9EDFFF); SnakeSkin.FIRE -> Color(0xFFFF623E)
    }
    Column(Modifier.clickable { onClick() }.padding(2.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Box(Modifier.size(31.dp).clip(CircleShape).background(color).border(if (selected) 3.dp else 1.dp, if (selected) Color.White else Color(0xFF53665A), CircleShape))
        Text(tx(language, skin.key), color = if (selected) Neon else Muted, fontSize = 9.sp, textAlign = TextAlign.Center)
    }
}

@Composable
private fun StatTile(label: String, value: String, modifier: Modifier) {
    Column(modifier.clip(RoundedCornerShape(13.dp)).background(Panel).border(1.dp, BorderColor, RoundedCornerShape(13.dp)).padding(vertical = 14.dp, horizontal = 9.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(value, color = Neon, fontWeight = FontWeight.Black, fontSize = 21.sp, textAlign = TextAlign.Center)
        Text(label, color = Muted, fontSize = 10.sp, textAlign = TextAlign.Center, lineHeight = 13.sp)
    }
}

@Composable
private fun buttonPressScale(interaction: MutableInteractionSource): Float {
    val pressed by interaction.collectIsPressedAsState()
    val target = if (LocalVisualMotionEnabled.current && pressed) 0.97f else 1f
    val scale by animateFloatAsState(target, tween(95), label = "PressScale")
    return scale
}

@Composable
private fun PrimaryActionButton(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val scale = buttonPressScale(interaction)
    Button(onClick = onClick,
        interactionSource = interaction,
        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).graphicsLayer { scaleX = scale; scaleY = scale },
        shape = RoundedCornerShape(13.dp), colors = ButtonDefaults.buttonColors(containerColor = Neon, contentColor = ButtonText)) {
        androidx.compose.material3.Icon(icon, null, modifier = Modifier.size(21.dp))
        Text(label, fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 8.dp))
    }
}

@Composable
private fun OutlinedActionButton(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val scale = buttonPressScale(interaction)
    OutlinedButton(onClick = onClick,
        interactionSource = interaction,
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).graphicsLayer { scaleX = scale; scaleY = scale },
        shape = RoundedCornerShape(13.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryText)) {
        androidx.compose.material3.Icon(icon, null, modifier = Modifier.size(19.dp))
        Text(label, fontSize = 13.sp, modifier = Modifier.padding(start = 8.dp))
    }
}

@Composable
private fun SecondaryTile(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier, onClick: () -> Unit) {
    Column(modifier.clip(RoundedCornerShape(13.dp)).background(Panel).border(1.dp, BorderColor, RoundedCornerShape(13.dp)).clickable { onClick() }.padding(horizontal = 7.dp, vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(7.dp)) {
        androidx.compose.material3.Icon(icon, null, tint = if (icon == Icons.Rounded.EmojiEvents) Gold else Neon, modifier = Modifier.size(24.dp))
        Text(label, color = PrimaryText, fontSize = 11.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center, lineHeight = 14.sp)
    }
}

@Composable
private fun IconButtonFrame(onClick: () -> Unit, modifier: Modifier = Modifier.size(44.dp), content: @Composable () -> Unit) {
    Box(modifier.clip(RoundedCornerShape(12.dp)).background(Panel).border(1.dp, BorderColor, RoundedCornerShape(12.dp)).clickable { onClick() }, contentAlignment = Alignment.Center) { content() }
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
