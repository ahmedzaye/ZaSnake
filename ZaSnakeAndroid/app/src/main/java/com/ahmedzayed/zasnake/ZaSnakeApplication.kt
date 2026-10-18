package com.ahmedzayed.zasnake

import android.app.Application
import com.google.android.gms.games.PlayGamesSdk

class ZaSnakeApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Platform auth starts silently. Actual cloud features are enabled after Play Console setup.
        runCatching { PlayGamesSdk.initialize(this) }
    }
}
