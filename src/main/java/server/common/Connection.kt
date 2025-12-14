package server.common

import proto.common.Payload
import java.io.*
import java.net.Socket

class Connection(val socket: Socket) {
    private val output = ObjectOutputStream(socket.getOutputStream())
    private val input = ObjectInputStream(socket.getInputStream())

    fun readMessage(): Payload? {
        return try {
            input.readObject() as? Payload
        } catch (e: IOException) {
            null
        } catch (e: ClassNotFoundException) {
            null
        }
    }

    fun sendMessage(msg: Payload) {
        try {
            output.writeObject(msg)
            output.flush()
        } catch (e: IOException) {
            // Connection closed
        }
    }

    fun close() {
        try {
            input.close()
            output.close()
            socket.close()
        } catch (e: IOException) {
            // Ignore close errors
        }
    }

    fun getRemoteAddress() = socket.remoteSocketAddress.toString()
}