package uno_server.protocol

import uno_server.common.Connection
import uno_server.Room

/**
 * Message sender for compatibility with client
 */
object MessageSender {
    fun sendError(connection: Connection, message: String, code: String) {
        // Convert to JSON format for client compatibility
        connection.sendLine("ERROR|$message")
    }

    fun sendToConnection(connection: Connection, message: String) {
        // Simple format for compatibility
        connection.sendLine("OK|$message")
    }

    fun broadcastToRoom(room: Room, message: String) {
        // Not implemented in simplified version
    }
}