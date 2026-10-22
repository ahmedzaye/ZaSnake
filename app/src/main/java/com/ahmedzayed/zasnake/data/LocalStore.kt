package com.ahmedzayed.zasnake.data

import android.content.Context
import android.util.AtomicFile
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.ahmedzayed.zasnake.game.ControlStyle
import com.ahmedzayed.zasnake.language.DeviceLanguage
import com.ahmedzayed.zasnake.game.AppSettings
import com.ahmedzayed.zasnake.game.PlayerNamePolicy
import com.ahmedzayed.zasnake.game.Cell
import com.ahmedzayed.zasnake.game.DeviceCounters
import com.ahmedzayed.zasnake.game.DailyDeviceProgress
import com.ahmedzayed.zasnake.game.Difficulty
import com.ahmedzayed.zasnake.game.Direction
import com.ahmedzayed.zasnake.game.GameMode
import com.ahmedzayed.zasnake.game.GameTheme
import com.ahmedzayed.zasnake.game.FREE_THEME_KEYS
import com.ahmedzayed.zasnake.game.GameSnapshot
import com.ahmedzayed.zasnake.game.PlayerStats
import com.ahmedzayed.zasnake.game.RoundTombstone
import com.ahmedzayed.zasnake.game.SnakeSkin
import com.ahmedzayed.zasnake.game.SnakeHeadStyle
import com.ahmedzayed.zasnake.game.SnakeBodyType
import com.ahmedzayed.zasnake.game.FREE_BODY_KEYS
import com.ahmedzayed.zasnake.game.FREE_HEAD_KEYS
import com.ahmedzayed.zasnake.game.FREE_SKIN_KEYS
import com.ahmedzayed.zasnake.game.restoredOwnedSkinKeys
import com.ahmedzayed.zasnake.game.SpecialFood
import com.ahmedzayed.zasnake.game.SpecialType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileNotFoundException
import java.nio.charset.StandardCharsets
import java.util.UUID

private val Context.settingsDataStore by preferencesDataStore(name = "za_snake_settings")

class SettingsRepository(private val context: Context) {
    private object Keys {
        val languageMode = stringPreferencesKey("language_mode")
        val playerName = stringPreferencesKey("local_player_name")
        val sound = booleanPreferencesKey("sound")
        val sfxVolume = intPreferencesKey("sfx_volume")
        val musicEnabled = booleanPreferencesKey("music_enabled")
        val musicVolume = intPreferencesKey("music_volume")
        val haptics = booleanPreferencesKey("haptics")
        val difficulty = stringPreferencesKey("difficulty")
        val walls = booleanPreferencesKey("walls")
        val skin = stringPreferencesKey("skin")
        val headStyle = stringPreferencesKey("head_style")
        val bodyType = stringPreferencesKey("body_type")
        val ownedBodies = stringPreferencesKey("owned_bodies")
        val theme = stringPreferencesKey("game_theme")
        val joystickLeft = booleanPreferencesKey("joystick_left_landscape")
        val controlStyle = stringPreferencesKey("control_style")
        val lastMode = stringPreferencesKey("last_mode")
        val ownedSkins = stringPreferencesKey("owned_skins")
        val ownedHeads = stringPreferencesKey("owned_heads")
        val ownedThemes = stringPreferencesKey("owned_themes")
        val installId = stringPreferencesKey("install_id")
    }

    val settings: Flow<AppSettings> = context.settingsDataStore.data.map { p ->
        AppSettings(
            // The old language field was device-only; start existing players in automatic mode.
            language = DeviceLanguage.resolve(p[Keys.languageMode], DeviceLanguage.fromLocale(context.resources.configuration.locales[0])),
            languageMode = DeviceLanguage.normalizePreference(p[Keys.languageMode]),
            playerName = PlayerNamePolicy.restored(p[Keys.playerName]),
            soundEnabled = p[Keys.sound] ?: true,
            sfxVolume = (p[Keys.sfxVolume] ?: 70).coerceIn(0, 100),
            musicEnabled = p[Keys.musicEnabled] ?: true,
            musicVolume = (p[Keys.musicVolume] ?: 25).coerceIn(0, 100),
            hapticsEnabled = p[Keys.haptics] ?: true,
            difficulty = Difficulty.fromKey(p[Keys.difficulty]),
            wallsEnabled = p[Keys.walls] ?: true,
            skin = SnakeSkin.fromKey(p[Keys.skin]),
            headStyle = SnakeHeadStyle.fromKey(p[Keys.headStyle]),
            bodyType = SnakeBodyType.fromKey(p[Keys.bodyType]),
            ownedBodyKeys = (p[Keys.ownedBodies] ?: "").split(",").filter(String::isNotBlank).toSet() + FREE_BODY_KEYS,
            theme = GameTheme.fromKey(p[Keys.theme]),
            joystickLeftLandscape = p[Keys.joystickLeft] ?: false,
            controlStyle = ControlStyle.fromKey(p[Keys.controlStyle]),
            lastMode = GameMode.fromKey(p[Keys.lastMode]),
            ownedSkinKeys = restoredOwnedSkinKeys(p[Keys.ownedSkins], p[Keys.skin]),
            ownedHeadKeys = (p[Keys.ownedHeads] ?: "").split(",").filter(String::isNotBlank).toSet() + FREE_HEAD_KEYS,
            ownedThemeKeys = (p[Keys.ownedThemes] ?: "").split(",").filter(String::isNotBlank).toSet() + FREE_THEME_KEYS
        )
    }

