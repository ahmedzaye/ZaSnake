package com.ahmedzayed.zasnake.age

import android.content.Context

/** On-device gate state. No DOB, full age, account identifier, or analytics is saved here. */
class AgeGateRepository(context: Context) {
    private val preferences = context.getSharedPreferences("za_snake_age_gate_v1", Context.MODE_PRIVATE)

    fun read(): AgeDecision? {
        val band = preferences.getString("age_band", null)?.let { saved ->
            AgeBand.entries.firstOrNull { it.name == saved }
        } ?: return null
        if (band != AgeBand.ADULT && !preferences.contains("next_age_transition")) return null
        val previous = AgeDecision(band,
            if (band == AgeBand.ADULT) null else preferences.getLong("next_age_transition", 0L))
        if (band != AgeBand.ADULT && previous.nextTransitionEpochMillis == 0L) return null
        val updated = AgePolicy.refresh(previous)
        if (updated != previous && !persist(updated)) return null
        return updated
    }

    /** Accept the screen only after a successful synchronous write; never bypass on storage error. */
    fun submit(day: Int, month: Int, year: Int): AgeDecision? {
        val choice = AgePolicy.classify(day, month, year) ?: return null
        return choice.takeIf { persist(it) }
    }

    private fun persist(decision: AgeDecision): Boolean {
        val edit = preferences.edit().putString("age_band", decision.band.name)
        if (decision.nextTransitionEpochMillis != null) {
            edit.putLong("next_age_transition", decision.nextTransitionEpochMillis)
        } else {
            edit.remove("next_age_transition")
        }
        return edit.commit()
    }
}
