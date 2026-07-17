package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "stall_events")
data class StallEvent(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val typ: String, // "kalbeverdacht", "austreibung", "eskalation", "brunstverdacht", "info"
    val kuhId: String?, // e.g. "Kuh #42" or null for system
    val kamera: String, // "stallwache", "futterwache"
    val nachricht: String,
    val timestamp: Long = System.currentTimeMillis(),
    val konfidenz: Double? = null,
    val resolved: Boolean = false
) : Serializable {
    companion object {
        fun getPrefilledEvents(): List<StallEvent> {
            val now = System.currentTimeMillis()
            return listOf(
                StallEvent(
                    id = 1,
                    typ = "kalbeverdacht",
                    kuhId = "Kuh #42",
                    kamera = "stallwache",
                    nachricht = "Kuh #42: Schwanzwinkel > 45° (aktuell 49.5°) in 26 % der Frames der letzten 30 Minuten.",
                    timestamp = now - 3600000, // 1 hour ago
                    konfidenz = 0.88
                ),
                StallEvent(
                    id = 2,
                    typ = "brunstverdacht",
                    kuhId = "Kuh #103",
                    kamera = "futterwache",
                    nachricht = "Aufsprungverhalten erkannt: Zwei Kühe überlagern sich (IoU 0.18), anhaltend für 6,2 Sekunden.",
                    timestamp = now - 7200000, // 2 hours ago
                    konfidenz = 0.94
                ),
                StallEvent(
                    id = 3,
                    typ = "info",
                    kuhId = null,
                    kamera = "stallwache",
                    nachricht = "RTSP-Stream reconnected (Versuch 1). Automatischer Switch auf Snapshot-Polling nicht erforderlich.",
                    timestamp = now - 18000000, // 5 hours ago
                    konfidenz = 1.0
                ),
                StallEvent(
                    id = 4,
                    typ = "info",
                    kuhId = null,
                    kamera = "futterwache",
                    nachricht = "KI-Wache-Agent gestartet. Modus: Aktiv. Modell: YOLOv8-Pose (best.pt).",
                    timestamp = now - 86400000 // 1 day ago
                )
            )
        }
    }
}
