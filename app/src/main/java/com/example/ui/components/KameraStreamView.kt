package com.example.ui.components

import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.example.api.StallwacheStreamClient
import com.example.data.KameraKonfig
import com.example.data.KameraRolle
import com.example.data.KameraState
import com.example.data.StreamEinstellungen
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.suspendCancellableCoroutine
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import kotlin.coroutines.resume
import kotlin.math.min

/** Vorschau: Einzelbilder statt Live-Stream – reduzierte Stream-Prioritaet. */
private const val VORSCHAU_INTERVALL_OK = 5_000L
private const val VORSCHAU_INTERVALL_FEHLER = 15_000L
/** Kopfstart der Hauptkamera: Vorschau beginnt erst kurz danach. */
private const val VORSCHAU_STARTVERZOEGERUNG = 600L

/**
 * Ein Kamera-Container fuer Stallblick – Android-Portierung von
 * components/CameraStream.tsx der Stallwache-Web-App.
 *
 * Quelle Bridge (go2rtc ODER MediaMTX, siehe [StreamEinstellungen.bridgeTyp]):
 *   Rolle HAUPT    → HLS ueber Media3/ExoPlayer mit automatischem Reconnect
 *                    (exponentieller Backoff, max. 30 s). Das Web nutzt
 *                    zusaetzlich WebRTC mit HLS-Fallback; in der App ist HLS
 *                    der robuste Standardweg.
 *   Rolle VORSCHAU → go2rtc: Snapshot-Polling ueber frame.jpeg (leichtgewichtig).
 *                    MediaMTX: kein JPEG-Snapshot vorhanden – daher nur ein
 *                    leichtes HEAD-Status-Polling (kein Videodecode) plus
 *                    ruhiger Platzhalter statt Thumbnail.
 *
 * Quelle Tuya-Cloud (kamera.tuyaFaehig, z.B. Futterwache/Stallbox):
 *   Rolle HAUPT    → kurzlebige HLS-URL vom Stallwache-Endpoint; bei
 *                    503/Fehler automatischer Fallback auf die Bridge.
 *                    Tuya-URLs laufen ab → bei fatalem Player-Fehler wird
 *                    eine frische URL geholt.
 *   Rolle VORSCHAU → wie Bridge-Vorschau oben (go2rtc-Snapshot wenn
 *                    verfuegbar, sonst ruhiger Platzhalter).
 *
 * Statuswechsel (online | offline | laedt | instabil) werden ueber [onState]
 * nach oben gemeldet (Header, Statusblock, Ereignisse).
 */
