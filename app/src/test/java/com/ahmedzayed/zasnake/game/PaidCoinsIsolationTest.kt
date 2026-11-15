package com.ahmedzayed.zasnake.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/** Regression guard: remote paid credits cannot unlock locally without server spend. */
class PaidCoinsIsolationTest {
    @Test fun paidGrantsAloneCannotPurchaseLocalItems() {
        val paidOnly = PlayerStats(
            verifiedPurchaseGrants = mapOf("a".repeat(64) to 4000)
        )
        assertEquals(0L, paidOnly.coins)
        assertSame(paidOnly, paidOnly.purchaseTheme(GameTheme.OCEAN))
        assertSame(paidOnly, paidOnly.purchaseSkin(SnakeSkin.EGYPT))
        assertTrue(paidOnly.coinSpending.isEmpty())
    }

    @Test fun freeCoinsStillWorkWhenPaidGrantsArePresent() {
        val free = PlayerStats().rewardCoins("round-1", 100)
        val withPaid = free.copy(verifiedPurchaseGrants = mapOf("b".repeat(64) to 2000))
        assertEquals(100L, withPaid.coins)
        val purchased = withPaid.purchaseTheme(GameTheme.OCEAN)
        assertEquals(20L, purchased.coins)
        assertEquals(80, purchased.coinSpending["shop_theme_ocean"])
        assertTrue("ocean" in purchased.purchasedThemeKeys)
    }

    @Test fun refreshingOrRemovingPaidGrantsNeverChangesFreeBalance() {
        val base = PlayerStats().rewardCoins("round-2", 175)
        val paid = base.copy(verifiedPurchaseGrants = mapOf("c".repeat(64) to 1000))
        assertEquals(base.coins, paid.coins)
        assertEquals(base.coins, paid.copy(verifiedPurchaseGrants = emptyMap()).coins)
        assertEquals(175L, base.merge(paid).coins)
    }
}
