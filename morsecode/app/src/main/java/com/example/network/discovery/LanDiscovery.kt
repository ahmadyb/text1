package com.example.network.discovery

import android.content.Context
import android.net.wifi.WifiManager
import com.example.core.model.Peer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress

class LanDiscovery(private val context: Context) {

    private val _discoveredPeers = MutableStateFlow<Map<String, Peer>>(emptyMap())
    val discoveredPeers: StateFlow<Map<String, Peer>> = _discoveredPeers.asStateFlow()

    private var multicastLock: WifiManager.MulticastLock? = null
    private var discoveryJob: Job? = null
    private var broadcastJob: Job? = null
    private var socket: DatagramSocket? = null

    val isMulticastLockHeld: Boolean
        get() = multicastLock?.isHeld == true

    fun startDiscovery(deviceName: String, deviceId: String, port: Int = 33456) {
        stopDiscovery()

        try {
            val wifi = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            multicastLock = wifi?.createMulticastLock("morsecode_multicast_lock")?.apply {
                setReferenceCounted(true)
                acquire()
            }
        } catch (_: Exception) {
        }

        try {
            socket = DatagramSocket(null).apply {
                reuseAddress = true
                broadcast = true
                bind(InetSocketAddress(DISCOVERY_PORT))
            }
        } catch (_: Exception) {
            try {
                socket = DatagramSocket(DISCOVERY_PORT).apply {
                    broadcast = true
                }
            } catch (_: Exception) {
            }
        }

        // Listener coroutine
        discoveryJob = CoroutineScope(Dispatchers.IO).launch {
            val buffer = ByteArray(1024)
            while (isActive) {
                try {
                    val s = socket ?: break
                    val packet = DatagramPacket(buffer, buffer.size)
                    s.receive(packet)
                    val message = String(packet.data, 0, packet.length).trim()
                    val hostAddress = packet.address.hostAddress ?: ""

                    // Packet format: MORSE|NAME|ID|PORT|BADGE
                    val parts = message.split("|")
                    if (parts.size >= 4 && parts[0] == "MORSE") {
                        val peerName = parts[1]
                        val peerId = parts[2]
                        val peerPort = parts[3].toIntOrNull() ?: 33456
                        val badge = if (parts.size >= 5) parts[4] else "LAN"

                        if (peerId != deviceId) {
                            val colors = listOf("#8B5CF6", "#0EA5E9", "#22C55E", "#EA580C")
                            val colorHex = colors[Math.abs(peerId.hashCode()) % colors.size]

                            val peer = Peer(
                                id = peerId,
                                name = peerName,
                                letter = peerName.take(1).uppercase(),
                                colorHex = colorHex,
                                sub = "$hostAddress · Phone · $badge",
                                badge = badge,
                                ip = hostAddress,
                                port = peerPort
                            )
                            val current = _discoveredPeers.value.toMutableMap()
                            current[peerId] = peer
                            _discoveredPeers.value = current
                        }
                    }
                } catch (_: Exception) {
                }
            }
        }

        // Beacon advertiser coroutine (every 1200ms)
        broadcastJob = CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                try {
                    val s = socket
                    if (s != null && !s.isClosed) {
                        val beaconMsg = "MORSE|$deviceName|$deviceId|$port|LAN"
                        val bytes = beaconMsg.toByteArray()
                        val broadcastAddr = InetAddress.getByName("255.255.255.255")
                        val packet = DatagramPacket(bytes, bytes.size, broadcastAddr, DISCOVERY_PORT)
                        s.send(packet)
                    }
                } catch (_: Exception) {
                }
                delay(BEACON_INTERVAL_MS)
            }
        }
    }

    fun stopDiscovery() {
        discoveryJob?.cancel()
        broadcastJob?.cancel()
        discoveryJob = null
        broadcastJob = null

        try {
            socket?.close()
        } catch (_: Exception) {
        }
        socket = null

        try {
            if (multicastLock?.isHeld == true) {
                multicastLock?.release()
            }
        } catch (_: Exception) {
        }
        multicastLock = null
    }

    fun addDiscoveredPeer(peer: Peer) {
        val current = _discoveredPeers.value.toMutableMap()
        current[peer.id] = peer
        _discoveredPeers.value = current
    }

    companion object {
        const val DISCOVERY_PORT = 33457
        const val BEACON_INTERVAL_MS = 1200L
    }
}
