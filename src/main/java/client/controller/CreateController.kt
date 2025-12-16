package client.controller

import javafx.stage.Stage
import client.view.MainMenuView
import java.util.UUID

class CreateController(private val stage: Stage, private val gameController: GameController) {
    fun backButton() {
        val mainView = MainMenuView(stage, gameController)
        stage.scene = mainView.scene
    }

    fun createLobby(
        maxPlayers: Int,
        allowStuck: Boolean,
        rules: List<Boolean>
    ) {
        val roomName = "Room-" + UUID.randomUUID().toString().substring(0, 4)
        val password: String? = null

        gameController.createRoom(
            roomName,
            password,
            maxPlayers,
            allowStuck,
            rules
        )
    }
}