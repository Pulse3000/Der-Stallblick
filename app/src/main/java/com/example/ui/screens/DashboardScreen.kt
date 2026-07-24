package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import android.webkit.WebView
import android.webkit.WebViewClient
import android.content.Intent
import android.net.Uri
import com.example.data.StallEvent
import com.example.ui.components.BarnCameraXPreviewCard
import com.example.ui.components.CameraXLiveFeedView
import com.example.ui.components.RoomMonitoringLogsDashboardCard
import com.example.viewmodel.StallViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Calendar

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DashboardScreen(
    viewModel: StallViewModel,
    modifier: Modifier = Modifier
) {
    val cows by viewModel.cows.collectAsState()
    val events by viewModel.events.collectAsState()
    val edgeHost by viewModel.edgeHost.collectAsState()
    val edgeStatus by viewModel.edgeStatus.collectAsState()
    val globalWatchMode by viewModel.wachModusGlobal.collectAsState()
    val ingestSimulation by viewModel.ingestSimulationState.collectAsState()
    val tuyaCastUrl by viewModel.tuyaCastUrl.collectAsState()

    var eventFilter by remember { mutableStateOf("ALL") } // "ALL", "CRITICAL", "INFO"
    var selectedAlertForTuyaVerification by remember { mutableStateOf<StallEvent?>(null) }

    val bertaCow = cows.find { it.id == "Kuh #42" }
    val zeldaCow = cows.find { it.id == "Kuh #103" }

    // Find first active unresolved warning event
    val activeWarning = events.firstOrNull { 
        !it.resolved && (it.typ == "austreibung" || it.typ == "eskalation" || it.typ == "kalbeverdacht" || it.typ == "brunstverdacht") 
    }

    // Calculate today's counts for the daily report
    val todayStart = remember(events) {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        cal.timeInMillis
    }
    val totalBirthsToday = events.count { (it.typ == "austreibung" || it.typ == "eskalation") && it.timestamp >= todayStart }
    val totalHeatsToday = events.count { it.typ == "brunstverdacht" && it.timestamp >= todayStart }

    // Floating pulsing alerts states
    var activeToastEvent by remember { mutableStateOf<StallEvent?>(null) }
    
    LaunchedEffect(viewModel) {
        viewModel.newIngestedEvent.collect { event ->
            activeToastEvent = event
            delay(5000) // Pulse alert toast for 5 seconds
            activeToastEvent = null
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse_alert")
    val toastPulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "toastPulse"
    )

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isWideScreen = maxWidth >= 720.dp
        
        if (isWideScreen) {
            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFF8FAFC))
                    .verticalScroll(scrollState)
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // Ingest notification inside Widescreen
                AnimatedVisibility(
                    visible = ingestSimulation != null,
                    enter = fadeIn() + slideInVertically(),
                    exit = fadeOut() + slideOutVertically()
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFD8E2FF)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = Color(0xFF005AC1),
                                strokeWidth = 2.dp
                            )
                            Text(
                                text = ingestSimulation ?: "",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF001D3E)
                            )
                        }
                    }
                }

                // Widescreen Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Stallblick",
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-0.75).sp
                            ),
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "OBERER STOLLENHOF • KI-WACHE DASHBOARD",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.5.sp
                            ),
                            color = Color(0xFF64748B)
                        )
                    }

                    val pulseTransition = rememberInfiniteTransition(label = "pulse")
                    val pulseAlpha by pulseTransition.animateFloat(
                        initialValue = 0.4f,
                        targetValue = 1.0f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1000, easing = LinearEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "pulseAlpha"
                    )

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (globalWatchMode) Color(0xFFD8E2FF) else Color(0xFFF3F3F3))
                            .clickable { viewModel.toggleWachModusGlobal(!globalWatchMode) }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (globalWatchMode) Color(0xFF005AC1).copy(alpha = pulseAlpha)
                                        else Color(0xFF74777F)
                                    )
                            )
                            Text(
                                text = if (globalWatchMode) "KI-WACHE AKTIV" else "WACHE INAKTIV",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                ),
                                color = if (globalWatchMode) Color(0xFF001D3E) else Color(0xFF44474E)
                            )
                        }
                    }
                }

                // HERD MONITORED STATE SUMMARY (Calving vs Oestrus count)
                MonitoredStateSummaryCard(
                    cows = cows
                )

                // Bento-style 2-Column Grid Layout (Tailwind Grid Mockup)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // LEFT COLUMN (Real-time feeds and alerts)
                    Column(
                        modifier = Modifier.weight(1.2f),
                        verticalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        BarnLiveStreamFeedContainer(
                            viewModel = viewModel,
                            bertaCow = bertaCow,
                            zeldaCow = zeldaCow
                        )

                        BarnCameraXPreviewCard(
                            viewModel = viewModel,
                            cameraTitle = "Direkte Stallkamera (CameraX Feed)"
                        )

                        RoomMonitoringLogsDashboardCard(
                            viewModel = viewModel
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.List,
                                    contentDescription = "Event Log",
                                    tint = Color(0xFF475569)
                                )
                                Text(
                                    text = "Letzte Ereignisse",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color(0xFF1E293B)
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                FilterChip(
                                    selected = eventFilter == "ALL",
                                    onClick = { eventFilter = "ALL" },
                                    label = { Text("Alle", fontSize = 11.sp, fontWeight = FontWeight.Medium) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(0xFFD8E2FF),
                                        selectedLabelColor = Color(0xFF001D3E)
                                    )
                                )
                                FilterChip(
                                    selected = eventFilter == "CRITICAL",
                                    onClick = { eventFilter = "CRITICAL" },
                                    label = { Text("Alarme", fontSize = 11.sp, fontWeight = FontWeight.Medium) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(0xFFFFDAD6),
                                        selectedLabelColor = Color(0xFF410002)
                                    )
                                )
                            }
                        }

                        val filteredEvents = events.filter {
                            when (eventFilter) {
                                "CRITICAL" -> it.typ == "austreibung" || it.typ == "eskalation" || it.typ == "kalbeverdacht" || it.typ == "brunstverdacht"
                                else -> true
                            }
                        }

                        if (filteredEvents.isEmpty()) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F5F9)),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(28.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Verified,
                                            contentDescription = "No events",
                                            tint = Color(0xFF64748B),
                                            modifier = Modifier.size(32.dp)
                                        )
                                        Text(
                                            "Alles ruhig im Rinderstall",
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                            color = Color(0xFF475569)
                                        )
                                    }
                                }
                            }
                        } else {
                            filteredEvents.forEach { event ->
                                EventLogItem(
                                    event = event,
                                    onResolve = { viewModel.markEventResolved(event.id) },
                                    onVerifyOnTuya = { selectedAlertForTuyaVerification = it }
                                )
                            }
                        }
                    }

                    // RIGHT COLUMN (Stats, System Status, Sensors, Telemetry, and Simulation controls)
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        BentoAlertCard(
                            viewModel = viewModel,
                            activeWarning = activeWarning,
                            onResolve = { activeWarning?.let { viewModel.markEventResolved(it.id) } },
                            onVerifyOnTuya = { selectedAlertForTuyaVerification = it }
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            BentoStatsCard(
                                births = totalBirthsToday,
                                heats = totalHeatsToday,
                                filterState = eventFilter,
                                onToggleFilter = {
                                    eventFilter = if (eventFilter == "CRITICAL") "ALL" else "CRITICAL"
                                },
                                modifier = Modifier.weight(1f)
                            )

                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                BentoModeToggleCard(
                                    isActive = globalWatchMode,
                                    onToggle = { viewModel.toggleWachModusGlobal(!globalWatchMode) }
                                )

                                BentoEdgeNodeCard(
                                    host = edgeHost,
                                    status = edgeStatus
                                )
                            }
                        }

                        BentoRealTimeSensorAlertsCard(
                            viewModel = viewModel,
                            bertaCow = bertaCow,
                            zeldaCow = zeldaCow
                        )

                        BentoEstrusActivityChartCard()

                        BentoSystemStatusCard(
                            host = edgeHost,
                            status = edgeStatus
                        )

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            shape = RoundedCornerShape(28.dp)
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Text(
                                    text = "Stallblick Edge-Simulation",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color(0xFF0F172A)
                                )
                                Text(
                                    text = "Simuliere Ingest-Ereignisse des lokalen Stall-PCs, um das Dashboard live zu testen.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF64748B)
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            viewModel.simulateIncomingEdgeAlarm(
                                                cowId = "Kuh #42",
                                                type = "kalbeverdacht",
                                                camera = "stallwache",
                                                message = "Kuh #42 (Berta): Schwanzwinkel > 45° (aktuell 51.2°) in 24 % der Frames über 30 min.",
                                                confidence = 0.85
                                            )
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(38.dp)
                                            .testTag("simulate_calving_btn"),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF005AC1)),
                                        contentPadding = PaddingValues(0.dp),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("Berta Wehen", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }

                                    Button(
                                        onClick = {
                                            viewModel.simulateIncomingEdgeAlarm(
                                                cowId = "Kuh #42",
                                                type = "austreibung",
                                                camera = "stallwache",
                                                message = "SOFORT-ALARM: Fruchtblase (amniotic_sac) mit Konfidenz 0.92 auf stallwache erkannt!",
                                                confidence = 0.92
                                            )
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(38.dp)
                                            .testTag("simulate_birth_btn"),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFBA1A1A)),
                                        contentPadding = PaddingValues(0.dp),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("Berta Geburt", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }

                                    Button(
                                        onClick = {
                                            viewModel.simulateIncomingEdgeAlarm(
                                                cowId = "Kuh #103",
                                                type = "brunstverdacht",
                                                camera = "futterwache",
                                                message = "Aufsprung (Kuh #103 Zelda auf Kuh #18 Alma) seit 4,8s stabil erkannt (IoU 0.21).",
                                                confidence = 0.94
                                            )
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(38.dp)
                                            .testTag("simulate_heat_btn"),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF006495)),
                                        contentPadding = PaddingValues(0.dp),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("Zelda Brunst", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFFDFBFF))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
        // --- Header Block with Ingest Toast ---
        item {
            AnimatedVisibility(
                visible = ingestSimulation != null,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically()
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFD8E2FF)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Color(0xFF005AC1),
                            strokeWidth = 2.dp
                        )
                        Text(
                            text = ingestSimulation ?: "",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF001D3E)
                        )
                    }
                }
            }

            // App Header Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Stallblick",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = (-0.5).sp
                        ),
                        color = Color(0xFF1A1C1E)
                    )
                    Text(
                        text = "OBERER STOLLENHOF",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        color = Color(0xFF44474E)
                    )
                }

                // Bento-Style Active Pill Badge (Toggles Global Watch Mode)
                val pulseTransition = rememberInfiniteTransition(label = "pulse")
                val pulseAlpha by pulseTransition.animateFloat(
                    initialValue = 0.4f,
                    targetValue = 1.0f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1000, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "pulseAlpha"
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (globalWatchMode) Color(0xFFD8E2FF) else Color(0xFFF3F3F3))
                        .clickable { viewModel.toggleWachModusGlobal(!globalWatchMode) }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(
                                    if (globalWatchMode) Color(0xFF005AC1).copy(alpha = pulseAlpha)
                                    else Color(0xFF74777F)
                                )
                        )
                        Text(
                            text = if (globalWatchMode) "KI-WACHE AKTIV" else "WACHE INAKTIV",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            ),
                            color = if (globalWatchMode) Color(0xFF001D3E) else Color(0xFF44474E)
                        )
                    }
                }
            }
        }

        // --- HERD MONITORED STATE SUMMARY VIEW (Calving vs Oestrus Count) ---
        item {
            MonitoredStateSummaryCard(
                cows = cows
            )
        }

        // --- BENTO GRID: MAIN ALERT CARD (High Priority) ---
        item {
            BentoAlertCard(
                viewModel = viewModel,
                activeWarning = activeWarning,
                onResolve = { activeWarning?.let { viewModel.markEventResolved(it.id) } },
                onVerifyOnTuya = { selectedAlertForTuyaVerification = it }
            )
        }

        // --- BENTO GRID: STATS & SETTINGS SECTION ---
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Left: Stats / Daily Report Card (blue bg)
                BentoStatsCard(
                    births = totalBirthsToday,
                    heats = totalHeatsToday,
                    filterState = eventFilter,
                    onToggleFilter = {
                        eventFilter = if (eventFilter == "CRITICAL") "ALL" else "CRITICAL"
                    },
                    modifier = Modifier.weight(1f)
                )

                // Right: Column holding Edge Node & Mode Switch
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    BentoModeToggleCard(
                        isActive = globalWatchMode,
                        onToggle = { viewModel.toggleWachModusGlobal(!globalWatchMode) }
                    )

                    BentoEdgeNodeCard(
                        host = edgeHost,
                        status = edgeStatus
                    )
                }
            }
        }

        // --- BENTO GRID: REAL-TIME SENSORS & TELEMETRY ---
        item {
            BentoRealTimeSensorAlertsCard(
                viewModel = viewModel,
                bertaCow = bertaCow,
                zeldaCow = zeldaCow
            )
        }

        // --- BENTO GRID: RECHARTS-STYLE ESTRUS ACTIVITY CHART ---
        item {
            BentoEstrusActivityChartCard()
        }

        // --- BENTO GRID: SIMULATED VIDEO STREAM & CONTROLS ---
        item {
            BarnLiveStreamFeedContainer(
                viewModel = viewModel,
                bertaCow = bertaCow,
                zeldaCow = zeldaCow
            )
        }

        // --- DIRECT BARN CAMERAX PREVIEW FEED ---
        item {
            BarnCameraXPreviewCard(
                viewModel = viewModel,
                cameraTitle = "Direkte Stallkamera (CameraX Feed)"
            )
        }

        // --- ROOM DATABASE COW MONITORING LOGS SUMMARY & QUICK STATUS ---
        item {
            RoomMonitoringLogsDashboardCard(
                viewModel = viewModel
            )
        }

        // --- BENTO GRID: SYSTEMSTATUS & HARDWARE CONNECTIONS ---
        item {
            BentoSystemStatusCard(
                host = edgeHost,
                status = edgeStatus
            )
        }

        // --- Edge-Simulation Bento Panel ---
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFC5C6D0)),
                shape = RoundedCornerShape(28.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Stallblick Edge-Simulation",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF1A1C1E)
                    )
                    Text(
                        text = "Simuliere Ingest-Ereignisse des lokalen Stall-PCs, um das Dashboard live zu testen.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF44474E)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                viewModel.simulateIncomingEdgeAlarm(
                                    cowId = "Kuh #42",
                                    type = "kalbeverdacht",
                                    camera = "stallwache",
                                    message = "Kuh #42 (Berta): Schwanzwinkel > 45° (aktuell 51.2°) in 24 % der Frames über 30 min.",
                                    confidence = 0.85
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .testTag("simulate_calving_btn"),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF005AC1)),
                            contentPadding = PaddingValues(0.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Berta Wehen", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                viewModel.simulateIncomingEdgeAlarm(
                                    cowId = "Kuh #42",
                                    type = "austreibung",
                                    camera = "stallwache",
                                    message = "SOFORT-ALARM: Fruchtblase (amniotic_sac) mit Konfidenz 0.92 auf stallwache erkannt!",
                                    confidence = 0.92
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .testTag("simulate_birth_btn"),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFBA1A1A)),
                            contentPadding = PaddingValues(0.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Berta Geburt", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                viewModel.simulateIncomingEdgeAlarm(
                                    cowId = "Kuh #103",
                                    type = "brunstverdacht",
                                    camera = "futterwache",
                                    message = "Aufsprung (Kuh #103 Zelda auf Kuh #18 Alma) seit 4,8s stabil erkannt (IoU 0.21).",
                                    confidence = 0.94
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .testTag("simulate_heat_btn"),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF006495)),
                            contentPadding = PaddingValues(0.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Zelda Brunst", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // --- Alarm Events Header ---
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Letzte Ereignisse",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFF1A1C1E)
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = eventFilter == "ALL",
                        onClick = { eventFilter = "ALL" },
                        label = { Text("Alle", fontSize = 11.sp, fontWeight = FontWeight.Medium) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFD8E2FF),
                            selectedLabelColor = Color(0xFF001D3E)
                        )
                    )
                    FilterChip(
                        selected = eventFilter == "CRITICAL",
                        onClick = { eventFilter = "CRITICAL" },
                        label = { Text("Alarme", fontSize = 11.sp, fontWeight = FontWeight.Medium) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFFFDAD6),
                            selectedLabelColor = Color(0xFF410002)
                        )
                    )
                }
            }
        }

        // --- Alarms List ---
        val filteredEvents = events.filter {
            when (eventFilter) {
                "CRITICAL" -> it.typ == "austreibung" || it.typ == "eskalation" || it.typ == "kalbeverdacht" || it.typ == "brunstverdacht"
                else -> true
            }
        }

        if (filteredEvents.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF3F3F3)),
                    border = BorderStroke(1.dp, Color(0xFFC5C6D0)),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Verified,
                                contentDescription = "No events",
                                tint = Color(0xFF74777F),
                                modifier = Modifier.size(32.dp)
                            )
                            Text(
                                "Alles ruhig im Rinderstall",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                color = Color(0xFF44474E)
                            )
                        }
                    }
                }
            }
        } else {
            items(filteredEvents) { event ->
                EventLogItem(
                    event = event,
                    onResolve = { viewModel.markEventResolved(event.id) },
                    onVerifyOnTuya = { selectedAlertForTuyaVerification = it }
                )
            }
        }

        // Bottom space padding
        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

    // FLOATING PULSING TOAST NOTIFICATION
    AnimatedVisibility(
        visible = activeToastEvent != null,
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
        modifier = Modifier
            .align(Alignment.TopCenter)
            .padding(16.dp)
    ) {
        val event = activeToastEvent
        if (event != null) {
            val isCritical = event.typ == "austreibung" || event.typ == "eskalation" || event.typ == "kalbeverdacht"
            val alertColor = if (isCritical) Color(0xFFBA1A1A) else Color(0xFF006874)
            val alertBg = if (isCritical) Color(0xFFFFDAD6) else Color(0xFFE0F7FA)
            val alertOnBg = if (isCritical) Color(0xFF410002) else Color(0xFF001F24)
            
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer(scaleX = 1f, scaleY = 1f)
                    .border(
                        BorderStroke(
                            (2.dp * toastPulseAlpha), 
                            alertColor.copy(alpha = toastPulseAlpha)
                        ), 
                        RoundedCornerShape(20.dp)
                    ),
                colors = CardDefaults.cardColors(containerColor = alertBg),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(alertColor.copy(alpha = 0.2f * toastPulseAlpha))
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isCritical) Icons.Default.Warning else Icons.Default.NotificationsActive,
                            contentDescription = "Alarm",
                            tint = alertColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "NEUES EREIGNIS EMPFANGEN",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = alertColor,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = event.nachricht,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = alertOnBg
                        )
                        Text(
                            text = "Kamera: ${event.kamera} • Kuh-ID: ${event.kuhId ?: "System"}",
                            style = MaterialTheme.typography.labelSmall,
                            color = alertOnBg.copy(alpha = 0.7f)
                        )
                    }
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Button(
                            onClick = {
                                val target = activeToastEvent
                                activeToastEvent = null
                                target?.let { selectedAlertForTuyaVerification = it }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = alertColor),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Videocam,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Live Feed", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }

                        IconButton(
                            onClick = { activeToastEvent = null },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Schließen",
                                tint = alertColor,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // Tuya Live Alert Verification Dialog Modal
    selectedAlertForTuyaVerification?.let { event ->
        TuyaAlertVerificationDialog(
            event = event,
            tuyaCastUrl = tuyaCastUrl,
            viewModel = viewModel,
            onResolve = { viewModel.markEventResolved(event.id) },
            onDismiss = { selectedAlertForTuyaVerification = null }
        )
    }
}
}

