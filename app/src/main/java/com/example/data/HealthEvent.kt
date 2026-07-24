package com.example.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.io.Serializable

/**
 * Room entity to track timestamped health events for individual animals.
 * Supports status tracking (e.g. calving, oestrus/Brunst, fever, general health) with timestamps.
 */
@Entity(
    tableName = "health_events",
    foreignKeys = [
        ForeignKey(
            entity = Cow::class,
            parentColumns = ["id"],
            childColumns = ["animalId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["animalId"]), Index(value = ["timestamp"])]
)
data class HealthEvent(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val animalId: String,               // e.g. "Kuh #42" - references Cow.id
    val timestamp: Long = System.currentTimeMillis(),
    val eventType: String,              // "Calving" (Kalbung), "Oestrus" (Brunst), "HealthCheck", "Unrest", "Fever", "Lameness"
    val status: String,                 // "calving", "oestrus", "normal", "active", "resolved", "critical"
    val severity: String = "Normal",    // "Low", "Medium", "High", "Critical"
    val description: String = "",       // Detailed observation or AI analysis message
    val recordedBy: String = "KI-Wache" // "KI-Wache", "Landwirt", "Veterinär"
) : Serializable {
    companion object {
        fun getPrefilledHealthEvents(): List<HealthEvent> {
            val now = System.currentTimeMillis()
            return listOf(
                HealthEvent(
                    id = 1,
                    animalId = "Kuh #42",
                    timestamp = now - 1800000, // 30 min ago
                    eventType = "Calving",
                    status = "calving",
                    severity = "Critical",
                    description = "Austreibungsphase: Fruchtblase sichtbar, Schwanzwinkel 52°. Vorbereitung zur Geburt.",
                    recordedBy = "KI-Wache Kamera"
                ),
                HealthEvent(
                    id = 2,
                    animalId = "Kuh #103",
                    timestamp = now - 7200000, // 2 hours ago
                    eventType = "Oestrus",
                    status = "oestrus",
                    severity = "High",
                    description = "Brunstsymptom: Wiederholtes Aufsprungverhalten (IoU > 0.15) über 6 Sekunden.",
                    recordedBy = "KI-Wache Kamera"
                ),
                HealthEvent(
                    id = 3,
                    animalId = "Kuh #18",
                    timestamp = now - 86400000, // 1 day ago
                    eventType = "HealthCheck",
                    status = "normal",
                    severity = "Low",
                    description = "Routinemäßige Gesundheitskontrolle: Wiederkaubewegungen und Bewegungsaktivität im Normalbereich.",
                    recordedBy = "Landwirt"
                ),
                HealthEvent(
                    id = 4,
                    animalId = "Kuh #7",
                    timestamp = now - 172800000, // 2 days ago
                    eventType = "Oestrus",
                    status = "resolved",
                    severity = "Medium",
                    description = "Hauptbrunst abgeschlossen. Aktivitätswerte wieder stabil.",
                    recordedBy = "KI-Wache Kamera"
                )
            )
        }
    }
}
