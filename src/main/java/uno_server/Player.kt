package uno_server

import uno_server.common.Connection

/**
 * Simple player representation
 */
data class Player(
    val id: Int,
    val name: String,
    val connection: Connection
)