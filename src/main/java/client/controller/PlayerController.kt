package client.controller

import javafx.stage.Stage
import client.view.CreateView
import client.view.LobbyView
import client.view.MainMenuView

class PlayerController(private val stage: Stage, private val gameController: GameController, private val isJoin: Boolean) {
    fun backButton() {
        val mainView = MainMenuView(stage, gameController);
        stage.scene = mainView.scene;
    }

    fun createPlayer(name: String, avatar: String) {
        if (isJoin) {
            gameController.addPlayer(name, avatar, isJoin)
            val lobby = LobbyView(stage, rules = listOf(), gameController)
            stage.scene = lobby.scene
        } else {
            gameController.addPlayer(name, avatar, !isJoin)
            val createView = CreateView(stage, gameController)
            stage.scene = createView.scene
        }
    }
}