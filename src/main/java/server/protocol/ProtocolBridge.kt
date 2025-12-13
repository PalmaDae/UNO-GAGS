package uno_server.protocol

import uno_proto.common.NetworkMessage
import uno_proto.common.Method
import uno_proto.dto.*
import uno_server.common.Connection
import java.util.logging.Level
import java.util.logging.Logger

/**
 * Protocol bridge that handles both JSON and text protocols
 * Routes messages to appropriate handlers
 */
class ProtocolBridge {
    private val logger = Logger.getLogger(ProtocolBridge::class.java.name)
    private val connectionManager = ConnectionManager()
    private val roomManager = uno_server.RoomManager()

    fun handleMessage(connection: Connection, rawMessage: String, clientId: Int) {
        try {
            // Detect message format
            if (rawMessage.startsWith("{") || rawMessage.contains("\"method\"")) {
                // JSON format - convert to text protocol
                handleJsonMessage(connection, rawMessage, clientId)
            } else {
                // Text protocol format
                handleTextMessage(connection, rawMessage, clientId)
            }
        } catch (e: Exception) {
            logger.log(Level.SEVERE, "Error handling message", e)
            connection.sendLine("ERROR|Message processing failed: ${e.message}")
        }
    }

    private fun handleJsonMessage(connection: Connection, jsonMessage: String, clientId: Int) {
        val message = MessageParser.fromJson(jsonMessage) ?: run {
            connection.sendLine("ERROR|Invalid JSON message")
            return
        }

        logger.info("JSON message received: ${message.method}")

        when (message.method) {
            Method.CREATE_ROOM -> handleCreateRoom(connection, clientId)
            Method.JOIN_ROOM -> handleJoinRoom(connection, clientId)
            Method.LEAVE_ROOM -> handleLeaveRoom(connection, clientId)
            Method.START_GAME -> handleStartGame(connection, clientId)
            Method.PLAY_CARD -> handlePlayCard(connection, clientId)
            Method.DRAW_CARD -> handleDrawCard(connection, clientId)
            Method.SAY_UNO -> handleSayUno(connection, clientId)
            Method.GET_ROOMS -> handleGetRooms(connection)
            Method.PING -> connection.sendLine("OK|PONG")
            else -> connection.sendLine("ERROR|Unsupported method: ${message.method}")
        }
    }

    private fun handleTextMessage(connection: Connection, textMessage: String, clientId: Int) {
        val parts = textMessage.split("|")
        when {
            textMessage.startsWith("CREATE_ROOM|") -> handleCreateRoom(connection, clientId, parts.getOrNull(1) ?: "Room")
            textMessage.startsWith("JOIN_ROOM|") -> handleJoinRoom(connection, clientId, parts.getOrNull(1) ?: "", parts.getOrNull(2) ?: "Player$clientId")
            textMessage.startsWith("LEAVE_ROOM|") -> handleLeaveRoom(connection, clientId)
            textMessage.startsWith("START_GAME|") -> handleStartGame(connection, clientId)
            textMessage.startsWith("PLAY_CARD|") -> handlePlayCard(connection, clientId)
            textMessage.startsWith("DRAW_CARD") -> handleDrawCard(connection, clientId)
            textMessage.startsWith("SAY_UNO") -> handleSayUno(connection, clientId)
            textMessage.startsWith("GET_ROOMS") -> handleGetRooms(connection)
            textMessage.startsWith("CHAT_MESSAGE|") -> handleChatMessage(connection, clientId, parts.drop(1).joinToString("|"))
            textMessage.startsWith("GET_STATE") -> handleGetState(connection, clientId)
            else -> connection.sendLine("ERROR|Unknown command")
        }
    }

    private fun handleCreateRoom(connection: uno_server.common.Connection, clientId: Int, roomName: String = "Room") {
        val roomId = roomManager.createRoom(roomName)
        connection.sendLine("OK|Room created: $roomId")
    }

    private fun handleJoinRoom(connection: uno_server.common.Connection, clientId: Int, roomId: String = "", playerName: String = "Player$clientId") {
        if (roomId.isEmpty()) {
            connection.sendLine("ERROR|Room ID required")
            return
        }

        if (roomManager.joinRoom(roomId, playerName, clientId, connection)) {
            connection.sendLine("OK|Joined room: $roomId")
            connection.sendLine("LOBBY_UPDATE|${roomManager.getRoom(roomId)?.getPlayerNames()?.joinToString(",") ?: ""}")
        } else {
            connection.sendLine("ERROR|Cannot join room: $roomId")
        }
    }

