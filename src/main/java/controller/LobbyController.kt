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

    fun copyPassword() {

    }

    fun deleteLobby() {

    }
}