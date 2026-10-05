package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
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
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ui.components.AppDestination
import com.example.ui.components.MorsecodeBottomNav
import com.example.ui.connect.ConnectHubScreen
import com.example.ui.connect.ConnectionDoctorScreen
import com.example.ui.connect.DiscoverScreen
import com.example.ui.connect.HelpScreen
import com.example.ui.connect.OnboardingScreen
import com.example.ui.files.BrowseStorageScreen
import com.example.ui.files.FilesCategoryScreen
import com.example.ui.files.MusicPlayerScreen
import com.example.ui.files.PhotoViewerScreen
import com.example.ui.files.VideoPlayerScreen
import com.example.ui.settings.CrashesScreen
import com.example.ui.settings.HistoryScreen
import com.example.ui.settings.LogsScreen
import com.example.ui.settings.SettingsScreen
import com.example.ui.settings.ShareBottomSheet
import com.example.ui.settings.SortBottomSheet
import com.example.ui.settings.WebShareControlScreen
import com.example.ui.theme.LocalMorsecodeColors
import com.example.ui.theme.MorsecodeTheme
import com.example.ui.transfer.BroadcastCompleteScreen
import com.example.ui.transfer.BroadcastSenderScreen
import com.example.ui.transfer.ReceiveScreen
import com.example.ui.transfer.ReceivingScreen
import com.example.ui.transfer.SendingScreen
import com.example.viewmodel.MorsecodeViewModel
import com.example.viewmodel.ScreenState
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    private val viewModel: MorsecodeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val isDark by viewModel.isDarkTheme.collectAsState()
            val accent by viewModel.currentAccent.collectAsState()
            val currentScreen by viewModel.currentScreen.collectAsState()

            MorsecodeTheme(
                darkTheme = isDark,
                accent = accent
            ) {
                // Request runtime permissions politely on launch
                RequestPermissionsSafely()

                val colors = LocalMorsecodeColors.current
                val isSortOpen by viewModel.isSortSheetOpen.collectAsState()
                val isShareOpen by viewModel.isShareSheetOpen.collectAsState()
                val toastMsg by viewModel.toastMessage.collectAsState()

                // Determine active navigation item
                val activeNav = when (currentScreen) {
                    ScreenState.CONNECT, ScreenState.DISCOVER, ScreenState.DISCOVER_MULTI, ScreenState.RECEIVE, ScreenState.WEBSHARE -> AppDestination.CONNECT
                    ScreenState.FILES, ScreenState.BROWSE_STORAGE, ScreenState.SENDING, ScreenState.RECEIVING, ScreenState.BROADCAST_SENDER -> AppDestination.FILES
                    ScreenState.HISTORY -> AppDestination.HISTORY
                    ScreenState.SETTINGS, ScreenState.LOGS, ScreenState.CRASHES, ScreenState.DOCTOR, ScreenState.HELP -> AppDestination.SETTINGS
                    else -> AppDestination.CONNECT
                }

                val showBottomNav = currentScreen in listOf(
                    ScreenState.CONNECT,
                    ScreenState.FILES,
                    ScreenState.HISTORY,
                    ScreenState.SETTINGS
                )

                // Back navigation handling
                BackHandler(enabled = currentScreen != ScreenState.CONNECT) {
                    when (currentScreen) {
                        ScreenState.DISCOVER, ScreenState.DISCOVER_MULTI, ScreenState.RECEIVE, ScreenState.WEBSHARE -> viewModel.navigateTo(ScreenState.CONNECT)
                        ScreenState.SENDING, ScreenState.RECEIVING, ScreenState.BROADCAST_SENDER, ScreenState.BROADCAST_COMPLETE -> viewModel.navigateTo(ScreenState.CONNECT)
                        ScreenState.BROWSE_STORAGE, ScreenState.PHOTO_VIEWER, ScreenState.MUSIC_PLAYER, ScreenState.VIDEO_PLAYER -> viewModel.navigateTo(ScreenState.FILES)
                        ScreenState.LOGS, ScreenState.CRASHES, ScreenState.DOCTOR, ScreenState.HELP -> viewModel.navigateTo(ScreenState.SETTINGS)
                        ScreenState.ONBOARDING -> viewModel.navigateTo(ScreenState.CONNECT)
                        else -> viewModel.navigateTo(ScreenState.CONNECT)
                    }
                }

                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(colors.bg)
                        .safeDrawingPadding(),
                    bottomBar = {
                        if (showBottomNav) {
                            MorsecodeBottomNav(
                                currentDestination = activeNav,
                                onNavigate = { dest ->
                                    when (dest) {
                                        AppDestination.CONNECT -> viewModel.navigateTo(ScreenState.CONNECT)
                                        AppDestination.FILES -> viewModel.navigateTo(ScreenState.FILES)
                                        AppDestination.HISTORY -> viewModel.navigateTo(ScreenState.HISTORY)
                                        AppDestination.SETTINGS -> viewModel.navigateTo(ScreenState.SETTINGS)
                                    }
                                }
                            )
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        when (currentScreen) {
                            ScreenState.ONBOARDING -> OnboardingScreen(viewModel)
                            ScreenState.CONNECT -> ConnectHubScreen(viewModel)
                            ScreenState.DISCOVER -> DiscoverScreen(viewModel, isMultiSelect = false)
                            ScreenState.DISCOVER_MULTI -> DiscoverScreen(viewModel, isMultiSelect = true)
                            ScreenState.SENDING -> SendingScreen(viewModel)
                            ScreenState.RECEIVE -> ReceiveScreen(viewModel)
                            ScreenState.RECEIVING -> ReceivingScreen(viewModel)
                            ScreenState.BROADCAST_SENDER -> BroadcastSenderScreen(viewModel)
                            ScreenState.BROADCAST_RECEIVERS -> com.example.ui.transfer.BroadcastReceiversScreen(viewModel)
                            ScreenState.BROADCAST_COMPLETE -> BroadcastCompleteScreen(viewModel)
                            ScreenState.CONSENT_PEER -> com.example.ui.connect.ConsentScreen(viewModel, isBrowserConsent = false)
                            ScreenState.CONSENT_BROWSER -> com.example.ui.connect.ConsentScreen(viewModel, isBrowserConsent = true)
                            ScreenState.WEBSHARE -> WebShareControlScreen(viewModel)
                            ScreenState.DOCTOR -> ConnectionDoctorScreen(viewModel)
                            ScreenState.HELP -> HelpScreen(viewModel)
                            ScreenState.FILES -> FilesCategoryScreen(viewModel)
                            ScreenState.BROWSE_STORAGE -> BrowseStorageScreen(viewModel)
                            ScreenState.PHOTO_VIEWER -> PhotoViewerScreen(viewModel)
                            ScreenState.MUSIC_PLAYER -> MusicPlayerScreen(viewModel)
                            ScreenState.VIDEO_PLAYER -> VideoPlayerScreen(viewModel)
                            ScreenState.HISTORY -> HistoryScreen(viewModel)
                            ScreenState.SETTINGS -> SettingsScreen(viewModel)
                            ScreenState.LOGS -> LogsScreen(viewModel)
                            ScreenState.CRASHES -> CrashesScreen(viewModel)
                        }

                        // Floating Toast
                        AnimatedVisibility(
                            visible = toastMsg != null,
                            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = if (showBottomNav) 80.dp else 24.dp)
                        ) {
                            toastMsg?.let { msg ->
                                LaunchedEffect(msg) {
                                    delay(2000L)
                                    viewModel.clearToast()
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(colors.card)
                                        .border(1.dp, colors.line, RoundedCornerShape(12.dp))
                                        .padding(horizontal = 16.dp, vertical = 10.dp)
                                ) {
                                    Text(
                                        text = msg,
                                        color = colors.t1,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }

                        // Bottom Sheets
                        if (isSortOpen) {
                            SortBottomSheet(
                                viewModel = viewModel,
                                onDismiss = { viewModel.isSortSheetOpen.value = false }
                            )
                        }

                        if (isShareOpen) {
                            ShareBottomSheet(
                                viewModel = viewModel,
                                onDismiss = { viewModel.isShareSheetOpen.value = false }
                            )
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun RequestPermissionsSafely() {
        val permissionsLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestMultiplePermissions()
        ) { _ ->
            // Re-run diagnostics
            viewModel.runDoctorDiagnostics()
        }

        LaunchedEffect(Unit) {
            val permissions = mutableListOf<String>()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                permissions.add(Manifest.permission.POST_NOTIFICATIONS)
                permissions.add(Manifest.permission.READ_MEDIA_IMAGES)
                permissions.add(Manifest.permission.READ_MEDIA_VIDEO)
                permissions.add(Manifest.permission.READ_MEDIA_AUDIO)
                permissions.add(Manifest.permission.NEARBY_WIFI_DEVICES)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                permissions.add(Manifest.permission.BLUETOOTH_SCAN)
                permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
                permissions.add(Manifest.permission.BLUETOOTH_ADVERTISE)
                permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            } else {
                permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
                permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
            }

            val needed = permissions.filter {
                ContextCompat.checkSelfPermission(this@MainActivity, it) != PackageManager.PERMISSION_GRANTED
            }
            if (needed.isNotEmpty()) {
                permissionsLauncher.launch(needed.toTypedArray())
            }
        }
    }
}
