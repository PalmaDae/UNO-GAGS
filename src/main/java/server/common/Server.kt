package server.common

import proto.dto.*
import server.game.GameSession
import server.game.PlayerState
import java.io.IOException
import java.net.ServerSocket
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import java.util.logging.Logger

class Server : AutoCloseable {

    private val serverSocket = ServerSocket(PORT)
    private val clientIndex = AtomicInteger(0)
    private val rooms = ConcurrentHashMap<Long, RoomOnServer>()
    private val users = ConcurrentHashMap<Long, PlayerOnServer>()
    private val nextRoomId = AtomicLong(1L)
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

        val initialPlayerStates = room.players.map { player ->
            PlayerState(
                playerId = player.id,
                username = player.username,
                avatar = player.avatar
            )
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

    private fun sendHandUpdate(room: RoomOnServer, playerId: Long) {
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

    private fun sendHandUpdates(room: RoomOnServer) {
        room.players.forEach { player ->
            sendHandUpdate(room, player.id)
        }
    }

    private fun broadcastGameState(room: RoomOnServer) {
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
                is StartGameRequest -> handleStartGame(connection, clientId, payload)
                is PlayCardRequest -> handlePlayCard(connection, clientId, payload)
                is DrawCardRequest -> handleDrawCard(connection, clientId, payload)
                is ChooseColorRequest -> handleChooseColor(connection, clientId, payload)
                is SayUnoRequest -> handleSayUno(connection, clientId, payload)
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
        val roomId = nextRoomId.getAndIncrement()
        val room = RoomOnServer(
            id = roomId,
            creatorId = clientId,
            password = request.password
        )
        rooms[roomId] = room

        val user = PlayerOnServer(clientId, request.username, request.avatar, connection)
        users[clientId] = user
        room.addPlayer(user)

        connection.sendMessage(
            CreateRoomResponse(
                roomId = roomId,
                password = request.password,
                isSuccessful = true
            )
        )
        broadcastRoomUpdate(roomId)
    }

    /*
        метод, описывающий, что реквест отправил овнер по id комнаты,
        т.к. другой юзер будет джойниться по паролю
    */
    private fun isOwnerJoinRoomRequest(request: JoinRoomRequest) =
        request.roomId != null && request.password == null

    private fun handleJoinRoom(connection: Connection, clientId: Long, request: JoinRoomRequest) {
        val isOwner = isOwnerJoinRoomRequest(request)
        val room = if (isOwner)
            rooms[request.roomId]
        else
            rooms.values.firstOrNull { it.password == request.password }

        if (room == null) {
            connection.sendMessage(ErrorMessage("Room not found"))
            return
        }

        val user = users.getOrPut(clientId) {
            PlayerOnServer(clientId, request.username, request.avatar, connection)
        }

        room.addPlayer(user)

        connection.sendMessage(
            JoinRoomResponse(
                roomId = room.id,
                isSuccessful = true
            )
        )
        broadcastRoomUpdate(room.id)
    }

    /*
        общая часть обработки в методах handleChooseColor и handlePlayCard, handleDrawCard и handleSayUno
     */
    private fun commonHandle(connection: Connection, clientId: Long, request: GameRequest, inTurn: Boolean): Pair<RoomOnServer, GameSession>? {
        val room = rooms[request.roomId] ?: run {
            connection.sendMessage(ErrorMessage("Room not found"))
            return null
        }

        val session = room.gameSession ?: run {
            connection.sendMessage(ErrorMessage("Game not started"))
            return null
        }

        if (inTurn && clientId != session.currentPlayerId) {
            connection.sendMessage(ErrorMessage("Not your turn"))
            return null
        }

        return room to session
    }

    private fun handleChooseColor(connection: Connection, clientId: Long, request: ChooseColorRequest) {
        val roomAndSession = commonHandle(connection, clientId, request, true)
        if (roomAndSession == null) return

        try {
            val room = roomAndSession.component1()
            val session = roomAndSession.component2()

            session.setChosenColor(request.chosenColor)
            connection.sendMessage(OkMessage("Color chosen successfully"))

            // First broadcast: DRAWING_CARD phase (clients can show transition)
            sendHandUpdates(room)
            broadcastGameState(room)

            // Small delay to let clients see DRAWING_CARD phase
            Thread.sleep(100)

            // Second broadcast: WAITING_TURN phase
            session.finishColorSelection()
            broadcastGameState(room)

        } catch (e: IllegalStateException) {
            connection.sendMessage(ErrorMessage(e.message ?: "Invalid color choice"))
        } catch (e: Exception) {
            logger.warning("Error choosing color: ${e.message}")
            connection.sendMessage(ErrorMessage("Server error during color choice"))
        }
    }

    private fun handlePlayCard(connection: Connection, clientId: Long, request: PlayCardRequest) {
        val roomAndSession = commonHandle(connection, clientId, request, true)
        if (roomAndSession == null) return

        try {
            val room = roomAndSession.component1()
            val session = roomAndSession.component2()

            session.playCard(clientId, request.cardIndex, request.chosenColor)
            connection.sendMessage(OkMessage("Card played successfully"))

            sendHandUpdates(room)
            broadcastGameState(room)

        } catch (e: IllegalStateException) {
            connection.sendMessage(ErrorMessage(e.message ?: "Invalid move"))
        } catch (e: Exception) {
            logger.warning("Error playing card: ${e.message}")
            connection.sendMessage(ErrorMessage("Server error during card play"))
        }
    }

    private fun handleDrawCard(connection: Connection, clientId: Long, request: DrawCardRequest) {
        val roomAndSession = commonHandle(connection, clientId, request, true)
        if (roomAndSession == null) return

        try {
            val room = roomAndSession.component1()
            val session = roomAndSession.component2()

            session.drawCard(clientId)

            // First broadcast: DRAWING_CARD phase
            sendHandUpdates(room)
            broadcastGameState(room)

            // Small delay to let clients see DRAWING_CARD phase
            Thread.sleep(100)

            session.finishDrawing(clientId)

            // Second broadcast: WAITING_TURN phase
            broadcastGameState(room)

            connection.sendMessage(OkMessage("Card drawn successfully"))

        } catch (e: IllegalStateException) {
            connection.sendMessage(ErrorMessage(e.message ?: "Invalid move"))
        } catch (e: Exception) {
            logger.warning("Error drawing card: ${e.message}")
            connection.sendMessage(ErrorMessage("Server error during draw card"))
        }
    }

    private fun handleSayUno(connection: Connection, clientId: Long, request: SayUnoRequest) {
        val roomAndSession = commonHandle(connection, clientId, request, false)
        if (roomAndSession == null) return

        if (roomAndSession.component2().players[clientId] == null) {
            connection.sendMessage(ErrorMessage("Player not in game"))
            return
        }

        try {
            roomAndSession.component2().sayUno(clientId)
            connection.sendMessage(OkMessage("UNO declared successfully"))

        } catch (e: IllegalStateException) {
            connection.sendMessage(ErrorMessage(e.message ?: "Could not declare UNO"))
        } catch (e: Exception) {
            logger.warning("Error declaring UNO: ${e.message}")
            connection.sendMessage(ErrorMessage("Server error during UNO declaration"))
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
                players = room.players.map { playerFromServer ->
                    PlayerInfo(
                        userId = playerFromServer.id,
                        username = playerFromServer.username,
                        avatar = playerFromServer.avatar,
                        isOwner = playerFromServer.id == room.creatorId,
                        hasUnoDeclared = false,
                        isReady = false,
                        cardCount = 0
                    )
                },
                roomStatus = if (room.gameStarted) RoomStatus.IN_PROGRESS else RoomStatus.WAITING
            )
            room.players.forEach { it.connection.sendMessage(update) }
        }
    }

    override fun close() {
        running = false
        serverSocket.close()
    }

    companion object {
        const val PORT = 9090
        private val logger = Logger.getLogger(Server::class.java.name)
    }
}

data class RoomOnServer(
    val id: Long,
    val creatorId: Long,
    val password: String?,
    val players: MutableList<PlayerOnServer> = mutableListOf(),
    var gameStarted: Boolean = false,
    var gameSession: GameSession? = null
) {
    fun addPlayer(user: PlayerOnServer) {
        if (!players.any { it.id == user.id }) {
            players.add(user)
        }
    }

    fun removePlayer(user: PlayerOnServer) = players.removeAll { it.id == user.id }
}

data class PlayerOnServer(
    val id: Long,
    val username: String,
    val avatar: String,
    val connection: Connection
)