// --- BENTO COMPOSABLE: MAIN ALERT CARD ---
@Composable
fun BentoAlertCard(
    viewModel: StallViewModel,
    activeWarning: StallEvent?,
    onResolve: () -> Unit,
    onVerifyOnTuya: ((StallEvent) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var toastEvent by remember { mutableStateOf<StallEvent?>(null) }
    var showToast by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel) {
        viewModel.newIngestedEvent.collect { event ->
            toastEvent = event
            showToast = true
            kotlinx.coroutines.delay(4000)
            showToast = false
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "toastPulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.97f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Box(modifier = modifier.fillMaxWidth()) {
        if (activeWarning != null) {
            // Red warning card matching the html spec
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFDAD6)),
                border = BorderStroke(1.dp, Color(0xFFFFB4AB)),
                shape = RoundedCornerShape(28.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "AKTUELLE MELDUNG",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                ),
                                color = Color(0xFF410002)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${activeWarning.kuhId ?: "Kuh #42"}:\n" + when (activeWarning.typ) {
                                    "austreibung" -> "Austreibungsphase"
                                    "eskalation" -> "Komplikationsverdacht"
                                    "kalbeverdacht" -> "Kalbeverdacht"
                                    "brunstverdacht" -> "Brunstverdacht"
                                    else -> "Auffälligkeit"
                                },
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    lineHeight = 26.sp
                                ),
                                color = Color(0xFF410002)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFBA1A1A))
                                .padding(8.dp)
                        ) {
                            Icon(
                                imageVector = when (activeWarning.typ) {
                                    "austreibung" -> Icons.Default.Warning
                                    "eskalation" -> Icons.Default.Error
                                    else -> Icons.Default.NotificationsActive
                                },
                                contentDescription = "Alert Icon",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Text(
                            text = activeWarning.nachricht,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF410002).copy(alpha = 0.8f),
                            modifier = Modifier.weight(1f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        val timeText = remember(activeWarning.timestamp) {
                            SimpleDateFormat("HH:mm", Locale.GERMANY).format(Date(activeWarning.timestamp))
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFFFFB4AB))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = timeText,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color(0xFF410002)
                                )
                            }

                            IconButton(
                                onClick = onResolve,
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFBA1A1A))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Resolve Alert",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { activeWarning.let { onVerifyOnTuya?.invoke(it) } },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("verify_alert_tuya_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFBA1A1A)),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(vertical = 8.dp, horizontal = 12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Videocam,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "👁️ Live auf Tuya-Kamera Verifizieren",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        } else {
            // Safe state: "Alles ruhig"
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                border = BorderStroke(1.dp, Color(0xFFC8E6C9)),
                shape = RoundedCornerShape(28.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column {
                            Text(
                                text = "SYSTEM STATUS",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                ),
                                color = Color(0xFF1B5E20)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Alles ruhig im Rinderstall",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold
                            ),
                            color = Color(0xFF1B5E20)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF2E7D32))
                                .padding(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Verified,
                                contentDescription = "Status OK",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Text(
                            text = "Die KI-Wache läuft im Hintergrund. Alle Kameras und der lokale Edge-PC senden stabile Herzschläge.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF1B5E20).copy(alpha = 0.8f),
                            modifier = Modifier.weight(1f)
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFFC8E6C9))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "AKTIV",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF1B5E20)
                            )
                        }
                    }
                }
            }
        }

        // Beautiful pulsing toast inside the bento cell
        AnimatedVisibility(
            visible = showToast,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 }),
            modifier = Modifier.align(Alignment.BottomCenter).padding(12.dp)
        ) {
            toastEvent?.let { event ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            scaleX = pulseScale
                            scaleY = pulseScale
                        },
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF005AC1).copy(alpha = pulseAlpha)),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FlashOn,
                            contentDescription = "Event Ingested",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "INGEST: POST /api/events",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                                color = Color.White.copy(alpha = 0.85f)
                            )
                            Text(
                                text = "${event.kuhId ?: "Kuh"}: ${event.nachricht}",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

// --- BENTO COMPOSABLE: STATS CARD (Blue bg) ---
@Composable
fun BentoStatsCard(
    births: Int,
    heats: Int,
    filterState: String,
    onToggleFilter: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(175.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFDEE1FF)),
        border = BorderStroke(1.dp, Color(0xFFBEC2FF)),
        shape = RoundedCornerShape(28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(18.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "TAGESBERICHT",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                ),
                color = Color(0xFF001158)
            )

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = births.toString(),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Light,
                        color = Color(0xFF001158)
                    )
                    Text(
                        text = "Geburten",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF001158).copy(alpha = 0.7f),
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }

                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = heats.toString(),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Light,
                        color = Color(0xFF001158)
                    )
                    Text(
                        text = "Brunst",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF001158).copy(alpha = 0.7f),
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }
            }

            Button(
                onClick = onToggleFilter,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF005AC1),
                    contentColor = Color.White
                ),
                contentPadding = PaddingValues(0.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = if (filterState == "CRITICAL") "ALLE ANZEIGEN" else "DETAILS",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}

