package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.stream.CameraRole
import com.example.stream.CameraState
import com.example.stream.CameraStreamView
import com.example.stream.label
import com.example.viewmodel.StallViewModel

/**
 * Live-Kameras (Android-Port des Stallblick-Hauptscreens StallblickApp.tsx).
 *
 * Alle Kamera-Container sind movableContentOf-Instanzen: Rollenwechsel und
 * Vollbild binden nur denselben Container an anderer Stelle ein – der
 * ExoPlayer bleibt erhalten, es gibt keinen Stream-Neuaufbau.
 */
@Composable
fun LiveScreen(
    viewModel: StallViewModel,
    onOpenWache: () -> Unit,
    onOpenStall3D: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val einstellungen by viewModel.streamSettings.collectAsState()
    val vollbild by viewModel.liveVollbild.collectAsState()
    val ereignisse by viewModel.liveEreignisse.collectAsState()
    val kameras = einstellungen.cameras

    var hauptkamera by rememberSaveable { mutableStateOf("stallwache") }
    val camStates = remember { mutableStateMapOf<String, CameraState>() }

    fun stateVon(id: String): CameraState = camStates[id]
        ?: if (einstellungen.isBridgeConfigured) CameraState.LAEDT else CameraState.OFFLINE

    fun kameraName(id: String) = einstellungen.cameraById(id).name

    /** Setzt eine bestimmte Kamera als Hauptbild (ohne Vollbild zu oeffnen). */
    fun setHaupt(id: String) {
        if (hauptkamera != id) {
            viewModel.addLiveEreignis("${kameraName(id)} ist jetzt Hauptbild")
            hauptkamera = id
        }
    }

    /** Wechselt reihum zur naechsten Kamera in der Liste (Rundlauf). */
    fun tauschen() {
        val idx = kameras.indexOfFirst { it.id == hauptkamera }
        val next = kameras[(idx + 1) % kameras.size].id
        viewModel.addLiveEreignis("${kameraName(next)} ist jetzt Hauptbild")
        hauptkamera = next
    }

    // Ein movableContent pro Kamera: bleibt beim Umsortieren/Vollbild gemountet.
    val kameraTiles = remember {
        mutableMapOf<String, @Composable (CameraRole) -> Unit>()
    }
    kameras.forEach { cam ->
        if (kameraTiles[cam.id] == null) {
            kameraTiles[cam.id] = movableContentOf { role: CameraRole ->
                val aktuelleEinstellungen by viewModel.streamSettings.collectAsState()
                CameraStreamView(
                    camera = aktuelleEinstellungen.cameraById(cam.id),
                    role = role,
                    settings = aktuelleEinstellungen,
                    httpClient = viewModel.cloudHttpClient,
                    resolveQuelle = viewModel::resolveStreamQuelle,
                    onState = { id, state ->
                        val vorher = camStates[id]
                        camStates[id] = state
                        if (vorher != state &&
                            (state == CameraState.ONLINE || state == CameraState.OFFLINE)
                        ) {
                            viewModel.addLiveEreignis(
                                "${kameraName(id)} ist ${state.label.lowercase()}"
                            )
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }

    val onlineCount = kameras.count { stateVon(it.id) == CameraState.ONLINE }
    val systemOk = onlineCount == kameras.size

    if (vollbild) {
        // ---- Vollbild: Hauptkamera fuellt den Screen, reduzierte Aktionen ----
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            Box(modifier = Modifier.weight(1f)) {
                kameraTiles[hauptkamera]?.invoke(CameraRole.HAUPT)
                StatusZeile(
                    name = kameraName(hauptkamera),
                    state = stateVon(hauptkamera),
                    istHaupt = true,
                    modifier = Modifier.padding(10.dp),
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AktionsButton("← Zurück", Modifier.weight(1f).testTag("vollbild_zurueck_btn")) {
                    viewModel.setLiveVollbild(false)
                }
                AktionsButton("Tauschen", Modifier.weight(1f)) { tauschen() }
                AktionsButton("Snapshot", Modifier.weight(1f)) {
                    viewModel.speichereSnapshot(hauptkamera)
                }
            }
        }
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // 1 · Header – nur Orientierung, kompakt
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF1E293B)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.Videocam,
                        contentDescription = null,
                        tint = Color(0xFF4ADE80),
                        modifier = Modifier.size(20.dp),
                    )
                }
                Column {
                    Text(
                        text = "Stallblick",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 19.sp,
                    )
                    Text(
                        text = if (systemOk) "${kameras.size} Kameras online"
                        else "$onlineCount von ${kameras.size} Kameras online",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 11.sp,
                    )
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Color(0xFF1E293B))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                systemOk -> Color(0xFF4ADE80)
                                onlineCount > 0 -> Color(0xFFFBBF24)
                                else -> Color(0xFFEF4444)
                            }
                        )
                )
                Text("System", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
            }
        }

        // 2+3 · Kamera-Karten: Hauptbild zuerst, Vorschau-Karten danach
        val sortiert = kameras.sortedBy { if (it.id == hauptkamera) 0 else 1 }
        sortiert.forEach { cam ->
            key(cam.id) {
                val istHaupt = cam.id == hauptkamera
                val state = stateVon(cam.id)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(18.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(if (istHaupt) 16f / 9f else 21f / 9f)
                            .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
                            .let {
                                if (istHaupt) it
                                    .clickable { viewModel.setLiveVollbild(true) }
                                    .testTag("live_hauptbild")
                                else it
                            },
                    ) {
                        kameraTiles[cam.id]?.invoke(
                            if (istHaupt) CameraRole.HAUPT else CameraRole.VORSCHAU
                        )
                        StatusZeile(
                            name = cam.name,
                            state = state,
                            istHaupt = istHaupt,
                            modifier = Modifier.padding(10.dp),
                        )
                    }

                    if (istHaupt) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            AktionsButton("Vollbild", Modifier.weight(1f).testTag("vollbild_btn")) {
                                viewModel.setLiveVollbild(true)
                            }
                            AktionsButton("Tauschen", Modifier.weight(1f).testTag("tauschen_btn")) {
                                tauschen()
                            }
                            AktionsButton("Snapshot", Modifier.weight(1f).testTag("snapshot_btn")) {
                                viewModel.speichereSnapshot(hauptkamera)
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = cam.ort,
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )
                            AktionsButton("Öffnen", klein = true) {
                                setHaupt(cam.id)
                                viewModel.setLiveVollbild(true)
                            }
                            AktionsButton(
                                "Als Hauptbild",
                                klein = true,
                                modifier = Modifier.testTag("haupt_${cam.id}_btn"),
                            ) {
                                setHaupt(cam.id)
                            }
                        }
                    }
                }
            }
        }

        // 4 · Statusblock – kompakt, unabhaengig vom Videostream
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            kameras.forEach { cam ->
                val state = stateVon(cam.id)
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "${cam.name} · ${if (cam.id == hauptkamera) "Hauptbild" else "Vorschau"}",
                            color = Color.White.copy(alpha = 0.4f),
                            fontSize = 8.sp,
                            letterSpacing = 0.5.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(statusFarbe(state))
                            )
                            Text(
                                text = state.label,
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
            }
        }

        // 5 · KI-Wache-Verweis (eigener Tab, laedt nichts vor)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onOpenWache() }
                .testTag("wache_link"),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            shape = RoundedCornerShape(12.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "KI-Wache",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Brunst- & Kalbeerkennung",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 11.sp,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.4f),
                    modifier = Modifier.size(16.dp),
                )
            }
        }

        // 5b · Stall-3D-Rundgang (three.js-Szene der Webapp im WebView)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onOpenStall3D() }
                .testTag("stall3d_link"),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            shape = RoundedCornerShape(12.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Stall in 3D",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Rundgang durch die Herde",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 11.sp,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.4f),
                    modifier = Modifier.size(16.dp),
                )
            }
        }

        // 6 · Letzte Ereignisse – nachgelagert geladen
        Column {
            Text(
                text = "LETZTE EREIGNISSE",
                color = Color.White.copy(alpha = 0.4f),
                fontSize = 9.sp,
                letterSpacing = 1.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (ereignisse.isEmpty()) {
                    Text(
                        text = "Noch keine Ereignisse.",
                        color = Color.White.copy(alpha = 0.4f),
                        fontSize = 11.sp,
                        modifier = Modifier.padding(12.dp),
                    )
                } else {
                    Column {
                        ereignisse.forEachIndexed { i, (zeit, text) ->
                            if (i > 0) {
                                HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                            }
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Text(
                                    text = zeit,
                                    color = Color.White.copy(alpha = 0.4f),
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                )
                                Text(
                                    text = text,
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 11.sp,
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

private fun statusFarbe(state: CameraState): Color = when (state) {
    CameraState.ONLINE -> Color(0xFF4ADE80)
    CameraState.OFFLINE -> Color(0xFFEF4444)
    CameraState.LAEDT -> Color(0xFFFBBF24)
    CameraState.INSTABIL -> Color(0xFFFB923C)
}

@Composable
private fun StatusZeile(
    name: String,
    state: CameraState,
    istHaupt: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (istHaupt && state == CameraState.ONLINE) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFFDC2626))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            ) {
                Text(
                    text = "LIVE",
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                )
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(Color.Black.copy(alpha = 0.7f))
                .padding(horizontal = 6.dp, vertical = 2.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(statusFarbe(state))
            )
            Text(
                text = "$name · ${state.label}",
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
        if (!istHaupt) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.Black.copy(alpha = 0.7f))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            ) {
                Text(
                    text = "Vorschau",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 10.sp,
                )
            }
        }
    }
}

@Composable
private fun AktionsButton(
    text: String,
    modifier: Modifier = Modifier,
    klein: Boolean = false,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        color = Color.White.copy(alpha = 0.1f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
        shape = RoundedCornerShape(12.dp),
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = if (klein) 11.sp else 13.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.padding(
                horizontal = if (klein) 10.dp else 12.dp,
                vertical = if (klein) 8.dp else 12.dp,
            ),
        )
    }
}
