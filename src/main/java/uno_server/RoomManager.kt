package uno_server

import uno_server.common.Connection
import java.util.concurrent.ConcurrentHashMap

/**
 * Manages room operations - create, join, leave, get rooms
 */
class RoomManager {
    val rooms: MutableMap<String, Room> = ConcurrentHashMap()
    private val playerConnections = ConcurrentHashMap<Int, uno_server.common.Connection>()

    fun createRoom(name: String): String {
        val roomId = generateRoomId()
        rooms[roomId] = Room(roomId, name)
        return roomId
    }

    fun joinRoom(roomId: String, playerName: String, playerId: Int, connection: uno_server.common.Connection): Boolean {
        val room = rooms[roomId] ?: return false
        
        if (room.isStarted) {
            return false
        }

        if (room.players.size >= 8) {
            return false
        }

        room.addPlayer(Player(playerId, playerName, connection))
        playerConnections[playerId] = connection
        return true
    }

    fun leaveRoom(roomId: String, playerId: Int): Boolean {
        val room = rooms[roomId] ?: return false
        val removed = room.removePlayer(playerId)
        
        if (removed && room.players.isEmpty()) {
            rooms.remove(roomId)
        }
        
        playerConnections.remove(playerId)
        return removed
    }

    fun getRoom(roomId: String): Room? {
        return rooms[roomId]
    }

    fun getConnection(playerId: Int): uno_server.common.Connection? {
        return playerConnections[playerId]
    }

    fun getPlayerRoom(playerId: Int): String? {
        return rooms.values.find { room ->
            room.players.any { it.id == playerId }
        }?.id
    }

    private fun generateRoomId(): String {
        return (1000..9999).random().toString()
    }
}