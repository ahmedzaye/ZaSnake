package com.ahmedzayed.zasnake.game

/** Local Za Snake nickname, independent from the Google Play Games profile. */
object PlayerNamePolicy {
    const val MAX_LENGTH = 20
    const val DEFAULT_NAME = "Player"

    fun normalized(input: String): String? {
        if (input.any { it.isISOControl() }) return null
        val value = input.trim().replace(Regex("\\s+"), " ")
        if (value.isBlank() || value.length > MAX_LENGTH) return null
        return value
    }

    fun restored(input: String?): String = normalized(input.orEmpty()) ?: DEFAULT_NAME
}
