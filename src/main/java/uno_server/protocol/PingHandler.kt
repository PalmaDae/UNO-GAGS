package uno_server.protocol

import uno_proto.common.Method
import uno_server.common.Connection

/**
 * Handles PING/PONG messages.
 */
class PingHandler(private val messageSender: MessageSender) {
    
    fun handlePing(connection: Connection) {
        messageSender.send(connection, Method.PONG, MessageParser.EmptyPayload)
    }
}
