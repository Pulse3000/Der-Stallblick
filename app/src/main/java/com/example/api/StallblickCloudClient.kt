package com.example.api

import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Client fuer die deployte Stallblick-Webapp (Die-Stallwache-Repo auf Vercel).
 *
 * Bildet die drei Serverfunktionen der Webapp fuer die native App ab:
 *   - POST /api/login            gemeinsames Passwort (STALLBLICK_PASSWORT) ->
 *                                HMAC-signiertes Session-Cookie (7 Tage)
 *   - GET  /api/events           Ereignisliste der KI-Wache (neueste zuerst)
 *   - GET  /api/<kamera>/stream  kurzlebige Tuya-HLS-URL (laeuft ueber den
 *                                CORS-Proxy der Webapp; der Session-Cookie
 *                                wird auch von ExoPlayer mitgesendet, weil
 *                                der Player denselben OkHttp-Client nutzt)
 *
 * Das Session-Cookie wird in SharedPreferences persistiert, damit ein Login
 * App-Neustarts uebersteht (Laufzeit wie in der Webapp: 7 Tage).
 */
class StallblickCloudClient(private val prefs: SharedPreferences) {

    companion object {
        const val SESSION_COOKIE = "stallblick_session"
        private const val PREF_COOKIE = "cloud_session_cookie"
        private const val PREF_COOKIE_HOST = "cloud_session_cookie_host"
    }

    /** Persistenter Cookie-Speicher nur fuer das Stallblick-Session-Cookie. */
    private val cookieJar = object : CookieJar {
        override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
            for (c in cookies) {
                if (c.name == SESSION_COOKIE) {
                    prefs.edit()
                        .putString(PREF_COOKIE, c.value)
                        .putString(PREF_COOKIE_HOST, url.host)
                        .apply()
                }
            }
        }

