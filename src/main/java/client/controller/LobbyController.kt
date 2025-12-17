package client.controller

import javafx.scene.input.Clipboard
import javafx.scene.input.ClipboardContent
import javafx.stage.Stage
class LobbyController(private val stage: Stage, private val gameController: GameController) {

    fun kickPlayer(playerId: Long) {
        val roomId = gameController.getCurrentRoomId()
        if (roomId != null) {
            // gameController.kickPlayer(roomId, playerId)
            println("Sending kick request for player ID: $playerId in room $roomId")
        }
    }

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

    fun deleteLobby() {
        println("Sending Delete Lobby request...")
    }
}