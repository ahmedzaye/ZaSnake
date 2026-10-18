package com.ahmedzayed.zasnake.game

import java.util.UUID

enum class GameMode(val key: String) {
    CLASSIC("classic"), TIME_ATTACK("time"), ZEN("zen");

    companion object { fun fromKey(value: String?) = entries.firstOrNull { it.key == value } ?: CLASSIC }
}

enum class Difficulty(val key: String, val stepMillis: Long) {
    EASY("easy", 200L), MEDIUM("medium", 130L), HARD("hard", 80L);
    companion object { fun fromKey(value: String?) = entries.firstOrNull { it.key == value } ?: MEDIUM }
}

enum class SnakeSkin(val key: String) {
    GREEN("green"), PURPLE("purple"), YELLOW("yellow"), EGYPT("egypt");
    companion object { fun fromKey(value: String?) = entries.firstOrNull { it.key == value } ?: GREEN }
}

data class AppSettings(
    val language: String = "ar",
    val soundEnabled: Boolean = true,
    val hapticsEnabled: Boolean = true,
    val difficulty: Difficulty = Difficulty.MEDIUM,
    val wallsEnabled: Boolean = true,
    val skin: SnakeSkin = SnakeSkin.GREEN,
    val joystickLeftLandscape: Boolean = false,
    val lastMode: GameMode = GameMode.CLASSIC
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
    val savedAtEpochMs: Long = System.currentTimeMillis()
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
    val unlockedAchievements: Set<String> = emptySet()
) {
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
            unlockedAchievements = unlockedAchievements + other.unlockedAchievements
        )
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
    val progress: (PlayerStats) -> Long
)

val ACHIEVEMENTS = listOf(
    AchievementDefinition("first", "🍎", "أول قضمة", "First Bite", "كُل أول تفاحة", "Eat your first apple", 1) { it.food },
    AchievementDefinition("apples50", "🍏", "جامع التفاح", "Apple Collector", "كُل 50 تفاحة", "Eat 50 apples", 50) { it.food },
    AchievementDefinition("apples250", "🧺", "حارس البستان", "Orchard Keeper", "كُل 250 تفاحة", "Eat 250 apples", 250) { it.food },
    AchievementDefinition("score10", "⭐", "بداية جيدة", "Good Start", "اجمع 10 نقاط", "Reach a score of 10", 10) { it.bestScore.toLong() },
    AchievementDefinition("score50", "🏆", "بطل النقاط", "Score Champion", "اجمع 50 نقطة", "Reach a score of 50", 50) { it.bestScore.toLong() },
    AchievementDefinition("golden", "🥇", "لمسة ذهبية", "Golden Touch", "كُل تفاحة ذهبية", "Eat a golden apple", 1) { it.golden },
    AchievementDefinition("barber", "✂️", "الحلاق", "The Barber", "استخدم المقص", "Use the scissors", 1) { it.scissors },
    AchievementDefinition("long30", "🐍", "ثعبان طويل", "Long Snake", "اوصل لطول 30", "Reach a length of 30", 30) { it.maxLength.toLong() },
    AchievementDefinition("games10", "🎮", "لاعب مخلص", "Regular", "العب 10 مرات", "Play 10 games", 10) { it.games },
    AchievementDefinition("clock15", "⏱️", "سابق الزمن", "Beat the Clock", "15 نقطة في هجوم الوقت", "Score 15 in Time Attack", 15) { it.bestTimeScore.toLong() },
    AchievementDefinition("dedicated", "⌛", "صبور", "Dedicated", "العب 10 دقائق إجمالًا", "Play for 10 minutes in total", 600_000L) { it.playMs }
)

data class EngineEvent(
    val type: Type,
    val reason: String? = null,
    val score: Int = 0,
    val snakeLength: Int = 0
) {
    enum class Type { FOOD, GOLDEN, SCISSORS, GAME_OVER }
}

enum class AppScreen { HOME, MODE_SELECT, GAME, PAUSE, GAME_OVER, SETTINGS, STATISTICS }
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
    val pendingConfirmation: ConfirmationAction? = null,
    val conflict: PendingRoundConflict? = null,
    val cloudStatus: String = "local",
    val toast: String? = null,
    val newAchievementIds: List<String> = emptyList()
)
