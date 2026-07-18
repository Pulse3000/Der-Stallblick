package com.example.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Client fuer die deployte Stallblick-Webapp (Die-Stallwache-Repo auf Vercel).
 *
 * Bildet die Serverfunktionen der Webapp fuer die native App ab:
 *   - GET  /api/events           Ereignisliste der KI-Wache (neueste zuerst)
 *   - GET  /api/<kamera>/stream  kurzlebige Tuya-HLS-URL (laeuft ueber den
 *                                CORS-Proxy der Webapp)
 *   - POST /api/events           Ingest mit x-ingest-token (Edge-Agent-Weg)
 *
 * Kein Passwort-Login: Die App greift ohne Session auf die Webapp zu und
 * erwartet, dass die Webapp offen betrieben wird (STALLBLICK_PASSWORT nicht
 * gesetzt). Ist die Webapp doch geschuetzt (HTTP 401), schlagen die
 * Cloud-Aufrufe fehl und die Streams laufen ueber die direkte
 * Tuya-OpenAPI bzw. die Bridge weiter.
 */
class StallblickCloudClient {

    /** Gemeinsamer HTTP-Client – auch ExoPlayer nutzt ihn fuer Proxy-Streams. */
    val http: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

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

    suspend fun holeEreignisse(basisUrl: String): CloudEreignisse =
        withContext(Dispatchers.IO) {
            val url = "${basisUrl.trimEnd('/')}/api/events"
            http.newCall(Request.Builder().url(url).get().build()).execute().use { res ->
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
