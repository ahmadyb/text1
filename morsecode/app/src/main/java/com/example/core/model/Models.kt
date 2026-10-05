package com.example.core.model

enum class TransferKind(val extensionHint: String) {
    IMAGE("image"),
    VIDEO("video"),
    AUDIO("audio"),
    DOC("doc"),
    ZIP("zip"),
    APK("apk");

    companion object {
        fun fromFileName(name: String): TransferKind {
            val lower = name.lowercase()
            return when {
                lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png") || lower.endsWith(".webp") || lower.endsWith(".gif") -> IMAGE
                lower.endsWith(".mp4") || lower.endsWith(".mkv") || lower.endsWith(".webm") || lower.endsWith(".mov") || lower.endsWith(".avi") -> VIDEO
                lower.endsWith(".mp3") || lower.endsWith(".flac") || lower.endsWith(".wav") || lower.endsWith(".m4a") || lower.endsWith(".ogg") -> AUDIO
                lower.endsWith(".zip") || lower.endsWith(".tar") || lower.endsWith(".gz") || lower.endsWith(".7z") || lower.endsWith(".rar") -> ZIP
                lower.endsWith(".apk") -> APK
                else -> DOC
            }
        }
    }
}

enum class TransferState(val raw: String) {
    SENDING("sending"),
    QUEUED("queued"),
    PAUSED("paused"),
    RECEIVING("receiving"),
    DONE("done"),
    FAILED("failed"),
    SKIPPED("skipped"),
    ERROR("error"),
    CANCELLED("cancelled")
}

data class TransferItem(
    val id: String,
    val name: String,
    val kind: TransferKind,
    val size: Long,
    val sent: Long = 0L,
    val speed: Long = 0L,
    val state: TransferState = TransferState.QUEUED,
    val isIncoming: Boolean = false,
    val localUri: String? = null,
    val checksum: String? = null,
    val errorReason: String? = null
) {
    val progress: Float
        get() = if (size > 0L) (sent.toFloat() / size.toFloat()).coerceIn(0f, 1f) else 0f
}

data class Peer(
    val id: String,
    val name: String,
    val letter: String = name.take(1).uppercase(),
    val colorHex: String = "#8B5CF6",
    val sub: String = "",
    val badge: String = "LAN",
    val ip: String = "",
    val port: Int = 33456,
    val isNearby: Boolean = false
)

data class LogItem(
    val timestamp: String,
    val level: String, // INFO, WARN, ERROR
    val message: String
)

data class CrashItem(
    val id: String,
    val whenText: String,
    val whereText: String,
    val error: String,
    val note: String
)

data class DoctorCheckItem(
    val key: String,
    val value: String,
    val status: String // ok, warn, err
)

data class WebShareSession(
    val id: String,
    val clientName: String,
    val ipAddress: String,
    val acceptedAt: String,
    val token: String,
    val isLive: Boolean = true
)
