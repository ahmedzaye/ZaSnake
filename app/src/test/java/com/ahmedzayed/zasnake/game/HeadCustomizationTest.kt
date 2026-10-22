package com.ahmedzayed.zasnake.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class HeadCustomizationTest {
    @Test fun canonicalPricesAndDefaults() {
        assertEquals(listOf(0, 120, 120, 180, 220), SnakeHeadStyle.entries.map { it.priceCoins })
        assertEquals(setOf("classic"), FREE_HEAD_KEYS)
        assertEquals(SnakeHeadStyle.CLASSIC, AppSettings().headStyle)
        assertEquals(SnakeBodyType.SEGMENTED, AppSettings().bodyType)
        assertEquals(FREE_HEAD_KEYS, AppSettings().ownedHeadKeys)
        assertEquals(SnakeHeadStyle.CLASSIC, SnakeHeadStyle.fromKey("unrecognized"))
        assertEquals(SnakeBodyType.SEGMENTED, SnakeBodyType.fromKey("unknown"))
    }

    @Test fun purchaseRequiresFundsAndCannotBuyFreeHead() {
        val stats = PlayerStats().rewardCoins("test", 119)
        assertSame(stats, stats.purchaseHead(SnakeHeadStyle.CLASSIC))
        assertSame(stats, stats.purchaseHead(SnakeHeadStyle.SHARP))
    }

    @Test fun buyingHeadDoesNotChangeSkinOrThemeOrOtherEntitlements() {
        val start = PlayerStats().rewardCoins("test", 500).purchaseSkin(SnakeSkin.EGYPT)
        val bought = start.purchaseHead(SnakeHeadStyle.CUTE)
        assertEquals(start.coins - 120, bought.coins)
        assertEquals(start.purchasedSkinKeys, bought.purchasedSkinKeys)
        assertEquals(start.purchasedThemeKeys, bought.purchasedThemeKeys)
        assertEquals(setOf("cute"), bought.purchasedHeadKeys)
        assertEquals(120, bought.coinSpending["shop_head_cute"])
        assertSame(bought, bought.purchaseHead(SnakeHeadStyle.CUTE))
    }

    @Test fun sameHeadPurchasedFromTwoDevicesIsCountedOnce() {
        val start = PlayerStats().rewardCoins("test", 600)
        val a = start.purchaseHead(SnakeHeadStyle.ROYAL)
        val b = start.purchaseHead(SnakeHeadStyle.ROYAL)
        val merged = a.merge(b)
        assertEquals(420L, merged.coins)
        assertEquals(1, merged.coinSpending.size)
        assertEquals(setOf("royal"), merged.purchasedHeadKeys)
        assertSame(merged, merged.purchaseHead(SnakeHeadStyle.ROYAL))
        assertEquals(merged, merged.merge(merged))
    }

    @Test fun transactionRestoresOwnershipIfUnlockFlagIsMissing() {
        val saved = PlayerStats(
            coinRewards = mapOf("round" to 500),
            coinSpending = mapOf("shop_head_cyber" to 220)
        )
        assertTrue("cyber" in saved.purchasedHeadKeys)
        assertSame(saved, saved.purchaseHead(SnakeHeadStyle.CYBER))
        assertEquals(280L, saved.coins)
        assertFalse("cyber" in FREE_HEAD_KEYS)
    }
}
