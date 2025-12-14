package client.controller

import client.enity.Player
import javafx.stage.Stage
import client.view.LobbyView
import client.view.MainMenuView

class GameController(private val stage: Stage) {
    val players = mutableListOf<Player>();

    fun addPlayer(name: String, avatar: String, isOwner: Boolean = false) {
        val role = if (isOwner) "OWNER" else "PLAYER"
        val player = Player(
            id = System.currentTimeMillis(),
            name = name,
            avatar = avatar,
            role = role
        )

        players.add(player)

        createLobby()
    }

    fun closedGame() {
        val menuView = MainMenuView(stage, gameController = GameController(stage))
        stage.scene = menuView.scene
    }

    fun createLobby() {
        val lobby = LobbyView(stage, rules = listOf())
        stage.scene = lobby.scene;
    }
}