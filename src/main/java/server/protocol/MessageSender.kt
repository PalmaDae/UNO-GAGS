package server.protocol

import proto.common.Method
import proto.common.NetworkMessage
import proto.common.Payload
import proto.common.Version
import server.common.Connection
import java.io.IOException
import java.util.concurrent.atomic.AtomicLong
import java.util.logging.Level
import java.util.logging.Logger

/**
 * Handles sending messages to connections.
 */
class MessageSender {
    private val logger = Logger.getLogger(MessageSender::class.java.name)
    private val parser = MessageParser()
    private val messageIdGenerator = AtomicLong(1)

    fun send(connection: Connection, method: Method, payload: Payload) {
        try {
            val message = NetworkMessage(
                messageIdGenerator.getAndIncrement(),
                Version.V1,
                method,
                payload,
                System.currentTimeMillis()
            )
            val json = parser.toJson(message) ?: return
            connection.sendLine(json)
        } catch (e: IOException) {
            logger.log(Level.SEVERE, "Error sending message", e)
        }
    }

    fun sendError(connection: Connection, errorMessage: String, errorCode: String) {
        val payload = MessageParser.ErrorPayload(errorMessage, errorCode)
        send(connection, Method.ERROR, payload)
    }

    fun broadcastToRoom(room: Room, method: Method, payload: Payload) {
        room.getPlayers().filterNotNull().forEach { player ->
            player.connection?.let { conn ->
                send(conn, method, payload)
            }
        }
    }
}