    suspend fun save(settings: AppSettings) {
        context.settingsDataStore.edit { p ->
            p[Keys.languageMode] = DeviceLanguage.normalizePreference(settings.languageMode)
            p[Keys.playerName] = PlayerNamePolicy.restored(settings.playerName)
            p[Keys.sound] = settings.soundEnabled
            p[Keys.sfxVolume] = settings.sfxVolume.coerceIn(0, 100)
            p[Keys.musicEnabled] = settings.musicEnabled
            p[Keys.musicVolume] = settings.musicVolume.coerceIn(0, 100)
            p[Keys.haptics] = settings.hapticsEnabled
            p[Keys.difficulty] = settings.difficulty.key
            p[Keys.walls] = settings.wallsEnabled
            p[Keys.skin] = settings.skin.key
            p[Keys.headStyle] = settings.headStyle.key
            p[Keys.bodyType] = settings.bodyType.key
            p[Keys.ownedBodies] = (settings.ownedBodyKeys + FREE_BODY_KEYS).joinToString(",")
            p[Keys.theme] = settings.theme.key
            p[Keys.joystickLeft] = settings.joystickLeftLandscape
            p[Keys.controlStyle] = settings.controlStyle.key
            p[Keys.lastMode] = settings.lastMode.key
            p[Keys.ownedSkins] = (settings.ownedSkinKeys + FREE_SKIN_KEYS).joinToString(",")
            p[Keys.ownedHeads] = (settings.ownedHeadKeys + FREE_HEAD_KEYS).joinToString(",")
            p[Keys.ownedThemes] = (settings.ownedThemeKeys + FREE_THEME_KEYS).joinToString(",")
        }
    }

    suspend fun installationId(): String {
        val current = context.settingsDataStore.data.map { it[Keys.installId] }.first()
        if (!current.isNullOrBlank()) return current
        val generated = UUID.randomUUID().toString()
        context.settingsDataStore.edit { it[Keys.installId] = generated }
        return generated
    }
}

class LocalStore(context: Context) {
    private val saveDirectory = File(context.filesDir, "za_snake")
    private val gameFile = AtomicFile(File(saveDirectory, "current_round.json"))
    private val statsFile = AtomicFile(File(saveDirectory, "player_stats.json"))
    private val tombstoneFile = AtomicFile(File(saveDirectory, "round_tombstone.json"))
    private val gameMutex = Mutex()
    private val statsMutex = Mutex()
    private val tombstoneMutex = Mutex()

    suspend fun saveGame(snapshot: GameSnapshot, forceReplace: Boolean = false) = withContext(Dispatchers.IO) {
        gameMutex.withLock {
            // Delayed coroutine writes must not replace a newer checkpoint after a new round starts.
            val existing = try { decodeSnapshot(readAtomic(gameFile)) } catch (_: Exception) { null }
            if (!forceReplace && existing != null && existing.savedAtEpochMs > snapshot.savedAtEpochMs) return@withLock
            saveAtomic(gameFile, encodeSnapshot(snapshot))
        }
    }

    suspend fun loadGame(): GameSnapshot? = withContext(Dispatchers.IO) {
        gameMutex.withLock { try { decodeSnapshot(readAtomic(gameFile)) } catch (_: Exception) { null } }
    }