@Composable
fun KameraStreamView(
    kamera: KameraKonfig,
    rolle: KameraRolle,
    einstellungen: StreamEinstellungen,
    onState: (String, KameraState) -> Unit,
    modifier: Modifier = Modifier,
    neuLadenTrigger: Int = 0,
) {
    val kontext = LocalContext.current

    var videoLive by remember(kamera.id) { mutableStateOf(false) }
    var snapUrl by remember(kamera.id) { mutableStateOf<String?>(null) }
    var snapSichtbar by remember(kamera.id) { mutableStateOf(false) }
    var warLive by remember(kamera.id) { mutableStateOf(false) }

    // Letzten gemeldeten State deduplizieren (wie stateRef im Web).
    val letzterState = remember(kamera.id) { mutableStateOf<KameraState?>(null) }
    val aktuellesOnState by rememberUpdatedState(onState)
    fun melde(state: KameraState) {
        if (letzterState.value == state) return
        letzterState.value = state
        aktuellesOnState(kamera.id, state)
    }

    val exoPlayer = remember(kamera.id, rolle == KameraRolle.HAUPT) {
        if (rolle == KameraRolle.HAUPT)
            ExoPlayer.Builder(kontext).build().apply { volume = 0f } // stumm wie im Web
        else null
    }
    DisposableEffect(exoPlayer) {
        onDispose { exoPlayer?.release() }
    }

    if (!einstellungen.kameraBedienbar(kamera)) {
        // Weder Bridge noch Tuya konfiguriert -> Kachel bleibt offline.
        LaunchedEffect(kamera.id) { melde(KameraState.OFFLINE) }
    } else if (rolle == KameraRolle.HAUPT && exoPlayer != null) {
        // ---- Haupt-Modus: Live-Stream mit hoechster Prioritaet ----
        LaunchedEffect(kamera.id, einstellungen, neuLadenTrigger) {
            var versuche = 0
            // Tuya fehlgeschlagen -> fuer diese Haupt-Session auf die Bridge bleiben.
            var tuyaAufgegeben = false

            while (isActive) {
                videoLive = false
                melde(if (warLive) KameraState.INSTABIL else KameraState.LAEDT)

                var url: String? = null
                if (kamera.tuyaFaehig && !tuyaAufgegeben && einstellungen.stallwacheKonfiguriert) {
                    url = try {
                        StallwacheStreamClient.holeTuyaStreamUrl(einstellungen, kamera)
                    } catch (_: Exception) {
                        // Tuya nicht verfuegbar -> fuer diese Session auf die Bridge
                        // umschalten (nur wenn es eine Bridge gibt, sonst weiter probieren).
                        if (einstellungen.bridgeKonfiguriert) tuyaAufgegeben = true
                        null
                    }
                }
                if (url == null && einstellungen.bridgeKonfiguriert) {
                    url = einstellungen.hlsUrl(kamera.streamName)
                }

                if (url != null) {
                    // Suspendiert waehrend der Wiedergabe; kehrt erst bei fatalem
                    // Player-Fehler zurueck (Tuya-URLs laufen ab -> naechste Runde
                    // holt eine frische URL).
                    exoPlayer.spieleUndUeberwache(url) {
                        versuche = 0
                        warLive = true
                        videoLive = true
                        melde(KameraState.ONLINE)
                    }
                    videoLive = false
                }

                melde(if (versuche >= 2) KameraState.OFFLINE else KameraState.INSTABIL)
                val wartezeit = min(30_000L, 2_000L * (1L shl min(versuche, 10)))
                versuche += 1
                delay(wartezeit)
            }
        }
    } else if (rolle == KameraRolle.VORSCHAU) {
        if (einstellungen.snapshotVerfuegbar) {
            // ---- Vorschau via go2rtc-Snapshot ----
            LaunchedEffect(kamera.id, einstellungen, letzterState) {
                melde(if (warLive) KameraState.ONLINE else KameraState.LAEDT)
                delay(VORSCHAU_STARTVERZOEGERUNG)
                while (isActive) {
                    snapUrl = "${einstellungen.snapshotUrl(kamera.streamName)}&t=${System.currentTimeMillis()}"
                    delay(
                        if (letzterState.value == KameraState.ONLINE) VORSCHAU_INTERVALL_OK
                        else VORSCHAU_INTERVALL_FEHLER
                    )
                }
            }
        } else if (einstellungen.bridgeKonfiguriert) {
            // ---- MediaMTX: kein Snapshot-Endpoint – leichtes HEAD-Polling ----
            LaunchedEffect(kamera.id, einstellungen) {
                while (isActive) {
                    val erreichbar =
                        StallwacheStreamClient.hlsErreichbar(einstellungen.hlsUrl(kamera.streamName))
                    melde(if (erreichbar) KameraState.ONLINE else KameraState.OFFLINE)
                    delay(if (erreichbar) VORSCHAU_INTERVALL_OK else VORSCHAU_INTERVALL_FEHLER)
                }
            }
        } else {
            // ---- Tuya-only: ruhiger Platzhalter, kein zweiter Live-Stream ----
            LaunchedEffect(kamera.id) {
                melde(if (warLive) KameraState.ONLINE else KameraState.LAEDT)
            }
        }
    }

    Box(modifier = modifier.background(Color.Black)) {
        // Letztes Standbild bleibt als ruhiger Hintergrund erhalten.
        if (snapUrl != null) {
            AsyncImage(
                model = snapUrl,
                contentDescription = "${kamera.name} – Vorschau",
                onSuccess = {
                    snapSichtbar = true
                    if (rolle == KameraRolle.VORSCHAU) melde(KameraState.ONLINE)
                },
                onError = {
                    if (rolle == KameraRolle.VORSCHAU) melde(KameraState.OFFLINE)
                },
                modifier = Modifier.fillMaxSize(),
                alpha = if (snapSichtbar && !videoLive) 1f else 0f,
            )
        }

        if (rolle == KameraRolle.HAUPT && exoPlayer != null) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        useController = false
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT,
                        )
                    }
                },
                update = { view -> view.player = exoPlayer },
                modifier = Modifier.fillMaxSize(),
            )
        }

        val platzhalterText = when {
            !einstellungen.kameraBedienbar(kamera) ->
                "Warte auf Bridge – sobald die Bridge-URL in den Einstellungen gesetzt ist, erscheint hier das Bild der ${kamera.name}."
            rolle == KameraRolle.VORSCHAU && !einstellungen.snapshotVerfuegbar ->
                "${kamera.name} · Vorschau"
            rolle == KameraRolle.HAUPT && !videoLive && kamera.tuyaFaehig && !einstellungen.bridgeKonfiguriert ->
                "${kamera.name} über Tuya-Cloud – verbinde…"
            else -> null
        }
        if (platzhalterText != null && !videoLive && !snapSichtbar) {
            Box(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = platzhalterText,
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.widthIn(max = 280.dp),
                )
            }
        }
    }
}

/**
 * Spielt eine HLS-URL und bleibt suspendiert, solange die Wiedergabe laeuft.
 * [beiLive] feuert, sobald der Player bereit ist. Rueckkehr erst bei fatalem
 * Player-Fehler – der Aufrufer entscheidet dann ueber Reconnect/Fallback.
 */
private suspend fun ExoPlayer.spieleUndUeberwache(
    url: String,
    beiLive: () -> Unit,
) = suspendCancellableCoroutine { fortsetzung ->
    val listener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_READY) beiLive()
        }

        override fun onPlayerError(error: PlaybackException) {
            removeListener(this)
            if (fortsetzung.isActive) fortsetzung.resume(Unit)
        }
    }
    addListener(listener)
    setMediaItem(MediaItem.fromUri(url))
    prepare()
    playWhenReady = true
    fortsetzung.invokeOnCancellation {
        removeListener(listener)
        stop()
    }
}
