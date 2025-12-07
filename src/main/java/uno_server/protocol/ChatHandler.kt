package uno_server.protocol

import uno_proto.common.Method
import uno_proto.dto.ChatMessage
import uno_server.common.Connection
import java.util.logging.Level
import java.util.logging.Logger

/**
 * Handles chat messages in lobbies and games.
 */
class ChatHandler(
    private val connectionManager: ConnectionManager,
    private val roomManager: RoomManager,
    private val messageSender: MessageSender
) {
    private val logger = Logger.getLogger(ChatHandler::class.java.name)

    fun handleLobbyChat(connection: Connection, chatMessage: ChatMessage) {
        val userId = connectionManager.getUserId(connection)
        if (userId == null) {
            messageSender.sendError(connection, "User not found", "USER_NOT_FOUND")
            return
        }

        try {
            val player = connectionManager.getPlayer(userId)!!
            val roomId = player.currentRoomId

            if (roomId != null) {
                val room = roomManager.getRoom(roomId)
                if (room != null) {
                    messageSender.broadcastToRoom(room, Method.LOBBY_CHAT, chatMessage)
                }
            }
        } catch (e: Exception) {
            logger.log(Level.SEVERE, "Error handling lobby chat", e)
        }
    }

    fun handleGameChat(connection: Connection, chatMessage: ChatMessage) {
        val userId = connectionManager.getUserId(connection)
        if (userId == null) {
            messageSender.sendError(connection, "User not found", "USER_NOT_FOUND")
            return
        }

        try {
            val player = connectionManager.getPlayer(userId)!!
            val roomId = player.currentRoomId

            if (roomId != null) {
                val room = roomManager.getRoom(roomId)
                if (room != null) {
                    messageSender.broadcastToRoom(room, Method.GAME_CHAT, chatMessage)
                }
            }
        } catch (e: Exception) {
            logger.log(Level.SEVERE, "Error handling game chat", e)
        }
    }
}