    suspend fun clearGame(expectedSessionId: String? = null) = withContext(Dispatchers.IO) {
        gameMutex.withLock {
            try {
                val existing = decodeSnapshot(readAtomic(gameFile))
                // Prevent an old game-over callback from clearing a newer round saved in the meantime.
                if (expectedSessionId == null || existing?.sessionId == expectedSessionId) gameFile.delete()
            } catch (_: FileNotFoundException) {
                Unit
            } catch (_: Exception) {
                if (expectedSessionId == null) runCatching { gameFile.delete() }
            }
        }
    }

    suspend fun saveStats(stats: PlayerStats) = withContext(Dispatchers.IO) {
        statsMutex.withLock {
            // Stats are monotonic: merging on write protects against out-of-order async saves.
            val existing = try { decodeStats(readAtomic(statsFile)) } catch (_: Exception) { PlayerStats() }
            saveAtomic(statsFile, encodeStats(existing.merge(stats)))
        }
    }

    suspend fun loadStats(): PlayerStats = withContext(Dispatchers.IO) {
        statsMutex.withLock { try { decodeStats(readAtomic(statsFile)) } catch (_: Exception) { PlayerStats() } }
    }

    suspend fun saveRoundTombstone(tombstone: RoundTombstone) = withContext(Dispatchers.IO) {
        tombstoneMutex.withLock {
            val existing = try { decodeTombstone(readAtomic(tombstoneFile)) } catch (_: Exception) { null }
            val chosen = if (existing == null || tombstone.completedAtEpochMs >= existing.completedAtEpochMs) tombstone else existing
            saveAtomic(tombstoneFile, encodeTombstone(chosen))
        }
    }

    suspend fun loadRoundTombstone(): RoundTombstone? = withContext(Dispatchers.IO) {
        tombstoneMutex.withLock { try { decodeTombstone(readAtomic(tombstoneFile)) } catch (_: Exception) { null } }
    }

    private fun saveAtomic(file: AtomicFile, text: String) {
        saveDirectory.mkdirs()
        var stream: java.io.FileOutputStream? = null
        try {
            stream = file.startWrite()
            stream.write(text.toByteArray(StandardCharsets.UTF_8))
            stream.fd.sync()
            file.finishWrite(stream)
        } catch (t: Throwable) {
            if (stream != null) file.failWrite(stream)
            throw t
        }
    }

    private fun readAtomic(file: AtomicFile): String {
        return file.openRead().bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
    }
}

fun encodeSnapshot(s: GameSnapshot): String = JSONObject().apply {
    put("schemaVersion", 1)
    put("sessionId", s.sessionId)
    put("mode", s.mode.key)
    put("difficulty", s.difficulty.key)
    put("wallsEnabled", s.wallsEnabled)
    put("snake", JSONArray().apply { s.snake.forEach { put(cellJson(it)) } })
    put("direction", s.direction.name)
    put("nextDirection", s.nextDirection.name)
    put("food", cellJson(s.food))
    put("specials", JSONArray().apply {
        s.specials.forEach { special ->
            put(JSONObject().apply {
                put("type", special.type.key)
                put("cell", cellJson(special.cell))
                put("remainingMs", special.remainingMs)
                put("lifetimeMs", special.lifetimeMs)
            })
        }
    })
    put("score", s.score)
    put("timeRemainingMs", s.timeRemainingMs)
    put("revision", s.revision)
    put("writerDeviceId", s.writerDeviceId)
    put("savedAtEpochMs", s.savedAtEpochMs)
    put("pendingCollision", s.pendingCollision)
    put("reviveUsed", s.reviveUsed)
    put("shieldRemainingMs", s.shieldRemainingMs)
}.toString()

