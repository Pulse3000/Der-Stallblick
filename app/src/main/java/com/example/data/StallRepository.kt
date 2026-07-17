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
}
