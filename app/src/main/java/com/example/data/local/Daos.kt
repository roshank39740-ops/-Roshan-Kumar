package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface RecordingDao {
    @Query("SELECT * FROM recordings ORDER BY createdAt DESC")
    fun getAllRecordings(): Flow<List<RecordingEntity>>

    @Query("SELECT * FROM recordings WHERE id = :id")
    suspend fun getRecordingById(id: String): RecordingEntity?

    @Query("""
        SELECT * FROM recordings 
        WHERE title LIKE '%' || :query || '%' 
           OR transcript LIKE '%' || :query || '%' 
           OR summary LIKE '%' || :query || '%'
        ORDER BY createdAt DESC
    """)
    fun searchRecordings(query: String): Flow<List<RecordingEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecording(recording: RecordingEntity)

    @Update
    suspend fun updateRecording(recording: RecordingEntity)

    @Delete
    suspend fun deleteRecording(recording: RecordingEntity)

    @Query("DELETE FROM recordings WHERE id = :id")
    suspend fun deleteRecordingById(id: String)

    @Query("SELECT * FROM recordings WHERE createdAt >= :startTime AND createdAt <= :endTime ORDER BY createdAt ASC")
    suspend fun getRecordingsForDay(startTime: Long, endTime: Long): List<RecordingEntity>
}

@Dao
interface DailyReportDao {
    @Query("SELECT * FROM daily_reports ORDER BY timestamp DESC")
    fun getAllReports(): Flow<List<DailyReportEntity>>

    @Query("SELECT * FROM daily_reports WHERE dateString = :dateString LIMIT 1")
    suspend fun getReportForDate(dateString: String): DailyReportEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReport(report: DailyReportEntity)

    @Delete
    suspend fun deleteReport(report: DailyReportEntity)
}
