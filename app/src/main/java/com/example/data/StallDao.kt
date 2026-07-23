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

    // --- Cow Monitoring Logs Queries ---
    @Query("SELECT * FROM cow_monitoring_logs ORDER BY timestamp DESC")
    fun getAllMonitoringLogs(): Flow<List<CowMonitoringLog>>

    @Query("SELECT * FROM cow_monitoring_logs WHERE eventType = :eventType ORDER BY timestamp DESC")
    fun getLogsByEventType(eventType: String): Flow<List<CowMonitoringLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMonitoringLog(log: CowMonitoringLog)

    @Query("UPDATE cow_monitoring_logs SET status = :status WHERE id = :id")
    suspend fun updateMonitoringLogStatus(id: Long, status: String)

    @Query("DELETE FROM cow_monitoring_logs WHERE id = :id")
    suspend fun deleteMonitoringLogById(id: Long)

    @Query("DELETE FROM cow_monitoring_logs")
    suspend fun clearAllMonitoringLogs()
}
