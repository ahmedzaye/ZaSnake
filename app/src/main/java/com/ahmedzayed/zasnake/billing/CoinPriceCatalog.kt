package com.ahmedzayed.zasnake.billing

import android.app.Activity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.ahmedzayed.zasnake.BuildConfig
import com.ahmedzayed.zasnake.game.COIN_PACKAGES
import com.ahmedzayed.zasnake.platform.PlayGamesBridge
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.concurrent.Executors

/**
 * Staging-only Billing integration. No checkout until BOTH the compile-time flag and
 * authenticated server session are available. This class NEVER credits a local receipt.
 * Server-side claims are the only source of paid-grant snapshots.
 */
class CoinPriceCatalog(
    private val activity: Activity,
    private val playGames: PlayGamesBridge,
    private val onVerifiedGrants: (Map<String, Int>) -> Unit
) {
    enum class State { IDLE, LOADING, READY, UNAVAILABLE }

    var state by mutableStateOf(State.IDLE)
        private set
    var localizedPrices by mutableStateOf<Map<String, String>>(emptyMap())
        private set
    var statusMessage by mutableStateOf("Purchases remain disabled until server verification is tested.")
        private set

    private val worker = Executors.newSingleThreadExecutor()
    private val allowedIds = COIN_PACKAGES.map { it.productId }.toSet()
    private val productDetails = mutableMapOf<String, ProductDetails>()
    private var closed = false
    private var connecting = false
    private var requestVersion = 0
    private var sessionToken: String? = null
    private var accountHash: String? = null
    private var authBusy = false
    private var processing = false

    private val listener = PurchasesUpdatedListener { result, purchases ->
        activity.runOnUiThread {
            if (closed) return@runOnUiThread
            when (result.responseCode) {
                BillingClient.BillingResponseCode.OK -> purchases?.let(::handlePurchases)
                BillingClient.BillingResponseCode.USER_CANCELED ->
                    statusMessage = "Purchase canceled."
                else -> statusMessage = "Google Play returned code ${result.responseCode}."
            }
        }
    }
    private val client = BillingClient.newBuilder(activity.applicationContext)
        .setListener(listener)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder().enableOneTimeProducts().build()
        )
        .enableAutoServiceReconnection()
        .build()

    private val stagingEnabled: Boolean
        get() = BuildConfig.BILLING_INTEGRATION_ENABLED &&
            BuildConfig.PGS_WEB_CLIENT_ID.endsWith(".apps.googleusercontent.com")

    /** The OAuth secret must NEVER be present in BuildConfig or the application. */
    fun refresh() {
        if (closed || connecting || state == State.LOADING) return
        if (client.isReady) {
            queryPrices()
            ensureSession()
            resumePendingPurchases()
            return
        }
        state = State.LOADING
        connecting = true
        client.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                activity.runOnUiThread {
                    if (closed) return@runOnUiThread
                    connecting = false
                    if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                        queryPrices()
                        ensureSession()
                        resumePendingPurchases()
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
                    productDetails.clear()
                    state = State.UNAVAILABLE
                }
            }
        })
    }

    private fun queryPrices() {
        if (closed || !client.isReady) return
        val id = ++requestVersion
        state = State.LOADING
        val products = COIN_PACKAGES.map { pack ->
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(pack.productId)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        }
        client.queryProductDetailsAsync(
            QueryProductDetailsParams.newBuilder().setProductList(products).build()
        ) { result, response ->
            activity.runOnUiThread {
                if (closed || id != requestVersion) return@runOnUiThread
                productDetails.clear()
                if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                    localizedPrices = emptyMap()
                    state = State.UNAVAILABLE
                    return@runOnUiThread
                }
                localizedPrices = response.productDetailsList.mapNotNull { detail ->
                    if (detail.productId !in allowedIds) return@mapNotNull null
                    val offer = detail.oneTimePurchaseOfferDetailsList?.firstOrNull()
                        ?: return@mapNotNull null
                    productDetails[detail.productId] = detail
                    detail.productId to offer.formattedPrice
                }.toMap()
                state = if (localizedPrices.isEmpty()) State.UNAVAILABLE else State.READY
            }
        }
    }

    private fun ensureSession() {
        if (closed || !stagingEnabled || authBusy || sessionToken != null) return
        authBusy = true
        playGames.requestServerAuthCode(BuildConfig.PGS_WEB_CLIENT_ID) { code ->
            if (closed) return@requestServerAuthCode
            if (code.isNullOrBlank()) {
                authBusy = false
                statusMessage = "Play Games server sign-in unavailable."
                return@requestServerAuthCode
            }
            worker.execute {
                val response = runCatching {
                    postJson("/session", JSONObject().put("serverAuthCode", code), null)
                }.getOrNull()
                activity.runOnUiThread {
                    if (closed) return@runOnUiThread
                    authBusy = false
                    sessionToken = response?.optString("sessionToken")?.takeIf(String::isNotBlank)
                    accountHash = response?.optString("obfuscatedAccountId")
                        ?.takeIf { it.matches(Regex("[0-9a-f]{64}")) }
                    if (sessionToken != null && accountHash != null) {
                        statusMessage = "Billing staging session authenticated."
                        loadGrants()
                        resumePendingPurchases()
                    } else {
                        sessionToken = null
                        accountHash = null
                        onVerifiedGrants(emptyMap())
                        statusMessage = "Billing server authentication unavailable."
                    }
                }
            }
        }
    }

    fun canBuy(productId: String): Boolean =
        !closed && stagingEnabled && client.isReady && sessionToken != null &&
            accountHash != null && !processing && productId in productDetails

    fun buy(productId: String) {
        if (!canBuy(productId)) return
        val detail = productDetails[productId] ?: return
        val offer = detail.oneTimePurchaseOfferDetailsList?.firstOrNull() ?: return
        val flow = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(detail)
                        .setOfferToken(offer.offerToken)
                        .build()
                )
            )
            .setObfuscatedAccountId(checkNotNull(accountHash))
            .build()
        val result = client.launchBillingFlow(activity, flow)
        if (result.responseCode != BillingClient.BillingResponseCode.OK) {
            statusMessage = "Unable to start Google Play checkout (${result.responseCode})."
        }
    }

    /** Re-query after a restart and after returning from Play's purchase screen. */
    fun resumePendingPurchases() {
        if (closed || !stagingEnabled || !client.isReady) return
        if (sessionToken == null) {
            ensureSession()
            return
        }
        client.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.INAPP).build()
        ) { result, purchases ->
            activity.runOnUiThread {
                if (!closed && result.responseCode == BillingClient.BillingResponseCode.OK) {
                    handlePurchases(purchases)
                }
            }
        }
    }

    private fun handlePurchases(purchases: List<Purchase>) {
        if (closed || !stagingEnabled || processing) return
        val completed = purchases.filter {
            it.purchaseState == Purchase.PurchaseState.PURCHASED &&
                it.products.size == 1 && it.products[0] in allowedIds
        }
        if (purchases.any { it.purchaseState == Purchase.PurchaseState.PENDING }) {
            statusMessage = "Payment pending: no coins have been granted."
        }
        if (completed.isEmpty()) return
        val session = sessionToken ?: run { ensureSession(); return }
        processing = true
        worker.execute {
            var ok = true
            for (purchase in completed) {
                val reply = runCatching {
                    postJson("/claim", JSONObject()
                        .put("productId", purchase.products.single())
                        .put("purchaseToken", purchase.purchaseToken), session)
                }.getOrNull()
                if (reply == null || reply.optJSONObject("grants") == null) ok = false
            }
            activity.runOnUiThread {
                if (closed) return@runOnUiThread
                processing = false
                statusMessage = if (ok) "Purchases verified by the server."
                    else "Purchase verification delayed; retry from the shop."
                loadGrants()
            }
        }
    }

    private fun loadGrants() {
        val session = sessionToken ?: return
        if (closed || !stagingEnabled) return
        worker.execute {
            val response = runCatching { getJson("/grants", session) }.getOrNull()
            val verified = mutableMapOf<String, Int>()
            val grants = response?.optJSONObject("grants")
            if (grants != null) {
                val keys = grants.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    val coins = grants.optInt(key, -1)
                    if (key.matches(Regex("[0-9a-f]{64}")) && coins in setOf(1000, 2000, 4000)) {
                        verified[key] = coins
                    }
                }
            }
            activity.runOnUiThread {
                if (!closed && response != null) onVerifiedGrants(verified)
            }
        }
    }

    private fun postJson(path: String, json: JSONObject, bearer: String?): JSONObject =
        request(path, "POST", json, bearer)

    private fun getJson(path: String, bearer: String): JSONObject =
        request(path, "GET", null, bearer)

    private fun request(path: String, method: String, json: JSONObject?, bearer: String?): JSONObject {
        // API is private during staging. The flag must remain off until a reviewed
        // mobile-facing authenticated gateway is deployed.
        val base = BuildConfig.BILLING_BASE_URL.removeSuffix("/")
        val host = URL(base)
        require(host.protocol == "https" && host.host.endsWith(".run.app"))
        val connection = URL(base + path).openConnection() as HttpURLConnection
        return try {
            connection.requestMethod = method
            connection.connectTimeout = 10000
            connection.readTimeout = 12000
            connection.setRequestProperty("Accept", "application/json")
            if (!bearer.isNullOrBlank()) {
                connection.setRequestProperty("Authorization", "Bearer $bearer")
            }
            if (json != null) {
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json")
                connection.outputStream.use {
                    it.write(json.toString().toByteArray(Charsets.UTF_8))
                }
            }
            val code = connection.responseCode
            if (code !in 200..299) error("billing_api_http_$code")
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            JSONObject(body)
        } finally {
            connection.disconnect()
        }
    }

    fun close() {
        closed = true
        requestVersion++
        sessionToken = null
        accountHash = null
        worker.shutdownNow()
        client.endConnection()
    }
}