fun decodeSnapshot(raw: String): GameSnapshot? {
    if (raw.isBlank()) return null
    val o = JSONObject(raw)
    val snakeArray = o.optJSONArray("snake") ?: return null
    val snake = (0 until snakeArray.length()).mapNotNull { snakeArray.optJSONObject(it)?.let(::readCell) }
    if (snake.size < 3 || snake.any { it.x !in 0 until 20 || it.y !in 0 until 20 }) return null
    val specialsArray = o.optJSONArray("specials") ?: JSONArray()
    val specials = (0 until specialsArray.length()).mapNotNull { i ->
        val sp = specialsArray.optJSONObject(i) ?: return@mapNotNull null
        val type = when (sp.optString("type")) {
            "golden" -> SpecialType.GOLDEN
            "scissors" -> SpecialType.SCISSORS
            else -> return@mapNotNull null
        }
        val cell = sp.optJSONObject("cell")?.let(::readCell) ?: return@mapNotNull null
        val remaining = sp.optLong("remainingMs", 0L)
        val lifetime = sp.optLong("lifetimeMs", 1L)
        if (remaining <= 0 || lifetime <= 0) return@mapNotNull null
        SpecialFood(type, cell, remaining, lifetime)
    }
    return GameSnapshot(
        sessionId = o.optString("sessionId", UUID.randomUUID().toString()),
        mode = GameMode.fromKey(o.optString("mode")),
        difficulty = Difficulty.fromKey(o.optString("difficulty")),
        wallsEnabled = o.optBoolean("wallsEnabled", true),
        snake = snake,
        direction = enumOr(o.optString("direction"), Direction.RIGHT),
        nextDirection = enumOr(o.optString("nextDirection"), Direction.RIGHT),
        food = o.optJSONObject("food")?.let(::readCell) ?: return null,
        specials = specials,
        score = o.optInt("score", 0).coerceAtLeast(0),
        timeRemainingMs = o.optLong("timeRemainingMs", 60_000L).coerceAtLeast(0L),
        revision = o.optLong("revision", 0L).coerceAtLeast(0L),
        writerDeviceId = o.optString("writerDeviceId", ""),
        savedAtEpochMs = o.optLong("savedAtEpochMs", System.currentTimeMillis()),
        pendingCollision = o.optBoolean("pendingCollision", false),
        reviveUsed = o.optBoolean("reviveUsed", false),
        shieldRemainingMs = o.optLong("shieldRemainingMs", 0L).coerceIn(0L, 5_000L)
    )
}

fun encodeStats(stats: PlayerStats): String = JSONObject().apply {
    put("schemaVersion", 1)
    put("devices", JSONObject().apply {
        stats.deviceCounters.forEach { (id, counters) ->
            put(id, JSONObject().apply {
                put("games", counters.games)
                put("food", counters.food)
                put("playMs", counters.playMs)
                put("golden", counters.golden)
                put("scissors", counters.scissors)
            })
        }
    })
    put("bestScore", stats.bestScore)
    put("bestTimeScore", stats.bestTimeScore)
    put("maxLength", stats.maxLength)
    put("unlockedAchievements", JSONArray().apply { stats.unlockedAchievements.forEach { put(it) } })
    put("coinRewards", JSONObject().apply { stats.coinRewards.forEach { (sessionId, amount) -> put(sessionId, amount) } })
    put("doubledRoundCoins", JSONObject().apply { stats.doubledRoundCoins.forEach { (id, amount) -> put(id, amount) } })
    put("claimedAchievementRewards", JSONObject().apply { stats.claimedAchievementRewards.forEach { (id, amount) -> put(id, amount) } })
    put("coinSpending", JSONObject().apply { stats.coinSpending.forEach { (transactionId, amount) -> put(transactionId, amount) } })
    put("unlockedSkins", JSONArray().apply { stats.unlockedSkins.forEach { put(it) } })
    put("unlockedHeads", JSONArray().apply { stats.unlockedHeads.forEach { put(it) } })
    put("unlockedBodies", JSONArray().apply { stats.unlockedBodies.forEach { put(it) } })
    put("unlockedThemes", JSONArray().apply { stats.unlockedThemes.forEach { put(it) } })
    put("dailyActivity", JSONObject().apply {
        stats.dailyActivity.forEach { (date, devices) ->
            put(date, JSONObject().apply {
                devices.forEach { (id, progress) ->
                    put(id, JSONObject().apply {
                        put("apples", progress.apples)
                        put("completedGames", progress.completedGames)
                        put("bestRoundScore", progress.bestRoundScore)
                    })
                }
            })
        }
    })
    put("claimedDailyChallenges", JSONObject().apply {
        stats.claimedDailyChallenges.forEach { (id, amount) -> put(id, amount) }
    })
    put("claimedDailyLogins", JSONObject().apply {
        stats.claimedDailyLogins.forEach { (day, index) -> put(day, index) }
    })
    put("latestDailyDate", stats.latestDailyDate)
}.toString()

