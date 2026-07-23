package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.example.data.CowMonitoringLog
import com.example.viewmodel.StallViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun RoomMonitoringLogsDashboardCard(
    viewModel: StallViewModel,
    modifier: Modifier = Modifier
) {
    val monitoringLogs by viewModel.monitoringLogs.collectAsState()
    var selectedFilter by remember { mutableStateOf("ALL") } // "ALL", "Calving", "Heat", "Active"

    // Quick status indicator counts
    val activeCalvingCount = remember(monitoringLogs) {
        monitoringLogs.count { it.eventType.equals("Calving", ignoreCase = true) && !it.status.equals("Resolved", ignoreCase = true) }
    }
    val activeHeatCount = remember(monitoringLogs) {
        monitoringLogs.count { it.eventType.equals("Heat", ignoreCase = true) && !it.status.equals("Resolved", ignoreCase = true) }
    }
    val totalActiveAlerts = remember(monitoringLogs) {
        monitoringLogs.count { !it.status.equals("Resolved", ignoreCase = true) }
    }

    val filteredLogs = remember(monitoringLogs, selectedFilter) {
        when (selectedFilter) {
            "Calving" -> monitoringLogs.filter { it.eventType.equals("Calving", ignoreCase = true) }
            "Heat" -> monitoringLogs.filter { it.eventType.equals("Heat", ignoreCase = true) }
            "Active" -> monitoringLogs.filter { !it.status.equals("Resolved", ignoreCase = true) }
            else -> monitoringLogs
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("room_monitoring_logs_card"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
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
                    Icon(
                        Icons.Default.Storage,
                        contentDescription = "Room Logs",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Column {
                        Text(
                            text = "Überwachungsprotokolle (Room DB)",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Gerspeicherte Ereignisse & Status-Historie",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "${monitoringLogs.size} Protokolle",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Quick Status Indicators Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickStatusTile(
                    title = "Kalbung Alarme",
                    value = "$activeCalvingCount Aktiv",
                    icon = Icons.Default.ChildCare,
                    color = Color(0xFFD32F2F),
                    modifier = Modifier.weight(1f)
                )

                QuickStatusTile(
                    title = "Brunst Alarme",
                    value = "$activeHeatCount Aktiv",
                    icon = Icons.Default.LocalActivity,
                    color = Color(0xFF0288D1),
                    modifier = Modifier.weight(1f)
                )

                QuickStatusTile(
                    title = "Gesamt Aktiv",
                    value = "$totalActiveAlerts Ereignisse",
                    icon = Icons.Default.NotificationsActive,
                    color = Color(0xFF388E3C),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Filter Chips Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Letchte Alarme (Kalbung & Brunst)",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    FilterChipItem(
                        label = "Alle",
                        isSelected = selectedFilter == "ALL",
                        onClick = { selectedFilter = "ALL" }
                    )
                    FilterChipItem(
                        label = "Kalbung",
                        isSelected = selectedFilter == "Calving",
                        onClick = { selectedFilter = "Calving" }
                    )
                    FilterChipItem(
                        label = "Brunst",
                        isSelected = selectedFilter == "Heat",
                        onClick = { selectedFilter = "Heat" }
                    )
                    FilterChipItem(
                        label = "Aktiv",
                        isSelected = selectedFilter == "Active",
                        onClick = { selectedFilter = "Active" }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Log Items List
            if (filteredLogs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Keine passenden Protokolle vorhanden.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    filteredLogs.take(5).forEach { log ->
                        MonitoringLogListItem(
                            log = log,
                            onStatusChange = { newStatus ->
                                viewModel.updateMonitoringLogStatus(log.id, newStatus)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun QuickStatusTile(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = color.copy(alpha = 0.08f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
                Text(title, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = color, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text(value, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
fun FilterChipItem(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.padding(2.dp)
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun MonitoringLogListItem(
    log: CowMonitoringLog,
    onStatusChange: (String) -> Unit
) {
    val isCalving = log.eventType.equals("Calving", ignoreCase = true)
    val isResolved = log.status.equals("Resolved", ignoreCase = true)
    val isCritical = log.status.equals("Critical", ignoreCase = true)

    val typeColor = if (isCalving) Color(0xFFD32F2F) else Color(0xFF0288D1)
    val typeBg = typeColor.copy(alpha = 0.1f)

    val dateFormat = remember { SimpleDateFormat("HH:mm - dd.MM", Locale.GERMANY) }
    val formattedTime = remember(log.timestamp) { dateFormat.format(Date(log.timestamp)) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("monitoring_log_item_${log.id}"),
        colors = CardDefaults.cardColors(
            containerColor = if (isCritical) Color(0xFFFFDAD6).copy(alpha = 0.4f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        border = BorderStroke(
            1.dp,
            if (isCritical) Color(0xFFFFB4AB) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Type badge
            Surface(
                color = typeBg,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, typeColor.copy(alpha = 0.4f))
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = if (isCalving) Icons.Default.ChildCare else Icons.Default.LocalActivity,
                        contentDescription = null,
                        tint = typeColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (isCalving) "KALBUNG" else "BRUNST",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = typeColor
                    )
                }
            }

            // Description & details
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (log.cowId != null) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = log.cowId,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }

                    Text(
                        text = log.cameraLocation,
                        fontSize = 9.sp,
                        color = Color.Gray
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    Text(
                        text = formattedTime,
                        fontSize = 9.sp,
                        color = Color.Gray,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Text(
                    text = log.description,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                if (log.confidence != null) {
                    Text(
                        text = "Konfidenz: ${(log.confidence * 100).toInt()}%",
                        fontSize = 8.sp,
                        color = typeColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Status Badge / Action Button
            IconButton(
                onClick = {
                    val nextStatus = if (isResolved) "Active" else "Resolved"
                    onStatusChange(nextStatus)
                },
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(if (isResolved) Color(0xFF2E7D32).copy(alpha = 0.15f) else Color(0xFFFF9800).copy(alpha = 0.15f))
            ) {
                Icon(
                    imageVector = if (isResolved) Icons.Outlined.CheckCircle else Icons.Default.Pending,
                    contentDescription = if (isResolved) "Erledigt" else "Als Erledigt markieren",
                    tint = if (isResolved) Color(0xFF2E7D32) else Color(0xFFFF9800),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
