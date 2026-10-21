package com.example.audio

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException

enum class RecorderState {
    IDLE,
    RECORDING,
    PAUSED,
    STOPPED
}

data class RecordingSession(
    val id: String,
    val file: File,
    val durationSeconds: Long,
    val state: RecorderState,
    val currentAmplitudes: List<Float> = emptyList()
)

class AudioRecorderManager(private val context: Context) {
    private var mediaRecorder: MediaRecorder? = null
    private var currentFile: File? = null
    private var currentId: String = ""
    private var startTimeMillis: Long = 0
    private var totalRecordedDurationMillis: Long = 0
    private var isPaused = false

    private val _sessionState = MutableStateFlow(
        RecordingSession(
            id = "",
            file = File(""),
            durationSeconds = 0,
            state = RecorderState.IDLE
        )
    )
    val sessionState: StateFlow<RecordingSession> = _sessionState.asStateFlow()

    private var pollJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    private fun getRecordingsDir(): File {
        val dir = File(context.filesDir, "recordings")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    fun startRecording(recordingId: String): Boolean {
        try {
            stopRecordingInternal()

            currentId = recordingId
            val file = File(getRecordingsDir(), "recording_${recordingId}.m4a")
            currentFile = file

            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            recorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }

            mediaRecorder = recorder
            startTimeMillis = System.currentTimeMillis()
            totalRecordedDurationMillis = 0
            isPaused = false

            _sessionState.value = RecordingSession(
                id = currentId,
                file = file,
                durationSeconds = 0,
                state = RecorderState.RECORDING,
                currentAmplitudes = emptyList()
            )

            startAmplitudePolling()
            return true
        } catch (e: Exception) {
            Log.e("AudioRecorderManager", "Error starting recording", e)
            _sessionState.value = _sessionState.value.copy(state = RecorderState.IDLE)
            return false
        }
    }

    fun pauseRecording() {
        if (_sessionState.value.state == RecorderState.RECORDING && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                mediaRecorder?.pause()
                val elapsed = System.currentTimeMillis() - startTimeMillis
                totalRecordedDurationMillis += elapsed
                isPaused = true
                _sessionState.value = _sessionState.value.copy(state = RecorderState.PAUSED)
            } catch (e: Exception) {
                Log.e("AudioRecorderManager", "Error pausing recorder", e)
            }
        }
    }

    fun resumeRecording() {
        if (_sessionState.value.state == RecorderState.PAUSED && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                mediaRecorder?.resume()
                startTimeMillis = System.currentTimeMillis()
                isPaused = false
                _sessionState.value = _sessionState.value.copy(state = RecorderState.RECORDING)
            } catch (e: Exception) {
                Log.e("AudioRecorderManager", "Error resuming recorder", e)
            }
        }
    }

    fun stopRecording(): File? {
        val savedFile = currentFile
        stopRecordingInternal()
        return savedFile
    }

    fun cancelRecording() {
        val fileToDelete = currentFile
        stopRecordingInternal()
        fileToDelete?.delete()
    }

    private fun stopRecordingInternal() {
        pollJob?.cancel()
        pollJob = null

        try {
            if (_sessionState.value.state == RecorderState.RECORDING || _sessionState.value.state == RecorderState.PAUSED) {
                if (!isPaused) {
                    val elapsed = System.currentTimeMillis() - startTimeMillis
                    totalRecordedDurationMillis += elapsed
                }
                mediaRecorder?.stop()
            }
        } catch (e: Exception) {
            Log.w("AudioRecorderManager", "Exception stopping recorder: ${e.message}")
        } finally {
            try {
                mediaRecorder?.release()
            } catch (e: Exception) {
                // Ignore
            }
            mediaRecorder = null
        }

        val finalDurationSec = totalRecordedDurationMillis / 1000
        _sessionState.value = _sessionState.value.copy(
            state = RecorderState.STOPPED,
            durationSeconds = finalDurationSec
        )
    }

    private fun startAmplitudePolling() {
        pollJob?.cancel()
        pollJob = scope.launch {
            val amplitudes = mutableListOf<Float>()
            while (isActive) {
                delay(80)
                if (_sessionState.value.state == RecorderState.RECORDING) {
                    val maxAmp = try {
                        mediaRecorder?.maxAmplitude ?: 0
                    } catch (e: Exception) {
                        0
                    }

                    // Normalize amplitude 0.0 .. 1.0 (max around 32767)
                    val normalized = (maxAmp.toFloat() / 32767f).coerceIn(0.05f, 1.0f)
                    amplitudes.add(normalized)
                    if (amplitudes.size > 50) {
                        amplitudes.removeAt(0)
                    }

                    val elapsedSinceResume = System.currentTimeMillis() - startTimeMillis
                    val currentTotalDurationSec = (totalRecordedDurationMillis + elapsedSinceResume) / 1000

                    _sessionState.value = _sessionState.value.copy(
                        durationSeconds = currentTotalDurationSec,
                        currentAmplitudes = amplitudes.toList()
                    )
                }
            }
        }
    }
}
