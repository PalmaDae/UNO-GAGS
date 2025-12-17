package client.controller

import javafx.stage.Stage
import client.view.MainMenuView
import java.util.UUID

class CreateController(private val stage: Stage, private val gameController: GameController) {
    fun generatePassword(): String {
        val chars = "0123456789"
        return (1..5)
            .map { chars.random() }
            .joinToString("")
    }

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
        val roomPass = generatePassword()

        gameController.createRoom(
            password = roomPass,
            allowStuck = allowStuck,
            allowStuckCards = allowStuckCards,
            infinityDrawing = infinityDrawing,
            maxPlayers = maxPlayers,
        )
    }
}