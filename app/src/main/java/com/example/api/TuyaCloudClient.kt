package com.example.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.net.URLEncoder
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Direkte Tuya-Cloud-Anbindung fuer Tuya-faehige Kameras (Android-Port von
 * lib/tuya.ts aus dem Die-Stallwache-Repo).
 *
 * Mehrere Kameras koennen ueber dasselbe Tuya-Cloud-Projekt laufen (ein
 * Access ID/Secret-Paar), aber jede mit ihrer eigenen Geraete-ID. Die
 * Tuya-OpenAPI liefert auf Anfrage eine kurzlebige HLS-Stream-URL, die
 * ExoPlayer direkt abspielen kann (kein CORS, kein Proxy noetig – der
 * Browser-Umweg der Webapp entfaellt in der nativen App).
 *
 * Signierung gemaess Tuya-Doku: HMAC-SHA256 ueber
 *   client_id [+ access_token] + t + stringToSign
 * mit stringToSign = METHOD \n sha256(body) \n \n pfad
 */
class TuyaCloudClient(
    private val accessId: String,
    private val accessSecret: String,
    private val deviceIds: Map<String, String>,
    apiBase: String = "https://openapi.tuyaeu.com",
) {
    private val base = apiBase.trim().trimEnd('/').ifEmpty { "https://openapi.tuyaeu.com" }

    private val http = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    fun konfiguriert(kamera: String): Boolean =
        accessId.isNotBlank() && accessSecret.isNotBlank() &&
            !deviceIds[kamera].isNullOrBlank()

    private fun sha256Hex(s: String): String =
        MessageDigest.getInstance("SHA-256").digest(s.toByteArray())
            .joinToString("") { "%02x".format(it) }

    private fun hmacUpper(s: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(accessSecret.toByteArray(), "HmacSHA256"))
        return mac.doFinal(s.toByteArray())
            .joinToString("") { "%02x".format(it) }.uppercase()
    }

    private suspend fun tuyaRequest(
        method: String,
        pfad: String,
        body: String? = null,
        accessToken: String? = null,
    ): JSONObject = withContext(Dispatchers.IO) {
        val t = System.currentTimeMillis().toString()
        val stringToSign = listOf(method, sha256Hex(body ?: ""), "", pfad).joinToString("\n")
        val signatur = hmacUpper(accessId + (accessToken ?: "") + t + stringToSign)

        val builder = Request.Builder()
            .url("$base$pfad")
            .header("client_id", accessId)
            .header("t", t)
            .header("sign_method", "HMAC-SHA256")
            .header("sign", signatur)
        if (accessToken != null) builder.header("access_token", accessToken)
        if (body != null) {
            builder.method(method, body.toRequestBody("application/json".toMediaType()))
        } else {
            builder.method(method, null)
        }

        http.newCall(builder.build()).execute().use { res ->
            val json = JSONObject(res.body?.string() ?: "{}")
            if (!json.optBoolean("success", false)) {
                throw TuyaApiException(
                    "Tuya-API-Fehler ${json.opt("code") ?: res.code}: " +
                        (json.optString("msg").ifEmpty { "unbekannt" })
                )
            }
            json.getJSONObject("result")
        }
    }

    // Token cachen (Tuya-Tokens gelten ~2 h). Gilt projektweit (nicht pro
    // Geraet), daher ein gemeinsamer Cache pro Client-Instanz.
    @Volatile
    private var tokenCache: Pair<String, Long>? = null

    private suspend fun holeToken(): String {
        tokenCache?.let { (token, ablauf) ->
            if (System.currentTimeMillis() < ablauf) return token
        }
        val r = tuyaRequest("GET", "/v1.0/token?grant_type=1")
        val token = r.getString("access_token")
        val expire = r.optLong("expire_time", 7200)
        tokenCache = token to
            (System.currentTimeMillis() + maxOf(60, expire - 60) * 1000)
        return token
    }

    /** Fordert bei Tuya eine kurzlebige Stream-URL fuer die angegebene Kamera an. */
    suspend fun holeStreamUrl(kamera: String, typ: String = "hls"): String {
        val deviceId = deviceIds[kamera]
            ?: throw TuyaApiException("Keine Geraete-ID fuer $kamera konfiguriert")
        val token = holeToken()
        val r = tuyaRequest(
            "POST",
            "/v1.0/devices/${URLEncoder.encode(deviceId, "UTF-8")}/stream/actions/allocate",
            JSONObject().put("type", typ).toString(),
            token,
        )
        return r.getString("url")
    }
}

class TuyaApiException(message: String) : Exception(message)
