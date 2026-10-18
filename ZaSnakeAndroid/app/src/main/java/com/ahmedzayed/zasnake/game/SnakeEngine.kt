package com.ahmedzayed.zasnake.game

import kotlin.math.floor
import kotlin.random.Random
import java.util.UUID

/** Deterministic 20x20 logic grid. Rendering interpolates between fixed cell steps. */
class SnakeEngine {
    companion object {
        const val GRID_SIZE = 20
        const val START_TIME_MS = 60_000L
        const val TIME_BONUS_MS = 2_000L
        const val GOLDEN_TTL_MS = 5_000L
        const val SCISSORS_TTL_MS = 8_000L
        const val GOLDEN_POINTS = 5
        const val LEVEL_STEP = 8
        private val random = Random.Default
    }

    var mode: GameMode = GameMode.CLASSIC; private set
    var difficulty: Difficulty = Difficulty.MEDIUM; private set
    var wallsEnabled: Boolean = true; private set
    var snake: List<Cell> = listOf(Cell(10, 10), Cell(9, 10), Cell(8, 10)); private set
    var previousSnake: List<Cell> = snake; private set
    var direction: Direction = Direction.RIGHT; private set
    var nextDirection: Direction = Direction.RIGHT; private set
    var food: Cell = Cell(14, 10); private set
    var specials: List<SpecialFood> = emptyList(); private set
    var score: Int = 0; private set
    var timeRemainingMs: Long = START_TIME_MS; private set
    var accumulatorMs: Double = 0.0; private set
    var isRunning: Boolean = false; private set
    var isGameOver: Boolean = false; private set
    var gameOverReason: String? = null; private set
    var sessionId: String = UUID.randomUUID().toString(); private set
    var revision: Long = 0L; private set
    var savedAtEpochMs: Long = System.currentTimeMillis(); private set
    var writerDeviceId: String = ""; private set

    val level: Int get() = floor(score.toDouble() / LEVEL_STEP).toInt() + 1
    val speedMs: Long get() = difficulty.stepMillis
    val isWallsEffective: Boolean get() = mode != GameMode.ZEN && wallsEnabled
    val interpolation: Float get() = (accumulatorMs / speedMs).toFloat().coerceIn(0f, 1f)

    fun startNew(
        newMode: GameMode,
        newDifficulty: Difficulty,
        newWallsEnabled: Boolean,
        deviceId: String = ""
    ) {
        mode = newMode
        difficulty = newDifficulty
        wallsEnabled = newWallsEnabled
        snake = listOf(Cell(10, 10), Cell(9, 10), Cell(8, 10))
        previousSnake = snake
        direction = Direction.RIGHT
        nextDirection = Direction.RIGHT
        score = 0
        timeRemainingMs = START_TIME_MS
        accumulatorMs = 0.0
        specials = emptyList()
        food = randomFreeCell(snake, specials) ?: Cell(15, 10)
        isRunning = true
        isGameOver = false
        gameOverReason = null
        sessionId = UUID.randomUUID().toString()
        revision = 0L
        writerDeviceId = deviceId
        savedAtEpochMs = System.currentTimeMillis()
    }

    fun resume() {
        if (!isGameOver) {
            previousSnake = snake
            accumulatorMs = 0.0
            isRunning = true
        }
    }

    fun pause() { isRunning = false }

    fun requestDirection(requested: Direction) {
        if (isGameOver) return
        // Disallow a 180-degree turn against the direction currently being executed.
        if (!requested.isReverseOf(direction)) nextDirection = requested
    }

    fun update(frameDeltaMs: Long): List<EngineEvent> {
        if (!isRunning || isGameOver) return emptyList()
        val dt = frameDeltaMs.coerceIn(0L, 250L)
        val events = mutableListOf<EngineEvent>()

        if (mode == GameMode.TIME_ATTACK) {
            timeRemainingMs = (timeRemainingMs - dt).coerceAtLeast(0L)
            if (timeRemainingMs <= 0L) {
                finish("time")
                events += EngineEvent(EngineEvent.Type.GAME_OVER, "time", score, snake.size)
                return events
            }
        }

        if (specials.isNotEmpty()) {
            specials = specials.mapNotNull { special ->
                val remaining = special.remainingMs - dt
                if (remaining <= 0L) null else special.copy(remainingMs = remaining)
            }
        }

        accumulatorMs += dt.toDouble()
        var safetyTicks = 0
        while (accumulatorMs >= speedMs && isRunning && safetyTicks < 6) {
            accumulatorMs -= speedMs
            val event = step()
            if (event != null) events += event
            safetyTicks++
        }

        revision += if (events.isNotEmpty()) 1 else 0
        return events
    }

