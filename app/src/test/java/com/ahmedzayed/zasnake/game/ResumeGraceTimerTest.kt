package com.ahmedzayed.zasnake.game

import org.junit.Assert.assertEquals
import org.junit.Test

class ResumeGraceTimerTest {
    @Test fun countdownUsesMonotonicTimeAndExpiresAfterThreeSeconds() {
        val timer = ResumeGraceTimer()
        timer.begin(1000L)
        assertEquals(3, timer.secondsRemaining(1000L))
        assertEquals(3, timer.secondsRemaining(1999L))
        assertEquals(2, timer.secondsRemaining(2000L))
        assertEquals(1, timer.secondsRemaining(3999L))
        assertEquals(0, timer.secondsRemaining(4000L))
        assertEquals(0, timer.secondsRemaining(5000L))
    }

    @Test fun intentionalInputCanCancelPendingCountdown() {
        val timer = ResumeGraceTimer()
        timer.begin(4_000L)
        timer.clear()
        assertEquals(0, timer.secondsRemaining(4_100L))
    }

    @Test fun newResumeStartsFreshThreeSecondWindow() {
        val timer = ResumeGraceTimer()
        timer.begin(2_000L)
        timer.begin(3_000L)
        assertEquals(3, timer.secondsRemaining(3_000L))
        assertEquals(1, timer.secondsRemaining(5_999L))
        assertEquals(0, timer.secondsRemaining(6_000L))
    }
}
