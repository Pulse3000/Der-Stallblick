package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "analysis_reports")
data class AnalysisReport(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val cowId: String?,
    val imageUri: String?, // Location of the analyzed frame (or mock placeholder tag)
    val prompt: String,
    val thinkingProcess: String, // The thinking/reasoning process from Gemini Pro
    val resultText: String       // The final vet report
) : Serializable
