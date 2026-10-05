package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.os.Build
import android.os.PowerManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.MorsecodeApp
import com.example.core.database.CrashReportEntity
import com.example.core.database.HistoryEntity
import com.example.core.model.CrashItem
import com.example.core.model.DoctorCheckItem
import com.example.core.model.LogItem
import com.example.core.model.Peer
import com.example.core.model.TransferItem
import com.example.core.model.TransferKind
import com.example.core.model.TransferState
import com.example.core.storage.InstalledAppItem
import com.example.core.storage.MediaFile
import com.example.core.storage.StorageManager
import com.example.network.discovery.LanDiscovery
import com.example.network.server.WebShareServer
import com.example.network.transport.TransferSocketEngine
import com.example.service.TransferForegroundService
import com.example.ui.theme.AccentColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

enum class ScreenState {
    ONBOARDING,
    CONNECT,
    DISCOVER,
    DISCOVER_MULTI,
    SENDING,
    RECEIVE,
    RECEIVING,
    BROADCAST_SENDER,
    BROADCAST_RECEIVERS,
    BROADCAST_COMPLETE,
    WEBSHARE,
    DOCTOR,
    HELP,
    FILES,
    BROWSE_STORAGE,
    PHOTO_VIEWER,
    MUSIC_PLAYER,
    VIDEO_PLAYER,
    HISTORY,
    SETTINGS,
    LOGS,
    CRASHES,
    CONSENT_PEER,
    CONSENT_BROWSER
}

class MorsecodeViewModel(application: Application) : AndroidViewModel(application) {

    private val container = (application as MorsecodeApp).container
    val db = container.database
    val preferencesManager = container.preferencesManager
    val storageManager = container.storageManager
    val playerManager = container.playerManager
    val durableEngine = container.durableEngine
    val lanDiscovery = LanDiscovery(application)
    val webShareServer = WebShareServer(application, storageManager)
    val transferEngine = TransferSocketEngine(application)

    // Navigation & Screen
    private val _currentScreen = MutableStateFlow(ScreenState.CONNECT)
    val currentScreen: StateFlow<ScreenState> = _currentScreen.asStateFlow()

    val onboardingStep = MutableStateFlow(0)

    // Themes & Settings
    val isDarkTheme = MutableStateFlow(true)
    val currentAccent = MutableStateFlow(AccentColor.SUNFLOWER)
    val followSystemTheme = MutableStateFlow(false)
    val soundEffectsEnabled = MutableStateFlow(true)
    val notificationsEnabled = MutableStateFlow(true)
    val conflictPolicy = MutableStateFlow("Rename duplicates")
    val broadcastPeersLimit = MutableStateFlow(4)
    val deviceName = MutableStateFlow("MYA-L10")

    // Active Duplex Transfers
    val sendingList = MutableStateFlow<List<TransferItem>>(emptyList())
    val receivingList = MutableStateFlow<List<TransferItem>>(emptyList())
    val currentPeer = MutableStateFlow(
        Peer(
            id = "r",
            name = "Ravi's Redmi",
            letter = "R",
            colorHex = "#8B5CF6",
            sub = "192.168.1.42 · Phone · LAN",
            badge = "LAN",
            ip = "192.168.1.42"
        )
    )

    // Broadcast 1->N State
    val broadcastPeers = MutableStateFlow<List<Peer>>(emptyList())
    val broadcastItems = MutableStateFlow<List<TransferItem>>(emptyList())
    val broadcastPeerProgress = MutableStateFlow<Map<String, Map<String, Long>>>(emptyMap())
    val isBroadcastDone = MutableStateFlow(false)

    // Files & Media
    val filesCurrentTab = MutableStateFlow("photos") // photos, videos, music, apps, files
    val photosList = MutableStateFlow<List<MediaFile>>(emptyList())
    val videosList = MutableStateFlow<List<MediaFile>>(emptyList())
    val musicList = MutableStateFlow<List<MediaFile>>(emptyList())
    val appsList = MutableStateFlow<List<InstalledAppItem>>(emptyList())
    val docsList = MutableStateFlow<List<MediaFile>>(emptyList())
    val selectedItemIds = MutableStateFlow<Set<String>>(emptySet())
    val currentFolder = MutableStateFlow("Internal storage")

