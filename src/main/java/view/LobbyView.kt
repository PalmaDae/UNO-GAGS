package view

import config.StageConfig
import controller.LobbyController
import enity.Player
import javafx.geometry.Pos
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.layout.VBox
import javafx.stage.Stage
import jdk.internal.vm.ThreadContainers.root

class LobbyView(private val stage: Stage, private val rules: List<Boolean>) {
    lateinit var scene: Scene;
    private val controller = LobbyController(stage);

    init {
        val startButton = Button("Start game").apply { setOnAction { controller.startGame() } }

        val root = VBox(16.0, startButton).apply {
            alignment = Pos.CENTER
        }

        root.children.add(Label("Dae, OWNER"))

        scene = Scene(root, StageConfig.getWidth(stage), StageConfig.getHeight(stage))
        scene.stylesheets.add(javaClass.getResource("/css/style.css").toExternalForm())
    }
}