    private fun step(): EngineEvent? {
        previousSnake = snake.toList()
        if (!nextDirection.isReverseOf(direction)) direction = nextDirection
        var head = Cell(snake.first().x + direction.dx, snake.first().y + direction.dy)

        if (isWallsEffective) {
            if (head.x !in 0 until GRID_SIZE || head.y !in 0 until GRID_SIZE) {
                finish("hit")
                return EngineEvent(EngineEvent.Type.GAME_OVER, "hit", score, snake.size)
            }
        } else {
            head = Cell((head.x + GRID_SIZE) % GRID_SIZE, (head.y + GRID_SIZE) % GRID_SIZE)
        }

        val matchingSpecial = specials.firstOrNull { it.cell == head }
        val eatsRegular = head == food
        val willGrow = eatsRegular || matchingSpecial?.type == SpecialType.GOLDEN
        if (mode != GameMode.ZEN && snake.any { it == head }) {
            finish("hit")
            return EngineEvent(EngineEvent.Type.GAME_OVER, "hit", score, snake.size)
        }

        var result: EngineEvent? = null
        snake = listOf(head) + snake
        if (eatsRegular) {
            score += 1
            food = randomFreeCell(snake, specials) ?: food
            if (mode == GameMode.TIME_ATTACK) timeRemainingMs += TIME_BONUS_MS
            result = EngineEvent(EngineEvent.Type.FOOD, score = score, snakeLength = snake.size)
        }
        if (matchingSpecial != null) {
            specials = specials.filterNot { it == matchingSpecial }
            when (matchingSpecial.type) {
                SpecialType.GOLDEN -> {
                    score += GOLDEN_POINTS
                    if (mode == GameMode.TIME_ATTACK) timeRemainingMs += TIME_BONUS_MS
                    result = EngineEvent(EngineEvent.Type.GOLDEN, score = score, snakeLength = snake.size)
                }
                SpecialType.SCISSORS -> {
                    // Shrink after the normal tail movement below, matching the web rules.
                    result = EngineEvent(EngineEvent.Type.SCISSORS, score = score, snakeLength = snake.size)
                }
            }
        }
        if (!willGrow) snake = snake.dropLast(1)
        if (matchingSpecial?.type == SpecialType.SCISSORS) {
            val length = snake.size
            val cut = maxOf(1, floor(length * 0.2).toInt())
            snake = snake.take(maxOf(3, length - cut))
            result = EngineEvent(EngineEvent.Type.SCISSORS, score = score, snakeLength = snake.size)
        }
        // The source game rolls for bonus collectibles per logic step.
        trySpawnSpecialsOnTick()
        return result
    }

    private fun trySpawnSpecialsOnTick() {
        val perTick = speedMs / 1000.0
        if (specials.none { it.type == SpecialType.GOLDEN } && random.nextDouble() < 0.08 * perTick) {
            randomFreeCell(snake, specials)?.let { cell ->
                specials = specials + SpecialFood(SpecialType.GOLDEN, cell, GOLDEN_TTL_MS, GOLDEN_TTL_MS)
            }
        }
        if (snake.size >= 6 && specials.none { it.type == SpecialType.SCISSORS } && random.nextDouble() < 0.04 * perTick) {
            randomFreeCell(snake, specials)?.let { cell ->
                specials = specials + SpecialFood(SpecialType.SCISSORS, cell, SCISSORS_TTL_MS, SCISSORS_TTL_MS)
            }
        }
    }

    private fun randomFreeCell(currentSnake: List<Cell>, currentSpecials: List<SpecialFood>): Cell? {
        repeat(500) {
            val cell = Cell(Random.nextInt(GRID_SIZE), Random.nextInt(GRID_SIZE))
            if (cell !in currentSnake && currentSpecials.none { it.cell == cell } && cell != food) return cell
        }
        return null
    }

    private fun finish(reason: String) {
        isRunning = false
        isGameOver = true
        gameOverReason = reason
        accumulatorMs = 0.0
    }

    fun interpolatedSnake(): List<Pair<Float, Float>> {
        val p = interpolation
        return snake.mapIndexed { index, cell ->
            val previous = previousSnake.getOrNull(index) ?: cell
            var px = previous.x.toFloat()
            var py = previous.y.toFloat()
            val cx = cell.x.toFloat()
            val cy = cell.y.toFloat()
            // Preserve the short direction of travel when wrapping around an edge.
            if (!isWallsEffective) {
                if (cx - px > 1f) px += GRID_SIZE
                else if (cx - px < -1f) px -= GRID_SIZE
                if (cy - py > 1f) py += GRID_SIZE
                else if (cy - py < -1f) py -= GRID_SIZE
            }
            (px + (cx - px) * p) to (py + (cy - py) * p)
        }
    }

    fun toSnapshot(deviceId: String): GameSnapshot {
        writerDeviceId = deviceId
        savedAtEpochMs = System.currentTimeMillis()
        return GameSnapshot(
            sessionId = sessionId,
            mode = mode,
            difficulty = difficulty,
            wallsEnabled = wallsEnabled,
            snake = snake,
            direction = direction,
            nextDirection = nextDirection,
            food = food,
            specials = specials,
            score = score,
            timeRemainingMs = timeRemainingMs,
            revision = revision,
            writerDeviceId = deviceId,
            savedAtEpochMs = savedAtEpochMs
        )
    }

    fun restore(snapshot: GameSnapshot) {
        sessionId = snapshot.sessionId
        mode = snapshot.mode
        difficulty = snapshot.difficulty
        wallsEnabled = snapshot.wallsEnabled
        snake = snapshot.snake.ifEmpty { listOf(Cell(10, 10), Cell(9, 10), Cell(8, 10)) }
        previousSnake = snake
        direction = snapshot.direction
        nextDirection = if (snapshot.nextDirection.isReverseOf(direction)) direction else snapshot.nextDirection
        food = snapshot.food
        specials = snapshot.specials
        score = snapshot.score.coerceAtLeast(0)
        timeRemainingMs = snapshot.timeRemainingMs.coerceAtLeast(0L)
        revision = snapshot.revision
        writerDeviceId = snapshot.writerDeviceId
        savedAtEpochMs = snapshot.savedAtEpochMs
        accumulatorMs = 0.0
        isGameOver = false
        gameOverReason = null
        isRunning = false
    }
}
