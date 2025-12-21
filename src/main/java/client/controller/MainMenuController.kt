package client.controller

import javafx.stage.Stage

class MainMenuController(
    private val stage: Stage,
    private val gameController: GameController
) {
    fun onCreateGameRequested() {
        // Navigation to PlayerView for create flow
    }

    fun onJoinGameRequested() {
        // Navigation to JoinView
    }

    fun onExitRequested() {
        stage.close()
    }
}