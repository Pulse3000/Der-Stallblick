package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "cows")
data class Cow(
    @PrimaryKey val id: String, // e.g. "Kuh #42"
    val name: String,
    val status: String, // "Normal", "Kalbeverdacht", "Austreibung", "Brunstverdacht", "Trächtig"
    val calvingDueDate: String, // e.g. "2026-07-21"
    val watchMode: Boolean = false,
    val lastAngle: Float = 12.0f,
    val lastActiveTime: Long = System.currentTimeMillis(),
    val earTagId: String = "",
    val physicalDescription: String = ""
) : Serializable {
    companion object {
        fun getPrefilledCows(): List<Cow> {
            return listOf(
                Cow("Kuh #42", "Berta", "Kalbeverdacht", "2026-07-21", watchMode = true, lastAngle = 49.5f, earTagId = "DE 08 152 42931", physicalDescription = "Schwarzbunte Holsteinkuh, markante weiße Blesse auf der Stirn, ruhiges Gemüt."),
                Cow("Kuh #18", "Alma", "Normal", "2026-08-05", watchMode = false, lastAngle = 14.2f, earTagId = "DE 08 152 18452", physicalDescription = "Braunes Fleckvieh, weiße Beine, sehr groß gewachsen."),
                Cow("Kuh #103", "Zelda", "Brunstverdacht", "2026-09-12", watchMode = false, lastAngle = 8.5f, earTagId = "DE 08 152 03928", physicalDescription = "Vollkommen rote Kuh, etwas scheu, reagiert empfindlich."),
                Cow("Kuh #7", "Lotte", "Normal", "2026-07-30", watchMode = true, lastAngle = 18.1f, earTagId = "DE 08 152 07312", physicalDescription = "Geflecktes Hinterwälder-Rind, kompakte Statur, sehr agil."),
                Cow("Kuh #55", "Gundi", "Normal", "2026-10-01", watchMode = false, lastAngle = 11.0f, earTagId = "DE 08 152 55019", physicalDescription = "Graubraunes Tiroler Grauvieh, dunkle Augenringe, neugierig.")
            )
        }
    }
}
