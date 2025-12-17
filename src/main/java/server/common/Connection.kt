package server.common

import proto.common.Method
import proto.common.NetworkMessage
import proto.dto.Payload
import java.io.*
import java.net.Socket

class Connection(val socket: Socket) {
    private val output = ObjectOutputStream(socket.getOutputStream())
    private val input = ObjectInputStream(socket.getInputStream())

    fun readMessage(): Payload? {
        return try {
            val message = input.readObject() as? NetworkMessage
            message?.payload
        } catch (e: Exception) {
            println("[Server] Read Error: ${e.message}")
            null
        }
    }

    fun sendMessage(msg: Payload) {
        try {
            val wrapped = NetworkMessage(
                id = 0,
                method = Method.OK,
                payload = msg
            )
            output.writeObject(wrapped)
            output.flush()
        } catch (e: IOException) {
            println("[Server] Write Error: ${e.message}")
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