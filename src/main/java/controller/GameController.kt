package controller

import enity.Player
import javafx.stage.Stage
import view.CreateLobbyView
import view.CreateView
import view.LobbyView

class GameController(private val stage: Stage) {
    val players = mutableListOf<Player>();

    fun createPlayer(name: String, avatar: String) {
        val role = if (players.isEmpty()) "OWNER" else "PLAYER"
        val player = Player(
            id = System.currentTimeMillis(),
            name = name,
            avatar = avatar,
            role = role
        )

        players.add(player)

        openLobby()
    }

    fun openLobby() {
        val lobbyView = LobbyView(stage, players);
        stage.scene = lobbyView.scene;
    }
}