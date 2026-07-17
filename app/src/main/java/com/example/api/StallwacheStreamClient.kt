package com.example.api

import com.example.data.KameraKonfig
import com.example.data.StreamEinstellungen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.CacheControl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * HTTP-Client fuer die Kamera-Streams der Stallwache.
 *
 * Gegenstueck zu den Fetches in components/CameraStream.tsx der Web-App:
 *  - Tuya-faehige Kameras: GET auf den Allokations-Endpoint der
 *    Stallwache-Web-App (/api/futterwache/stream bzw. /api/stallbox/stream)
 *    liefert { url } mit einer kurzlebigen HLS-URL. Die URL zeigt auf den
 *    Same-Origin-Proxy der Web-App und wird hier gegen deren Basis-URL
 *    aufgeloest. Tuya-URLs laufen ab – bei fatalem Player-Fehler wird eine
 *    frische URL geholt.
 *  - MediaMTX-Vorschau: kein JPEG-Snapshot vorhanden, daher nur ein leichtes
 *    HEAD-Status-Polling auf die HLS-Playlist (kein Videodecode).
 */
object StallwacheStreamClient {

    class TuyaStreamFehler(nachricht: String) : Exception(nachricht)

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    /**
     * Holt eine frische, kurzlebige Tuya-HLS-URL fuer eine tuyaFaehige Kamera.
     * Wirft [TuyaStreamFehler], wenn der Endpoint geschlossen (503), die
     * Anfrage fehlgeschlagen (502) oder keine URL enthalten ist – der
     * Aufrufer faellt dann auf die Bridge zurueck.
     */
    suspend fun holeTuyaStreamUrl(
        einstellungen: StreamEinstellungen,
        kamera: KameraKonfig,
    ): String = withContext(Dispatchers.IO) {
        val endpoint = einstellungen.tuyaEndpointUrl(kamera)
            ?: throw TuyaStreamFehler("Kamera ${kamera.id} hat keinen Tuya-Endpoint")
        val request = Request.Builder()
            .url(endpoint)
            .cacheControl(CacheControl.Builder().noStore().build())
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw TuyaStreamFehler("Tuya HTTP ${response.code}")
            val body = response.body?.string() ?: throw TuyaStreamFehler("Tuya ohne Antwort")
            val url = JSONObject(body).optString("url")
            if (url.isEmpty()) throw TuyaStreamFehler("Tuya ohne URL")
            einstellungen.aufgeloesteStreamUrl(url)
        }
    }

    /** Leichter Status-Ping auf eine HLS-Playlist (MediaMTX-Vorschau ohne Snapshot). */
    suspend fun hlsErreichbar(url: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(url)
                .head()
                .cacheControl(CacheControl.Builder().noStore().build())
                .build()
            client.newCall(request).execute().use { it.isSuccessful }
        } catch (_: Exception) {
            false
        }
    }
}
