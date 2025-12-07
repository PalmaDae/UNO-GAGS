package uno_server.protocol

import uno_server.common.Connection
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * Manages player connections and user ID assignments.
 */
class ConnectionManager {
    private val players = ConcurrentHashMap<Long, PlayerConnection>()
    private val connectionToUserId = ConcurrentHashMap<Connection, Long>()
    private val userIdGenerator = AtomicLong(1)

    fun getOrCreateUser(connection: Connection): Long {
        return connectionToUserId.getOrPut(connection) {
            val userId = userIdGenerator.getAndIncrement()
            val username = "Player$userId"
            val player = PlayerConnection(userId, username, connection)
            players[userId] = player
            userId
        }
    }

    fun getUserId(connection: Connection): Long? = connectionToUserId[connection]

    fun getPlayer(userId: Long): PlayerConnection? = players[userId]

    fun removeUser(connection: Connection): PlayerConnection? {
        val userId = connectionToUserId.remove(connection) ?: return null
        return players.remove(userId)
    }

    fun getAllPlayers(): Collection<PlayerConnection> = players.values
}