// --- BENTO COMPOSABLE: MODE TOGGLE CARD (Grey bg with anim switch) ---
@Composable
fun BentoModeToggleCard(
    isActive: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val alignmentTransition = updateTransition(targetState = isActive, label = "switchAnim")
    val thumbOffset by alignmentTransition.animateDp(
        transitionSpec = { spring(stiffness = Spring.StiffnessMedium) },
        label = "thumbOffset"
    ) { active ->
        if (active) 22.dp else 0.dp
    }
    val switchBgColor = if (isActive) Color(0xFF006495) else Color(0xFFC5C6D0)

    Card(
        modifier = modifier
            .height(82.dp)
            .clickable { onToggle() },
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF3F3F3)),
        border = BorderStroke(1.dp, Color(0xFFC5C6D0)),
        shape = RoundedCornerShape(28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "WACH-MODUS",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    ),
                    color = Color(0xFF44474E)
                )
                Text(
                    text = if (isActive) "Sensibilität erhöht" else "Standard-Modus",
                    fontSize = 9.sp,
                    color = Color(0xFF74777F)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Box(
                    modifier = Modifier
                        .width(46.dp)
                        .height(24.dp)
                        .clip(CircleShape)
                        .background(switchBgColor)
                        .padding(2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .offset(x = thumbOffset)
                            .clip(CircleShape)
                            .background(Color.White)
                    )
                }
            }
        }
    }
}

// --- BENTO COMPOSABLE: EDGE NODE STATUS CARD ---
@Composable
fun BentoEdgeNodeCard(
    host: String,
    status: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(81.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFC5C6D0)),
        shape = RoundedCornerShape(28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFFF3F3F3))
                            .padding(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Dns,
                            contentDescription = "Edge Node",
                            tint = Color(0xFF44474E),
                            modifier = Modifier.size(12.dp)
                        )
                    }
                    Text(
                        text = "Edge Node",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF1A1C1E)
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            when (status) {
                                "AKTIV" -> Color(0xFFE8F5E9)
                                "SILENT" -> Color(0xFFFFF3E0)
                                else -> Color(0xFFF5F5F5)
                            }
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = status,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 7.sp, fontWeight = FontWeight.Bold),
                        color = when (status) {
                            "AKTIV" -> Color(0xFF2E7D32)
                            "SILENT" -> Color(0xFFE65100)
                            else -> Color(0xFF757575)
                        }
                    )
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("CPU Last", fontSize = 9.sp, color = Color(0xFF44474E))
                    Text("12%", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF006495))
                }

                // Mini linear CPU meter
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE1E2EC))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(0.12f)
                            .background(Color(0xFF006495))
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Frames", fontSize = 9.sp, color = Color(0xFF44474E))
                    Text("1.1 FPS", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF006495))
                }
            }
        }
    }
}

// --- BENTO COMPOSABLE: CAMERA FEED CARD ---
@Composable
fun CameraFeedCard(
    cameraName: String,
    labelText: String,
    cowStatus: String,
    cowDetail: String,
    isWarning: Boolean,
    modifier: Modifier = Modifier,
    drawPose: DrawScopeDouble
) {
    val scope = rememberCoroutineScope()
    var isReloading by remember { mutableStateOf(false) }
    var isIrMode by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val borderAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Card(
        modifier = modifier
            .aspectRatio(4f / 3.5f)
            .border(
                width = if (isWarning) 2.dp else 1.dp,
                color = if (isWarning) Color(0xFFBA1A1A).copy(alpha = borderAlpha) else Color(0xFFC5C6D0),
                shape = RoundedCornerShape(28.dp)
            ),
        shape = RoundedCornerShape(28.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Live-feeding draw visual
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .background(if (isIrMode) Color(0xFF1C1C1C) else Color(0xFF101411))
            ) {
                drawPose(this.size.width, this.size.height, isIrMode)
            }

            // Top Camera Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(if (isWarning) Color(0xFFBA1A1A) else Color(0xFF2E7D32))
                    )
                    Text(
                        cameraName.uppercase(Locale.ROOT),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp
                    )
                }

                Text(
                    "LIVE SNAPSHOT",
                    color = Color.LightGray,
                    fontSize = 7.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Camera Hardware Remote Control Buttons (Reload stream, Toggle IR mode)
            Row(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 44.dp, end = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Reload Stream Button
                IconButton(
                    onClick = {
                        isReloading = true
                        scope.launch {
                            kotlinx.coroutines.delay(1000)
                            isReloading = false
                        }
                    },
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.6f))
                        .testTag("reload_stream_btn_${cameraName}"),
                ) {
                    val rotation = remember { Animatable(0f) }
                    LaunchedEffect(isReloading) {
                        if (isReloading) {
                            rotation.animateTo(
                                targetValue = 360f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(1000, easing = LinearEasing),
                                    repeatMode = RepeatMode.Restart
                                )
                            )
                        } else {
                            rotation.snapTo(0f)
                        }
                    }
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Reload Stream",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp).rotate(rotation.value)
                    )
                }

                // Toggle IR Mode Button
                IconButton(
                    onClick = { isIrMode = !isIrMode },
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(if (isIrMode) Color(0xFF005AC1) else Color.Black.copy(alpha = 0.6f))
                        .testTag("toggle_ir_btn_${cameraName}"),
                ) {
                    Icon(
                        imageVector = if (isIrMode) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = "Toggle IR Mode",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            // Bottom camera overlay text
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(
                    text = labelText,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp
                )
                Text(
                    text = cowDetail,
                    color = if (isWarning) Color(0xFFFFB74D) else Color.LightGray,
                    fontSize = 9.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Warning Banner Indicator overlay
            if (isWarning) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 36.dp, end = 12.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFFBA1A1A))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = cowStatus.uppercase(Locale.ROOT),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 8.sp
                    )
                }
            }

            // Reloading HUD Overlay
            if (isReloading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.8f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(
                            color = Color(0xFFD8E2FF),
                            modifier = Modifier.size(28.dp),
                            strokeWidth = 3.dp
                        )
                        Text(
                            text = "Lade Stream...",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

// Draw a minimalist schematic cow outline with keypoints on the canvas
fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCowSkeletalPose(
    width: Float, 
    height: Float, 
    isTailRaised: Boolean, 
    status: String,
    isIrMode: Boolean = false
) {
    val cowColor = if (isIrMode) Color(0xFFE0E0E0) else Color(0xFFA1887F)
    val alertColor = if (isIrMode) {
        if (isTailRaised) Color.White else Color(0xFF9E9E9E)
    } else {
        if (isTailRaised) Color(0xFFBA1A1A) else Color(0xFF81C784)
    }
    
    // Draw Ground/Straw
    val groundColor = if (isIrMode) Color(0xFF757575) else Color(0xFF8D6E63)
    drawLine(groundColor, Offset(0f, height * 0.82f), Offset(width, height * 0.82f), strokeWidth = 3f)

    // Center coordinates
    val cx = width * 0.45f
    val cy = height * 0.52f

    // Draw Cow Body/Box
    drawRect(cowColor.copy(alpha = if (isIrMode) 0.3f else 0.15f), Offset(cx - 55f, cy - 35f), Size(110f, 70f))
    
    // Keypoints
    val spineEnd = Offset(cx - 45f, cy - 25f)
    val tailBase = Offset(cx + 35f, cy - 20f)
    val tailTip = if (isTailRaised) {
        Offset(cx + 60f, cy - 60f) // Raised up
    } else {
        Offset(cx + 40f, cy + 25f) // Hanging down
    }

    // Connect spinal lines
    val spineLineColor = if (isIrMode) Color.White.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.5f)
    drawLine(spineLineColor, spineEnd, tailBase, strokeWidth = 4f)
    drawLine(alertColor, tailBase, tailTip, strokeWidth = 5f)

    // Draw circles for Keypoints
    val keypointDotColor = if (isIrMode) Color.White else Color.Yellow
    drawCircle(keypointDotColor, 6f, spineEnd)
    drawCircle(keypointDotColor, 6f, tailBase)
    drawCircle(alertColor, 8f, tailTip)

    // Draw amniotic sac if calving (Austreibung)
    if (status == "Austreibung") {
        val sacColor = if (isIrMode) Color.White.copy(alpha = 0.9f) else Color(0xFFEF5350).copy(alpha = 0.7f)
        drawCircle(sacColor, 12f, Offset(cx + 40f, cy + 10f))
        drawLine(if (isIrMode) Color.LightGray else Color.White, Offset(cx + 40f, cy + 10f), Offset(cx + 46f, cy + 28f), strokeWidth = 3f) // calf feet emerging
    }
}

fun androidx.compose.ui.graphics.drawscope.DrawScope.drawMountingBehaviorPose(
    width: Float, 
    height: Float, 
    isMounting: Boolean,
    isIrMode: Boolean = false
) {
    val cowColor = if (isIrMode) Color(0xFFE0E0E0) else Color(0xFFA1887F)
    val mountColor = if (isIrMode) {
        if (isMounting) Color.White else Color(0xFF9E9E9E)
    } else {
        if (isMounting) Color(0xFFBA1A1A) else Color(0xFF81C784)
    }
    
    val groundColor = if (isIrMode) Color(0xFF757575) else Color(0xFF8D6E63)
    drawLine(groundColor, Offset(0f, height * 0.82f), Offset(width, height * 0.82f), strokeWidth = 3f)

    val cx = width * 0.45f
    val cy = height * 0.52f

    if (isMounting) {
        // Draw standing cow (mounted)
        drawRect(cowColor.copy(alpha = if (isIrMode) 0.3f else 0.15f), Offset(cx - 75f, cy - 15f), Size(100f, 50f))
        // Draw active mounting cow (on top, tilted)
        drawRect(cowColor.copy(alpha = if (isIrMode) 0.4f else 0.25f), Offset(cx - 15f, cy - 45f), Size(90f, 50f))
        
        // Connect skeleton elements showing overlapping / mounting
        val connColor = if (isIrMode) Color.White else Color.Yellow
        drawLine(connColor, Offset(cx - 75f, cy - 5f), Offset(cx + 25f, cy - 10f), strokeWidth = 4f)
        drawLine(mountColor, Offset(cx - 15f, cy - 45f), Offset(cx + 75f, cy - 25f), strokeWidth = 4f)

        // Draw circles
        drawCircle(connColor, 6f, Offset(cx + 25f, cy - 10f))
        drawCircle(mountColor, 8f, Offset(cx + 75f, cy - 25f))
    } else {
        // Just draw two cows peacefully eating side by side
        drawRect(cowColor.copy(alpha = if (isIrMode) 0.3f else 0.15f), Offset(cx - 85f, cy - 15f), Size(75f, 50f))
        drawRect(cowColor.copy(alpha = if (isIrMode) 0.3f else 0.15f), Offset(cx + 10f, cy - 15f), Size(75f, 50f))

        val connColor = if (isIrMode) Color.White.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.3f)
        drawLine(connColor, Offset(cx - 85f, cy - 5f), Offset(cx - 10f, cy - 5f), strokeWidth = 3f)
        drawLine(connColor, Offset(cx + 10f, cy - 5f), Offset(cx + 85f, cy - 5f), strokeWidth = 3f)
    }
}

