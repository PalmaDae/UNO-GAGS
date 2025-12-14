package server.protocol

import proto.common.Method
import server.common.Connection

/**
 * Handles PING/PONG messages.
 */
class PingHandler(private val messageSender: MessageSender) {
    
    fun handlePing(connection: Connection) {
        messageSender.send(connection, Method.PONG, MessageParser.EmptyPayload)
    }
}
