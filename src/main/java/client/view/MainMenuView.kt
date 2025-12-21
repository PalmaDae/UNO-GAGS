package client.view

import client.config.StageConfig
import client.controller.GameController
import javafx.geometry.Pos
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.layout.VBox
import javafx.stage.Stage

class MainMenuView(
    stage: Stage,
    private val gameController: GameController
) {
    var scene: Scene

    init {
        val createButton = Button("Create").apply {
            setOnAction { gameController.onCreateGameRequested() }
        }

        val joinButton = Button("Join").apply {
            setOnAction { gameController.onJoinGameRequested() }
        }

        val exitButton = Button("Exit").apply {
            setOnAction { gameController.onExitRequested() }
        }

        listOf(createButton, joinButton, exitButton).forEach { it.prefWidth = 200.0 }

        val root = VBox(16.0).apply {
            children.addAll(createButton, joinButton, exitButton)
            alignment = Pos.CENTER
        }

        scene = Scene(root, StageConfig.getWidth(stage), StageConfig.getHeight(stage))
        scene.stylesheets.add(javaClass.getResource("/css/style.css").toExternalForm())
    }
}