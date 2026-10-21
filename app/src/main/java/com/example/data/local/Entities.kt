package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.ActionItem
import com.example.data.model.Recording
import com.example.data.model.TranscriptSegment

@Entity(tableName = "recordings")
data class RecordingEntity(
    @PrimaryKey val id: String,
    val title: String,
    val filePath: String,
    val durationSeconds: Long,
    val createdAt: Long,
    val category: String,
    val language: String,
    val transcript: String,
    val transcriptSegments: List<TranscriptSegment>,
    val summary: String,
    val keyPoints: List<String>,
    val actionItems: List<ActionItem>,
    val uncertainPoints: List<String>,
    val isProcessed: Boolean,
    val isSynced: Boolean,
    val tags: List<String>
) {
    fun toDomainModel(): Recording = Recording(
        id = id,
        title = title,
        filePath = filePath,
        durationSeconds = durationSeconds,
        createdAt = createdAt,
        category = category,
        language = language,
        transcript = transcript,
        transcriptSegments = transcriptSegments,
        summary = summary,
        keyPoints = keyPoints,
        actionItems = actionItems,
        uncertainPoints = uncertainPoints,
        isProcessed = isProcessed,
        isSynced = isSynced,
        tags = tags
    )

    companion object {
        fun fromDomainModel(recording: Recording): RecordingEntity = RecordingEntity(
            id = recording.id,
            title = recording.title,
            filePath = recording.filePath,
            durationSeconds = recording.durationSeconds,
            createdAt = recording.createdAt,
            category = recording.category,
            language = recording.language,
            transcript = recording.transcript,
            transcriptSegments = recording.transcriptSegments,
            summary = recording.summary,
            keyPoints = recording.keyPoints,
            actionItems = recording.actionItems,
            uncertainPoints = recording.uncertainPoints,
            isProcessed = recording.isProcessed,
            isSynced = recording.isSynced,
            tags = recording.tags
        )
    }
}

@Entity(tableName = "daily_reports")
data class DailyReportEntity(
    @PrimaryKey val id: String,
    val dateString: String,
    val recordingCount: Int,
    val totalDurationMinutes: Int,
    val executiveSummary: String,
    val keyDiscussions: List<String>,
    val actionItems: List<ActionItem>,
    val decisions: List<String>,
    val timestamp: Long
)
