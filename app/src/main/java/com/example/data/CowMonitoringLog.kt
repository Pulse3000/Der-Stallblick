package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "cow_monitoring_logs")
data class CowMonitoringLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val eventType: String, // "Calving" or "Heat"
    val status: String,    // "Active", "Resolved", "Critical", "Pending", "Normal"
    val cowId: String? = null,
    val cameraLocation: String = "Stallwache",
    val description: String = "",
    val confidence: Double? = null,
    val isAlert: Boolean = true
) : Serializable {
    companion object {
        fun getPrefilledLogs(): List<CowMonitoringLog> {
            val now = System.currentTimeMillis()
            return listOf(
                CowMonitoringLog(
                    id = 1,
                    timestamp = now - 1800000,
                    eventType = "Calving",
                    status = "Critical",
                    cowId = "Kuh #42",
                    cameraLocation = "Stallbox 1",
                    description = "Schwanzwinkel > 45° in 28% der Frames. Fruchtblasen-Austreibung erkannt.",
                    confidence = 0.92,
                    isAlert = true
                ),
                CowMonitoringLog(
                    id = 2,
                    timestamp = now - 3600000,
                    eventType = "Calving",
                    status = "Active",
                    cowId = "Kuh #42",
                    cameraLocation = "Stallbox 1",
                    description = "Kalbeverdacht: Erhöhte Unruhe und Schwanzanhebung registriert.",
                    confidence = 0.85,
                    isAlert = true
                ),
                CowMonitoringLog(
                    id = 3,
                    timestamp = now - 7200000,
                    eventType = "Heat",
                    status = "Active",
                    cowId = "Kuh #103",
                    cameraLocation = "Futterwache",
                    description = "Brunstverdacht: Aufsprungverhalten mit Kuh #88 festgestellt (Dauer 6.4s).",
                    confidence = 0.94,
                    isAlert = true
                ),
                CowMonitoringLog(
                    id = 4,
                    timestamp = now - 14400000,
                    eventType = "Heat",
                    status = "Resolved",
                    cowId = "Kuh #88",
                    cameraLocation = "Futterwache",
                    description = "Erhöhte Laufaktivität und Schnüffeln am Futtertisch.",
                    confidence = 0.81,
                    isAlert = false
                ),
                CowMonitoringLog(
                    id = 5,
                    timestamp = now - 28800000,
                    eventType = "Calving",
                    status = "Resolved",
                    cowId = "Kuh #12",
                    cameraLocation = "Stallbox 2",
                    description = "Erfolgreiche Kalbung ohne Komplikationen abgeschlossen. Kalb wohlauf.",
                    confidence = 0.98,
                    isAlert = false
                )
            )
        }
    }
}
