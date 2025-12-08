package controller

import javafx.scene.Scene
import javafx.scene.layout.StackPane
import javafx.stage.Stage
import view.CreateView
import view.PlayerView

class MainMenuController(private val stage: Stage) {
    fun createButton() {
        val playerView = PlayerView(stage)
        stage.scene = playerView.scene;
    }

    fun joinButton() {

    }

    fun exitButton() {
        stage.close()
    }
}