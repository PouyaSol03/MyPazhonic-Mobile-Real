package com.example.mypazhonictest.panel

import java.io.InputStream
import java.io.OutputStream
import java.net.Socket
import java.nio.charset.Charset
import javax.inject.Inject

/**
 * Reusable TCP client for panel communication.
 * Connect → send data → receive response → disconnect.
 */
class PanelTcpClient @Inject constructor() {

    private var socket: Socket? = null
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null

    companion object {
        private const val CONNECT_TIMEOUT_MS = 10_000
        private const val SO_TIMEOUT_MS = 15_000
        private val CHARSET = Charset.defaultCharset()
    }

    /**
     * Connect to panel at given host and port.
     */
    @Throws(Exception::class)
    fun connect(host: String, port: Int) {
        disconnect()
        socket = Socket(host, port).apply {
            soTimeout = SO_TIMEOUT_MS
        }
        inputStream = socket!!.getInputStream()
        outputStream = socket!!.getOutputStream()
    }

    /**
     * Send [data] and read response until newline or end of stream.
     * Returns the response as string (trimmed).
     */
    @Throws(Exception::class)
    fun sendAndReceive(data: String): String {
        val out = outputStream ?: throw IllegalStateException("Not connected")
        val input = inputStream ?: throw IllegalStateException("Not connected")
        out.write(data.toByteArray(CHARSET))
        out.flush()
        val buffer = mutableListOf<Byte>()
        var b: Int
        while (input.read().also { b = it } != -1) {
            buffer.add(b.toByte())
            if (b == '\n'.code || b == '\r'.code) break
        }
        return String(buffer.toByteArray(), CHARSET).trim()
    }

    fun disconnect() {
        try {
            inputStream?.close()
        } catch (_: Exception) { }
        try {
            outputStream?.close()
        } catch (_: Exception) { }
        try {
            socket?.close()
        } catch (_: Exception) { }
        socket = null
        inputStream = null
        outputStream = null
    }

    fun isConnected(): Boolean = socket?.isConnected == true
}
