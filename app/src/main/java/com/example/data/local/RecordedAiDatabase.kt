package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [RecordingEntity::class, DailyReportEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class RecordedAiDatabase : RoomDatabase() {
    abstract fun recordingDao(): RecordingDao
    abstract fun dailyReportDao(): DailyReportDao

    companion object {
        @Volatile
        private var INSTANCE: RecordedAiDatabase? = null

        fun getInstance(context: Context): RecordedAiDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    RecordedAiDatabase::class.java,
                    "recorded_ai.db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
