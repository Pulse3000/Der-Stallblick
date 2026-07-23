package com.example.ui.screens

import android.app.DatePickerDialog
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale as modifierScale
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale as drawScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import com.example.data.Cow
import com.example.data.StallEvent
import com.example.viewmodel.StallViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HerdScreen(
    viewModel: StallViewModel,
    modifier: Modifier = Modifier
) {
    val cows by viewModel.cows.collectAsState()
    val context = LocalContext.current

    var searchQuery by remember { mutableStateOf("") }
    var selectedCowForDetails by remember { mutableStateOf<Cow?>(null) }
    var activeDetailTab by remember { mutableStateOf(0) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var editingCow by remember { mutableStateOf<Cow?>(null) }

    // --- New Cow Form States ---
    var newId by remember { mutableStateOf("") }
    var newName by remember { mutableStateOf("") }
    var newDueDate by remember { mutableStateOf("") }
    var newStatus by remember { mutableStateOf("Normal") }
    var newWatchMode by remember { mutableStateOf(false) }
    var newEarTagId by remember { mutableStateOf("") }
    var newPhysicalDescription by remember { mutableStateOf("") }

    // --- Edit Cow Form States ---
    var editId by remember { mutableStateOf("") }
    var editName by remember { mutableStateOf("") }
    var editDueDate by remember { mutableStateOf("") }
    var editStatus by remember { mutableStateOf("Normal") }
    var editWatchMode by remember { mutableStateOf(false) }
    var editEarTagId by remember { mutableStateOf("") }
    var editPhysicalDescription by remember { mutableStateOf("") }

    val filteredCows = remember(cows, searchQuery) {
        cows.filter {
            it.id.contains(searchQuery, ignoreCase = true) ||
            it.name.contains(searchQuery, ignoreCase = true) ||
            it.earTagId.contains(searchQuery, ignoreCase = true)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    // Reset form and show
                    newId = "Kuh #" + (cows.size + 10).toString()
                    newName = ""
                    newDueDate = SimpleDateFormat("yyyy-MM-dd", Locale.GERMANY).format(Date())
                    newStatus = "Normal"
                    newWatchMode = false
                    newEarTagId = "DE 08 152 " + (10000 + Random().nextInt(90000)).toString()
                    newPhysicalDescription = ""
                    showAddDialog = true
                },
                modifier = Modifier.testTag("add_cow_fab"),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Kuh hinzufügen")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // --- Title ---
            Column {
                Text(
                    text = "Rinder-Herde",
                    style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.ExtraBold)
                )
                Text(
                    text = "Monitore den Abkalbe- und Brunstzyklus deines Herdenbestands.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // --- Echtzeit-Videostream der Stallkameras ---
            HerdScreenLiveCameraSection(viewModel = viewModel)

            // --- Search Field ---
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth().testTag("cow_search_input"),
                placeholder = { Text("Suche nach Kuh-ID oder Name...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            // --- Info Banner on Watch-Modus ---
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFDEE1FF)),
                border = BorderStroke(1.dp, Color(0xFFBEC2FF)),
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFF001158))
                    Text(
                        text = "Wach-Modus: Bei Aktivierung (~14 Tage vor Termin) senkt der Edge-PC die Schwellenwerte (Vorkommen Wehenwinkel >45° halbiert), um Fehlalarme im Alltag zu reduzieren und vor der Geburt höchste Sensitivität zu garantieren.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF001158)
                    )
                }
            }

            // --- Cow List ---
            if (filteredCows.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Keine Kühe im Bestand gefunden.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredCows, key = { it.id }) { cow ->
                        CowListItem(
                            cow = cow,
                            onWatchModeToggle = { active ->
                                viewModel.updateCow(cow.copy(watchMode = active))
                            },
                            onEdit = {
                                editingCow = cow
                                editId = cow.id
                                editName = cow.name
                                editDueDate = cow.calvingDueDate
                                editStatus = cow.status
                                editWatchMode = cow.watchMode
                                editEarTagId = cow.earTagId
                                editPhysicalDescription = cow.physicalDescription
                                showEditDialog = true
                            },
                            onDelete = {
                                viewModel.deleteCow(cow)
                            },
                            onViewDetails = {
                                selectedCowForDetails = cow
                                activeDetailTab = 0
                            }
                        )
                    }
                }
            }
        }
    }

    // --- Add Cow Dialog ---
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Kuh im Bestand registrieren") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    OutlinedTextField(
                        value = newId,
                        onValueChange = { newId = it },
                        label = { Text("Kuh-ID (z.B. Kuh #12)") },
                        modifier = Modifier.fillMaxWidth().testTag("new_cow_id_input"),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("Name (z.B. Berta)") },
                        modifier = Modifier.fillMaxWidth().testTag("new_cow_name_input"),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = newEarTagId,
                        onValueChange = { newEarTagId = it },
                        label = { Text("Ohrmarke (z.B. DE 08 152 42931)") },
                        modifier = Modifier.fillMaxWidth().testTag("new_cow_eartag_input"),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = newPhysicalDescription,
                        onValueChange = { newPhysicalDescription = it },
                        label = { Text("Körperliche Merkmale / Beschreibung") },
                        modifier = Modifier.fillMaxWidth().testTag("new_cow_desc_input"),
                        minLines = 2
                    )

                    // Simple Date Picker Dialog trigger
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val calendar = Calendar.getInstance()
                                DatePickerDialog(
                                    context,
                                    { _, year, month, dayOfMonth ->
                                        val cal = Calendar.getInstance()
                                        cal.set(year, month, dayOfMonth)
                                        newDueDate = SimpleDateFormat("yyyy-MM-dd", Locale.GERMANY).format(cal.time)
                                    },
                                    calendar.get(Calendar.YEAR),
                                    calendar.get(Calendar.MONTH),
                                    calendar.get(Calendar.DAY_OF_MONTH)
                                ).show()
                            }
                            .border(1.dp, Color.Gray, RoundedCornerShape(4.dp))
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Abkalbetermin: $newDueDate",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                    }

                    // Status Dropdown selector simulation
                    Text(
                        text = "Status: $newStatus",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("Normal", "Kalbeverdacht", "Trächtig", "Brunstverdacht").forEach { status ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        if (newStatus == status) MaterialTheme.colorScheme.primaryContainer 
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    .clickable { newStatus = status }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = status,
                                    fontSize = 11.sp,
                                    color = if (newStatus == status) MaterialTheme.colorScheme.onPrimaryContainer 
                                            else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Watch Mode toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Aktivierender Wach-Modus", style = MaterialTheme.typography.bodyMedium)
                            Text("Soll die Kuh sofort in die engere Geburtshilfe-Wache eingereiht werden?", fontSize = 10.sp, color = Color.Gray)
                        }
                        Switch(
                            checked = newWatchMode,
                            onCheckedChange = { newWatchMode = it },
                            modifier = Modifier.testTag("new_cow_watch_switch")
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newId.isNotEmpty()) {
                            viewModel.addCow(
                                Cow(
                                    id = newId,
                                    name = newName.ifEmpty { "Kuh " + newId.substringAfter("#") },
                                    status = newStatus,
                                    calvingDueDate = newDueDate,
                                    watchMode = newWatchMode,
                                    earTagId = newEarTagId,
                                    physicalDescription = newPhysicalDescription
                                )
                            )
                            showAddDialog = false
                        }
                    },
                    modifier = Modifier.testTag("save_cow_btn")
                ) {
                    Text("Hinzufügen")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Abbrechen")
                }
            }
        )
    }

    // --- Edit Cow Dialog ---
    if (showEditDialog && editingCow != null) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Kuh-Profil bearbeiten") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = "Kuh-ID: $editId",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )

                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Name (z.B. Berta)") },
                        modifier = Modifier.fillMaxWidth().testTag("edit_cow_name_input"),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = editEarTagId,
                        onValueChange = { editEarTagId = it },
                        label = { Text("Ohrmarke (z.B. DE 08 152 42931)") },
                        modifier = Modifier.fillMaxWidth().testTag("edit_cow_eartag_input"),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = editPhysicalDescription,
                        onValueChange = { editPhysicalDescription = it },
                        label = { Text("Körperliche Merkmale / Beschreibung") },
                        modifier = Modifier.fillMaxWidth().testTag("edit_cow_desc_input"),
                        minLines = 2
                    )

                    // Simple Date Picker Dialog trigger
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val calendar = Calendar.getInstance()
                                DatePickerDialog(
                                    context,
                                    { _, year, month, dayOfMonth ->
                                        val cal = Calendar.getInstance()
                                        cal.set(year, month, dayOfMonth)
                                        editDueDate = SimpleDateFormat("yyyy-MM-dd", Locale.GERMANY).format(cal.time)
                                    },
                                    calendar.get(Calendar.YEAR),
                                    calendar.get(Calendar.MONTH),
                                    calendar.get(Calendar.DAY_OF_MONTH)
                                ).show()
                            }
                            .border(1.dp, Color.Gray, RoundedCornerShape(4.dp))
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Abkalbetermin: $editDueDate",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                    }

                    // Status Dropdown selector simulation
                    Text(
                        text = "Status: $editStatus",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("Normal", "Kalbeverdacht", "Trächtig", "Austreibung", "Brunstverdacht").forEach { status ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        if (editStatus == status) MaterialTheme.colorScheme.primaryContainer 
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    .clickable { editStatus = status }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = status,
                                    fontSize = 11.sp,
                                    color = if (editStatus == status) MaterialTheme.colorScheme.onPrimaryContainer 
                                            else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Watch Mode toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Aktivierender Wach-Modus", style = MaterialTheme.typography.bodyMedium)
                            Text("Soll die Kuh in der engere Geburtshilfe-Wache geführt werden?", fontSize = 10.sp, color = Color.Gray)
                        }
                        Switch(
                            checked = editWatchMode,
                            onCheckedChange = { editWatchMode = it },
                            modifier = Modifier.testTag("edit_cow_watch_switch")
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cowToUpdate = editingCow
                        if (cowToUpdate != null) {
                            viewModel.updateCow(
                                cowToUpdate.copy(
                                    name = editName.ifEmpty { "Kuh " + editId.substringAfter("#") },
                                    status = editStatus,
                                    calvingDueDate = editDueDate,
                                    watchMode = editWatchMode,
                                    earTagId = editEarTagId,
                                    physicalDescription = editPhysicalDescription
                                )
                            )
                            showEditDialog = false
                        }
                    },
                    modifier = Modifier.testTag("update_cow_btn")
                ) {
                    Text("Speichern")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("Abbrechen")
                }
            }
        )
    }

    // --- Animal Detail & History Logs Dialog ---
    if (selectedCowForDetails != null) {
        val cow = selectedCowForDetails!!
        val allEvents by viewModel.events.collectAsState()
        val cowEvents = remember(allEvents, cow) {
            allEvents.filter { it.kuhId == cow.id }
        }
        
        var showManualLogInput by remember { mutableStateOf(false) }
        var manualLogText by remember { mutableStateOf("") }
        var manualLogType by remember { mutableStateOf("info") }
        
        AlertDialog(
            onDismissRequest = { 
                selectedCowForDetails = null
                showManualLogInput = false
                manualLogText = ""
            },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = cow.id,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold)
                        )
                        Text(
                            text = "Name: ${cow.name}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    val statusColor = when (cow.status) {
                        "Kalbeverdacht" -> Color(0xFFFFA000)
                        "Austreibung" -> Color(0xFFD32F2F)
                        "Brunstverdacht" -> Color(0xFF00796B)
                        "Trächtig" -> Color(0xFF1976D2)
                        else -> Color(0xFF2E7D32)
                    }
                    
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(statusColor.copy(alpha = 0.15f))
                            .border(1.dp, statusColor, RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = cow.status.uppercase(Locale.ROOT),
                            color = statusColor,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 550.dp)
                ) {
                    TabRow(
                        selectedTabIndex = activeDetailTab,
                        modifier = Modifier.fillMaxWidth().testTag("cow_detail_tab_row"),
                        containerColor = Color.Transparent
                    ) {
                        Tab(
                            selected = activeDetailTab == 0,
                            onClick = { activeDetailTab = 0 },
                            text = { Text("Vitalität", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            icon = { Icon(Icons.Default.Favorite, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            modifier = Modifier.testTag("detail_tab_vital")
                        )
                        Tab(
                            selected = activeDetailTab == 1,
                            onClick = { activeDetailTab = 1 },
                            text = { Text("Aktivität", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            icon = { Icon(Icons.Default.Timeline, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            modifier = Modifier.testTag("detail_tab_activity")
                        )
                        Tab(
                            selected = activeDetailTab == 2,
                            onClick = { activeDetailTab = 2 },
                            text = { Text("Protokoll", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            icon = { Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            modifier = Modifier.testTag("detail_tab_protocol")
                        )
                        Tab(
                            selected = activeDetailTab == 3,
                            onClick = { activeDetailTab = 3 },
                            text = { Text("Live-Stream", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            icon = { Icon(Icons.Default.Videocam, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            modifier = Modifier.testTag("detail_tab_livestream")
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        when (activeDetailTab) {
                            0 -> {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF7F8FC)),
                                    border = BorderStroke(1.dp, Color(0xFFE1E2EC)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(
                                            text = "Allgemeiner Gesundheitszustand",
                                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        val desc = when (cow.status) {
                                            "Kalbeverdacht" -> "Frühzeitige Wehensignale detektiert. Die Kuh hebt statistisch häufiger den Schwanz an. Kamera 2 (Stallbox) überwacht die Kuh mit hoher Sensitivität."
                                            "Austreibung" -> "Akute Geburtsphase! Fruchtblase oder Kälberfüße wurden per YOLOv8-Pose erkannt. Sofortige Kontrolle im Stall oder Live-Stream überprüfen!"
                                            "Brunstverdacht" -> "Brunstaktivität detektiert. Die Kuh zeigt deutliches Aufsprungverhalten auf Herdengenossen. Optimales Besamungszeitfenster ist aktiv."
                                            "Trächtig" -> "Kuh ist trächtig. Trächtigkeit verläuft planmäßig. Voraussichtlicher Abkalbetermin ist am ${cow.calvingDueDate}."
                                            else -> "Unauffälliges Verhalten. Alle Vitalparameter und Bewegungsprofile liegen im grünen Bereich."
                                        }
                                        Text(
                                            text = desc,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            lineHeight = 16.sp
                                        )
                                    }
                                }
                                
                                Text(
                                    text = "Physiologische Messdaten",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color.Gray
                                )
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    VitalItem(
                                        modifier = Modifier.weight(1f),
                                        label = "Temperatur",
                                        value = when (cow.status) {
                                            "Kalbeverdacht", "Austreibung" -> "39.1 °C"
                                            else -> "38.6 °C"
                                        },
                                        status = when (cow.status) {
                                            "Kalbeverdacht", "Austreibung" -> "Erhöht"
                                            else -> "Normal"
                                        },
                                        statusColor = when (cow.status) {
                                            "Kalbeverdacht", "Austreibung" -> Color(0xFFD32F2F)
                                            else -> Color(0xFF2E7D32)
                                        }
                                    )
                                    
                                    VitalItem(
                                        modifier = Modifier.weight(1f),
                                        label = "Wiederkauen",
                                        value = when (cow.status) {
                                            "Kalbeverdacht", "Austreibung" -> "310 min"
                                            else -> "540 min"
                                        },
                                        status = when (cow.status) {
                                            "Kalbeverdacht", "Austreibung" -> "Verringert"
                                            else -> "Optimal"
                                        },
                                        statusColor = when (cow.status) {
                                            "Kalbeverdacht", "Austreibung" -> Color(0xFFFFA000)
                                            else -> Color(0xFF2E7D32)
                                        }
                                    )
                                }
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    VitalItem(
                                        modifier = Modifier.weight(1f),
                                        label = "Aktivität",
                                        value = when (cow.status) {
                                            "Brunstverdacht" -> "Sehr Hoch"
                                            "Kalbeverdacht", "Austreibung" -> "Unruhig"
                                            else -> "Normal"
                                        },
                                        status = when (cow.status) {
                                            "Brunstverdacht" -> "Aufsprung"
                                            "Kalbeverdacht", "Austreibung" -> "Wehentypisch"
                                            else -> "Ausgeglichen"
                                        },
                                        statusColor = when (cow.status) {
                                            "Brunstverdacht" -> Color(0xFF00796B)
                                            "Kalbeverdacht", "Austreibung" -> Color(0xFFFFA000)
                                            else -> Color(0xFF2E7D32)
                                        }
                                    )
                                    
                                    VitalItem(
                                        modifier = Modifier.weight(1f),
                                        label = "Futteraufnahme",
                                        value = when (cow.status) {
                                            "Kalbeverdacht", "Austreibung" -> "42 kg"
                                            else -> "65 kg"
                                        },
                                        status = when (cow.status) {
                                            "Kalbeverdacht", "Austreibung" -> "Reduziert"
                                            else -> "Konstant"
                                        },
                                        statusColor = when (cow.status) {
                                            "Kalbeverdacht", "Austreibung" -> Color(0xFFFFA000)
                                            else -> Color(0xFF2E7D32)
                                        }
                                    )
                                }
                            }
                            
                            1 -> {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF7F8FC)),
                                    border = BorderStroke(1.dp, Color(0xFFE1E2EC)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(
                                            text = "Aktuelle KI-Kamera-Auswertung",
                                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("Letzter Schwanzwinkel:", style = MaterialTheme.typography.bodySmall)
                                            Text(
                                                text = "${String.format(Locale.US, "%.1f", cow.lastAngle)}°",
                                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                                color = if (cow.lastAngle > 45f) Color(0xFFD32F2F) else Color.DarkGray
                                            )
                                        }
                                        
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("Schwanzwinkel-Status:", style = MaterialTheme.typography.bodySmall)
                                            Text(
                                                text = if (cow.lastAngle > 45f) "Wehenverdacht (>45°)" else "Normal (<45°)",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (cow.lastAngle > 45f) Color(0xFFD32F2F) else Color(0xFF2E7D32)
                                            )
                                        }
                                        
                                        Divider(color = Color(0xFFE1E2EC))
                                        
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("Letzte detektierte Aktivität:", style = MaterialTheme.typography.bodySmall)
                                            val timeStr = remember(cow.lastActiveTime) {
                                                SimpleDateFormat("HH:mm, dd. MMM", Locale.GERMANY).format(Date(cow.lastActiveTime))
                                            }
                                            Text(
                                                text = timeStr,
                                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                                            )
                                        }
                                    }
                                }
                                
                                Text(
                                    text = "Aktivitätsverteilung (Letzte 24h)",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color.Gray
                                )
                                
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    ActivityBar("Stehen (Standing)", if (cow.status == "Brunstverdacht") 0.65f else 0.40f, MaterialTheme.colorScheme.primary)
                                    ActivityBar("Liegen (Lying)", if (cow.status == "Brunstverdacht") 0.15f else 0.45f, Color(0xFF9E9E9E))
                                    ActivityBar("Fressen (Eating)", if (cow.status == "Brunstverdacht") 0.20f else 0.15f, Color(0xFF00796B))
                                }
                            }
                            
                            2 -> {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Protokolleinträge (${cowEvents.size})",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = Color.Gray
                                    )
                                    
                                    Button(
                                        onClick = { showManualLogInput = !showManualLogInput },
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                        modifier = Modifier.height(28.dp).testTag("add_log_btn")
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(if (showManualLogInput) "Schließen" else "Eintrag", fontSize = 10.sp)
                                    }
                                }
                                
                                AnimatedVisibility(visible = showManualLogInput) {
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(10.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(
                                                text = "Manuellen Protokolleintrag erstellen:",
                                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            
                                            OutlinedTextField(
                                                value = manualLogText,
                                                onValueChange = { manualLogText = it },
                                                placeholder = { Text("z.B. Tierarzt-Kontrolle, Besamung, Klauenpflege...", fontSize = 11.sp) },
                                                modifier = Modifier.fillMaxWidth().testTag("manual_log_text_field"),
                                                textStyle = MaterialTheme.typography.bodySmall,
                                                minLines = 2
                                            )
                                            
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                listOf(
                                                    "info" to "Info",
                                                    "kalbeverdacht" to "Wehen",
                                                    "brunstverdacht" to "Brunst"
                                                ).forEach { (typeKey, label) ->
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(6.dp))
                                                            .background(
                                                                if (manualLogType == typeKey) MaterialTheme.colorScheme.primary 
                                                                else Color.White
                                                            )
                                                            .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(6.dp))
                                                            .clickable { manualLogType = typeKey }
                                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                                    ) {
                                                        Text(
                                                            text = label,
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = if (manualLogType == typeKey) Color.White 
                                                                    else MaterialTheme.colorScheme.primary
                                                        )
                                                    }
                                                }
                                            }
                                            
                                            Button(
                                                onClick = {
                                                    if (manualLogText.trim().isNotEmpty()) {
                                                        viewModel.simulateIncomingEdgeAlarm(
                                                            cowId = cow.id,
                                                            type = manualLogType,
                                                            camera = "stallwache",
                                                            message = "[Manuelle Notiz] $manualLogText",
                                                            confidence = 1.0
                                                        )
                                                        manualLogText = ""
                                                        showManualLogInput = false
                                                    }
                                                },
                                                modifier = Modifier.align(Alignment.End).testTag("save_manual_log_button")
                                            ) {
                                                Text("Hinzufügen", fontSize = 11.sp)
                                            }
                                        }
                                    }
                                }
                                
                                if (cowEvents.isEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(24.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.HourglassEmpty,
                                                contentDescription = null,
                                                tint = Color.LightGray,
                                                modifier = Modifier.size(32.dp)
                                            )
                                            Text(
                                                text = "Keine historischen Logs für diese Kuh.",
                                                fontSize = 11.sp,
                                                color = Color.Gray
                                            )
                                        }
                                    }
                                } else {
                                    cowEvents.sortedByDescending { it.timestamp }.forEach { event ->
                                        LogEventItem(event)
                                    }
                                }
                            }
                            
                            3 -> {
                                SimulatedLiveStreamPlayer(
                                    viewModel = viewModel,
                                    initialStall = if (cow.status == "Brunstverdacht" || cow.id == "Kuh #103") 1 else 2,
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { 
                        selectedCowForDetails = null
                        showManualLogInput = false
                        manualLogText = ""
                    },
                    modifier = Modifier.testTag("close_cow_details_btn")
                ) {
                    Text("Schließen")
                }
            }
        )
    }
}

