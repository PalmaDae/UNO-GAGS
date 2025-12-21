package client.controller

import javafx.stage.Stage

class CreateController(
    private val stage: Stage,
    private val gameController: GameController
) {
    fun onCreateLobbyRequested(
        maxPlayers: Int,
        allowStuck: Boolean,
        allowStuckCards: Boolean,
        infinityDrawing: Boolean
    ) {
        val roomPass = generatePassword()
        gameController.createRoom(
            password = roomPass,
            maxPlayers = maxPlayers,
            allowStuck = allowStuck,
            allowStuckCards = allowStuckCards,
            infinityDrawing = infinityDrawing
        )
    }

    fun onBackRequested() {
        // Вернуться в меню
    }

    private fun generatePassword(): String {
        val chars = "0123456789"
        return (1..5)
            .map { chars.random() }
            .joinToString("")
    }
}