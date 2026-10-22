package com.ahmedzayed.zasnake.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class AchievementRewardsTest {
    @Test
    fun rewardBudgetIs1225ForTwentyAchievements() {
        assertEquals(20, ACHIEVEMENTS.size)
        assertEquals(1225, ACHIEVEMENT_REWARD_BUDGET)
        assertEquals(7, ACHIEVEMENTS.count { it.rewardCoins == 25 })
        assertEquals(9, ACHIEVEMENTS.count { it.rewardCoins == 50 })
        assertEquals(4, ACHIEVEMENTS.count { it.rewardCoins == 150 })
        assertEquals(setOf(25, 50, 150), ACHIEVEMENTS.map { it.rewardCoins }.toSet())
        assertEquals(1225, ACHIEVEMENTS.sumOf { it.rewardCoins })
        assertEquals(ACHIEVEMENTS.size, ACHIEVEMENTS.map { it.id }.toSet().size)
    }

    @Test
    fun completionDoesNotPayAutomaticallyAndMustBeClaimed() {
        val achievement = ACHIEVEMENTS.first { it.id == "first" }
        val stats = PlayerStats(deviceCounters = mapOf("a" to DeviceCounters(food = 1)))
        assertEquals(0L, stats.coins)
        assertTrue(achievement.progress(stats) >= achievement.target)
        val claimed = stats.claimAchievementReward(achievement)
        assertEquals(25L, claimed.coins)
        assertTrue("first" in claimed.claimedAchievementRewards)
    }

    @Test
    fun incompleteAchievementCannotClaim() {
        val stats = PlayerStats(deviceCounters = mapOf("a" to DeviceCounters(food = 10)))
        val achievement = ACHIEVEMENTS.first { it.id == "apples500" }
        assertSame(stats, stats.claimAchievementReward(achievement))
        assertFalse(achievement.id in stats.claimedAchievementRewards)
    }

    @Test
    fun priorUnlockedAchievementCanBeClaimedAfterUpgrade() {
        val stats = PlayerStats(unlockedAchievements = setOf("long30"))
        val achievement = ACHIEVEMENTS.first { it.id == "long30" }
        val claimed = stats.claimAchievementReward(achievement)
        assertEquals(50L, claimed.coins)
    }

    @Test
    fun repeatedClaimAndCloudMergeCountOnlyOnce() {
        val achievement = ACHIEVEMENTS.first { it.id == "games50" }
        val stats = PlayerStats(deviceCounters = mapOf("a" to DeviceCounters(games = 50)))
        val claimOne = stats.claimAchievementReward(achievement)
        val claimTwo = stats.claimAchievementReward(achievement)
        assertSame(claimOne, claimOne.claimAchievementReward(achievement))
        val merged = claimOne.merge(claimTwo)
        assertEquals(50L, merged.coins)
        assertEquals(merged, merged.merge(claimOne))
        assertEquals(merged, claimTwo.merge(claimOne))
    }

    @Test
    fun longTermAchievementIs150AndNeverPaysTwiceAfterMerge() {
        val legendary = ACHIEVEMENTS.first { it.id == "apples1000" }
        val progress = PlayerStats(deviceCounters = mapOf("test" to DeviceCounters(food = 1000)))
        val claimed = progress.claimAchievementReward(legendary)
        assertEquals(150L, claimed.coins)
        assertEquals(claimed, claimed.claimAchievementReward(legendary))
        assertEquals(150L, claimed.merge(claimed).coins)
    }

    @Test
    fun achievementRewardDoesNotChangeRoundRewardOrShopSpend() {
        val stats = PlayerStats(
            deviceCounters = mapOf("a" to DeviceCounters(food = 1)),
            coinSpending = mapOf("shop_skin_cyber" to 120)
        ).rewardCoins("round-123", 150)
        val claimed = stats.claimAchievementReward(ACHIEVEMENTS.first { it.id == "first" })
        assertEquals(150, claimed.coinRewards["round-123"])
        assertEquals(120, claimed.coinSpending["shop_skin_cyber"])
        assertEquals(55L, claimed.coins)
    }
}
