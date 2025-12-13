package client.controller

import javafx.stage.Stage
import client.view.JoinView
import client.view.PlayerView

class MainMenuController(private val stage: Stage, private val gameController: GameController) {
    fun createButton() {
        val playerView = PlayerView(stage, gameController, isJoin = false)
        stage.scene = playerView.scene;
    }

    fun joinButton() {
        val joinView = JoinView(stage, gameController)
        stage.scene = joinView.scene
    }

    fun exitButton() {
        stage.close()
    }
}