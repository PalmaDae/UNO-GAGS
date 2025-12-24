package server.common

import proto.dto.*
import server.game.*
import java.io.IOException
import java.net.ServerSocket
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import java.util.logging.Logger

/*
 * класс, описывающий игровой сервер
 */
class Server : AutoCloseable {

    /*
     * поля: серверный сокет,
     * потокобезопасный индекс клиента,
     * потокобезопасные мапы пользователей и игровых комнат,
     * потокобезопасный индекс комнат, и потокобезопасное свойство запущенности сервера
     */

    private val serverSocket = ServerSocket(PORT)
    private val clientIndex = AtomicInteger(0)
    private val rooms = ConcurrentHashMap<Long, GameRoom>()
    private val users = ConcurrentHashMap<Long, PlayerState>()
    private val nextRoomId = AtomicLong(1L)
    @Volatile
    private var running = true

    init {
        serverSocket.reuseAddress = true
        logger.info("Server created on port $PORT")
    }

    // метод сервера, вызываемый извне
    fun listen() {
        logger.info("Server started on port ${serverSocket.localPort}")

        while (running) {
            try {
                // блокирующая операция ожидания клиента
                val socket = serverSocket.accept()
                // заворачиваем клиента в Connection
                val connection = Connection(socket)
                // выдаём уникальный номер клиенту
                val clientId = clientIndex.incrementAndGet().toLong()
                logger.info("Client #$clientId connected from ${connection.getRemoteAddress()}")

                // вся обработка клиента идёт в отдельном потоке сервера
                Thread {
                    handleClient(connection, clientId)
                }.apply {
                    // настройка клиентского треда
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

    // основной метод обработки клиента
    private fun handleClient(connection: Connection, clientId: Long) {
        try {
            while (running) {
                // клиентская обёртка читает сообщения
                val payload = connection.readMessage() ?: break
                logger.info("Client #$clientId received: ${payload::class.simpleName}")
                // передаём клиентское сообщения следующему методу
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

    // свич-кейс по нагрузке сообщения
    // делегируем каждый тип сообщения определённому методу
    private fun processPayload(connection: Connection, clientId: Long, payload: Payload) {
        try {
            when (payload) {
                is CreateRoomRequest -> handleCreateRoom(connection, clientId, payload)
                is FinishDrawingRequest -> handleFinishDrawing(connection, clientId, payload)
                is JoinRoomRequest -> handleJoinRoom(connection, clientId, payload)
                is StartGameRequest -> handleStartGame(connection, clientId, payload)
                is PlayCardRequest -> handlePlayCard(connection, clientId, payload)
                is DrawCardRequest -> handleDrawCard(connection, clientId, payload)
                is ChooseColorRequest -> handleChooseColor(connection, clientId, payload)
                is SayUnoRequest -> handleSayUno(connection, clientId, payload)
                is PingMessage -> connection.sendMessage(PongMessage())
                is PongMessage -> logger.info("Received PONG from client #$clientId")
                is LeaveRoomRequest -> handleLeaveRoom(connection, clientId, payload)
                // сервер умеет обрабатывать только верние сообщения, остальное он не понимает
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

    private fun handleStartGame(connection: Connection, clientId: Long, request: StartGameRequest) {
        val room = rooms[request.roomId] ?: run {
            connection.sendMessage(ErrorMessage("Room not found"))
            return
        }

        logger.info("Start request from client #$clientId. Room creator is #${room.creatorId}")

        if (room.creatorId != clientId) {
            logger.warning("Access denied: Client #$clientId is not the owner of room ${room.id}")
            connection.sendMessage(ErrorMessage("Only creator can start game"))
            return
        }

        if (room.players.size < 2) {
            connection.sendMessage(ErrorMessage("Need at least 2 players"))
            return
        }

        val initialPlayerStates = room.players.map { player ->
            PlayerState(
                id = player.id,
                username = player.username,
                avatar = player.avatar,
                connection = player.connection,
            )
        }.toMutableList()

        val gameSession = GameSession(request.roomId, initialPlayerStates, false)
        room.gameSession = gameSession

        room.gameStarted = true
        connection.sendMessage(OkMessage("Game started"))

        room.players.forEach { player ->
            sendHandUpdate(room, player.id)
        }
        broadcastGameState(room)

        broadcastRoomUpdate(request.roomId)
    }

    private fun sendHandUpdate(room: GameRoom, playerId: Long) {
        room.gameSession?.let { session ->
            val playerState = session.players[playerId]

            if (playerState != null) {
                val update = PlayerHandUpdate(playerState.hand.toList())

                room.players.firstOrNull { it.id == playerId }?.connection?.sendMessage(update)
            } else {
                logger.warning("Attempted to send hand update to unknown player: $playerId")
            }
        }
    }

    private fun sendHandUpdates(room: GameRoom) {
        room.players.forEach { player ->
            sendHandUpdate(room, player.id)
        }
    }

    private fun broadcastGameState(room: GameRoom) {
        room.gameSession?.let { session ->
            val gameState = session.gameState
            room.players.forEach { player ->
                player.connection.sendMessage(gameState)
            }
        }
    }

    private fun handleFinishDrawing(connection: Connection, clientId: Long, request: FinishDrawingRequest) {
        val roomAndSession = commonHandle(connection, clientId, request, true) ?: return
        val room = roomAndSession.first
        val session = roomAndSession.second

        try {
            session.finishDrawing(clientId)
            broadcastGameState(room)
            connection.sendMessage(OkMessage("Turn finished"))
        } catch (e: Exception) {
            connection.sendMessage(ErrorMessage(e.message ?: "Error finishing drawing"))
        }
    }

    private fun handleCreateRoom(connection: Connection, clientId: Long, request: CreateRoomRequest) {
        val roomId = nextRoomId.getAndIncrement()
        val room = GameRoom(
            id = roomId,
            creatorId = clientId,
            password = request.password
        )
        rooms[roomId] = room

        val user = PlayerState(clientId, request.username, request.avatar, connection)
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
            PlayerState(clientId, request.username, request.avatar, connection)
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
    private fun commonHandle(
        connection: Connection,
        clientId: Long,
        request: GameRequest,
        inTurn: Boolean
    ): Pair<GameRoom, GameSession>? {
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
        val roomAndSession = commonHandle(connection, clientId, request, true) ?: return
        val room = roomAndSession.first
        val session = roomAndSession.second

        try {
            session.setChosenColor(request.chosenColor)

            session.finishColorSelection()

            sendHandUpdates(room)
            broadcastGameState(room)

            connection.sendMessage(OkMessage("Color chosen successfully"))
        } catch (e: Exception) {
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
        val roomAndSession = commonHandle(connection, clientId, request, true) ?: return
        val room = roomAndSession.first
        val session = roomAndSession.second

        try {
            session.drawCard(clientId)

            session.finishDrawing(clientId)

            sendHandUpdates(room)
            broadcastGameState(room)

            connection.sendMessage(OkMessage("Card drawn, turn passed"))
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

    // реализуем метод, пришедший от AutoClosable
    override fun close() {
        running = false
        serverSocket.close()
    }

    // статические поля класса
    companion object {
        const val PORT = 9090
        private val logger = Logger.getLogger(Server::class.java.name)
    }
}