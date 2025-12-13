package server.protocol

import proto.common.Method
import proto.dto.*
import server.common.Connection
import server.protocol.MessageParser.RoomsListPayload
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import java.util.logging.Level
import java.util.logging.Logger

/**
 * Manages room creation, joining, leaving, and listing.
 */
class RoomManager(
    private val connectionManager: ConnectionManager,
    private val messageSender: MessageSender
) {
    private val logger = Logger.getLogger(RoomManager::class.java.name)
    private val rooms = ConcurrentHashMap<Long, Room>()
    private val roomIdGenerator = AtomicLong(1)

    fun handleCreateRoom(connection: Connection, request: CreateRoomRequest) {
        try {
            val userId = connectionManager.getOrCreateUser(connection)
            val roomId = roomIdGenerator.getAndIncrement()
            
            val room = Room(
                roomId,
                request.roomName,
                request.password,
                request.maxPlayers,
                request.allowStuck,
                userId
            )
            rooms[roomId] = room

            val creator = connectionManager.getPlayer(userId)!!
            room.addPlayer(creator)
            creator.currentRoomId = roomId

            val response = CreateRoomResponse(roomId, request.roomName, true)
            messageSender.send(connection, Method.ROOM_CREATED_SUCCESS, response)

            broadcastLobbyUpdate(roomId)
            logger.log(Level.INFO, "Room created: $roomId by user $userId")
        } catch (e: Exception) {
            logger.log(Level.SEVERE, "Error creating room", e)
            messageSender.sendError(connection, "Failed to create room: ${e.message}", "CREATE_ROOM_ERROR")
        }
    }

    fun handleGetRooms(connection: Connection) {
        try {
            val roomList: MutableList<RoomInfo?> = rooms.values.map { room ->
                val creator = connectionManager.getPlayer(room.creatorId)
                val creatorName = creator?.username ?: "Unknown"
                RoomInfo(
                    room.roomId,
                    room.roomName ?: "",
                    room.hasPassword(),
                    room.maxPlayers,
                    room.currentPlayerCount,
                    room.status ?: RoomStatus.WAITING,
                    creatorName
                )
            }.toMutableList()

            val payload = RoomsListPayload(roomList)
            messageSender.send(connection, Method.ROOMS_LIST, payload)
        } catch (e: Exception) {
            logger.log(Level.SEVERE, "Error getting rooms", e)
            messageSender.sendError(connection, "Failed to get rooms: ${e.message}", "GET_ROOMS_ERROR")
        }
    }

    fun handleJoinRoom(connection: Connection, request: JoinRoomRequest) {
        try {
            val room = rooms[request.roomId]
            if (room == null) {
                messageSender.sendError(connection, "Room not found", "ROOM_NOT_FOUND")
                return
            }

            if (room.isFull) {
                messageSender.sendError(connection, "Room is full", "ROOM_FULL")
                return
            }

            if (room.hasPassword() && room.password != request.password) {
                messageSender.sendError(connection, "Invalid password", "INVALID_PASSWORD")
                return
            }

            val userId = connectionManager.getOrCreateUser(connection)
            val player = connectionManager.getPlayer(userId)!!

            if (room.addPlayer(player)) {
                player.currentRoomId = request.roomId
                val response = JoinRoomResponse(request.roomId, true)
                messageSender.send(connection, Method.JOIN_ROOM_SUCCESS, response)
                broadcastLobbyUpdate(request.roomId)
                logger.log(Level.INFO, "User $userId joined room ${request.roomId}")
            } else {
                messageSender.sendError(connection, "Failed to join room", "JOIN_FAILED")
            }
        } catch (e: Exception) {
            logger.log(Level.SEVERE, "Error joining room", e)
            messageSender.sendError(connection, "Failed to join room: ${e.message}", "JOIN_ROOM_ERROR")
        }
    }

    fun handleLeaveRoom(connection: Connection) {
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

            val room = rooms[roomId]
            if (room != null) {
                room.removePlayer(userId)
                player.currentRoomId = null
                player.isReady = false

                if (room.currentPlayerCount == 0) {
                    rooms.remove(roomId)
                } else {
                    broadcastLobbyUpdate(roomId)
                }

                messageSender.send(connection, Method.OK, MessageParser.EmptyPayload)
                logger.log(Level.INFO, "User $userId left room $roomId")
            }
        } catch (e: Exception) {
            logger.log(Level.SEVERE, "Error leaving room", e)
            messageSender.sendError(connection, "Failed to leave room: ${e.message}", "LEAVE_ROOM_ERROR")
        }
    }

    fun getRoom(roomId: Long): Room? = rooms[roomId]

    fun removeRoom(roomId: Long) {
        rooms.remove(roomId)
    }

    fun broadcastLobbyUpdate(roomId: Long) {
        val room = rooms[roomId] ?: return

        val playerInfos = room.getPlayers().filterNotNull().map { p ->
            PlayerInfo(
                p.userId,
                p.username ?: "",
                p.userId == room.creatorId,
                p.isReady
            )
        }

        val update = LobbyUpdate(playerInfos, room.status ?: RoomStatus.WAITING)
        messageSender.broadcastToRoom(room, Method.LOBBY_UPDATE, update)
    }

    fun handleDisconnect(userId: Long, roomId: Long?) {
        if (roomId != null) {
            val room = rooms[roomId]
            if (room != null) {
                room.removePlayer(userId)
                if (room.currentPlayerCount == 0) {
                    rooms.remove(roomId)
                } else {
                    broadcastLobbyUpdate(roomId)
                }
            }
        }
    }
}