fun decodeStats(raw: String): PlayerStats {
    val o = JSONObject(raw)
    val devicesJson = o.optJSONObject("devices") ?: JSONObject()
    val counters = mutableMapOf<String, DeviceCounters>()
    val keys = devicesJson.keys()
    while (keys.hasNext()) {
        val id = keys.next()
        val v = devicesJson.optJSONObject(id) ?: continue
        counters[id] = DeviceCounters(
            games = v.optLong("games", 0L).coerceAtLeast(0),
            food = v.optLong("food", 0L).coerceAtLeast(0),
            playMs = v.optLong("playMs", 0L).coerceAtLeast(0),
            golden = v.optLong("golden", 0L).coerceAtLeast(0),
            scissors = v.optLong("scissors", 0L).coerceAtLeast(0)
        )
    }
    val achievementsArray = o.optJSONArray("unlockedAchievements") ?: JSONArray()
    val achievements = (0 until achievementsArray.length()).mapNotNull { achievementsArray.optString(it).takeIf(String::isNotBlank) }.toSet()
    val coinRewardsJson = o.optJSONObject("coinRewards") ?: JSONObject()
    val coinRewards = mutableMapOf<String, Int>()
    val rewardKeys = coinRewardsJson.keys()
    while (rewardKeys.hasNext()) {
        val sessionId = rewardKeys.next()
        if (sessionId.isNotBlank()) coinRewards[sessionId] = coinRewardsJson.optInt(sessionId, 0).coerceAtLeast(0)
    }
    val doubledJson = o.optJSONObject("doubledRoundCoins") ?: JSONObject()
    val doubledCoins = mutableMapOf<String, Int>()
    val doubledKeys = doubledJson.keys()
    while (doubledKeys.hasNext()) {
        val id = doubledKeys.next()
        if (id.isNotBlank() && id in coinRewards) {
            doubledCoins[id] = doubledJson.optInt(id, 0).coerceIn(0, coinRewards[id] ?: 0)
        }
    }
    // Old saves don't have this field: every completed achievement remains claimable.
    val claimedJson = o.optJSONObject("claimedAchievementRewards") ?: JSONObject()
    val claimedRewards = mutableMapOf<String, Int>()
    val claimedKeys = claimedJson.keys()
    while (claimedKeys.hasNext()) {
        val id = claimedKeys.next()
        if (id.isNotBlank()) claimedRewards[id] = claimedJson.optInt(id, 0).coerceAtLeast(0)
    }
    val spendingJson = o.optJSONObject("coinSpending") ?: JSONObject()
    val spending = mutableMapOf<String, Int>()
    val spendingKeys = spendingJson.keys()
    while (spendingKeys.hasNext()) {
        val transactionId = spendingKeys.next()
        if (transactionId.isNotBlank()) spending[transactionId] = spendingJson.optInt(transactionId, 0).coerceAtLeast(0)
    }
    val dailyActivity = mutableMapOf<String, Map<String, DailyDeviceProgress>>()
    val dailyJson = o.optJSONObject("dailyActivity") ?: JSONObject()
    val dailyDates = dailyJson.keys()
    while (dailyDates.hasNext()) {
        val date = dailyDates.next()
        val byDevice = dailyJson.optJSONObject(date) ?: continue
        val values = mutableMapOf<String, DailyDeviceProgress>()
        val ids = byDevice.keys()
        while (ids.hasNext()) {
            val id = ids.next()
            val p = byDevice.optJSONObject(id) ?: continue
            values[id] = DailyDeviceProgress(
                apples = p.optLong("apples", 0).coerceAtLeast(0L),
                completedGames = p.optLong("completedGames", 0).coerceAtLeast(0L),
                bestRoundScore = p.optInt("bestRoundScore", 0).coerceAtLeast(0)
            )
        }
        if (date.isNotBlank()) dailyActivity[date] = values
    }
    val challengeClaims = mutableMapOf<String, Int>()
    val challengeJson = o.optJSONObject("claimedDailyChallenges") ?: JSONObject()
    val challengeKeys = challengeJson.keys()
    while (challengeKeys.hasNext()) {
        val id = challengeKeys.next()
        if (id.isNotBlank()) challengeClaims[id] = challengeJson.optInt(id, 0).coerceIn(0, 3)
    }
    val loginClaims = mutableMapOf<String, Int>()
    val loginJson = o.optJSONObject("claimedDailyLogins") ?: JSONObject()
    val loginKeys = loginJson.keys()
    while (loginKeys.hasNext()) {
        val date = loginKeys.next()
        val index = loginJson.optInt(date, 0)
        if (date.isNotBlank() && index in 1..7) loginClaims[date] = index
    }
    return PlayerStats(
        deviceCounters = counters,
        bestScore = o.optInt("bestScore", 0).coerceAtLeast(0),
        bestTimeScore = o.optInt("bestTimeScore", 0).coerceAtLeast(0),
        maxLength = o.optInt("maxLength", 0).coerceAtLeast(0),
        unlockedAchievements = achievements,
        coinRewards = coinRewards,
        doubledRoundCoins = doubledCoins,
        claimedAchievementRewards = claimedRewards,
        coinSpending = spending,
        unlockedSkins = (0 until (o.optJSONArray("unlockedSkins")?.length() ?: 0)).mapNotNull { o.optJSONArray("unlockedSkins")?.optString(it)?.takeIf(String::isNotBlank) }.toSet(),
        unlockedHeads = (0 until (o.optJSONArray("unlockedHeads")?.length() ?: 0)).mapNotNull { o.optJSONArray("unlockedHeads")?.optString(it)?.takeIf(String::isNotBlank) }.toSet(),
        unlockedBodies = (0 until (o.optJSONArray("unlockedBodies")?.length() ?: 0)).mapNotNull { o.optJSONArray("unlockedBodies")?.optString(it)?.takeIf(String::isNotBlank) }.toSet(),
        unlockedThemes = (0 until (o.optJSONArray("unlockedThemes")?.length() ?: 0)).mapNotNull { o.optJSONArray("unlockedThemes")?.optString(it)?.takeIf(String::isNotBlank) }.toSet(),
        dailyActivity = dailyActivity.toSortedMap().entries.toList().takeLast(35).associate { it.key to it.value },
        claimedDailyChallenges = challengeClaims,
        claimedDailyLogins = loginClaims,
        latestDailyDate = o.optString("latestDailyDate", "")
    )
}

