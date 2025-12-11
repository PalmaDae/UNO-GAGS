package controller

import javafx.stage.Stage
import view.GameView
import view.MainMenuView
import view.PlayerView

class LobbyController(private val stage: Stage) {
    fun kickPlayer() {

    }

    fun startGame() {
        val gameView = GameView(stage)
        stage.scene = gameView.scene;
    }

    fun generatePassword(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return (1..5)
            .map { chars.random() }
            .joinToString("")
    }

    fun copyPassword(password: String) {
        val clipboard = javafx.scene.input.Clipboard.getSystemClipboard()
        val content = javafx.scene.input.ClipboardContent()
        content.putString(password)
        clipboard.setContent(content)
    }

    fun deleteLobby() {

    }
}