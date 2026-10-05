package com.example.network.transport

import android.content.Context
import com.example.core.model.Peer
import com.example.core.model.TransferItem
import com.example.core.model.TransferKind
import com.example.core.model.TransferState
import com.example.core.transport.LanSocketTransportChannel
import com.example.core.transport.TransportChannel
import com.example.service.TransferForegroundService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.net.ServerSocket
import java.net.Socket
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.zip.CRC32

class TransferSocketEngine(
    private val context: Context,
    val port: Int = 33456
) {
    companion object {
        const val FRAME_HELLO: Byte = 0x01
        const val FRAME_FILE_START: Byte = 0x02
        const val FRAME_CHUNK: Byte = 0x03
        const val FRAME_ACK: Byte = 0x04
        const val FRAME_FILE_RESUME: Byte = 0x05
        const val FRAME_FILE_PAUSE: Byte = 0x06
        const val FRAME_FILE_CANCEL: Byte = 0x07
        const val FRAME_DUPLEX_PROMPT: Byte = 0x08
    }

    private var serverSocket: ServerSocket? = null
    private var serverJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _activeTransfers = MutableStateFlow<List<TransferItem>>(emptyList())
    val activeTransfers: StateFlow<List<TransferItem>> = _activeTransfers.asStateFlow()

    // Per-file control flags
    private val pausedFiles = ConcurrentHashMap<String, Boolean>()
    private val cancelledFiles = ConcurrentHashMap<String, Boolean>()

    fun startListening(
        customPort: Int = port,
        onIncomingConsent: ((peerName: String, peerId: String) -> Unit)? = null
    ) {
        if (serverSocket != null) return

        try {
            serverSocket = ServerSocket(customPort)
            serverJob = scope.launch {
                while (isActive) {
                    try {
                        val client = serverSocket?.accept() ?: break
                        launch {
                            handleIncomingConnection(client, onIncomingConsent)
                        }
                    } catch (_: Exception) {
                    }
                }
            }
        } catch (_: Exception) {
        }
    }

    fun stopListening() {
        serverJob?.cancel()
        serverJob = null
        try {
            serverSocket?.close()
        } catch (_: Exception) {
        }
        serverSocket = null
        TransferForegroundService.stopService(context)
    }

    private suspend fun handleIncomingConnection(
        socket: Socket,
        onIncomingConsent: ((String, String) -> Unit)?
    ) = withContext(Dispatchers.IO) {
        try {
            socket.use { s ->
                val dis = DataInputStream(s.getInputStream())
                val dos = DataOutputStream(s.getOutputStream())

                // 1. Read HELLO
                val magic = dis.readByte()
                if (magic != FRAME_HELLO) return@withContext
                val peerName = dis.readUTF()
                val peerId = dis.readUTF()

                onIncomingConsent?.invoke(peerName, peerId)

                // Welcome ACK
                dos.writeByte(FRAME_HELLO.toInt())
                dos.flush()

                // Read file streams
                while (isActive && !s.isClosed) {
                    val frameType = try { dis.readByte() } catch (_: Exception) { break }

                    if (frameType == FRAME_FILE_START || frameType == FRAME_FILE_RESUME) {
                        val transferId = dis.readUTF()
                        val fileName = dis.readUTF()
                        val fileSize = dis.readLong()
                        val startOffset = if (frameType == FRAME_FILE_RESUME) dis.readLong() else 0L

                        val destFile = File(context.cacheDir, "$fileName.part")
                        val raf = RandomAccessFile(destFile, "rw")
                        raf.seek(startOffset)

                        val incomingItem = TransferItem(
                            id = transferId,
                            name = fileName,
                            kind = TransferKind.fromFileName(fileName),
                            size = fileSize,
                            sent = startOffset,
                            speed = 6_500_000L,
                            state = TransferState.RECEIVING,
                            isIncoming = true
                        )
                        updateTransfer(incomingItem)
                        updateNotification(fileName, startOffset, fileSize)

                        var receivedBytes = startOffset
                        val chunkBuffer = ByteArray(65536)

                        try {
                            while (receivedBytes < fileSize && isActive) {
                                if (pausedFiles.containsKey(transferId)) {
                                    updateTransfer(incomingItem.copy(sent = receivedBytes, state = TransferState.PAUSED))
                                    break
                                }
                                if (cancelledFiles.containsKey(transferId)) {
                                    updateTransfer(incomingItem.copy(state = TransferState.CANCELLED))
                                    raf.close()
                                    if (destFile.exists()) destFile.delete()
                                    break
                                }

                                val chunkFrame = dis.readByte()
                                if (chunkFrame != FRAME_CHUNK) break

                                val offset = dis.readLong()
                                val len = dis.readInt()
                                val crcExpected = dis.readLong()

                                dis.readFully(chunkBuffer, 0, len)

                                val crc = CRC32()
                                crc.update(chunkBuffer, 0, len)
                                if (crc.value != crcExpected) {
                                    // CRC Mismatch, request re-send
                                    dos.writeByte(0xFF) // NACK
                                    dos.writeLong(receivedBytes)
                                    dos.flush()
                                    continue
                                }

                                raf.write(chunkBuffer, 0, len)
                                receivedBytes += len

                                updateTransfer(
                                    incomingItem.copy(
                                        sent = receivedBytes,
                                        state = if (receivedBytes >= fileSize) TransferState.DONE else TransferState.RECEIVING
                                    )
                                )
                                updateNotification(fileName, receivedBytes, fileSize)

                                // Send ACK
                                dos.writeByte(FRAME_ACK.toInt())
                                dos.writeLong(receivedBytes)
                                dos.flush()
                            }
                        } finally {
                            raf.close()
                        }

                        if (receivedBytes >= fileSize) {
                            val finalFile = File(context.cacheDir, fileName)
                            if (finalFile.exists()) finalFile.delete()
                            destFile.renameTo(finalFile)
                            updateTransfer(incomingItem.copy(sent = fileSize, state = TransferState.DONE))
                        }
                    }
                }
            }
        } catch (_: Exception) {
        }
    }

    suspend fun sendFile(
        peerIp: String,
        peerPort: Int,
        file: File,
        startOffset: Long = 0L,
        onProgress: (Long, Long) -> Unit = { _, _ -> }
    ): Boolean = withContext(Dispatchers.IO) {
        val transferId = UUID.randomUUID().toString()
        val item = TransferItem(
            id = transferId,
            name = file.name,
            kind = TransferKind.fromFileName(file.name),
            size = file.length(),
            sent = startOffset,
            speed = 7_500_000L,
            state = TransferState.SENDING,
            isIncoming = false
        )
        updateTransfer(item)
        updateNotification(file.name, startOffset, file.length())

        try {
            Socket(peerIp, peerPort).use { s ->
                val dis = DataInputStream(s.getInputStream())
                val dos = DataOutputStream(s.getOutputStream())

                // Send HELLO
                dos.writeByte(FRAME_HELLO.toInt())
                dos.writeUTF("MYA-L10")
                dos.writeUTF(UUID.randomUUID().toString())
                dos.flush()

                // Expect WELCOME
                val ack = dis.readByte()
                if (ack != FRAME_HELLO) return@withContext false

                // Send FILE_START or RESUME
                if (startOffset > 0) {
                    dos.writeByte(FRAME_FILE_RESUME.toInt())
                    dos.writeUTF(transferId)
                    dos.writeUTF(file.name)
                    dos.writeLong(file.length())
                    dos.writeLong(startOffset)
                } else {
                    dos.writeByte(FRAME_FILE_START.toInt())
                    dos.writeUTF(transferId)
                    dos.writeUTF(file.name)
                    dos.writeLong(file.length())
                }
                dos.flush()

                val raf = RandomAccessFile(file, "r")
                raf.seek(startOffset)
                val buffer = ByteArray(65536)
                var sentTotal = startOffset

                try {
                    while (sentTotal < file.length() && isActive) {
                        if (pausedFiles.containsKey(transferId)) {
                            updateTransfer(item.copy(sent = sentTotal, state = TransferState.PAUSED))
                            return@withContext false
                        }
                        if (cancelledFiles.containsKey(transferId)) {
                            updateTransfer(item.copy(state = TransferState.CANCELLED))
                            return@withContext false
                        }

                        val toRead = minOf(buffer.size.toLong(), file.length() - sentTotal).toInt()
                        val read = raf.read(buffer, 0, toRead)
                        if (read <= 0) break

                        dos.writeByte(FRAME_CHUNK.toInt())
                        dos.writeLong(sentTotal)
                        dos.writeInt(read)

                        val crc = CRC32()
                        crc.update(buffer, 0, read)
                        dos.writeLong(crc.value)

                        dos.write(buffer, 0, read)
                        dos.flush()

                        // Wait ACK
                        val ackType = dis.readByte()
                        if (ackType == FRAME_ACK) {
                            val confirmed = dis.readLong()
                            sentTotal = confirmed
                            updateTransfer(
                                item.copy(
                                    sent = sentTotal,
                                    state = if (sentTotal >= file.length()) TransferState.DONE else TransferState.SENDING
                                )
                            )
                            updateNotification(file.name, sentTotal, file.length())
                            onProgress(sentTotal, file.length())
                        }
                    }
                } finally {
                    raf.close()
                }

                if (sentTotal >= file.length()) {
                    updateTransfer(item.copy(sent = file.length(), state = TransferState.DONE))
                    return@withContext true
                }
                return@withContext false
            }
        } catch (e: Exception) {
            updateTransfer(item.copy(state = TransferState.FAILED, errorReason = e.message))
            return@withContext false
        }
    }

    /**
     * Broadcast to N phones simultaneously:
     * Independent coroutine streams ensure that a slower phone never stalls faster phones.
     */
    suspend fun broadcastFileToPeers(
        peers: List<Peer>,
        file: File,
        onPeerProgress: (peerId: String, sent: Long, total: Long) -> Unit = { _, _, _ -> }
    ): Map<String, Boolean> = withContext(Dispatchers.IO) {
        val results = ConcurrentHashMap<String, Boolean>()

        val jobs = peers.map { peer ->
            async {
                val success = sendFile(
                    peerIp = peer.ip,
                    peerPort = if (peer.port > 0) peer.port else 33456,
                    file = file
                ) { sent, total ->
                    onPeerProgress(peer.id, sent, total)
                }
                results[peer.id] = success
            }
        }

        jobs.awaitAll()
        results
    }

    fun pauseFile(fileId: String) {
        pausedFiles[fileId] = true
    }

    fun resumeFile(fileId: String) {
        pausedFiles.remove(fileId)
    }

    fun cancelFile(fileId: String) {
        cancelledFiles[fileId] = true
    }

    fun retryFile(fileId: String) {
        pausedFiles.remove(fileId)
        cancelledFiles.remove(fileId)
    }

    private fun updateTransfer(item: TransferItem) {
        val current = _activeTransfers.value.toMutableList()
        val index = current.indexOfFirst { it.id == item.id }
        if (index >= 0) {
            current[index] = item
        } else {
            current.add(item)
        }
        _activeTransfers.value = current
    }

    private fun updateNotification(fileName: String, sent: Long, total: Long) {
        val percent = if (total > 0) ((sent.toFloat() / total.toFloat()) * 100).toInt() else 0
        try {
            TransferForegroundService.startService(
                context = context,
                title = "Transferring: $fileName",
                subtitle = "$percent% · ${(sent / 1024)} KB of ${(total / 1024)} KB",
                progress = percent
            )
        } catch (_: Exception) {
        }
    }

    fun clearCompleted() {
        _activeTransfers.value = _activeTransfers.value.filter { it.state != TransferState.DONE }
    }

    fun pauseAll() {
        _activeTransfers.value.forEach { pauseFile(it.id) }
    }

    fun resumeAll() {
        _activeTransfers.value.forEach { resumeFile(it.id) }
    }
}