        override fun loadForRequest(url: HttpUrl): List<Cookie> {
            val wert = prefs.getString(PREF_COOKIE, null) ?: return emptyList()
            val host = prefs.getString(PREF_COOKIE_HOST, null) ?: return emptyList()
            if (url.host != host) return emptyList()
            return listOf(
                Cookie.Builder()
                    .name(SESSION_COOKIE)
                    .value(wert)
                    .domain(host)
                    .path("/")
                    .build()
            )
        }
    }

    /** Gemeinsamer HTTP-Client – auch ExoPlayer nutzt ihn (Cookie fuer den Tuya-Proxy). */
    val http: OkHttpClient = OkHttpClient.Builder()
        .cookieJar(cookieJar)
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    fun hatSession(): Boolean = prefs.getString(PREF_COOKIE, null) != null

    fun logout() {
        prefs.edit().remove(PREF_COOKIE).remove(PREF_COOKIE_HOST).apply()
    }

    sealed class LoginErgebnis {
        /** Angemeldet – oder Webapp laeuft ohne Passwortschutz. */
        data object Ok : LoginErgebnis()
        data object FalschesPasswort : LoginErgebnis()
        data class Fehler(val meldung: String) : LoginErgebnis()
    }

    suspend fun login(basisUrl: String, passwort: String): LoginErgebnis =
        withContext(Dispatchers.IO) {
            val url = "${basisUrl.trimEnd('/')}/api/login"
            try {
                val body = JSONObject().put("passwort", passwort).toString()
                    .toRequestBody("application/json".toMediaType())
                http.newCall(Request.Builder().url(url).post(body).build())
                    .execute().use { res ->
                        when {
                            res.isSuccessful -> LoginErgebnis.Ok
                            res.code == 401 -> LoginErgebnis.FalschesPasswort
                            else -> LoginErgebnis.Fehler("Login-HTTP-Fehler ${res.code}")
                        }
                    }
            } catch (e: Exception) {
                LoginErgebnis.Fehler(e.message ?: "Webapp nicht erreichbar")
            }
        }

    /** Ereignis der KI-Wache, wie es die Webapp liefert (lib/events.ts). */
    data class CloudEreignis(
        val id: String,
        val typ: String,
        val kuhId: String?,
        val kamera: String,
        val nachricht: String,
        val konfidenz: Double?,
        /** ISO-8601-Zeitstempel. */
        val zeit: String,
    )

    data class CloudEreignisse(
        val ereignisse: List<CloudEreignis>,
        val letzterKontakt: String?,
        /** "edge-agent" = echte Daten, "demo" = Platzhalter der Webapp. */
        val quelle: String,
    )

    class NichtAngemeldetException : Exception("Webapp meldet 401 – Session abgelaufen")

    suspend fun holeEreignisse(basisUrl: String): CloudEreignisse =
        withContext(Dispatchers.IO) {
            val url = "${basisUrl.trimEnd('/')}/api/events"
            http.newCall(Request.Builder().url(url).get().build()).execute().use { res ->
                if (res.code == 401) throw NichtAngemeldetException()
                if (!res.isSuccessful) throw Exception("Events-HTTP-Fehler ${res.code}")
                val json = JSONObject(res.body?.string() ?: "{}")
                val liste = json.optJSONArray("ereignisse")
                val ereignisse = buildList {
                    if (liste != null) {
                        for (i in 0 until liste.length()) {
                            val e = liste.optJSONObject(i) ?: continue
                            add(
                                CloudEreignis(
                                    id = e.optString("id"),
                                    typ = e.optString("typ", "info"),
                                    kuhId = if (e.isNull("kuhId")) null else e.optString("kuhId"),
                                    kamera = e.optString("kamera", "stallwache"),
                                    nachricht = e.optString("nachricht"),
                                    konfidenz = if (e.isNull("konfidenz")) null
                                    else e.optDouble("konfidenz"),
                                    zeit = e.optString("zeit"),
                                )
                            )
                        }
                    }
                }
                CloudEreignisse(
                    ereignisse = ereignisse,
                    letzterKontakt = if (json.isNull("letzterKontakt")) null
                    else json.optString("letzterKontakt"),
                    quelle = json.optString("quelle", "demo"),
                )
            }
        }

    /**
     * Holt vom Webapp-Endpoint (z.B. /api/futterwache/stream) eine kurzlebige
     * Tuya-HLS-URL. Die Webapp liefert eine relative Proxy-URL
     * (/api/futterwache/proxy?url=...) – die wird gegen die Basis-URL
     * aufgeloest, damit ExoPlayer sie laden kann.
     */
    suspend fun holeTuyaStreamUrl(basisUrl: String, endpoint: String): String =
        withContext(Dispatchers.IO) {
            val basis = basisUrl.trimEnd('/')
            http.newCall(Request.Builder().url("$basis$endpoint").get().build())
                .execute().use { res ->
                    if (res.code == 401) throw NichtAngemeldetException()
                    if (!res.isSuccessful) throw Exception("Tuya-HTTP-Fehler ${res.code}")
                    val json = JSONObject(res.body?.string() ?: "{}")
                    val url = json.optString("url")
                    if (url.isEmpty()) throw Exception("Tuya-Antwort ohne URL")
                    if (url.startsWith("http")) url else "$basis$url"
                }
        }

    /**
     * Meldet ein Ereignis an die Ingest-API der Webapp (POST /api/events mit
     * x-ingest-token) – derselbe Weg, den der Edge-Agent im Stall nutzt.
     */
    suspend fun sendeEreignis(
        basisUrl: String,
        ingestToken: String,
        typ: String,
        nachricht: String,
        kuhId: String?,
        kamera: String,
        konfidenz: Double?,
    ): Boolean = withContext(Dispatchers.IO) {
        val url = "${basisUrl.trimEnd('/')}/api/events"
        try {
            val json = JSONObject()
                .put("typ", typ)
                .put("nachricht", nachricht)
                .put("kamera", kamera)
            if (kuhId != null) json.put("kuhId", kuhId)
            if (konfidenz != null) json.put("konfidenz", konfidenz)
            val body = json.toString().toRequestBody("application/json".toMediaType())
            http.newCall(
                Request.Builder().url(url)
                    .header("x-ingest-token", ingestToken)
                    .post(body).build()
            ).execute().use { it.isSuccessful }
        } catch (_: Exception) {
            false
        }
    }

    /** Laedt ein Einzelbild (Snapshot) als JPEG-Bytes, z.B. von der go2rtc-Bridge. */
    suspend fun ladeBytes(url: String): ByteArray = withContext(Dispatchers.IO) {
        http.newCall(Request.Builder().url(url).get().build()).execute().use { res ->
            if (!res.isSuccessful) throw Exception("HTTP ${res.code}")
            res.body?.bytes() ?: throw Exception("Leere Antwort")
        }
    }
}
