package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.core.media.Media3PlayerManager
import com.example.core.model.TransferKind
import com.example.core.storage.MediaFile
import com.example.core.storage.StorageManager
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class Milestone3MediaAndStorageTest {

    private lateinit var context: Context
    private lateinit var storageManager: StorageManager
    private lateinit var playerManager: Media3PlayerManager

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext<MorsecodeApp>()
        storageManager = StorageManager(context)
        playerManager = Media3PlayerManager(context)
    }

    @Test
    fun testStorageManagerQueries() = runBlocking {
        val photos = storageManager.queryPhotos()
        assertTrue(photos.isNotEmpty())
        assertEquals(TransferKind.IMAGE, photos.first().kind)

        val videos = storageManager.queryVideos()
        assertTrue(videos.isNotEmpty())
        assertEquals(TransferKind.VIDEO, videos.first().kind)

        val music = storageManager.queryMusic()
        assertTrue(music.isNotEmpty())
        assertEquals(TransferKind.AUDIO, music.first().kind)

        val docs = storageManager.queryDocuments()
        assertTrue(docs.isNotEmpty())

        val apps = storageManager.queryInstalledApps()
        assertTrue(apps.isNotEmpty())
    }

    @Test
    fun testPlayerManagerMusicSeekingAndVolume() {
        // Initial state
        assertEquals(0.7f, playerManager.musicState.value.volume, 0.01f)
        assertFalse(playerManager.musicState.value.isMuted)

        // Volume adjustment
        playerManager.setMusicVolume(0.4f)
        assertEquals(0.4f, playerManager.musicState.value.volume, 0.01f)

        // Mute toggle
        playerManager.toggleMusicMute()
        assertTrue(playerManager.musicState.value.isMuted)

        // Unmute restores volume
        playerManager.toggleMusicMute()
        assertFalse(playerManager.musicState.value.isMuted)
        assertEquals(0.4f, playerManager.musicState.value.volume, 0.01f)
    }

    @Test
    fun testPlayerManagerVideoControlsAndRelativeSkip() {
        // Initial video state
        assertEquals(0.7f, playerManager.videoState.value.volume, 0.01f)
        assertFalse(playerManager.videoState.value.isMuted)

        // Relative skip ±10 seconds
        playerManager.seekVideoRelative(10000L)
        // With duration 0 in test environment, coerced safely to 0
        assertEquals(0L, playerManager.videoState.value.currentPositionMs)

        // Video Volume adjustment
        playerManager.setVideoVolume(0.9f)
        assertEquals(0.9f, playerManager.videoState.value.volume, 0.01f)

        playerManager.toggleVideoMute()
        assertTrue(playerManager.videoState.value.isMuted)
    }

    @Test
    fun testMediaFileModelFormatting() {
        val file = MediaFile(
            id = "f1",
            name = "test_video.mp4",
            path = "/storage/emulated/0/Movies/test_video.mp4",
            uri = null,
            size = 45_000_000L,
            dateModified = System.currentTimeMillis(),
            kind = TransferKind.VIDEO,
            durationMs = 125_000L
        )
        assertEquals("2:05", file.durationFormatted)
    }
}
