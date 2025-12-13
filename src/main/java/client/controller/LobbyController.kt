package client.controller

import javafx.scene.input.Clipboard
import javafx.scene.input.ClipboardContent
import javafx.stage.Stage
import client.view.GameView

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
        val clipboard = Clipboard.getSystemClipboard()
        val content = ClipboardContent()
        content.putString(password)
        clipboard.setContent(content)
    }

    fun deleteLobby() {

    }
}