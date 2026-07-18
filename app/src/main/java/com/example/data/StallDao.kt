package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface StallDao {

    // --- Cows Queries ---
    @Query("SELECT * FROM cows ORDER BY calvingDueDate ASC")
    fun getAllCows(): Flow<List<Cow>>

    @Query("SELECT * FROM cows WHERE id = :id LIMIT 1")
    suspend fun getCowById(id: String): Cow?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCow(cow: Cow)

    @Update
    suspend fun updateCow(cow: Cow)

    @Delete
    suspend fun deleteCow(cow: Cow)

    // --- StallEvents Queries ---
    @Query("SELECT * FROM stall_events ORDER BY timestamp DESC")
    fun getAllEvents(): Flow<List<StallEvent>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: StallEvent)

    @Query("DELETE FROM stall_events WHERE id = :id")
    suspend fun deleteEventById(id: Int)

    @Query("DELETE FROM stall_events")
    suspend fun clearAllEvents()

    // --- AnalysisReports Queries ---
    @Query("SELECT * FROM analysis_reports ORDER BY timestamp DESC")
    fun getAllReports(): Flow<List<AnalysisReport>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReport(report: AnalysisReport)

    @Query("DELETE FROM analysis_reports WHERE id = :id")
    suspend fun deleteReportById(id: Int)
}
