package client.controller

import javafx.scene.input.Clipboard
import javafx.scene.input.ClipboardContent

class LobbyController(private val gameController: GameController) {

    fun startGame() {
        val roomId = gameController.getCurrentRoomId()
        if (roomId != null) {
            gameController.startGame(roomId)
            println("Sending StartGame request for room $roomId")
        }
    }

    fun generatePassword(): String {
        val chars = "0123456789"
        return (1..5)
            .map { chars.random() }
            .joinToString("")
    }

    fun copyPassword(password: String) {
        val clipboard = Clipboard.getSystemClipboard()
        val content = ClipboardContent()
        content.putString(password)
        clipboard.setContent(content)
        println("Room ID copied: $password")
    }

}