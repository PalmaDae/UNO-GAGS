package controller

import javafx.scene.Scene
import javafx.scene.layout.StackPane
import javafx.stage.Stage
import view.CreateView

class MainMenuController(private val stage: Stage) {
    fun createButton() {
        val createView = CreateView(stage)
//        stage.scene = createView.scene;
    }

    fun joinButton() {

    }

    fun exitButton() {
        stage.close()
    }
}