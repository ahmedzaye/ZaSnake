package com.ahmedzayed.zasnake.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.ArrayDeque

class SnakeEngineTest {
    @Test
    fun controlStyleDefaultsToFloatingAndHandlesUnknownSavedValues() {
        assertEquals(ControlStyle.FLOATING_JOYSTICK, AppSettings().controlStyle)
        assertEquals(ControlStyle.FLOATING_JOYSTICK, ControlStyle.fromKey(null))
        assertEquals(ControlStyle.FLOATING_JOYSTICK, ControlStyle.fromKey("unexpected"))
        assertEquals(ControlStyle.D_PAD, ControlStyle.fromKey("d_pad"))
    }

    @Test
    fun applePointsScaleWithDifficulty() {
        assertEquals(1, Difficulty.EASY.pointsPerApple)
        assertEquals(2, Difficulty.MEDIUM.pointsPerApple)
        assertEquals(4, Difficulty.HARD.pointsPerApple)
    }

    @Test
    fun classicEndsWhenHeadHitsEnabledWall() {
        val engine = SnakeEngine()
        engine.startNew(GameMode.CLASSIC, Difficulty.EASY, newWallsEnabled = true, deviceId = "test")

        repeat(10) { engine.update(200L) }

        assertTrue(engine.isGameOver)
        assertEquals("hit", engine.gameOverReason)
    }

    @Test
    fun zenWrapsAtBoardEdgeAndDoesNotEndFromCollision() {
        val engine = SnakeEngine()
        engine.startNew(GameMode.ZEN, Difficulty.EASY, newWallsEnabled = true, deviceId = "test")

        repeat(25) { engine.update(200L) }

        assertFalse(engine.isGameOver)
        assertEquals(GameMode.ZEN, engine.mode)
    }

    @Test
    fun timeAttackRegularFoodAddsTwoSeconds() {
        val engine = SnakeEngine()
        engine.startNew(GameMode.TIME_ATTACK, Difficulty.EASY, newWallsEnabled = false, deviceId = "test")
        var updates = 0
        while (engine.score == 0 && !engine.isGameOver && updates < 100) {
            engine.requestDirection(nextStepTowardFood(engine))
            engine.update(200L)
            updates++
        }

        assertFalse("The route should reach the first regular apple", engine.isGameOver)
        assertEquals(Difficulty.EASY.pointsPerApple, engine.score)
        assertEquals(SnakeEngine.START_TIME_MS - updates * 200L + SnakeEngine.TIME_BONUS_MS, engine.timeRemainingMs)
    }

    @Test
    fun foodVisualCueUsesTheActualImpactCellWithoutChangingScore() {
        val engine = SnakeEngine()
        engine.startNew(GameMode.TIME_ATTACK, Difficulty.EASY, newWallsEnabled = false, deviceId = "test")
        var foodEvent: EngineEvent? = null
        repeat(100) {
            if (foodEvent == null && !engine.isGameOver) {
                engine.requestDirection(nextStepTowardFood(engine))
                foodEvent = engine.update(200L).firstOrNull { it.type == EngineEvent.Type.FOOD }
            }
        }
        assertTrue("Expected to reach a regular apple", foodEvent != null)
        assertEquals(engine.snake.first(), foodEvent?.cell)
        assertEquals(1, engine.score)
    }

    @Test
    fun timeAttackClockPausesWithTheRound() {
        val engine = SnakeEngine()
        engine.startNew(GameMode.TIME_ATTACK, Difficulty.HARD, newWallsEnabled = false, deviceId = "test")
        engine.update(250L)
        val frozenAt = engine.timeRemainingMs
        engine.pause()
        engine.update(250L)

        assertEquals(SnakeEngine.START_TIME_MS - 250L, frozenAt)
        assertEquals(frozenAt, engine.timeRemainingMs)
    }

    @Test
    fun deviceCounterMergeIsIdempotent() {
        val counters = DeviceCounters(games = 4, food = 12, playMs = 30_000, golden = 2, scissors = 1)
        val merged = counters.merge(counters)
        assertEquals(counters, merged)
    }

    @Test
    fun playerStatsMergeCombinesDifferentDevicesWithoutDoubleCountingSameDevice() {
        val first = PlayerStats(deviceCounters = mapOf("device-a" to DeviceCounters(games = 2, food = 5)), bestScore = 20)
        val second = PlayerStats(deviceCounters = mapOf("device-a" to DeviceCounters(games = 2, food = 5), "device-b" to DeviceCounters(games = 3, food = 7)), bestScore = 18)
        val merged = first.merge(second)
        assertEquals(5L, merged.games)
        assertEquals(12L, merged.food)
        assertEquals(20, merged.bestScore)
    }

    @Test
    fun coinRewardIsGrantedOnlyOnceForTheSameRound() {
        val firstGrant = PlayerStats().rewardCoins("round-123", 12)
        val repeatedGrant = firstGrant.rewardCoins("round-123", 12)

        assertEquals(12L, firstGrant.coins)
        assertEquals(firstGrant, repeatedGrant)
    }

    @Test
    fun mergingTheSameRoundRewardFromTwoDevicesDoesNotDoubleCoins() {
        val local = PlayerStats().rewardCoins("shared-round", 9)
        val cloud = PlayerStats().rewardCoins("shared-round", 9)
            .rewardCoins("other-round", 4)

        val merged = local.merge(cloud)

        assertEquals(13L, merged.coins)
        assertEquals(merged, merged.merge(local))
    }

    @Test
    fun snapshotRestoreRetainsModeScoreAndSettings() {
        val engine = SnakeEngine()
        engine.startNew(GameMode.TIME_ATTACK, Difficulty.HARD, newWallsEnabled = false, deviceId = "test-device")
        engine.update(100L)
        val snapshot = engine.toSnapshot("test-device")

        val restored = SnakeEngine()
        restored.restore(snapshot)

        assertEquals(GameMode.TIME_ATTACK, restored.mode)
        assertEquals(Difficulty.HARD, restored.difficulty)
        assertFalse(restored.wallsEnabled)
        assertEquals(snapshot.score, restored.score)
        assertEquals(snapshot.timeRemainingMs, restored.timeRemainingMs)
        assertEquals(snapshot.snake, restored.snake)
        assertFalse(restored.isRunning)
    }

    private fun nextStepTowardFood(engine: SnakeEngine): Direction {
        data class Node(val cell: Cell, val first: Direction?)
        val queue = ArrayDeque<Node>()
        val visited = mutableSetOf<Cell>()
        val blocked = engine.snake.drop(1).toSet() + engine.specials.map { it.cell }
        queue.add(Node(engine.snake.first(), null))
        visited += engine.snake.first()
        val directions = listOf(Direction.UP, Direction.RIGHT, Direction.DOWN, Direction.LEFT)
        while (queue.isNotEmpty()) {
            val node = queue.removeFirst()
            for (direction in directions) {
                val next = Cell(
                    (node.cell.x + direction.dx + SnakeEngine.GRID_SIZE) % SnakeEngine.GRID_SIZE,
                    (node.cell.y + direction.dy + SnakeEngine.GRID_SIZE) % SnakeEngine.GRID_SIZE
                )
                if (next in blocked || !visited.add(next)) continue
                val first = node.first ?: direction
                if (next == engine.food) return first
                queue.add(Node(next, first))
            }
        }
        return engine.direction
    }
}
