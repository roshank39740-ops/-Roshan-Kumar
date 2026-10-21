package com.example.data.model

data class ActionItem(
    val id: String,
    val task: String,
    val assignee: String = "Self",
    val deadline: String = "No deadline",
    val isCompleted: Boolean = false
)

data class TranscriptSegment(
    val id: String,
    val speaker: String,
    val timestampSeconds: Int,
    val text: String
)

data class Recording(
    val id: String,
    val title: String,
    val filePath: String,
    val durationSeconds: Long,
    val createdAt: Long,
    val category: String = "Meeting", // Meeting, Lecture, Interview, Conversation, Note
    val language: String = "English",
    val transcript: String = "",
    val transcriptSegments: List<TranscriptSegment> = emptyList(),
    val summary: String = "",
    val keyPoints: List<String> = emptyList(),
    val actionItems: List<ActionItem> = emptyList(),
    val uncertainPoints: List<String> = emptyList(),
    val isProcessed: Boolean = false,
    val isSynced: Boolean = false,
    val tags: List<String> = emptyList()
)

data class DailyReport(
    val id: String,
    val dateString: String,
    val recordingCount: Int,
    val totalDurationMinutes: Int,
    val executiveSummary: String,
    val keyDiscussions: List<String>,
    val actionItems: List<ActionItem>,
    val decisions: List<String>,
    val timestamp: Long
)

data class UserProfile(
    val name: String = "Alex Morgan",
    val email: String = "alex.morgan@recorded.ai",
    val isLoggedIn: Boolean = true,
    val preferredLanguage: String = "English",
    val dailyReportTime: String = "18:00",
    val audioRetentionDays: Int = 90,
    val autoTranscribe: Boolean = true,
    val cloudSync: Boolean = true
)
