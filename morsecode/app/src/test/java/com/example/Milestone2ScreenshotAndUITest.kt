package com.example

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.example.ui.connect.ConnectHubScreen
import com.example.ui.connect.ConnectionDoctorScreen
import com.example.ui.connect.ConsentScreen
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
import com.example.ui.settings.WebShareControlScreen
import com.example.ui.theme.MorsecodeTheme
import com.example.ui.transfer.BroadcastCompleteScreen
import com.example.ui.transfer.BroadcastReceiversScreen
import com.example.ui.transfer.BroadcastSenderScreen
import com.example.ui.transfer.ReceiveScreen
import com.example.ui.transfer.ReceivingScreen
import com.example.ui.transfer.SendingScreen
import com.example.viewmodel.MorsecodeViewModel
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h740dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class Milestone2ScreenshotAndUITest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var viewModel: MorsecodeViewModel

    @Before
    fun setup() {
        val app = ApplicationProvider.getApplicationContext<MorsecodeApp>()
        viewModel = MorsecodeViewModel(app)
    }

    @Test
    fun testOnboardingScreenAt360x740() {
        composeTestRule.setContent {
            MorsecodeTheme {
                Box(modifier = Modifier.size(360.dp, 740.dp)) {
                    OnboardingScreen(viewModel = viewModel)
                }
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Send files without the internet").assertIsDisplayed()
        composeTestRule.onNodeWithText("Continue").assertIsDisplayed()
    }

    @Test
    fun testConnectHubScreenAt360x740() {
        composeTestRule.setContent {
            MorsecodeTheme {
                Box(modifier = Modifier.size(360.dp, 740.dp)) {
                    ConnectHubScreen(viewModel = viewModel)
                }
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Connect").assertIsDisplayed()
        composeTestRule.onNodeWithText("↑ Send").assertIsDisplayed()
        composeTestRule.onNodeWithText("↓ Receive").assertIsDisplayed()
        composeTestRule.onNodeWithText("⇶ Broadcast to several phones").assertIsDisplayed()
        composeTestRule.onNodeWithText("WebShare · PC").assertIsDisplayed()
    }

    @Test
    fun testDiscoverScreenSingleAndMultiAt360x740() {
        composeTestRule.setContent {
            MorsecodeTheme {
                Box(modifier = Modifier.size(360.dp, 740.dp)) {
                    DiscoverScreen(viewModel = viewModel, isMultiSelect = false)
                }
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Send files").assertIsDisplayed()
        composeTestRule.onNodeWithText("LAN or Nearby").assertIsDisplayed()
    }

    @Test
    fun testConsentDialogsPeerAndBrowserAt360x740() {
        composeTestRule.setContent {
            MorsecodeTheme {
                Box(modifier = Modifier.size(360.dp, 740.dp)) {
                    ConsentScreen(viewModel = viewModel, isBrowserConsent = false)
                }
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("consent_peer_card").assertIsDisplayed()
        composeTestRule.onNodeWithTag("consent_reject_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("consent_accept_button").assertIsDisplayed()
    }

    @Test
    fun testFilesCategoryScreenTabsAt360x740() {
        composeTestRule.setContent {
            MorsecodeTheme {
                Box(modifier = Modifier.size(360.dp, 740.dp)) {
                    FilesCategoryScreen(viewModel = viewModel)
                }
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onAllNodes(androidx.compose.ui.test.hasText("Files"))[0].assertIsDisplayed()
        composeTestRule.onNodeWithText("Photos").assertIsDisplayed()
        composeTestRule.onNodeWithText("Videos").assertIsDisplayed()
        composeTestRule.onNodeWithText("Music").assertIsDisplayed()
        composeTestRule.onNodeWithText("Apps").assertIsDisplayed()
    }

    @Test
    fun testBrowseStorageScreenAt360x740() {
        composeTestRule.setContent {
            MorsecodeTheme {
                Box(modifier = Modifier.size(360.dp, 740.dp)) {
                    BrowseStorageScreen(viewModel = viewModel)
                }
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Internal storage").assertIsDisplayed()
        composeTestRule.onNodeWithText("DCIM").assertIsDisplayed()
        composeTestRule.onNodeWithText("Download").assertIsDisplayed()
    }

    @Test
    fun testMediaPlayersAt360x740() {
        composeTestRule.setContent {
            MorsecodeTheme {
                Box(modifier = Modifier.size(360.dp, 740.dp)) {
                    MusicPlayerScreen(viewModel = viewModel)
                }
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("NOW PLAYING").assertIsDisplayed()
        composeTestRule.onNodeWithText("Ocean Eyes (Live)").assertIsDisplayed()
    }

    @Test
    fun testVideoPlayerScreenAt360x740() {
        composeTestRule.setContent {
            MorsecodeTheme {
                Box(modifier = Modifier.size(360.dp, 740.dp)) {
                    VideoPlayerScreen(viewModel = viewModel)
                }
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("holiday_2019.mp4").assertIsDisplayed()
    }

    @Test
    fun testPhotoViewerScreenAt360x740() {
        composeTestRule.setContent {
            MorsecodeTheme {
                Box(modifier = Modifier.size(360.dp, 740.dp)) {
                    PhotoViewerScreen(viewModel = viewModel)
                }
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("‹ swipe to browse ›").assertIsDisplayed()
    }

    @Test
    fun testSendingAndReceivingDuplexAt360x740() {
        composeTestRule.setContent {
            MorsecodeTheme {
                Box(modifier = Modifier.size(360.dp, 740.dp)) {
                    SendingScreen(viewModel = viewModel)
                }
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Sending + receiving").assertIsDisplayed()
        composeTestRule.onNodeWithText("Add files").assertIsDisplayed()
        composeTestRule.onNodeWithText("Background").assertIsDisplayed()
        composeTestRule.onNodeWithText("End").assertIsDisplayed()
    }

    @Test
    fun testBroadcastScreensAt360x740() {
        composeTestRule.setContent {
            MorsecodeTheme {
                Box(modifier = Modifier.size(360.dp, 740.dp)) {
                    BroadcastSenderScreen(viewModel = viewModel)
                }
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Broadcasting").assertIsDisplayed()
        composeTestRule.onNodeWithText("See completion screens").assertIsDisplayed()
    }

    @Test
    fun testHistoryScreenAt360x740() {
        composeTestRule.setContent {
            MorsecodeTheme {
                Box(modifier = Modifier.size(360.dp, 740.dp)) {
                    HistoryScreen(viewModel = viewModel)
                }
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("History").assertIsDisplayed()
        composeTestRule.onNodeWithText("Received").assertIsDisplayed()
        composeTestRule.onNodeWithText("Sent").assertIsDisplayed()
    }

    @Test
    fun testSettingsAndDiagnosticsAt360x740() {
        composeTestRule.setContent {
            MorsecodeTheme {
                Box(modifier = Modifier.size(360.dp, 740.dp)) {
                    SettingsScreen(viewModel = viewModel)
                }
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Settings").assertIsDisplayed()
        composeTestRule.onNodeWithText("MYA-L10").assertIsDisplayed()
        composeTestRule.onNodeWithText("Connection Doctor").assertExists()
        composeTestRule.onNodeWithText("Crash reports").assertExists()
    }

    @Test
    fun testConnectionDoctorScreenAt360x740() {
        composeTestRule.setContent {
            MorsecodeTheme {
                Box(modifier = Modifier.size(360.dp, 740.dp)) {
                    ConnectionDoctorScreen(viewModel = viewModel)
                }
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Connection Doctor").assertIsDisplayed()
        composeTestRule.onNodeWithText("Wi-Fi connected").assertIsDisplayed()
        composeTestRule.onNodeWithText("Request battery exemption").assertIsDisplayed()
    }
}
