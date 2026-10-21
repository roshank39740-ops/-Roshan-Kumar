package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ui.NavigationScreen
import com.example.ui.RecordedAiViewModel
import com.example.ui.components.AudioPlayerBottomBar
import com.example.ui.components.NotificationBanner
import com.example.ui.components.RecordingConsentDialog
import com.example.ui.components.RecordingHudSheet
import com.example.ui.screens.DailyReportScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.RecordingDetailScreen
import com.example.ui.screens.RecordingsHistoryScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.CardNavyBorder
import com.example.ui.theme.CardNavySurface
import com.example.ui.theme.DeepNavyBg
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.RecordedAiTheme
import com.example.ui.theme.RecordingRed

class MainActivity : ComponentActivity() {
    private val viewModel: RecordedAiViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            RecordedAiTheme(darkTheme = true) {
                MainAppScreen(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MainAppScreen(viewModel: RecordedAiViewModel) {
    val context = LocalContext.current
    val currentScreen by viewModel.currentScreen.collectAsState()
    val selectedRecording by viewModel.selectedRecording.collectAsState()
    val allRecordings by viewModel.allRecordings.collectAsState()
    val filteredRecordings by viewModel.filteredRecordings.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val categoryFilter by viewModel.selectedCategoryFilter.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    val sessionState by viewModel.sessionState.collectAsState()
    val playerState by viewModel.playerState.collectAsState()
    val showRecordingSheet by viewModel.showRecordingSheet.collectAsState()
    val showConsentDialog by viewModel.showConsentDialog.collectAsState()
    val recordingTitle by viewModel.recordingTitle.collectAsState()
    val recordingCategory by viewModel.recordingCategory.collectAsState()
    val recordingLanguage by viewModel.recordingLanguage.collectAsState()
    val dailyReport by viewModel.dailyReport.collectAsState()
    val selectedDateString by viewModel.selectedDateString.collectAsState()
    val isGeneratingReport by viewModel.isGeneratingReport.collectAsState()
    val activeNotification by viewModel.activeNotification.collectAsState()

    // Permission Launcher for Microphone
    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.requestStartRecording()
        } else {
            Toast.makeText(
                context,
                "Microphone permission is required to record audio sessions.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // Permission Launcher for Notifications on Android 13+
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { /* Permission result handled gracefully */ }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    fun handleStartRecordingClick() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            viewModel.requestStartRecording()
        } else {
            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    // Back handling
    BackHandler(enabled = selectedRecording != null || currentScreen != NavigationScreen.HOME) {
        if (selectedRecording != null) {
            viewModel.selectRecording(null)
        } else if (currentScreen != NavigationScreen.HOME) {
            viewModel.navigateTo(NavigationScreen.HOME)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = DeepNavyBg,
        bottomBar = {
            if (selectedRecording == null) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Mini Audio Player if playing or loaded
                    if (playerState.currentRecordingId != null) {
                        val currentPlayingRec = allRecordings.firstOrNull { it.id == playerState.currentRecordingId }
                        AudioPlayerBottomBar(
                            playerState = playerState,
                            recordingTitle = currentPlayingRec?.title ?: "Playing Audio",
                            onPlayPause = {
                                if (currentPlayingRec != null) {
                                    viewModel.playRecording(currentPlayingRec)
                                }
                            },
                            onSeek = { viewModel.seekPlayback(it) },
                            onSkipForward = { viewModel.skipPlaybackForward() },
                            onSkipBackward = { viewModel.skipPlaybackBackward() },
                            onClose = { viewModel.pausePlayback() },
                            onClickBar = {
                                if (currentPlayingRec != null) {
                                    viewModel.selectRecording(currentPlayingRec)
                                }
                            }
                        )
                    }

                    // Navigation Bar
                    NavigationBar(
                        containerColor = CardNavySurface,
                        tonalElevation = 8.dp
                    ) {
                        NavigationBarItem(
                            selected = currentScreen == NavigationScreen.HOME,
                            onClick = { viewModel.navigateTo(NavigationScreen.HOME) },
                            icon = {
                                Icon(
                                    imageVector = if (currentScreen == NavigationScreen.HOME) Icons.Filled.Home else Icons.Outlined.Home,
                                    contentDescription = "Home"
                                )
                            },
                            label = { Text("Home", fontSize = 11.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = ElectricBlue,
                                selectedTextColor = ElectricBlue,
                                unselectedIconColor = Color(0xFF94A3B8),
                                unselectedTextColor = Color(0xFF94A3B8),
                                indicatorColor = CardNavyBorder
                            ),
                            modifier = Modifier.testTag("nav_home")
                        )

                        NavigationBarItem(
                            selected = currentScreen == NavigationScreen.RECORDINGS,
                            onClick = { viewModel.navigateTo(NavigationScreen.RECORDINGS) },
                            icon = {
                                Icon(
                                    imageVector = if (currentScreen == NavigationScreen.RECORDINGS) Icons.Filled.GraphicEq else Icons.Outlined.GraphicEq,
                                    contentDescription = "Recordings"
                                )
                            },
                            label = { Text("Recordings", fontSize = 11.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = ElectricBlue,
                                selectedTextColor = ElectricBlue,
                                unselectedIconColor = Color(0xFF94A3B8),
                                unselectedTextColor = Color(0xFF94A3B8),
                                indicatorColor = CardNavyBorder
                            ),
                            modifier = Modifier.testTag("nav_recordings")
                        )

                        NavigationBarItem(
                            selected = currentScreen == NavigationScreen.DAILY_REPORT,
                            onClick = { viewModel.navigateTo(NavigationScreen.DAILY_REPORT) },
                            icon = {
                                Icon(
                                    imageVector = if (currentScreen == NavigationScreen.DAILY_REPORT) Icons.Filled.Assessment else Icons.Outlined.Assessment,
                                    contentDescription = "Daily Report"
                                )
                            },
                            label = { Text("Daily Digest", fontSize = 11.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = ElectricBlue,
                                selectedTextColor = ElectricBlue,
                                unselectedIconColor = Color(0xFF94A3B8),
                                unselectedTextColor = Color(0xFF94A3B8),
                                indicatorColor = CardNavyBorder
                            ),
                            modifier = Modifier.testTag("nav_daily_report")
                        )

                        NavigationBarItem(
                            selected = currentScreen == NavigationScreen.SETTINGS,
                            onClick = { viewModel.navigateTo(NavigationScreen.SETTINGS) },
                            icon = {
                                Icon(
                                    imageVector = if (currentScreen == NavigationScreen.SETTINGS) Icons.Filled.Settings else Icons.Outlined.Settings,
                                    contentDescription = "Settings"
                                )
                            },
                            label = { Text("Settings", fontSize = 11.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = ElectricBlue,
                                selectedTextColor = ElectricBlue,
                                unselectedIconColor = Color(0xFF94A3B8),
                                unselectedTextColor = Color(0xFF94A3B8),
                                indicatorColor = CardNavyBorder
                            ),
                            modifier = Modifier.testTag("nav_settings")
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            if (selectedRecording == null && currentScreen != NavigationScreen.HOME) {
                FloatingActionButton(
                    onClick = { handleStartRecordingClick() },
                    containerColor = RecordingRed,
                    contentColor = Color.White,
                    elevation = FloatingActionButtonDefaults.elevation(6.dp),
                    modifier = Modifier.testTag("fab_record")
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Record Audio"
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Main Screen Routing
            if (selectedRecording != null) {
                RecordingDetailScreen(
                    recording = selectedRecording!!,
                    playerState = playerState,
                    onBack = { viewModel.selectRecording(null) },
                    onPlayPause = { viewModel.playRecording(selectedRecording!!) },
                    onSeek = { viewModel.seekPlayback(it) },
                    onSkipForward = { viewModel.skipPlaybackForward() },
                    onSkipBackward = { viewModel.skipPlaybackBackward() },
                    onToggleActionItem = { actionId, completed ->
                        viewModel.toggleActionItem(selectedRecording!!.id, actionId, completed)
                    },
                    onUpdateTranscript = { newTranscript ->
                        viewModel.updateTranscript(selectedRecording!!.id, newTranscript)
                    }
                )
            } else {
                when (currentScreen) {
                    NavigationScreen.HOME -> {
                        DashboardScreen(
                            recordings = allRecordings,
                            userProfile = userProfile,
                            playerState = playerState,
                            onStartRecording = { handleStartRecordingClick() },
                            onRecordingClick = { viewModel.selectRecording(it) },
                            onPlayPauseClick = { viewModel.playRecording(it) },
                            onToggleActionItem = { recId, actId, isCompleted ->
                                viewModel.toggleActionItem(recId, actId, isCompleted)
                            },
                            onNavigate = { viewModel.navigateTo(it) }
                        )
                    }
                    NavigationScreen.RECORDINGS -> {
                        RecordingsHistoryScreen(
                            recordings = filteredRecordings,
                            searchQuery = searchQuery,
                            selectedCategory = categoryFilter,
                            playerState = playerState,
                            onSearchChange = { viewModel.setSearchQuery(it) },
                            onCategoryChange = { viewModel.setCategoryFilter(it) },
                            onRecordingClick = { viewModel.selectRecording(it) },
                            onPlayPauseClick = { viewModel.playRecording(it) },
                            onRenameRecording = { recId, newTitle ->
                                viewModel.renameRecording(recId, newTitle)
                            },
                            onDeleteRecording = { viewModel.deleteRecording(it) }
                        )
                    }
                    NavigationScreen.DAILY_REPORT -> {
                        DailyReportScreen(
                            report = dailyReport,
                            selectedDate = selectedDateString,
                            userProfile = userProfile,
                            isLoading = isGeneratingReport,
                            onDateSelected = { viewModel.setDailyReportDate(it) },
                            onToggleActionItem = { recId, actId, isCompleted ->
                                viewModel.toggleActionItem(recId, actId, isCompleted)
                            },
                            onSendPushNotification = { viewModel.triggerDailyPushNotification() }
                        )
                    }
                    NavigationScreen.SETTINGS -> {
                        SettingsScreen(
                            userProfile = userProfile,
                            onUpdateProfile = { viewModel.updateUserProfile(it) }
                        )
                    }
                }
            }

            // Top In-App Notification Toast
            NotificationBanner(
                notification = activeNotification,
                onDismiss = { viewModel.dismissNotificationToast() },
                modifier = Modifier.align(Alignment.TopCenter)
            )

            // Recording Consent Dialog
            if (showConsentDialog) {
                RecordingConsentDialog(
                    onConfirm = { title, category, language ->
                        viewModel.confirmConsentAndStart(title, category, language)
                    },
                    onDismiss = { viewModel.dismissConsentDialog() }
                )
            }

            // Active Recording HUD Bottom Sheet
            if (showRecordingSheet) {
                RecordingHudSheet(
                    session = sessionState,
                    title = recordingTitle,
                    category = recordingCategory,
                    language = recordingLanguage,
                    onPauseResume = { viewModel.togglePauseRecording() },
                    onStopAndSave = { viewModel.stopAndSaveRecording() },
                    onCancel = { viewModel.cancelRecording() }
                )
            }
        }
    }
}
