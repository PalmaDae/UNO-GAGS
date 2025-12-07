package uno_server.protocol

import uno_proto.common.Method
import uno_proto.dto.GameState
import uno_proto.dto.PlayCardRequest
import uno_proto.dto.RoomStatus
import uno_server.common.Connection
import uno_server.game.GameSessionManager
import uno_server.game.PlayerState
import java.util.logging.Level
import java.util.logging.Logger

/**
 * Handles game-related actions: start, play, draw, uno.
 */
class GameHandler(
    private val connectionManager: ConnectionManager,
    private val roomManager: RoomManager,
    private val messageSender: MessageSender,
    private val gameManager: GameSessionManager
) {
    private val logger = Logger.getLogger(GameHandler::class.java.name)

    fun handleStartGame(connection: Connection) {
        val userId = connectionManager.getUserId(connection)
        if (userId == null) {
            messageSender.sendError(connection, "User not found", "USER_NOT_FOUND")
            return
        }

        try {
            val player = connectionManager.getPlayer(userId)!!
            val roomId = player.currentRoomId

            if (roomId == null) {
                messageSender.sendError(connection, "Not in a room", "NOT_IN_ROOM")
                return
            }

            val room = roomManager.getRoom(roomId)
            if (room == null) {
                messageSender.sendError(connection, "Room not found", "ROOM_NOT_FOUND")
                return
            }

            if (room.creatorId != userId) {
                messageSender.sendError(connection, "Only room creator can start the game", "NOT_CREATOR")
                return
            }

            if (room.currentPlayerCount < 2) {
                messageSender.sendError(connection, "Need at least 2 players to start", "NOT_ENOUGH_PLAYERS")
                return
            }

            val gamePlayers = room.getPlayers().filterNotNull().map { p ->
                PlayerState(p.userId, p.username ?: "")
            }

            val session = gameManager.createSession(roomId, gamePlayers)
            room.status = RoomStatus.IN_PROGRESS

            val gameState = session.gameState
            messageSender.broadcastToRoom(room, Method.GAME_START, gameState)

            logger.log(Level.INFO, "Game started in room $roomId")
        } catch (e: Exception) {
            logger.log(Level.SEVERE, "Error starting game", e)
            messageSender.sendError(connection, "Failed to start game: ${e.message}", "START_GAME_ERROR")
        }
    }

    fun handlePlayCard(connection: Connection, request: PlayCardRequest) {
        val userId = connectionManager.getUserId(connection)
        if (userId == null) {
            messageSender.sendError(connection, "User not found", "USER_NOT_FOUND")
            return
        }

        try {
            val player = connectionManager.getPlayer(userId)!!
            val roomId = player.currentRoomId

            if (roomId == null) {
                messageSender.sendError(connection, "Not in a room", "NOT_IN_ROOM")
                return
            }

            val gameState = gameManager.playCard(roomId, userId, request.cardIndex, request.chosenColor)
            val room = roomManager.getRoom(roomId)
            if (room != null) {
                messageSender.broadcastToRoom(room, Method.GAME_STATE, gameState)
            }

            logger.log(Level.INFO, "User $userId played card in room $roomId")
        } catch (e: IllegalStateException) {
            logger.log(Level.WARNING, "Invalid card play", e)
            messageSender.sendError(connection, e.message ?: "Invalid play", "INVALID_PLAY")
        } catch (e: IllegalArgumentException) {
            logger.log(Level.WARNING, "Invalid card play", e)
            messageSender.sendError(connection, e.message ?: "Invalid play", "INVALID_PLAY")
        } catch (e: Exception) {
            logger.log(Level.SEVERE, "Error playing card", e)
            messageSender.sendError(connection, "Failed to play card: ${e.message}", "PLAY_CARD_ERROR")
        }
    }

    fun handleDrawCard(connection: Connection) {
        val userId = connectionManager.getUserId(connection)
        if (userId == null) {
            messageSender.sendError(connection, "User not found", "USER_NOT_FOUND")
            return
        }

        try {
            val player = connectionManager.getPlayer(userId)!!
            val roomId = player.currentRoomId

            if (roomId == null) {
                messageSender.sendError(connection, "Not in a room", "NOT_IN_ROOM")
                return
            }

            val gameState = gameManager.drawCard(roomId, userId)
            val room = roomManager.getRoom(roomId)
            if (room != null) {
                messageSender.broadcastToRoom(room, Method.GAME_STATE, gameState)
            }

            logger.log(Level.INFO, "User $userId drew a card in room $roomId")
        } catch (e: IllegalStateException) {
            logger.log(Level.WARNING, "Invalid draw", e)
            messageSender.sendError(connection, e.message ?: "Invalid draw", "INVALID_DRAW")
        } catch (e: IllegalArgumentException) {
            logger.log(Level.WARNING, "Invalid draw", e)
            messageSender.sendError(connection, e.message ?: "Invalid draw", "INVALID_DRAW")
        } catch (e: Exception) {
            logger.log(Level.SEVERE, "Error drawing card", e)
            messageSender.sendError(connection, "Failed to draw card: ${e.message}", "DRAW_CARD_ERROR")
        }
    }

    fun handleSayUno(connection: Connection) {
        val userId = connectionManager.getUserId(connection)
        if (userId == null) {
            messageSender.sendError(connection, "User not found", "USER_NOT_FOUND")
            return
        }

        try {
            val player = connectionManager.getPlayer(userId)!!
            val roomId = player.currentRoomId

            if (roomId == null) {
                messageSender.sendError(connection, "Not in a room", "NOT_IN_ROOM")
                return
            }

            val gameState = gameManager.sayUno(roomId, userId)
            val room = roomManager.getRoom(roomId)
            if (room != null) {
                messageSender.broadcastToRoom(room, Method.GAME_STATE, gameState)
            }

            logger.log(Level.INFO, "User $userId said UNO in room $roomId")
        } catch (e: Exception) {
            logger.log(Level.SEVERE, "Error saying UNO", e)
            messageSender.sendError(connection, "Failed to say UNO: ${e.message}", "SAY_UNO_ERROR")
        }
    }

    fun handleDisconnect(roomId: Long?) {
        if (roomId != null) {
            gameManager.removeSession(roomId)
        }
    }
}
