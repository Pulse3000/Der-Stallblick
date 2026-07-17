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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Cow
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
}

@Composable
fun CowListItem(
    cow: Cow,
    onWatchModeToggle: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
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
                        modifier = Modifier.scale(0.8f).testTag("watch_switch_${cow.id}")
                    )
                }
            }

            // Quick Actions: Edit / Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
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
                    
                    Spacer(modifier = Modifier.width(12.dp))

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
