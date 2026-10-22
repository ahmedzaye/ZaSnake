package com.ahmedzayed.zasnake.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class ArabSkinsTest {
    @Test fun allSixArabCollectionSkinsCostExactly180Coins() {
        assertEquals(6, ARAB_COLLECTION.size)
        assertEquals(6, ARAB_COLLECTION.map { it.key }.distinct().size)
        assertTrue(ARAB_COLLECTION.all { it.priceCoins == 180 && !it.isFree })
        assertEquals(1080, ARAB_COLLECTION.sumOf { it.priceCoins })
        assertEquals(setOf("green", "purple", "yellow"), FREE_SKIN_KEYS)
        assertEquals(12, SnakeSkin.entries.size)
        assertEquals(20, ACHIEVEMENTS.size)
        assertEquals(1225, ACHIEVEMENT_REWARD_BUDGET)
    }

    @Test fun egyptIsPaidOnFreshInstallButOwnedOnExistingSaves() {
        assertFalse("egypt" in restoredOwnedSkinKeys(null, null))
        assertTrue("egypt" in restoredOwnedSkinKeys("green,purple,yellow,egypt", "green"))
        assertTrue("egypt" in restoredOwnedSkinKeys(null, "egypt"))
        assertTrue("egypt" in restoredOwnedSkinKeys("green,purple,yellow", "egypt"))
        assertEquals(FREE_SKIN_KEYS, AppSettings().ownedSkinKeys)
    }

    @Test fun cannotPurchaseFreeSkinsOrBuyWithoutEnoughCoins() {
        val base = PlayerStats().rewardCoins("round", 179)
        assertSame(base, base.purchaseSkin(SnakeSkin.GREEN))
        assertSame(base, base.purchaseSkin(SnakeSkin.EGYPT))
        assertSame(base, base.purchaseSkin(SnakeSkin.UAE))
    }

    @Test fun purchaseChargesOnceAndUnlocksOneSkin() {
        val base = PlayerStats().rewardCoins("round", 500)
        val bought = base.purchaseSkin(SnakeSkin.UAE)
        assertEquals(320L, bought.coins)
        assertTrue("uae" in bought.purchasedSkinKeys)
        assertEquals(180, bought.coinSpending["shop_skin_uae"])
        assertSame(bought, bought.purchaseSkin(SnakeSkin.UAE))
        assertEquals(320L, bought.purchaseSkin(SnakeSkin.UAE).coins)
    }

    @Test fun parallelPurchasesMergeWithoutDoubleCharges() {
        val base = PlayerStats().rewardCoins("round", 600)
        val phoneA = base.purchaseSkin(SnakeSkin.JORDAN)
        val phoneB = base.purchaseSkin(SnakeSkin.JORDAN)
        val merged = phoneA.merge(phoneB)
        assertEquals(420L, merged.coins)
        assertEquals(1, merged.coinSpending.size)
        assertEquals(180, merged.coinSpending["shop_skin_jordan"])
        assertEquals(merged, merged.merge(merged))
        assertSame(merged, merged.purchaseSkin(SnakeSkin.JORDAN))
    }

    @Test fun cloudTransactionRestoresOwnershipEvenWithoutUnlockFlag() {
        val remote = PlayerStats(
            coinRewards = mapOf("round" to 500),
            coinSpending = mapOf("shop_skin_palestine" to 180)
        )
        assertTrue("palestine" in remote.purchasedSkinKeys)
        assertSame(remote, remote.purchaseSkin(SnakeSkin.PALESTINE))
        assertEquals(320L, remote.coins)
    }

    @Test fun legacyEgyptOwnershipSurvivesSyncWithoutCharging() {
        val legacy = PlayerStats(unlockedSkins = setOf("egypt"))
        val other = PlayerStats().rewardCoins("round", 200)
        val merged = legacy.merge(other)
        assertTrue("egypt" in merged.purchasedSkinKeys)
        assertFalse("shop_skin_egypt" in merged.coinSpending)
        assertSame(merged, merged.purchaseSkin(SnakeSkin.EGYPT))
    }
}
