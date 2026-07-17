package com.example.stream

import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

/** Rolle einer Kamera-Kachel: grosses Hauptbild oder ressourcenschonende Vorschau. */
enum class CameraRole { HAUPT, VORSCHAU }

/** Aufgeloeste Stream-Quelle fuer das Hauptbild. */
data class StreamQuelle(
    val url: String,
    /** true = kurzlebige Tuya-URL (bei Fehler frische URL holen). */
    val istTuya: Boolean,
)

/** Vorschau: Einzelbilder statt Live-Stream – reduzierte Stream-Prioritaet. */
private const val PREVIEW_INTERVAL_OK = 5_000L
private const val PREVIEW_INTERVAL_ERR = 15_000L

/** Kopfstart der Hauptkamera: Vorschau beginnt erst kurz danach. */
private const val PREVIEW_INITIAL_DELAY = 600L

/**
 * Ein Kamera-Container fuer Stallblick (Android-Port von CameraStream.tsx).
 *
 * Rolle HAUPT    -> ExoPlayer spielt die HLS-Quelle mit hoechster Prioritaet:
 *                   Tuya-Cloud fuer tuyaFaehige Kameras (via [resolveQuelle],
 *                   mit automatischem Bridge-Fallback), sonst Bridge-HLS.
 *                   Reconnect mit exponentiellem Backoff; abgelaufene
 *                   Tuya-URLs werden bei jedem Versuch frisch geholt.
 * Rolle VORSCHAU -> go2rtc: Snapshot-Polling (leichtgewichtig, kein Decode).
 *                   MediaMTX: kein JPEG-Snapshot vorhanden – nur ein leichtes
 *                   HEAD-Status-Polling plus ruhiger Platzhalter.
 *                   Tuya ohne Bridge: ruhiger Platzhalter, kein zweiter
 *                   Live-Stream.
 */
