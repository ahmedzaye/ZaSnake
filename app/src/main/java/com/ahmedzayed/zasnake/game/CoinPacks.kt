package com.ahmedzayed.zasnake.game

/**
 * Public Play product identifiers and non-monetary entitlement amounts.
 * Real-money prices/currencies must always come from Google Play Billing,
 * never from this file or another hard-coded UI value.
 *
 * This catalog only backs a non-purchasable preview until verified Billing
 * and server-side purchase fulfillment are added in a later stage.
 */
data class CoinPack(val productId: String, val coinAmount: Int)

val COIN_PACKAGES: List<CoinPack> = listOf(
    CoinPack("zasnake_coins_1000", 1_000),
    CoinPack("zasnake_coins_2000", 2_000),
    CoinPack("zasnake_coins_4000", 4_000)
)
