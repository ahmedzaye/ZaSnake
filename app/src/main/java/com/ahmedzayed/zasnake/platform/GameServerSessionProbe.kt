package com.ahmedzayed.zasnake.platform

import android.app.Activity
import com.ahmedzayed.zasnake.BuildConfig
import com.google.android.gms.tasks.Tasks
import com.google.firebase.appcheck.FirebaseAppCheck
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.util.concurrent.TimeUnit

/**
 * Adult Debug-only, one-shot Play Games OAuth probe.
 * Never stores, logs, displays, or forwards server session credentials to Billing.
 */
object GameServerSessionProbe {
    private fun endpoint(): URL? {
        if (!BuildConfig.DEBUG) return null
        val parsed = runCatching { URI(BuildConfig.PGS_AUTH_PROBE_URL.trim()) }.getOrNull()
            ?: return null
        if (parsed.rawUserInfo != null || parsed.rawQuery != null ||
            parsed.rawFragment != null || parsed.rawPath !in listOf(null, "", "/")) return null
        // Local ADB reverse -> developer's authenticated Cloud Run proxy.
        // HTTP is permitted only to this exact loopback address + port in Debug.
        if (parsed.scheme == "http") {
            if (parsed.host != "127.0.0.1" || parsed.port != 8765) return null
            return runCatching { URL("http://127.0.0.1:8765/session") }.getOrNull()
        }
        if (parsed.scheme != "https" || parsed.host?.endsWith(".run.app") != true ||
            parsed.port != -1) return null
        return runCatching { URL("https://${parsed.host}/session") }.getOrNull()
    }

    fun run(activity: Activity, games: PlayGamesBridge, onResult: (String) -> Unit) {
        if (!BuildConfig.DEBUG || !games.isAllowedByAge() || !games.isConfigured() ||
            !BuildConfig.PGS_WEB_CLIENT_ID.endsWith(".apps.googleusercontent.com") ||
            endpoint() == null) {
            onResult("NOT_CONFIGURED")
            return
        }
        games.requestServerAuthCode(BuildConfig.PGS_WEB_CLIENT_ID) { authCode ->
            if (authCode.isNullOrBlank()) {
                onResult("AUTH_CODE")
                return@requestServerAuthCode
            }
            // A new short-lived single-use code must be exchanged only once.
            Thread({
                val result = exchange(authCode)
                activity.runOnUiThread { onResult(result) }
            }, "ZaSnakeAuthProbe").start()
        }
    }

    private fun exchange(authCode: String): String {
        val url = endpoint() ?: return "NOT_CONFIGURED"
        val appCheckToken = try {
            Tasks.await(FirebaseAppCheck.getInstance().getAppCheckToken(false),
                        12, TimeUnit.SECONDS).token
        } catch (_: Exception) {
            return "APP_CHECK"
        }
        if (appCheckToken.isBlank()) return "APP_CHECK"
        val conn = try {
            url.openConnection() as HttpURLConnection
        } catch (_: Exception) {
            return "NETWORK"
        }
        return try {
            conn.instanceFollowRedirects = false
            conn.requestMethod = "POST"
            conn.connectTimeout = 10000
            conn.readTimeout = 15000
            conn.setRequestProperty("Accept", "application/json")
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("X-Firebase-AppCheck", appCheckToken)
            conn.doOutput = true
            // Auth code remains in memory and is never printed or written to disk.
            val payload = JSONObject().put("serverAuthCode", authCode).toString()
            conn.outputStream.use { it.write(payload.toByteArray(Charsets.UTF_8)) }
            val status = conn.responseCode
            when (status) {
                200 -> {
                    val data = JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
                    val session = data.optString("sessionToken")
                    val accountHash = data.optString("obfuscatedAccountId")
                    if (session.matches(Regex("[A-Za-z0-9_-]{48,128}")) &&
                        accountHash.matches(Regex("[0-9a-f]{64}")) &&
                        data.optInt("expiresInSeconds") > 0) "OK" else "BAD_RESPONSE"
                    // Both sensitive response values are deliberately discarded.
                }
                401 -> {
                    val error = runCatching {
                        JSONObject(conn.errorStream?.bufferedReader()?.use { it.readText() } ?: "")
                            .optString("error")
                    }.getOrDefault("")
                    when (error) {
                        "app_attestation_required", "app_attestation_rejected", "app_identity_mismatch" ->
                            "APP_CHECK_REJECTED"
                        "games_auth_failed", "invalid_player" -> "GAMES_AUTH"
                        else -> "NETWORK"
                    }
                }
                503 -> {
                    val error = runCatching {
                        JSONObject(conn.errorStream?.bufferedReader()?.use { it.readText() } ?: "")
                            .optString("error")
                    }.getOrDefault("")
                    if (error == "gateway_disabled" || error == "unsafe_gateway_configuration")
                        "GATEWAY_DISABLED" else "NETWORK"
                }
                else -> "NETWORK"
            }
        } catch (_: Exception) {
            "NETWORK"
        } finally {
            conn.disconnect()
        }
    }
}
