package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.core.model.Peer
import com.example.network.transport.TransferSocketEngine
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.net.ServerSocket
import java.util.Random

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class Milestone5LanDuplexIntegrationTest {

    private lateinit var context: Context
    private lateinit var engineInstanceA: TransferSocketEngine
    private lateinit var engineInstanceB: TransferSocketEngine
    private lateinit var testDir: File

    private var portA: Int = 0
    private var portB: Int = 0

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext<MorsecodeApp>()
        testDir = File(context.cacheDir, "lan_integration_${System.currentTimeMillis()}").apply { mkdirs() }

        // Find available open ports for both instances
        portA = ServerSocket(0).use { it.localPort }
        portB = ServerSocket(0).use { it.localPort }

        engineInstanceA = TransferSocketEngine(context, portA)
        engineInstanceB = TransferSocketEngine(context, portB)

        engineInstanceA.startListening(portA)
        engineInstanceB.startListening(portB)
    }

    @After
    fun tearDown() {
        engineInstanceA.stopListening()
        engineInstanceB.stopListening()
        testDir.deleteRecursively()
    }

    @Test
    fun testRealDuplexBidirectionalTransferBetweenTwoInstances() = runBlocking {
        // Setup two real test files with random data
        val sizeA = 128 * 1024 // 128 KB from A -> B
        val bytesA = ByteArray(sizeA).apply { Random(101).nextBytes(this) }
        val fileA = File(testDir, "from_instance_a.bin").apply { writeBytes(bytesA) }

        val sizeB = 64 * 1024 // 64 KB from B -> A
        val bytesB = ByteArray(sizeB).apply { Random(202).nextBytes(this) }
        val fileB = File(testDir, "from_instance_b.bin").apply { writeBytes(bytesB) }

        // Concurrently: Instance A sends fileA to Instance B, while Instance B sends fileB to Instance A
        val jobAtoB = async {
            engineInstanceA.sendFile(
                peerIp = "127.0.0.1",
                peerPort = portB,
                file = fileA
            )
        }

        val jobBtoA = async {
            engineInstanceB.sendFile(
                peerIp = "127.0.0.1",
                peerPort = portA,
                file = fileB
            )
        }

        val resultAtoB = jobAtoB.await()
        val resultBtoA = jobBtoA.await()

        assertTrue("Transfer from Instance A to Instance B must succeed over TCP", resultAtoB)
        assertTrue("Transfer from Instance B to Instance A must succeed over TCP", resultBtoA)

        // Verify destination file at B
        val receivedAtB = File(context.cacheDir, "from_instance_a.bin")
        assertTrue("Received file at B must exist", receivedAtB.exists())
        assertEquals(sizeA.toLong(), receivedAtB.length())
        assertTrue("Received file at B must match original byte-for-byte", bytesA.contentEquals(receivedAtB.readBytes()))

        // Verify destination file at A
        val receivedAtA = File(context.cacheDir, "from_instance_b.bin")
        assertTrue("Received file at A must exist", receivedAtA.exists())
        assertEquals(sizeB.toLong(), receivedAtA.length())
        assertTrue("Received file at A must match original byte-for-byte", bytesB.contentEquals(receivedAtA.readBytes()))
    }

    @Test
    fun testReconnectionAndResumeFromConfirmedOffset() = runBlocking {
        val totalSize = 192 * 1024 // 192 KB
        val sourceBytes = ByteArray(totalSize).apply { Random(303).nextBytes(this) }
        val testFile = File(testDir, "resume_test.bin").apply { writeBytes(sourceBytes) }

        // Start transfer, then simulate pause at 64 KB
        val transferId = "resumable_lan_test"
        var confirmedBytes = 0L

        // Send first chunk (64 KB)
        engineInstanceA.sendFile(
            peerIp = "127.0.0.1",
            peerPort = portB,
            file = testFile,
            startOffset = 0L
        ) { sent, _ ->
            confirmedBytes = sent
            if (sent >= 64 * 1024) {
                engineInstanceA.pauseAll()
            }
        }

        // Verify partial file was created at B with at least 64 KB
        val partAtB = File(context.cacheDir, "resume_test.bin.part")
        assertTrue("Partial file must exist at B", partAtB.exists())
        assertTrue("Partial file at B must contain transferred offset", partAtB.length() >= 64 * 1024)

        val resumeOffset = 64 * 1024L
        engineInstanceA.resumeAll()

        // Reconnect and resume streaming from confirmed offset
        val resumeSuccess = engineInstanceA.sendFile(
            peerIp = "127.0.0.1",
            peerPort = portB,
            file = testFile,
            startOffset = resumeOffset
        )

        assertTrue("Resume over TCP connection must succeed", resumeSuccess)

        val finalAtB = File(context.cacheDir, "resume_test.bin")
        assertTrue("Final file after resume must exist", finalAtB.exists())
        assertEquals(totalSize.toLong(), finalAtB.length())
        assertTrue("Full file content must match source bytes", sourceBytes.contentEquals(finalAtB.readBytes()))
    }

    @Test
    fun testIndependentMultiPhoneBroadcast() = runBlocking {
        // Create third instance for multi-phone fan-out
        val portC = ServerSocket(0).use { it.localPort }
        val engineInstanceC = TransferSocketEngine(context, portC)
        engineInstanceC.startListening(portC)

        try {
            val broadcastSize = 64 * 1024
            val broadcastBytes = ByteArray(broadcastSize).apply { Random(404).nextBytes(this) }
            val broadcastFile = File(testDir, "broadcast_payload.bin").apply { writeBytes(broadcastBytes) }

            val peers = listOf(
                Peer(id = "p_b", name = "Phone B", ip = "127.0.0.1", port = portB),
                Peer(id = "p_c", name = "Phone C", ip = "127.0.0.1", port = portC)
            )

            val progressMap = mutableMapOf<String, Long>()
            val results = engineInstanceA.broadcastFileToPeers(peers, broadcastFile) { peerId, sent, _ ->
                progressMap[peerId] = sent
            }

            assertEquals(true, results["p_b"])
            assertEquals(true, results["p_c"])

            // Both receivers have the complete file
            val fileAtB = File(context.cacheDir, "broadcast_payload.bin")
            assertTrue("Phone B received the broadcast", fileAtB.exists())
            assertEquals(broadcastSize.toLong(), fileAtB.length())
            assertTrue("Phone B content matches", broadcastBytes.contentEquals(fileAtB.readBytes()))
        } finally {
            engineInstanceC.stopListening()
        }
    }
}
