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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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

    // --- New Cow Form States ---
    var newId by remember { mutableStateOf("") }
    var newName by remember { mutableStateOf("") }
    var newDueDate by remember { mutableStateOf("") }
    var newStatus by remember { mutableStateOf("Normal") }
    var newWatchMode by remember { mutableStateOf(false) }

    val filteredCows = remember(cows, searchQuery) {
        cows.filter {
            it.id.contains(searchQuery, ignoreCase = true) ||
            it.name.contains(searchQuery, ignoreCase = true)
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
                    modifier = Modifier.fillMaxWidth()
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
                        listOf("Normal", "Kalbeverdacht", "Trächtig").forEach { status ->
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
                                    watchMode = newWatchMode
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
}

@Composable
fun CowListItem(
    cow: Cow,
    onWatchModeToggle: (Boolean) -> Unit,
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
                        Text(
                            text = cow.id,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
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

            // Quick Delete option
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
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
                        onClick = { confirmDelete = true },
                        modifier = Modifier.size(24.dp).testTag("delete_cow_btn_${cow.id}")
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