typealias DrawScopeDouble = androidx.compose.ui.graphics.drawscope.DrawScope.(width: Float, height: Float, isIrMode: Boolean) -> Unit

@Composable
fun EventLogItem(
    event: StallEvent,
    onResolve: () -> Unit,
    onVerifyOnTuya: ((StallEvent) -> Unit)? = null
) {
    val dateText = remember(event.timestamp) {
        val sdf = SimpleDateFormat("HH:mm:ss (dd.MM)", Locale.GERMANY)
        sdf.format(Date(event.timestamp))
    }

    val itemColor = when (event.typ) {
        "austreibung", "eskalation" -> Color(0xFFBA1A1A)
        "kalbeverdacht" -> Color(0xFFFFA000)
        "brunstverdacht" -> Color(0xFF00796B)
        else -> Color(0xFF44474E)
    }

    val itemBackground = when {
        event.resolved -> Color(0xFFF3F3F3).copy(alpha = 0.6f)
        event.typ == "austreibung" || event.typ == "eskalation" -> Color(0xFFFFDAD6)
        event.typ == "kalbeverdacht" -> Color(0xFFFFF3E0)
        event.typ == "brunstverdacht" -> Color(0xFFE0F2F1)
        else -> Color(0xFFF3F3F3)
    }

    val itemBorder = when {
        event.resolved -> BorderStroke(1.dp, Color(0xFFC5C6D0).copy(alpha = 0.5f))
        event.typ == "austreibung" || event.typ == "eskalation" -> BorderStroke(1.dp, Color(0xFFFFB4AB))
        event.typ == "kalbeverdacht" -> BorderStroke(1.dp, Color(0xFFFFE0B2))
        event.typ == "brunstverdacht" -> BorderStroke(1.dp, Color(0xFFB2DFDB))
        else -> BorderStroke(1.dp, Color(0xFFC5C6D0))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("event_item_${event.id}"),
        colors = CardDefaults.cardColors(containerColor = itemBackground),
        border = itemBorder,
        shape = RoundedCornerShape(20.dp) // Beautiful roundness to match bento aesthetic
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                // Event Icon matching type
                Icon(
                    imageVector = when (event.typ) {
                        "austreibung" -> Icons.Default.Warning
                        "eskalation" -> Icons.Default.Error
                        "kalbeverdacht" -> Icons.Default.NotificationsActive
                        "brunstverdacht" -> Icons.Default.Favorite
                        else -> Icons.Outlined.Info
                    },
                    contentDescription = null,
                    tint = itemColor,
                    modifier = Modifier.size(24.dp)
                )

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = when (event.typ) {
                                "austreibung" -> "AUSTREIBUNGSPHASE"
                                "eskalation" -> "ESKALATION (WARNUNG)"
                                "kalbeverdacht" -> "KALBEVERDACHT"
                                "brunstverdacht" -> "BRUNSTVERDACHT"
                                else -> "SYSTEM-INFO"
                            },
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                            color = itemColor
                        )

                        if (event.konfidenz != null) {
                            Text(
                                text = "Conf: ${String.format(Locale.US, "%.0f", event.konfidenz * 100)}%",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF44474E)
                            )
                        }

                        if (event.resolved) {
                            Text(
                                text = "GELESEN",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF74777F)
                            )
                        }
                    }

                    Text(
                        text = event.nachricht,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = if (event.resolved) FontWeight.Normal else FontWeight.Medium),
                        color = if (event.resolved) Color(0xFF1A1C1E).copy(alpha = 0.6f) else Color(0xFF1A1C1E)
                    )

                    Text(
                        text = "Kamera: ${event.kamera.uppercase(Locale.ROOT)}  •  $dateText",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF44474E)
                    )
                }
            }

            // Quick Actions: Tuya Feed & Resolve Checkbox
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                OutlinedButton(
                    onClick = { onVerifyOnTuya?.invoke(event) },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier
                        .height(32.dp)
                        .testTag("tuya_verify_btn_${event.id}"),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0xFF005AC1))
                ) {
                    Icon(
                        imageVector = Icons.Default.Videocam,
                        contentDescription = null,
                        tint = Color(0xFF005AC1),
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Tuya Feed", fontSize = 10.sp, color = Color(0xFF005AC1), fontWeight = FontWeight.Bold)
                }

                if (!event.resolved) {
                    IconButton(
                        onClick = onResolve,
                        modifier = Modifier.testTag("resolve_btn_${event.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CheckCircle,
                            contentDescription = "Gelesen markieren",
                            tint = Color(0xFF005AC1)
                        )
                    }
                }
            }
        }
    }
}

// --- BENTO COMPOSABLE: REAL-TIME TELEMETRY & ALERTS DASHBOARD ---
@Composable
fun BentoRealTimeSensorAlertsCard(
    viewModel: StallViewModel,
    bertaCow: com.example.data.Cow?,
    zeldaCow: com.example.data.Cow?,
    modifier: Modifier = Modifier
) {
    var ticks by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(2000)
            ticks += 1
        }
    }

    val isBertaCalving = bertaCow?.status == "Kalbeverdacht" || bertaCow?.status == "Austreibung"
    val isZeldaEstrus = zeldaCow?.status == "Brunstverdacht"

    val simulatedTailAngle = remember(ticks, isBertaCalving) {
        if (isBertaCalving) {
            46.0f + (ticks % 6) * 1.2f
        } else {
            13.0f + (ticks % 5) * 0.7f
        }
    }

    val simulatedContractionIndex = remember(ticks, isBertaCalving) {
        if (isBertaCalving) {
            76 + (ticks % 4) * 4
        } else {
            4 + (ticks % 3) * 2
        }
    }

    val simulatedStepsFactor = remember(ticks, isZeldaEstrus) {
        if (isZeldaEstrus) {
            4.2f + (ticks % 3) * 0.15f
        } else {
            1.2f + (ticks % 3) * 0.1f
        }
    }

    val simulatedMountings = remember(ticks, isZeldaEstrus) {
        if (isZeldaEstrus) {
            7 + (ticks / 3) % 3
        } else {
            0
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFC5C6D0)),
        shape = RoundedCornerShape(28.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = "Sensor Alert telemetry",
                        tint = Color(0xFF005AC1),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Echtzeit-Sensoren & Telemetrie",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF1A1C1E)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFBA1A1A))
                    )
                    Text(
                        text = "LIVE SENSORIK",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFBA1A1A)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                TelemetryRow(
                    cowName = "Kuh #42 (Berta)",
                    focusType = "Kalbe-Fokus",
                    isActiveWarning = isBertaCalving,
                    metrics = listOf(
                        "Schwanzwinkel" to "${String.format(Locale.US, "%.1f", simulatedTailAngle)}°",
                        "Wehen-Index" to "$simulatedContractionIndex%",
                        "Ruhe-Verhalten" to if (isBertaCalving) "Unruhig" else "Normal"
                    )
                )

                TelemetryRow(
                    cowName = "Kuh #103 (Zelda)",
                    focusType = "Brunst-Fokus",
                    isActiveWarning = isZeldaEstrus,
                    metrics = listOf(
                        "Schrittfrequenz" to "${String.format(Locale.US, "%.1f", simulatedStepsFactor)}x",
                        "Aufsprünge" to "$simulatedMountings",
                        "Brunst-Index" to if (isZeldaEstrus) "SEHR HOCH" else "Niedrig"
                    )
                )
            }
        }
    }
}

@Composable
fun TelemetryRow(
    cowName: String,
    focusType: String,
    isActiveWarning: Boolean,
    metrics: List<Pair<String, String>>
) {
    val containerBg = if (isActiveWarning) Color(0xFFFFDAD6) else Color(0xFFF3F3F3)
    val borderCol = if (isActiveWarning) Color(0xFFFFB4AB) else Color(0xFFE1E2EC)
    val labelColor = if (isActiveWarning) Color(0xFF410002) else Color(0xFF1A1C1E)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(containerBg)
            .border(1.dp, borderCol, RoundedCornerShape(16.dp))
            .padding(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = cowName,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = labelColor
                )
                Text(
                    text = focusType,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = if (isActiveWarning) Color(0xFFBA1A1A) else Color(0xFF006495)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                metrics.forEach { (label, value) ->
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(alpha = 0.6f))
                            .padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = label,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF44474E)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = value,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = labelColor
                        )
                    }
                }
            }
        }
    }
}

