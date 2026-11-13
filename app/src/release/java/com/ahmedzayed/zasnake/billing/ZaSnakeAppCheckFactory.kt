package com.ahmedzayed.zasnake.billing

import com.google.firebase.appcheck.AppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory

/** Play-distributed releases must pass Play Integrity verification. */
object ZaSnakeAppCheckFactory {
    fun create(): AppCheckProviderFactory = PlayIntegrityAppCheckProviderFactory.getInstance()
}
