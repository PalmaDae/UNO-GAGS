package controller

import javafx.scene.Scene
import javafx.scene.layout.StackPane
import javafx.stage.Stage
import view.CreateView
import view.JoinView
import view.PlayerView

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