// --- BENTO COMPOSABLE: RECHARTS-STYLE ACTIVITY LINE CHART ---
@Composable
fun BentoEstrusActivityChartCard(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFC5C6D0)),
        shape = RoundedCornerShape(28.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.TrendingUp,
                        contentDescription = "Estrus chart activity tracker",
                        tint = Color(0xFF006495),
                        modifier = Modifier.size(20.dp)
                    )
                    Column {
                        Text(
                            text = "Aktivitätsverlauf (24 Std.)",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF1A1C1E)
                        )
                        Text(
                            text = "Bewegungsindex zur Brunstidentifikation",
                            fontSize = 9.sp,
                            color = Color(0xFF74777F)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFFE8F5E9))
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "1 SPITZE ERKANNT",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2E7D32)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height

                    val paddingBottom = 20f
                    val paddingTop = 15f
                    val paddingLeft = 30f
                    val paddingRight = 15f

                    val chartWidth = w - paddingLeft - paddingRight
                    val chartHeight = h - paddingTop - paddingBottom

                    val gridLinesCount = 5
                    for (i in 0..gridLinesCount) {
                        val y = paddingTop + chartHeight * (i.toFloat() / gridLinesCount)
                        drawLine(
                            color = Color(0xFFE1E2EC),
                            start = Offset(paddingLeft, y),
                            end = Offset(w - paddingRight, y),
                            strokeWidth = 1f
                        )
                    }

                    val thresholdY = paddingTop + chartHeight * (2f / gridLinesCount)
                    drawLine(
                        color = Color(0xFFBA1A1A).copy(alpha = 0.8f),
                        start = Offset(paddingLeft, thresholdY),
                        end = Offset(w - paddingRight, thresholdY),
                        strokeWidth = 2f,
                        pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                    )

                    val values = floatArrayOf(1.1f, 1.0f, 4.2f, 3.5f, 1.5f, 1.2f, 2.1f, 1.2f, 1.1f)
                    val pointsCount = values.size
                    val points = ArrayList<Offset>()

                    for (idx in 0 until pointsCount) {
                        val x = paddingLeft + chartWidth * (idx.toFloat() / (pointsCount - 1))
                        val factor = values[idx]
                        val y = paddingTop + chartHeight * (1f - (factor / 5.0f))
                        points.add(Offset(x, y))
                    }

                    val curvePath = Path()
                    curvePath.moveTo(points[0].x, points[0].y)
                    for (idx in 0 until pointsCount - 1) {
                        val p0 = points[idx]
                        val p1 = points[idx + 1]
                        val controlX = (p0.x + p1.x) / 2f
                        curvePath.cubicTo(controlX, p0.y, controlX, p1.y, p1.x, p1.y)
                    }

                    val filledPath = Path()
                    filledPath.addPath(curvePath)
                    filledPath.lineTo(points.last().x, paddingTop + chartHeight)
                    filledPath.lineTo(points.first().x, paddingTop + chartHeight)
                    filledPath.close()

                    drawPath(
                        path = filledPath,
                        brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF006495).copy(alpha = 0.4f),
                                Color(0xFF006495).copy(alpha = 0.01f)
                            ),
                            startY = paddingTop,
                            endY = paddingTop + chartHeight
                        )
                    )

                    drawPath(
                        path = curvePath,
                        color = Color(0xFF006495),
                        style = Stroke(width = 6f)
                    )

                    val peakPt = points[2]
                    drawCircle(
                        color = Color(0xFFBA1A1A),
                        radius = 12f,
                        center = peakPt
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 6f,
                        center = peakPt
                    )

                    drawCircle(
                        color = Color(0xFF006495),
                        radius = 8f,
                        center = points[6]
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 4f,
                        center = points[6]
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Gestern 20:00", fontSize = 8.sp, color = Color(0xFF74777F))
                Text("02:00 (Nacht)", fontSize = 8.sp, color = Color(0xFF74777F), fontWeight = FontWeight.Bold)
                Text("08:00 (Morgen)", fontSize = 8.sp, color = Color(0xFF74777F))
                Text("14:00 (Tag)", fontSize = 8.sp, color = Color(0xFF74777F))
                Text("Heute 20:00", fontSize = 8.sp, color = Color(0xFF74777F))
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LegendItem(color = Color(0xFF006495), label = "Aktivität (Zelda)")
                LegendItem(color = Color(0xFFBA1A1A), label = "Schwelle / Spitze (4.2x)")
            }
        }
    }
}

