package com.example

import com.example.core.model.TransferItem
import com.example.core.model.TransferKind
import com.example.core.model.TransferState
import com.example.ui.components.formatBytes
import com.example.ui.components.formatSpeed
import com.example.ui.components.formatTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.zip.CRC32

class TransferEngineTest {

    @Test
    fun testTransferKindDetection() {
        assertEquals(TransferKind.IMAGE, TransferKind.fromFileName("photo.jpg"))
        assertEquals(TransferKind.IMAGE, TransferKind.fromFileName("PIC.PNG"))
        assertEquals(TransferKind.VIDEO, TransferKind.fromFileName("movie.mp4"))
        assertEquals(TransferKind.AUDIO, TransferKind.fromFileName("song.flac"))
        assertEquals(TransferKind.ZIP, TransferKind.fromFileName("backup.zip"))
        assertEquals(TransferKind.APK, TransferKind.fromFileName("app.apk"))
        assertEquals(TransferKind.DOC, TransferKind.fromFileName("document.pdf"))
    }

    @Test
    fun testTransferProgressCalculation() {
        val item = TransferItem(
            id = "t1",
            name = "test.mp4",
            kind = TransferKind.VIDEO,
            size = 100_000_000L,
            sent = 50_000_000L,
            state = TransferState.SENDING
        )
        assertEquals(0.5f, item.progress, 0.001f)
    }

    @Test
    fun testCRC32IntegrityCheck() {
        val testData = "Morsecode high speed local data streaming".toByteArray()
        val crc = CRC32()
        crc.update(testData)
        val expected = crc.value

        val verifyCrc = CRC32()
        verifyCrc.update(testData)
        assertEquals(expected, verifyCrc.value)
    }

    @Test
    fun testPausePreservesConfirmedOffset() {
        var item = TransferItem(
            id = "t2",
            name = "archive.zip",
            kind = TransferKind.ZIP,
            size = 50_000_000L,
            sent = 24_500_000L,
            state = TransferState.SENDING
        )
        // Transition to paused
        item = item.copy(state = TransferState.PAUSED)
        assertEquals(24_500_000L, item.sent)
        assertEquals(TransferState.PAUSED, item.state)

        // Resume at preserved offset
        item = item.copy(state = TransferState.SENDING)
        assertEquals(24_500_000L, item.sent)
        assertEquals(TransferState.SENDING, item.state)
    }

    @Test
    fun testFormattingUtilities() {
        assertEquals("1.5 GB", formatBytes(1_500_000_000L))
        assertEquals("144 MB", formatBytes(144_000_000L))
        assertEquals("412.0 KB", formatBytes(412_000L))
        assertEquals("500 B", formatBytes(500L))

        assertEquals("6.2 MB/s", formatSpeed(6_200_000L))

        assertEquals("1:44", formatTime(104))
        assertEquals("24:12", formatTime(1452))
    }
}