@OptIn(UnstableApi::class)
@Composable
fun CameraStreamView(
    camera: CameraConfig,
    role: CameraRole,
    settings: StreamSettings,
    httpClient: OkHttpClient,
    resolveQuelle: suspend (CameraConfig, Boolean) -> StreamQuelle?,
    onState: (String, CameraState) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val aktuellerOnState by rememberUpdatedState(onState)

    var player by remember(camera.id) { mutableStateOf<ExoPlayer?>(null) }
    var videoLive by remember(camera.id) { mutableStateOf(false) }
    var wasLive by remember(camera.id) { mutableStateOf(false) }
    var snapUrl by remember(camera.id) { mutableStateOf<String?>(null) }
    var snapVisible by remember(camera.id) { mutableStateOf(false) }
    var snapFehler by remember(camera.id) { mutableLongStateOf(0L) }
    var letzterState by remember(camera.id) { mutableStateOf<CameraState?>(null) }

    fun report(s: CameraState) {
        if (letzterState == s) return
        letzterState = s
        aktuellerOnState(camera.id, s)
    }

    // Kamera ist bedienbar, wenn Bridge konfiguriert ODER Tuya moeglich ist.
    val bedienbar = settings.isBridgeConfigured ||
        (camera.tuyaFaehig && (settings.isWebappConfigured || camera.tuyaEndpoint.isNotEmpty()))

    LaunchedEffect(role, settings, camera.id) {
        player?.release()
        player = null
        videoLive = false

        if (!settings.isBridgeConfigured && !camera.tuyaFaehig) {
            report(CameraState.OFFLINE)
            return@LaunchedEffect
        }

        if (role == CameraRole.VORSCHAU) {
            when {
                settings.isBridgeConfigured && settings.snapshotSupported -> {
                    // ---- Vorschau via go2rtc-Snapshot ----
                    report(if (wasLive) CameraState.ONLINE else CameraState.LAEDT)
                    delay(PREVIEW_INITIAL_DELAY)
                    while (isActive) {
                        snapUrl = "${settings.snapshotUrl(camera.streamName)}&t=${System.currentTimeMillis()}"
                        delay(if (snapFehler == 0L) PREVIEW_INTERVAL_OK else PREVIEW_INTERVAL_ERR)
                    }
                }

                settings.isBridgeConfigured -> {
                    // ---- MediaMTX: kein Snapshot-Endpoint – leichtes HEAD-Polling ----
                    while (isActive) {
                        val ok = withContext(Dispatchers.IO) {
                            try {
                                httpClient.newCall(
                                    Request.Builder()
                                        .url(settings.hlsUrl(camera.streamName))
                                        .head().build()
                                ).execute().use { it.isSuccessful }
                            } catch (_: Exception) {
                                false
                            }
                        }
                        report(if (ok) CameraState.ONLINE else CameraState.OFFLINE)
                        delay(if (ok) PREVIEW_INTERVAL_OK else PREVIEW_INTERVAL_ERR)
                    }
                }

                else -> {
                    // ---- Tuya-only: ruhiger Platzhalter, kein zweiter Live-Stream ----
                    report(if (wasLive) CameraState.ONLINE else CameraState.LAEDT)
                }
            }
            return@LaunchedEffect
        }

        // ---- Haupt-Modus: Live-Stream mit hoechster Prioritaet ----
        var versuche = 0
        // Tuya fehlgeschlagen -> fuer diese Haupt-Session auf die Bridge bleiben.
        var tuyaErlaubt = camera.tuyaFaehig

        while (isActive) {
            report(if (wasLive) CameraState.INSTABIL else CameraState.LAEDT)

            val quelle = resolveQuelle(camera, tuyaErlaubt)
            if (quelle == null) {
                // Tuya nicht verfuegbar → fuer diese Session auf die Bridge umschalten.
                if (tuyaErlaubt && settings.isBridgeConfigured) {
                    tuyaErlaubt = false
                    continue
                }
                report(if (versuche >= 2) CameraState.OFFLINE else CameraState.INSTABIL)
                val warte = minOf(30_000L, 2_000L * (1L shl minOf(versuche, 4)))
                versuche += 1
                delay(warte)
                continue
            }

            val fehler = CompletableDeferred<Unit>()
            val exo = ExoPlayer.Builder(context)
                .setMediaSourceFactory(
                    DefaultMediaSourceFactory(OkHttpDataSource.Factory(httpClient))
                )
                .build()
            exo.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    if (state == Player.STATE_READY) {
                        versuche = 0
                        wasLive = true
                        videoLive = true
                        report(CameraState.ONLINE)
                    }
                }

                override fun onPlayerError(error: PlaybackException) {
                    fehler.complete(Unit)
                }
            })
            exo.setMediaItem(
                MediaItem.Builder()
                    .setUri(quelle.url)
                    .setMimeType(MimeTypes.APPLICATION_M3U8)
                    .build()
            )
            exo.volume = 0f
            exo.playWhenReady = true
            exo.prepare()
            player = exo

            // Bis zum fatalen Fehler abspielen; danach Backoff + frische URL
            // (Tuya-URLs laufen ab -> naechster Schleifendurchlauf holt neu).
            fehler.await()

            videoLive = false
            player = null
            exo.release()
            if (!isActive) break
            report(if (versuche >= 2) CameraState.OFFLINE else CameraState.INSTABIL)
            val warte = minOf(30_000L, 2_000L * (1L shl minOf(versuche, 4)))
            versuche += 1
            delay(warte)
        }
    }

    DisposableEffect(camera.id) {
        onDispose {
            player?.release()
            player = null
        }
    }

    Box(modifier = modifier.background(Color.Black)) {
        // Letztes Standbild bleibt als ruhiger Hintergrund erhalten.
        if (snapUrl != null) {
            AsyncImage(
                model = snapUrl,
                contentDescription = "${camera.name} – Vorschau",
                onSuccess = {
                    snapVisible = true
                    snapFehler = 0L
                    if (role == CameraRole.VORSCHAU) report(CameraState.ONLINE)
                },
                onError = {
                    snapFehler = System.currentTimeMillis()
                    if (role == CameraRole.VORSCHAU) report(CameraState.OFFLINE)
                },
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(if (snapVisible && !videoLive) 1f else 0f),
            )
        }

        if (role == CameraRole.HAUPT) {
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
                update = { view -> view.player = player },
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(if (videoLive) 1f else 0f),
            )
        }

        // Bridge nicht konfiguriert (nur fuer reine Bridge-Kameras)
        if (!bedienbar || (!settings.isBridgeConfigured && !camera.tuyaFaehig)) {
            Platzhalter(
                "Warte auf Bridge – sobald die Bridge-URL in der Konfiguration " +
                    "gesetzt ist, erscheint hier das Bild der ${camera.name}."
            )
        } else if (role == CameraRole.VORSCHAU && settings.isBridgeConfigured && !settings.snapshotSupported) {
            // MediaMTX-Vorschau: kein Snapshot-Endpoint -> ruhiger Platzhalter
            Platzhalter("${camera.name} · Vorschau")
        } else if (camera.tuyaFaehig && !settings.isBridgeConfigured && !videoLive && !snapVisible) {
            // Tuya-Kamera ohne Bridge: ruhiger Hinweis, solange kein Livebild steht
            Platzhalter(
                if (role == CameraRole.HAUPT) "${camera.name} über Tuya-Cloud – verbinde…"
                else "${camera.name} · Vorschau"
            )
        }
    }
}

@Composable
private fun Platzhalter(text: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1E293B)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(16.dp),
        )
    }
}
