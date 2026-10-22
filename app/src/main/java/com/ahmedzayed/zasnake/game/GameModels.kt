package com.ahmedzayed.zasnake.game

import java.util.UUID

enum class GameMode(val key: String) {
    CLASSIC("classic"), TIME_ATTACK("time"), ZEN("zen");

    companion object { fun fromKey(value: String?) = entries.firstOrNull { it.key == value } ?: CLASSIC }
}

enum class Difficulty(val key: String, val stepMillis: Long, val pointsPerApple: Int) {
    EASY("easy", 200L, 1), MEDIUM("medium", 130L, 2), HARD("hard", 80L, 4);
    companion object { fun fromKey(value: String?) = entries.firstOrNull { it.key == value } ?: MEDIUM }
}

/** The UI may display prices, but only these canonical prices are used for purchases. */
enum class SnakeSkin(val key: String, val priceCoins: Int) {
    GREEN("green", 0), PURPLE("purple", 0), YELLOW("yellow", 0),
    EGYPT("egypt", 180), CYBER("cyber", 120), ICE("ice", 180), FIRE("fire", 250),
    UAE("uae", 180), JORDAN("jordan", 180), PALESTINE("palestine", 180),
    KUWAIT("kuwait", 180), SAUDI("saudi", 180);

    val isFree: Boolean get() = priceCoins == 0
    companion object { fun fromKey(value: String?) = entries.firstOrNull { it.key == value } ?: GREEN }
}

val FREE_SKIN_KEYS: Set<String> = SnakeSkin.entries.filter { it.isFree }.map { it.key }.toSet()
val ARAB_COLLECTION: List<SnakeSkin> = listOf(
    SnakeSkin.EGYPT, SnakeSkin.UAE, SnakeSkin.JORDAN, SnakeSkin.PALESTINE,
    SnakeSkin.KUWAIT, SnakeSkin.SAUDI
)

/** Existing installs persisted Egypt as owned when it used to be free; new installs do not.
 * This also supports older installs which persisted only the selected skin. */
fun restoredOwnedSkinKeys(stored: String?, selected: String?): Set<String> {
    val explicit = stored?.split(",")?.filter(String::isNotBlank)?.toSet().orEmpty()
    val legacySelected = if (selected == SnakeSkin.EGYPT.key) setOf(SnakeSkin.EGYPT.key) else emptySet()
    return FREE_SKIN_KEYS + explicit + legacySelected
}

/** Free themes stay available forever. Premium prices are canonical, never supplied by the UI. */
enum class GameTheme(val key: String, val priceCoins: Int) {
    NIGHT("night", 0), LIGHT("light", 0), EMERALD("emerald", 0),
    OCEAN("ocean", 80), SUNSET("sunset", 100), ROYAL("royal", 120),
    DESERT("desert", 140), ROSE("rose", 160);

    val isFree: Boolean get() = priceCoins == 0
    companion object { fun fromKey(value: String?) = entries.firstOrNull { it.key == value } ?: NIGHT }
}

val FREE_THEME_KEYS: Set<String> = GameTheme.entries.filter { it.isFree }.map { it.key }.toSet()

/** Head silhouette is independent from the snake skin palette and body geometry. */
enum class SnakeHeadStyle(val key: String, val priceCoins: Int) {
    CLASSIC("classic", 0), SHARP("sharp", 120), CUTE("cute", 120),
    ROYAL("royal", 180), CYBER("cyber", 220);

    val isFree: Boolean get() = priceCoins == 0
    companion object { fun fromKey(value: String?) = entries.firstOrNull { it.key == value } ?: CLASSIC }
}

val FREE_HEAD_KEYS: Set<String> = SnakeHeadStyle.entries.filter { it.isFree }.map { it.key }.toSet()

/** Body geometry is independent of the snake skin's colors and its head style. */
enum class SnakeBodyType(val key: String, val priceCoins: Int) {
    SEGMENTED("segmented", 0), SMOOTH("smooth", 250),
    RETRO_PIXEL("retro_pixel", 200), ARMORED("armored", 300), CRYSTAL("crystal", 350);
    val isFree: Boolean get() = priceCoins == 0
    companion object { fun fromKey(value: String?) = entries.firstOrNull { it.key == value } ?: SEGMENTED }
}

