package com.example.core.transport

import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.Socket

enum class TransportKindType {
    LAN,
    NEARBY,
    WEBSHARE
}

interface TransportChannel {
    val transportType: TransportKindType
    val peerId: String
    val peerName: String
    val isConnected: Boolean

    suspend fun sendFrame(type: Byte, payload: ByteArray)
    suspend fun readFrame(): Pair<Byte, ByteArray>
    fun close()
}

class LanSocketTransportChannel(
    val socket: Socket,
    override val peerId: String,
    override val peerName: String
) : TransportChannel {

    override val transportType: TransportKindType = TransportKindType.LAN

    private val dis = DataInputStream(socket.getInputStream())
    private val dos = DataOutputStream(socket.getOutputStream())

    override val isConnected: Boolean
        get() = !socket.isClosed && socket.isConnected

    override suspend fun sendFrame(type: Byte, payload: ByteArray) {
        synchronized(dos) {
            dos.writeByte(type.toInt())
            dos.writeInt(payload.size)
            if (payload.isNotEmpty()) {
                dos.write(payload)
            }
            dos.flush()
        }
    }

    override suspend fun readFrame(): Pair<Byte, ByteArray> {
        val type = dis.readByte()
        val length = dis.readInt()
        val payload = ByteArray(length)
        if (length > 0) {
            dis.readFully(payload)
        }
        return Pair(type, payload)
    }

    override fun close() {
        try {
            socket.close()
        } catch (_: Exception) {
        }
    }
}
