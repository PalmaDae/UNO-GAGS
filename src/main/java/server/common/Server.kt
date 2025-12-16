package server.common

import proto.common.Payload
import proto.dto.*
import server.game.GameSession
import server.game.PlayerState
import java.io.IOException
import java.net.ServerSocket
import java.util.concurrent.atomic.AtomicInteger
import java.util.logging.Logger

class Server : AutoCloseable {
    companion object {
        private const val PORT = 9090
        private val logger = Logger.getLogger(Server::class.java.name)
    }

    private val serverSocket = ServerSocket(PORT)
    private val clientIndex = AtomicInteger(0)
    private val rooms = mutableMapOf<Long, ServerRoom>()
    private val users = mutableMapOf<Long, UserSession>()
    private var nextRoomId = 1L
    private var running = true

    init {
        serverSocket.reuseAddress = true
        logger.info("Server created on port $PORT")
    }

    fun listen() {
        logger.info("Server started on port ${serverSocket.localPort}")

        while (running) {
            try {
                val socket = serverSocket.accept()
                val connection = Connection(socket)
                val clientId = clientIndex.incrementAndGet().toLong()
                logger.info("Client #$clientId connected from ${connection.getRemoteAddress()}")

                Thread {
                    handleClient(connection, clientId)
                }.apply {
                    name = "client-$clientId"
                    isDaemon = true
                    start()
                }
            } catch (e: IOException) {
                if (running) {
                    logger.warning("Accept error: ${e.message}")
                }
            }
        }
    }

    private fun handleStartGame(connection: Connection, clientId: Long, request: StartGameRequest) {
        val room = rooms[request.roomId] ?: run {
            connection.sendMessage(ErrorMessage("Room not found"))
            return
        }

        if (room.creatorId != clientId) {
            connection.sendMessage(ErrorMessage("Only creator can start game"))
            return
        }

        if (room.players.size < 2) {
            connection.sendMessage(ErrorMessage("Need at least 2 players"))
            return
        }

        val initialPlayerStates = room.players.map { userSession ->
            PlayerState(userSession.id, userSession.name)
        }.toMutableList()

        val gameSession = GameSession(request.roomId, initialPlayerStates)
        room.gameSession = gameSession

        room.gameStarted = true
        connection.sendMessage(OkMessage("Game started"))

        room.players.forEach { player ->
            sendHandUpdate(room, player.id)
        }
        broadcastGameState(room)

        broadcastRoomUpdate(request.roomId)
    }

    private fun handleClient(connection: Connection, clientId: Long) {
        try {
            while (running) {
                val payload = connection.readMessage() ?: break
                logger.info("Client #$clientId received: ${payload::class.simpleName}")
                processPayload(connection, clientId, payload)
            }
        } catch (e: Exception) {
            logger.warning("Client #$clientId error: ${e.message}")
        } finally {
            handleDisconnect(connection, clientId)
            connection.close()
            logger.info("Client #$clientId disconnected")
        }
    }

    private fun sendHandUpdate(room: ServerRoom, playerId: Long) {
        room.gameSession?.let { session ->
            val playerState = session.players[playerId]

            if (playerState != null) {
                val update = PlayerHandUpdate(playerState.hand)

                room.players.firstOrNull { it.id == playerId }?.connection?.sendMessage(update)
            } else {
                logger.warning("Attempted to send hand update to unknown player: $playerId")
            }
        }
    }

    private fun broadcastGameState(room: ServerRoom) {
        room.gameSession?.let { session ->
            val gameState = session.gameState
            room.players.forEach { player ->
                player.connection.sendMessage(gameState)
            }
        }
    }

    private fun processPayload(connection: Connection, clientId: Long, payload: Payload) {
        try {
            when (payload) {
                is CreateRoomRequest -> handleCreateRoom(connection, clientId, payload)
                is JoinRoomRequest -> handleJoinRoom(connection, clientId, payload)
                is GetRoomsRequest -> handleGetRooms(connection)
                is StartGameRequest -> handleStartGame(connection, clientId, payload)
                is PlayCardRequest -> handlePlayCard(connection, clientId, payload)
                is DrawCardRequest -> handleDrawCard(connection, clientId, payload)
                is SayUnoRequest -> handleSayUno(connection, clientId, payload)
                is ChatMessage -> handleChat(connection, clientId, payload)
                is PingMessage -> connection.sendMessage(PongMessage())
                is PongMessage -> logger.info("Received PONG from client #$clientId")
                is LeaveRoomRequest -> handleLeaveRoom(connection, clientId, payload)
                else -> {
                    logger.warning("Unknown message type: ${payload::class.simpleName}")
                    connection.sendMessage(ErrorMessage("Unknown message type"))
                }
            }
        } catch (e: Exception) {
            logger.warning("Error processing payload: ${e.message}")
            connection.sendMessage(ErrorMessage("Server error: ${e.message}"))
        }
    }

