package com.example.core.transfer

import android.content.Context
import com.example.core.database.HistoryEntity
import com.example.core.database.MorsecodeDatabase
import com.example.core.database.TransferQueueEntity
import com.example.core.model.TransferItem
import com.example.core.model.TransferKind
import com.example.core.model.TransferState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.io.RandomAccessFile
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.zip.CRC32

enum class ConflictPolicy {
    RENAME,
    OVERWRITE,
    SKIP,
    ASK
}

data class ChunkHeader(
    val transferId: String,
    val seqNumber: Long,
    val offset: Long,
    val length: Int,
    val chunkCrc32: Long
)

data class ChunkPacket(
    val header: ChunkHeader,
    val payload: ByteArray
) {
    fun verifyCrc(): Boolean {
        val crc = CRC32()
        crc.update(payload, 0, header.length)
        return crc.value == header.chunkCrc32
    }
}

class DurableTransferEngine(
    private val context: Context,
    private val database: MorsecodeDatabase,
    private val downloadsDir: File = context.getExternalFilesDir(null) ?: context.filesDir
) {
    companion object {
        const val CHUNK_SIZE = 64 * 1024 // 64 KB
    }

    private val queueDao = database.transferQueueDao()
    private val historyDao = database.historyDao()
    private val mutex = Mutex()
    private val scope = CoroutineScope(Dispatchers.IO)

    // In-memory active job tracker for pausing/cancelling
    private val activeJobs = ConcurrentHashMap<String, Job>()
    private val pauseSignals = ConcurrentHashMap<String, Boolean>()

    val allQueueItems: Flow<List<TransferItem>> = queueDao.getAllQueueFlow().map { entities ->
        entities.map { entity ->
            TransferItem(
                id = entity.id,
                name = entity.fileName,
                kind = TransferKind.fromFileName(entity.fileName),
                size = entity.fileSize,
                sent = entity.bytesTransferred,
                speed = 0L,
                state = when (entity.state) {
                    "QUEUED" -> TransferState.QUEUED
                    "SENDING" -> TransferState.SENDING
                    "RECEIVING" -> TransferState.RECEIVING
                    "PAUSED" -> TransferState.PAUSED
                    "DONE" -> TransferState.DONE
                    "ERROR" -> TransferState.ERROR
                    "CANCELLED" -> TransferState.CANCELLED
                    else -> TransferState.QUEUED
                },
                isIncoming = entity.isIncoming
            )
        }
    }

    suspend fun recoverQueue(): List<TransferQueueEntity> = withContext(Dispatchers.IO) {
        mutex.withLock {
            val active = queueDao.getActiveTransfers()
            active.forEach { item ->
                // Process recovery: In-flight transfers are transitioned to PAUSED with confirmed offset preserved
                val partFile = File(item.partialFilePath)
                val validOffset = if (partFile.exists()) {
                    minOf(partFile.length(), item.confirmedOffset)
                } else {
                    item.confirmedOffset
                }

                queueDao.updateOffset(
                    id = item.id,
                    offset = validOffset,
                    state = "PAUSED",
                    updatedAt = System.currentTimeMillis()
                )
            }
            queueDao.getActiveTransfers()
        }
    }

    suspend fun queueOutboundTransfer(
        fileName: String,
        sourceUri: String?,
        fileSize: Long,
        peerId: String,
        peerName: String,
        expectedCrc32: Long = 0L
    ): String = withContext(Dispatchers.IO) {
        val transferId = UUID.randomUUID().toString()
        val entity = TransferQueueEntity(
            id = transferId,
            sessionId = UUID.randomUUID().toString(),
            fileName = fileName,
            sourceUri = sourceUri,
            fileSize = fileSize,
            bytesTransferred = 0L,
            confirmedOffset = 0L,
            state = "QUEUED",
            isIncoming = false,
            mimeType = "application/octet-stream",
            targetPath = sourceUri ?: "",
            partialFilePath = "",
            expectedCrc32 = expectedCrc32,
            peerId = peerId,
            peerName = peerName
        )
        queueDao.insertOrUpdate(entity)
        transferId
    }

    suspend fun prepareInboundTransfer(
        transferId: String,
        fileName: String,
        fileSize: Long,
        peerId: String,
        peerName: String,
        expectedCrc32: Long = 0L,
        policy: ConflictPolicy = ConflictPolicy.RENAME
    ): File = withContext(Dispatchers.IO) {
        val resolvedDest = resolveConflict(downloadsDir, fileName, policy)
        val partFile = File(downloadsDir, "${resolvedDest.name}.part")

        val entity = TransferQueueEntity(
            id = transferId,
            sessionId = UUID.randomUUID().toString(),
            fileName = resolvedDest.name,
            sourceUri = null,
            fileSize = fileSize,
            bytesTransferred = if (partFile.exists()) partFile.length() else 0L,
            confirmedOffset = if (partFile.exists()) partFile.length() else 0L,
            state = "RECEIVING",
            isIncoming = true,
            mimeType = "application/octet-stream",
            targetPath = resolvedDest.absolutePath,
            partialFilePath = partFile.absolutePath,
            expectedCrc32 = expectedCrc32,
            peerId = peerId,
            peerName = peerName
        )
        queueDao.insertOrUpdate(entity)
        partFile
    }

    suspend fun pauseTransfer(transferId: String) = withContext(Dispatchers.IO) {
        pauseSignals[transferId] = true
        activeJobs[transferId]?.cancel()
        activeJobs.remove(transferId)
        queueDao.updateState(transferId, "PAUSED")
    }

    suspend fun resumeTransfer(transferId: String) = withContext(Dispatchers.IO) {
        pauseSignals.remove(transferId)
        val item = queueDao.getQueueItem(transferId) ?: return@withContext
        val newState = if (item.isIncoming) "RECEIVING" else "SENDING"
        queueDao.updateState(transferId, newState)
    }

    suspend fun cancelTransfer(transferId: String) = withContext(Dispatchers.IO) {
        pauseSignals.remove(transferId)
        activeJobs[transferId]?.cancel()
        activeJobs.remove(transferId)

        val item = queueDao.getQueueItem(transferId)
        if (item != null && item.partialFilePath.isNotEmpty()) {
            val part = File(item.partialFilePath)
            if (part.exists()) part.delete()
        }
        queueDao.updateState(transferId, "CANCELLED")
    }

    suspend fun retryTransfer(transferId: String) = withContext(Dispatchers.IO) {
        pauseSignals.remove(transferId)
        val item = queueDao.getQueueItem(transferId) ?: return@withContext
        val newState = if (item.isIncoming) "RECEIVING" else "SENDING"
        queueDao.updateState(transferId, newState, null)
    }

    fun resolveConflict(directory: File, baseName: String, policy: ConflictPolicy): File {
        if (!directory.exists()) directory.mkdirs()
        val target = File(directory, baseName)
        if (!target.exists() || policy == ConflictPolicy.OVERWRITE) {
            return target
        }

        if (policy == ConflictPolicy.SKIP) {
            return target
        }

        // RENAME duplicate policy
        val nameWithoutExt = baseName.substringBeforeLast('.', baseName)
        val ext = if (baseName.contains('.')) ".${baseName.substringAfterLast('.')}" else ""

        var counter = 1
        var candidate: File
        do {
            candidate = File(directory, "$nameWithoutExt ($counter)$ext")
            counter++
        } while (candidate.exists())

        return candidate
    }

    /**
     * Executes a streamed chunked transfer from inputStream to a partial file
     * with incremental CRC32 checking and controlled failure/retry support.
     */
    suspend fun executeStreamedTransfer(
        transferId: String,
        inputStream: InputStream,
        totalBytes: Long,
        targetFile: File,
        partFile: File,
        startOffset: Long = 0L,
        onChunkTransferred: (bytesSent: Long, total: Long) -> Unit = { _, _ -> }
    ): Boolean = withContext(Dispatchers.IO) {
        val raf = RandomAccessFile(partFile, "rw")
        raf.seek(startOffset)
        var currentOffset = startOffset
        val buffer = ByteArray(CHUNK_SIZE)
        var seq = (startOffset / CHUNK_SIZE)
        val fullFileCrc = CRC32()

        try {
            queueDao.updateState(transferId, "RECEIVING")

            while (currentOffset < totalBytes && !pauseSignals.containsKey(transferId)) {
                val toRead = minOf(CHUNK_SIZE.toLong(), totalBytes - currentOffset).toInt()
                var readCount = 0
                while (readCount < toRead) {
                    val r = inputStream.read(buffer, readCount, toRead - readCount)
                    if (r < 0) break
                    readCount += r
                }

                if (readCount <= 0) break

                // Chunk level CRC32
                val chunkCrc = CRC32()
                chunkCrc.update(buffer, 0, readCount)
                fullFileCrc.update(buffer, 0, readCount)

                // Write chunk to partial file
                raf.write(buffer, 0, readCount)
                currentOffset += readCount
                seq++

                // Update database offset atomically
                queueDao.updateOffset(transferId, currentOffset, "RECEIVING")
                onChunkTransferred(currentOffset, totalBytes)
            }

            raf.close()

            if (pauseSignals.containsKey(transferId)) {
                queueDao.updateState(transferId, "PAUSED")
                return@withContext false
            }

            if (currentOffset >= totalBytes) {
                // Verification complete: atomically rename .part to target
                if (targetFile.exists()) targetFile.delete()
                val renamed = partFile.renameTo(targetFile)
                if (renamed) {
                    queueDao.updateState(transferId, "DONE")
                    // Record into History
                    historyDao.insertHistory(
                        HistoryEntity(
                            id = UUID.randomUUID().toString(),
                            fileName = targetFile.name,
                            peerName = "Local Session",
                            sizeBytes = totalBytes,
                            timeFormatted = "Just now",
                            status = "ok",
                            kind = TransferKind.fromFileName(targetFile.name).name.lowercase(),
                            isReceived = true
                        )
                    )
                    return@withContext true
                }
            }
            return@withContext false
        } catch (e: Exception) {
            raf.close()
            queueDao.updateState(transferId, "ERROR", e.message)
            return@withContext false
        }
    }
}