val FREE_BODY_KEYS: Set<String> = SnakeBodyType.entries.filter { it.isFree }.map { it.key }.toSet()


enum class ControlStyle(val key: String) {
    FLOATING_JOYSTICK("floating_joystick"), D_PAD("d_pad");
    companion object { fun fromKey(value: String?) = entries.firstOrNull { it.key == value } ?: FLOATING_JOYSTICK }
}

data class AppSettings(
    val language: String = "ar",
    val languageMode: String = "system",
    val playerName: String = PlayerNamePolicy.DEFAULT_NAME,
    val soundEnabled: Boolean = true,
    val sfxVolume: Int = 70,
    val musicEnabled: Boolean = true,
    val musicVolume: Int = 25,
    val hapticsEnabled: Boolean = true,
    val difficulty: Difficulty = Difficulty.MEDIUM,
    val wallsEnabled: Boolean = true,
    val skin: SnakeSkin = SnakeSkin.GREEN,
    val headStyle: SnakeHeadStyle = SnakeHeadStyle.CLASSIC,
    val bodyType: SnakeBodyType = SnakeBodyType.SEGMENTED,
    val theme: GameTheme = GameTheme.NIGHT,
    val joystickLeftLandscape: Boolean = false,
    val controlStyle: ControlStyle = ControlStyle.FLOATING_JOYSTICK,
    val lastMode: GameMode = GameMode.CLASSIC,
    val ownedSkinKeys: Set<String> = FREE_SKIN_KEYS,
    val ownedHeadKeys: Set<String> = FREE_HEAD_KEYS,
    val ownedBodyKeys: Set<String> = FREE_BODY_KEYS,
    val ownedThemeKeys: Set<String> = FREE_THEME_KEYS
)

data class Cell(val x: Int, val y: Int)
enum class Direction(val dx: Int, val dy: Int) {
    UP(0, -1), RIGHT(1, 0), DOWN(0, 1), LEFT(-1, 0);
    fun isReverseOf(other: Direction) = dx == -other.dx && dy == -other.dy
}

enum class SpecialType(val key: String) { GOLDEN("golden"), SCISSORS("scissors") }
data class SpecialFood(val type: SpecialType, val cell: Cell, val remainingMs: Long, val lifetimeMs: Long)

data class GameSnapshot(
    val sessionId: String = UUID.randomUUID().toString(),
    val mode: GameMode = GameMode.CLASSIC,
    val difficulty: Difficulty = Difficulty.MEDIUM,
    val wallsEnabled: Boolean = true,
    val snake: List<Cell> = listOf(Cell(10, 10), Cell(9, 10), Cell(8, 10)),
    val direction: Direction = Direction.RIGHT,
    val nextDirection: Direction = Direction.RIGHT,
    val food: Cell = Cell(14, 10),
    val specials: List<SpecialFood> = emptyList(),
    val score: Int = 0,
    val timeRemainingMs: Long = 60_000L,
    val revision: Long = 0L,
    val writerDeviceId: String = "",
    val savedAtEpochMs: Long = System.currentTimeMillis(),
    /** Pending collision is saved until the player revives or declines the ad. */
    val pendingCollision: Boolean = false,
    val reviveUsed: Boolean = false,
    val shieldRemainingMs: Long = 0L
)

data class DeviceCounters(
    val games: Long = 0,
    val food: Long = 0,
    val playMs: Long = 0,
    val golden: Long = 0,
    val scissors: Long = 0
) {
    fun merge(other: DeviceCounters) = DeviceCounters(
        games = maxOf(games, other.games),
        food = maxOf(food, other.food),
        playMs = maxOf(playMs, other.playMs),
        golden = maxOf(golden, other.golden),
        scissors = maxOf(scissors, other.scissors)
    )
}

/** Per-device monotonic counters make cloud merges idempotent: merge takes max for each device,
 * then totals across devices. Best values and unlocked achievements use max/union. */
