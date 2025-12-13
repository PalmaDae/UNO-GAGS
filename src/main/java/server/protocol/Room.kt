package server.protocol

import proto.dto.RoomStatus
import java.util.ArrayList
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Представляет игровую комнату на сервере.
 * Отслеживает метаданные комнаты, игроков и информацию о соединении.
 */
class Room(
    val roomId: Long,
    val roomName: String?,
    val password: String?,
    val maxPlayers: Int,
    val allowStack: Boolean,
    val creatorId: Long
) {
    private val players: MutableList<PlayerConnection?> = CopyOnWriteArrayList<PlayerConnection?>()

    var status: RoomStatus? = RoomStatus.WAITING

    val currentPlayerCount: Int
        get() = players.size

    val isFull: Boolean
        get() = players.size >= maxPlayers

    fun addPlayer(player: PlayerConnection?): Boolean {
        if (players.size < maxPlayers) {
            players += player
            return true
        }
        return false
    }

    fun removePlayer(userId: Long) =
        players.removeIf { p: PlayerConnection? -> p!!.userId == userId }


    fun getPlayers(): MutableList<PlayerConnection?> {
        return ArrayList(players)
    }

    fun hasPassword() =
        password?.isNotEmpty() ?: false
}