@Composable
fun CowListItem(
    cow: Cow,
    onWatchModeToggle: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onViewDetails: () -> Unit
) {
    var confirmDelete by remember { mutableStateOf(false) }

    val statusColor = when (cow.status) {
        "Kalbeverdacht" -> Color(0xFFFFA000)
        "Austreibung" -> Color(0xFFD32F2F)
        "Brunstverdacht" -> Color(0xFF00796B)
        "Trächtig" -> Color(0xFF1976D2)
        else -> Color(0xFF2E7D32)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onViewDetails() }
            .testTag("cow_item_${cow.id}"),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFC5C6D0)),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Pets, contentDescription = null, modifier = Modifier.size(20.dp), tint = statusColor)
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = cow.id,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            if (cow.earTagId.isNotEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0xFFF1F3F4))
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = cow.earTagId,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF5F6368)
                                    )
                                }
                            }
                        }
                        Text(
                            text = "Name: ${cow.name}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Status tag
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(statusColor.copy(alpha = 0.15f))
                        .border(1.dp, statusColor, RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = cow.status.uppercase(Locale.ROOT),
                        color = statusColor,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }

            if (cow.physicalDescription.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFFBFBFB))
                        .padding(8.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "Körperliche Merkmale / Beschreibung:",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray
                        )
                        Text(
                            text = cow.physicalDescription,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF44474E)
                        )
                    }
                }
            }

            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Calving Info
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Termin: ${cow.calvingDueDate}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Watch Mode Switch
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Wach-Modus",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (cow.watchMode) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (cow.watchMode) FontWeight.Bold else FontWeight.Normal
                    )
                    Switch(
                        checked = cow.watchMode,
                        onCheckedChange = onWatchModeToggle,
                        modifier = Modifier.modifierScale(0.8f).testTag("watch_switch_${cow.id}")
                    )
                }
            }

            // Quick Actions: Akte ansehen & Edit / Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onViewDetails,
                    modifier = Modifier.testTag("view_details_cow_${cow.id}"),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Assessment,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Akte & Protokoll", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                if (confirmDelete) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Löschen?", fontSize = 11.sp, color = Color.Red, fontWeight = FontWeight.Bold)
                        TextButton(onClick = onDelete) { Text("Ja", color = Color.Red) }
                        TextButton(onClick = { confirmDelete = false }) { Text("Nein") }
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onEdit,
                            modifier = Modifier.size(32.dp).testTag("edit_cow_btn_${cow.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Kuh bearbeiten",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(8.dp))

                        IconButton(
                            onClick = { confirmDelete = true },
                            modifier = Modifier.size(32.dp).testTag("delete_cow_btn_${cow.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Kuh löschen",
                                tint = Color.Gray.copy(alpha = 0.5f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// --- Animal Detail Sub-Composables ---

@Composable
fun VitalItem(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    status: String,
    statusColor: Color
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE1E2EC)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(label, fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.SemiBold)
            Text(value, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(statusColor.copy(alpha = 0.12f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(status, fontSize = 8.sp, color = statusColor, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun ActivityBar(
    label: String,
    percentage: Float,
    color: Color
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = Color.DarkGray)
            Text("${(percentage * 100).toInt()}%", fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Color(0xFFE1E2EC))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(percentage)
                    .clip(RoundedCornerShape(3.dp))
                    .background(color)
            )
        }
    }
}

@Composable
fun LogEventItem(event: StallEvent) {
    val eventColor = when (event.typ) {
        "kalbeverdacht" -> Color(0xFFFFA000)
        "austreibung" -> Color(0xFFD32F2F)
        "eskalation" -> Color(0xFFD32F2F)
        "brunstverdacht" -> Color(0xFF00796B)
        else -> Color(0xFF2E7D32)
    }
    
    val timeStr = remember(event.timestamp) {
        SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.GERMANY).format(Date(event.timestamp))
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE1E2EC)),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = when (event.typ) {
                    "kalbeverdacht", "eskalation", "austreibung" -> Icons.Default.Warning
                    "brunstverdacht" -> Icons.Default.FlashOn
                    else -> Icons.Default.Info
                },
                contentDescription = null,
                tint = eventColor,
                modifier = Modifier.size(18.dp)
            )
            
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = event.typ.uppercase(Locale.ROOT),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = eventColor
                    )
                    Text(
                        text = timeStr,
                        fontSize = 9.sp,
                        color = Color.Gray
                    )
                }
                Text(
                    text = event.nachricht,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.DarkGray
                )
            }
        }
    }
}

