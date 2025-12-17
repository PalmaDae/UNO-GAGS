package client.controller

import javafx.scene.input.Clipboard
import javafx.stage.Stage
import client.view.MainMenuView
import client.view.PlayerView

class JoinController(private val stage: Stage, private val gameController: GameController) {

    fun joinGame(roomPasswordText: String) {
        if (roomPasswordText.isBlank()) {
            System.err.println("Room ID cannot be empty.")
            return
        }

        val playerView = PlayerView(
            stage,
            gameController,
            isJoin = true,
            initialPassword = roomPasswordText
        )
        stage.scene = playerView.scene
    }

    fun backTo() {
        val menuView = MainMenuView(stage, gameController)
        stage.scene = menuView.scene
    }

    fun pasteKey(): String {
        val clipboard = Clipboard.getSystemClipboard()
        return if (clipboard.hasString())
            clipboard.string
        else ""
    }
}