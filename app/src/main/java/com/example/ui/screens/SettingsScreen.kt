package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.stream.BridgeType
import com.example.viewmodel.StallViewModel

@Composable
fun SettingsScreen(
    viewModel: StallViewModel,
    modifier: Modifier = Modifier
) {
    val edgeHost by viewModel.edgeHost.collectAsState()
    val edgeToken by viewModel.edgeToken.collectAsState()
    val customApiKey by viewModel.customApiKey.collectAsState()
    val globalWatchMode by viewModel.wachModusGlobal.collectAsState()
    val cooldownMinutes by viewModel.cooldownMinutes.collectAsState()
    val edgeStatus by viewModel.edgeStatus.collectAsState()
    val selectedTheme by viewModel.selectedTheme.collectAsState()
    val streamSettings by viewModel.streamSettings.collectAsState()
    val cloudStatus by viewModel.cloudStatus.collectAsState()
    val tuyaAccessId by viewModel.tuyaAccessId.collectAsState()
    val tuyaAccessSecret by viewModel.tuyaAccessSecret.collectAsState()
    val tuyaDeviceIdFutterwache by viewModel.tuyaDeviceIdFutterwache.collectAsState()
    val tuyaDeviceIdStallbox by viewModel.tuyaDeviceIdStallbox.collectAsState()
    val tuyaApiBase by viewModel.tuyaApiBase.collectAsState()

    var hostInput by remember { mutableStateOf(edgeHost) }
    var tokenInput by remember { mutableStateOf(edgeToken) }
    var apiKeyInput by remember { mutableStateOf(customApiKey) }
    var showApiKey by remember { mutableStateOf(false) }

    // --- Stallblick Cloud & Streams (Port des Die-Stallwache-Repos) ---
    var webappUrlInput by remember { mutableStateOf(streamSettings.webappUrl) }
    var cloudPasswortInput by remember { mutableStateOf("") }
    var showCloudPasswort by remember { mutableStateOf(false) }
    var bridgeUrlInput by remember { mutableStateOf(streamSettings.bridgeUrl) }
    var bridgeTypeInput by remember { mutableStateOf(streamSettings.bridgeType) }
    var streamNameInput by remember { mutableStateOf(streamSettings.streamNameStallwache) }
    var streamName2Input by remember { mutableStateOf(streamSettings.streamNameFutterwache) }
    var streamName3Input by remember { mutableStateOf(streamSettings.streamNameStallbox) }
    var tuyaIdInput by remember { mutableStateOf(tuyaAccessId) }
    var tuyaSecretInput by remember { mutableStateOf(tuyaAccessSecret) }
    var showTuyaSecret by remember { mutableStateOf(false) }
    var tuyaDevFwInput by remember { mutableStateOf(tuyaDeviceIdFutterwache) }
    var tuyaDevSbInput by remember { mutableStateOf(tuyaDeviceIdStallbox) }
    var tuyaBaseInput by remember { mutableStateOf(tuyaApiBase) }

    var saveStatusMsg by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- Screen Title ---
        item {
            Column {
                Text(
                    text = "Konfiguration",
                    style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.ExtraBold)
                )
                Text(
                    text = "Verwalte Stall-Kameras, API-Schnittstellen und Alarmeinstellungen.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // --- Save Success Message Banner ---
        if (saveStatusMsg != null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text(
                            text = saveStatusMsg ?: "",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }

        // --- Firebase User Profile Card (Requested) ---
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "AS",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Angemeldet via Google Sign-In",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "axe2kgaming@gmail.com", // Injected user email from metadata
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Firestore-Datenbank: Synchronisiert (Real-Time)",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray
                        )
                    }

                    Icon(
                        imageVector = Icons.Default.VerifiedUser,
                        contentDescription = "Verifiziert",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // --- Camera & Edge Server Node Section ---
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.SettingsInputAntenna, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text(
                            text = "Stall-PC Verbindung (Bridge)",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    Text(
                        text = "Das Stallblick-System benötigt go2rtc / MediaMTX lokal zur RTSP-Umsortierung.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )

                    // --- Web Stream Mapping Info Box ---
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Web-Dashboard Stream-Schnittstellen:",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("Kamera 1: Futterwache", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                                        Text("Physische Cam am Futtertisch", fontSize = 9.sp, color = Color.Gray)
                                    }
                                    Surface(
                                        color = MaterialTheme.colorScheme.surface,
                                        shape = RoundedCornerShape(4.dp),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                    ) {
                                        Text(
                                            text = "/api/stallbox/stream",
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            fontSize = 8.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("Kamera 2: Stallbox", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                                        Text("Physische Cam am Abkalbebereich", fontSize = 9.sp, color = Color.Gray)
                                    }
                                    Surface(
                                        color = MaterialTheme.colorScheme.surface,
                                        shape = RoundedCornerShape(4.dp),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                    ) {
                                        Text(
                                            text = "/api/futterwache/stream",
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            fontSize = 8.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                            
                            Text(
                                text = "Hinweis: Auf dem Oberen Stollenhof sind die physischen Kameratitel relativ zur logischen API-Route im Next.js Web-Repo umgedreht. Die App gleicht diesen Versatz im Dashboard automatisch aus.",
                                fontSize = 8.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                                lineHeight = 11.sp
                            )
                        }
                    }

                    OutlinedTextField(
                        value = hostInput,
                        onValueChange = { hostInput = it },
                        modifier = Modifier.fillMaxWidth().testTag("edge_host_input"),
                        label = { Text("IP-Host / DNS-Adresse") },
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )

                    OutlinedTextField(
                        value = tokenInput,
                        onValueChange = { tokenInput = it },
                        modifier = Modifier.fillMaxWidth().testTag("edge_token_input"),
                        label = { Text("Ingest Token (x-ingest-token)") },
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                viewModel.updateEdgeSettings(hostInput, tokenInput)
                                saveStatusMsg = "Bridge-Einstellungen gespeichert!"
                            },
                            modifier = Modifier.weight(1f).testTag("save_edge_btn")
                        ) {
                            Text("Speichern")
                        }

                        OutlinedButton(
                            onClick = {
                                viewModel.updateEdgeStatus(if (edgeStatus == "AKTIV") "SILENT" else "AKTIV")
                            },
                            modifier = Modifier.weight(1f).testTag("toggle_silent_btn")
                        ) {
                            Text(if (edgeStatus == "AKTIV") "Silent-Modus aktivieren" else "Analyse aktivieren")
                        }
                    }
                }
            }
        }

        // --- Stallblick Cloud (Webapp-Login, KI-Wache-Sync, Tuya via Proxy) ---
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Cloud, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text(
                            text = "Stallblick Cloud (Webapp)",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    Text(
                        text = "Verbindung zur deployten Stallblick-Webapp (Die-Stallwache auf Vercel): " +
                            "liefert die Tuya-Livestreams von Futterwache/Stallbox und synchronisiert " +
                            "die echten KI-Wache-Ereignisse ins Dashboard. Login mit dem gemeinsamen " +
                            "Stallblick-Passwort (STALLBLICK_PASSWORT).",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )

                    OutlinedTextField(
                        value = webappUrlInput,
                        onValueChange = { webappUrlInput = it },
                        modifier = Modifier.fillMaxWidth().testTag("webapp_url_input"),
                        label = { Text("Webapp-URL") },
                        placeholder = { Text("https://die-stallwache.vercel.app") },
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )

                    OutlinedTextField(
                        value = cloudPasswortInput,
                        onValueChange = { cloudPasswortInput = it },
                        modifier = Modifier.fillMaxWidth().testTag("cloud_passwort_input"),
                        label = { Text("Stallblick-Passwort") },
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        visualTransformation = if (showCloudPasswort) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showCloudPasswort = !showCloudPasswort }) {
                                Icon(
                                    imageVector = if (showCloudPasswort) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = null
                                )
                            }
                        }
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                viewModel.updateStreamSettings(
                                    streamSettings.copy(webappUrl = webappUrlInput)
                                )
                                viewModel.cloudLogin(cloudPasswortInput)
                                saveStatusMsg = "Cloud-Anmeldung gestartet…"
                            },
                            modifier = Modifier.weight(1f).testTag("cloud_login_btn")
                        ) {
                            Text("Anmelden")
                        }
                        OutlinedButton(
                            onClick = {
                                viewModel.cloudLogout()
                                saveStatusMsg = "Cloud-Session gelöscht."
                            },
                            modifier = Modifier.weight(1f).testTag("cloud_logout_btn")
                        ) {
                            Text("Abmelden")
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = if (cloudStatus.startsWith("Angemeldet")) Icons.Default.CheckCircle else Icons.Default.Info,
                            contentDescription = null,
                            tint = if (cloudStatus.startsWith("Angemeldet")) Color(0xFF2E7D32) else Color.Gray,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Status: $cloudStatus",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.testTag("cloud_status_text")
                        )
                    }
                }
            }
        }

        // --- Kamera-Bridge (go2rtc / MediaMTX) & Stream-Namen ---
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Videocam, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text(
                            text = "Kamera-Bridge (Livestreams)",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    Text(
                        text = "Die Bridge (go2rtc oder MediaMTX) wandelt die RTSP-Streams der Kameras " +
                            "im Stall-LAN in HLS um und ist per Cloudflare Tunnel erreichbar. " +
                            "Die Stallwache läuft immer über die Bridge; Futterwache/Stallbox nutzen " +
                            "sie als Fallback zur Tuya-Cloud.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )

                    OutlinedTextField(
                        value = bridgeUrlInput,
                        onValueChange = { bridgeUrlInput = it },
                        modifier = Modifier.fillMaxWidth().testTag("bridge_url_input"),
                        label = { Text("Bridge-URL (NEXT_PUBLIC_BRIDGE_URL)") },
                        placeholder = { Text("https://stallwache.deine-domain.de") },
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Bridge-Typ:", style = MaterialTheme.typography.bodyMedium)
                        FilterChip(
                            selected = bridgeTypeInput == BridgeType.GO2RTC,
                            onClick = { bridgeTypeInput = BridgeType.GO2RTC },
                            label = { Text("go2rtc") },
                            modifier = Modifier.testTag("bridge_type_go2rtc")
                        )
                        FilterChip(
                            selected = bridgeTypeInput == BridgeType.MEDIAMTX,
                            onClick = { bridgeTypeInput = BridgeType.MEDIAMTX },
                            label = { Text("MediaMTX") },
                            modifier = Modifier.testTag("bridge_type_mediamtx")
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = streamNameInput,
                            onValueChange = { streamNameInput = it },
                            modifier = Modifier.weight(1f),
                            label = { Text("Stallwache") },
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp)
                        )
                        OutlinedTextField(
                            value = streamName2Input,
                            onValueChange = { streamName2Input = it },
                            modifier = Modifier.weight(1f),
                            label = { Text("Futterwache") },
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp)
                        )
                        OutlinedTextField(
                            value = streamName3Input,
                            onValueChange = { streamName3Input = it },
                            modifier = Modifier.weight(1f),
                            label = { Text("Stallbox") },
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp)
                        )
                    }

                    Button(
                        onClick = {
                            viewModel.updateStreamSettings(
                                streamSettings.copy(
                                    bridgeUrl = bridgeUrlInput,
                                    bridgeType = bridgeTypeInput,
                                    streamNameStallwache = streamNameInput,
                                    streamNameFutterwache = streamName2Input,
                                    streamNameStallbox = streamName3Input,
                                    webappUrl = webappUrlInput,
                                )
                            )
                            saveStatusMsg = "Stream-Einstellungen gespeichert!"
                        },
                        modifier = Modifier.fillMaxWidth().testTag("save_stream_btn")
                    ) {
                        Text("Stream-Einstellungen speichern")
                    }
                }
            }
        }

        // --- Tuya-Cloud direkt (ohne Webapp) ---
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.CameraOutdoor, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text(
                            text = "Tuya-Cloud direkt (optional)",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    Text(
                        text = "Futterwache und Stallbox hängen bereits in der Tuya-Cloud. Ohne Webapp " +
                            "kann die App die kurzlebigen HLS-URLs auch direkt bei der Tuya-OpenAPI " +
                            "anfordern (iot.tuya.com → Cloud-Projekt). Ein Access-ID/Secret-Paar gilt " +
                            "fürs ganze Projekt, jede Kamera hat ihre eigene Geräte-ID.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )

                    OutlinedTextField(
                        value = tuyaIdInput,
                        onValueChange = { tuyaIdInput = it },
                        modifier = Modifier.fillMaxWidth().testTag("tuya_id_input"),
                        label = { Text("Access ID (TUYA_ACCESS_ID)") },
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )

                    OutlinedTextField(
                        value = tuyaSecretInput,
                        onValueChange = { tuyaSecretInput = it },
                        modifier = Modifier.fillMaxWidth().testTag("tuya_secret_input"),
                        label = { Text("Access Secret (TUYA_ACCESS_SECRET)") },
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        visualTransformation = if (showTuyaSecret) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showTuyaSecret = !showTuyaSecret }) {
                                Icon(
                                    imageVector = if (showTuyaSecret) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = null
                                )
                            }
                        }
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = tuyaDevFwInput,
                            onValueChange = { tuyaDevFwInput = it },
                            modifier = Modifier.weight(1f),
                            label = { Text("Geräte-ID Futterwache") },
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp)
                        )
                        OutlinedTextField(
                            value = tuyaDevSbInput,
                            onValueChange = { tuyaDevSbInput = it },
                            modifier = Modifier.weight(1f),
                            label = { Text("Geräte-ID Stallbox") },
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp)
                        )
                    }

                    OutlinedTextField(
                        value = tuyaBaseInput,
                        onValueChange = { tuyaBaseInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("API-Basis (Default EU)") },
                        placeholder = { Text("https://openapi.tuyaeu.com") },
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )

                    Button(
                        onClick = {
                            viewModel.updateTuyaSettings(
                                accessId = tuyaIdInput,
                                accessSecret = tuyaSecretInput,
                                deviceIdFutterwache = tuyaDevFwInput,
                                deviceIdStallbox = tuyaDevSbInput,
                                apiBase = tuyaBaseInput,
                            )
                            saveStatusMsg = "Tuya-Zugangsdaten gespeichert!"
                        },
                        modifier = Modifier.fillMaxWidth().testTag("save_tuya_btn")
                    ) {
                        Text("Tuya-Zugangsdaten speichern")
                    }
                }
            }
        }

        // --- Gemini Key Override Section ---
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Outlined.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text(
                            text = "Gemini API-Schlüssel",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    Text(
                        text = "Hier kannst du deinen eigenen API-Key eintragen, um die Diagnostik (Gemini 3.1 Pro) und den Chat (Flash-Lite) direkt über deine eigene Abrechnung laufen zu lassen. Falls leer, wird der systemseitige Schlüssel verwendet.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )

                    OutlinedTextField(
                        value = apiKeyInput,
                        onValueChange = { apiKeyInput = it },
                        modifier = Modifier.fillMaxWidth().testTag("api_key_input"),
                        label = { Text("Gemini API Key Override") },
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        visualTransformation = if (showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showApiKey = !showApiKey }) {
                                Icon(
                                    imageVector = if (showApiKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = null
                                )
                            }
                        }
                    )

                    Button(
                        onClick = {
                            viewModel.updateCustomApiKey(apiKeyInput)
                            saveStatusMsg = "Gemini API-Schlüssel erfolgreich überschrieben!"
                        },
                        modifier = Modifier.fillMaxWidth().testTag("save_key_btn")
                    ) {
                        Text("API Schlüssel sichern")
                    }
                }
            }
        }

        // --- Theme / Design Selection ---
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text(
                            text = "Stallblick Design-Thema",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    Text(
                        text = "Passe das visuelle Erscheinungsbild von Stallblick an deine Präferenzen oder Lichtverhältnisse an.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )

                    listOf(
                        Triple("ORGANIC_GREEN", "Bio-Hof Waldgrün", "Natürliche, warme Erdtöne für ein nachhaltiges, entspanntes Lesegefühl."),
                        Triple("CLASSIC_BLUE", "Klassisch Blau", "Klares, professionelles High-Tech Layout mit starkem Kontrast."),
                        Triple("MIDNIGHT_DARK", "Stallwache Midnight Dark", "Augenschonende Infrarot-Nachtoptik für späte Kontrollgänge im Stall.")
                    ).forEach { (themeId, label, desc) ->
                        val isSelected = selectedTheme == themeId
                        val border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                                     else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        val background = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                                         else MaterialTheme.colorScheme.surface
                        
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { 
                                    viewModel.updateSelectedTheme(themeId)
                                    saveStatusMsg = "Design auf '$label' geändert!"
                                }
                                .testTag("theme_select_$themeId"),
                            border = border,
                            colors = CardDefaults.cardColors(containerColor = background),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { 
                                        viewModel.updateSelectedTheme(themeId)
                                        saveStatusMsg = "Design auf '$label' geändert!"
                                    }
                                )
                                Column {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        text = desc,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- Alarms Cooldown & Threshold Control ---
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.HourglassEmpty, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text(
                            text = "Alarm Cooldown & Schwellen",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Alarm-Cooldown", style = MaterialTheme.typography.bodyMedium)
                            Text("Empfohlener Spam-Filter für Telegram- und Dashboard-Pings.", fontSize = 10.sp, color = Color.Gray)
                        }
                        Text(
                            text = "$cooldownMinutes min",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 14.sp
                        )
                    }

                    Slider(
                        value = cooldownMinutes.toFloat(),
                        onValueChange = { viewModel.updateCooldown(it.toInt()) },
                        valueRange = 5f..60f,
                        steps = 11,
                        modifier = Modifier.testTag("cooldown_slider")
                    )

                    Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Globaler Wach-Modus", style = MaterialTheme.typography.bodyMedium)
                            Text("Scharfschaltung 14 Tage vor Abkalben (senkt Toleranzen).", fontSize = 10.sp, color = Color.Gray)
                        }
                        Switch(
                            checked = globalWatchMode,
                            onCheckedChange = { viewModel.toggleWachModusGlobal(it) },
                            modifier = Modifier.testTag("global_watch_switch")
                        )
                    }
                }
            }
        }

        // --- Database & Seed Reset Section ---
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.2f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.DeleteForever, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Text(
                            text = "Gefahrenzone (Wartung)",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    Text(
                        text = "Durch das Leeren der Protokolle löschst du alle Alarme im Dashboard. Ein Zurücksetzen der Datenbank stellt die Demo-Kühe und -Ereignisse wieder her.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                viewModel.clearAllEvents()
                                saveStatusMsg = "Alle Dashboard-Ereignisse gelöscht!"
                            },
                            modifier = Modifier.weight(1f).testTag("clear_events_btn"),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                        ) {
                            Text("Ereignisliste leeren", color = MaterialTheme.colorScheme.error, fontSize = 11.sp, maxLines = 1)
                        }
                    }
                }
            }
        }

        // Spacing bottom
        item { Spacer(modifier = Modifier.height(48.dp)) }
    }
}