@Composable
fun HerdScreenLiveCameraSection(viewModel: StallViewModel) {
    var isExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth().testTag("herd_camera_expandable_card"),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFC5C6D0)),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Videocam,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Echtzeit-Videostreams der Stallkameras",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            val infiniteTransition = rememberInfiniteTransition(label = "badge")
                            val badgeAlpha by infiniteTransition.animateFloat(
                                initialValue = 0.3f,
                                targetValue = 1.0f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(800, easing = LinearEasing),
                                    repeatMode = RepeatMode.Reverse
                                ),
                                label = "badgePulse"
                            )
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF2E7D32).copy(alpha = badgeAlpha))
                            )
                        }
                        Text(
                            text = "Visuelle Echtzeitüberwachung von Futterwache & Abkalbe-Stallbox.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "Schließen" else "Öffnen",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = 14.dp)) {
                    SimulatedLiveStreamPlayer(
                        viewModel = viewModel,
                        initialStall = 1
                    )
                }
            }
        }
    }
}

@Composable
fun SimulatedLiveStreamPlayer(
    viewModel: StallViewModel,
    initialStall: Int,
    modifier: Modifier = Modifier
) {
    val cows by viewModel.cows.collectAsState()
    val bertaCow = remember(cows) { cows.firstOrNull { it.id == "Kuh #42" } }
    val zeldaCow = remember(cows) { cows.firstOrNull { it.id == "Kuh #103" } }

    val scope = rememberCoroutineScope()
    var selectedStall by remember { mutableStateOf(initialStall) }
    var isTransitioning by remember { mutableStateOf(false) }
    var isReloading by remember { mutableStateOf(false) }
    var isIrMode by remember { mutableStateOf(false) }
    var zoomScale by remember { mutableStateOf(1.0f) }
    var brightnessLevel by remember { mutableStateOf(0f) }
    var contrastBoost by remember { mutableStateOf(true) }
    var showSnapshotSuccess by remember { mutableStateOf(false) }

    LaunchedEffect(selectedStall) {
        isTransitioning = true
        delay(1000)
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

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { selectedStall = 1 },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedStall == 1) MaterialTheme.colorScheme.primary else Color(0xFFE2E8F0),
                    contentColor = if (selectedStall == 1) Color.White else Color(0xFF475569)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f).height(36.dp).testTag("herd_stream_tab_1"),
                contentPadding = PaddingValues(0.dp)
            ) {
                Icon(Icons.Default.Videocam, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Kamera 1: Futterwache", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = { selectedStall = 2 },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedStall == 2) MaterialTheme.colorScheme.primary else Color(0xFFE2E8F0),
                    contentColor = if (selectedStall == 2) Color.White else Color(0xFF475569)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f).height(36.dp).testTag("herd_stream_tab_2"),
                contentPadding = PaddingValues(0.dp)
            ) {
                Icon(Icons.Default.Videocam, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Kamera 2: Stallbox", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(16.dp))
                .border(1.5.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .background(if (isIrMode) Color(0xFF1C1C1C) else Color(0xFF101411))
            ) {
                val w = size.width
                val h = size.height

                drawScale(scale = zoomScale, pivot = Offset(w / 2f, h / 2f)) {
                    when (selectedStall) {
                        1 -> {
                            drawMountingBehaviorPose(
                                width = w,
                                height = h,
                                isMounting = zeldaCow?.status == "Brunstverdacht",
                                isIrMode = isIrMode
                            )

                            if (contrastBoost) {
                                val boxColor = if (isIrMode) Color.White else Color(0xFF006495)
                                val cx = w * 0.45f
                                val cy = h * 0.52f
                                drawRect(
                                    color = boxColor,
                                    topLeft = Offset(cx - 100f, cy - 60f),
                                    size = Size(200f, 120f),
                                    style = Stroke(width = 2f)
                                )
                            }
                        }
                        2 -> {
                            drawCowSkeletalPose(
                                width = w,
                                height = h,
                                isTailRaised = (bertaCow?.lastAngle ?: 15f) > 40f,
                                status = bertaCow?.status ?: "Normal",
                                isIrMode = isIrMode
                            )

                            if (contrastBoost) {
                                val boxColor = if (isIrMode) Color.White else Color(0xFFBA1A1A)
                                val cx = w * 0.45f
                                val cy = h * 0.52f
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

                val gridColor = Color.White.copy(alpha = 0.08f)
                drawLine(gridColor, Offset(w * 0.25f, 0f), Offset(w * 0.25f, h), strokeWidth = 1f)
                drawLine(gridColor, Offset(w * 0.5f, 0f), Offset(w * 0.5f, h), strokeWidth = 1f)
                drawLine(gridColor, Offset(w * 0.75f, 0f), Offset(w * 0.75f, h), strokeWidth = 1f)
                drawLine(gridColor, Offset(0f, h * 0.25f), Offset(w, h * 0.25f), strokeWidth = 1f)
                drawLine(gridColor, Offset(0f, h * 0.5f), Offset(w, h * 0.5f), strokeWidth = 1f)
                drawLine(gridColor, Offset(0f, h * 0.75f), Offset(w, h * 0.75f), strokeWidth = 1f)

                val bracketColor = Color.White.copy(alpha = 0.2f)
                val bSize = 15f
                val bThick = 2f
                drawLine(bracketColor, Offset(15f, 15f), Offset(15f + bSize, 15f), strokeWidth = bThick)
                drawLine(bracketColor, Offset(15f, 15f), Offset(15f, 15f + bSize), strokeWidth = bThick)
                drawLine(bracketColor, Offset(w - 15f, 15f), Offset(w - 15f - bSize, 15f), strokeWidth = bThick)
                drawLine(bracketColor, Offset(w - 15f, 15f), Offset(w - 15f, 15f + bSize), strokeWidth = bThick)
                drawLine(bracketColor, Offset(15f, h - 15f), Offset(15f + bSize, h - 15f), strokeWidth = bThick)
                drawLine(bracketColor, Offset(15f, h - 15f), Offset(15f, h - 15f - bSize), strokeWidth = bThick)
                drawLine(bracketColor, Offset(w - 15f, h - 15f), Offset(w - 15f - bSize, h - 15f), strokeWidth = bThick)
                drawLine(bracketColor, Offset(w - 15f, h - 15f), Offset(w - 15f, h - 15f - bSize), strokeWidth = bThick)

                drawCircle(
                    color = Color.Red.copy(alpha = livePulseAlpha),
                    radius = 6f,
                    center = Offset(30f, 30f)
                )
            }

            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(timecodeText, color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .background(Color.Black.copy(alpha = 0.6f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = if (selectedStall == 1) "Kamera 1: Futterwache" else "Kamera 2: Stallbox",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp
                )
                Text(
                    text = if (selectedStall == 1) "Route: /api/futterwache/stream" else "Route: /api/stallbox/stream",
                    color = Color(0xFF64B5F6),
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 8.sp
                )
                Text(
                    text = if (selectedStall == 1) {
                        zeldaCow?.let { "Kuh #103 (Zelda) • Aktivität hoch" } ?: "Normal"
                    } else {
                        bertaCow?.let { "Kuh #42 (Berta) • Schwanzwinkel: ${String.format(Locale.US, "%.1f", it.lastAngle)}°" } ?: "Normal"
                    },
                    color = Color.LightGray,
                    fontSize = 8.sp
                )
            }

            Row(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        scope.launch {
                            showSnapshotSuccess = true
                            delay(2000)
                            showSnapshotSuccess = false
                        }
                    },
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.6f))
                        .testTag("stream_snapshot_btn")
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = "Foto-Beweis", tint = Color.White, modifier = Modifier.size(12.dp))
                }

                IconButton(
                    onClick = {
                        scope.launch {
                            isReloading = true
                            delay(800)
                            isReloading = false
                        }
                    },
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.6f))
                        .testTag("stream_reload_btn")
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Reload stream", tint = Color.White, modifier = Modifier.size(12.dp))
                }

                IconButton(
                    onClick = { isIrMode = !isIrMode },
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(if (isIrMode) MaterialTheme.colorScheme.primary else Color.Black.copy(alpha = 0.6f))
                        .testTag("stream_ir_btn")
                ) {
                    Icon(
                        imageVector = if (isIrMode) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = "IR Mode",
                        tint = Color.White,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }

            if (isTransitioning) {
                Box(
                    modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.9f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Text("RTSP-Verbindung wird aufgebaut...", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (isReloading) {
                Box(
                    modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.8f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Text("Verbindung neu laden...", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (showSnapshotSuccess) {
                Box(
                    modifier = Modifier.align(Alignment.TopCenter).padding(top = 10.dp)
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF2E7D32)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                            Text("Live-Snapshot gespeichert in ./aufnahmen/", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F5F9)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.Settings, contentDescription = null, tint = Color(0xFF475569), modifier = Modifier.size(14.dp))
                        Text("Detaileinstellungen & Filter", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF334155))
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("KI-Skelett Overlay", fontSize = 10.sp, color = Color(0xFF334155))
                        Switch(
                            checked = contrastBoost,
                            onCheckedChange = { contrastBoost = it },
                            modifier = Modifier.modifierScale(0.6f).height(20.dp).testTag("herd_stream_ki_overlay_switch")
                        )
                    }
                }

                Divider(color = Color(0xFFCBD5E1))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Digital-Zoom (x${String.format(Locale.US, "%.1f", zoomScale)})", fontSize = 9.sp, color = Color(0xFF64748B))
                            if (zoomScale > 1f) {
                                TextButton(
                                    onClick = { zoomScale = 1.0f },
                                    contentPadding = PaddingValues(0.dp),
                                    modifier = Modifier.height(16.dp)
                                ) {
                                    Text("Reset", fontSize = 8.sp)
                                }
                            }
                        }
                        Slider(
                            value = zoomScale,
                            onValueChange = { zoomScale = it },
                            valueRange = 1.0f..2.5f,
                            modifier = Modifier.height(18.dp).testTag("herd_stream_zoom_slider")
                        )
                    }

                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(
                                text = "Belichtung (" + (if (brightnessLevel >= 0) "+" else "") + String.format(Locale.US, "%.1f", brightnessLevel) + ")",
                                fontSize = 9.sp,
                                color = Color(0xFF64748B)
                            )
                            if (brightnessLevel != 0f) {
                                TextButton(
                                    onClick = { brightnessLevel = 0f },
                                    contentPadding = PaddingValues(0.dp),
                                    modifier = Modifier.height(16.dp)
                                ) {
                                    Text("Reset", fontSize = 8.sp)
                                }
                            }
                        }
                        Slider(
                            value = brightnessLevel,
                            onValueChange = { brightnessLevel = it },
                            valueRange = -3.0f..3.0f,
                            modifier = Modifier.height(18.dp).testTag("herd_stream_brightness_slider")
                        )
                    }
                }
            }
        }
    }
}
