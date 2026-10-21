package com.example.audio

import android.content.Context
import android.media.MediaPlayer
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

data class PlayerState(
    val currentRecordingId: String? = null,
    val isPlaying: Boolean = false,
    val currentPositionMs: Int = 0,
    val durationMs: Int = 0,
    val progress: Float = 0f
)

class AudioPlayerManager(private val context: Context) {
    private var mediaPlayer: MediaPlayer? = null
    private var progressJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    private val _playerState = MutableStateFlow(PlayerState())
    val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()

    fun playOrPause(recordingId: String, filePath: String) {
        if (_playerState.value.currentRecordingId == recordingId && mediaPlayer != null) {
            if (_playerState.value.isPlaying) {
                pause()
            } else {
                resume()
            }
        } else {
            startPlaying(recordingId, filePath)
        }
    }

    fun startPlaying(recordingId: String, filePath: String) {
        stop()

        val file = File(filePath)
        if (!file.exists() || file.length() == 0L) {
            Log.e("AudioPlayerManager", "Audio file not found: $filePath")
            return
        }

        try {
            val player = MediaPlayer().apply {
                setDataSource(filePath)
                prepare()
                start()
            }

            val totalDuration = player.duration
            _playerState.value = PlayerState(
                currentRecordingId = recordingId,
                isPlaying = true,
                currentPositionMs = 0,
                durationMs = totalDuration,
                progress = 0f
            )

            player.setOnCompletionListener {
                _playerState.value = _playerState.value.copy(
                    isPlaying = false,
                    currentPositionMs = 0,
                    progress = 0f
                )
                progressJob?.cancel()
            }

            mediaPlayer = player
            startTrackingProgress()
        } catch (e: Exception) {
            Log.e("AudioPlayerManager", "Error starting playback", e)
            stop()
        }
    }

    fun pause() {
        try {
            mediaPlayer?.pause()
            _playerState.value = _playerState.value.copy(isPlaying = false)
            progressJob?.cancel()
        } catch (e: Exception) {
            Log.e("AudioPlayerManager", "Error pausing playback", e)
        }
    }

    fun resume() {
        try {
            mediaPlayer?.start()
            _playerState.value = _playerState.value.copy(isPlaying = true)
            startTrackingProgress()
        } catch (e: Exception) {
            Log.e("AudioPlayerManager", "Error resuming playback", e)
        }
    }

    fun seekTo(progress: Float) {
        val player = mediaPlayer ?: return
        val targetMs = (player.duration * progress.coerceIn(0f, 1f)).toInt()
        try {
            player.seekTo(targetMs)
            _playerState.value = _playerState.value.copy(
                currentPositionMs = targetMs,
                progress = progress
            )
        } catch (e: Exception) {
            Log.e("AudioPlayerManager", "Error seeking", e)
        }
    }

    fun skipForward(seconds: Int = 5) {
        val player = mediaPlayer ?: return
        val newPos = (player.currentPosition + seconds * 1000).coerceAtMost(player.duration)
        try {
            player.seekTo(newPos)
            _playerState.value = _playerState.value.copy(
                currentPositionMs = newPos,
                progress = if (player.duration > 0) newPos.toFloat() / player.duration else 0f
            )
        } catch (e: Exception) {
            Log.e("AudioPlayerManager", "Error skipping forward", e)
        }
    }

    fun skipBackward(seconds: Int = 5) {
        val player = mediaPlayer ?: return
        val newPos = (player.currentPosition - seconds * 1000).coerceAtLeast(0)
        try {
            player.seekTo(newPos)
            _playerState.value = _playerState.value.copy(
                currentPositionMs = newPos,
                progress = if (player.duration > 0) newPos.toFloat() / player.duration else 0f
            )
        } catch (e: Exception) {
            Log.e("AudioPlayerManager", "Error skipping backward", e)
        }
    }

    fun stop() {
        progressJob?.cancel()
        progressJob = null
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (e: Exception) {
            Log.w("AudioPlayerManager", "Exception releasing player: ${e.message}")
        } finally {
            mediaPlayer = null
            _playerState.value = PlayerState()
        }
    }

    private fun startTrackingProgress() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                delay(100)
                val player = mediaPlayer
                if (player != null && player.isPlaying) {
                    val currentPos = player.currentPosition
                    val duration = player.duration
                    val progress = if (duration > 0) currentPos.toFloat() / duration else 0f
                    _playerState.value = _playerState.value.copy(
                        currentPositionMs = currentPos,
                        durationMs = duration,
                        progress = progress
                    )
                }
            }
        }
    }
}
