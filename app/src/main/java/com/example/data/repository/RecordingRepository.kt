package com.example.data.repository

import android.content.Context
import com.example.ai.AiTranscriptionService
import com.example.ai.DailyReportGenerator
import com.example.audio.AudioPlayerManager
import com.example.audio.AudioRecorderManager
import com.example.data.local.DailyReportEntity
import com.example.data.local.RecordedAiDatabase
import com.example.data.local.RecordingDao
import com.example.data.local.RecordingEntity
import com.example.data.model.ActionItem
import com.example.data.model.DailyReport
import com.example.data.model.Recording
import com.example.data.model.TranscriptSegment
import com.example.data.model.UserProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

class RecordingRepository(
    private val context: Context,
    private val database: RecordedAiDatabase,
    val recorderManager: AudioRecorderManager,
    val playerManager: AudioPlayerManager,
    val aiService: AiTranscriptionService,
    val reportGenerator: DailyReportGenerator
) {
    private val recordingDao = database.recordingDao()
    private val reportDao = database.dailyReportDao()

    private val _userProfile = MutableStateFlow(UserProfile())
    val userProfile: StateFlow<UserProfile> = _userProfile.asStateFlow()

    fun getAllRecordings(): Flow<List<Recording>> {
        return recordingDao.getAllRecordings().map { list ->
            list.map { it.toDomainModel() }
        }
    }

    fun searchRecordings(query: String): Flow<List<Recording>> {
        return recordingDao.searchRecordings(query).map { list ->
            list.map { it.toDomainModel() }
        }
    }

    suspend fun getRecordingById(id: String): Recording? = withContext(Dispatchers.IO) {
        recordingDao.getRecordingById(id)?.toDomainModel()
    }

    suspend fun saveNewRecording(
        id: String,
        title: String,
        category: String,
        language: String,
        audioFile: File,
        durationSeconds: Long
    ): Recording = withContext(Dispatchers.IO) {
        // First save as local unprocessed recording
        val initialRecording = Recording(
            id = id,
            title = title.ifBlank { "Recording ${SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(Date())}" },
            filePath = audioFile.absolutePath,
            durationSeconds = durationSeconds,
            createdAt = System.currentTimeMillis(),
            category = category,
            language = language,
            transcript = "Processing audio with AI...",
            summary = "Generating AI executive summary...",
            isProcessed = false,
            isSynced = false
        )
        recordingDao.insertRecording(RecordingEntity.fromDomainModel(initialRecording))

        // Trigger AI processing
        val aiResult = aiService.processRecording(
            recordingTitle = initialRecording.title,
            category = category,
            language = language,
            audioFile = audioFile,
            durationSeconds = durationSeconds
        )

        val processedRecording = initialRecording.copy(
            transcript = aiResult.transcript,
            transcriptSegments = aiResult.segments,
            summary = aiResult.summary,
            keyPoints = aiResult.keyPoints,
            actionItems = aiResult.actionItems,
            uncertainPoints = aiResult.uncertainPoints,
            isProcessed = true,
            isSynced = true
        )

        recordingDao.updateRecording(RecordingEntity.fromDomainModel(processedRecording))
        return@withContext processedRecording
    }

    suspend fun updateRecording(recording: Recording) = withContext(Dispatchers.IO) {
        recordingDao.updateRecording(RecordingEntity.fromDomainModel(recording))
    }

    suspend fun updateActionItemStatus(recordingId: String, actionItemId: String, isCompleted: Boolean) = withContext(Dispatchers.IO) {
        val entity = recordingDao.getRecordingById(recordingId) ?: return@withContext
        val domain = entity.toDomainModel()
        val updatedActions = domain.actionItems.map {
            if (it.id == actionItemId) it.copy(isCompleted = isCompleted) else it
        }
        val updated = domain.copy(actionItems = updatedActions)
        recordingDao.updateRecording(RecordingEntity.fromDomainModel(updated))
    }

    suspend fun deleteRecording(recording: Recording) = withContext(Dispatchers.IO) {
        recordingDao.deleteRecordingById(recording.id)
        val file = File(recording.filePath)
        if (file.exists()) {
            file.delete()
        }
    }

    suspend fun updateTitle(recordingId: String, newTitle: String) = withContext(Dispatchers.IO) {
        val entity = recordingDao.getRecordingById(recordingId) ?: return@withContext
        val updated = entity.toDomainModel().copy(title = newTitle)
        recordingDao.updateRecording(RecordingEntity.fromDomainModel(updated))
    }

    suspend fun updateTranscript(recordingId: String, newTranscript: String) = withContext(Dispatchers.IO) {
        val entity = recordingDao.getRecordingById(recordingId) ?: return@withContext
        val updated = entity.toDomainModel().copy(transcript = newTranscript)
        recordingDao.updateRecording(RecordingEntity.fromDomainModel(updated))
    }

    suspend fun getDailyReportForDate(dateString: String): DailyReport = withContext(Dispatchers.IO) {
        // Parse start and end of day timestamp
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val date = try {
            sdf.parse(dateString) ?: Date()
        } catch (e: Exception) {
            Date()
        }
        val cal = Calendar.getInstance().apply {
            time = date
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfDay = cal.timeInMillis
        cal.add(Calendar.DAY_OF_YEAR, 1)
        val endOfDay = cal.timeInMillis

        val dayEntities = recordingDao.getRecordingsForDay(startOfDay, endOfDay)
        val dayRecordings = dayEntities.map { it.toDomainModel() }

        return@withContext reportGenerator.generateDailyReport(dateString, dayRecordings)
    }

    fun updateUserProfile(profile: UserProfile) {
        _userProfile.value = profile
    }

    suspend fun seedSampleRecordingsIfEmpty() = withContext(Dispatchers.IO) {
        val count = recordingDao.getRecordingsForDay(0, Long.MAX_VALUE).size
        if (count == 0) {
            val now = System.currentTimeMillis()
            val sample1 = Recording(
                id = UUID.randomUUID().toString(),
                title = "Product Strategy & Q4 Roadmap Sync",
                filePath = "",
                durationSeconds = 1840,
                createdAt = now - (2 * 3600 * 1000), // 2 hours ago
                category = "Meeting",
                language = "English",
                transcript = "Sarah: Welcome everyone. We are aligning on the Q4 release milestone for Recorded AI.\n\n" +
                        "David: We completed testing for background microphone capture and amplitude waveform rendering.\n\n" +
                        "Elena: From the privacy side, users must see an explicit consent dialog before recording starts.\n\n" +
                        "Sarah: Agreed. Let's make sure our daily reports compile at 6:00 PM every evening with push notifications.",
                transcriptSegments = listOf(
                    TranscriptSegment(UUID.randomUUID().toString(), "Sarah", 0, "Welcome everyone. We are aligning on the Q4 release milestone for Recorded AI."),
                    TranscriptSegment(UUID.randomUUID().toString(), "David", 35, "We completed testing for background microphone capture and amplitude waveform rendering."),
                    TranscriptSegment(UUID.randomUUID().toString(), "Elena", 70, "From the privacy side, users must see an explicit consent dialog before recording starts."),
                    TranscriptSegment(UUID.randomUUID().toString(), "Sarah", 110, "Agreed. Let's make sure our daily reports compile at 6:00 PM every evening with push notifications.")
                ),
                summary = "The team reviewed Q4 deliverables for Recorded AI, highlighting completed background recording testing, consent policy enforcement, and 6:00 PM automated daily reporting.",
                keyPoints = listOf(
                    "Explicit consent badge and dialog mandatory before recording sessions.",
                    "Live waveform visualizer updates with low latency via amplitude polling.",
                    "Daily report digest scheduled for 6:00 PM."
                ),
                actionItems = listOf(
                    ActionItem(UUID.randomUUID().toString(), "Deploy background foreground notification service", "David", "Tomorrow 5 PM", false),
                    ActionItem(UUID.randomUUID().toString(), "Review GDPR & CCPA recording consent documentation", "Elena", "Friday", true),
                    ActionItem(UUID.randomUUID().toString(), "Configure daily report notification trigger", "Sarah", "Wednesday", false)
                ),
                uncertainPoints = listOf(
                    "Whether cloud transcription will default to 192kbps or 128kbps on metered cellular networks."
                ),
                isProcessed = true,
                isSynced = true,
                tags = listOf("Product", "Strategy", "Roadmap")
            )

            val sample2 = Recording(
                id = UUID.randomUUID().toString(),
                title = "CS301: Distributed Systems & Consensus",
                filePath = "",
                durationSeconds = 2750,
                createdAt = now - (5 * 3600 * 1000), // 5 hours ago
                category = "Lecture",
                language = "English",
                transcript = "Professor: Today we cover leader election in distributed systems.\n\n" +
                        "Professor: Heartbeats are transmitted at fixed intervals. If a follower misses three consecutive heartbeats, an election timer triggers.\n\n" +
                        "Student: What happens if network partitions isolate two sub-clusters?\n\n" +
                        "Professor: Only the partition with a strict majority quorum can commit log entries.",
                transcriptSegments = listOf(
                    TranscriptSegment(UUID.randomUUID().toString(), "Professor", 0, "Today we cover leader election in distributed systems."),
                    TranscriptSegment(UUID.randomUUID().toString(), "Professor", 45, "Heartbeats are transmitted at fixed intervals. If a follower misses three consecutive heartbeats, an election timer triggers."),
                    TranscriptSegment(UUID.randomUUID().toString(), "Student", 95, "What happens if network partitions isolate two sub-clusters?"),
                    TranscriptSegment(UUID.randomUUID().toString(), "Professor", 130, "Only the partition with a strict majority quorum can commit log entries.")
                ),
                summary = "Lecture on distributed consensus, quorum requirements during split-brain network partitions, and heartbeat threshold algorithms.",
                keyPoints = listOf(
                    "Leader heartbeats keep election timers in check across nodes.",
                    "Strict quorum prevents inconsistent state machine commits.",
                    "Midterm examination includes Raft log compaction."
                ),
                actionItems = listOf(
                    ActionItem(UUID.randomUUID().toString(), "Read Chapter 4 on Quorum Systems", "Self", "Thursday", false),
                    ActionItem(UUID.randomUUID().toString(), "Submit Lab 2: Leader Election Implementation", "Self", "Sunday midnight", false)
                ),
                uncertainPoints = listOf(
                    "Specific extra-credit grading criteria for Paxos comparison assignment."
                ),
                isProcessed = true,
                isSynced = true,
                tags = listOf("University", "Computer Science", "Exams")
            )

            val sample3 = Recording(
                id = UUID.randomUUID().toString(),
                title = "Client Interview - FinTech App UX",
                filePath = "",
                durationSeconds = 1420,
                createdAt = now - (24 * 3600 * 1000), // Yesterday
                category = "Interview",
                language = "English",
                transcript = "Interviewer: What is the primary friction point users experience in existing recording apps?\n\n" +
                        "Participant: Searching through an hour-long recording is frustrating. Having key takeaways and automated action items saves hours of re-listening.\n\n" +
                        "Interviewer: How important is multilingual support like Hindi and Hinglish?\n\n" +
                        "Participant: Extremely important for global teams who switch naturally between languages during conversations.",
                transcriptSegments = listOf(
                    TranscriptSegment(UUID.randomUUID().toString(), "Interviewer", 0, "What is the primary friction point users experience in existing recording apps?"),
                    TranscriptSegment(UUID.randomUUID().toString(), "Participant", 30, "Searching through an hour-long recording is frustrating. Having key takeaways and automated action items saves hours of re-listening."),
                    TranscriptSegment(UUID.randomUUID().toString(), "Interviewer", 70, "How important is multilingual support like Hindi and Hinglish?"),
                    TranscriptSegment(UUID.randomUUID().toString(), "Participant", 100, "Extremely important for global teams who switch naturally between languages during conversations.")
                ),
                summary = "User research session emphasizing the high utility of AI action item extraction and seamless bilingual / Hinglish transcription.",
                keyPoints = listOf(
                    "Summarization and action item checklists address the greatest user pain point.",
                    "Code-switching support (Hinglish/Spanish) is a major competitive differentiator.",
                    "Dark mode interface is strongly preferred during evening lectures and meetings."
                ),
                actionItems = listOf(
                    ActionItem(UUID.randomUUID().toString(), "Synthesize UX interview findings into slide deck", "Self", "Monday", true),
                    ActionItem(UUID.randomUUID().toString(), "Prototype bilingual language selector in Settings", "Self", "Wednesday", false)
                ),
                uncertainPoints = listOf(
                    "Participant requested integration with Notion or Slack (exact export format undefined)."
                ),
                isProcessed = true,
                isSynced = true,
                tags = listOf("UX Research", "Interviews")
            )

            recordingDao.insertRecording(RecordingEntity.fromDomainModel(sample1))
            recordingDao.insertRecording(RecordingEntity.fromDomainModel(sample2))
            recordingDao.insertRecording(RecordingEntity.fromDomainModel(sample3))
        }
    }
}
