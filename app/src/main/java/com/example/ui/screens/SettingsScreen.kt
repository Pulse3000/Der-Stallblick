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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

    var hostInput by remember { mutableStateOf(edgeHost) }
    var tokenInput by remember { mutableStateOf(edgeToken) }
    var apiKeyInput by remember { mutableStateOf(customApiKey) }
    var showApiKey by remember { mutableStateOf(false) }

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
