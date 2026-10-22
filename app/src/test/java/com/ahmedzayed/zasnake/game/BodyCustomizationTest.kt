package com.ahmedzayed.zasnake.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class BodyCustomizationTest {
    @Test fun bodyPricesAndDefaults() {
        assertEquals(listOf(0, 250, 200, 300, 350), SnakeBodyType.entries.map { it.priceCoins })
        assertEquals(setOf("segmented"), FREE_BODY_KEYS)
        assertEquals(SnakeBodyType.SEGMENTED, AppSettings().bodyType)
        assertEquals(FREE_BODY_KEYS, AppSettings().ownedBodyKeys)
        assertEquals(SnakeBodyType.SEGMENTED, SnakeBodyType.fromKey("invalid"))
        assertTrue(SnakeBodyType.SMOOTH.key !in AppSettings().ownedBodyKeys)
        assertTrue(SnakeBodyType.RETRO_PIXEL.key !in AppSettings().ownedBodyKeys)
        assertTrue(SnakeBodyType.ARMORED.key !in AppSettings().ownedBodyKeys)
        assertTrue(SnakeBodyType.CRYSTAL.key !in AppSettings().ownedBodyKeys)
        assertEquals(SnakeBodyType.RETRO_PIXEL, SnakeBodyType.fromKey("retro_pixel"))
        assertEquals(SnakeBodyType.ARMORED, SnakeBodyType.fromKey("armored"))
        assertEquals(SnakeBodyType.CRYSTAL, SnakeBodyType.fromKey("crystal"))
    }

    @Test fun purchaseRequiresEnoughCoinsAndNeverChargesForSegmented() {
        val insufficient = PlayerStats().rewardCoins("round", 249)
        assertSame(insufficient, insufficient.purchaseBody(SnakeBodyType.SMOOTH))
        assertSame(insufficient, insufficient.purchaseBody(SnakeBodyType.SEGMENTED))
        assertEquals(249L, insufficient.coins)
    }

    @Test fun smoothPurchaseIsIndependentFromSkinsHeadsAndThemes() {
        val original = PlayerStats().rewardCoins("round", 800)
            .purchaseSkin(SnakeSkin.UAE).purchaseHead(SnakeHeadStyle.CUTE)
        val purchased = original.purchaseBody(SnakeBodyType.SMOOTH)
        assertEquals(original.coins - 250L, purchased.coins)
        assertEquals(original.purchasedSkinKeys, purchased.purchasedSkinKeys)
        assertEquals(original.purchasedHeadKeys, purchased.purchasedHeadKeys)
        assertEquals(original.purchasedThemeKeys, purchased.purchasedThemeKeys)
        assertTrue("smooth" in purchased.purchasedBodyKeys)
        assertEquals(250, purchased.coinSpending["shop_body_smooth"])
        assertSame(purchased, purchased.purchaseBody(SnakeBodyType.SMOOTH))
    }

    @Test fun sameBodyPurchaseOnTwoDevicesMergesOnce() {
        val base = PlayerStats().rewardCoins("round", 500)
        val one = base.purchaseBody(SnakeBodyType.SMOOTH)
        val other = base.purchaseBody(SnakeBodyType.SMOOTH)
        val merged = one.merge(other)
        assertEquals(250L, merged.coins)
        assertEquals(1, merged.coinSpending.size)
        assertEquals(merged, merged.merge(merged))
    }

    @Test fun transactionRestoresBodyOwnershipWhenUnlockSetMissing() {
        val saved = PlayerStats(
            coinRewards = mapOf("round" to 500),
            coinSpending = mapOf("shop_body_smooth" to 250)
        )
        assertTrue("smooth" in saved.purchasedBodyKeys)
        assertSame(saved, saved.purchaseBody(SnakeBodyType.SMOOTH))
        assertFalse("smooth" in FREE_BODY_KEYS)
    }
    @Test fun newBodyStylesHaveIndependentPricesAndTransactionIds() {
        val initial = PlayerStats().rewardCoins("round", 1_200)
        val retro = initial.purchaseBody(SnakeBodyType.RETRO_PIXEL)
        val armored = retro.purchaseBody(SnakeBodyType.ARMORED)
        val crystal = armored.purchaseBody(SnakeBodyType.CRYSTAL)
        assertEquals(350L, crystal.coins)
        assertEquals(200, crystal.coinSpending["shop_body_retro_pixel"])
        assertEquals(300, crystal.coinSpending["shop_body_armored"])
        assertEquals(350, crystal.coinSpending["shop_body_crystal"])
        assertEquals(setOf("retro_pixel", "armored", "crystal"), crystal.purchasedBodyKeys)
        for (body in listOf(SnakeBodyType.RETRO_PIXEL, SnakeBodyType.ARMORED, SnakeBodyType.CRYSTAL)) {
            assertSame(crystal, crystal.purchaseBody(body))
        }
        assertEquals(initial.purchasedSkinKeys, crystal.purchasedSkinKeys)
        assertEquals(initial.purchasedHeadKeys, crystal.purchasedHeadKeys)
        assertEquals(initial.purchasedThemeKeys, crystal.purchasedThemeKeys)
    }

    @Test fun newBodyPurchaseRequiresCoinsAndMergesWithoutDoubleCharge() {
        val insufficient = PlayerStats().rewardCoins("round", 199)
        for (body in listOf(SnakeBodyType.RETRO_PIXEL, SnakeBodyType.ARMORED, SnakeBodyType.CRYSTAL)) {
            assertSame(insufficient, insufficient.purchaseBody(body))
        }
        for (body in listOf(SnakeBodyType.RETRO_PIXEL, SnakeBodyType.ARMORED, SnakeBodyType.CRYSTAL)) {
            val base = PlayerStats().rewardCoins("round", 600)
            val left = base.purchaseBody(body)
            val right = base.purchaseBody(body)
            val merged = left.merge(right)
            assertEquals(600L - body.priceCoins, merged.coins)
            assertEquals(1, merged.coinSpending.size)
            assertTrue(body.key in merged.purchasedBodyKeys)
            assertSame(merged, merged.purchaseBody(body))
        }
    }

    @Test fun cloudPurchaseTransactionRestoresNewBodyOwnership() {
        for (body in listOf(SnakeBodyType.RETRO_PIXEL, SnakeBodyType.ARMORED, SnakeBodyType.CRYSTAL)) {
            val restored = PlayerStats(
                coinRewards = mapOf("round" to 500),
                coinSpending = mapOf("shop_body_${body.key}" to body.priceCoins)
            )
            assertTrue(body.key in restored.purchasedBodyKeys)
            assertSame(restored, restored.purchaseBody(body))
        }
    }

}
