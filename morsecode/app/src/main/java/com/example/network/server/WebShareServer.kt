package com.example.network.server

import android.content.Context
import android.os.Environment
import android.os.StatFs
import com.example.core.model.TransferKind
import com.example.core.storage.MediaFile
import com.example.core.storage.StorageManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedOutputStream
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

data class BrowserSession(
    val token: String,
    val clientIp: String,
    val userAgent: String,
    val createdAt: Long = System.currentTimeMillis(),
    var isApproved: Boolean = false
)

class WebShareServer(
    private val context: Context,
    private val storageManager: StorageManager,
    val port: Int = 33455
) {
    private var serverSocket: ServerSocket? = null
    private var serverJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    val sessions = ConcurrentHashMap<String, BrowserSession>()
    var onSessionRequested: ((BrowserSession) -> Unit)? = null

    var isRunning = false
        private set

    private val baseStorageDir: File by lazy {
        Environment.getExternalStorageDirectory() ?: context.filesDir
    }

    fun getLocalIpAddress(): String {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val iface = interfaces.nextElement()
                if (iface.isLoopback || !iface.isUp) continue
                val addresses = iface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val addr = addresses.nextElement()
                    if (!addr.isLoopbackAddress && addr.hostAddress?.indexOf(':') == -1) {
                        return addr.hostAddress ?: "127.0.0.1"
                    }
                }
            }
        } catch (_: Exception) {
        }
        return "192.168.1.24"
    }

    fun startServer(customPort: Int = port) {
        if (isRunning) return
        try {
            serverSocket = ServerSocket(customPort, 50, InetAddress.getByName("0.0.0.0"))
            isRunning = true

            serverJob = scope.launch {
                while (isActive && isRunning) {
                    try {
                        val clientSocket = serverSocket?.accept() ?: break
                        launch {
                            handleClient(clientSocket)
                        }
                    } catch (_: Exception) {
                    }
                }
            }
        } catch (e: Exception) {
            isRunning = false
        }
    }

    fun stopServer() {
        isRunning = false
        serverJob?.cancel()
        serverJob = null
        try {
            serverSocket?.close()
        } catch (_: Exception) {
        }
        serverSocket = null
        sessions.clear()
    }

    fun approveSession(token: String) {
        sessions[token]?.let {
            it.isApproved = true
        }
    }

    fun revokeSession(token: String) {
        sessions.remove(token)
    }

    fun revokeAllSessions() {
        sessions.clear()
    }

    private suspend fun handleClient(socket: Socket) = withContext(Dispatchers.IO) {
        try {
            socket.use { s ->
                val reader = BufferedReader(InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8))
                val firstLine = reader.readLine() ?: return@withContext
                val parts = firstLine.split(" ")
                if (parts.size < 2) return@withContext

                val method = parts[0].uppercase()
                val fullUri = parts[1]

                val headers = mutableMapOf<String, String>()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    if (line.isNullOrBlank()) break
                    val colonIdx = line!!.indexOf(':')
                    if (colonIdx > 0) {
                        val k = line!!.substring(0, colonIdx).trim().lowercase()
                        val v = line!!.substring(colonIdx + 1).trim()
                        headers[k] = v
                    }
                }

                val out = BufferedOutputStream(s.getOutputStream())
                val uriPath = fullUri.substringBefore('?')
                val queryParams = parseQueryParams(fullUri)

                // Session Token Handling
                var token = headers["x-session-token"] ?: queryParams["token"]
                val clientIp = s.inetAddress.hostAddress ?: "127.0.0.1"
                val userAgent = headers["user-agent"] ?: "Browser"

                // Static assets, auth endpoints, or initial index do not require pre-approval
                if (uriPath.startsWith("/api/v1/auth")) {
                    handleAuthApi(out, method, uriPath, queryParams, clientIp, userAgent)
                    out.flush()
                    return@withContext
                }

                // If accessing other APIs, require session approval
                if (uriPath.startsWith("/api/v1/")) {
                    val session = if (token != null) sessions[token] else null
                    if (session == null || !session.isApproved) {
                        sendErrorResponse(out, 401, "Phone approval required")
                        out.flush()
                        return@withContext
                    }
                }

                when {
                    uriPath.startsWith("/api/v1/storage") -> serveStorageApi(out)
                    uriPath.startsWith("/api/v1/photos") -> servePhotosApi(out)
                    uriPath.startsWith("/api/v1/videos") -> serveVideosApi(out)
                    uriPath.startsWith("/api/v1/music") -> serveMusicApi(out)
                    uriPath.startsWith("/api/v1/apps") -> serveAppsApi(out)
                    uriPath.startsWith("/api/v1/files") -> serveFilesApi(out, queryParams["path"])
                    uriPath.startsWith("/api/v1/download") -> serveDownload(out, queryParams["path"], headers["range"])
                    uriPath.startsWith("/api/v1/zip") -> serveZip(out, queryParams["path"], queryParams["items"])
                    uriPath.startsWith("/api/v1/upload") && method == "POST" -> handleUpload(out, s.getInputStream(), headers)
                    else -> serveStaticAsset(out, uriPath)
                }
                out.flush()
            }
        } catch (_: Exception) {
        }
    }

    private fun handleAuthApi(
        out: OutputStream,
        method: String,
        uriPath: String,
        params: Map<String, String>,
        clientIp: String,
        userAgent: String
    ) {
        if (uriPath == "/api/v1/auth/request") {
            val token = UUID.randomUUID().toString()
            val session = BrowserSession(
                token = token,
                clientIp = clientIp,
                userAgent = userAgent,
                isApproved = false
            )
            sessions[token] = session
            onSessionRequested?.invoke(session)

            val json = JSONObject().apply {
                put("token", token)
                put("status", "pending")
                put("message", "Approval request sent to phone")
            }
            sendJsonResponse(out, json.toString())
        } else if (uriPath == "/api/v1/auth/status") {
            val token = params["token"]
            val session = if (token != null) sessions[token] else null
            val isApproved = session?.isApproved == true

            val json = JSONObject().apply {
                put("token", token ?: "")
                put("isApproved", isApproved)
                put("status", if (isApproved) "approved" else if (session != null) "pending" else "revoked")
            }
            sendJsonResponse(out, json.toString())
        } else if (uriPath == "/api/v1/auth/revoke") {
            val token = params["token"]
            if (token != null) {
                revokeSession(token)
            }
            sendJsonResponse(out, "{\"status\":\"revoked\"}")
        } else {
            sendErrorResponse(out, 404, "Unknown auth endpoint")
        }
    }

    private fun parseQueryParams(uri: String): Map<String, String> {
        val map = mutableMapOf<String, String>()
        val q = uri.substringAfter('?', "")
        if (q.isNotEmpty()) {
            q.split('&').forEach { param ->
                val parts = param.split('=')
                if (parts.size == 2) {
                    try {
                        map[URLDecoder.decode(parts[0], "UTF-8")] = URLDecoder.decode(parts[1], "UTF-8")
                    } catch (_: Exception) {
                    }
                }
            }
        }
        return map
    }

    private fun isPathSafe(target: File): Boolean {
        return try {
            val canonicalTarget = target.canonicalPath
            val canonicalBase = baseStorageDir.canonicalPath
            val canonicalCache = context.cacheDir.canonicalPath
            val canonicalFiles = context.filesDir.canonicalPath
            canonicalTarget.startsWith(canonicalBase) ||
                    canonicalTarget.startsWith(canonicalCache) ||
                    canonicalTarget.startsWith(canonicalFiles)
        } catch (_: Exception) {
            false
        }
    }

    private fun serveStorageApi(out: OutputStream) {
        val totalBytes = 128_000_000_000L
        val freeBytes = 64_000_000_000L
        val usedBytes = totalBytes - freeBytes

        val json = JSONObject().apply {
            put("totalBytes", totalBytes)
            put("usedBytes", usedBytes)
            put("freeBytes", freeBytes)
            put("photosBytes", 43_600_000_000L)
            put("videosBytes", 33_100_000_000L)
            put("musicBytes", 26_800_000_000L)
            put("otherBytes", 10_700_000_000L)
        }
        sendJsonResponse(out, json.toString())
    }

    private suspend fun servePhotosApi(out: OutputStream) {
        val photos = storageManager.queryPhotos()
        val arr = JSONArray()
        photos.forEach { p ->
            arr.put(JSONObject().apply {
                put("id", p.id)
                put("name", p.name)
                put("path", p.path)
                put("size", p.size)
                put("album", p.albumOrFolder)
                put("dateModified", p.dateModified)
            })
        }
        sendJsonResponse(out, arr.toString())
    }

    private suspend fun serveVideosApi(out: OutputStream) {
        val videos = storageManager.queryVideos()
        val arr = JSONArray()
        videos.forEach { v ->
            arr.put(JSONObject().apply {
                put("id", v.id)
                put("name", v.name)
                put("path", v.path)
                put("size", v.size)
                put("duration", v.durationFormatted)
                put("dateModified", v.dateModified)
            })
        }
        sendJsonResponse(out, arr.toString())
    }

    private suspend fun serveMusicApi(out: OutputStream) {
        val songs = storageManager.queryMusic()
        val arr = JSONArray()
        songs.forEach { s ->
            arr.put(JSONObject().apply {
                put("id", s.id)
                put("title", s.name.removeSuffix(".mp3"))
                put("artist", s.artist)
                put("path", s.path)
                put("size", s.size)
                put("duration", s.durationFormatted)
            })
        }
        sendJsonResponse(out, arr.toString())
    }

    private suspend fun serveAppsApi(out: OutputStream) {
        val apps = storageManager.queryInstalledApps()
        val arr = JSONArray()
        apps.forEach { a ->
            arr.put(JSONObject().apply {
                put("packageName", a.packageName)
                put("appName", a.appName)
                put("version", a.versionName)
                put("size", a.apkSize)
            })
        }
        sendJsonResponse(out, arr.toString())
    }

    private fun serveFilesApi(out: OutputStream, pathParam: String?) {
        val cleanParam = pathParam?.replace("\\", "/") ?: ""
        if (cleanParam.contains("..")) {
            sendErrorResponse(out, 403, "Directory traversal prohibited")
            return
        }

        val targetDir = if (cleanParam.isEmpty() || cleanParam == "Internal storage") {
            baseStorageDir
        } else {
            File(baseStorageDir, cleanParam.removePrefix("Internal storage/").removePrefix("/"))
        }

        if (!isPathSafe(targetDir)) {
            sendErrorResponse(out, 403, "Path access prohibited")
            return
        }

        val itemsArr = JSONArray()
        val files = targetDir.listFiles() ?: emptyArray()

        for (f in files) {
            itemsArr.put(JSONObject().apply {
                put("name", f.name)
                put("path", f.absolutePath)
                put("isFolder", f.isDirectory)
                put("size", if (f.isDirectory) 0L else f.length())
                put("dateModified", f.lastModified())
                put("kind", if (f.isDirectory) "folder" else TransferKind.fromFileName(f.name).extensionHint)
            })
        }

        // Build breadcrumbs
        val breadcrumbsArr = JSONArray()
        breadcrumbsArr.put(JSONObject().apply {
            put("name", "Internal storage")
            put("path", "")
        })

        if (cleanParam.isNotEmpty() && cleanParam != "Internal storage") {
            val segments = cleanParam.removePrefix("Internal storage/").removePrefix("/").split("/")
            var accumulated = ""
            for (seg in segments) {
                if (seg.isNotEmpty()) {
                    accumulated = if (accumulated.isEmpty()) seg else "$accumulated/$seg"
                    breadcrumbsArr.put(JSONObject().apply {
                        put("name", seg)
                        put("path", accumulated)
                    })
                }
            }
        }

        val responseObj = JSONObject().apply {
            put("currentPath", cleanParam)
            put("breadcrumbs", breadcrumbsArr)
            put("items", itemsArr)
        }

        sendJsonResponse(out, responseObj.toString())
    }

    private fun serveDownload(out: OutputStream, filePath: String?, rangeHeader: String?) {
        if (filePath.isNullOrEmpty() || filePath.contains("..")) {
            sendErrorResponse(out, 403, "Invalid download path")
            return
        }

        val file = File(filePath)
        if (!isPathSafe(file)) {
            sendErrorResponse(out, 403, "File path outside allowed storage roots")
            return
        }

        if (!file.exists() || file.isDirectory) {
            sendErrorResponse(out, 404, "File not found")
            return
        }

        val totalLength = file.length()
        var start = 0L
        var end = totalLength - 1L

        // HTTP Byte Range support (HTTP 206 Partial Content)
        if (!rangeHeader.isNullOrEmpty() && rangeHeader.startsWith("bytes=")) {
            val rangeVal = rangeHeader.removePrefix("bytes=").trim()
            val dashIdx = rangeVal.indexOf('-')
            if (dashIdx >= 0) {
                val startStr = rangeVal.substring(0, dashIdx).trim()
                val endStr = rangeVal.substring(dashIdx + 1).trim()
                if (startStr.isNotEmpty() && endStr.isNotEmpty()) {
                    start = startStr.toLongOrNull() ?: 0L
                    end = endStr.toLongOrNull() ?: (totalLength - 1L)
                } else if (startStr.isNotEmpty()) {
                    start = startStr.toLongOrNull() ?: 0L
                    end = totalLength - 1L
                } else if (endStr.isNotEmpty()) {
                    val suffixLen = endStr.toLongOrNull() ?: 0L
                    start = (totalLength - suffixLen).coerceAtLeast(0L)
                    end = totalLength - 1L
                }
            }

            start = start.coerceIn(0L, (totalLength - 1L).coerceAtLeast(0L))
            end = end.coerceIn(start, (totalLength - 1L).coerceAtLeast(0L))
            val contentLength = if (totalLength == 0L) 0L else (end - start + 1L)

            val responseHeader = "HTTP/1.1 206 Partial Content\r\n" +
                    "Content-Type: ${getMimeType(file.name)}\r\n" +
                    "Content-Length: $contentLength\r\n" +
                    "Content-Range: bytes $start-$end/$totalLength\r\n" +
                    "Accept-Ranges: bytes\r\n" +
                    "Connection: close\r\n\r\n"
            out.write(responseHeader.toByteArray(StandardCharsets.UTF_8))

            if (contentLength > 0) {
                FileInputStream(file).use { fis ->
                    fis.skip(start)
                    val buffer = ByteArray(32768)
                    var remaining = contentLength
                    while (remaining > 0) {
                        val read = fis.read(buffer, 0, minOf(buffer.size.toLong(), remaining).toInt())
                        if (read == -1) break
                        out.write(buffer, 0, read)
                        remaining -= read
                    }
                }
            }
        } else {
            val responseHeader = "HTTP/1.1 200 OK\r\n" +
                    "Content-Type: ${getMimeType(file.name)}\r\n" +
                    "Content-Length: $totalLength\r\n" +
                    "Accept-Ranges: bytes\r\n" +
                    "Content-Disposition: attachment; filename=\"${file.name}\"\r\n" +
                    "Connection: close\r\n\r\n"
            out.write(responseHeader.toByteArray(StandardCharsets.UTF_8))

            FileInputStream(file).use { fis ->
                val buffer = ByteArray(32768)
                var read: Int
                while (fis.read(buffer).also { read = it } != -1) {
                    out.write(buffer, 0, read)
                }
            }
        }
    }

    private fun serveZip(out: OutputStream, folderPathParam: String?, itemsParam: String?) {
        val folder = if (!folderPathParam.isNullOrEmpty()) File(folderPathParam) else baseStorageDir
        if (!isPathSafe(folder)) {
            sendErrorResponse(out, 403, "Invalid folder path")
            return
        }

        val header = "HTTP/1.1 200 OK\r\n" +
                "Content-Type: application/zip\r\n" +
                "Content-Disposition: attachment; filename=\"${folder.name}.zip\"\r\n" +
                "Connection: close\r\n\r\n"
        out.write(header.toByteArray(StandardCharsets.UTF_8))

        val zipOut = ZipOutputStream(out)
        try {
            val filesToZip = if (!itemsParam.isNullOrEmpty()) {
                itemsParam.split(",").map { File(folder, it.trim()) }.filter { it.exists() }
            } else {
                folder.listFiles()?.toList() ?: emptyList()
            }

            fun addFileToZip(f: File, base: String) {
                if (f.isDirectory) {
                    f.listFiles()?.forEach { child ->
                        addFileToZip(child, "$base/${f.name}")
                    }
                } else {
                    zipOut.putNextEntry(ZipEntry("$base/${f.name}".removePrefix("/")))
                    FileInputStream(f).use { fis ->
                        val buf = ByteArray(16384)
                        var r: Int
                        while (fis.read(buf).also { r = it } != -1) {
                            zipOut.write(buf, 0, r)
                        }
                    }
                    zipOut.closeEntry()
                }
            }

            filesToZip.take(100).forEach { f ->
                addFileToZip(f, "")
            }
        } catch (_: Exception) {
        } finally {
            try {
                zipOut.finish()
            } catch (_: Exception) {
            }
        }
    }

    private fun handleUpload(out: OutputStream, inputStream: InputStream, headers: Map<String, String>) {
        val destDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "Morsecode")
        if (!destDir.exists()) destDir.mkdirs()

        val rawName = headers["x-filename"] ?: "upload_${System.currentTimeMillis()}.bin"
        val fileName = rawName.replace("..", "").replace("/", "_").replace("\\", "_")
        val targetFile = File(destDir, fileName)

        try {
            FileOutputStream(targetFile).use { fos ->
                val buf = ByteArray(32768)
                var read: Int
                val contentLength = headers["content-length"]?.toLongOrNull() ?: 0L
                var received = 0L

                while (inputStream.read(buf).also { read = it } != -1) {
                    fos.write(buf, 0, read)
                    received += read
                    if (contentLength > 0 && received >= contentLength) break
                }
            }
            sendJsonResponse(out, "{\"status\":\"ok\",\"file\":\"$fileName\"}")
        } catch (e: Exception) {
            sendErrorResponse(out, 500, "Upload failed: ${e.message}")
        }
    }

    private fun serveStaticAsset(out: OutputStream, path: String) {
        val assetPath = if (path == "/" || path.isEmpty()) "webshare/index.html" else "webshare" + path
        try {
            context.assets.open(assetPath).use { stream ->
                val bytes = stream.readBytes()
                val mime = when {
                    assetPath.endsWith(".html") -> "text/html; charset=utf-8"
                    assetPath.endsWith(".js") -> "application/javascript; charset=utf-8"
                    assetPath.endsWith(".css") -> "text/css; charset=utf-8"
                    assetPath.endsWith(".svg") -> "image/svg+xml"
                    assetPath.endsWith(".json") -> "application/json"
                    else -> "text/plain"
                }
                val header = "HTTP/1.1 200 OK\r\n" +
                        "Content-Type: $mime\r\n" +
                        "Content-Length: ${bytes.size}\r\n" +
                        "Connection: close\r\n\r\n"
                out.write(header.toByteArray(StandardCharsets.UTF_8))
                out.write(bytes)
            }
        } catch (_: Exception) {
            sendErrorResponse(out, 404, "Asset not found")
        }
    }

    private fun sendJsonResponse(out: OutputStream, json: String) {
        val bytes = json.toByteArray(StandardCharsets.UTF_8)
        val header = "HTTP/1.1 200 OK\r\n" +
                "Content-Type: application/json; charset=utf-8\r\n" +
                "Access-Control-Allow-Origin: *\r\n" +
                "Content-Length: ${bytes.size}\r\n" +
                "Connection: close\r\n\r\n"
        out.write(header.toByteArray(StandardCharsets.UTF_8))
        out.write(bytes)
    }

    private fun sendErrorResponse(out: OutputStream, code: Int, message: String) {
        val body = "{\"error\": \"$message\"}".toByteArray(StandardCharsets.UTF_8)
        val header = "HTTP/1.1 $code Error\r\n" +
                "Content-Type: application/json\r\n" +
                "Access-Control-Allow-Origin: *\r\n" +
                "Content-Length: ${body.size}\r\n" +
                "Connection: close\r\n\r\n"
        out.write(header.toByteArray(StandardCharsets.UTF_8))
        out.write(body)
    }

    private fun getMimeType(name: String): String {
        val lower = name.lowercase()
        return when {
            lower.endsWith(".mp4") -> "video/mp4"
            lower.endsWith(".mp3") -> "audio/mpeg"
            lower.endsWith(".flac") -> "audio/flac"
            lower.endsWith(".jpg") || lower.endsWith(".jpeg") -> "image/jpeg"
            lower.endsWith(".png") -> "image/png"
            lower.endsWith(".webp") -> "image/webp"
            lower.endsWith(".pdf") -> "application/pdf"
            lower.endsWith(".zip") -> "application/zip"
            lower.endsWith(".apk") -> "application/vnd.android.package-archive"
            else -> "application/octet-stream"
        }
    }
}
