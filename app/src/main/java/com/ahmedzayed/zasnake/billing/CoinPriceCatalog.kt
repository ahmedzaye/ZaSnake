package com.ahmedzayed.zasnake.billing

import android.app.Activity
import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.ahmedzayed.zasnake.R
import com.ahmedzayed.zasnake.game.COIN_PACKAGES
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.google.android.gms.games.PlayGames
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Play Billing 8 checkout backed by authenticated Google Play Games sessions.
 * NEVER grant from a Billing callback: only signed-in server-verified ledger responses count.
 * Checkout is fail-closed when the real HTTPS backend / PGS web credential is not configured.
 */
class CoinPriceCatalog(
    private val activity: Activity,
    private val onVerifiedGrants: (Map<String, Int>) -> Unit
) {
    enum class State { IDLE, LOADING, READY, UNAVAILABLE }
    var state by mutableStateOf(State.IDLE)
        private set
    var localizedPrices by mutableStateOf<Map<String, String>>(emptyMap())
        private set
    var purchaseReady by mutableStateOf(false)
        private set
    var purchaseStatus by mutableStateOf<String?>(null)
        private set

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val backendUrl = activity.getString(R.string.billing_backend_url).trim().trimEnd('/')
    private val webClientId = activity.getString(R.string.pgs_web_client_id).trim()
    private val configured = backendUrl.startsWith("https://") &&
        webClientId.endsWith(".apps.googleusercontent.com")
    private var closed = false
    private var connecting = false
    private var serverConnecting = false
    private var requestVersion = 0
    private var sessionToken: String? = null
    private var sessionExpiresAt = 0L
    private var obfuscatedAccountId: String? = null
    private val products = mutableMapOf<String, ProductDetails>()
    private val handlingTokens = mutableSetOf<String>()
    private val allowedAmounts = COIN_PACKAGES.map { it.coinAmount }.toSet()

    private val client = BillingClient.newBuilder(activity.applicationContext)
        .setListener { result, purchases ->
            activity.runOnUiThread {
                if (closed) return@runOnUiThread
                when (result.responseCode) {
                    BillingClient.BillingResponseCode.OK -> purchases.orEmpty().forEach(::processPurchase)
                    BillingClient.BillingResponseCode.USER_CANCELED -> purchaseStatus = "canceled"
                    else -> purchaseStatus = "play_error"
                }
            }
        }
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder().enableOneTimeProducts().build()
        )
        .enableAutoServiceReconnection()
        .build()

    fun refresh() {
        if (closed) return
        ensureServerSession()
        if (connecting || state == State.LOADING) return
        if (client.isReady) {
            queryPrices()
            queryOutstanding()
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
                        queryOutstanding()
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
                    products.clear()
                    localizedPrices = emptyMap()
                    state = State.UNAVAILABLE
                }
            }
        })
    }

    private fun ensureServerSession() {
        if (!configured || closed || serverConnecting) return
        if (sessionToken != null && SystemClock.elapsedRealtime() < sessionExpiresAt) return
        purchaseReady = false
        serverConnecting = true
        val games = PlayGames.getGamesSignInClient(activity)
        games.isAuthenticated.addOnCompleteListener { status ->
            if (closed) return@addOnCompleteListener
            if (!status.isSuccessful || status.result?.isAuthenticated != true) {
                serverConnecting = false
                purchaseStatus = "pgs_signin_required"
                return@addOnCompleteListener
            }
            games.requestServerSideAccess(webClientId, false)
                .addOnSuccessListener { code ->
                    scope.launch {
                        try {
                            val result = request("/session", JSONObject().put("serverAuthCode", code), null)
                            val token = result.getString("sessionToken")
                            val account = result.getString("obfuscatedAccountId")
                            val seconds = result.optLong("expiresInSeconds", 3600).coerceIn(60, 3600)
                            sessionToken = token
                            obfuscatedAccountId = account
                            sessionExpiresAt = SystemClock.elapsedRealtime() + (seconds - 30) * 1000
                            purchaseReady = products.isNotEmpty()
                            purchaseStatus = null
                            refreshGrants()
                            queryOutstanding()
                        } catch (_: Exception) {
                            purchaseStatus = "server_unavailable"
                        } finally {
                            serverConnecting = false
                        }
                    }
                }
                .addOnFailureListener {
                    serverConnecting = false
                    purchaseStatus = "pgs_signin_required"
                }
        }
    }

    private fun queryPrices() {
        if (closed) return
        val version = ++requestVersion
        state = State.LOADING
        val query = QueryProductDetailsParams.newBuilder().setProductList(
            COIN_PACKAGES.map {
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(it.productId)
                    .setProductType(BillingClient.ProductType.INAPP)
                    .build()
            }
        ).build()
        client.queryProductDetailsAsync(query) { result, response ->
            activity.runOnUiThread {
                if (closed || version != requestVersion) return@runOnUiThread
                products.clear()
                localizedPrices = emptyMap()
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    val prices = mutableMapOf<String, String>()
                    response.productDetailsList.forEach { detail ->
                        if (COIN_PACKAGES.none { it.productId == detail.productId }) return@forEach
                        // Never expose rental/preorder options for consumable game currency.
                        val offer = detail.oneTimePurchaseOfferDetailsList
                            ?.firstOrNull { it.rentalDetails == null && it.preorderDetails == null }
                        if (offer != null && offer.offerToken.isNotBlank()) {
                            products[detail.productId] = detail
                            prices[detail.productId] = offer.formattedPrice
                        }
                    }
                    localizedPrices = prices
                }
                state = if (localizedPrices.isEmpty()) State.UNAVAILABLE else State.READY
                purchaseReady = configured && products.isNotEmpty() &&
                    sessionToken != null && SystemClock.elapsedRealtime() < sessionExpiresAt
            }
        }
    }

    fun buy(productId: String) {
        if (!purchaseReady || !client.isReady || closed) return
        val detail = products[productId] ?: return
        val account = obfuscatedAccountId ?: return
        if (SystemClock.elapsedRealtime() >= sessionExpiresAt) {
            sessionToken = null
            purchaseReady = false
            ensureServerSession()
            return
        }
        val offer = detail.oneTimePurchaseOfferDetailsList
            ?.firstOrNull { it.rentalDetails == null && it.preorderDetails == null } ?: return
        val item = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(detail)
            .setOfferToken(offer.offerToken)
            .build()
        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(item))
            .setObfuscatedAccountId(account)
            .build()
        val result = client.launchBillingFlow(activity, params)
        if (result.responseCode != BillingClient.BillingResponseCode.OK) {
            purchaseStatus = "play_error"
        }
    }

    private fun queryOutstanding() {
        if (!client.isReady || sessionToken == null || closed) return
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP).build()
        client.queryPurchasesAsync(params) { result, purchases ->
            activity.runOnUiThread {
                if (!closed && result.responseCode == BillingClient.BillingResponseCode.OK) {
                    purchases.forEach(::processPurchase)
                }
            }
        }
    }

    private fun processPurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PENDING) {
            purchaseStatus = "pending"
            return
        }
        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) return
        val productId = purchase.products.singleOrNull() ?: return
        if (COIN_PACKAGES.none { it.productId == productId }) return
        val session = sessionToken ?: run { ensureServerSession(); return }
        val token = purchase.purchaseToken
        if (!handlingTokens.add(token)) return
        purchaseStatus = "verifying"
        scope.launch {
            try {
                val reply = request("/claim", JSONObject().apply {
                    put("productId", productId)
                    put("purchaseToken", token)
                }, session)
                applyServerGrants(reply)
                purchaseStatus = "verified"
                // Refresh full server ledger to recover all prior grants.
                refreshGrants()
            } catch (_: Exception) {
                purchaseStatus = "verification_retry"
            } finally {
                handlingTokens.remove(token)
            }
        }
    }

    private suspend fun refreshGrants() {
        val token = sessionToken ?: return
        try {
            val response = request("/grants", null, token)
            applyServerGrants(response)
        } catch (_: Exception) {
            // The server remains authoritative: no speculative coins on network failure.
        }
    }

    private fun applyServerGrants(reply: JSONObject) {
        if (closed) return
        val entries = reply.optJSONObject("grants") ?: return
        val result = mutableMapOf<String, Int>()
        val keys = entries.keys()
        while (keys.hasNext()) {
            val id = keys.next()
            val amount = entries.optInt(id, 0)
            if (id.length == 64 && id.all { it in '0'..'9' || it in 'a'..'f' } &&
                amount in allowedAmounts) {
                result[id] = amount
            }
        }
        if (result.isNotEmpty()) onVerifiedGrants(result)
    }

    private suspend fun request(path: String, body: JSONObject?, bearer: String?): JSONObject =
        withContext(Dispatchers.IO) {
            val connection = URL(backendUrl + path).openConnection() as HttpURLConnection
            try {
                connection.requestMethod = if (body == null) "GET" else "POST"
                connection.connectTimeout = 12000
                connection.readTimeout = 12000
                connection.setRequestProperty("Accept", "application/json")
                if (bearer != null) connection.setRequestProperty("Authorization", "Bearer " + bearer)
                if (body != null) {
                    connection.doOutput = true
                    connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                    connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
                }
                if (connection.responseCode !in 200..299) throw IllegalStateException("Server refused request")
                JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
            } finally {
                connection.disconnect()
            }
        }

    fun close() {
        if (closed) return
        closed = true
        requestVersion++
        sessionToken = null
        scope.cancel()
        client.endConnection()
    }
}
