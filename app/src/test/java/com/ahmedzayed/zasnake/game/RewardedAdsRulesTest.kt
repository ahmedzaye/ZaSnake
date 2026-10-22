package com.ahmedzayed.zasnake.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RewardedAdsRulesTest {
    @Test fun collisionWaitsForReviveDecisionAndRestoresSafely() {
        val engine = SnakeEngine()
        engine.startNew(GameMode.CLASSIC, Difficulty.EASY, true, "device")
        val start = engine.toSnapshot("device").copy(
            snake = listOf(Cell(19, 8), Cell(18, 8), Cell(17, 8)),
            direction = Direction.RIGHT, nextDirection = Direction.RIGHT,
            food = Cell(2, 2)
        )
        engine.restore(start)
        engine.resume()
        engine.update(200L)
        assertTrue(engine.canRevive)
        assertTrue(engine.pendingCollision)
        assertFalse(engine.reviveUsed)
        val checkpoint = engine.toSnapshot("device")
        val restored = SnakeEngine()
        restored.restore(checkpoint)
        assertTrue(restored.canRevive)
        assertTrue(restored.reviveFromCollision())
        assertEquals(5_000L, restored.shieldRemainingMs)
        assertFalse(restored.reviveFromCollision())
        restored.update(200L)
        assertEquals(Cell(0, 8), restored.snake.first())
        assertFalse(restored.isGameOver)
        restored.pause()
        val frozen = restored.shieldRemainingMs
        restored.update(200L)
        assertEquals(frozen, restored.shieldRemainingMs)
    }

    @Test fun protectedSnakeCanPassItsOwnBody() {
        val engine = SnakeEngine()
        engine.restore(GameSnapshot(
            snake = listOf(Cell(5, 5), Cell(6, 5), Cell(7, 5)),
            direction = Direction.RIGHT, nextDirection = Direction.RIGHT,
            food = Cell(12, 12), reviveUsed = true, shieldRemainingMs = 3_000L
        ))
        engine.resume()
        engine.update(200L)
        assertFalse(engine.isGameOver)
        assertEquals(Cell(6, 5), engine.snake.first())
    }

    @Test fun afterReviveNextCollisionIsFinal() {
        val engine = SnakeEngine()
        engine.restore(GameSnapshot(
            snake = listOf(Cell(19, 5), Cell(18, 5), Cell(17, 5)),
            direction = Direction.RIGHT, nextDirection = Direction.RIGHT,
            food = Cell(12, 12), reviveUsed = true
        ))
        engine.resume()
        engine.update(200L)
        assertTrue(engine.isGameOver)
        assertFalse(engine.pendingCollision)
        assertFalse(engine.canRevive)
    }

    @Test fun declineFinishesWithoutUsingRevive() {
        val engine = SnakeEngine()
        engine.restore(GameSnapshot(
            snake = listOf(Cell(19, 5), Cell(18, 5), Cell(17, 5)),
            direction = Direction.RIGHT, nextDirection = Direction.RIGHT,
            food = Cell(12, 12)
        ))
        engine.resume()
        engine.update(200L)
        assertTrue(engine.finalizePendingCollision())
        assertFalse(engine.finalizePendingCollision())
        assertFalse(engine.canRevive)
    }

    @Test fun usingReviveDoesNotConsumeDoubleCoinsEntitlement() {
        val engine = SnakeEngine()
        engine.restore(GameSnapshot(sessionId = "revived-then-finished", reviveUsed = true))
        assertTrue(engine.reviveUsed)
        assertFalse(engine.canRevive)
        val finished = PlayerStats().rewardCoins(engine.sessionId, 60)
        val doubled = finished.doubleRoundCoins(engine.sessionId)
        assertEquals(120L, doubled.coins)
        assertEquals(doubled, doubled.doubleRoundCoins(engine.sessionId))
    }

    @Test fun doubleRewardIsIdempotentAcrossCloudMerge() {
        val original = PlayerStats().rewardCoins("session-one", 84)
        assertEquals(original, original.doubleRoundCoins("not-ended"))
        val doubled = original.doubleRoundCoins("session-one")
        assertEquals(168L, doubled.coins)
        assertEquals(doubled, doubled.doubleRoundCoins("session-one"))
        assertEquals(doubled, doubled.merge(doubled))
        assertEquals(168L, doubled.merge(original).coins)
        assertEquals(168L, original.merge(doubled).coins)
    }
}
