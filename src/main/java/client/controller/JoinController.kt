package client.controller

import javafx.stage.Stage

class JoinController(
    private val stage: Stage,
    private val gameController: GameController
) {
    fun onJoinRequested(roomIdText: String) {
        if (roomIdText.isBlank()) {
            System.err.println("Room ID cannot be empty")
            return
        }
        // Переход в PlayerView для ввода имени
    }

    fun onBackRequested() {
        // Возврат в меню
    }

    fun pasteRoomId(): String {
        return gameController.pastePassword()
    }
}