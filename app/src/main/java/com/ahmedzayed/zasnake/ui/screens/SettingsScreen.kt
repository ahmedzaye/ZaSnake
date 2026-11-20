package com.ahmedzayed.zasnake.ui

import androidx.activity.compose.BackHandler
import com.ahmedzayed.zasnake.BuildConfig
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Gamepad
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import com.ahmedzayed.zasnake.language.DeviceLanguage
import com.ahmedzayed.zasnake.game.AppSettings
import com.ahmedzayed.zasnake.game.PlayerNamePolicy
import com.ahmedzayed.zasnake.game.AppUiState
import com.ahmedzayed.zasnake.game.ControlStyle
import com.ahmedzayed.zasnake.game.Difficulty
import com.ahmedzayed.zasnake.game.GameTheme
import com.ahmedzayed.zasnake.game.SnakeSkin
import com.ahmedzayed.zasnake.game.SnakeHeadStyle
import com.ahmedzayed.zasnake.game.SnakeBodyType

@Composable
internal fun VolumeSetting(label: String, percent: Int, enabled: Boolean, onCommit: (Int) -> Unit) {
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
internal fun SettingsScreen(
    state: AppUiState,
    onBack: () -> Unit,
    onSettingsChange: (AppSettings) -> Unit,
    onUpdatePlayerName: (String) -> Boolean,
    onEditAge: () -> Unit,
    onChangePlayGamesAccount: () -> Unit,
    privacyOptionsRequired: Boolean,
    onPrivacyOptions: () -> Unit,
    playGamesAllowed: Boolean,
    playGamesConfigured: Boolean,
    onRequestPlayGamesSignIn: () -> Unit,
    onTestGameServerAuth: (((Boolean) -> Unit) -> Unit)?
) {
    val lang = state.settings.language
    val settings = state.settings
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var appearanceSection by remember { mutableStateOf("snake") }
    var appearanceSnakeTab by remember { mutableStateOf("colors") }
    var editingPlayerName by remember { mutableStateOf(false) }
    var nameDraft by remember { mutableStateOf("") }
    var nameInvalid by remember { mutableStateOf(false) }
    // Debug-only result: never keep a server auth code in Compose state.
    var serverAuthTestRunning by remember { mutableStateOf(false) }
    var serverAuthTestResult by remember { mutableStateOf<Boolean?>(null) }

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

    if (selectedCategory == "account_stats") {
        StatisticsScreen(state, onBack = { selectedCategory = null },
            onClaimReward = { false }, achievementsOnly = false)
    } else if (selectedCategory == null) {
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
            item {
                SettingsCategoryCard(
                    title = tx(lang, "statistics"),
                    description = if (lang == "ar") "نتائجك ومبارياتك ووقت اللعب"
                    else "Scores, games and total playtime",
                    icon = Icons.Rounded.BarChart,
                    accent = Playform.Lavender,
                    onClick = { selectedCategory = "account_stats" }
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
                    item {
                        SettingsPanel {
                            SettingHeading(
                                if (lang == "ar") "بياناتي الشخصية والعمر" else "Personal details and age",
                                Icons.Rounded.Person
                            )
                            Text(
                                if (lang == "ar")
                                    "تعديل الاسم متاح بالأعلى. لتصحيح تاريخ الميلاد، أعد إدخاله في شاشة التحقق من العمر. لا نحتفظ بتاريخ ميلادك الكامل."
                                else
                                    "Edit your local name above. To correct your birth date, enter it again on the age verification screen. Your full birth date is not stored.",
                                color = Muted, fontSize = 12.sp, lineHeight = 17.sp
                            )
                            OutlinedButton(onClick = onEditAge) {
                                Text(if (lang == "ar") "تصحيح العمر" else "Correct age")
                            }
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
                                    else -> (if (state.cloudStatus == "account_changed") {
                                    if (lang == "ar")
                                        "تم اكتشاف حساب Google مختلف. أوقفنا المزامنة والشراء لحماية تقدم حسابك السابق. أعد اختيار الحساب القديم من إعدادات Google."
                                    else
                                        "A different Google account was detected. Cloud sync and billing are blocked to protect the previous account's progress. Re-select the original account in Google settings."
                                } else cloudStatusLabel(state.cloudStatus, lang))
                                }, color = Muted, fontSize = 12.sp, lineHeight = 17.sp
                            )
                            if (playGamesAllowed && playGamesConfigured &&
                                state.cloudStatus !in listOf("checking", "syncing", "conflict")) {
                                OutlinedButton(onClick = onRequestPlayGamesSignIn) {
                                    Text(tx(lang, if (state.cloudStatus in listOf("synced", "connected", "sync_error"))
                                        "pgs_retry" else "pgs_connect"))
                                }
                            }

                            if (playGamesAllowed) {
                                HorizontalDivider(color = DividerColor)
                                SettingHeading(
                                    if (lang == "ar") "تسجيل الخروج أو تغيير حساب Google"
                                    else "Sign out or change Google account",
                                    Icons.Rounded.Person
                                )
                                Text(
                                    if (lang == "ar")
                                        "تسجيل الخروج واختيار حساب Play Games آخر يتمان من إعدادات Google على الجهاز، وليس من داخل اللعبة. افتح الإعدادات ← Google ← خدمات Play Games ← تغيير الحساب للألعاب. بعد التغيير أغلق اللعبة بالكامل وأعد فتحها. بيانات اللعبة المحلية مشتركة حاليًا؛ لا تزامن حسابين مختلفين قبل فصل الحفظ."
                                    else
                                        "Google manages Play Games sign-out and account selection in Android settings, not inside this game. Open Settings → Google → Play Games services → Change account for games. Fully close and reopen Za Snake afterward. Local game data is not yet separated by Google account; don't sync different accounts until profiles are isolated.",
                                    color = Muted, fontSize = 12.sp, lineHeight = 17.sp
                                )
                                OutlinedButton(onClick = onChangePlayGamesAccount) {
                                    Text(if (lang == "ar") "فتح إعدادات حساب Google" else "Open Google account settings")
                                }
                            }

                            if (BuildConfig.DEBUG && onTestGameServerAuth != null && playGamesAllowed) {
                                val webClientConfigured =
                                    BuildConfig.PGS_WEB_CLIENT_ID.endsWith(".apps.googleusercontent.com")
                                HorizontalDivider(color = DividerColor)
                                Text(
                                    if (lang == "ar") "اختبار مصادقة خادم الألعاب (Debug)"
                                    else "Game server authentication test (Debug)",
                                    color = PrimaryText, fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                OutlinedButton(
                                    onClick = {
                                        serverAuthTestRunning = true
                                        serverAuthTestResult = null
                                        onTestGameServerAuth { success ->
                                            serverAuthTestRunning = false
                                            serverAuthTestResult = success
                                        }
                                    },
                                    enabled = playGamesConfigured && webClientConfigured &&
                                        !serverAuthTestRunning
                                ) {
                                    Text(if (lang == "ar") "اختبار رمز الخادم"
                                         else "Test game server authentication")
                                }
                                Text(
                                    when {
                                        !playGamesConfigured ->
                                            if (lang == "ar") "خدمة Play Games غير مهيأة."
                                            else "Play Games is not configured."
                                        !webClientConfigured ->
                                            if (lang == "ar") "أضف pgsWebClientId إلى إعدادات بناء Debug أولًا."
                                            else "Set pgsWebClientId in the Debug build configuration first."
                                        serverAuthTestRunning ->
                                            if (lang == "ar") "جارٍ طلب رمز مؤقت من Play Games..."
                                            else "Requesting a temporary code from Play Games..."
                                        serverAuthTestResult == true ->
                                            if (lang == "ar") "نجح: استلم التطبيق رمز الخادم. لم يُختبر تبادل الرمز مع Backend."
                                            else "Success: received a server auth code. Backend exchange was not tested."
                                        serverAuthTestResult == false ->
                                            if (lang == "ar") "فشل طلب الرمز. راجع تسجيل دخول Play Games وإعداد Game server."
                                            else "Code request failed. Check Play Games sign-in and Game server credentials."
                                        else ->
                                            if (lang == "ar") "سجّل الدخول إلى Play Games أولًا. الاختبار لا يرسل الرمز ولا يفعّل الشراء."
                                            else "Sign in to Play Games first. This test does not send the code or enable purchases."
                                    },
                                    color = Muted, fontSize = 12.sp, lineHeight = 17.sp
                                )
                            }
                            HorizontalDivider(color = DividerColor)
                            SettingHeading(tx(lang, "cloud_sync_title"), Icons.Rounded.CloudOff)
                            Text(if (playGamesAllowed) (if (state.cloudStatus == "account_changed") {
                                    if (lang == "ar")
                                        "تم اكتشاف حساب Google مختلف. أوقفنا المزامنة والشراء لحماية تقدم حسابك السابق. أعد اختيار الحساب القديم من إعدادات Google."
                                    else
                                        "A different Google account was detected. Cloud sync and billing are blocked to protect the previous account's progress. Re-select the original account in Google settings."
                                } else cloudStatusLabel(state.cloudStatus, lang))
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
internal fun <T> SettingsOwnedChoiceGrid(
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
internal fun SettingsOwnedChoiceCard(
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

internal fun themeTitle(theme: GameTheme, language: String) = tx(language, "theme_${theme.key}")
internal fun themeDescription(theme: GameTheme, language: String) = tx(language, "theme_${theme.key}_desc")


@Composable
internal fun SettingsCategoryCard(
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
