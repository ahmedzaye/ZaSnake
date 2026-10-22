package com.ahmedzayed.zasnake.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DailyRewardsTest {
    @Test fun rewardsNeedExplicitClaimAndNeverPayTwice() {
        val day = "2026-10-18"
        val progress = PlayerStats().recordDailyActivity(day, "device-a") {
            it.copy(apples = 20, completedGames = 3, bestRoundScore = 30)
        }
        assertEquals(0L, progress.coins)
        val once = progress.claimDailyChallenge(day, DailyChallenge.APPLES)
        assertEquals(15L, once.coins)
        assertEquals(once, once.claimDailyChallenge(day, DailyChallenge.APPLES))
        val merged = once.merge(once.claimDailyChallenge(day, DailyChallenge.APPLES))
        assertEquals(15L, merged.coins)
        assertEquals(45L, DailyChallenge.entries.fold(progress) { stats, challenge -> stats.claimDailyChallenge(day, challenge) }.coins)
    }

    @Test fun dailyProgressIsolatedAndMergedPerDeviceWithoutDuplication() {
        val a = PlayerStats().recordDailyActivity("2026-10-18", "a") { it.copy(apples = 12) }
        val b = PlayerStats().recordDailyActivity("2026-10-18", "b") { it.copy(apples = 8) }
        val merged = a.merge(b)
        assertEquals(20L, merged.dailyProgress("2026-10-18").apples)
        assertEquals(20L, merged.merge(a).merge(b).dailyProgress("2026-10-18").apples)
        assertEquals(0L, merged.dailyProgress("2026-10-19").apples)
        assertEquals(15L, merged.claimDailyChallenge("2026-10-18", DailyChallenge.APPLES).coins)
    }

    @Test fun consecutiveDaysCycleAndGapResets() {
        var stats = PlayerStats()
        val dates = listOf("2026-10-18", "2026-10-19", "2026-10-20", "2026-10-21", "2026-10-22", "2026-10-23", "2026-10-24")
        dates.forEachIndexed { i, date ->
            stats = stats.claimDailyLogin(date)
            assertEquals(i + 1, stats.claimedDailyLogins[date])
        }
        assertEquals(listOf(10, 15, 20, 25, 30, 40, 60), DAILY_LOGIN_COIN_SEQUENCE)
        assertEquals(200L, stats.coins)
        stats = stats.claimDailyLogin("2026-10-25")
        assertEquals(1, stats.claimedDailyLogins["2026-10-25"])
        stats = stats.claimDailyLogin("2026-10-27")
        assertEquals(1, stats.claimedDailyLogins["2026-10-27"])
        assertEquals(220L, stats.coins) // Day 8 after complete cycle and day after missed claim each pay 10.
        assertEquals(stats, stats.claimDailyLogin("2026-10-27"))
    }

    @Test fun sameDayLoginMergedOnlyOnceAndBackdatingBlocked() {
        val a = PlayerStats().claimDailyLogin("2026-10-18")
        val b = PlayerStats().claimDailyLogin("2026-10-18")
        val merged = a.merge(b)
        assertEquals(10L, merged.coins)
        assertEquals(merged, merged.claimDailyLogin("2026-10-18"))
        assertEquals(merged.touchDailyDate("2026-10-19"), merged.touchDailyDate("2026-10-19").claimDailyLogin("2026-10-18"))
        assertTrue("2026-10-18" in merged.claimedDailyLogins)
        assertFalse("2026-10-19" in merged.claimedDailyLogins)
    }

    @Test fun legacyStatsAndAchievementsRemainUnchanged() {
        assertEquals(20, ACHIEVEMENTS.size)
        assertEquals(1225, ACHIEVEMENT_REWARD_BUDGET)
        val previous = PlayerStats(coinRewards = mapOf("round1" to 20),
            claimedAchievementRewards = mapOf("first" to 5), coinSpending = mapOf("shop" to 10))
        assertEquals(15L, previous.coins)
        assertEquals(25L, previous.claimDailyLogin("2026-10-18").coins)
    }
}
