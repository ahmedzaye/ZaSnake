package com.ahmedzayed.zasnake.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioSettingsTest {
    @Test fun defaultAudioSettingsPreserveOldSoundAndHaptics() {
        val settings = AppSettings()
        assertTrue(settings.soundEnabled)
        assertTrue(settings.hapticsEnabled)
        assertEquals(70, settings.sfxVolume)
        assertTrue(settings.musicEnabled)
        assertEquals(25, settings.musicVolume)
    }

    @Test fun separateMusicAndEffectsSettingsDoNotChangeControlStyle() {
        val original = AppSettings(controlStyle = ControlStyle.D_PAD, joystickLeftLandscape = true)
        val changed = original.copy(soundEnabled = false, sfxVolume = 10, musicEnabled = true, musicVolume = 40)
        assertEquals(ControlStyle.D_PAD, changed.controlStyle)
        assertTrue(changed.joystickLeftLandscape)
        assertEquals(10, changed.sfxVolume)
        assertEquals(40, changed.musicVolume)
        assertTrue(changed.musicEnabled)
    }
}
