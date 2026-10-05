package com.example.core.database

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "transfer_history")
data class HistoryEntity(
    @PrimaryKey val id: String,
    val fileName: String,
    val peerName: String,
    val sizeBytes: Long,
    val timeFormatted: String,
    val status: String, // ok, err
    val kind: String, // video, image, doc, zip, apk, audio
    val isReceived: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "crash_reports")
data class CrashReportEntity(
    @PrimaryKey val id: String,
    val whenText: String,
    val component: String,
    val error: String,
    val note: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "transfer_queue")
data class TransferQueueEntity(
    @PrimaryKey val id: String,
    val sessionId: String,
    val fileName: String,
    val sourceUri: String?,
    val fileSize: Long,
    val bytesTransferred: Long,
    val confirmedOffset: Long,
    val state: String, // QUEUED, SENDING, RECEIVING, PAUSED, DONE, ERROR, CANCELLED
    val isIncoming: Boolean,
    val mimeType: String,
    val targetPath: String,
    val partialFilePath: String,
    val expectedCrc32: Long = 0L,
    val actualCrc32: Long = 0L,
    val peerId: String = "",
    val peerName: String = "",
    val errorMessage: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Dao
interface HistoryDao {
    @Query("SELECT * FROM transfer_history WHERE isReceived = :received ORDER BY timestamp DESC")
    fun getHistory(received: Boolean): Flow<List<HistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(item: HistoryEntity)

    @Query("DELETE FROM transfer_history WHERE isReceived = :received")
    suspend fun clearHistory(received: Boolean)

    @Query("DELETE FROM transfer_history")
    suspend fun clearAll()
}

@Dao
interface CrashReportDao {
    @Query("SELECT * FROM crash_reports ORDER BY timestamp DESC")
    fun getAllCrashReports(): Flow<List<CrashReportEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCrashReport(report: CrashReportEntity)

    @Query("DELETE FROM crash_reports")
    suspend fun clearAll()
}

@Dao
interface TransferQueueDao {
    @Query("SELECT * FROM transfer_queue ORDER BY createdAt ASC")
    fun getAllQueueFlow(): Flow<List<TransferQueueEntity>>

    @Query("SELECT * FROM transfer_queue WHERE id = :id LIMIT 1")
    suspend fun getQueueItem(id: String): TransferQueueEntity?

    @Query("SELECT * FROM transfer_queue WHERE state IN ('SENDING', 'RECEIVING', 'QUEUED')")
    suspend fun getActiveTransfers(): List<TransferQueueEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(item: TransferQueueEntity)

    @Query("UPDATE transfer_queue SET confirmedOffset = :offset, bytesTransferred = :offset, state = :state, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateOffset(id: String, offset: Long, state: String, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE transfer_queue SET state = :state, errorMessage = :error, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateState(id: String, state: String, error: String? = null, updatedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM transfer_queue WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM transfer_queue WHERE state IN ('DONE', 'CANCELLED')")
    suspend fun clearCompleted()

    @Query("DELETE FROM transfer_queue")
    suspend fun clearAll()
}

@Database(entities = [HistoryEntity::class, CrashReportEntity::class, TransferQueueEntity::class], version = 2, exportSchema = false)
abstract class MorsecodeDatabase : RoomDatabase() {
    abstract fun historyDao(): HistoryDao
    abstract fun crashReportDao(): CrashReportDao
    abstract fun transferQueueDao(): TransferQueueDao
}
