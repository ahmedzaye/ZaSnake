package com.ahmedzayed.zasnake.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlayerNamePolicyTest {
    @Test fun trimsAndCollapsesSpaces() {
        assertEquals("Za Snake", PlayerNamePolicy.normalized("  Za    Snake  "))
        assertEquals("أحمد", PlayerNamePolicy.normalized(" أحمد "))
    }
    @Test fun refusesEmptyAndTooLongNames() {
        assertNull(PlayerNamePolicy.normalized("   "))
        assertNull(PlayerNamePolicy.normalized("x".repeat(21)))
        assertEquals("x".repeat(20), PlayerNamePolicy.normalized("x".repeat(20)))
    }
    @Test fun refusesHiddenControlChars() {
        assertNull(PlayerNamePolicy.normalized("A\nB"))
        assertNull(PlayerNamePolicy.normalized("A\u0000B"))
    }
    @Test fun oldInstallationsGetSafeDefault() {
        assertEquals("Player", PlayerNamePolicy.restored(null))
        assertEquals("Player", PlayerNamePolicy.restored(""))
        assertEquals("Player", AppSettings().playerName)
    }
    @Test fun localNicknameDoesNotChangeOwnership() {
        val original = AppSettings()
        val renamed = original.copy(playerName = "Snake Fan")
        assertEquals(original.ownedBodyKeys, renamed.ownedBodyKeys)
        assertEquals(original.ownedHeadKeys, renamed.ownedHeadKeys)
        assertEquals(original.ownedSkinKeys, renamed.ownedSkinKeys)
    }
}
