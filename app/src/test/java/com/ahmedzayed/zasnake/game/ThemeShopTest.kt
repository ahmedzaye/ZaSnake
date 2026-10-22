package com.ahmedzayed.zasnake.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeShopTest {
    @Test fun freeThemesStayFreeAndPremiumPricesAreStable() {
        assertEquals(setOf("night", "light", "emerald"), FREE_THEME_KEYS)
        val premium = GameTheme.entries.filterNot { it.isFree }
        assertEquals(5, premium.size)
        assertEquals(600, premium.sumOf { it.priceCoins })
        assertEquals(8, GameTheme.entries.size)
    }

    @Test fun cannotBuyLockedThemeWithInsufficientCoins() {
        val stats = PlayerStats().rewardCoins("round", 79)
        assertSame(stats, stats.purchaseTheme(GameTheme.OCEAN))
        assertSame(stats, stats.purchaseTheme(GameTheme.NIGHT))
        assertEquals(79L, stats.coins)
    }

    @Test fun purchasesSpendOnceAndSurviveMerges() {
        val base = PlayerStats().rewardCoins("round", 500)
        val ocean = base.purchaseTheme(GameTheme.OCEAN)
        assertEquals(420L, ocean.coins)
        assertTrue("ocean" in ocean.purchasedThemeKeys)
        assertSame(ocean, ocean.purchaseTheme(GameTheme.OCEAN))
        val cloud = base.purchaseTheme(GameTheme.OCEAN)
        val merged = ocean.merge(cloud)
        assertEquals(420L, merged.coins)
        assertEquals(80, merged.coinSpending["shop_theme_ocean"])
        assertEquals(merged, merged.merge(cloud))
        assertEquals(merged, cloud.merge(ocean))
    }

    @Test fun differentThemePurchasesAreSeparate() {
        val initial = PlayerStats().rewardCoins("round", 700)
        val ocean = initial.purchaseTheme(GameTheme.OCEAN)
        val desert = ocean.purchaseTheme(GameTheme.DESERT)
        assertEquals(480L, desert.coins)
        assertEquals(setOf("ocean", "desert"), desert.purchasedThemeKeys)
        assertFalse("royal" in desert.purchasedThemeKeys)
    }

    @Test fun transactionRestoresThemeIfExplicitOwnershipIsMissing() {
        val legacy = PlayerStats(coinRewards = mapOf("round" to 200), coinSpending = mapOf("shop_theme_ocean" to 80))
        assertTrue("ocean" in legacy.purchasedThemeKeys)
        assertSame(legacy, legacy.purchaseTheme(GameTheme.OCEAN))
        assertEquals(120L, legacy.coins)
    }
}
