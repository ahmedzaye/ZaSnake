package com.ahmedzayed.zasnake.game

/** Monotonic, presentation-independent resume delay. Physics must not tick while it is active. */
class ResumeGraceTimer(private val durationMs: Long = 3_000L) {
    private var deadlineMs: Long? = null

    fun begin(nowMs: Long) { deadlineMs = nowMs + durationMs }
    fun clear() { deadlineMs = null }
    fun secondsRemaining(nowMs: Long): Int {
        val deadline = deadlineMs ?: return 0
        val remaining = deadline - nowMs
        if (remaining <= 0L) { clear(); return 0 }
        return ((remaining + 999L) / 1_000L).toInt().coerceAtLeast(1)
    }
}
