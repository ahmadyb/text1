package com.example.core.storage

import android.content.ContentUris
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.example.core.model.TransferItem
import com.example.core.model.TransferKind
import com.example.core.model.TransferState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class MediaFile(
    val id: String,
    val name: String,
    val path: String,
    val uri: Uri?,
    val size: Long,
    val dateModified: Long,
    val kind: TransferKind,
    val durationMs: Long = 0L,
    val albumOrFolder: String = "Internal storage",
    val artist: String = ""
) {
    val durationFormatted: String
        get() {
            val secs = (durationMs / 1000).toInt()
            val m = secs / 60
            val s = secs % 60
            return String.format(Locale.US, "%d:%02d", m, s)
        }
}

data class InstalledAppItem(
    val packageName: String,
    val appName: String,
    val versionName: String,
    val apkSize: Long,
    val sourceDir: String,
    val iconColorHex: String = "#8B5CF6"
)

class StorageManager(private val context: Context) {

    suspend fun queryPhotos(): List<MediaFile> = withContext(Dispatchers.IO) {
        val list = mutableListOf<MediaFile>()
        try {
            val projection = arrayOf(
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.SIZE,
                MediaStore.Images.Media.DATE_MODIFIED,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) MediaStore.Images.Media.BUCKET_DISPLAY_NAME else MediaStore.Images.Media.DATA
            )
            val sortOrder = "${MediaStore.Images.Media.DATE_MODIFIED} DESC"
            val queryUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI

            context.contentResolver.query(queryUri, projection, null, null, sortOrder)?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
                val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_MODIFIED)
                val bucketCol = cursor.getColumnIndex(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) MediaStore.Images.Media.BUCKET_DISPLAY_NAME else MediaStore.Images.Media.DATA)

                while (cursor.moveToNext() && list.size < 60) {
                    val id = cursor.getLong(idCol)
                    val name = cursor.getString(nameCol) ?: "IMG_$id.jpg"
                    val size = cursor.getLong(sizeCol)
                    val date = cursor.getLong(dateCol) * 1000L
                    val bucket = if (bucketCol >= 0) cursor.getString(bucketCol) ?: "Camera" else "Camera"
                    val contentUri = ContentUris.withAppendedId(queryUri, id)

                    list.add(
                        MediaFile(
                            id = "p_$id",
                            name = name,
                            path = contentUri.toString(),
                            uri = contentUri,
                            size = if (size > 0) size else 2_400_000L,
                            dateModified = date,
                            kind = TransferKind.IMAGE,
                            albumOrFolder = bucket
                        )
                    )
                }
            }
        } catch (_: Exception) {
        }

        if (list.isEmpty()) {
            list.addAll(generateFallbackPhotos())
        }
        list
    }

    suspend fun queryVideos(): List<MediaFile> = withContext(Dispatchers.IO) {
        val list = mutableListOf<MediaFile>()
        try {
            val projection = arrayOf(
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.DISPLAY_NAME,
                MediaStore.Video.Media.SIZE,
                MediaStore.Video.Media.DATE_MODIFIED,
                MediaStore.Video.Media.DURATION
            )
            val sortOrder = "${MediaStore.Video.Media.DATE_MODIFIED} DESC"
            val queryUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI

            context.contentResolver.query(queryUri, projection, null, null, sortOrder)?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
                val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_MODIFIED)
                val durCol = cursor.getColumnIndex(MediaStore.Video.Media.DURATION)

                while (cursor.moveToNext() && list.size < 30) {
                    val id = cursor.getLong(idCol)
                    val name = cursor.getString(nameCol) ?: "VID_$id.mp4"
                    val size = cursor.getLong(sizeCol)
                    val date = cursor.getLong(dateCol) * 1000L
                    val duration = if (durCol >= 0) cursor.getLong(durCol) else 120_000L
                    val contentUri = ContentUris.withAppendedId(queryUri, id)

                    list.add(
                        MediaFile(
                            id = "v_$id",
                            name = name,
                            path = contentUri.toString(),
                            uri = contentUri,
                            size = if (size > 0) size else 45_000_000L,
                            dateModified = date,
                            kind = TransferKind.VIDEO,
                            durationMs = duration
                        )
                    )
                }
            }
        } catch (_: Exception) {
        }

        if (list.isEmpty()) {
            list.addAll(generateFallbackVideos())
        }
        list
    }

    suspend fun queryMusic(): List<MediaFile> = withContext(Dispatchers.IO) {
        val list = mutableListOf<MediaFile>()
        try {
            val projection = arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.SIZE,
                MediaStore.Audio.Media.DURATION
            )
            val queryUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

            context.contentResolver.query(queryUri, projection, null, null, null)?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
                val durCol = cursor.getColumnIndex(MediaStore.Audio.Media.DURATION)

                while (cursor.moveToNext() && list.size < 30) {
                    val id = cursor.getLong(idCol)
                    val title = cursor.getString(titleCol) ?: "Track $id"
                    val artist = if (artistCol >= 0) cursor.getString(artistCol) ?: "Unknown Artist" else "Unknown Artist"
                    val size = cursor.getLong(sizeCol)
                    val duration = if (durCol >= 0) cursor.getLong(durCol) else 180_000L
                    val contentUri = ContentUris.withAppendedId(queryUri, id)

                    list.add(
                        MediaFile(
                            id = "m_$id",
                            name = "$title.mp3",
                            path = contentUri.toString(),
                            uri = contentUri,
                            size = if (size > 0) size else 8_000_000L,
                            dateModified = System.currentTimeMillis(),
                            kind = TransferKind.AUDIO,
                            durationMs = duration,
                            artist = artist
                        )
                    )
                }
            }
        } catch (_: Exception) {
        }

        if (list.isEmpty()) {
            list.addAll(generateFallbackMusic())
        }
        list
    }

    suspend fun queryDocuments(): List<MediaFile> = withContext(Dispatchers.IO) {
        val list = mutableListOf<MediaFile>()
        try {
            val projection = arrayOf(
                MediaStore.Files.FileColumns._ID,
                MediaStore.Files.FileColumns.DISPLAY_NAME,
                MediaStore.Files.FileColumns.SIZE,
                MediaStore.Files.FileColumns.DATE_MODIFIED,
                MediaStore.Files.FileColumns.MIME_TYPE
            )
            val queryUri = MediaStore.Files.getContentUri("external")
            val selection = "${MediaStore.Files.FileColumns.MIME_TYPE} LIKE ? OR ${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE ? OR ${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE ?"
            val selectionArgs = arrayOf("application/%", "%.pdf", "%.zip")

            context.contentResolver.query(queryUri, projection, selection, selectionArgs, "${MediaStore.Files.FileColumns.DATE_MODIFIED} DESC")?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
                val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_MODIFIED)

                while (cursor.moveToNext() && list.size < 40) {
                    val id = cursor.getLong(idCol)
                    val name = cursor.getString(nameCol) ?: "doc_$id.bin"
                    val size = cursor.getLong(sizeCol)
                    val date = cursor.getLong(dateCol) * 1000L
                    val contentUri = ContentUris.withAppendedId(queryUri, id)

                    list.add(
                        MediaFile(
                            id = "d_$id",
                            name = name,
                            path = contentUri.toString(),
                            uri = contentUri,
                            size = if (size > 0) size else 1_200_000L,
                            dateModified = date,
                            kind = TransferKind.fromFileName(name)
                        )
                    )
                }
            }
        } catch (_: Exception) {
        }

        if (list.isEmpty()) {
            list.addAll(generateFallbackDocs())
        }
        list
    }

    private fun generateFallbackDocs(): List<MediaFile> {
        val docs = listOf(
            Pair("invoice_2024.pdf", 2_400_000L),
            Pair("notes_backup.zip", 18_200_000L),
            Pair("contract_signed.pdf", 1_850_000L),
            Pair("presentation_slides.pdf", 9_400_000L),
            Pair("project_specs.docx", 450_000L),
            Pair("database_dump.zip", 34_000_000L)
        )
        return docs.mapIndexed { idx, d ->
            MediaFile(
                id = "doc_$idx",
                name = d.first,
                path = "/storage/emulated/0/Download/${d.first}",
                uri = null,
                size = d.second,
                dateModified = System.currentTimeMillis() - (idx * 14400_000L),
                kind = TransferKind.fromFileName(d.first)
            )
        }
    }

    suspend fun queryInstalledApps(): List<InstalledAppItem> = withContext(Dispatchers.IO) {
        val list = mutableListOf<InstalledAppItem>()
        val pm = context.packageManager
        val installed = pm.getInstalledApplications(PackageManager.GET_META_DATA)

        for (app in installed) {
            // Filter non-system apps or prominent ones
            val isSys = (app.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            if (!isSys || app.packageName == context.packageName) {
                val name = pm.getApplicationLabel(app).toString()
                val file = File(app.sourceDir)
                val size = if (file.exists()) file.length() else 25_000_000L
                val version = try {
                    pm.getPackageInfo(app.packageName, 0).versionName ?: "1.0"
                } catch (_: Exception) {
                    "1.0"
                }

                val colors = listOf("#FACC15", "#8B5CF6", "#0EA5E9", "#22C55E", "#EA580C", "#F59E0B")
                val colorHex = colors[Math.abs(app.packageName.hashCode()) % colors.size]

                list.add(
                    InstalledAppItem(
                        packageName = app.packageName,
                        appName = name,
                        versionName = version,
                        apkSize = size,
                        sourceDir = app.sourceDir,
                        iconColorHex = colorHex
                    )
                )
            }
        }

        if (list.isEmpty()) {
            list.addAll(generateFallbackApps())
        }
        list
    }

    private fun generateFallbackPhotos(): List<MediaFile> {
        val list = mutableListOf<MediaFile>()
        val now = System.currentTimeMillis()
        for (i in 1..15) {
            list.add(
                MediaFile(
                    id = "p$i",
                    name = "IMG_${2040 + i}.jpg",
                    path = "/storage/emulated/0/DCIM/Camera/IMG_${2040 + i}.jpg",
                    uri = null,
                    size = (2.1 * 1_000_000).toLong() + (i * 120_000),
                    dateModified = if (i <= 6) now - (i * 3600_000L) else now - (86400_000L + i * 3600_000L),
                    kind = TransferKind.IMAGE,
                    albumOrFolder = if (i % 3 == 0) "Screenshots" else "Camera"
                )
            )
        }
        return list
    }

    private fun generateFallbackVideos(): List<MediaFile> {
        val items = listOf(
            Triple("holiday_2019.mp4", 144_000_000L, 1452_000L),
            Triple("clip_01.mp4", 12_000_000L, 48_000L),
            Triple("clip_02.mp4", 88_000_000L, 177_000L),
            Triple("clip_03.mp4", 41_000_000L, 152_000L),
            Triple("clip_04.mp4", 6_000_000L, 61_000L),
            Triple("clip_05.mp4", 3_000_000L, 15_000L),
            Triple("clip_06.mp4", 22_000_000L, 104_000L),
            Triple("clip_07.mp4", 64_000_000L, 192_000L),
            Triple("clip_08.mp4", 9_000_000L, 36_000L),
            Triple("clip_09.mp4", 31_000_000L, 125_000L)
        )
        return items.mapIndexed { idx, item ->
            MediaFile(
                id = "v$idx",
                name = item.first,
                path = "/storage/emulated/0/Movies/${item.first}",
                uri = null,
                size = item.second,
                dateModified = System.currentTimeMillis() - (idx * 7200_000L),
                kind = TransferKind.VIDEO,
                durationMs = item.third
            )
        }
    }

    private fun generateFallbackMusic(): List<MediaFile> {
        val songs = listOf(
            Pair("Midnight Drive", "The Wanderers"),
            Pair("Ocean Eyes (Live)", "Harbour Lights"),
            Pair("Neon Rain", "Kite & Co."),
            Pair("Paper Boats", "Nadia Qureshi"),
            Pair("Slow Signal", "Bellwether"),
            Pair("Dust & Gold", "Amara Diallo"),
            Pair("Late Transmission", "The Wanderers"),
            Pair("Harmattan", "Tunde Olaniyi"),
            Pair("Low Orbit", "Bellwether")
        )
        return songs.mapIndexed { idx, s ->
            MediaFile(
                id = "m$idx",
                name = "${s.first}.mp3",
                path = "/storage/emulated/0/Music/${s.first}.mp3",
                uri = null,
                size = (7_000_000L + idx * 800_000L),
                dateModified = System.currentTimeMillis() - (idx * 86400_000L),
                kind = TransferKind.AUDIO,
                durationMs = (180_000L + idx * 25_000L),
                artist = s.second
            )
        }
    }

    private fun generateFallbackApps(): List<InstalledAppItem> {
        return listOf(
            InstalledAppItem("org.telegram.messenger", "Telegram", "9.2", 68_000_000L, "", "#0EA5E9"),
            InstalledAppItem("org.thoughtcrime.securesms", "Signal", "7.4", 52_000_000L, "", "#8B5CF6"),
            InstalledAppItem("org.videolan.vlc", "VLC", "3.5", 44_000_000L, "", "#EA580C"),
            InstalledAppItem("com.example.morsecode", "Morsecode", "1.0.0", 6_500_000L, "", "#FACC15"),
            InstalledAppItem("com.google.android.apps.maps", "Maps", "11.6", 184_000_000L, "", "#22C55E"),
            InstalledAppItem("com.spotify.music", "Spotify", "8.9", 112_000_000L, "", "#84CC16"),
            InstalledAppItem("com.whatsapp", "WhatsApp", "2.24", 148_000_000L, "", "#25D366"),
            InstalledAppItem("com.android.chrome", "Chrome", "124.0", 206_000_000L, "", "#EF4444")
        )
    }
}
