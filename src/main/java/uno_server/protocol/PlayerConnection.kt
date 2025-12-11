package uno_server.protocol

import uno_server.common.Connection

/**
 * Представляет связь игрока и его метаданные.
 * Связывает объект Connection с пользовательской информацией.
 */
class PlayerConnection(
    val userId: Long,
    val username: String?,
    val connection: Connection?
) {
    var isReady: Boolean = false
    var currentRoomId: Long? = null
}