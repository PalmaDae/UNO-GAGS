package controller

import javafx.stage.Stage
import view.CreateView
import view.MainMenuView
import view.PlayerView

class PlayerController(private val stage: Stage) {
    fun backButton() {
        val mainView = MainMenuView(stage);
        stage.scene = mainView.scene;
    }

    fun createPlayer() {
        val playerView = PlayerView(stage);
        stage.scene = playerView.scene;
    }
}