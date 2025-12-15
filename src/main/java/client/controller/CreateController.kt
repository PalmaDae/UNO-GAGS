package client.controller

import javafx.stage.Stage
import client.view.LobbyView
import client.view.MainMenuView

class CreateController(private val stage: Stage, private val gameController: GameController) {
    fun backButton() {
        val mainView = MainMenuView(stage, gameController);
        stage.scene = mainView.scene;
    }

    fun createOwner(name: String, avatar: String) {
        gameController.addPlayer(name, avatar, true)
    }

    fun createLobby(r1: Boolean, r2: Boolean, r3: Boolean) {
        val lobby = LobbyView(stage, listOf(r1, r2, r3), gameController)
        stage.scene = lobby.scene
    }

}