package controller

import javafx.stage.Stage
import view.CreateView
import view.MainMenuView

class PlayerController(private val stage: Stage) {
    fun backButton() {
        val mainView = MainMenuView(stage);
        stage.scene = mainView.scene;
    }

    fun createPlayer() {
        val createLobbyView = CreateView(stage);
        stage.scene = createLobbyView.scene;
    }
}