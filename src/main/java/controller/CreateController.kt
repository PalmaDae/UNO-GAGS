package controller

import javafx.stage.Stage
import view.MainMenuView

class CreateController(private val stage: Stage) {
    fun backButton() {
        val mainView = MainMenuView(stage);
        stage.scene = mainView.scene;
    }
}