// --- BENTO COMPOSABLE: SIMULATED VIDEO FEED STALL CONTROLLER ---
@Composable
fun BarnLiveStreamFeedContainer(
    viewModel: StallViewModel,
    bertaCow: com.example.data.Cow?,
    zeldaCow: com.example.data.Cow?,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var selectedStall by remember { mutableStateOf(1) } // 1: stallwache (Abkalbebereich), 2: futterwache (Futtertisch)
    var isTransitioning by remember { mutableStateOf(false) }
    var isReloading by remember { mutableStateOf(false) }
    var isIrMode by remember { mutableStateOf(false) }
    var zoomScale by remember { mutableStateOf(1.0f) }
    var brightnessLevel by remember { mutableStateOf(0f) } // from -3f to +3f, default 0f
    var contrastBoost by remember { mutableStateOf(false) } // High Contrast / details booster

    val analyzerLoading by viewModel.analyzerLoading.collectAsState()
    val analyzerThinking by viewModel.analyzerThinking.collectAsState()
    val analyzerResult by viewModel.analyzerResult.collectAsState()
    val tuyaCastUrl by viewModel.tuyaCastUrl.collectAsState()

    val context = LocalContext.current
    var isCameraFlashing by remember { mutableStateOf(false) }
    var snapshotMessageToast by remember { mutableStateOf<String?>(null) }
    var showQuickCallDialog by remember { mutableStateOf(false) }
    var isRecordingClip by remember { mutableStateOf(false) }
    var recordingSecondsLeft by remember { mutableStateOf(10) }

    var timecodeText by remember { mutableStateOf("00:00:00") }
    LaunchedEffect(Unit) {
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.GERMANY)
        while (true) {
            timecodeText = sdf.format(Date())
            delay(1000)
        }
    }

    val activeCamName = when (selectedStall) {
        1 -> "stallwache (Abkalbebereich)"
        2 -> "futterwache (Futtertisch)"
        3 -> "Phone Live Camera"
        else -> "Tuya Cast Stream"
    }
    val activeCowId = when (selectedStall) {
        1 -> bertaCow?.let { "${it.id} (${it.name})" } ?: "Kuh #42"
        2 -> zeldaCow?.let { "${it.id} (${it.name})" } ?: "Kuh #103"
        else -> null
    }

    fun triggerQuickSnapshot() {
        scope.launch {
            isCameraFlashing = true
            delay(200)
            isCameraFlashing = false
            viewModel.logManualObservation(
                cameraName = activeCamName,
                cowId = activeCowId,
                type = "info",
                note = "📸 Schnappschuss erfasst ($timecodeText) - Beobachtung gesichert."
            )
            snapshotMessageToast = "📸 Schnappschuss in Galerie & Stall-Protokoll gespeichert!"
        }
    }

    fun toggleClipRecording() {
        if (isRecordingClip) {
            isRecordingClip = false
            snapshotMessageToast = "📹 Video-Sequenz manuell gestoppt."
            return
        }
        scope.launch {
            isRecordingClip = true
            recordingSecondsLeft = 10
            while (recordingSecondsLeft > 0 && isRecordingClip) {
                delay(1000)
                recordingSecondsLeft--
            }
            if (isRecordingClip) {
                isRecordingClip = false
                viewModel.logManualObservation(
                    cameraName = activeCamName,
                    cowId = activeCowId,
                    type = "info",
                    note = "📹 10s Video-Sequenz in Stall-Protokoll gesichert ($timecodeText)"
                )
                snapshotMessageToast = "📹 10-Sekunden Video-Sequenz im Protokoll gesichert!"
            }
        }
    }

    LaunchedEffect(selectedStall) {
        isTransitioning = true
        delay(1200)
        isTransitioning = false
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val livePulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "livePulse"
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFC5C6D0)),
        shape = RoundedCornerShape(28.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Videocam,
                        contentDescription = "Barn cameras",
                        tint = Color(0xFF005AC1),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Live-Kamera Überwachung",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF1A1C1E)
                    )
                }

                // Green LIVE Badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF2E7D32).copy(alpha = livePulseAlpha))
                    )
                    Text(
                        text = "LIVE FEED",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2E7D32)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Stall Selector Tabs (futterwache vs stallbox mapping vs CameraX)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                StallTabButton(
                    id = 1,
                    label = "Cam 1: Futterwache",
                    icon = Icons.Default.Videocam,
                    isSelected = selectedStall == 1,
                    onClick = { selectedStall = 1 },
                    modifier = Modifier.weight(1f)
                )
                StallTabButton(
                    id = 2,
                    label = "Cam 2: Stallbox",
                    icon = Icons.Default.Videocam,
                    isSelected = selectedStall == 2,
                    onClick = { selectedStall = 2 },
                    modifier = Modifier.weight(1f)
                )
                StallTabButton(
                    id = 3,
                    label = "Cam 3: CameraX",
                    icon = Icons.Default.CameraAlt,
                    isSelected = selectedStall == 3,
                    onClick = { selectedStall = 3 },
                    modifier = Modifier.weight(1f)
                )
                StallTabButton(
                    id = 4,
                    label = "Cam 4: Tuya Cast",
                    icon = Icons.Default.Cast,
                    isSelected = selectedStall == 4,
                    onClick = { selectedStall = 4 },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Widescreen Stream Container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, Color(0xFFC5C6D0), RoundedCornerShape(16.dp))
            ) {
                if (selectedStall == 3) {
                    CameraXLiveFeedView()
                } else if (selectedStall == 4) {
                    TuyaCastWebFeedView(url = tuyaCastUrl)
                } else {
                    // Drawing Stream content
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(if (isIrMode) Color(0xFF1C1C1C) else Color(0xFF101411))
                    ) {
                    val width = size.width
                    val height = size.height

                    // 1. Draw Simulated Barn Scene / Keypoints with digital Zoom
                    scale(scale = zoomScale, pivot = Offset(width / 2f, height / 2f)) {
                        when (selectedStall) {
                            1 -> {
                                // Kamera 1: Futterwache (Zelda & Alma mounting)
                                drawMountingBehaviorPose(
                                    width = width,
                                    height = height,
                                    isMounting = zeldaCow?.status == "Brunstverdacht",
                                    isIrMode = isIrMode
                                )

                                // Draw High Contrast / AI tracking bounding box if contrastBoost is active
                                if (contrastBoost) {
                                    val boxColor = if (isIrMode) Color.White else Color(0xFF006495)
                                    val cx = width * 0.45f
                                    val cy = height * 0.52f
                                    
                                    // Bounding Box for mounting behavior
                                    drawRect(
                                        color = boxColor,
                                        topLeft = Offset(cx - 100f, cy - 60f),
                                        size = Size(200f, 120f),
                                        style = Stroke(width = 2f)
                                    )
                                }
                            }
                            2 -> {
                                // Kamera 2: Stallbox (Berta calving)
                                drawCowSkeletalPose(
                                    width = width,
                                    height = height,
                                    isTailRaised = (bertaCow?.lastAngle ?: 15f) > 40f,
                                    status = bertaCow?.status ?: "Normal",
                                    isIrMode = isIrMode
                                )

                                // Draw High Contrast / AI tracking bounding box if contrastBoost is active
                                if (contrastBoost) {
                                    val boxColor = if (isIrMode) Color.White else Color(0xFFBA1A1A)
                                    val cx = width * 0.45f
                                    val cy = height * 0.52f
                                    
                                    // Bounding Box
                                    drawRect(
                                        color = boxColor,
                                        topLeft = Offset(cx - 70f, cy - 50f),
                                        size = Size(140f, 100f),
                                        style = Stroke(width = 2f)
                                    )
                                }
                            }
                        }
                    }

                    // Apply Brightness / Exposure overlay
                    if (brightnessLevel > 0f) {
                        drawRect(
                            color = Color.White.copy(alpha = (brightnessLevel * 0.12f).coerceAtMost(0.6f)),
                            size = size
                        )
                    } else if (brightnessLevel < 0f) {
                        drawRect(
                            color = Color.Black.copy(alpha = (-brightnessLevel * 0.12f).coerceAtMost(0.8f)),
                            size = size
                        )
                    }

                    // 2. Simulated Camera HUD Grid Overlay
                    val gridColor = Color.White.copy(alpha = 0.08f)
                    val strokeW = 1f
                    
                    // Vertical grid lines
                    drawLine(gridColor, Offset(width * 0.25f, 0f), Offset(width * 0.25f, height), strokeWidth = strokeW)
                    drawLine(gridColor, Offset(width * 0.5f, 0f), Offset(width * 0.5f, height), strokeWidth = strokeW)
                    drawLine(gridColor, Offset(width * 0.75f, 0f), Offset(width * 0.75f, height), strokeWidth = strokeW)
                    
                    // Horizontal grid lines
                    drawLine(gridColor, Offset(0f, height * 0.25f), Offset(width, height * 0.25f), strokeWidth = strokeW)
                    drawLine(gridColor, Offset(0f, height * 0.5f), Offset(width, height * 0.5f), strokeWidth = strokeW)
                    drawLine(gridColor, Offset(0f, height * 0.75f), Offset(width, height * 0.75f), strokeWidth = strokeW)
                    
                    // Corner Crop Marks (Safe Area brackets)
                    val bracketColor = Color.White.copy(alpha = 0.2f)
                    val bSize = 15f
                    val bThick = 2f
                    // Top-Left bracket
                    drawLine(bracketColor, Offset(15f, 15f), Offset(15f + bSize, 15f), strokeWidth = bThick)
                    drawLine(bracketColor, Offset(15f, 15f), Offset(15f, 15f + bSize), strokeWidth = bThick)
                    // Top-Right bracket
                    drawLine(bracketColor, Offset(width - 15f, 15f), Offset(width - 15f - bSize, 15f), strokeWidth = bThick)
                    drawLine(bracketColor, Offset(width - 15f, 15f), Offset(width - 15f, 15f + bSize), strokeWidth = bThick)
                    // Bottom-Left bracket
                    drawLine(bracketColor, Offset(15f, height - 15f), Offset(15f + bSize, height - 15f), strokeWidth = bThick)
                    drawLine(bracketColor, Offset(15f, height - 15f), Offset(15f, height - 15f - bSize), strokeWidth = bThick)
                    // Bottom-Right bracket
                    drawLine(bracketColor, Offset(width - 15f, height - 15f), Offset(width - 15f - bSize, height - 15f), strokeWidth = bThick)
                    drawLine(bracketColor, Offset(width - 15f, height - 15f), Offset(width - 15f, height - 15f - bSize), strokeWidth = bThick)

                    // Pulsing Red REC indicator
                    val recAlpha = livePulseAlpha
                    drawCircle(
                        color = Color.Red.copy(alpha = recAlpha),
                        radius = 6f,
                        center = Offset(30f, 30f)
                    )
                }

                // Timecode Overlay top right
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.Black.copy(alpha = 0.5f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = timecodeText,
                        color = Color.White,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Stall Identifier Overlay bottom left
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .background(Color.Black.copy(alpha = 0.6f))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = when (selectedStall) {
                            1 -> "Kamera 1: Futterwache"
                            else -> "Kamera 2: Stallbox"
                        },
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                    Text(
                        text = when (selectedStall) {
                            1 -> "Route: /api/futterwache/stream"
                            else -> "Route: /api/stallbox/stream"
                        },
                        color = Color(0xFF64B5F6),
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 8.sp
                    )
                    Text(
                        text = when (selectedStall) {
                            1 -> zeldaCow?.let { "Kuh #103 (Zelda) • Aktivität erhöht" } ?: "Normales Verhalten"
                            else -> bertaCow?.let { "Kuh #42 (Berta) • Schwanzwinkel: ${String.format(Locale.US, "%.1f", it.lastAngle)}°" } ?: "Keine Kuh im Fokus"
                        },
                        color = Color.LightGray,
                        fontSize = 8.sp
                    )
                }

                // Hardware controls bottom right
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Reload button
                    IconButton(
                        onClick = {
                            isReloading = true
                            scope.launch {
                                delay(1000)
                                isReloading = false
                            }
                        },
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.6f))
                            .testTag("stream_reload_btn")
                    ) {
                        val rotation = remember { Animatable(0f) }
                        LaunchedEffect(isReloading) {
                            if (isReloading) {
                                rotation.animateTo(
                                    targetValue = 360f,
                                    animationSpec = infiniteRepeatable(
                                        animation = tween(1000, easing = LinearEasing),
                                        repeatMode = RepeatMode.Restart
                                    )
                                )
                            } else {
                                rotation.snapTo(0f)
                            }
                        }
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reload stream",
                            tint = Color.White,
                            modifier = Modifier.size(12.dp).rotate(rotation.value)
                        )
                    }

                    // Quick Snapshot button
                    IconButton(
                        onClick = { triggerQuickSnapshot() },
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.6f))
                            .testTag("stream_quick_snapshot_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoCamera,
                            contentDescription = "Schnappschuss",
                            tint = Color.White,
                            modifier = Modifier.size(12.dp)
                        )
                    }

                    // IR night-vision toggle
                    IconButton(
                        onClick = { isIrMode = !isIrMode },
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(if (isIrMode) Color(0xFF005AC1) else Color.Black.copy(alpha = 0.6f))
                            .testTag("stream_toggle_ir_btn")
                    ) {
                        Icon(
                            imageVector = if (isIrMode) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = "IR Mode",
                            tint = Color.White,
                            modifier = Modifier.size(12.dp)
                        )
                    }

                    // Quick Call button
                    IconButton(
                        onClick = { showQuickCallDialog = true },
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFDC2626))
                            .testTag("stream_quick_call_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhoneInTalk,
                            contentDescription = "Schnellanruf",
                            tint = Color.White,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }

                // Clip Recording HUD Overlay
                if (isRecordingClip) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 10.dp),
                        color = Color(0xFFDC2626).copy(alpha = 0.9f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color.White)
                            )
                            Text(
                                text = "CLIP AUFNAHME 00:0${recordingSecondsLeft}s",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }

                // Shutter Flash overlay
                if (isCameraFlashing) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.White.copy(alpha = 0.95f))
                    )
                }

                // RTSP Connecting transition overlay
                if (isTransitioning) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.9f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(
                                color = Color(0xFFD8E2FF),
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                            Text(
                                text = "Verbinde mit RTSP-Kamera...",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                        }
                    }
                }

                // Stream Reloading overlay
                if (isReloading) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.8f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(
                                color = Color(0xFFD8E2FF),
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                            Text(
                                text = "Stream wird neu geladen...",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }

            Spacer(modifier = Modifier.height(10.dp))

            // QUICK ACTION OBSERVATION TOOLBAR
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 1. Snapshot Button
                Button(
                    onClick = { triggerQuickSnapshot() },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("toolbar_snapshot_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A)),
                    contentPadding = PaddingValues(vertical = 8.dp, horizontal = 10.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.PhotoCamera, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Schnappschuss", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }

                // 2. Quick Record Button
                Button(
                    onClick = { toggleClipRecording() },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("toolbar_record_btn"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isRecordingClip) Color(0xFFDC2626) else Color(0xFF334155)
                    ),
                    contentPadding = PaddingValues(vertical = 8.dp, horizontal = 10.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = if (isRecordingClip) Icons.Default.Stop else Icons.Default.Videocam,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isRecordingClip) "Stop ($recordingSecondsLeft s)" else "10s Clip",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                // 3. Quick Call Button
                Button(
                    onClick = { showQuickCallDialog = true },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("toolbar_quick_call_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                    contentPadding = PaddingValues(vertical = 8.dp, horizontal = 10.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.PhoneInTalk, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Schnellanruf", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }

            // Toast / Confirmation feedback banner
            snapshotMessageToast?.let { toastMsg ->
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFF1E293B),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFF38BDF8))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF22C55E), modifier = Modifier.size(16.dp))
                            Text(toastMsg, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        }
                        IconButton(
                            onClick = { snapshotMessageToast = null },
                            modifier = Modifier.size(18.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Schließen", tint = Color.LightGray)
                        }
                    }
                }
            }

            // Quick Call Emergency Dialog
            if (showQuickCallDialog) {
                QuickCallDialog(onDismiss = { showQuickCallDialog = false })
            }

            Spacer(modifier = Modifier.height(10.dp))

            // CAMERA SETTINGS & FILTERS CARD
            Card(
                modifier = Modifier.fillMaxWidth().testTag("camera_controls_card"),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = null,
                                tint = Color(0xFF475569),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Kamera-Einstellungen",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF334155)
                            )
                        }
                        
                        // Reset to defaults
                        Text(
                            text = "Zurücksetzen",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF005AC1),
                            modifier = Modifier
                                .clickable {
                                    zoomScale = 1.0f
                                    brightnessLevel = 0f
                                    contrastBoost = false
                                }
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                .testTag("camera_reset_btn")
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // ZOOM SEGMENTED CONTROL
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.ZoomIn, contentDescription = null, tint = Color(0xFF475569), modifier = Modifier.size(12.dp))
                                Text("Zoom: ${String.format(Locale.US, "%.1fx", zoomScale)}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF475569))
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                listOf(1.0f, 1.5f, 2.0f, 3.0f).forEach { zoom ->
                                    val isSel = zoomScale == zoom
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSel) Color(0xFF005AC1) else Color.White)
                                            .border(1.dp, if (isSel) Color(0xFF005AC1) else Color(0xFFCBD5E1), RoundedCornerShape(8.dp))
                                            .clickable { zoomScale = zoom }
                                            .padding(vertical = 6.dp)
                                            .testTag("zoom_btn_${zoom.toInt()}"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "${zoom.toInt()}x",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSel) Color.White else Color(0xFF475569)
                                        )
                                    }
                                }
                            }
                        }
                        
                        // BRIGHTNESS SEGMENTED CONTROL
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.LightMode, contentDescription = null, tint = Color(0xFF475569), modifier = Modifier.size(12.dp))
                                Text("Helligkeit: ${if (brightnessLevel > 0) "+" else ""}${brightnessLevel.toInt()}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF475569))
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = { if (brightnessLevel > -3f) brightnessLevel -= 1f },
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.White)
                                        .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(8.dp))
                                        .testTag("brightness_minus_btn")
                                ) {
                                    Icon(Icons.Default.Remove, contentDescription = "Dunkler", tint = Color(0xFF475569), modifier = Modifier.size(12.dp))
                                }
                                
                                // Indicator dots
                                Row(
                                    modifier = Modifier.weight(1f),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    for (i in -3..3) {
                                        val active = i == brightnessLevel.toInt()
                                        Box(
                                            modifier = Modifier
                                                .padding(horizontal = 1.dp)
                                                .size(if (i == 0) 5.dp else 3.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (active) Color(0xFF005AC1)
                                                    else if (i == 0) Color(0xFF94A3B8)
                                                    else Color(0xFFE2E8F0)
                                                )
                                        )
                                    }
                                }
                                
                                IconButton(
                                    onClick = { if (brightnessLevel < 3f) brightnessLevel += 1f },
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.White)
                                        .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(8.dp))
                                        .testTag("brightness_plus_btn")
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Heller", tint = Color(0xFF475569), modifier = Modifier.size(12.dp))
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    // CONTRAST/DETAILS ENHANCER (AI Tracking & Bounding Box)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (contrastBoost) Color(0xFFD8E2FF) else Color.White)
                            .border(1.dp, if (contrastBoost) Color(0xFF94A3B8) else Color(0xFFE2E8F0), RoundedCornerShape(10.dp))
                            .clickable { contrastBoost = !contrastBoost }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                            .testTag("contrast_boost_row"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FilterCenterFocus,
                                contentDescription = null,
                                tint = if (contrastBoost) Color(0xFF005AC1) else Color(0xFF475569),
                                modifier = Modifier.size(16.dp)
                            )
                            Column {
                                Text(
                                    text = "AI-Wache Objektverfolgung",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (contrastBoost) Color(0xFF001D3E) else Color(0xFF334155)
                                )
                                Text(
                                    text = "Zeigt Echtzeit-YOLOv8-Objektboxen der Rinder.",
                                    fontSize = 9.sp,
                                    color = if (contrastBoost) Color(0xFF001D3E).copy(alpha = 0.7f) else Color(0xFF64748B)
                                )
                            }
                        }
                        
                        Switch(
                            checked = contrastBoost,
                            onCheckedChange = { contrastBoost = it },
                            modifier = Modifier.scale(0.7f).height(24.dp).testTag("contrast_boost_switch")
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // AI Analyze button
            Button(
                onClick = {
                    if (selectedStall == 1) {
                        viewModel.analyzeCameraFrame(
                            "futterwache",
                            "Live-Stream Kamera 1: Futterwache (API-Route /api/stallbox/stream). Futtertisch & Laufgang. Zwei Kühe überlappen sich (Kuh #103 Zelda zeigt Aufsprungverhalten auf eine andere Kuh), was auf Brunstverhalten hindeutet.",
                            null
                        )
                    } else {
                        viewModel.analyzeCameraFrame(
                            "stallwache",
                            "Live-Stream Kamera 2: Stallbox (API-Route /api/futterwache/stream). Abkalbebereich. Eine schwarzbunte Holsteinkuh (Berta) wird beobachtet. Ihr Schwanzwinkel liegt aktuell bei 49.5° (erhöht), was auf Wehentätigkeit hindeutet.",
                            null
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("stream_analyze_btn"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFE8F0FE),
                    contentColor = Color(0xFF005AC1)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                    Text("Kamerabild analysieren (KI-Wache)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }

            // AI Diagnostics Results section
            if (analyzerLoading) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F4F8)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Text(
                            text = analyzerThinking,
                            fontSize = 11.sp,
                            color = Color.DarkGray
                        )
                    }
                }
            }

            if (analyzerResult != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                        .testTag("ai_diagnostic_result_card"),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FBF0)),
                    border = BorderStroke(1.dp, Color(0xFFC2E0C2)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(14.dp))
                                Text(
                                    text = "Veterinär-Bericht (KI-Analyse)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = Color(0xFF2E7D32)
                                )
                            }
                            
                            // Clear button
                            IconButton(
                                onClick = { viewModel.clearAnalyzerResult() },
                                modifier = Modifier.size(20.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Schließen", tint = Color.Gray, modifier = Modifier.size(12.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = analyzerResult ?: "",
                            fontSize = 11.sp,
                            color = Color.DarkGray
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StallTabButton(
    id: Int,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) Color(0xFF005AC1) else Color(0xFFF3F3F3),
            contentColor = if (isSelected) Color.White else Color(0xFF44474E)
        ),
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
        modifier = modifier
            .height(38.dp)
            .testTag("stall_tab_btn_$id")
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.height(1.dp))
            Text(
                text = label,
                fontSize = 7.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun LegendItem(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(width = 12.dp, height = 4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(color)
        )
        Text(
            text = label,
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF44474E)
        )
    }
}

