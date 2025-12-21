package client.controller

import javafx.scene.input.Clipboard
import javafx.scene.input.ClipboardContent

class LobbyController(
    private val gameController: GameController
) {
    fun onStartGameRequested() {
        gameController.startGame(gameController.getCurrentRoomId() ?: return)
    }

    fun onLeaveRequested() {
        gameController.disconnect()
        // View переходит в MainMenu
    }

    fun copyPassword(password: String) {
        val clipboard = Clipboard.getSystemClipboard()
        val content = ClipboardContent()
        content.putString(password)
        clipboard.setContent(content)
    }
}