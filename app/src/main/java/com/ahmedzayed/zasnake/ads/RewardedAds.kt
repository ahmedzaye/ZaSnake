package com.ahmedzayed.zasnake.ads

import android.app.Activity
import android.os.Bundle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.ahmedzayed.zasnake.age.AgeBand
import com.google.ads.mediation.admob.AdMobAdapter
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AgeRestrictedTreatment
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

/** Two independent ad placements: a revive must not consume the end-of-round coin bonus. */
enum class RewardPlacement { REVIVE, DOUBLE_COINS }

/** Test ads only; no ad SDK instance is created for children. */
class RewardedAds(private val activity: Activity, private val ageBand: AgeBand) {
    private companion object {
        const val TEST_REWARDED_UNIT = "ca-app-pub-3940256099942544/5224354917"
    }

    private fun adUnitId(placement: RewardPlacement): String = when (placement) {
        RewardPlacement.REVIVE -> activity.getString(com.ahmedzayed.zasnake.R.string.admob_rewarded_revive)
        RewardPlacement.DOUBLE_COINS -> activity.getString(com.ahmedzayed.zasnake.R.string.admob_rewarded_double)
    }

    private val ads = mutableMapOf<RewardPlacement, RewardedAd>()
    private val loading = mutableSetOf<RewardPlacement>()
    private var showing = false
    private var consentAllowed = false
    private var mobileAdsInitialized = false
    // Invalidate asynchronous loads when consent is withdrawn.
    private var requestGeneration = 0
    var readyVersion by mutableStateOf(0)
        private set

    fun readyFor(placement: RewardPlacement): Boolean {
        // Observe the version so a Compose screen updates when its placement becomes ready.
        readyVersion
        return consentAllowed && placement in ads && !showing
    }

    /** All ad SDK work stays blocked until UMP reports that ads can be requested. */
    fun updateConsentAllowance(allowed: Boolean) {
        if (ageBand == AgeBand.CHILD) return
        if (consentAllowed == allowed) return
        consentAllowed = allowed
        requestGeneration++
        ads.clear()
        loading.clear()
        readyVersion++
        if (!allowed) return
        if (mobileAdsInitialized) {
            RewardPlacement.entries.forEach(::load)
        } else {
            initializeMobileAds()
        }
    }

    private fun initializeMobileAds() {
        if (!consentAllowed || mobileAdsInitialized) return
        mobileAdsInitialized = true
        val treatment = if (ageBand == AgeBand.TEEN) AgeRestrictedTreatment.TEEN
            else AgeRestrictedTreatment.UNSPECIFIED
        val configuration = MobileAds.getRequestConfiguration().toBuilder()
            .setAgeRestrictedTreatment(treatment)
            .setMaxAdContentRating(RequestConfiguration.MAX_AD_CONTENT_RATING_G)
            .build()
        MobileAds.setRequestConfiguration(configuration)
        MobileAds.initialize(activity) { activity.runOnUiThread {
            if (consentAllowed) RewardPlacement.entries.forEach(::load)
        } }
    }

    private fun load(placement: RewardPlacement) {
        if (ageBand == AgeBand.CHILD || !consentAllowed ||
            placement in loading || placement in ads) return
        loading += placement
        val generation = requestGeneration
        // Keep older users' test-ad requests non-personalized pending the consent stage.
        val extras = Bundle().apply { putString("npa", "1") }
        val request = AdRequest.Builder().addNetworkExtrasBundle(AdMobAdapter::class.java, extras).build()
        RewardedAd.load(activity, adUnitId(placement), request,
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    if (generation != requestGeneration || !consentAllowed) return
                    loading -= placement
                    ads[placement] = ad
                    readyVersion++
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    if (generation != requestGeneration || !consentAllowed) return
                    loading -= placement
                    ads.remove(placement)
                    readyVersion++
                }
            })
    }

    /** Grant after both SDK earn confirmation and dismissal, never merely on opening the ad. */
    fun show(placement: RewardPlacement, onEarned: () -> Unit, onUnavailable: () -> Unit) {
        if (ageBand == AgeBand.CHILD || !consentAllowed) { onUnavailable(); return }
        val ad = ads.remove(placement)
        if (ad == null || showing) {
            if (ad != null) ads[placement] = ad
            if (!showing) { load(placement); onUnavailable() }
            return
        }
        showing = true
        readyVersion++
        var earned = false
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                showing = false
                readyVersion++
                if (earned) onEarned() else onUnavailable()
                load(placement)
            }
            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                showing = false
                readyVersion++
                onUnavailable()
                load(placement)
            }
        }
        ad.show(activity) { earned = true }
    }
}
