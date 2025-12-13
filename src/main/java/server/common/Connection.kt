package server.common

import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.Socket

/**
 * Simple connection wrapper for I/O operations
 */
class Connection(val socket: Socket) {
    private val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
    private val writer = PrintWriter(socket.getOutputStream(), true)

    fun sendLine(msg: String) {
        writer.println(msg)
    }

    fun readLine(): String? {
        return try {
            reader.readLine()
        } catch (e: IOException) {
            null
        }
    }

    fun close() {
        try {
            reader.close()
            writer.close()
            socket.close()
        } catch (e: IOException) {
            // Ignore close errors
        }
    }

    fun getRemoteAddress(): String {
        return socket.remoteSocketAddress.toString()
    }
}