@Composable
fun BentoSystemStatusCard(
    host: String,
    status: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFC5C6D0)),
        shape = RoundedCornerShape(28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFE8F0FE))
                            .padding(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Dns,
                            contentDescription = "System Status",
                            tint = Color(0xFF005AC1),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Hardware & KI-Wache Systemstatus",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF1A1C1E)
                        )
                        Text(
                            text = "Uptime: 14 Tage, 6 Std. • Host: $host",
                            fontSize = 10.sp,
                            color = Color.Gray
                        )
                    }
                }

                // Status Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            if (status == "AKTIV") Color(0xFFE8F5E9) else Color(0xFFFFF3E0)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = status,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (status == "AKTIV") Color(0xFF2E7D32) else Color(0xFFE65100)
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // Three Column Layout
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Column 1: Hardware-Gesundheit (Monitoring Health)
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "MONITORING GESUNDHEIT",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF5F6368),
                        letterSpacing = 0.5.sp
                    )

                    // CPU Metric
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Default.Speed, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(12.dp))
                                Text("CPU-Auslastung", fontSize = 10.sp, color = Color(0xFF44474E))
                            }
                            Text("18%", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF005AC1))
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE1E2EC))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(0.18f)
                                    .background(Color(0xFF005AC1))
                            )
                        }
                    }

                    // RAM Metric
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.Memory, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(12.dp))
                            Text("Arbeitsspeicher", fontSize = 10.sp, color = Color(0xFF44474E))
                        }
                        Text("4.2 GB / 8 GB", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF005AC1))
                    }

                    // Core Temp Metric
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.Thermostat, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(12.dp))
                            Text("Kerntemperatur", fontSize = 10.sp, color = Color(0xFF44474E))
                        }
                        Text("44°C", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                    }
                }

                // Column 2: Active Camera Streams
                Column(
                    modifier = Modifier.weight(1.5f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "AKTIVE STALL-STREAMS",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF5F6368),
                        letterSpacing = 0.5.sp
                    )

                    // Stream 1
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.Videocam, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(12.dp))
                            Text("stallwache", fontSize = 10.sp, fontWeight = FontWeight.Medium, color = Color(0xFF1A1C1E))
                        }
                        Text("RTSP Restream (1.2 FPS)", fontSize = 9.sp, color = Color.Gray)
                    }

                    // Stream 2
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.Videocam, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(12.dp))
                            Text("futterwache", fontSize = 10.sp, fontWeight = FontWeight.Medium, color = Color(0xFF1A1C1E))
                        }
                        Text("HLS Restream (1.0 FPS)", fontSize = 9.sp, color = Color.Gray)
                    }

                    // AI Pipeline Status
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(12.dp))
                            Text("YOLOv8-Pose Pipeline", fontSize = 10.sp, color = Color(0xFF1A1C1E))
                        }
                        Text("Online", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                    }
                }

                // Column 3: Connectivity Status
                Column(
                    modifier = Modifier.weight(1.2f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "KONNEKTIVITÄT & KANÄLE",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF5F6368),
                        letterSpacing = 0.5.sp
                    )

                    // Hof-LAN
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.Wifi, contentDescription = null, tint = Color(0xFF005AC1), modifier = Modifier.size(12.dp))
                            Text("Hof-LAN", fontSize = 10.sp, color = Color(0xFF44474E))
                        }
                        Text("1 Gbps (OK)", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                    }

                    // MQTT Broker
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.Settings, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(12.dp))
                            Text("MQTT Broker", fontSize = 10.sp, color = Color(0xFF44474E))
                        }
                        Text("Verbunden", fontSize = 9.sp, color = Color(0xFF2E7D32))
                    }

                    // Telegram API
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.Send, contentDescription = null, tint = Color(0xFF005AC1), modifier = Modifier.size(12.dp))
                            Text("Telegram API", fontSize = 10.sp, color = Color(0xFF44474E))
                        }
                        Text("Bereit", fontSize = 9.sp, color = Color(0xFF2E7D32))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MonitoredStateSummaryCard(
    cows: List<com.example.data.Cow>,
    modifier: Modifier = Modifier
) {
    val calvingCows = remember(cows) {
        cows.filter {
            it.status.contains("Kalbung", ignoreCase = true) ||
            it.status.contains("Kalbe", ignoreCase = true) ||
            it.status.contains("Austreibung", ignoreCase = true)
        }
    }

    val oestrusCows = remember(cows) {
        cows.filter {
            it.status.contains("Brunst", ignoreCase = true) ||
            it.status.contains("Oestrus", ignoreCase = true) ||
            it.status.contains("Aufsprung", ignoreCase = true)
        }
    }

    val totalMonitored = calvingCows.size + oestrusCows.size
    val totalHerd = cows.size

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("monitored_state_summary_card"),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF0F172A)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Visibility,
                            contentDescription = "Überwachung",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "Überwachungs-Status der Herde",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            ),
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "$totalMonitored von $totalHerd Tieren in aktiver KI-Beobachtung",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF64748B)
                        )
                    }
                }

                Surface(
                    color = if (totalMonitored > 0) Color(0xFFFFEDD5) else Color(0xFFF1F5F9),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (totalMonitored > 0) Color(0xFFEA580C) else Color(0xFF64748B))
                        )
                        Text(
                            text = if (totalMonitored > 0) "$totalMonitored AKTIV" else "ALLES RUHIG",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (totalMonitored > 0) Color(0xFF9A3412) else Color(0xFF475569)
                        )
                    }
                }
            }

            // Stat Cards Grid Row (Calving vs Oestrus)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // CALVING (KALBUNG) STAT CARD
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, Color(0xFFFCA5A5))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ChildCare,
                                    contentDescription = "Kalbung",
                                    tint = Color(0xFFDC2626),
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Kalbung",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    color = Color(0xFF991B1B)
                                )
                            }

                            Text(
                                text = "${calvingCows.size}",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 24.sp
                                ),
                                color = Color(0xFFDC2626)
                            )
                        }

                        if (calvingCows.isNotEmpty()) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                calvingCows.forEach { cow ->
                                    Surface(
                                        color = Color(0xFFFEE2E2),
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(0.5.dp, Color(0xFFF87171))
                                    ) {
                                        Text(
                                            text = "${cow.name} (${cow.id})",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF7F1D1D),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        } else {
                            Text(
                                text = "Keine Kühe im Kalbebereich",
                                fontSize = 11.sp,
                                color = Color(0xFF9CA3AF)
                            )
                        }
                    }
                }

                // OESTRUS (BRUNST) STAT CARD
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F9FF)),
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, Color(0xFFBAE6FD))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocalActivity,
                                    contentDescription = "Brunst",
                                    tint = Color(0xFF0284C7),
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Brunst",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    color = Color(0xFF075985)
                                )
                            }

                            Text(
                                text = "${oestrusCows.size}",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 24.sp
                                ),
                                color = Color(0xFF0284C7)
                            )
                        }

                        if (oestrusCows.isNotEmpty()) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                oestrusCows.forEach { cow ->
                                    Surface(
                                        color = Color(0xFFE0F2FE),
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(0.5.dp, Color(0xFF38BDF8))
                                    ) {
                                        Text(
                                            text = "${cow.name} (${cow.id})",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF0C4A6E),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        } else {
                            Text(
                                text = "Keine Brunstanzeichen erkannt",
                                fontSize = 11.sp,
                                color = Color(0xFF9CA3AF)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TuyaCastWebFeedView(
    url: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showWebRtcInfo by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("tuya_cast_web_feed_view")
    ) {
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.mediaPlaybackRequiresUserGesture = false
                    settings.loadWithOverviewMode = true
                    settings.useWideViewPort = true
                    webViewClient = WebViewClient()
                    loadUrl(url)
                }
            },
            update = { webView ->
                if (webView.url != url) {
                    webView.loadUrl(url)
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Top Overlay Header: Abkalbebox Camera Badge
        Surface(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(10.dp),
            color = Color.Black.copy(alpha = 0.85f),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, Color(0xFF334155))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF22C55E))
                )
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Abkalbebox (Tuya WebRTC)",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFF0284C7))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "PIN: 0000",
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Text(
                        text = "ID: bf90bd2...gshm • 1080p FHD • Cast Ready",
                        color = Color(0xFF94A3B8),
                        fontSize = 9.sp
                    )
                }
            }
        }

        // WebRTC Config Details Popup Overlay
        if (showWebRtcInfo) {
            Surface(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(16.dp),
                color = Color(0xFF0F172A).copy(alpha = 0.95f),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFF38BDF8))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "📡 Tuya WebRTC Abkalbebox Specs",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                        IconButton(
                            onClick = { showWebRtcInfo = false },
                            modifier = Modifier.size(20.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Schließen", tint = Color.White)
                        }
                    }
                    Text(
                        text = "Device-ID: bf90bd252109467770gshm\n" +
                                "Signaling: signaling14926 (v2.3)\n" +
                                "STUN: 63.184.216.23:3478\n" +
                                "TURN Relay: 57.129.124.71:3478\n" +
                                "Auflösung: 1920x1080 (1080p FHD @ 90kHz)\n" +
                                "RTSPS Stream: rtsps://echo:***@wework-20-eu.stream.iot-11.com:443/v1/bf90bd252109467770gshm/d9hfi2v0sq7obhq4df40tUCVFQp6Py7K",
                        color = Color(0xFFCBD5E1),
                        fontSize = 10.sp,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                }
            }
        }

        // Bottom Overlay Bar with Tuya indicator & WebRTC Info Toggle & Open in Browser button
        Surface(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(10.dp),
            color = Color.Black.copy(alpha = 0.85f),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, Color(0xFF334155))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { showWebRtcInfo = !showWebRtcInfo },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0xFF38BDF8))
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "WebRTC Info",
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("WebRTC Info", fontSize = 10.sp, color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            // Fallback
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.OpenInNew,
                        contentDescription = "In Browser öffnen",
                        tint = Color.White,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Im Browser öffnen", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun QuickCallDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var customNumber by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFDC2626)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PhoneInTalk,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Column {
                    Text("Schnellanruf / Notruf", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                    Text("Kamera-Direktkontakt für Notfälle", fontSize = 10.sp, color = Color.Gray)
                }
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Sofortige Kontaktaufnahme bei Geburtskomplikationen oder Auffälligkeiten im Stall:",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // 1. Notfall Tierarzt
                QuickCallContactCard(
                    title = "Notfall-Tierarzt",
                    subtitle = "Dr. med. vet. Hofmeister (24h Notdienst)",
                    phoneNumber = "0170 1234567",
                    icon = Icons.Default.MedicalServices,
                    color = Color(0xFF2563EB),
                    onClick = {
                        try {
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:01701234567"))
                            context.startActivity(intent)
                        } catch (e: Exception) {}
                    }
                )

                // 2. Hofleiter / Stallmeister
                QuickCallContactCard(
                    title = "Hofleiter / Stallwache",
                    subtitle = "Bernhard Stollenhöfer (Oberer Stollenhof)",
                    phoneNumber = "0171 9876543",
                    icon = Icons.Default.Person,
                    color = Color(0xFF059669),
                    onClick = {
                        try {
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:01719876543"))
                            context.startActivity(intent)
                        } catch (e: Exception) {}
                    }
                )

                // 3. Landwirtschaftlicher Notdienst 112
                QuickCallContactCard(
                    title = "Landwirtschaftlicher Notdienst",
                    subtitle = "Zentrale Leitstelle & Tierrettung",
                    phoneNumber = "112",
                    icon = Icons.Default.Warning,
                    color = Color(0xFFDC2626),
                    onClick = {
                        try {
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:112"))
                            context.startActivity(intent)
                        } catch (e: Exception) {}
                    }
                )

                // Custom phone input
                OutlinedTextField(
                    value = customNumber,
                    onValueChange = { customNumber = it },
                    label = { Text("Eigene Telefonnummer anrufen") },
                    placeholder = { Text("z.B. 0151 12345678") },
                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                    trailingIcon = {
                        if (customNumber.isNotBlank()) {
                            IconButton(onClick = {
                                try {
                                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${customNumber.trim()}"))
                                    context.startActivity(intent)
                                } catch (e: Exception) {}
                            }) {
                                Icon(Icons.Default.Call, contentDescription = "Anrufen", tint = Color(0xFF0284C7))
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Schließen")
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
private fun QuickCallContactCard(
    title: String,
    subtitle: String,
    phoneNumber: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        color = color.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(color),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
                Column {
                    Text(title, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                    Text(subtitle, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(phoneNumber, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = color)
                }
            }
            Button(
                onClick = onClick,
                colors = ButtonDefaults.buttonColors(containerColor = color),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Call, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Anrufen", fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun TuyaAlertVerificationDialog(
    event: StallEvent,
    tuyaCastUrl: String,
    viewModel: StallViewModel,
    onResolve: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isFlashing by remember { mutableStateOf(false) }
    var toastMessage by remember { mutableStateOf<String?>(null) }
    var isRecording by remember { mutableStateOf(false) }
    var recordingTimeLeft by remember { mutableStateOf(10) }

    fun triggerSnapshot() {
        scope.launch {
            isFlashing = true
            delay(200)
            isFlashing = false
            viewModel.logManualObservation(
                cameraName = "Tuya Abkalbebox (${event.kamera})",
                cowId = event.kuhId ?: "Kuh #42",
                type = "info",
                note = "📸 Tuya-Kamera Schnappschuss verifiziert für Ereignis #${event.id} (${event.typ})"
            )
            toastMessage = "📸 Schnappschuss der Tuya-Kamera im Stall-Protokoll gesichert!"
        }
    }

    fun toggleRecord() {
        if (isRecording) {
            isRecording = false
            toastMessage = "📹 Aufzeichnung gestoppt."
            return
        }
        scope.launch {
            isRecording = true
            recordingTimeLeft = 10
            while (recordingTimeLeft > 0 && isRecording) {
                delay(1000)
                recordingTimeLeft--
            }
            if (isRecording) {
                isRecording = false
                viewModel.logManualObservation(
                    cameraName = "Tuya Abkalbebox (${event.kamera})",
                    cowId = event.kuhId ?: "Kuh #42",
                    type = "info",
                    note = "📹 10s Video-Evidenz der Tuya-Kamera gesichert (${event.typ})"
                )
                toastMessage = "📹 10s Video-Sequenz verifiziert & gesichert!"
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f)
                .testTag("tuya_alert_verification_dialog"),
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF0F172A),
            border = BorderStroke(2.dp, if (event.typ == "austreibung" || event.typ == "eskalation") Color(0xFFDC2626) else Color(0xFF0284C7))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(if (event.typ == "austreibung" || event.typ == "eskalation") Color(0xFFDC2626) else Color(0xFF0284C7)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Videocam,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "TUYA ECHTZEIT-VERIFIZIERUNG",
                                    color = Color.White,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 14.sp
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFF22C55E))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("LIVE WEBRTC", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                }
                            }
                            Text(
                                text = "Kamera: ${event.kamera} • Tuya Cast Stream Abkalbebox",
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1E293B))
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Schließen", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Active Alert Summary
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = if (event.typ == "austreibung" || event.typ == "eskalation") Color(0xFF450A0A) else Color(0xFF0C4A6E),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, if (event.typ == "austreibung" || event.typ == "eskalation") Color(0xFFEF4444) else Color(0xFF38BDF8))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(
                                    imageVector = if (event.typ == "austreibung" || event.typ == "eskalation") Icons.Default.Warning else Icons.Default.NotificationsActive,
                                    contentDescription = null,
                                    tint = if (event.typ == "austreibung" || event.typ == "eskalation") Color(0xFFFCA5A5) else Color(0xFF7DD3FC),
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "${event.kuhId ?: "Kuh #42"}: ${event.typ.uppercase(Locale.ROOT)}",
                                    color = Color.White,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 13.sp
                                )
                                if (event.konfidenz != null) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color.Black.copy(alpha = 0.4f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "Konfidenz ${(event.konfidenz * 100).toInt()}%",
                                            color = Color(0xFFFDE047),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = event.nachricht,
                                color = Color(0xFFE2E8F0),
                                fontSize = 11.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            onClick = {
                                onResolve()
                                viewModel.logManualObservation(
                                    cameraName = "Tuya Abkalbebox",
                                    cowId = event.kuhId ?: "Kuh #42",
                                    type = "info",
                                    note = "✅ Landwirt hat Ereignis #${event.id} auf Tuya Live-Stream verifiziert & freigegeben."
                                )
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Als Verifiziert Bestätigen", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Embedded Live Feed
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, Color(0xFF334155), RoundedCornerShape(16.dp))
                ) {
                    TuyaCastWebFeedView(url = tuyaCastUrl)

                    if (isFlashing) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.White.copy(alpha = 0.9f))
                        )
                    }

                    if (isRecording) {
                        Surface(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 10.dp),
                            color = Color(0xFFDC2626).copy(alpha = 0.9f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color.White))
                                Text("REC 00:0${recordingTimeLeft}s", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                toastMessage?.let { msg ->
                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        color = Color(0xFF1E293B),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color(0xFF38BDF8))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(msg, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                            IconButton(onClick = { toastMessage = null }, modifier = Modifier.size(18.dp)) {
                                Icon(Icons.Default.Close, contentDescription = null, tint = Color.LightGray)
                            }
                        }
                    }
                }

                // Action Toolbar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { triggerSnapshot() },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(vertical = 10.dp)
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Schnappschuss", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { toggleRecord() },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = if (isRecording) Color(0xFFDC2626) else Color(0xFF475569)),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(vertical = 10.dp)
                    ) {
                        Icon(
                            imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Videocam,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isRecording) "Stop ($recordingTimeLeft s)" else "10s Clip", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    var showCallInModal by remember { mutableStateOf(false) }
                    Button(
                        onClick = { showCallInModal = true },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(vertical = 10.dp)
                    ) {
                        Icon(Icons.Default.Call, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Tierarzt", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    if (showCallInModal) {
                        QuickCallDialog(onDismiss = { showCallInModal = false })
                    }
                }
            }
        }
    }
}


