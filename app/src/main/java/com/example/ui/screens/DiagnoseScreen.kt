package com.example.ui.screens

import androidx.compose.animation.*
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AnalysisReport
import com.example.viewmodel.StallViewModel
import java.text.SimpleDateFormat
import java.util.*
import android.graphics.Bitmap
import android.graphics.Paint
import android.util.Base64
import java.io.ByteArrayOutputStream
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextOverflow
import kotlinx.coroutines.delay

@Composable
fun DiagnoseScreen(
    viewModel: StallViewModel,
    modifier: Modifier = Modifier
) {
    val reports by viewModel.reports.collectAsState()
    val analyzerLoading by viewModel.analyzerLoading.collectAsState()
    val analyzerThinking by viewModel.analyzerThinking.collectAsState()
    val analyzerResult by viewModel.analyzerResult.collectAsState()

    var activeTab by remember { mutableStateOf("NEW") } // "NEW", "HISTORY"

    // --- Active Case Selection for Analysis ---
    val cases = listOf(
        DiagnosticCase(
            name = "Berta (Kuh #42) - Wehenbeginn",
            description = "Stallwache Kamera snapshot: Berta steht unruhig im Abkalbebox-Stroh, sie hat einen hoch erhobenen Schwanz (Winkel ca. 50 Grad gemessen) und tritt unruhig mit den Hinterbeinen. Keine Fruchtblase sichtbar.",
            status = "WARNUNG",
            details = "Regelbasierter Kalbeverdacht ausgelöst durch Schwanzwinkel über 45° in 22% der Frames."
        ),
        DiagnosticCase(
            name = "Berta (Kuh #42) - Austreibungsphase",
            description = "Stallwache Kamera snapshot: Berta liegt auf der Seite im Stroh. Eine große, dunkle Fruchtblase (amniotic_sac) ist hinten sichtbar. Sie presst aktiv. Keine Gliedmaßen sichtbar.",
            status = "AKUT",
            details = "Modellauswertung meldet amniotic_sac mit Konfidenz 0.95."
        ),
        DiagnosticCase(
            name = "Berta (Kuh #42) - Eskalation (Kein Fortschritt)",
            description = "Stallwache Kamera snapshot: Seit 70 Minuten ist die Fruchtblase sichtbar. Berta liegt erschöpft flach im Stroh. Es ist noch kein Kalb geboren. Keine Beine sichtbar. Wehen schwächen sich ab.",
            status = "ESKALATION",
            details = "Austreibungszeit überschreitet 60 Minuten ohne Geburtsfortschritt."
        ),
        DiagnosticCase(
            name = "Zelda (Kuh #103) - Aufsprungverhalten",
            description = "Futterwache Kamera snapshot: Zelda springt auf Alma auf. Alma bleibt ruhig stehen (Duldungsreflex). Zelda klammert sich an Almas Rücken fest. Das Verhalten dauert seit 6 Sekunden an.",
            status = "BRUNST",
            details = "YOLOv8-Pose meldet Rumpf-Überlappung IoU 0.22 für 6,2 Sekunden."
        ),
        DiagnosticCase(
            name = "Alma (Kuh #18) - Normalzustand",
            description = "Stallwache Kamera snapshot: Alma liegt entspannt im Liegebereich und kaut zufrieden wieder. Kopf ist ruhig erhoben, Atmung normal, Schwanz liegt flach an.",
            status = "NORMAL",
            details = "Keine Anomalien im Verhalten oder Skelett."
        )
    )

    var selectedCaseIndex by remember { mutableStateOf(0) }
    var customNotes by remember { mutableStateOf("") }
    var showThinkingProcess by remember { mutableStateOf(true) }

    var useLiveCameraFeed by remember { mutableStateOf(false) }
    var activeCameraFeed by remember { mutableStateOf("stallwache") }
    var liveIrMode by remember { mutableStateOf(false) }
    var showYoloOverlays by remember { mutableStateOf(true) }
    var simulatedNoise by remember { mutableStateOf(false) }
    var capturedImageBase64 by remember { mutableStateOf<String?>(null) }
    var capturedImageTime by remember { mutableStateOf<Long?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- Screen Title ---
        Column {
            Text(
                text = "KI-Diagnose",
                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.ExtraBold)
            )
            Text(
                text = "Nutze Gemini Pro 3.1 mit hohem Denkvermögen für veterinär-medizinische Kamera-Analysen.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // --- Tabs (New Analysis vs History) ---
        TabRow(
            selectedTabIndex = if (activeTab == "NEW") 0 else 1,
            containerColor = Color.Transparent,
            modifier = Modifier.fillMaxWidth()
        ) {
            Tab(
                selected = activeTab == "NEW",
                onClick = { activeTab = "NEW" },
                text = { Text("Neue Analyse") },
                icon = { Icon(Icons.Default.Analytics, contentDescription = null) }
            )
            Tab(
                selected = activeTab == "HISTORY",
                onClick = { activeTab = "HISTORY" },
                text = { Text("Verlauf (${reports.size})") },
                icon = { Icon(Icons.Default.History, contentDescription = null) }
            )
        }

        if (activeTab == "NEW") {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // --- Integration Mode Card ---
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F4F8)),
                        border = BorderStroke(1.dp, Color(0xFFC5C6D0)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Kamera-Eingangskanal wählen:",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF005AC1)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { useLiveCameraFeed = false },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (!useLiveCameraFeed) Color(0xFF005AC1) else Color(0xFFE1E2EC),
                                        contentColor = if (!useLiveCameraFeed) Color.White else Color(0xFF44474E)
                                    ),
                                    modifier = Modifier.weight(1f).height(38.dp).testTag("mode_static_btn"),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Icon(Icons.Default.List, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Feste Vorlagen", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                Button(
                                    onClick = { useLiveCameraFeed = true },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (useLiveCameraFeed) Color(0xFF005AC1) else Color(0xFFE1E2EC),
                                        contentColor = if (useLiveCameraFeed) Color.White else Color(0xFF44474E)
                                    ),
                                    modifier = Modifier.weight(1f).height(38.dp).testTag("mode_live_video_btn"),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Icon(Icons.Default.Videocam, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Live-Videofeed", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                if (!useLiveCameraFeed) {
                    // --- Case Selection Dropdown ---
                    item {
                        Text(
                            text = "Wähle eine Stallwache-Situation:",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        cases.forEachIndexed { index, diagnosticCase ->
                            val isSelected = index == selectedCaseIndex
                            val border = if (isSelected) BorderStroke(2.dp, Color(0xFF005AC1)) 
                                         else BorderStroke(1.dp, Color(0xFFC5C6D0))
                            val background = if (isSelected) Color(0xFFDEE1FF) 
                                             else Color.White
                            
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable { selectedCaseIndex = index }
                                    .testTag("case_select_$index"),
                                border = border,
                                colors = CardDefaults.cardColors(containerColor = background),
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .clip(CircleShape)
                                            .background(
                                                when (diagnosticCase.status) {
                                                    "AKUT", "ESKALATION" -> Color.Red
                                                    "WARNUNG" -> Color(0xFFFFA000)
                                                    "BRUNST" -> Color(0xFF00796B)
                                                    else -> Color(0xFF2E7D32)
                                                }
                                            )
                                    )
                                    Column {
                                        Text(
                                            text = diagnosticCase.name,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                        )
                                        Text(
                                            text = diagnosticCase.details,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // --- Live Stream Integration Component ---
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, Color(0xFFC5C6D0)),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
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
                                            imageVector = Icons.Default.Tv,
                                            contentDescription = null,
                                            tint = Color(0xFF005AC1),
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text(
                                            text = "Kamera-Stream Empfänger",
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = Color(0xFF1A1C1E)
                                        )
                                    }
                                    
                                    var ticksLive by remember { mutableStateOf(0) }
                                    LaunchedEffect(Unit) {
                                        while (true) {
                                            delay(1000)
                                            ticksLive += 1
                                        }
                                    }
                                    val isRecVisible = ticksLive % 2 == 0
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(if (isRecVisible) Color.Red else Color.Transparent)
                                        )
                                        Text(
                                            text = "EMPFANG",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.Red
                                        )
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(10.dp))
                                
                                Text(
                                    text = "Aktive Signalquelle:",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    listOf(
                                        "stallwache" to "Box 1: Abkalbe",
                                        "futterwache" to "Box 2: Futter",
                                        "ruhebereich" to "Box 3: Ruhe"
                                    ).forEach { (id, label) ->
                                        val isCamSelected = activeCameraFeed == id
                                        Button(
                                            onClick = { 
                                                activeCameraFeed = id
                                                capturedImageBase64 = null
                                                capturedImageTime = null
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (isCamSelected) Color(0xFF005AC1) else Color(0xFFF3F3F3),
                                                contentColor = if (isCamSelected) Color.White else Color(0xFF44474E)
                                            ),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                            modifier = Modifier.weight(1f).height(36.dp).testTag("live_source_btn_$id")
                                        ) {
                                            Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(12.dp))
                                
                                var flashTrigger by remember { mutableStateOf(false) }
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(16f / 9f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .border(1.dp, Color(0xFFC5C6D0), RoundedCornerShape(12.dp))
                                ) {
                                    var animTicks by remember { mutableStateOf(0) }
                                    LaunchedEffect(activeCameraFeed) {
                                        while (true) {
                                            delay(500)
                                            animTicks += 1
                                        }
                                    }
                                    
                                    Canvas(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(if (liveIrMode) Color(0xFF1C1C1C) else Color(0xFF101411))
                                    ) {
                                        val w = size.width
                                        val h = size.height
                                        
                                        if (simulatedNoise) {
                                            for (i in 0..h.toInt() step 12) {
                                                drawLine(
                                                    color = Color.White.copy(alpha = 0.08f),
                                                    start = Offset(0f, i.toFloat()),
                                                    end = Offset(w, i.toFloat()),
                                                    strokeWidth = 1f
                                                )
                                            }
                                        }
                                        
                                        when (activeCameraFeed) {
                                            "stallwache" -> {
                                                val tailRaised = animTicks % 5 != 0
                                                drawCowSkeletalPose(
                                                    width = w,
                                                    height = h,
                                                    isTailRaised = tailRaised,
                                                    status = if (tailRaised) "Austreibung" else "Normal",
                                                    isIrMode = liveIrMode
                                                )
                                                
                                                if (showYoloOverlays) {
                                                    drawRect(
                                                        color = Color.Red.copy(alpha = 0.5f),
                                                        topLeft = Offset(w * 0.2f, h * 0.25f),
                                                        size = Size(w * 0.55f, h * 0.58f),
                                                        style = Stroke(width = 3f)
                                                    )
                                                    if (tailRaised) {
                                                        drawRect(
                                                            color = Color.Magenta.copy(alpha = 0.6f),
                                                            topLeft = Offset(w * 0.55f, h * 0.5f),
                                                            size = Size(60f, 60f),
                                                            style = Stroke(width = 2f)
                                                        )
                                                    }
                                                }
                                            }
                                            "futterwache" -> {
                                                drawMountingBehaviorPose(
                                                    width = w,
                                                    height = h,
                                                    isMounting = true,
                                                    isIrMode = liveIrMode
                                                )
                                                if (showYoloOverlays) {
                                                    drawRect(
                                                        color = Color(0xFFFF9800),
                                                        topLeft = Offset(w * 0.22f, h * 0.18f),
                                                        size = Size(w * 0.56f, h * 0.65f),
                                                        style = Stroke(width = 3f)
                                                    )
                                                }
                                            }
                                            else -> {
                                                val groundColor = if (liveIrMode) Color(0xFF757575) else Color(0xFF8D6E63)
                                                val bodyColor = if (liveIrMode) Color(0xFFE0E0E0) else Color(0xFFA1887F)
                                                val keypointColor = if (liveIrMode) Color.White else Color.Yellow

                                                drawLine(groundColor, Offset(0f, h * 0.82f), Offset(w, h * 0.82f), strokeWidth = 3f)

                                                val cx = w * 0.48f
                                                val cy = h * 0.58f
                                                val breathingVal = 1.0f + 0.05f * kotlin.math.sin(animTicks.toDouble() * 0.5).toFloat()

                                                drawOval(
                                                    color = bodyColor.copy(alpha = if (liveIrMode) 0.35f else 0.2f),
                                                    topLeft = Offset(cx - 70f * breathingVal, cy - 35f * breathingVal),
                                                    size = Size(140f * breathingVal, 70f * breathingVal)
                                                )

                                                drawCircle(
                                                    color = bodyColor.copy(alpha = if (liveIrMode) 0.45f else 0.3f),
                                                    radius = 22f,
                                                    center = Offset(cx - 75f, cy + 10f)
                                                )

                                                if (showYoloOverlays) {
                                                    drawCircle(keypointColor, 5f, Offset(cx - 80f, cy + 12f))
                                                    drawCircle(keypointColor, 6f, Offset(cx - 20f, cy - 10f))
                                                    drawCircle(keypointColor, 6f, Offset(cx + 40f, cy + 10f))
                                                    
                                                    drawRect(
                                                        color = Color.Green,
                                                        topLeft = Offset(cx - 105f, cy - 45f),
                                                        size = Size(190f, 95f),
                                                        style = Stroke(width = 2f)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    
                                    Text(
                                        text = "CAM: ${activeCameraFeed.uppercase(Locale.ROOT)}",
                                        color = Color.Green,
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        modifier = Modifier
                                            .align(Alignment.TopStart)
                                            .padding(8.dp)
                                            .background(Color.Black.copy(alpha = 0.5f))
                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                    
                                    val timeStr = SimpleDateFormat("HH:mm:ss", Locale.GERMANY).format(Date())
                                    Text(
                                        text = timeStr,
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(8.dp)
                                            .background(Color.Black.copy(alpha = 0.5f))
                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                    
                                    var flashAlpha by remember { mutableStateOf(0f) }
                                    LaunchedEffect(flashTrigger) {
                                        if (flashTrigger) {
                                            flashAlpha = 0.8f
                                            while (flashAlpha > 0f) {
                                                delay(40)
                                                flashAlpha = (flashAlpha - 0.1f).coerceAtLeast(0f)
                                            }
                                            flashTrigger = false
                                        }
                                    }
                                    if (flashAlpha > 0f) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(Color.White.copy(alpha = flashAlpha))
                                        )
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(10.dp))
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Checkbox(
                                            checked = liveIrMode,
                                            onCheckedChange = { liveIrMode = it },
                                            modifier = Modifier.testTag("live_ir_toggle")
                                        )
                                        Text("IR Modus", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                    
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Checkbox(
                                            checked = showYoloOverlays,
                                            onCheckedChange = { showYoloOverlays = it },
                                            modifier = Modifier.testTag("live_yolo_toggle")
                                        )
                                        Text("Overlays", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                    
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Checkbox(
                                            checked = simulatedNoise,
                                            onCheckedChange = { simulatedNoise = it },
                                            modifier = Modifier.testTag("live_noise_toggle")
                                        )
                                        Text("Rauschen", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(10.dp))
                                
                                Button(
                                    onClick = {
                                        flashTrigger = true
                                        val isCalving = activeCameraFeed == "stallwache"
                                        val isEstrus = activeCameraFeed == "futterwache"
                                        capturedImageBase64 = generateMockCameraImage(
                                            cameraName = activeCameraFeed,
                                            isIrMode = liveIrMode,
                                            isCalving = isCalving,
                                            isEstrus = isEstrus
                                        )
                                        capturedImageTime = System.currentTimeMillis()
                                    },
                                    modifier = Modifier.fillMaxWidth().height(42.dp).testTag("capture_frame_btn"),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF006495)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.PhotoCamera, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Snapshot erfassen", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                                
                                capturedImageTime?.let { time ->
                                    Spacer(modifier = Modifier.height(10.dp))
                                    val snapTimeStr = SimpleDateFormat("HH:mm:ss", Locale.GERMANY).format(Date(time))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color(0xFFE8F5E9))
                                            .border(1.dp, Color(0xFFA5D6A7), RoundedCornerShape(12.dp))
                                            .padding(10.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = Color(0xFF2E7D32),
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Column {
                                                Text(
                                                    text = "Snapshot erfolgreich kodiert! ($snapTimeStr Uhr)",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF1B5E20)
                                                )
                                                Text(
                                                    text = "Das JPEG-Bild wurde in Base64 umgewandelt und wird beim Starten direkt an die Gemini API übertragen.",
                                                    fontSize = 9.sp,
                                                    color = Color(0xFF2E7D32)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // --- Additional notes ---
                item {
                    Text(
                        text = "Zusätzliche Fragen / Notizen an die KI:",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = customNotes,
                        onValueChange = { customNotes = it },
                        modifier = Modifier.fillMaxWidth().testTag("custom_notes_input"),
                        placeholder = { Text("z.B. Ist das Fruchtwasser grünlich? Soll ich den Tierarzt rufen? Soll ich das Kalb ziehen?") },
                        minLines = 2,
                        maxLines = 4,
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                // --- Run Button ---
                item {
                    Button(
                        onClick = {
                            if (useLiveCameraFeed && capturedImageBase64 != null) {
                                val camLabel = when (activeCameraFeed) {
                                    "stallwache" -> "Stallwache (Live-Snapshot)"
                                    "futterwache" -> "Futterwache (Live-Snapshot)"
                                    else -> "Ruhebereich (Live-Snapshot)"
                                }
                                val dynamicDesc = when (activeCameraFeed) {
                                    "stallwache" -> "Live-Snapshot von der Stallwache (Abkalbebereich). Eine Kuh (Berta Kuh #42) steht unruhig im Stroh. YOLOv8-Pose hat einen erhöhten Schwanzwinkel von 47.5° gemessen."
                                    "futterwache" -> "Live-Snapshot von der Futterwache (Futtertisch). Zwei Kühe überlappen sich im Bild (Zelda Kuh #103 springt auf Alma Kuh #18 auf). YOLO-Detektionen zeigen Rumpfüberlappung IoU 0.22 seit mehreren Sekunden."
                                    else -> "Live-Snapshot vom Ruhebereich. Die Kühe liegen friedlich im Liegebereich, atmen ruhig und kauen wieder."
                                }
                                val fullDesc = "$dynamicDesc\nFrage des Landwirts: ${customNotes.ifEmpty { "Keine Zusatzfragen." }}"
                                viewModel.analyzeCameraFrame(camLabel, fullDesc, imageBase64 = capturedImageBase64)
                            } else {
                                val activeCase = cases[selectedCaseIndex]
                                val fullDesc = "${activeCase.description}\nFrage des Landwirts: ${customNotes.ifEmpty { "Keine Zusatzfragen." }}"
                                viewModel.analyzeCameraFrame(activeCase.name, fullDesc, imageBase64 = null)
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("trigger_analysis_btn"),
                        enabled = !analyzerLoading && (!useLiveCameraFeed || capturedImageBase64 != null),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        if (analyzerLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("KI denkt nach (High Thinking)...")
                        } else {
                            Icon(Icons.Default.Bolt, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                if (useLiveCameraFeed && capturedImageBase64 == null) "Erfasse zuerst einen Snapshot"
                                else "Gemini Pro Deep-Analysis starten"
                            )
                        }
                    }
                }

                // --- Analyzer Results Area ---
                if (analyzerLoading || analyzerResult != null) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Analyse-Ergebnis (gemini-3.1-pro-preview)",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                    
                                    if (analyzerLoading) {
                                        LinearProgressIndicator(modifier = Modifier.width(80.dp))
                                    }
                                }

                                // --- Gemini Thinking Process Terminal ---
                                val thinking = analyzerThinking
                                if (thinking.isNotEmpty()) {
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { showThinkingProcess = !showThinkingProcess }
                                                .padding(vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Icon(Icons.Default.Psychology, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                                Text(
                                                    text = "Gedankengang der KI (Deep Thinking)",
                                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                            Icon(
                                                imageVector = if (showThinkingProcess) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }

                                        AnimatedVisibility(visible = showThinkingProcess) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(Color(0xFF1E1E1E))
                                                    .border(1.dp, Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                                    .padding(10.dp)
                                            ) {
                                                Text(
                                                    text = thinking,
                                                    fontFamily = FontFamily.Monospace,
                                                    fontSize = 11.sp,
                                                    color = Color(0xFFA5D6A7),
                                                    modifier = Modifier.testTag("thinking_process_text")
                                                )
                                            }
                                        }
                                    }
                                }

                                // --- Final Medical Report ---
                                val result = analyzerResult
                                if (result != null) {
                                    Text(
                                        text = "Veterinär-Diagnostischer Bericht",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(MaterialTheme.colorScheme.surface)
                                            .padding(12.dp)
                                    ) {
                                        Text(
                                            text = result,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.testTag("analysis_result_text")
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Spacing bottom
                item { Spacer(modifier = Modifier.height(48.dp)) }
            }
        } else {
            // --- History Tab ---
            if (reports.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(36.dp), tint = Color.Gray)
                        Text("Noch keine Berichte im Archiv vorhanden.", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(reports, key = { it.id }) { report ->
                        HistoryReportItem(
                            report = report,
                            onDelete = { viewModel.deleteReport(report) }
                        )
                    }
                    
                    item { Spacer(modifier = Modifier.height(48.dp)) }
                }
            }
        }
    }
}

@Composable
fun HistoryReportItem(
    report: AnalysisReport,
    onDelete: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var showThinking by remember { mutableStateOf(false) }
    
    val dateText = remember(report.timestamp) {
        val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.GERMANY)
        sdf.format(Date(report.timestamp))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("report_item_${report.id}"),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Assignment, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Column {
                        Text(
                            text = report.imageUri ?: "Stallblick Analyse",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = dateText,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = { expanded = !expanded }) {
                        Icon(
                            imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = "Bericht ausklappen"
                        )
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Bericht löschen", tint = Color.Gray.copy(alpha = 0.6f))
                    }
                }
            }

            AnimatedVisibility(visible = expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Divider(color = MaterialTheme.colorScheme.outlineVariant)
                    
                    // Display requested prompt notes
                    Text(
                        text = "Kamerabeschreibung / Notiz:",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = report.prompt,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Thinking Collapse
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showThinking = !showThinking }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Gedankengang der Tierarzt-KI (Deep Thinking):",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Icon(
                                imageVector = if (showThinking) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        AnimatedVisibility(visible = showThinking) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF2E2E2E))
                                    .padding(8.dp)
                            ) {
                                Text(
                                    text = report.thinkingProcess,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    color = Color(0xFFA5D6A7)
                                )
                            }
                        }
                    }

                    // Result report text
                    Text(
                        text = "Berichts-Einschätzung:",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(10.dp)
                    ) {
                        Text(
                            text = report.resultText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

data class DiagnosticCase(
    val name: String,
    val description: String,
    val status: String,
    val details: String
)

fun generateMockCameraImage(cameraName: String, isIrMode: Boolean, isCalving: Boolean, isEstrus: Boolean): String {
    val bitmap = android.graphics.Bitmap.createBitmap(320, 240, android.graphics.Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    val paint = android.graphics.Paint()
    
    // Background color
    paint.color = if (isIrMode) android.graphics.Color.DKGRAY else android.graphics.Color.rgb(40, 50, 42)
    canvas.drawRect(0f, 0f, 320f, 240f, paint)
    
    // Draw scanlines
    paint.color = android.graphics.Color.argb(30, 255, 255, 255)
    for (i in 0..240 step 15) {
        canvas.drawLine(0f, i.toFloat(), 320f, i.toFloat(), paint)
    }
    
    // Draw text overlays
    paint.color = android.graphics.Color.GREEN
    paint.textSize = 14f
    paint.isFakeBoldText = true
    canvas.drawText("LIVE SOURCE: ${cameraName.uppercase(Locale.US)}", 15f, 25f, paint)
    
    val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.GERMANY).format(Date())
    paint.textSize = 10f
    paint.isFakeBoldText = false
    canvas.drawText(dateStr, 15f, 40f, paint)
    
    // Draw minimalist representation of cow body
    paint.color = if (isIrMode) android.graphics.Color.WHITE else android.graphics.Color.rgb(141, 110, 99)
    canvas.drawOval(80f, 100f, 240f, 180f, paint) // Cow body
    canvas.drawCircle(65f, 130f, 25f, paint) // Cow head
    
    // Draw skeletal keypoint dots
    paint.color = android.graphics.Color.YELLOW
    canvas.drawCircle(95f, 120f, 5f, paint) // Spine end
    canvas.drawCircle(225f, 130f, 5f, paint) // Tail base
    
    if (isCalving) {
        // Red raised tail tip
        paint.color = android.graphics.Color.RED
        canvas.drawCircle(245f, 90f, 6f, paint) // Tail tip raised
        paint.strokeWidth = 3f
        canvas.drawLine(225f, 130f, 245f, 90f, paint) // Tail bone
        
        paint.textSize = 11f
        paint.isFakeBoldText = true
        canvas.drawText("YOLO: amniotic_sac DETECTED 0.94", 15f, 215f, paint)
    } else {
        // Normal hanging tail
        paint.color = android.graphics.Color.YELLOW
        canvas.drawCircle(235f, 170f, 5f, paint) // Tail tip hanging
        paint.strokeWidth = 3f
        canvas.drawLine(225f, 130f, 235f, 170f, paint) // Tail bone
    }
    
    if (isEstrus) {
        paint.color = android.graphics.Color.CYAN
        paint.textSize = 11f
        paint.isFakeBoldText = true
        canvas.drawText("YOLO: OVERLAP IoU 0.22", 15f, 215f, paint)
    }
    
    // Draw crosshair or scan overlays
    paint.color = android.graphics.Color.GREEN
    paint.strokeWidth = 1f
    canvas.drawLine(10f, 120f, 30f, 120f, paint)
    canvas.drawLine(20f, 110f, 20f, 130f, paint)
    
    val outputStream = ByteArrayOutputStream()
    bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 70, outputStream)
    val bytes = outputStream.toByteArray()
    return Base64.encodeToString(bytes, Base64.DEFAULT)
}