data class PlayerStats(
    val deviceCounters: Map<String, DeviceCounters> = emptyMap(),
    val bestScore: Int = 0,
    val bestTimeScore: Int = 0,
    val maxLength: Int = 0,
    val unlockedAchievements: Set<String> = emptySet(),
    /** One reward entry per completed round keeps coin grants idempotent across devices. */
    val coinRewards: Map<String, Int> = emptyMap(),
    /** Extra rewarded-ad coins, keyed by completed session id and merged idempotently. */
    val doubledRoundCoins: Map<String, Int> = emptyMap(),
    /** Manual achievement claims, keyed by achievement id; merge counts each reward once. */
    val claimedAchievementRewards: Map<String, Int> = emptyMap(),
    /** Idempotent shop transactions; merged by transaction id across local/cloud saves. */
    val coinSpending: Map<String, Int> = emptyMap(),
    val unlockedSkins: Set<String> = emptySet(),
    /** Head unlocks are independent of colors; synced with stable transaction ids. */
    val unlockedHeads: Set<String> = emptySet(),
    /** Body purchases use independent, idempotent transaction identifiers. */
    val unlockedBodies: Set<String> = emptySet(),
    /** Purchased themes are cloud-merged separately from local active-theme preference. */
    val unlockedThemes: Set<String> = emptySet(),
    /** UTC date -> install id -> monotonic daily activity. */
    val dailyActivity: Map<String, Map<String, DailyDeviceProgress>> = emptyMap(),
    /** Permanent claim ledger; keys are UTC date + challenge id. */
    val claimedDailyChallenges: Map<String, Int> = emptyMap(),
    /** Date -> claimed day index 1..7. The value determines the day's coin grant. */
    val claimedDailyLogins: Map<String, Int> = emptyMap(),
    /** Blocks backdating after a newer UTC day was observed. */
    val latestDailyDate: String = ""
) {
    val coins: Long get() = (
        coinRewards.values.sumOf { it.toLong() } +
        doubledRoundCoins.values.sumOf { it.toLong() } +
        claimedAchievementRewards.values.sumOf { it.toLong() } +
        claimedDailyChallenges.values.sumOf { it.toLong() } +
        claimedDailyLogins.values.sumOf { day -> DAILY_LOGIN_COIN_SEQUENCE.getOrElse(day - 1) { 0 }.toLong() } -
        coinSpending.values.sumOf { it.toLong() }
    ).coerceAtLeast(0L)
    /** A transaction also proves ownership for older or partially-written cloud snapshots. */
    val purchasedThemeKeys: Set<String> get() = unlockedThemes +
        GameTheme.entries.filter { "shop_theme_${it.key}" in coinSpending }.map { it.key }
    val purchasedSkinKeys: Set<String> get() = unlockedSkins +
        SnakeSkin.entries.filter { "shop_skin_${it.key}" in coinSpending }.map { it.key }
    val purchasedHeadKeys: Set<String> get() = unlockedHeads +
        SnakeHeadStyle.entries.filter { "shop_head_${it.key}" in coinSpending }.map { it.key }
    val purchasedBodyKeys: Set<String> get() = unlockedBodies +
        SnakeBodyType.entries.filter { "shop_body_${it.key}" in coinSpending }.map { it.key }
    val games: Long get() = deviceCounters.values.sumOf { it.games }
    val food: Long get() = deviceCounters.values.sumOf { it.food }
    val playMs: Long get() = deviceCounters.values.sumOf { it.playMs }
    val golden: Long get() = deviceCounters.values.sumOf { it.golden }
    val scissors: Long get() = deviceCounters.values.sumOf { it.scissors }

    fun editDevice(deviceId: String, transform: (DeviceCounters) -> DeviceCounters): PlayerStats {
        val current = deviceCounters[deviceId] ?: DeviceCounters()
        return copy(deviceCounters = deviceCounters + (deviceId to transform(current)))
    }

    fun merge(other: PlayerStats): PlayerStats {
        val allIds = deviceCounters.keys + other.deviceCounters.keys
        val mergedCounters = allIds.associateWith { id ->
            (deviceCounters[id] ?: DeviceCounters()).merge(other.deviceCounters[id] ?: DeviceCounters())
        }
        return PlayerStats(
            deviceCounters = mergedCounters,
            bestScore = maxOf(bestScore, other.bestScore),
            bestTimeScore = maxOf(bestTimeScore, other.bestTimeScore),
            maxLength = maxOf(maxLength, other.maxLength),
            unlockedAchievements = unlockedAchievements + other.unlockedAchievements,
            // The same completed round can arrive from multiple devices; count its reward once.
            coinRewards = (coinRewards.keys + other.coinRewards.keys).associateWith { sessionId ->
                maxOf(coinRewards[sessionId] ?: 0, other.coinRewards[sessionId] ?: 0)
            },
            doubledRoundCoins = (doubledRoundCoins.keys + other.doubledRoundCoins.keys).associateWith { sessionId ->
                maxOf(doubledRoundCoins[sessionId] ?: 0, other.doubledRoundCoins[sessionId] ?: 0)
            },
            claimedAchievementRewards = (claimedAchievementRewards.keys + other.claimedAchievementRewards.keys).associateWith { id ->
                maxOf(claimedAchievementRewards[id] ?: 0, other.claimedAchievementRewards[id] ?: 0)
            },
            coinSpending = (coinSpending.keys + other.coinSpending.keys).associateWith { transactionId ->
                maxOf(coinSpending[transactionId] ?: 0, other.coinSpending[transactionId] ?: 0)
            },
            unlockedSkins = unlockedSkins + other.unlockedSkins,
            unlockedHeads = unlockedHeads + other.unlockedHeads,
            unlockedBodies = unlockedBodies + other.unlockedBodies,
            unlockedThemes = unlockedThemes + other.unlockedThemes,
            dailyActivity = (dailyActivity.keys + other.dailyActivity.keys).sorted().takeLast(35).associateWith { date ->
                val local = dailyActivity[date].orEmpty()
                val remote = other.dailyActivity[date].orEmpty()
                (local.keys + remote.keys).associateWith { id ->
                    (local[id] ?: DailyDeviceProgress()).merge(remote[id] ?: DailyDeviceProgress())
                }
            },
            claimedDailyChallenges = (claimedDailyChallenges.keys + other.claimedDailyChallenges.keys).associateWith { id ->
                maxOf(claimedDailyChallenges[id] ?: 0, other.claimedDailyChallenges[id] ?: 0)
            },
            claimedDailyLogins = (claimedDailyLogins.keys + other.claimedDailyLogins.keys).associateWith { date ->
                maxOf(claimedDailyLogins[date] ?: 0, other.claimedDailyLogins[date] ?: 0)
            },
            latestDailyDate = maxOf(latestDailyDate, other.latestDailyDate)
        )
    }

    /** Stable transaction ids prevent paying twice after cloud merges or repeated taps. */
    fun purchaseSkin(skin: SnakeSkin): PlayerStats {
        if (skin.isFree || skin.key in purchasedSkinKeys) return this
        val id = "shop_skin_${skin.key}"
        if (id in coinSpending || coins < skin.priceCoins.toLong()) return this
        return copy(
            coinSpending = coinSpending + (id to skin.priceCoins),
            unlockedSkins = unlockedSkins + skin.key
        )
    }

    /** A head style is its own shop product. Stable id prevents repeat charges or cloud duplication. */
    fun purchaseHead(head: SnakeHeadStyle): PlayerStats {
        if (head.isFree || head.key in purchasedHeadKeys) return this
        val id = "shop_head_${head.key}"
        if (id in coinSpending || coins < head.priceCoins.toLong()) return this
        return copy(
            coinSpending = coinSpending + (id to head.priceCoins),
            unlockedHeads = unlockedHeads + head.key
        )
    }

    /** Purchase and unlock a body shape exactly once across cloud merges. */
    fun purchaseBody(body: SnakeBodyType): PlayerStats {
        if (body.isFree || body.key in purchasedBodyKeys) return this
        val id = "shop_body_${body.key}"
        if (id in coinSpending || coins < body.priceCoins.toLong()) return this
        return copy(
            coinSpending = coinSpending + (id to body.priceCoins),
            unlockedBodies = unlockedBodies + body.key
        )
    }

    /** A premium theme can be purchased once; the transaction key is stable across devices. */
    fun purchaseTheme(theme: GameTheme): PlayerStats {
        if (theme.isFree || theme.key in purchasedThemeKeys) return this
        val id = "shop_theme_${theme.key}"
        if (id in coinSpending || coins < theme.priceCoins.toLong()) return this
        return copy(
            coinSpending = coinSpending + (id to theme.priceCoins),
            unlockedThemes = unlockedThemes + theme.key
        )
    }

    /** Safe for repeated taps and cross-device sync: one fixed claim per achievement. */
    fun claimAchievementReward(achievement: AchievementDefinition): PlayerStats {
        if (achievement.id in claimedAchievementRewards) return this
        if (achievement.id !in unlockedAchievements && achievement.progress(this) < achievement.target) return this
        return copy(
            unlockedAchievements = unlockedAchievements + achievement.id,
            claimedAchievementRewards = claimedAchievementRewards + (achievement.id to achievement.rewardCoins)
        )
    }

    /** Grant one extra base payout only when the round's base reward was already recorded. */
    fun doubleRoundCoins(sessionId: String): PlayerStats {
        val base = coinRewards[sessionId] ?: return this
        if (sessionId.isBlank() || sessionId in doubledRoundCoins || base <= 0) return this
        return copy(doubledRoundCoins = doubledRoundCoins + (sessionId to base))
    }

    fun rewardCoins(sessionId: String, amount: Int): PlayerStats {
        if (sessionId.isBlank() || sessionId in coinRewards) return this
        return copy(coinRewards = coinRewards + (sessionId to amount.coerceAtLeast(0)))
    }
}

