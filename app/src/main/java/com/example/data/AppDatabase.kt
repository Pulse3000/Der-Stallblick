package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [Cow::class, StallEvent::class, AnalysisReport::class, CowMonitoringLog::class, HealthEvent::class],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun stallDao(): StallDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "stallblick_database"
                )
                .fallbackToDestructiveMigration()
                .addCallback(AppDatabaseCallback(scope))
                .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class AppDatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    val dao = database.stallDao()
                    
                    // Seed prefilled cows
                    Cow.getPrefilledCows().forEach { cow ->
                        dao.insertCow(cow)
                    }

                    // Seed prefilled events
                    StallEvent.getPrefilledEvents().forEach { event ->
                        dao.insertEvent(event)
                    }

                    // Seed prefilled monitoring logs
                    CowMonitoringLog.getPrefilledLogs().forEach { log ->
                        dao.insertMonitoringLog(log)
                    }

                    // Seed prefilled health events
                    HealthEvent.getPrefilledHealthEvents().forEach { healthEvent ->
                        dao.insertHealthEvent(healthEvent)
                    }
                }
            }
        }
    }
}
