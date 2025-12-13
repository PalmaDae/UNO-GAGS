package uno_server

import uno_server.game.GameSession

/**
 * Room state - tracks players, game session, and started status
 */
class Room {
    var id: String = ""
    var name: String = ""
    val players = mutableListOf<Player>()
    var isStarted = false
    var gameSession: GameSession? = null
    var creatorId: Int = 0

    constructor()

    constructor(id: String, name: String) {
        this.id = id
        this.name = name
    }

    fun addPlayer(player: Player) {
        if (players.isEmpty()) {
            creatorId = player.id
        }
        players.add(player)
    }

    fun removePlayer(playerId: Int): Boolean {
        val removed = players.removeAll { it.id == playerId }
        if (removed) {
            gameSession = null
            isStarted = false
        }
        return removed
    }

    fun getPlayerCount(): Int {
        return players.size
    }

    fun getPlayerNames(): List<String> {
        return players.map { it.name }
    }
}