data class AchievementDefinition(
    val id: String,
    val icon: String,
    val arName: String,
    val enName: String,
    val arDescription: String,
    val enDescription: String,
    val target: Long,
    val rewardCoins: Int,
    val progress: (PlayerStats) -> Long
)

val ACHIEVEMENTS = listOf(
    // Original 11 ids remain unchanged for existing saves and Play Games achievements.
    AchievementDefinition("first", "🍎", "أول قضمة", "First Bite", "كُل أول تفاحة", "Eat your first apple", 1, 25) { it.food },
    AchievementDefinition("apples50", "🍏", "جامع التفاح", "Apple Collector", "كُل 50 تفاحة", "Eat 50 apples", 50, 25) { it.food },
    AchievementDefinition("apples250", "🧺", "حارس البستان", "Orchard Keeper", "كُل 250 تفاحة", "Eat 250 apples", 250, 50) { it.food },
    AchievementDefinition("score10", "⭐", "بداية جيدة", "Good Start", "اجمع 10 نقاط في جولة", "Reach a score of 10 in a round", 10, 25) { it.bestScore.toLong() },
    AchievementDefinition("score50", "🏆", "بطل النقاط", "Score Champion", "اجمع 50 نقطة في جولة", "Reach a score of 50 in a round", 50, 50) { it.bestScore.toLong() },
    AchievementDefinition("golden", "🥇", "لمسة ذهبية", "Golden Touch", "كُل نقطة برتقالية", "Collect an orange bonus", 1, 25) { it.golden },
    AchievementDefinition("barber", "✂️", "الحلاق", "The Barber", "استخدم المقص", "Use the scissors", 1, 25) { it.scissors },
    AchievementDefinition("long30", "🐍", "ثعبان طويل", "Long Snake", "اوصل لطول 30", "Reach a length of 30", 30, 50) { it.maxLength.toLong() },
    AchievementDefinition("games10", "🎮", "لاعب مخلص", "Regular", "العب 10 مرات", "Play 10 games", 10, 25) { it.games },
    AchievementDefinition("clock15", "⏱️", "سابق الزمن", "Beat the Clock", "15 نقطة في هجوم الوقت", "Score 15 in Time Attack", 15, 50) { it.bestTimeScore.toLong() },
    AchievementDefinition("dedicated", "⌛", "صبور", "Dedicated", "العب 10 دقائق إجمالًا", "Play for 10 minutes in total", 600_000L, 25) { it.playMs },
    // Nine cumulative goals use the same monotonic stats as the original achievements.
    AchievementDefinition("apples500", "🍎", "ملك التفاح", "Apple King", "كُل 500 تفاحة إجمالًا", "Eat 500 apples in total", 500, 50) { it.food },
    AchievementDefinition("apples1000", "🧺", "أسطورة البستان", "Orchard Legend", "كُل 1000 تفاحة إجمالًا", "Eat 1,000 apples in total", 1000, 150) { it.food },
    AchievementDefinition("games50", "🎮", "المحترف", "Pro Player", "العب 50 مباراة", "Play 50 games", 50, 50) { it.games },
    AchievementDefinition("games100", "🎖️", "المخضرم", "Veteran", "العب 100 مباراة", "Play 100 games", 100, 150) { it.games },
    AchievementDefinition("golden10", "🥇", "صائد الذهب", "Gold Hunter", "كُل 10 نقاط برتقالية", "Collect 10 orange bonuses", 10, 50) { it.golden },
    AchievementDefinition("golden50", "✨", "عاشق الذهب", "Gold Lover", "كُل 50 نقطة برتقالية", "Collect 50 orange bonuses", 50, 150) { it.golden },
    AchievementDefinition("scissors25", "✂️", "خبير المقص", "Scissors Expert", "استخدم المقص 25 مرة", "Use the scissors 25 times", 25, 50) { it.scissors },
    AchievementDefinition("play60min", "⏳", "المثابر", "Persistent", "العب ساعة إجمالًا", "Play for one hour in total", 3_600_000L, 50) { it.playMs },
    AchievementDefinition("score100", "🏆", "أسطورة النقاط", "Score Legend", "اجمع 100 نقطة في جولة", "Reach a score of 100 in a round", 100, 150) { it.bestScore.toLong() }
)