private fun encodeTombstone(t: RoundTombstone): String = JSONObject().apply {
    put("completedSessionId", t.completedSessionId)
    put("completedAtEpochMs", t.completedAtEpochMs)
    put("writerDeviceId", t.writerDeviceId)
}.toString()

private fun decodeTombstone(raw: String): RoundTombstone? {
    val o = JSONObject(raw)
    val id = o.optString("completedSessionId")
    val at = o.optLong("completedAtEpochMs", 0L)
    if (id.isBlank() || at <= 0L) return null
    return RoundTombstone(id, at, o.optString("writerDeviceId", ""))
}

private fun cellJson(cell: Cell) = JSONObject().put("x", cell.x).put("y", cell.y)
private fun readCell(o: JSONObject) = Cell(o.optInt("x", -1), o.optInt("y", -1))
private inline fun <reified T : Enum<T>> enumOr(value: String, fallback: T): T =
    try { enumValueOf<T>(value) } catch (_: Exception) { fallback }

data class CloudBundle(
    val stats: PlayerStats,
    val game: GameSnapshot?,
    val roundTombstone: RoundTombstone? = null
)

fun encodeCloudBundle(bundle: CloudBundle): String = JSONObject().apply {
    put("bundleVersion", 1)
    put("stats", JSONObject(encodeStats(bundle.stats)))
    put("game", bundle.game?.let { JSONObject(encodeSnapshot(it)) } ?: JSONObject.NULL)
    put("roundTombstone", bundle.roundTombstone?.let { tombstone -> JSONObject().apply {
        put("completedSessionId", tombstone.completedSessionId)
        put("completedAtEpochMs", tombstone.completedAtEpochMs)
        put("writerDeviceId", tombstone.writerDeviceId)
    } } ?: JSONObject.NULL)
}.toString()

fun decodeCloudBundle(raw: String): CloudBundle? {
    if (raw.isBlank()) return null
    return try {
        val o = JSONObject(raw)
        val stats = o.optJSONObject("stats")?.let { decodeStats(it.toString()) } ?: PlayerStats()
        val gameRaw = o.opt("game")
        val game = if (gameRaw is JSONObject) decodeSnapshot(gameRaw.toString()) else null
        val tombstoneRaw = o.opt("roundTombstone")
        val tombstone = if (tombstoneRaw is JSONObject) {
            val id = tombstoneRaw.optString("completedSessionId")
            val completedAt = tombstoneRaw.optLong("completedAtEpochMs", 0L)
            if (id.isNotBlank() && completedAt > 0L) RoundTombstone(
                completedSessionId = id,
                completedAtEpochMs = completedAt,
                writerDeviceId = tombstoneRaw.optString("writerDeviceId", "")
            ) else null
        } else null
        CloudBundle(stats, game, tombstone)
    } catch (_: Exception) { null }
}
