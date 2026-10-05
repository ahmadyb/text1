package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.core.storage.StorageManager
import com.example.network.server.WebShareServer
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.ServerSocket
import java.net.URL
import java.util.zip.ZipInputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [23]) // Testing specifically on Android API 23 (Marshmallow)
class Milestone6WebShareIntegrationTest {

    private lateinit var context: Context
    private lateinit var storageManager: StorageManager
    private lateinit var webShareServer: WebShareServer
    private lateinit var testFolder: File
    private var port: Int = 0

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext<MorsecodeApp>()
        storageManager = StorageManager(context)
        port = ServerSocket(0).use { it.localPort }
        webShareServer = WebShareServer(context, storageManager, port)
        testFolder = File(context.cacheDir, "webshare_test_${System.currentTimeMillis()}").apply { mkdirs() }
        webShareServer.startServer(port)
    }

    @After
    fun tearDown() {
        webShareServer.stopServer()
        testFolder.deleteRecursively()
    }

    @Test
    fun testBrowserSessionApprovalFlowAndRevocationOnApi23() = runBlocking {
        // 1. Request new session token
        val reqUrl = URL("http://127.0.0.1:$port/api/v1/auth/request")
        val reqConn = (reqUrl.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
        }
        assertEquals(200, reqConn.responseCode)
        val reqBody = reqConn.inputStream.bufferedReader().readText()
        val token = JSONObject(reqBody).getString("token")
        assertNotNull(token)

        // 2. Unapproved session accessing /api/v1/storage must be rejected with 401
        val storageUrl = URL("http://127.0.0.1:$port/api/v1/storage")
        val unapprovedConn = (storageUrl.openConnection() as HttpURLConnection).apply {
            setRequestProperty("x-session-token", token)
        }
        assertEquals(401, unapprovedConn.responseCode)

        // 3. Status endpoint shows pending
        val statusUrl = URL("http://127.0.0.1:$port/api/v1/auth/status?token=$token")
        val statusConn = statusUrl.openConnection() as HttpURLConnection
        assertEquals(200, statusConn.responseCode)
        val statusBody = JSONObject(statusConn.inputStream.bufferedReader().readText())
        assertFalse(statusBody.getBoolean("isApproved"))

        // 4. Phone user approves session
        webShareServer.approveSession(token)

        // 5. Approved session now accesses storage API successfully
        val approvedConn = (storageUrl.openConnection() as HttpURLConnection).apply {
            setRequestProperty("x-session-token", token)
        }
        assertEquals(200, approvedConn.responseCode)
        val approvedJson = JSONObject(approvedConn.inputStream.bufferedReader().readText())
        assertTrue(approvedJson.has("totalBytes"))
        assertTrue(approvedJson.has("freeBytes"))

        // 6. Session revocation immediately terminates access
        webShareServer.revokeSession(token)
        val revokedConn = (storageUrl.openConnection() as HttpURLConnection).apply {
            setRequestProperty("x-session-token", token)
        }
        assertEquals(401, revokedConn.responseCode)
    }

    @Test
    fun testHttp206ByteRangeStreamingAndSeeking() = runBlocking {
        // Prepare approved session
        val token = "test_stream_token"
        webShareServer.sessions[token] = com.example.network.server.BrowserSession(
            token = token,
            clientIp = "127.0.0.1",
            userAgent = "TestBrowser",
            isApproved = true
        )

        // Create a 10KB test file
        val sampleData = ByteArray(10 * 1024) { (it % 256).toByte() }
        val testFile = File(context.cacheDir, "video_stream_sample.mp4").apply {
            writeBytes(sampleData)
        }

        // Test Bounded Range: bytes=100-499 (400 bytes)
        val url = URL("http://127.0.0.1:$port/api/v1/download?path=${testFile.absolutePath}&token=$token")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            setRequestProperty("Range", "bytes=100-499")
        }

        assertEquals(206, conn.responseCode)
        assertEquals("bytes 100-499/10240", conn.getHeaderField("Content-Range"))
        assertEquals("400", conn.getHeaderField("Content-Length"))

        val readBytes = conn.inputStream.readBytes()
        assertEquals(400, readBytes.size)
        // Verify byte values match
        for (i in 0 until 400) {
            assertEquals(sampleData[100 + i], readBytes[i])
        }

        // Test Open-ended Range: bytes=10000- (last 240 bytes)
        val connOpen = (url.openConnection() as HttpURLConnection).apply {
            setRequestProperty("Range", "bytes=10000-")
        }
        assertEquals(206, connOpen.responseCode)
        assertEquals("bytes 10000-10239/10240", connOpen.getHeaderField("Content-Range"))
        assertEquals(240, connOpen.inputStream.readBytes().size)
    }

    @Test
    fun testFolderZipStreamingDirectlyToClient() = runBlocking {
        val token = "zip_token"
        webShareServer.sessions[token] = com.example.network.server.BrowserSession(
            token = token,
            clientIp = "127.0.0.1",
            userAgent = "ZipBrowser",
            isApproved = true
        )

        // Populate folder with files
        val file1 = File(testFolder, "doc1.txt").apply { writeText("Hello World 1") }
        val file2 = File(testFolder, "doc2.txt").apply { writeText("Hello World 2") }

        val zipUrl = URL("http://127.0.0.1:$port/api/v1/zip?path=${testFolder.absolutePath}&token=$token")
        val conn = zipUrl.openConnection() as HttpURLConnection
        assertEquals(200, conn.responseCode)
        assertEquals("application/zip", conn.contentType)

        val zipStream = ZipInputStream(conn.inputStream)
        val entryNames = mutableListOf<String>()
        var entry = zipStream.nextEntry
        while (entry != null) {
            entryNames.add(File(entry.name).name)
            zipStream.closeEntry()
            entry = zipStream.nextEntry
        }

        assertTrue("ZIP stream must contain doc1.txt", entryNames.contains("doc1.txt"))
        assertTrue("ZIP stream must contain doc2.txt", entryNames.contains("doc2.txt"))
    }

    @Test
    fun testSecurityAndPathTraversalDefense() = runBlocking {
        val token = "secure_token"
        webShareServer.sessions[token] = com.example.network.server.BrowserSession(
            token = token,
            clientIp = "127.0.0.1",
            userAgent = "SecBrowser",
            isApproved = true
        )

        // Attempt directory traversal in /api/v1/files
        val attackFilesUrl = URL("http://127.0.0.1:$port/api/v1/files?path=../../../../etc&token=$token")
        val connFiles = attackFilesUrl.openConnection() as HttpURLConnection
        assertEquals(403, connFiles.responseCode)

        // Attempt reading /etc/passwd or arbitrary file via /api/v1/download
        val attackDownloadUrl = URL("http://127.0.0.1:$port/api/v1/download?path=/etc/passwd&token=$token")
        val connDownload = attackDownloadUrl.openConnection() as HttpURLConnection
        assertEquals(403, connDownload.responseCode)
    }
}
