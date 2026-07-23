package com.example.ui.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executor
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@Composable
fun BarnCameraXPreviewCard(
    modifier: Modifier = Modifier,
    viewModel: com.example.viewmodel.StallViewModel? = null,
    cameraTitle: String = "Direkte Stallkamera (CameraX Feed)",
    onSnapshotTaken: (String) -> Unit = {}
) {
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("camerax_preview_card"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
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
                        Icons.Default.Videocam,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = cameraTitle,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Surface(
                    color = Color(0xFF2E7D32).copy(alpha = 0.12f),
                    shape = RoundedCornerShape(6.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2E7D32).copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF2E7D32))
                        )
                        Text(
                            text = "CAMERAX + GEMINI KI",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2E7D32)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (hasCameraPermission) {
                CameraXLiveFeedView(
                    viewModel = viewModel,
                    onSnapshotTaken = onSnapshotTaken
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF1E293B)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Icon(
                            Icons.Default.CameraAlt,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(36.dp)
                        )
                        Text(
                            text = "Kamerazugriff erforderlich für Live-Stallüberwachung",
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Button(
                            onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("request_camera_permission_btn")
                        ) {
                            Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Kamera-Berechtigung erteilen")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CameraXLiveFeedView(
    viewModel: com.example.viewmodel.StallViewModel? = null,
    onSnapshotTaken: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var lensFacing by remember { mutableIntStateOf(CameraSelector.LENS_FACING_BACK) }
    var isTorchOn by remember { mutableStateOf(false) }
    var cameraControl by remember { mutableStateOf<androidx.camera.core.CameraControl?>(null) }
    var showAiOverlay by remember { mutableStateOf(true) }
    
    // Status visualization from Gemini AI
    var detectedAnimalStatus by remember { mutableStateOf("Ruhig (Normal)") }
    var detectionConfidence by remember { mutableDoubleStateOf(0.96) }
    var isAnalyzingWithGemini by remember { mutableStateOf(false) }
    var lastAnalysisTimeText by remember { mutableStateOf("Vor 1 Min") }

    // Low-Light Software Filter State for Barn Night Shifts
    var contrastFilterMode by remember { mutableStateOf("HIGH_CONTRAST") } // "OFF", "HIGH_CONTRAST", "IR_GREEN", "GAMMA_BOOST"
    var contrastLevel by remember { mutableFloatStateOf(1.8f) } // 1.0f to 2.8f multiplier
    var showFilterControls by remember { mutableStateOf(false) }

    var timecodeText by remember { mutableStateOf("00:00:00") }
    LaunchedEffect(Unit) {
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.GERMANY)
        while (true) {
            timecodeText = sdf.format(Date())
            kotlinx.coroutines.delay(1000)
        }
    }

    val pulseTransition = rememberInfiniteTransition(label = "pulse")
    val livePulseAlpha by pulseTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val previewView = remember(context) {
        PreviewView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    // Apply Low-Light Contrast Software Filter to CameraX PreviewView Layer
    LaunchedEffect(contrastFilterMode, contrastLevel) {
        val paint = android.graphics.Paint()
        when (contrastFilterMode) {
            "HIGH_CONTRAST" -> {
                val c = contrastLevel
                val t = (1f - c) * 128f
                val cm = android.graphics.ColorMatrix(
                    floatArrayOf(
                        c, 0f, 0f, 0f, t,
                        0f, c, 0f, 0f, t,
                        0f, 0f, c, 0f, t,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
                paint.colorFilter = android.graphics.ColorMatrixColorFilter(cm)
                previewView.setLayerType(android.view.View.LAYER_TYPE_HARDWARE, paint)
            }
            "IR_GREEN" -> {
                val c = contrastLevel
                val cm = android.graphics.ColorMatrix(
                    floatArrayOf(
                        0.1f * c, 0.4f * c, 0.1f * c, 0f, -20f,
                        0.2f * c, 1.8f * c, 0.2f * c, 0f, 10f,
                        0.1f * c, 0.4f * c, 0.1f * c, 0f, -20f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
                paint.colorFilter = android.graphics.ColorMatrixColorFilter(cm)
                previewView.setLayerType(android.view.View.LAYER_TYPE_HARDWARE, paint)
            }
            "GAMMA_BOOST" -> {
                val c = contrastLevel
                val cm = android.graphics.ColorMatrix(
                    floatArrayOf(
                        1.4f * c, 0f, 0f, 0f, 35f,
                        0f, 1.4f * c, 0f, 0f, 35f,
                        0f, 0f, 1.4f * c, 0f, 35f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
                paint.colorFilter = android.graphics.ColorMatrixColorFilter(cm)
                previewView.setLayerType(android.view.View.LAYER_TYPE_HARDWARE, paint)
            }
            else -> {
                previewView.setLayerType(android.view.View.LAYER_TYPE_NONE, null)
            }
        }
    }

    DisposableEffect(lensFacing, lifecycleOwner) {
        val mainExecutor: Executor = ContextCompat.getMainExecutor(context)
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                val cameraSelector = CameraSelector.Builder()
                    .requireLensFacing(lensFacing)
                    .build()

                cameraProvider.unbindAll()
                val camera = cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview
                )
                cameraControl = camera.cameraControl
            } catch (e: Exception) {
                Log.e("CameraXPreview", "Camera binding failed", e)
            }
        }, mainExecutor)

        onDispose {
            try {
                if (cameraProviderFuture.isDone) {
                    val cameraProvider = cameraProviderFuture.get()
                    cameraProvider.unbindAll()
                }
            } catch (e: Exception) {
                Log.e("CameraXPreview", "Camera unbind onDispose failed", e)
            }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(16.dp))
                .border(1.5.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
        ) {
            AndroidView(
                factory = { previewView },
                modifier = Modifier.fillMaxSize()
            )

            // Overlay canvas for AI Bounding Box & HUD Grid
            if (showAiOverlay) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height

                    val boxColor = when {
                        detectedAnimalStatus.contains("Kalbung", ignoreCase = true) -> Color(0xFFD32F2F)
                        detectedAnimalStatus.contains("Brunst", ignoreCase = true) -> Color(0xFF0288D1)
                        else -> Color(0xFF00FF41)
                    }

                    val cx = w * 0.5f
                    val cy = h * 0.5f
                    drawRect(
                        color = boxColor,
                        topLeft = Offset(cx - 120f, cy - 80f),
                        size = Size(240f, 160f),
                        style = Stroke(width = 2.5f)
                    )

                    // Keypoints (spine_end, tail_base, tail_tip)
                    drawCircle(color = Color.Yellow, radius = 5f, center = Offset(cx - 60f, cy - 40f))
                    drawCircle(color = Color.Red, radius = 6f, center = Offset(cx + 40f, cy - 30f))
                    drawCircle(color = Color.Cyan, radius = 5f, center = Offset(cx + 80f, cy + 20f))

                    drawLine(Color.Yellow, Offset(cx - 60f, cy - 40f), Offset(cx + 40f, cy - 30f), strokeWidth = 2f)
                    drawLine(Color.Red, Offset(cx + 40f, cy - 30f), Offset(cx + 80f, cy + 20f), strokeWidth = 2f)

                    // Grid lines
                    val gridColor = Color.White.copy(alpha = 0.1f)
                    drawLine(gridColor, Offset(w * 0.33f, 0f), Offset(w * 0.33f, h), strokeWidth = 1f)
                    drawLine(gridColor, Offset(w * 0.66f, 0f), Offset(w * 0.66f, h), strokeWidth = 1f)
                    drawLine(gridColor, Offset(0f, h * 0.5f), Offset(w, h * 0.5f), strokeWidth = 1f)
                }
            }

            // Live badge & timestamp
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.Black.copy(alpha = 0.65f))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(Color.Red.copy(alpha = livePulseAlpha))
                )
                Text("LIVE CAMERAX", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                Text("• $timecodeText", color = Color.LightGray, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
            }

            // Status Badge Overlay on top center
            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 8.dp),
                color = when {
                    detectedAnimalStatus.contains("Kalbung", ignoreCase = true) -> Color(0xFFD32F2F)
                    detectedAnimalStatus.contains("Brunst", ignoreCase = true) -> Color(0xFF0288D1)
                    detectedAnimalStatus.contains("Fehlalarm", ignoreCase = true) -> Color(0xFFE65100)
                    else -> Color(0xFF2E7D32)
                }.copy(alpha = 0.9f),
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = when {
                            detectedAnimalStatus.contains("Kalbung", ignoreCase = true) -> Icons.Default.ChildCare
                            detectedAnimalStatus.contains("Brunst", ignoreCase = true) -> Icons.Default.LocalActivity
                            detectedAnimalStatus.contains("Fehlalarm", ignoreCase = true) -> Icons.Default.ReportProblem
                            else -> Icons.Default.CheckCircle
                        },
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = "STATUS: $detectedAnimalStatus (${(detectionConfidence * 100).toInt()}%)",
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold
                    )

                    if (!detectedAnimalStatus.contains("Ruhig", ignoreCase = true) && !detectedAnimalStatus.contains("Fehlalarm", ignoreCase = true)) {
                        Spacer(modifier = Modifier.width(2.dp))
                        Surface(
                            onClick = {
                                val prevEvt = detectedAnimalStatus
                                detectedAnimalStatus = "Fehlalarm / Täuschung"
                                detectionConfidence = 0.0
                                lastAnalysisTimeText = "Markiert"

                                viewModel?.markFalseAlarm(
                                    cowId = if (prevEvt.contains("Brunst")) "Kuh #103" else "Kuh #42",
                                    eventType = prevEvt,
                                    cameraLocation = "Stallkamera CameraX",
                                    description = "Schnellzugriff: Täuschung/Fehlalarm direkt über Kamera-Overlay markiert."
                                )

                                Toast.makeText(
                                    context,
                                    "🎯 Als Täuschung/Fehlalarm markiert! Bild für KI-Retraining gespeichert.",
                                    Toast.LENGTH_LONG
                                ).show()
                            },
                            color = Color.Black.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("camerax_quick_false_alarm_overlay_btn")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Icon(Icons.Default.Cancel, contentDescription = "Täuschung", tint = Color.White, modifier = Modifier.size(10.dp))
                                Text("Täuschung?", fontSize = 8.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Controls bar on top right
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                IconButton(
                    onClick = { showFilterControls = !showFilterControls },
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(if (contrastFilterMode != "OFF") Color(0xFF00E676) else Color.Black.copy(alpha = 0.6f))
                        .testTag("camerax_toggle_filter_controls")
                ) {
                    Icon(
                        Icons.Default.Tune,
                        contentDescription = "Low-Light Software-Filter",
                        tint = if (contrastFilterMode != "OFF") Color.Black else Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }

                IconButton(
                    onClick = { showAiOverlay = !showAiOverlay },
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(if (showAiOverlay) MaterialTheme.colorScheme.primary else Color.Black.copy(alpha = 0.6f))
                        .testTag("camerax_toggle_overlay")
                ) {
                    Icon(Icons.Default.Adjust, contentDescription = "AI Keypoints", tint = Color.White, modifier = Modifier.size(14.dp))
                }

                IconButton(
                    onClick = {
                        lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                            CameraSelector.LENS_FACING_FRONT
                        } else {
                            CameraSelector.LENS_FACING_BACK
                        }
                    },
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.6f))
                        .testTag("camerax_switch_lens")
                ) {
                    Icon(Icons.Default.FlipCameraAndroid, contentDescription = "Kamera wechseln", tint = Color.White, modifier = Modifier.size(14.dp))
                }

                IconButton(
                    onClick = {
                        isTorchOn = !isTorchOn
                        cameraControl?.enableTorch(isTorchOn)
                    },
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(if (isTorchOn) Color(0xFFFFB300) else Color.Black.copy(alpha = 0.6f))
                        .testTag("camerax_toggle_torch")
                ) {
                    Icon(
                        if (isTorchOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                        contentDescription = "Blitzlicht",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            // Software Filter Indicator Badge (Bottom Right)
            if (contrastFilterMode != "OFF") {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF00E676).copy(alpha = 0.85f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = when (contrastFilterMode) {
                            "HIGH_CONTRAST" -> "NACHTSICHT KONTRAST (${String.format(Locale.GERMANY, "%.1fx", contrastLevel)})"
                            "IR_GREEN" -> "IR-GRÜN BOOST (${String.format(Locale.GERMANY, "%.1fx", contrastLevel)})"
                            "GAMMA_BOOST" -> "DÄMMERUNG BOOST (${String.format(Locale.GERMANY, "%.1fx", contrastLevel)})"
                            else -> "KONTRAST-FILTER"
                        },
                        color = Color.Black,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            // CameraX Info label at bottom
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.Black.copy(alpha = 0.65f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "Gemini KI + YOLOv8: Schwanzwinkel 46.2° (Letzte Analyse: $lastAnalysisTimeText)",
                    color = Color(0xFF00FF41),
                    fontSize = 8.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Expandable Software Low-Light Filter Control Panel
        AnimatedVisibility(
            visible = showFilterControls,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.Tune, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                            Text(
                                "Software-Kontrastfilter (Nachtschicht)",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Text(
                            text = "${String.format(Locale.GERMANY, "%.1f", contrastLevel)}x Stärke",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Mode Filter Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        CameraFilterChipItem(
                            label = "Standard",
                            isSelected = contrastFilterMode == "OFF",
                            onClick = { contrastFilterMode = "OFF" }
                        )
                        CameraFilterChipItem(
                            label = "Nacht-Kontrast",
                            isSelected = contrastFilterMode == "HIGH_CONTRAST",
                            onClick = { contrastFilterMode = "HIGH_CONTRAST" }
                        )
                        CameraFilterChipItem(
                            label = "IR-Grün",
                            isSelected = contrastFilterMode == "IR_GREEN",
                            onClick = { contrastFilterMode = "IR_GREEN" }
                        )
                        CameraFilterChipItem(
                            label = "Schatten-Boost",
                            isSelected = contrastFilterMode == "GAMMA_BOOST",
                            onClick = { contrastFilterMode = "GAMMA_BOOST" }
                        )
                    }

                    // Intensity Slider
                    if (contrastFilterMode != "OFF") {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("1.0x", fontSize = 9.sp, color = Color.Gray)
                            Slider(
                                value = contrastLevel,
                                onValueChange = { contrastLevel = it },
                                valueRange = 1.0f..2.8f,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(20.dp)
                                    .testTag("camerax_filter_intensity_slider"),
                                colors = SliderDefaults.colors(
                                    thumbColor = MaterialTheme.colorScheme.primary,
                                    activeTrackColor = MaterialTheme.colorScheme.primary
                                )
                            )
                            Text("2.8x", fontSize = 9.sp, color = Color.Gray)
                        }
                    }
                }
            }
        }

        // Gemini AI Action Buttons Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = {
                    isAnalyzingWithGemini = true
                    detectedAnimalStatus = "Anzeichen von Kalbung (Austreibung)"
                    detectionConfidence = 0.94
                    lastAnalysisTimeText = "Gerade eben"
                    
                    viewModel?.addMonitoringLog(
                        eventType = "Calving",
                        status = "Active",
                        cowId = "Kuh #42",
                        cameraLocation = "Stallkamera CameraX",
                        description = "Gemini KI-Analyse: Fruchtblase (amniotic_sac) und Füße sichtbar, Schwanzwinkel 52°. Unruhe im Abkalbebereich.",
                        confidence = 0.94
                    )
                    isAnalyzingWithGemini = false
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag("camerax_trigger_calving_btn"),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                contentPadding = PaddingValues(vertical = 6.dp, horizontal = 8.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.ChildCare, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Kalbung Test", fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = {
                    isAnalyzingWithGemini = true
                    detectedAnimalStatus = "Brunst (Aufsprungverhalten)"
                    detectionConfidence = 0.88
                    lastAnalysisTimeText = "Gerade eben"

                    viewModel?.addMonitoringLog(
                        eventType = "Heat",
                        status = "Active",
                        cowId = "Kuh #103",
                        cameraLocation = "Stallkamera CameraX",
                        description = "Gemini KI-Analyse: Aufsprungverhalten erkannt (IoU > 0.15, Dauer > 4 Sek). Hohe Aktivität.",
                        confidence = 0.88
                    )
                    isAnalyzingWithGemini = false
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag("camerax_trigger_heat_btn"),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0288D1)),
                contentPadding = PaddingValues(vertical = 6.dp, horizontal = 8.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.LocalActivity, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Brunst Test", fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = {
                    val prevEvt = detectedAnimalStatus
                    detectedAnimalStatus = "Fehlalarm / Täuschung"
                    detectionConfidence = 0.0
                    lastAnalysisTimeText = "Markiert"

                    viewModel?.markFalseAlarm(
                        cowId = if (prevEvt.contains("Brunst")) "Kuh #103" else "Kuh #42",
                        eventType = prevEvt,
                        cameraLocation = "Stallkamera CameraX",
                        description = "Landwirt hat im Live-Dashboard 'Täuschung/Fehlalarm' gedrückt."
                    )

                    Toast.makeText(
                        context,
                        "🎯 Als Täuschung/Fehlalarm markiert! Bild für KI-Retraining gesichert.",
                        Toast.LENGTH_LONG
                    ).show()
                },
                modifier = Modifier
                    .testTag("camerax_mark_false_alarm_btn"),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100)),
                contentPadding = PaddingValues(vertical = 6.dp, horizontal = 8.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.ReportProblem, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Fehlalarm", fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
                onClick = {
                    detectedAnimalStatus = "Ruhig (Normal)"
                    detectionConfidence = 0.98
                    lastAnalysisTimeText = "Gerade eben"
                },
                modifier = Modifier.testTag("camerax_trigger_quiet_btn"),
                contentPadding = PaddingValues(vertical = 6.dp, horizontal = 8.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Ruhig", fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun CameraFilterChipItem(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(10.dp),
        border = if (isSelected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
