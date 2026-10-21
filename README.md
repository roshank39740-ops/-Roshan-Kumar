# Recorded AI - Modern AI Voice Recorder for Android

**Recorded AI** is an enterprise-grade AI voice recording and speech intelligence mobile application built with **Jetpack Compose**, **Kotlin**, **Android MediaRecorder / MediaPlayer**, and **Gemini 3.5 Multimodal AI**. It captures meetings, university lectures, user interviews, and conversations with ethical participant consent, generating real-time waveforms, diarized transcripts with timestamps, executive summaries, actionable item checklists, and automated daily digests.

---

## 📱 Key Features

1. **Audio Recording System & Visualizer**
   - High-fidelity AAC audio capture via Android `MediaRecorder` saved locally in secure app sandbox storage (`context.filesDir/recordings/`).
   - Real-time amplitude polling (every 80ms) rendering a dynamic 36-bar audio soundwave visualizer.
   - Recording HUD with timer, pause/resume, cancel, and stop & analyze controls.
   - Distinctive pulsating red recording beacon ensuring transparent recording status.

2. **Ethical Consent Architecture**
   - Consent confirmation modal before starting any recording session.
   - Adheres to two-party and multi-party recording regulations.
   - Customizable session title, category tags (*Meeting*, *Lecture*, *Interview*, *Conversation*, *Note*), and target language.

3. **Multilingual AI Speech Intelligence (Gemini 3.5 Flash)**
   - Automatic speech-to-text with speaker diarization (`[Speaker 1]`, `[Speaker 2]`).
   - Multi-language support including English, Hindi, Hinglish (code-switching), and Spanish.
   - Executive summaries highlighting major topics and decisions.
   - Structured action items extraction with responsible assignees, deadlines, and toggleable checkboxes.
   - Flagged uncertainties: explicitly flags inaudible, ambiguous, or contradictory statements rather than inventing hallucinations.
   - Direct manual transcript editor allowing users to correct transcription inaccuracies.

4. **Daily Executive Reports**
   - Aggregates all audio sessions for any selected day.
   - Calculates total recordings and listening duration.
   - Synthesizes across all meetings for the day into a unified executive brief.
   - Consolidated action items list with interactive completion status.
   - One-tap "Share Daily Report" export and simulated evening push notification reminder.

5. **Audio Playback & History**
   - Embedded audio player with seek slider, duration counters, play/pause, and -5s/+5s skip controls.
   - Search across recordings by title, full transcript text, and topics.
   - Category filtering chips (*All*, *Meeting*, *Lecture*, *Interview*, *Note*).
   - In-place renaming, deletion, and note exporting.

6. **Privacy & Settings**
   - Configurable audio retention policies (*30 Days*, *90 Days*, or *Forever*).
   - Daily report delivery time scheduler (e.g. *18:00*).
   - Complete personal data export and database purge controls.

---

## 🛠️ Technology Stack & Architecture

- **Mobile Client**: Android Native (Kotlin, Jetpack Compose, Material Design 3)
- **Audio Capture & Playback**: Android `MediaRecorder` (AAC/M4A 128kbps) & `MediaPlayer`
- **Local Persistence**: Android Room Database (`RecordedAiDatabase`, `RecordingDao`, `DailyReportDao`)
- **Speech Intelligence**: Gemini 3.5 Flash REST API via OkHttp / Moshi (with offline contextual fallback)
- **Design System**: Dark Navy (`#090E17`), Electric Blue (`#38BDF8`), Accent Indigo (`#818CF8`), Recording Red (`#EF4444`)
- **Backend Architecture**: Designed with a clean repository layer compatible with a FastAPI + PostgreSQL backend.

---

## 📂 Project Structure

```
├── app/src/main/
│   ├── AndroidManifest.xml                  # Audio & Network permissions
│   ├── java/com/example/
│   │   ├── MainActivity.kt                  # App root, navigation scaffold, permissions
│   │   ├── ai/
│   │   │   ├── AiTranscriptionService.kt    # Gemini API speech intelligence & fallback
│   │   │   └── DailyReportGenerator.kt      # Cross-session daily aggregation
│   │   ├── audio/
│   │   │   ├── AudioRecorderManager.kt      # MediaRecorder & live amplitude polling
│   │   │   └── AudioPlayerManager.kt        # MediaPlayer with scrubber & skip controls
│   │   ├── data/
│   │   │   ├── local/
│   │   │   │   ├── Converters.kt            # Room JSON type adapters
│   │   │   │   ├── Daos.kt                  # RecordingDao & DailyReportDao
│   │   │   │   ├── Entities.kt              # Room database table entities
│   │   │   │   └── RecordedAiDatabase.kt    # Room database singleton
│   │   │   ├── model/
│   │   │   │   └── Models.kt                # Domain models (Recording, ActionItem, DailyReport)
│   │   │   └── repository/
│   │   │       └── RecordingRepository.kt   # Central data repository & audio file sync
│   │   └── ui/
│   │       ├── RecordedAiViewModel.kt       # StateFlow UI architecture
│   │       ├── components/
│   │       │   ├── CommonComponents.kt      # LiveWaveformVisualizer, PulseIndicator, Banner
│   │       │   ├── ConsentDialog.kt         # Recording consent & session configuration
│   │       │   ├── RecordingSheet.kt        # Active recording HUD bottom sheet
│   │       │   └── AudioPlayerBottomBar.kt  # Persistent mini player bar
│   │       ├── screens/
│   │       │   ├── DashboardScreen.kt       # Home dashboard, quick stats, pending tasks
│   │       │   ├── RecordingsHistoryScreen.kt# Searchable & filterable session archive
│   │       │   ├── DailyReportScreen.kt     # Executive synthesis & daily checklist
│   │       │   ├── RecordingDetailScreen.kt # Diarized transcript, summary & edit modal
│   │       │   └── SettingsScreen.kt        # Language, retention, schedule, data wipe
│   │       └── theme/
│   │           ├── Color.kt                 # Dark Navy & White palette
│   │           └── Theme.kt                 # Material 3 RecordedAiTheme
│   └── res/                                 # Adaptive launcher icons and vector assets
```

---

## 🚀 Build & Run

### Prerequisites
- Android Studio Ladybug / Meerkat or Gradle command line
- JDK 17 / 21
- Android SDK 36 (minSdk 24)

### Commands
```bash
# Compile and build debug APK
gradle assembleDebug

# Run Unit and Robolectric Tests
gradle :app:testDebugUnitTest
```

### Configuring Secrets (Gemini API Key)
Add your Gemini API Key in the AI Studio **Secrets panel** or in `.env`:
```properties
GEMINI_API_KEY=your_gemini_api_key_here
```
