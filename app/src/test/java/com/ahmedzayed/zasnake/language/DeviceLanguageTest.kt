package com.ahmedzayed.zasnake.language

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class DeviceLanguageTest {
    @Test fun arabicRegionalLocalesUseArabic() {
        assertEquals("ar", DeviceLanguage.fromLocale(Locale.forLanguageTag("ar-EG")))
        assertEquals("ar", DeviceLanguage.fromLocale(Locale.forLanguageTag("ar-AE")))
        assertEquals("ar", DeviceLanguage.fromLocale(Locale.forLanguageTag("ar-SA")))
    }

    @Test fun englishRegionalLocalesUseEnglish() {
        assertEquals("en", DeviceLanguage.fromLocale(Locale.forLanguageTag("en-US")))
        assertEquals("en", DeviceLanguage.fromLocale(Locale.forLanguageTag("en-GB")))
    }

    @Test fun unsupportedLanguagesSafelyFallBackToEnglish() {
        assertEquals("en", DeviceLanguage.fromLocale(Locale.forLanguageTag("fr-FR")))
        assertEquals("en", DeviceLanguage.fromLocale(Locale.forLanguageTag("de-DE")))
    }

    @Test fun automaticModeFollowsDeviceLanguage() {
        assertEquals("ar", DeviceLanguage.resolve("system", "ar"))
        assertEquals("en", DeviceLanguage.resolve("system", "en"))
        assertEquals("en", DeviceLanguage.resolve(null, "en"))
    }

    @Test fun manualArabicPersistsAcrossDeviceLanguageChanges() {
        assertEquals("ar", DeviceLanguage.resolve("ar", "en"))
        assertEquals("ar", DeviceLanguage.resolve("ar", "ar"))
    }

    @Test fun manualEnglishPersistsAcrossDeviceLanguageChanges() {
        assertEquals("en", DeviceLanguage.resolve("en", "ar"))
        assertEquals("en", DeviceLanguage.resolve("en", "en"))
    }

    @Test fun invalidPreferenceFallsBackToAutomatic() {
        assertEquals("system", DeviceLanguage.normalizePreference(null))
        assertEquals("system", DeviceLanguage.normalizePreference("fr"))
        assertEquals("ar", DeviceLanguage.resolve("fr", "ar"))
        assertEquals("en", DeviceLanguage.resolve("fr", "en"))
    }
}