    private fun handleLeaveRoom(connection: uno_server.common.Connection, clientId: Int) {
        val roomId = roomManager.getPlayerRoom(clientId) ?: run {
            connection.sendLine("ERROR|Not in room")
            return
        }

        if (roomManager.leaveRoom(roomId, clientId)) {
            connection.sendLine("OK|Left room: $roomId")
        } else {
            connection.sendLine("ERROR|Cannot leave room")
        }
    }

    private fun handleStartGame(connection: uno_server.common.Connection, clientId: Int) {
        val roomId = roomManager.getPlayerRoom(clientId) ?: run {
            connection.sendLine("ERROR|Not in room")
            return
        }

        val room = roomManager.getRoom(roomId) ?: run {
            connection.sendLine("ERROR|Room not found")
            return
        }

        if (clientId != room.creatorId) {
            connection.sendLine("ERROR|Only creator can start game")
            return
        }

        if (room.getPlayerCount() < 2) {
            connection.sendLine("ERROR|Need at least 2 players")
            return
        }

        room.isStarted = true
        connection.sendLine("OK|Game started")
        
        // Notify all players
        room.players.forEach { p ->
            p.connection.sendLine("GAME_STATE|Game started in room $roomId")
        }
    }

    private fun handlePlayCard(connection: uno_server.common.Connection, clientId: Int) {
        val roomId = roomManager.getPlayerRoom(clientId) ?: run {
            connection.sendLine("ERROR|Not in room")
            return
        }

        connection.sendLine("OK|Card played")
        broadcastToRoom(roomId, "GAME_STATE|Player $clientId played a card")
    }

    private fun handleDrawCard(connection: uno_server.common.Connection, clientId: Int) {
        val roomId = roomManager.getPlayerRoom(clientId) ?: run {
            connection.sendLine("ERROR|Not in room")
            return
        }

        connection.sendLine("OK|Card drawn")
        broadcastToRoom(roomId, "GAME_STATE|Player $clientId drew a card")
    }

    private fun handleSayUno(connection: uno_server.common.Connection, clientId: Int) {
        val roomId = roomManager.getPlayerRoom(clientId) ?: run {
            connection.sendLine("ERROR|Not in room")
            return
        }

        connection.sendLine("OK|UNO declared")
        broadcastToRoom(roomId, "GAME_STATE|Player $clientId said UNO!")
    }

    private fun handleGetRooms(connection: uno_server.common.Connection) {
        val rooms = roomManager.rooms.keys.joinToString(",")
        connection.sendLine("ROOMS|$rooms")
    }

    private fun handleChatMessage(connection: uno_server.common.Connection, clientId: Int, message: String) {
        val roomId = roomManager.getPlayerRoom(clientId) ?: return
        broadcastToRoom(roomId, "CHAT|Player$clientId: $message")
    }

    private fun handleGetState(connection: uno_server.common.Connection, clientId: Int) {
        val roomId = roomManager.getPlayerRoom(clientId) ?: run {
            connection.sendLine("STATE|Not in room")
            return
        }

        val room = roomManager.getRoom(roomId) ?: run {
            connection.sendLine("STATE|Room not found")
            return
        }

        if (room.isStarted) {
            connection.sendLine("GAME_STATE|Game in progress")
        } else {
            connection.sendLine("LOBBY_UPDATE|${room.getPlayerNames().joinToString(",")}")
        }
    }

    private fun broadcastToRoom(roomId: String, message: String) {
        roomManager.getRoom(roomId)?.players?.forEach { player ->
            player.connection.sendLine(message)
        }
    }

    fun handleDisconnect(connection: Connection, clientId: Int) {
        connectionManager.removeUser(connection)?.let { userId ->
            val roomId = roomManager.getPlayerRoom(userId)
            if (roomId != null) {
                roomManager.leaveRoom(roomId, userId)
                broadcastToRoom(roomId, "CHAT|Player $userId disconnected")
            }
        }
    }
}