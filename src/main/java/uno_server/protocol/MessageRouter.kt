package uno_server.protocol

import uno_proto.common.Method
import uno_proto.common.NetworkMessage
import uno_proto.dto.*
import uno_server.common.Connection
import uno_server.game.GameSessionManager
import java.util.logging.Level
import java.util.logging.Logger

/**
 * Routes incoming network messages to appropriate handlers.
 * This is the main entry point for all server-side message processing.
 */
class MessageRouter {
    private val logger = Logger.getLogger(MessageRouter::class.java.name)
    private val parser = MessageParser()
    private val gameManager = GameSessionManager()
    
    private val messageSender = MessageSender()
    private val connectionManager = ConnectionManager()
    private val roomManager = RoomManager(connectionManager, messageSender)
    private val gameHandler = GameHandler(connectionManager, roomManager, messageSender, gameManager)
    private val chatHandler = ChatHandler(connectionManager, roomManager, messageSender)
    private val pingHandler = PingHandler(messageSender)

    fun routeMessage(connection: Connection, rawMessage: String) {
        try {
            val message = parser.fromJson(rawMessage) ?: return
            logger.log(Level.INFO, "Received message: ${message.method} from ${connection.remoteAddress}")

            when (message.method) {
                Method.CREATE_ROOM -> roomManager.handleCreateRoom(connection, message.payload as CreateRoomRequest)
                Method.GET_ROOMS -> roomManager.handleGetRooms(connection)
                Method.JOIN_ROOM -> roomManager.handleJoinRoom(connection, message.payload as JoinRoomRequest)
                Method.LEAVE_ROOM -> roomManager.handleLeaveRoom(connection)
                Method.START_GAME -> gameHandler.handleStartGame(connection)
                Method.PLAY_CARD -> gameHandler.handlePlayCard(connection, message.payload as PlayCardRequest)
                Method.DRAW_CARD -> gameHandler.handleDrawCard(connection)
                Method.SAY_UNO -> gameHandler.handleSayUno(connection)
                Method.PING -> pingHandler.handlePing(connection)
                Method.LOBBY_CHAT -> chatHandler.handleLobbyChat(connection, message.payload as ChatMessage)
                Method.GAME_CHAT -> chatHandler.handleGameChat(connection, message.payload as ChatMessage)
                else -> messageSender.sendError(connection, "Unsupported method: ${message.method}", "UNSUPPORTED_METHOD")
            }
        } catch (e: Exception) {
            logger.log(Level.SEVERE, "Error routing message", e)
            messageSender.sendError(connection, "Error processing message: ${e.message}", "PROCESSING_ERROR")
        }
    }

    fun handleDisconnect(connection: Connection) {
        val player = connectionManager.removeUser(connection)
        if (player != null) {
            val roomId = player.currentRoomId
            roomManager.handleDisconnect(player.userId, roomId)
            gameHandler.handleDisconnect(roomId)
            logger.log(Level.INFO, "User ${player.userId} disconnected and cleaned up")
        }
    }
}
