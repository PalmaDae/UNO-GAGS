package controller

import javafx.stage.Stage
import view.CreateView
import view.LobbyView
import view.MainMenuView

class CreateController(private val stage: Stage) {
    fun backButton() {
        val mainView = MainMenuView(stage);
        stage.scene = mainView.scene;
    }

    fun createLobby(r1: Boolean, r2: Boolean, r3: Boolean) {
        val lobby = LobbyView(stage, listOf(r1, r2, r3))
        stage.scene = lobby.scene
    }
}