package server.game

data class GameRoom(
    val id: Long,
    val creatorId: Long,
    val password: String?,
    val players: MutableList<PlayerState> = mutableListOf(),
    var gameStarted: Boolean = false,
    var gameSession: GameSession? = null
) {
    fun addPlayer(user: PlayerState) {
        if (!players.any { it.id == user.id }) {
            players.add(user)
        }
    }

    fun removePlayer(user: PlayerState) = players.removeAll { it.id == user.id }
}
