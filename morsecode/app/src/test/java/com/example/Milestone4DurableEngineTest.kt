package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.core.database.MorsecodeDatabase
import com.example.core.database.TransferQueueEntity
import com.example.core.transfer.ChunkHeader
import com.example.core.transfer.ChunkPacket
import com.example.core.transfer.ConflictPolicy
import com.example.core.transfer.DurableTransferEngine
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream
import java.io.File
import java.util.Random
import java.util.UUID
import java.util.zip.CRC32

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class Milestone4DurableEngineTest {

    private lateinit var context: Context
    private lateinit var database: MorsecodeDatabase
    private lateinit var engine: DurableTransferEngine
    private lateinit var testDir: File

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext<MorsecodeApp>()
        database = (context as MorsecodeApp).database
        testDir = File(context.cacheDir, "engine_tests_${System.currentTimeMillis()}")
        testDir.mkdirs()
        engine = DurableTransferEngine(context, database, testDir)
    }

    @Test
    fun testChunkCrcVerificationAndControlledFailure() {
        val payload = "Hello Morsecode Chunk Verification".toByteArray(Charsets.UTF_8)
        val validCrc = CRC32().apply { update(payload) }.value

        val validPacket = ChunkPacket(
            header = ChunkHeader(
                transferId = "t1",
                seqNumber = 0L,
                offset = 0L,
                length = payload.size,
                chunkCrc32 = validCrc
            ),
            payload = payload
        )
        assertTrue("Valid chunk CRC must pass", validPacket.verifyCrc())

        val corruptedPacket = ChunkPacket(
            header = ChunkHeader(
                transferId = "t1",
                seqNumber = 0L,
                offset = 0L,
                length = payload.size,
                chunkCrc32 = validCrc xor 0xFF // Corrupted CRC
            ),
            payload = payload
        )
        assertFalse("Corrupted chunk CRC must fail verification", corruptedPacket.verifyCrc())
    }

    @Test
    fun testFullStreamedTransferThroughLocalStreams() = runBlocking {
        val dataSize = 256 * 1024 // 256 KB
        val randomBytes = ByteArray(dataSize).apply { Random(42).nextBytes(this) }
        val stream = ByteArrayInputStream(randomBytes)

        val transferId = UUID.randomUUID().toString()
        val targetFile = File(testDir, "streamed_target.bin")
        val partFile = File(testDir, "streamed_target.bin.part")

        // Prepare queue
        engine.prepareInboundTransfer(
            transferId = transferId,
            fileName = targetFile.name,
            fileSize = dataSize.toLong(),
            peerId = "peer1",
            peerName = "Redmi Note"
        )

        var bytesReported = 0L
        val success = engine.executeStreamedTransfer(
            transferId = transferId,
            inputStream = stream,
            totalBytes = dataSize.toLong(),
            targetFile = targetFile,
            partFile = partFile,
            startOffset = 0L
        ) { sent, _ ->
            bytesReported = sent
        }

        assertTrue("Streamed transfer must succeed", success)
        assertTrue("Final target file must exist", targetFile.exists())
        assertFalse("Partial file must be renamed", partFile.exists())
        assertEquals(dataSize.toLong(), targetFile.length())
        assertEquals(dataSize.toLong(), bytesReported)

        // Verify target file byte-for-byte matches source
        val resultBytes = targetFile.readBytes()
        assertTrue("Output bytes must match source data exactly", randomBytes.contentEquals(resultBytes))

        // Verify state is marked DONE in database
        val record = database.transferQueueDao().getQueueItem(transferId)
        assertEquals("DONE", record?.state)
    }

    @Test
    fun testPauseAndResumeStreamedTransfer() = runBlocking {
        val totalSize = 128 * 1024 // 128 KB
        val sourceBytes = ByteArray(totalSize).apply { Random(123).nextBytes(this) }

        val transferId = UUID.randomUUID().toString()
        val targetFile = File(testDir, "resumable.bin")
        val partFile = File(testDir, "resumable.bin.part")

        engine.prepareInboundTransfer(
            transferId = transferId,
            fileName = targetFile.name,
            fileSize = totalSize.toLong(),
            peerId = "peer_resume",
            peerName = "Pixel 7"
        )

        // Step 1: Stream first half (64 KB)
        val firstHalfStream = ByteArrayInputStream(sourceBytes, 0, 64 * 1024)
        engine.executeStreamedTransfer(
            transferId = transferId,
            inputStream = firstHalfStream,
            totalBytes = 64 * 1024L,
            targetFile = targetFile,
            partFile = partFile,
            startOffset = 0L
        )

        assertTrue(partFile.exists())
        assertEquals(64 * 1024L, partFile.length())

        // Pause
        engine.pauseTransfer(transferId)
        val pausedRecord = database.transferQueueDao().getQueueItem(transferId)
        assertEquals("PAUSED", pausedRecord?.state)

        // Step 2: Resume from 64 KB and stream remaining 64 KB
        engine.resumeTransfer(transferId)
        val resumedRecord = database.transferQueueDao().getQueueItem(transferId)
        assertEquals("RECEIVING", resumedRecord?.state)

        val secondHalfStream = ByteArrayInputStream(sourceBytes, 64 * 1024, 64 * 1024)
        val complete = engine.executeStreamedTransfer(
            transferId = transferId,
            inputStream = secondHalfStream,
            totalBytes = totalSize.toLong(),
            targetFile = targetFile,
            partFile = partFile,
            startOffset = 64 * 1024L
        )

        assertTrue("Transfer must complete upon resumption", complete)
        assertTrue(targetFile.exists())
        assertEquals(totalSize.toLong(), targetFile.length())
        assertTrue("Full data matches after pause-resume", sourceBytes.contentEquals(targetFile.readBytes()))
    }

    @Test
    fun testCancellationDeletesPartialFile() = runBlocking {
        val transferId = UUID.randomUUID().toString()
        val targetFile = File(testDir, "cancelled.bin")
        val partFile = File(testDir, "cancelled.bin.part")

        engine.prepareInboundTransfer(
            transferId = transferId,
            fileName = targetFile.name,
            fileSize = 50000L,
            peerId = "p",
            peerName = "Peer"
        )

        partFile.writeBytes(ByteArray(10000))
        assertTrue(partFile.exists())

        engine.cancelTransfer(transferId)

        val record = database.transferQueueDao().getQueueItem(transferId)
        assertEquals("CANCELLED", record?.state)
        assertFalse("Partial file must be deleted upon cancellation", partFile.exists())
    }

    @Test
    fun testDuplicateConflictPolicies() {
        val existing = File(testDir, "sample.txt")
        existing.writeText("existing original content")

        // RENAME policy
        val renameTarget = engine.resolveConflict(testDir, "sample.txt", ConflictPolicy.RENAME)
        assertEquals("sample (1).txt", renameTarget.name)

        renameTarget.writeText("sample 1 content")
        val renameTarget2 = engine.resolveConflict(testDir, "sample.txt", ConflictPolicy.RENAME)
        assertEquals("sample (2).txt", renameTarget2.name)

        // OVERWRITE policy
        val overwriteTarget = engine.resolveConflict(testDir, "sample.txt", ConflictPolicy.OVERWRITE)
        assertEquals("sample.txt", overwriteTarget.name)
    }

    @Test
    fun testProcessRecoveryPreservesConfirmedOffset() = runBlocking {
        val transferId = "crash_recovery_transfer"
        val partFile = File(testDir, "recovering.bin.part")
        partFile.writeBytes(ByteArray(32768)) // 32 KB confirmed on disk

        // Simulate a transfer that was actively RECEIVING when process died
        database.transferQueueDao().insertOrUpdate(
            TransferQueueEntity(
                id = transferId,
                sessionId = "s1",
                fileName = "recovering.bin",
                sourceUri = null,
                fileSize = 100000L,
                bytesTransferred = 32768L,
                confirmedOffset = 32768L,
                state = "RECEIVING",
                isIncoming = true,
                mimeType = "application/octet-stream",
                targetPath = File(testDir, "recovering.bin").absolutePath,
                partialFilePath = partFile.absolutePath
            )
        )

        // Simulate app reboot recovery
        engine.recoverQueue()

        val recovered = database.transferQueueDao().getQueueItem(transferId)
        assertEquals("PAUSED", recovered?.state)
        assertEquals(32768L, recovered?.confirmedOffset)
        assertEquals(32768L, recovered?.bytesTransferred)
    }
}
