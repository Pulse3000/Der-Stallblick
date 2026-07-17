package com.example.data

import java.net.URLEncoder

/**
 * Zentrale Kamera-Konfiguration von Stallblick.
 *
 * Portierung von lib/config.ts aus der Stallwache-Web-App (Die_Stallwache):
 * Kameras liefern lokal RTSP-Streams. Im Stall-Netz laeuft eine Bridge
 * (go2rtc ODER MediaMTX), die via Cloudflare Tunnel oeffentlich per HTTPS
 * erreichbar ist. Die App spricht ausschliesslich mit der Bridge – niemals
 * direkt mit den Kameras, ohne Zugangsdaten in der App.
 * Tuya-faehige Kameras (siehe [KameraKonfig.tuyaFaehig]) laufen stattdessen
 * primaer ueber die Tuya-Cloud (kurzlebige HLS-URL von der Stallwache-Web-App
 * allokiert), mit der Bridge nur als Fallback.
 *
 * Statt NEXT_PUBLIC_*-Umgebungsvariablen kommen die Werte hier aus den
 * App-Einstellungen (SharedPreferences, siehe StallViewModel).
 */

enum class BridgeTyp(val id: String) {
    GO2RTC("go2rtc"),
    MEDIAMTX("mediamtx");

    companion object {
        fun vonId(id: String?): BridgeTyp =
            if (id?.trim()?.lowercase() == MEDIAMTX.id) MEDIAMTX else GO2RTC
    }
}

/** Kamera-State laut State-Modell der Web-App: online | offline | laedt | instabil */
enum class KameraState {
    ONLINE, OFFLINE, LAEDT, INSTABIL
}

/** Rolle einer Kamera-Kachel: Hauptbild (Live-Stream) oder Vorschau (Snapshots). */
enum class KameraRolle {
    HAUPT, VORSCHAU
}

data class KameraKonfig(
    val id: String,
    /** Anzeigename in der UI. */
    val name: String,
    /** Name des Streams (go2rtc) bzw. Pfads (MediaMTX). */
    val streamName: String,
    /** Reduzierte Metadaten fuer die Vorschau-Karte. */
    val ort: String,
    /**
     * Kann diese Kamera als Hauptbild ueber die Tuya-Cloud laufen?
     * Wenn true, holt der Player zuerst eine HLS-URL vom [tuyaEndpoint]
     * der Stallwache-Web-App und faellt bei Fehlern auf die Bridge zurueck.
     */
    val tuyaFaehig: Boolean,
    /** API-Pfad der Stallwache-Web-App, der serverseitig eine kurzlebige Tuya-HLS-URL allokiert. */
    val tuyaEndpoint: String,
)

/** Stallwache = Hauptkamera (Default), weitere Kameras als Zweitkameras. */
val KAMERAS: List<KameraKonfig> = listOf(
    KameraKonfig(
        id = "stallwache",
        name = "Stallwache",
        streamName = "stallwache",
        ort = "Abkalbebereich",
        tuyaFaehig = false,
        tuyaEndpoint = "",
    ),
    KameraKonfig(
        id = "futterwache",
        name = "Futterwache",
        streamName = "futterwache",
        ort = "Futtertisch",
        tuyaFaehig = true,
        tuyaEndpoint = "/api/futterwache/stream",
    ),
    KameraKonfig(
        id = "stallbox",
        name = "Stallbox",
        streamName = "stallbox",
        ort = "Stallbox",
        tuyaFaehig = true,
        tuyaEndpoint = "/api/stallbox/stream",
    ),
)

fun kameraNachId(id: String): KameraKonfig = KAMERAS.find { it.id == id } ?: KAMERAS[0]

/**
 * Vom Betreiber konfigurierte Stream-Quellen (Einstellungen-Screen).
 *
 * @param bridgeUrl     Basis-URL der Bridge (Cloudflare-Tunnel), z.B. https://stallwache.example.com
 * @param bridgeTyp     go2rtc (Default) oder mediamtx
 * @param stallwacheUrl Basis-URL der Stallwache-Web-App (Vercel), liefert die Tuya-Stream-Endpoints
 */
data class StreamEinstellungen(
    val bridgeUrl: String = "",
    val bridgeTyp: BridgeTyp = BridgeTyp.GO2RTC,
    val stallwacheUrl: String = "",
) {
    private val bridgeBasis = bridgeUrl.trim().trimEnd('/')
    private val stallwacheBasis = stallwacheUrl.trim().trimEnd('/')

    val bridgeKonfiguriert: Boolean get() = bridgeBasis.isNotEmpty()
    val stallwacheKonfiguriert: Boolean get() = stallwacheBasis.isNotEmpty()

    /** MediaMTX hat kein eingebautes JPEG-Snapshot-Endpoint. */
    val snapshotVerfuegbar: Boolean get() = bridgeKonfiguriert && bridgeTyp == BridgeTyp.GO2RTC

    /** Kamera ist bedienbar, wenn Bridge konfiguriert ODER Tuya (via Stallwache-App) moeglich ist. */
    fun kameraBedienbar(kamera: KameraKonfig): Boolean =
        bridgeKonfiguriert || (kamera.tuyaFaehig && stallwacheKonfiguriert)

    private fun encode(streamName: String): String = URLEncoder.encode(streamName, "UTF-8")

    /** HLS-Playlist der Bridge (Haupt-Wiedergabe in der App; im Web der WebRTC-Fallback). */
    fun hlsUrl(streamName: String): String =
        if (bridgeTyp == BridgeTyp.MEDIAMTX)
            "$bridgeBasis/${encode(streamName)}/index.m3u8"
        else
            "$bridgeBasis/api/stream.m3u8?src=${encode(streamName)}"

    /** Einzelbild (Snapshot) – Grundlage der ressourcenschonenden Vorschau (nur go2rtc). */
    fun snapshotUrl(streamName: String): String =
        "$bridgeBasis/api/frame.jpeg?src=${encode(streamName)}"

    /** Voll qualifizierter Tuya-Allokations-Endpoint dieser Kamera, oder null. */
    fun tuyaEndpointUrl(kamera: KameraKonfig): String? =
        if (kamera.tuyaFaehig && stallwacheKonfiguriert) stallwacheBasis + kamera.tuyaEndpoint
        else null

    /**
     * Loest eine von der Stallwache-App gelieferte Stream-URL auf. Die
     * Tuya-Endpoints liefern bewusst relative Proxy-URLs
     * (/api/futterwache/proxy?...), die gegen die Stallwache-Basis
     * aufgeloest werden muessen.
     */
    fun aufgeloesteStreamUrl(url: String): String =
        if (url.startsWith("http://") || url.startsWith("https://")) url
        else stallwacheBasis + (if (url.startsWith("/")) url else "/$url")
}
