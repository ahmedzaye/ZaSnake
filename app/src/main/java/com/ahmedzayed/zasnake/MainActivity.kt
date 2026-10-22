package com.ahmedzayed.zasnake

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.core.view.WindowCompat
import com.ahmedzayed.zasnake.ads.RewardedAds
import com.ahmedzayed.zasnake.ads.AdConsentManager
import com.ahmedzayed.zasnake.billing.CoinPriceCatalog
import com.ahmedzayed.zasnake.age.AgeBand
import com.ahmedzayed.zasnake.age.AgeDecision
import com.ahmedzayed.zasnake.age.AgeGateRepository
import com.ahmedzayed.zasnake.age.AgeGateScreen
import com.ahmedzayed.zasnake.platform.PlayGamesBridge
import com.ahmedzayed.zasnake.ui.ZaSnakeApp
import com.google.android.gms.games.PlayGamesSdk

private val ZaGreen = Color(0xFF00FF88)
private val ZaBackground = Color(0xFF050A0E)
private val ZaSurface = Color(0xFF0A1520)

class MainActivity : ComponentActivity() {
    // Lazy: do not construct the game ViewModel or initialize third-party SDKs before age choice.
    private val gameViewModel: GameViewModel by viewModels()
    private lateinit var ageRepository: AgeGateRepository
    private var chosenAge by mutableStateOf<AgeDecision?>(null)
    private var playGamesBridge: PlayGamesBridge? = null
    private var rewardedAds: RewardedAds? = null
    private var coinPriceCatalog: CoinPriceCatalog? = null
    private var adConsentManager: AdConsentManager? = null
    private var privacyOptionsRequired by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.rgb(5, 10, 14)
        window.navigationBarColor = android.graphics.Color.rgb(5, 10, 14)
        ageRepository = AgeGateRepository(this)
        chosenAge = ageRepository.read()
        chosenAge?.let { setupAfterAgeGate(it) }
        setContent {
            ZaSnakeTheme {
                val choice = chosenAge
                if (choice == null) {
                    AgeGateScreen { day, month, year ->
                        val saved = ageRepository.submit(day, month, year)
                        if (saved == null) false else {
                            setupAfterAgeGate(saved)
                            chosenAge = saved
                            true
                        }
                    }
                } else {
                    ZaSnakeApp(
                        viewModel = gameViewModel,
                        playGamesBridge = checkNotNull(playGamesBridge),
                        rewardedAds = rewardedAds,
                        coinPriceCatalog = coinPriceCatalog,
                        privacyOptionsRequired = privacyOptionsRequired,
                        onPrivacyOptions = { adConsentManager?.showPrivacyOptions(::onConsentResult) },
                        onExitApp = { finish() }
                    )
                }
            }
        }
    }

    private fun setupAfterAgeGate(decision: AgeDecision) {
        if (playGamesBridge != null) return
        val olderUser = decision.band != AgeBand.CHILD
        // Child mode is local-only until PGS usage is separately reviewed for Families.
        if (olderUser) runCatching { PlayGamesSdk.initialize(this) }
        playGamesBridge = PlayGamesBridge(this, enabled = olderUser)
        // No mobile-ads init, load or ad request in child mode.
        if (decision.band == AgeBand.ADULT) coinPriceCatalog = CoinPriceCatalog(this)
        if (olderUser) {
            rewardedAds = RewardedAds(this, decision.band)
            adConsentManager = AdConsentManager(this).also { it.requestOnLaunch(::onConsentResult) }
        }
    }

    override fun onDestroy() {
        coinPriceCatalog?.close()
        super.onDestroy()
    }

    private fun onConsentResult(allowed: Boolean, optionsRequired: Boolean) {
        runOnUiThread {
            privacyOptionsRequired = optionsRequired
            rewardedAds?.updateConsentAllowance(allowed)
        }
    }
}

@Composable
private fun ZaSnakeTheme(content: @Composable () -> Unit) {
    val colors = darkColorScheme(
        primary = ZaGreen,
        onPrimary = Color(0xFF042014),
        background = ZaBackground,
        onBackground = Color(0xFFF2F7F4),
        surface = ZaSurface,
        onSurface = Color(0xFFE2EEE7),
        secondary = Color(0xFF00CFFF),
        tertiary = Color(0xFFFFD34E),
        error = Color(0xFFFF526F)
    )
    MaterialTheme(colorScheme = colors, content = content)
}
