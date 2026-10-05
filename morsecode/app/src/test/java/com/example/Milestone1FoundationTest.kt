package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.core.di.DefaultAppContainer
import com.example.core.model.Peer
import com.example.core.model.TransferItem
import com.example.core.model.TransferKind
import com.example.core.model.TransferState
import com.example.core.preferences.PreferencesManager
import com.example.ui.components.AppDestination
import com.example.ui.theme.AccentColor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class Milestone1FoundationTest {

    @Test
    fun testAccentColorsFromMockup() {
        assertEquals(AccentColor.SUNFLOWER, AccentColor.fromId("sunflower"))
        assertEquals(AccentColor.LEAF, AccentColor.fromId("leaf"))
        assertEquals(AccentColor.EMBER, AccentColor.fromId("ember"))
        assertEquals(AccentColor.VIOLET, AccentColor.fromId("violet"))
        assertEquals(AccentColor.SKY, AccentColor.fromId("sky"))
        assertEquals(AccentColor.SUNFLOWER, AccentColor.fromId("unknown_fallback"))
    }

    @Test
    fun testAppContainerAndDatabaseInitialization() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val container = DefaultAppContainer(context)
        assertNotNull(container.database)
        assertNotNull(container.preferencesManager)
        assertNotNull(container.storageManager)
    }

    @Test
    fun testPreferencesKeysDefined() {
        assertNotNull(PreferencesManager.KEY_DARK_THEME)
        assertNotNull(PreferencesManager.KEY_ACCENT)
        assertNotNull(PreferencesManager.KEY_CONFLICT_POLICY)
        assertNotNull(PreferencesManager.KEY_DEVICE_NAME)
    }

    @Test
    fun testNavigationDestinations() {
        val dests = AppDestination.entries
        assertEquals(4, dests.size)
        assertTrue(dests.any { it.route == "connect" })
        assertTrue(dests.any { it.route == "files" })
        assertTrue(dests.any { it.route == "history" })
        assertTrue(dests.any { it.route == "settings" })
    }

    @Test
    fun testDomainModelsAndInvariants() {
        val peer = Peer(
            id = "r",
            name = "Ravi's Redmi",
            letter = "R",
            badge = "LAN"
        )
        assertEquals("R", peer.letter)
        assertEquals("LAN", peer.badge)

        val item = TransferItem(
            id = "t1",
            name = "holiday_2019.mp4",
            kind = TransferKind.VIDEO,
            size = 144_000_000L,
            sent = 0L,
            state = TransferState.QUEUED
        )
        assertEquals(TransferState.QUEUED, item.state)
        assertEquals(0f, item.progress, 0.001f)
    }
}
