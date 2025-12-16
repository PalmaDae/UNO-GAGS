package client.controller

import javafx.scene.input.Clipboard
import javafx.stage.Stage
import client.view.MainMenuView
import client.view.PlayerView

class JoinController(private val stage: Stage, private val gameController: GameController) {

    fun joinGame(roomIdText: String) {
        if (roomIdText.isBlank()) {
            System.err.println("Room ID cannot be empty.")
            return
        }

        try {
            val roomId = roomIdText.toLong()

            val playerView = PlayerView(
                stage,
                gameController,
                isJoin = true,
                initialRoomId = roomId
            )
            stage.scene = playerView.scene

        } catch (e: NumberFormatException) {
            System.err.println("Invalid Room ID format. Must be a number.")
        }
    }

    fun backTo() {
        val menuView = MainMenuView(stage, gameController)
        stage.scene = menuView.scene
    }

    fun pasteKey(): String {
        val clipboard = Clipboard.getSystemClipboard()
        return if (clipboard.hasString()) {
            clipboard.string
        } else {
            ""
        }
    }
}