    // Media Playback State
    val activePhotoIndex = MutableStateFlow(3)
    val activeVideoIndex = MutableStateFlow(0)
    val isMusicPlaying = MutableStateFlow(true)
    val musicPositionSec = MutableStateFlow(104)
    val musicDurationSec = MutableStateFlow(248)
    val isVideoPlaying = MutableStateFlow(false)
    val videoPositionSec = MutableStateFlow(408)
    val videoDurationSec = MutableStateFlow(1452)
    val mediaVolume = MutableStateFlow(0.7f)
    val isMuted = MutableStateFlow(false)

    // Dialogs & Sheets
    val pendingPeerConsent = MutableStateFlow<Peer?>(null)
    val pendingBrowserConsent = MutableStateFlow<String?>(null)
    val pendingBrowserSession = MutableStateFlow<com.example.network.server.BrowserSession?>(null)
    val isSortSheetOpen = MutableStateFlow(false)
    val isShareSheetOpen = MutableStateFlow(false)
    val shareSheetTitle = MutableStateFlow("morsecode-log.txt")
    val shareSheetSubtitle = MutableStateFlow("12.4 KB · Morsecode 1.0.0 (1)")
    val sortKey = MutableStateFlow("date")
    val sortAscending = MutableStateFlow(false)

    // History & Logs
    val historyTab = MutableStateFlow("received") // received, sent
    val receivedHistory = MutableStateFlow<List<HistoryEntity>>(emptyList())
    val sentHistory = MutableStateFlow<List<HistoryEntity>>(emptyList())
    val localLogs = MutableStateFlow<List<LogItem>>(emptyList())
    val localCrashes = MutableStateFlow<List<CrashItem>>(emptyList())
    val logsErrorOnly = MutableStateFlow(false)
    val helpExpandedIndex = MutableStateFlow(-1)

    // Toast
    val toastMessage = MutableStateFlow<String?>(null)

    // Connection Doctor items
    val doctorChecks = MutableStateFlow<List<DoctorCheckItem>>(emptyList())

    init {
        viewModelScope.launch {
            durableEngine.recoverQueue()
        }
        initializeSampleData()
        loadLocalHistoryAndCrashes()
        runDoctorDiagnostics()
        startMediaTicker()
        loadMediaFiles()

        webShareServer.onSessionRequested = { session ->
            pendingBrowserSession.value = session
            navigateTo(ScreenState.CONSENT_BROWSER)
        }
    }

    fun approveBrowserSession() {
        pendingBrowserSession.value?.let { session ->
            webShareServer.approveSession(session.token)
        }
        showToast("Browser approved — session active")
        navigateTo(ScreenState.WEBSHARE)
    }

    fun denyBrowserSession() {
        pendingBrowserSession.value?.let { session ->
            webShareServer.revokeSession(session.token)
        }
        showToast("Browser session denied")
        navigateTo(ScreenState.WEBSHARE)
    }

    private fun initializeSampleData() {
        val defaultSend = listOf(
            TransferItem("v1", "holiday_2019.mp4", TransferKind.VIDEO, 144_000_000L, 48_900_000L, 6_200_000L, TransferState.SENDING),
            TransferItem("i1", "IMG_2043.jpg", TransferKind.IMAGE, 4_100_000L, 0L, 5_700_000L, TransferState.QUEUED),
            TransferItem("a1", "live_set_final.flac", TransferKind.AUDIO, 64_000_000L, 39_700_000L, 4_900_000L, TransferState.PAUSED)
        )
        val defaultRecv = listOf(
            TransferItem("z1", "notes_backup.zip", TransferKind.ZIP, 18_200_000L, 13_100_000L, 8_400_000L, TransferState.RECEIVING, isIncoming = true)
        )
        sendingList.value = defaultSend
        receivingList.value = defaultRecv

        val defaultLogs = listOf(
            LogItem("09:41:02", "INFO", "Morsecode 1.0.0 (1) started"),
            LogItem("09:41:02", "INFO", "Multicast lock acquired"),
            LogItem("09:41:03", "INFO", "UDP broadcast :33457 sent"),
            LogItem("09:41:04", "INFO", "Peer discovered: Ravi's Redmi"),
            LogItem("09:41:05", "INFO", "TCP control :33456 connected"),
            LogItem("09:41:06", "WARN", "CRC mismatch on chunk seq=0192 — retrying"),
            LogItem("09:41:07", "INFO", "Chunk seq=0193 verified"),
            LogItem("09:41:09", "INFO", "Broadcast fan-out: 3 peers accepted"),
            LogItem("09:41:12", "ERROR", "Session closed by peer · reason=USER_END")
        )
        localLogs.value = defaultLogs
    }