    private fun handleCreateRoom(connection: Connection, clientId: Long, request: CreateRoomRequest) {
        val roomId = nextRoomId++
        val room = ServerRoom(roomId, request.roomName, clientId, request.password)
        rooms[roomId] = room

        val user = UserSession(clientId, "User$clientId", connection)
        users[clientId] = user
        room.addPlayer(user)

        connection.sendMessage(CreateRoomResponse(roomId, request.roomName, true))
        broadcastRoomUpdate(roomId)
    }

    private fun handleJoinRoom(connection: Connection, clientId: Long, request: JoinRoomRequest) {
        val room = rooms[request.roomId]
        if (room == null) {
            connection.sendMessage(ErrorMessage("Room not found"))
            return
        }

        if (room.password != null && room.password != request.password) {
            connection.sendMessage(ErrorMessage("Invalid password"))
            return
        }

        val user = users.getOrPut(clientId) { UserSession(clientId, "User$clientId", connection) }
        room.addPlayer(user)

        connection.sendMessage(JoinRoomResponse(request.roomId, true))
        broadcastRoomUpdate(request.roomId)
    }

    private fun handleGetRooms(connection: Connection) {
        val roomsList = rooms.values.map { room ->
            RoomInfo(
                roomId = room.id,
                roomName = room.name,
                hasPassword = room.password != null,
                maxPlayers = 4,
                currentPlayers = room.players.size,
                status = if (room.gameStarted) RoomStatus.IN_PROGRESS else RoomStatus.WAITING,
                creatorName = "Creator"
            )
        }
        connection.sendMessage(RoomsListPayload(roomsList))
    }

    private fun handlePlayCard(connection: Connection, clientId: Long, request: PlayCardRequest) {
        val room = rooms[request.roomId] ?: run {
            connection.sendMessage(ErrorMessage("Room not found"))
            return
        }

        val session = room.gameSession ?: run {
            connection.sendMessage(ErrorMessage("Game not started"))
            return
        }

        if (clientId != session.currentPlayerId) {
            connection.sendMessage(ErrorMessage("Not your turn"))
            return
        }

        try {
            session.playCard(clientId, request.cardIndex, request.chosenColor)
            connection.sendMessage(OkMessage("Card played successfully"))

            sendHandUpdate(room, clientId)
            broadcastGameState(room)

        } catch (e: IllegalStateException) {
            connection.sendMessage(ErrorMessage(e.message ?: "Invalid move"))
        } catch (e: Exception) {
            logger.warning("Error playing card: ${e.message}")
            connection.sendMessage(ErrorMessage("Server error during card play"))
        }
    }


    private fun handleDrawCard(connection: Connection, clientId: Long, request: DrawCardRequest) =
        connection.sendMessage(OkMessage("Card drawn"))

    private fun handleSayUno(connection: Connection, clientId: Long, request: SayUnoRequest) =
        connection.sendMessage(OkMessage("UNO declared"))


    private fun handleChat(connection: Connection, clientId: Long, message: ChatMessage) {
        rooms.values.forEach { room ->
            if (room.players.any { it.id == clientId }) {
                room.players.forEach { player ->
                    if (player.id != clientId) {
                        player.connection.sendMessage(message)
                    }
                }
            }
        }
    }

    private fun handleLeaveRoom(connection: Connection, clientId: Long, request: LeaveRoomRequest) {
        val room = rooms[request.roomId]
        if (room != null) {
            val user = users[clientId]
            if (user != null) {
                room.removePlayer(user)
                if (room.players.isEmpty()) {
                    rooms.remove(request.roomId)
                } else {
                    broadcastRoomUpdate(request.roomId)
                }
            }
        }
        connection.sendMessage(OkMessage("Left room"))
    }

    private fun handleDisconnect(connection: Connection, clientId: Long) {
        users.remove(clientId)?.let { user ->
            rooms.values.forEach { room ->
                room.removePlayer(user)
            }
        }
    }

    private fun broadcastRoomUpdate(roomId: Long) {
        rooms[roomId]?.let { room ->
            val update = LobbyUpdate(
                players = room.players.map { PlayerInfo(it.id, it.name, it.id == room.creatorId, false) },
                roomStatus = if (room.gameStarted) RoomStatus.IN_PROGRESS else RoomStatus.WAITING
            )
            room.players.forEach { player ->
                player.connection.sendMessage(update)
            }
        }
    }

    override fun close() {
        running = false
        serverSocket.close()
    }
}

data class ServerRoom(
    val id: Long,
    val name: String,
    val creatorId: Long,
    val password: String?,
    val players: MutableList<UserSession> = mutableListOf(),
    var gameStarted: Boolean = false,
    var gameSession: GameSession? = null
) {
    fun addPlayer(user: UserSession) {
        if (!players.any { it.id == user.id }) {
            players.add(user)
        }
    }

    fun removePlayer(user: UserSession) = players.removeAll { it.id == user.id }
}

data class UserSession(
    val id: Long,
    val name: String,
    val connection: Connection
)