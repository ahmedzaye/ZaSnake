package com.ahmedzayed.zasnake.age

import java.util.Calendar
import java.util.GregorianCalendar

/** Only the category and the next age-boundary date are persisted. DOB is not stored. */
enum class AgeBand { CHILD, TEEN, ADULT }

data class AgeDecision(val band: AgeBand, val nextTransitionEpochMillis: Long?)

object AgePolicy {
    /** Returns null for invalid, future, or implausible dates. Uses the device's local civil date. */
    fun classify(day: Int, month: Int, year: Int, now: Calendar = Calendar.getInstance()): AgeDecision? {
        val birth = GregorianCalendar().apply {
            isLenient = false
            clear()
            set(year, month - 1, day)
        }
        val birthMillis = runCatching { birth.timeInMillis }.getOrNull() ?: return null
        if (year < 1900 || birthMillis > now.timeInMillis) return null
        val years = now.get(Calendar.YEAR) - year -
            if (now.get(Calendar.MONTH) + 1 < month ||
                (now.get(Calendar.MONTH) + 1 == month && now.get(Calendar.DAY_OF_MONTH) < day)) 1 else 0
        if (years !in 0..120) return null
        val band = when {
            years < 13 -> AgeBand.CHILD
            years < 18 -> AgeBand.TEEN
            else -> AgeBand.ADULT
        }
        val nextYear = when (band) {
            AgeBand.CHILD -> year + 13
            AgeBand.TEEN -> year + 18
            AgeBand.ADULT -> null
        }
        val nextTransition = nextYear?.let { boundaryYear ->
            GregorianCalendar().apply {
                isLenient = true // Feb 29 transitions on March 1 in non-leap years.
                clear()
                set(boundaryYear, month - 1, day)
            }.timeInMillis
        }
        return AgeDecision(band, nextTransition)
    }

    /** Advances persisted categories at 13 and 18 without retaining the original birth date. */
    fun refresh(decision: AgeDecision, nowMillis: Long = System.currentTimeMillis()): AgeDecision {
        val boundary = decision.nextTransitionEpochMillis ?: return decision
        if (nowMillis < boundary) return decision
        return when (decision.band) {
            AgeBand.CHILD -> {
                val nextBirthday = GregorianCalendar().apply {
                    timeInMillis = boundary
                    add(Calendar.YEAR, 5)
                }.timeInMillis
                if (nowMillis >= nextBirthday) AgeDecision(AgeBand.ADULT, null)
                else AgeDecision(AgeBand.TEEN, nextBirthday)
            }
            AgeBand.TEEN -> AgeDecision(AgeBand.ADULT, null)
            AgeBand.ADULT -> AgeDecision(AgeBand.ADULT, null)
        }
    }
}
