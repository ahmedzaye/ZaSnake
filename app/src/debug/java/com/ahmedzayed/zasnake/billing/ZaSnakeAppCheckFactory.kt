package com.ahmedzayed.zasnake.billing

import com.google.firebase.appcheck.AppCheckProviderFactory
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory

/** Debug attestation for local builds. Register the emitted debug token in Firebase Console;
 * NEVER hardcode the token or bundle this provider in release builds. */
object ZaSnakeAppCheckFactory {
    fun create(): AppCheckProviderFactory = DebugAppCheckProviderFactory.getInstance()
}
