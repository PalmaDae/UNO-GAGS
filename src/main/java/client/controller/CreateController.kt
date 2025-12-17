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
        allowStuckCards: Boolean,
        infinityDrawing: Boolean
    ) {
        gameController.createRoom(
            allowStuck = allowStuck,
            allowStuckCards = allowStuckCards,
            infinityDrawing = infinityDrawing,
            maxPlayers = maxPlayers,
        )
    }
}