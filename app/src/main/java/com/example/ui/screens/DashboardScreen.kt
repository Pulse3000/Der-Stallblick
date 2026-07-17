package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.StallEvent
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

    var eventFilter by remember { mutableStateOf("ALL") } // "ALL", "CRITICAL", "INFO"

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

    LazyColumn(
        modifier = modifier
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

        // --- BENTO GRID: MAIN ALERT CARD (High Priority) ---
        item {
            BentoAlertCard(
                viewModel = viewModel,
                activeWarning = activeWarning,
                onResolve = { activeWarning?.let { viewModel.markEventResolved(it.id) } }
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
                    onResolve = { viewModel.markEventResolved(event.id) }
                )
            }
        }

        // Bottom space padding
        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

// --- BENTO COMPOSABLE: MAIN ALERT CARD ---
@Composable
fun BentoAlertCard(
    viewModel: StallViewModel,
    activeWarning: StallEvent?,
    onResolve: () -> Unit,
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
    onResolve: () -> Unit
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

            // Quick Resolve Checkbox Button
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
    var selectedStall by remember { mutableStateOf(1) } // 1: Stall 1 (Abkalbebereich), 2: Stall 2 (Futterplatz), 3: Stall 3 (Ruhebereich), 4: Stall 4 (Melkstand)
    var isTransitioning by remember { mutableStateOf(false) }
    var isReloading by remember { mutableStateOf(false) }
    var isIrMode by remember { mutableStateOf(false) }

    LaunchedEffect(selectedStall) {
        isTransitioning = true
        delay(1200)
        isTransitioning = false
    }

    var timecodeText by remember { mutableStateOf("00:00:00") }
    LaunchedEffect(Unit) {
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.GERMANY)
        while (true) {
            timecodeText = sdf.format(Date())
            delay(1000)
        }
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

    val breathingValue by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathing"
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
                        text = "Barn Live Cam & Stall-Wahl",
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

            // Stall Selector Tabs
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                StallTabButton(
                    id = 1,
                    label = "Box 1: Abkalbe",
                    icon = Icons.Default.Home,
                    isSelected = selectedStall == 1,
                    onClick = { selectedStall = 1 },
                    modifier = Modifier.weight(1f)
                )
                StallTabButton(
                    id = 2,
                    label = "Box 2: Futter",
                    icon = Icons.Default.Pets,
                    isSelected = selectedStall == 2,
                    onClick = { selectedStall = 2 },
                    modifier = Modifier.weight(1f)
                )
                StallTabButton(
                    id = 3,
                    label = "Box 3: Ruhe",
                    icon = Icons.Default.Favorite,
                    isSelected = selectedStall == 3,
                    onClick = { selectedStall = 3 },
                    modifier = Modifier.weight(1f)
                )
                StallTabButton(
                    id = 4,
                    label = "Box 4: Melken",
                    icon = Icons.Default.FlashOn,
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
                // Drawing Stream content
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(if (isIrMode) Color(0xFF1C1C1C) else Color(0xFF101411))
                ) {
                    val width = size.width
                    val height = size.height

                    when (selectedStall) {
                        1 -> {
                            // Stall 1: Calving Skeletal Cow (Berta)
                            drawCowSkeletalPose(
                                width = width,
                                height = height,
                                isTailRaised = (bertaCow?.lastAngle ?: 15f) > 40f,
                                status = bertaCow?.status ?: "Normal",
                                isIrMode = isIrMode
                            )
                        }
                        2 -> {
                            // Stall 2: Mounting Overlapping Behavior (Zelda)
                            drawMountingBehaviorPose(
                                width = width,
                                height = height,
                                isMounting = zeldaCow?.status == "Brunstverdacht",
                                isIrMode = isIrMode
                            )
                        }
                        3 -> {
                            // Stall 3: Sleeping Cow (Alma) with breathing animation
                            val groundColor = if (isIrMode) Color(0xFF757575) else Color(0xFF8D6E63)
                            val bodyColor = if (isIrMode) Color(0xFFE0E0E0) else Color(0xFFA1887F)
                            val keypointColor = if (isIrMode) Color.White else Color.Yellow

                            drawLine(groundColor, Offset(0f, height * 0.82f), Offset(width, height * 0.82f), strokeWidth = 3f)

                            val cx = width * 0.48f
                            val cy = height * 0.58f

                            // Draw resting body oval utilizing the Breathing Infinite Transition!
                            drawOval(
                                color = bodyColor.copy(alpha = if (isIrMode) 0.35f else 0.2f),
                                topLeft = Offset(cx - 70f * breathingValue, cy - 35f * breathingValue),
                                size = Size(140f * breathingValue, 70f * breathingValue)
                            )

                            // Head of the sleeping cow
                            drawCircle(
                                color = bodyColor.copy(alpha = if (isIrMode) 0.45f else 0.3f),
                                radius = 22f,
                                center = Offset(cx - 75f, cy + 10f)
                            )

                            // Sleeping keypoints (eye, shoulder, rump)
                            drawCircle(keypointColor, 5f, Offset(cx - 80f, cy + 12f)) // Eye (closed)
                            drawCircle(keypointColor, 6f, Offset(cx - 20f, cy - 10f)) // Spine/Shoulder
                            drawCircle(keypointColor, 6f, Offset(cx + 40f, cy + 10f)) // Hip/Rump
                        }
                        4 -> {
                            // Stall 4: Milking frame cow (Mona)
                            val groundColor = if (isIrMode) Color(0xFF757575) else Color(0xFF8D6E63)
                            val bodyColor = if (isIrMode) Color(0xFFE0E0E0) else Color(0xFFA1887F)
                            val frameColor = if (isIrMode) Color(0xFF9E9E9E) else Color(0xFF74777F)

                            drawLine(groundColor, Offset(0f, height * 0.82f), Offset(width, height * 0.82f), strokeWidth = 3f)

                            val cx = width * 0.45f
                            val cy = height * 0.48f

                            // Cow standing in Milking Partition Frame
                            drawRect(bodyColor.copy(alpha = if (isIrMode) 0.3f else 0.15f), Offset(cx - 50f, cy - 30f), Size(100f, 60f))

                            // Milking metal frame partitions
                            drawLine(frameColor, Offset(cx - 65f, cy - 40f), Offset(cx - 65f, cy + 50f), strokeWidth = 4f)
                            drawLine(frameColor, Offset(cx + 65f, cy - 40f), Offset(cx + 65f, cy + 50f), strokeWidth = 4f)
                            drawLine(frameColor, Offset(cx - 65f, cy - 40f), Offset(cx + 65f, cy - 40f), strokeWidth = 4f)

                            // Four teat cups pulsing
                            val pulseGreen = if (livePulseAlpha > 0.5f) Color(0xFF2E7D32) else Color(0xFF81C784)
                            drawCircle(pulseGreen, 5f, Offset(cx - 10f, cy + 40f))
                            drawCircle(pulseGreen, 5f, Offset(cx - 2f, cy + 40f))
                            drawCircle(pulseGreen, 5f, Offset(cx + 6f, cy + 40f))
                            drawCircle(pulseGreen, 5f, Offset(cx + 14f, cy + 40f))
                        }
                    }
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
                        .background(Color.Black.copy(alpha = 0.5f))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = when (selectedStall) {
                            1 -> "Box 1: Abkalbebereich"
                            2 -> "Box 2: Futtertisch"
                            3 -> "Box 3: Ruhebereich"
                            else -> "Box 4: Melkstand"
                        },
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                    Text(
                        text = when (selectedStall) {
                            1 -> bertaCow?.let { "Kuh #42 (Berta) • Winkel: ${String.format(Locale.US, "%.1f", it.lastAngle)}°" } ?: "Keine Kuh im Fokus"
                            2 -> zeldaCow?.let { "Kuh #103 (Zelda) • Unruhig" } ?: "Keine Aktivität"
                            3 -> "Kuh #18 (Alma) • Ruht"
                            else -> "Kuh #88 (Mona) • Melkprozess aktiv"
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

