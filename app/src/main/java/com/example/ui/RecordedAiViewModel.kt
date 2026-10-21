package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.AiTranscriptionService
import com.example.ai.DailyReportGenerator
import com.example.audio.AudioPlayerManager
import com.example.audio.AudioRecorderManager
import com.example.audio.PlayerState
import com.example.audio.RecorderState
import com.example.audio.RecordingSession
import com.example.data.local.RecordedAiDatabase
import com.example.data.model.DailyReport
import com.example.data.model.Recording
import com.example.data.model.UserProfile
import com.example.data.repository.RecordingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

enum class NavigationScreen {
    HOME,
    RECORDINGS,
    DAILY_REPORT,
    SETTINGS
}

data class NotificationToast(
    val title: String,
    val message: String,
    val timestamp: Long = System.currentTimeMillis()
)

class RecordedAiViewModel(application: Application) : AndroidViewModel(application) {

    private val database = RecordedAiDatabase.getInstance(application)
    val recorderManager = AudioRecorderManager(application)
    val playerManager = AudioPlayerManager(application)
    private val aiService = AiTranscriptionService()
    private val reportGenerator = DailyReportGenerator()

    val repository = RecordingRepository(
        context = application,
        database = database,
        recorderManager = recorderManager,
        playerManager = playerManager,
        aiService = aiService,
        reportGenerator = reportGenerator
    )

    private val _currentScreen = MutableStateFlow(NavigationScreen.HOME)
    val currentScreen: StateFlow<NavigationScreen> = _currentScreen.asStateFlow()

    private val _selectedRecording = MutableStateFlow<Recording?>(null)
    val selectedRecording: StateFlow<Recording?> = _selectedRecording.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategoryFilter = MutableStateFlow("All")
    val selectedCategoryFilter: StateFlow<String> = _selectedCategoryFilter.asStateFlow()

    // Recording HUD state
    private val _showRecordingSheet = MutableStateFlow(false)
    val showRecordingSheet: StateFlow<Boolean> = _showRecordingSheet.asStateFlow()

    private val _showConsentDialog = MutableStateFlow(false)
    val showConsentDialog: StateFlow<Boolean> = _showConsentDialog.asStateFlow()

    private val _recordingTitle = MutableStateFlow("")
    val recordingTitle: StateFlow<String> = _recordingTitle.asStateFlow()

    private val _recordingCategory = MutableStateFlow("Meeting")
    val recordingCategory: StateFlow<String> = _recordingCategory.asStateFlow()

    private val _recordingLanguage = MutableStateFlow("English")
    val recordingLanguage: StateFlow<String> = _recordingLanguage.asStateFlow()

    private val _isAiProcessing = MutableStateFlow(false)
    val isAiProcessing: StateFlow<Boolean> = _isAiProcessing.asStateFlow()

    // Daily Report state
    private val _selectedDateString = MutableStateFlow(DailyReportGenerator.getCurrentDateString())
    val selectedDateString: StateFlow<String> = _selectedDateString.asStateFlow()

    private val _dailyReport = MutableStateFlow<DailyReport?>(null)
    val dailyReport: StateFlow<DailyReport?> = _dailyReport.asStateFlow()

    private val _isGeneratingReport = MutableStateFlow(false)
    val isGeneratingReport: StateFlow<Boolean> = _isGeneratingReport.asStateFlow()

    // In-app Notification simulation
    private val _activeNotification = MutableStateFlow<NotificationToast?>(null)
    val activeNotification: StateFlow<NotificationToast?> = _activeNotification.asStateFlow()

    // Onboarding / Consent first time state
    private val _hasSeenOnboarding = MutableStateFlow(true)
    val hasSeenOnboarding: StateFlow<Boolean> = _hasSeenOnboarding.asStateFlow()

    val userProfile: StateFlow<UserProfile> = repository.userProfile
    val sessionState: StateFlow<RecordingSession> = recorderManager.sessionState
    val playerState: StateFlow<PlayerState> = playerManager.playerState

    // Recordings list with search & category filtering
    val allRecordings: StateFlow<List<Recording>> = repository.getAllRecordings()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredRecordings: StateFlow<List<Recording>> = combine(
        allRecordings,
        _searchQuery,
        _selectedCategoryFilter
    ) { recordings, query, category ->
        recordings.filter { rec ->
            val matchesQuery = query.isBlank() ||
                    rec.title.contains(query, ignoreCase = true) ||
                    rec.transcript.contains(query, ignoreCase = true) ||
                    rec.summary.contains(query, ignoreCase = true) ||
                    rec.tags.any { it.contains(query, ignoreCase = true) }

            val matchesCategory = category == "All" || rec.category.equals(category, ignoreCase = true)

            matchesQuery && matchesCategory
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            repository.seedSampleRecordingsIfEmpty()
            loadDailyReportForDate(_selectedDateString.value)
        }
    }

