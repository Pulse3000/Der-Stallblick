package com.example.data

import kotlinx.coroutines.flow.Flow

class StallRepository(private val stallDao: StallDao) {

    // --- Cows ---
    val allCows: Flow<List<Cow>> = stallDao.getAllCows()

    suspend fun getCowById(id: String): Cow? = stallDao.getCowById(id)

    suspend fun insertCow(cow: Cow) = stallDao.insertCow(cow)

    suspend fun updateCow(cow: Cow) = stallDao.updateCow(cow)

    suspend fun deleteCow(cow: Cow) = stallDao.deleteCow(cow)

    // --- Events ---
    val allEvents: Flow<List<StallEvent>> = stallDao.getAllEvents()

    suspend fun insertEvent(event: StallEvent) = stallDao.insertEvent(event)

    suspend fun deleteEventById(id: Int) = stallDao.deleteEventById(id)

    suspend fun clearAllEvents() = stallDao.clearAllEvents()

    // --- Reports ---
    val allReports: Flow<List<AnalysisReport>> = stallDao.getAllReports()

    suspend fun insertReport(report: AnalysisReport) = stallDao.insertReport(report)

    suspend fun deleteReportById(id: Int) = stallDao.deleteReportById(id)

    // --- Monitoring Logs ---
    val allMonitoringLogs: Flow<List<CowMonitoringLog>> = stallDao.getAllMonitoringLogs()

    fun getMonitoringLogsByType(eventType: String): Flow<List<CowMonitoringLog>> = stallDao.getLogsByEventType(eventType)

    suspend fun insertMonitoringLog(log: CowMonitoringLog) = stallDao.insertMonitoringLog(log)

    suspend fun updateMonitoringLogStatus(id: Long, status: String) = stallDao.updateMonitoringLogStatus(id, status)

    suspend fun deleteMonitoringLogById(id: Long) = stallDao.deleteMonitoringLogById(id)

    suspend fun clearAllMonitoringLogs() = stallDao.clearAllMonitoringLogs()

    // --- Health Events ---
    val allHealthEvents: Flow<List<HealthEvent>> = stallDao.getAllHealthEvents()

    fun getHealthEventsByAnimalId(animalId: String): Flow<List<HealthEvent>> = stallDao.getHealthEventsByAnimalId(animalId)

    fun getHealthEventsByStatus(status: String): Flow<List<HealthEvent>> = stallDao.getHealthEventsByStatus(status)

    suspend fun insertHealthEvent(healthEvent: HealthEvent) = stallDao.insertHealthEvent(healthEvent)

    suspend fun updateHealthEventStatus(id: Long, status: String) = stallDao.updateHealthEventStatus(id, status)

    suspend fun deleteHealthEventById(id: Long) = stallDao.deleteHealthEventById(id)

    suspend fun clearAllHealthEvents() = stallDao.clearAllHealthEvents()
}
