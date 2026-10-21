package com.example.ai

import com.example.data.model.ActionItem
import com.example.data.model.DailyReport
import com.example.data.model.Recording
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class DailyReportGenerator {

    fun generateDailyReport(
        dateString: String,
        recordings: List<Recording>
    ): DailyReport {
        if (recordings.isEmpty()) {
            return DailyReport(
                id = UUID.randomUUID().toString(),
                dateString = dateString,
                recordingCount = 0,
                totalDurationMinutes = 0,
                executiveSummary = "No recordings logged for this date.",
                keyDiscussions = listOf("No audio captured on $dateString."),
                actionItems = emptyList(),
                decisions = emptyList(),
                timestamp = System.currentTimeMillis()
            )
        }

        val totalDurationSec = recordings.sumOf { it.durationSeconds }
        val totalMinutes = (totalDurationSec / 60).toInt().coerceAtLeast(1)

        val allActions = mutableListOf<ActionItem>()
        val allDiscussions = mutableListOf<String>()
        val allDecisions = mutableListOf<String>()

        recordings.forEach { recording ->
            allActions.addAll(recording.actionItems)
            if (recording.summary.isNotBlank()) {
                allDiscussions.add("${recording.title} (${recording.category}): ${recording.summary}")
            }
            recording.keyPoints.take(2).forEach { point ->
                allDecisions.add(point)
            }
        }

        val executiveSummary = buildString {
            append("On $dateString, ${recordings.size} voice sessions totaling $totalMinutes minutes were recorded and analyzed. ")
            if (allActions.isNotEmpty()) {
                val pending = allActions.count { !it.isCompleted }
                append("$pending active action items pending attention across discussions.")
            } else {
                append("All sessions processed without pending bottlenecks.")
            }
        }

        return DailyReport(
            id = UUID.randomUUID().toString(),
            dateString = dateString,
            recordingCount = recordings.size,
            totalDurationMinutes = totalMinutes,
            executiveSummary = executiveSummary,
            keyDiscussions = allDiscussions,
            actionItems = allActions,
            decisions = allDecisions.distinct().take(6),
            timestamp = System.currentTimeMillis()
        )
    }

    companion object {
        fun getCurrentDateString(): String {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            return sdf.format(Date())
        }

        fun formatDisplayDate(dateString: String): String {
            return try {
                val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val date = inputFormat.parse(dateString)
                val outputFormat = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault())
                if (date != null) outputFormat.format(date) else dateString
            } catch (e: Exception) {
                dateString
            }
        }
    }
}
