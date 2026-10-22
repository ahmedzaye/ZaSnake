package com.ahmedzayed.zasnake.billing

import android.app.Activity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.ahmedzayed.zasnake.game.COIN_PACKAGES
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.QueryProductDetailsParams

/** Read-only prices from Google Play. NEVER launch checkout or grant coins in this stage. */
class CoinPriceCatalog(private val activity: Activity) {
    enum class State { IDLE, LOADING, READY, UNAVAILABLE }

    var state by mutableStateOf(State.IDLE)
        private set

    var localizedPrices by mutableStateOf<Map<String, String>>(emptyMap())
        private set

    private var closed = false
    private var connecting = false
    private var requestVersion = 0
    private val client = BillingClient.newBuilder(activity.applicationContext)
        // No purchase flows exist in the current version. Never fulfill from this listener.
        .setListener { _, _ -> }
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder().enableOneTimeProducts().build()
        )
        .enableAutoServiceReconnection()
        .build()

    /** Called only after the mandatory age gate, and only for adult players. */
    fun refresh() {
        if (closed || connecting || state == State.LOADING) return
        if (client.isReady) {
            queryPrices()
            return
        }
        connecting = true
        state = State.LOADING
        client.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                activity.runOnUiThread {
                    if (closed) return@runOnUiThread
                    connecting = false
                    if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                        queryPrices()
                    } else {
                        localizedPrices = emptyMap()
                        state = State.UNAVAILABLE
                    }
                }
            }

            override fun onBillingServiceDisconnected() {
                activity.runOnUiThread {
                    if (closed) return@runOnUiThread
                    connecting = false
                    requestVersion++
                    localizedPrices = emptyMap()
                    state = State.UNAVAILABLE
                }
            }
        })
    }

    private fun queryPrices() {
        if (closed) return
        val requestId = ++requestVersion
        state = State.LOADING
        val products = COIN_PACKAGES.map { pack ->
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(pack.productId)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        }
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(products)
            .build()

        client.queryProductDetailsAsync(params) { result, details ->
            activity.runOnUiThread {
                if (closed || requestId != requestVersion) return@runOnUiThread
                if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                    localizedPrices = emptyMap()
                    state = State.UNAVAILABLE
                    return@runOnUiThread
                }
                val allowedIds = COIN_PACKAGES.mapTo(mutableSetOf()) { it.productId }
                localizedPrices = details.productDetailsList.mapNotNull { detail ->
                    if (detail.productId !in allowedIds) return@mapNotNull null
                    // Play returns user-eligible offers; use its formatted, local-currency price.
                    val displayPrice = detail.oneTimePurchaseOfferDetailsList
                        ?.firstOrNull()?.formattedPrice
                    displayPrice?.let { detail.productId to it }
                }.toMap()
                state = if (localizedPrices.isEmpty()) State.UNAVAILABLE else State.READY
            }
        }
    }

    fun close() {
        if (closed) return
        closed = true
        requestVersion++
        client.endConnection()
    }
}
