package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.ai.DailyReportGenerator
import com.example.data.model.ActionItem
import com.example.data.model.Recording
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Recorded AI", appName)
  }

  @Test
  fun `daily report generator compiles multiple recordings`() {
    val generator = DailyReportGenerator()
    val recordings = listOf(
      Recording(
        id = "1",
        title = "Sprint Planning",
        filePath = "",
        durationSeconds = 1200,
        createdAt = System.currentTimeMillis(),
        category = "Meeting",
        summary = "Aligned on sprint objectives and roadmap.",
        actionItems = listOf(
          ActionItem("a1", "Complete API integration", "Alex", "Friday", false)
        )
      ),
      Recording(
        id = "2",
        title = "Client Interview",
        filePath = "",
        durationSeconds = 1800,
        createdAt = System.currentTimeMillis(),
        category = "Interview",
        summary = "Reviewed feature requirements with client.",
        actionItems = listOf(
          ActionItem("a2", "Share transcript", "Sam", "Tomorrow", true)
        )
      )
    )

    val report = generator.generateDailyReport("2026-10-21", recordings)
    assertEquals(2, report.recordingCount)
    assertEquals(50, report.totalDurationMinutes)
    assertEquals(2, report.actionItems.size)
    assertTrue(report.executiveSummary.contains("2 voice sessions"))
  }
}