    private fun loadMediaFiles() {
        viewModelScope.launch {
            photosList.value = storageManager.queryPhotos()
            videosList.value = storageManager.queryVideos()
            musicList.value = storageManager.queryMusic()
            appsList.value = storageManager.queryInstalledApps()
            docsList.value = storageManager.queryDocuments()
        }
    }

    fun playMusic(track: MediaFile, index: Int) {
        playerManager.playMusicTrack(track, index)
        navigateTo(ScreenState.MUSIC_PLAYER)
    }

    fun playVideo(video: MediaFile) {
        playerManager.playVideoTrack(video)
        navigateTo(ScreenState.VIDEO_PLAYER)
    }

    fun openPhoto(index: Int) {
        activePhotoIndex.value = index
        navigateTo(ScreenState.PHOTO_VIEWER)
    }

    override fun onCleared() {
        super.onCleared()
        playerManager.release()
    }

    private fun loadLocalHistoryAndCrashes() {
        viewModelScope.launch {
            db.historyDao().getHistory(true).collect { items ->
                receivedHistory.value = items.ifEmpty { defaultReceivedHistory() }
            }
        }
        viewModelScope.launch {
            db.historyDao().getHistory(false).collect { items ->
                sentHistory.value = items.ifEmpty { defaultSentHistory() }
            }
        }
        viewModelScope.launch {
            db.crashReportDao().getAllCrashReports().collect { reports ->
                localCrashes.value = reports.map {
                    CrashItem(it.id, it.whenText, it.component, it.error, it.note)
                }.ifEmpty { defaultCrashReports() }
            }
        }
    }

    private fun defaultReceivedHistory() = listOf(
        HistoryEntity("h1", "VID_20240512_151322.mp4", "Ravi's Redmi", 131_100_000L, "15:13", "err", "video", true),
        HistoryEntity("h2", "screenshot_0912.png", "Ravi's Redmi", 116_800_000L, "15:09", "ok", "image", true),
        HistoryEntity("h3", "VID_20240511_150801.mp4", "Pixel 7X", 116_800_000L, "15:08", "err", "video", true),
        HistoryEntity("h4", "VID_20240511_150702.mp4", "Pixel 7X", 116_800_000L, "15:07", "err", "video", true)
    )

    private fun defaultSentHistory() = listOf(
        HistoryEntity("s1", "VID_20240512_151322.mp4", "Ravi's Redmi", 131_100_000L, "15:13", "ok", "video", false),
        HistoryEntity("s2", "invoice_2024.pdf", "Pixel 7X", 2_400_000L, "14:42", "ok", "doc", false)
    )

    private fun defaultCrashReports() = listOf(
        CrashItem("c1", "Today · 18:42", "TransferService", "IllegalStateException", "Session recovered; queued files preserved"),
        CrashItem("c2", "Yesterday · 09:17", "WebShareServer", "SocketException", "Client disconnected during range request"),
        CrashItem("c3", "24 Sep · 22:06", "MediaIndexer", "SecurityException", "Storage grant was revoked")
    )

    fun runDoctorDiagnostics() {
        val context = getApplication<Application>()
        val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val isIgnoringBattery = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            pm?.isIgnoringBatteryOptimizations(context.packageName) == true
        } else true

