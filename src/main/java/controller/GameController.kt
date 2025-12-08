package controller

import enity.Player
import javafx.stage.Stage
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

        createLobby()
    }

    fun createLobby() {
        val createView = CreateView(stage);
        stage.scene = createView.scene;
    }
}