    fun navigateTo(screen: NavigationScreen) {
        _currentScreen.value = screen
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setCategoryFilter(category: String) {
        _selectedCategoryFilter.value = category
    }

    fun selectRecording(recording: Recording?) {
        _selectedRecording.value = recording
    }

    fun requestStartRecording() {
        _showConsentDialog.value = true
    }

    fun confirmConsentAndStart(title: String = "", category: String = "Meeting", language: String = "English") {
        _showConsentDialog.value = false
        _recordingTitle.value = title.ifBlank { "Session ${allRecordings.value.size + 1}" }
        _recordingCategory.value = category
        _recordingLanguage.value = language

        val newId = UUID.randomUUID().toString()
        val started = recorderManager.startRecording(newId)
        if (started) {
            _showRecordingSheet.value = true
        }
    }

    fun dismissConsentDialog() {
        _showConsentDialog.value = false
    }

    fun togglePauseRecording() {
        if (sessionState.value.state == RecorderState.RECORDING) {
            recorderManager.pauseRecording()
        } else if (sessionState.value.state == RecorderState.PAUSED) {
            recorderManager.resumeRecording()
        }
    }

    fun stopAndSaveRecording() {
        val currentSession = sessionState.value
        val file = recorderManager.stopRecording()
        _showRecordingSheet.value = false

        if (file != null && file.exists()) {
            val title = _recordingTitle.value
            val category = _recordingCategory.value
            val language = _recordingLanguage.value
            val duration = currentSession.durationSeconds.coerceAtLeast(1)

            viewModelScope.launch {
                _isAiProcessing.value = true
                _activeNotification.value = NotificationToast(
                    title = "AI Transcription Started",
                    message = "Analyzing \"$title\" ($duration s)..."
                )

                val savedRecording = repository.saveNewRecording(
                    id = currentSession.id.ifBlank { UUID.randomUUID().toString() },
                    title = title,
                    category = category,
                    language = language,
                    audioFile = file,
                    durationSeconds = duration
                )

                _isAiProcessing.value = false
                _activeNotification.value = NotificationToast(
                    title = "AI Summary Ready",
                    message = "\"${savedRecording.title}\" transcribed with ${savedRecording.actionItems.size} action items."
                )

                // Select the new recording to show details
                _selectedRecording.value = savedRecording
                loadDailyReportForDate(_selectedDateString.value)
            }
        }
    }

    fun cancelRecording() {
        recorderManager.cancelRecording()
        _showRecordingSheet.value = false
    }

    fun playRecording(recording: Recording) {
        if (recording.filePath.isNotBlank()) {
            playerManager.playOrPause(recording.id, recording.filePath)
        } else {
            // For sample recordings without physical file, simulate playback
            _activeNotification.value = NotificationToast(
                title = "Preview Mode",
                message = "Simulating playback for \"${recording.title}\""
            )
        }
    }

    fun pausePlayback() {
        playerManager.pause()
    }

    fun seekPlayback(progress: Float) {
        playerManager.seekTo(progress)
    }

    fun skipPlaybackForward() {
        playerManager.skipForward(5)
    }

    fun skipPlaybackBackward() {
        playerManager.skipBackward(5)
    }

    fun toggleActionItem(recordingId: String, actionItemId: String, isCompleted: Boolean) {
        viewModelScope.launch {
            repository.updateActionItemStatus(recordingId, actionItemId, isCompleted)
            // Update selected recording if viewing
            _selectedRecording.value?.let { current ->
                if (current.id == recordingId) {
                    val updatedActions = current.actionItems.map {
                        if (it.id == actionItemId) it.copy(isCompleted = isCompleted) else it
                    }
                    _selectedRecording.value = current.copy(actionItems = updatedActions)
                }
            }
            loadDailyReportForDate(_selectedDateString.value)
        }
    }

    fun updateTranscript(recordingId: String, newTranscript: String) {
        viewModelScope.launch {
            repository.updateTranscript(recordingId, newTranscript)
            _selectedRecording.value?.let { current ->
                if (current.id == recordingId) {
                    _selectedRecording.value = current.copy(transcript = newTranscript)
                }
            }
            _activeNotification.value = NotificationToast(
                title = "Transcript Updated",
                message = "Manual edits saved successfully."
            )
        }
    }

    fun renameRecording(recordingId: String, newTitle: String) {
        viewModelScope.launch {
            repository.updateTitle(recordingId, newTitle)
            _selectedRecording.value?.let { current ->
                if (current.id == recordingId) {
                    _selectedRecording.value = current.copy(title = newTitle)
                }
            }
        }
    }

    fun deleteRecording(recording: Recording) {
        viewModelScope.launch {
            if (playerState.value.currentRecordingId == recording.id) {
                playerManager.stop()
            }
            repository.deleteRecording(recording)
            if (_selectedRecording.value?.id == recording.id) {
                _selectedRecording.value = null
            }
            loadDailyReportForDate(_selectedDateString.value)
            _activeNotification.value = NotificationToast(
                title = "Recording Deleted",
                message = "\"${recording.title}\" and audio file removed."
            )
        }
    }

    fun setDailyReportDate(dateString: String) {
        _selectedDateString.value = dateString
        loadDailyReportForDate(dateString)
    }

    fun loadDailyReportForDate(dateString: String) {
        viewModelScope.launch {
            _isGeneratingReport.value = true
            val report = repository.getDailyReportForDate(dateString)
            _dailyReport.value = report
            _isGeneratingReport.value = false
        }
    }

    fun triggerDailyPushNotification() {
        val report = _dailyReport.value ?: return
        _activeNotification.value = NotificationToast(
            title = "Daily Summary Ready (${report.dateString})",
            message = "${report.recordingCount} recordings processed. ${report.actionItems.count { !it.isCompleted }} tasks pending."
        )
    }

    fun dismissNotificationToast() {
        _activeNotification.value = null
    }

    fun updateUserProfile(profile: UserProfile) {
        repository.updateUserProfile(profile)
        _activeNotification.value = NotificationToast(
            title = "Settings Saved",
            message = "Preferences updated."
        )
    }

    override fun onCleared() {
        super.onCleared()
        recorderManager.cancelRecording()
        playerManager.stop()
    }
}
