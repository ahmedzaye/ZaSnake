package com.ahmedzayed.zasnake.language

import java.util.Locale

/** Za Snake currently ships Arabic and English. All other device languages use English. */
object DeviceLanguage {
    const val SYSTEM = "system"
    const val ARABIC = "ar"
    const val ENGLISH = "en"

    /** Persist only these three language choices; older/invalid preferences become automatic. */
    fun normalizePreference(preference: String?): String = when (preference) {
        ARABIC, ENGLISH -> preference
        else -> SYSTEM
    }

    fun fromLocale(locale: Locale): String =
        if (locale.language.equals("ar", ignoreCase = true)) ARABIC else ENGLISH

    /** A manual choice takes precedence; automatic follows Android on every locale change. */
    fun resolve(preference: String?, deviceLanguage: String): String = when (normalizePreference(preference)) {
        ARABIC -> ARABIC
        ENGLISH -> ENGLISH
        else -> if (deviceLanguage == ARABIC) ARABIC else ENGLISH
    }
}
