package uno_server.protocol

import uno_server.common.Connection
import java.util.logging.Level
import java.util.logging.Logger

/**
 * Простой обработчик чата в формате: CHAT_MESSAGE|playerName|text
 * Парсит сообщения и рассылает всем в комнате.
 */
class SimpleChatHandler(
    private val connectionManager: ConnectionManager,
    private val roomManager: RoomManager
) {
    private val logger = Logger.getLogger(SimpleChatHandler::class.java.name)

    /**
     * Парсит и обрабатывает простое текстовое сообщение формата: CHAT_MESSAGE|playerName|text
     */
    fun handleSimpleChatMessage(connection: Connection, message: String) {
        try {
            // Парсим формат: CHAT_MESSAGE|playerName|text
            val parts = message.split("|", limit = 3)
            
            if (parts.size < 3) {
                logger.log(Level.WARNING, "Invalid CHAT_MESSAGE format: $message")
                return
            }
            
            val command = parts[0]
            val playerName = parts[1]
            val text = parts[2]
            
            if (command != "CHAT_MESSAGE") {
                logger.log(Level.WARNING, "Expected CHAT_MESSAGE but got: $command")
                return
            }
            
            logger.log(Level.INFO, "Chat from $playerName: $text")
            
            // Получаем информацию о пользователе и комнате
            val userId = connectionManager.getUserId(connection)
            if (userId == null) {
                logger.log(Level.WARNING, "Chat message from unknown user")
                return
            }
            
            val player = connectionManager.getPlayer(userId)
            val roomId = player?.currentRoomId
            
            if (roomId == null) {
                logger.log(Level.WARNING, "User $userId not in any room")
                return
            }
            
            val room = roomManager.getRoom(roomId)
            if (room == null) {
                logger.log(Level.WARNING, "Room $roomId not found")
                return
            }
            
            // Рассылаем сообщение всем в комнате
            val broadcastMessage = "CHAT_MESSAGE|$playerName|$text"
            broadcastToRoom(room, broadcastMessage)
            
            logger.log(Level.INFO, "Broadcast chat to ${room.currentPlayerCount} players in room $roomId")
            
        } catch (e: Exception) {
            logger.log(Level.SEVERE, "Error handling simple chat message", e)
        }
    }
    
    /**
     * Отправляет текстовое сообщение всем игрокам в комнате
     */
    private fun broadcastToRoom(room: Room, message: String) {
        room.getPlayers().filterNotNull().forEach { playerConn ->
            try {
                playerConn.connection?.sendLine(message)
            } catch (e: Exception) {
                logger.log(Level.WARNING, "Failed to send chat to ${playerConn.userId}", e)
            }
        }
    }
}
