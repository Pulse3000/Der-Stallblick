package com.example.stream

import java.net.URLEncoder

/**
 * Zentrale Kamera- und Bridge-Konfiguration von Stallblick (Android-Port von
 * lib/config.ts aus dem Die-Stallwache-Repo).
 *
 * Kameras liefern lokal RTSP-Streams. Im Stall-Netz laeuft eine Bridge
 * (go2rtc ODER MediaMTX), die via Cloudflare Tunnel oeffentlich per HTTPS
 * erreichbar ist. Die App spricht ausschliesslich mit der Bridge – niemals
 * direkt mit den Kameras, keine Kamera-Zugangsdaten in der App.
 * Tuya-faehige Kameras (Futterwache, Stallbox) laufen stattdessen primaer
 * ueber die Tuya-Cloud (Webapp-Endpoint oder direkte Tuya-OpenAPI), mit der
 * Bridge nur als Fallback.
 *
 * Anders als im Browser gibt es hier kein CORS und kein hls.js: ExoPlayer
 * spielt die HLS-Playlists der Bridge und der Tuya-Cloud direkt ab.
 */

enum class BridgeType { GO2RTC, MEDIAMTX }

/** Kamera-State laut State-Modell der Webapp: online | offline | laedt | instabil */
enum class CameraState { ONLINE, OFFLINE, LAEDT, INSTABIL }

val CameraState.label: String
    get() = when (this) {
        CameraState.ONLINE -> "Online"
        CameraState.OFFLINE -> "Offline"
        CameraState.LAEDT -> "Lädt"
        CameraState.INSTABIL -> "Instabil"
    }

data class CameraConfig(
    /** stallwache | futterwache | stallbox */
    val id: String,
    /** Anzeigename in der UI. */
    val name: String,
    /** Name des Streams (go2rtc) bzw. Pfads (MediaMTX). */
    val streamName: String,
    /** Reduzierte Metadaten fuer die Vorschau-Karte. */
    val ort: String,
    /** Kann diese Kamera als Hauptbild ueber die Tuya-Cloud laufen? */
    val tuyaFaehig: Boolean,
    /** API-Route der Webapp, die serverseitig eine kurzlebige Tuya-HLS-URL allokiert. */
    val tuyaEndpoint: String,
)

/**
 * Vom Betreiber konfigurierbare Stream-Einstellungen (Settings-Screen),
 * Spiegel der NEXT_PUBLIC_*-Umgebungsvariablen der Webapp.
 */
data class StreamSettings(
    /** Basis-URL der Bridge (Cloudflare-Tunnel oder LAN), ohne abschliessenden Slash. */
    val bridgeUrl: String = "",
    val bridgeType: BridgeType = BridgeType.GO2RTC,
    val streamNameStallwache: String = "stallwache",
    val streamNameFutterwache: String = "futterwache",
    val streamNameStallbox: String = "stallbox",
    /** Deployte Stallblick-Webapp (Tuya-Streams, KI-Wache-Events, Login). */
    val webappUrl: String = DEFAULT_WEBAPP_URL,
    /** Futterwache/Stallbox als Hauptbild ueber die Tuya-Cloud (mit Bridge-Fallback). */
    val futterwacheTuya: Boolean = true,
    val stallboxTuya: Boolean = true,
) {
    companion object {
        const val DEFAULT_WEBAPP_URL = "https://die-stallwache.vercel.app"
    }

    val bridgeUrlClean: String get() = bridgeUrl.trim().trimEnd('/')
    val webappUrlClean: String get() = webappUrl.trim().trimEnd('/')

    val isBridgeConfigured: Boolean get() = bridgeUrlClean.isNotEmpty()
    val isWebappConfigured: Boolean get() = webappUrlClean.isNotEmpty()

    /** MediaMTX hat kein eingebautes JPEG-Snapshot-Endpoint. */
    val snapshotSupported: Boolean get() = bridgeType == BridgeType.GO2RTC

    /** Stallwache = Hauptkamera (Default), weitere Kameras als Zweitkameras. */
    val cameras: List<CameraConfig>
        get() = listOf(
            CameraConfig(
                id = "stallwache",
                name = "Stallwache",
                streamName = streamNameStallwache.trim().ifEmpty { "stallwache" },
                ort = "Abkalbebereich",
                tuyaFaehig = false,
                tuyaEndpoint = "",
            ),
            CameraConfig(
                id = "futterwache",
                name = "Futterwache",
                streamName = streamNameFutterwache.trim().ifEmpty { "futterwache" },
                ort = "Futtertisch",
                tuyaFaehig = futterwacheTuya,
                tuyaEndpoint = "/api/futterwache/stream",
            ),
            CameraConfig(
                id = "stallbox",
                name = "Stallbox",
                streamName = streamNameStallbox.trim().ifEmpty { "stallbox" },
                ort = "Stallbox",
                tuyaFaehig = stallboxTuya,
                tuyaEndpoint = "/api/stallbox/stream",
            ),
        )

    fun cameraById(id: String): CameraConfig =
        cameras.find { it.id == id } ?: cameras.first()

    private fun enc(s: String): String = URLEncoder.encode(s, "UTF-8")

    /** HLS-Playlist der Bridge – Standardweg fuer ExoPlayer. */
    fun hlsUrl(streamName: String): String =
        if (bridgeType == BridgeType.MEDIAMTX)
            "$bridgeUrlClean/${enc(streamName)}/index.m3u8"
        else
            "$bridgeUrlClean/api/stream.m3u8?src=${enc(streamName)}"

    /**
     * Einzelbild (Snapshot) – Grundlage der ressourcenschonenden Vorschau.
     * Nur bei go2rtc verfuegbar; bei MediaMTX siehe [snapshotSupported].
     */
    fun snapshotUrl(streamName: String): String =
        "$bridgeUrlClean/api/frame.jpeg?src=${enc(streamName)}"
}