/** Pre-release economy: 7 easy x25 + 9 medium x50 + 4 long-term x150 = 1,225 coins. */
val ACHIEVEMENT_REWARD_BUDGET = ACHIEVEMENTS.sumOf { it.rewardCoins }

data class EngineEvent(
    val type: Type,
    val reason: String? = null,
    val score: Int = 0,
    val snakeLength: Int = 0,
    /** Impact cell for decorative feedback; not saved or used by game rules. */
    val cell: Cell? = null
) {
    enum class Type { FOOD, GOLDEN, SCISSORS, GAME_OVER }
}

/** UI-only, monotonic-time visual signals. Never serialized or synced. */
data class VisualCue(
    val kind: EngineEvent.Type,
    val cell: Cell,
    val startedAtElapsedMs: Long
)

enum class AppScreen { HOME, MODE_SELECT, GAME, PAUSE, GAME_OVER, SETTINGS, STATISTICS, SHOP, DAILY_REWARDS }
enum class ConfirmationAction { START_NEW, RESTART, EXIT_APP }

data class PendingRoundConflict(
    val localSnapshot: GameSnapshot?,
    val cloudSnapshot: GameSnapshot?,
    val mergedStats: PlayerStats,
    val fromPgsConflict: Boolean = false
)

data class ConflictResolution(
    val payload: String,
    val fromPgsConflict: Boolean,
    val chooseServerSnapshot: Boolean
)

data class RoundTombstone(
    val completedSessionId: String,
    val completedAtEpochMs: Long,
    val writerDeviceId: String
)

data class AppUiState(
    val ready: Boolean = false,
    val screen: AppScreen = AppScreen.HOME,
    val settings: AppSettings = AppSettings(),
    val stats: PlayerStats = PlayerStats(),
    val selectedMode: GameMode = GameMode.CLASSIC,
    val hasSavedGame: Boolean = false,
    val frameVersion: Long = 0,
    val visualCues: List<VisualCue> = emptyList(),
    val newRecordThisRound: Boolean = false,
    val statisticsOpenAchievements: Boolean = false,
    val pendingConfirmation: ConfirmationAction? = null,
    val conflict: PendingRoundConflict? = null,
    val cloudStatus: String = "local",
    val toast: String? = null,
    val newAchievementIds: List<String> = emptyList(),
    val dailyDate: String = ""
)
