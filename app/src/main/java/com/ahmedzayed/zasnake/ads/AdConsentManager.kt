package com.ahmedzayed.zasnake.ads

import android.app.Activity
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform

/**
 * Run ONLY after the neutral age gate for a non-child account.
 * Never ask the ads SDK to initialize or load before UMP says ads can be requested.
 */
class AdConsentManager(private val activity: Activity) {
    private val consentInformation = UserMessagingPlatform.getConsentInformation(activity)

    val privacyOptionsRequired: Boolean
        get() = consentInformation.privacyOptionsRequirementStatus ==
            ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED

    fun requestOnLaunch(onChange: (canRequestAds: Boolean, privacyOptionsRequired: Boolean) -> Unit) {
        consentInformation.requestConsentInfoUpdate(
            activity,
            ConsentRequestParameters.Builder().build(),
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) {
                    notifyStatus(onChange)
                }
            },
            {
                // UMP may retain a valid decision from a previous launch on network failure.
                // Never infer consent ourselves; only ask the SDK.
                notifyStatus(onChange)
            }
        )
    }

    fun showPrivacyOptions(onChange: (canRequestAds: Boolean, privacyOptionsRequired: Boolean) -> Unit) {
        if (!privacyOptionsRequired) return
        UserMessagingPlatform.showPrivacyOptionsForm(activity) {
            notifyStatus(onChange)
        }
    }

    private fun notifyStatus(onChange: (Boolean, Boolean) -> Unit) {
        onChange(consentInformation.canRequestAds(), privacyOptionsRequired)
    }
}