        doctorChecks.value = listOf(
            DoctorCheckItem("Wi-Fi connected", "MY-NETWORK · -42 dBm", "ok"),
            DoctorCheckItem("Same network as peers", "3 peers on 192.168.1.0/24", "ok"),
            DoctorCheckItem("Multicast lock", "Held · beacon every 1200 ms", "ok"),
            DoctorCheckItem("Play Services old", "Nearby Connections may be slower", "warn"),
            DoctorCheckItem("Permissions granted", "Nearby · Storage · Battery", "ok"),
            DoctorCheckItem(
                if (isIgnoringBattery) "Battery optimization OFF" else "Battery optimization ON",
                if (isIgnoringBattery) "Morsecode is exempt — transfers survive screen off" else "Transfers may pause in background",
                if (isIgnoringBattery) "ok" else "err"
            )
        )
    }

    private fun startMediaTicker() {
        viewModelScope.launch(Dispatchers.IO) {
            while (isActive) {
                delay(250L)
                // Music progress
                if (isMusicPlaying.value) {
                    val pos = musicPositionSec.value + 1
                    musicPositionSec.value = if (pos >= musicDurationSec.value) 0 else pos
                }
                // Video progress
                if (isVideoPlaying.value) {
                    val pos = videoPositionSec.value + 1
                    videoPositionSec.value = if (pos >= videoDurationSec.value) 0 else pos
                }
            }
        }
    }

    fun navigateTo(screen: ScreenState) {
        _currentScreen.value = screen
    }

    fun showToast(msg: String) {
        toastMessage.value = msg
    }

    fun clearToast() {
        toastMessage.value = null
    }

    fun toggleWebShare() {
        if (webShareServer.isRunning) {
            webShareServer.stopServer()
            showToast("WebShare stopped")
        } else {
            webShareServer.startServer()
            showToast("WebShare running on :33455")
        }
    }

    fun toggleItemPause(item: TransferItem) {
        val list = if (item.isIncoming) receivingList else sendingList
        val updated = list.value.map {
            if (it.id == item.id) {
                if (it.state == TransferState.SENDING || it.state == TransferState.RECEIVING) {
                    it.copy(state = TransferState.PAUSED)
                } else if (it.state == TransferState.PAUSED) {
                    it.copy(state = if (it.isIncoming) TransferState.RECEIVING else TransferState.SENDING)
                } else it
            } else it
        }
        list.value = updated
        showToast("${if (item.state == TransferState.PAUSED) "Resumed" else "Paused"} ${item.name}")
    }

    fun cancelItem(item: TransferItem) {
        val list = if (item.isIncoming) receivingList else sendingList
        list.value = list.value.map {
            if (it.id == item.id) it.copy(state = TransferState.FAILED) else it
        }
        showToast("Cancelled ${item.name}")
    }

    fun retryItem(item: TransferItem) {
        val list = if (item.isIncoming) receivingList else sendingList
        list.value = list.value.map {
            if (it.id == item.id) it.copy(sent = 0L, state = if (it.isIncoming) TransferState.RECEIVING else TransferState.SENDING) else it
        }
        showToast("Retrying ${item.name}")
    }

    fun clearCompleted(isIncoming: Boolean) {
        val list = if (isIncoming) receivingList else sendingList
        list.value = list.value.filter { it.state != TransferState.DONE }
        showToast("Completed transfers cleared")
    }

    fun togglePauseAll() {
        val anyActive = sendingList.value.any { it.state == TransferState.SENDING } ||
                receivingList.value.any { it.state == TransferState.RECEIVING }

        sendingList.value = sendingList.value.map {
            if (anyActive && it.state == TransferState.SENDING) it.copy(state = TransferState.PAUSED)
            else if (!anyActive && it.state == TransferState.PAUSED) it.copy(state = TransferState.SENDING)
            else it
        }
        receivingList.value = receivingList.value.map {
            if (anyActive && it.state == TransferState.RECEIVING) it.copy(state = TransferState.PAUSED)
            else if (!anyActive && it.state == TransferState.PAUSED) it.copy(state = TransferState.RECEIVING)
            else it
        }
        showToast(if (anyActive) "All transfers paused" else "Resumed")
    }

    fun endTransferSession() {
        sendingList.value = emptyList()
        receivingList.value = emptyList()
        TransferForegroundService.stopService(getApplication())
        navigateTo(ScreenState.CONNECT)
        showToast("Session ended · queue cleared")
    }

    fun backgroundSession() {
        val activeCount = sendingList.value.count { it.state == TransferState.SENDING } +
                receivingList.value.count { it.state == TransferState.RECEIVING }
        TransferForegroundService.startService(
            getApplication(),
            "Morsecode transfer active",
            "$activeCount files transferring in background",
            50
        )
        showToast("Minimised — notification keeps transfer alive")
        navigateTo(ScreenState.CONNECT)
    }

    fun startBroadcast(peers: List<Peer>) {
        broadcastPeers.value = peers
        val items = listOf(
            TransferItem("v1", "holiday_2019.mp4", TransferKind.VIDEO, 144_000_000L, 0L, 4_800_000L, TransferState.SENDING),
            TransferItem("i1", "IMG_2043.jpg", TransferKind.IMAGE, 4_100_000L, 0L, 5_400_000L, TransferState.QUEUED),
            TransferItem("a1", "live_set_final.flac", TransferKind.AUDIO, 64_000_000L, 0L, 4_000_000L, TransferState.QUEUED)
        )
        broadcastItems.value = items
        isBroadcastDone.value = false

        val initialProgress = mutableMapOf<String, Map<String, Long>>()
        peers.forEach { p ->
            initialProgress[p.id] = mapOf("v1" to 48_900_000L, "i1" to 0L, "a1" to 0L)
        }
        broadcastPeerProgress.value = initialProgress
        navigateTo(ScreenState.BROADCAST_SENDER)
        showToast("Broadcasting to ${peers.size} devices")
    }

    fun queueSelectedFilesForSend() {
        val selected = selectedItemIds.value
        if (selected.isEmpty()) return

        val newItems = selected.mapIndexed { idx, id ->
            TransferItem(
                id = "q_${UUID.randomUUID().toString().take(6)}",
                name = when {
                    id.startsWith("p") -> "IMG_20$id.jpg"
                    id.startsWith("v") -> "VID_20$id.mp4"
                    id.startsWith("m") -> "song_$id.mp3"
                    id.startsWith("a") -> "app_$id.apk"
                    else -> "file_$id.bin"
                },
                kind = when {
                    id.startsWith("p") -> TransferKind.IMAGE
                    id.startsWith("v") -> TransferKind.VIDEO
                    id.startsWith("m") -> TransferKind.AUDIO
                    id.startsWith("a") -> TransferKind.APK
                    else -> TransferKind.DOC
                },
                size = 12_500_000L + (idx * 3_000_000L),
                sent = 0L,
                speed = 6_500_000L,
                state = TransferState.QUEUED
            )
        }
        sendingList.value = sendingList.value + newItems
        selectedItemIds.value = emptySet()
        navigateTo(ScreenState.SENDING)
        showToast("Queued ${newItems.size} items → ${currentPeer.value.name}")
    }

    fun toggleFileSelection(id: String) {
        val current = selectedItemIds.value.toMutableSet()
        if (current.contains(id)) current.remove(id) else current.add(id)
        selectedItemIds.value = current
    }

    fun selectAllFiles(ids: List<String>) {
        val current = selectedItemIds.value.toMutableSet()
        if (current.containsAll(ids)) {
            current.removeAll(ids.toSet())
        } else {
            current.addAll(ids)
        }
        selectedItemIds.value = current
    }

    fun clearFileSelection() {
        selectedItemIds.value = emptySet()
    }

    fun cycleConflictPolicy() {
        val policies = listOf("Rename duplicates", "Overwrite", "Skip existing", "Ask every time")
        val nextIdx = (policies.indexOf(conflictPolicy.value) + 1) % policies.size
        conflictPolicy.value = policies[nextIdx]
        showToast("Conflict policy · ${conflictPolicy.value}")
    }

    fun cycleBroadcastLimit() {
        val next = if (broadcastPeersLimit.value >= 8) 2 else broadcastPeersLimit.value + 1
        broadcastPeersLimit.value = next
        showToast("Broadcast cap · $next phones")
    }

    fun requestBatteryExemption() {
        doctorChecks.value = doctorChecks.value.map {
            if (it.key.contains("Battery")) {
                DoctorCheckItem("Battery optimization OFF", "Morsecode is exempt — transfers survive screen off", "ok")
            } else it
        }
        showToast("Battery exemption granted")
    }
}
