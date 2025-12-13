package uno_server.protocol

import uno_server.common.Connection

/**
 * Simple connection wrapper for compatibility
 */
class ConnectionManager {
    private val connections = mutableMapOf<Connection, Int>()

    fun addUser(connection: Connection, userId: Int) {
        connections[connection] = userId
    }

    fun removeUser(connection: Connection): Int? {
        return connections.remove(connection)
    }

    fun getUserId(connection: Connection): Int? {
        return connections[connection]
    }
}