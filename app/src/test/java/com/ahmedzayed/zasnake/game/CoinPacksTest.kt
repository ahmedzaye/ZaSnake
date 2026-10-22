package com.ahmedzayed.zasnake.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CoinPacksTest {
    @Test fun packIdsAndEntitlementsAreStable() {
        assertEquals(listOf(1_000, 2_000, 4_000), COIN_PACKAGES.map { it.coinAmount })
        assertEquals(
            listOf("zasnake_coins_1000", "zasnake_coins_2000", "zasnake_coins_4000"),
            COIN_PACKAGES.map { it.productId }
        )
        assertEquals(COIN_PACKAGES.size, COIN_PACKAGES.map { it.productId }.toSet().size)
        assertTrue(COIN_PACKAGES.all { it.coinAmount > 0 })
    }
}
