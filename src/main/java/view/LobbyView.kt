package view

import config.StageConfig
import controller.LobbyController
import javafx.geometry.Pos
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.control.TextField
import javafx.scene.layout.VBox
import javafx.stage.Stage

class LobbyView(private val stage: Stage, private val rules: List<Boolean>) {
    lateinit var scene: Scene
    private val controller = LobbyController(stage)

    init {
        val password = controller.generatePassword()

        val passwordField = TextField(password).apply {
            isEditable = false
            alignment = Pos.CENTER
            prefWidth = 160.0
            maxWidth = 160.0
            minWidth = 160.0
            styleClass.add("uno-input")
        }

        val copyButton = Button("Copy").apply {
            setOnAction { controller.copyPassword(password) }
        }

        val startButton = Button("Start game").apply {
            setOnAction { controller.startGame() }
        }

        val root = VBox(16.0).apply {
            alignment = Pos.CENTER
            children.addAll(
                Label("Lobby Owner"),
                passwordField,
                copyButton,
                startButton
            )
        }

        scene = Scene(root, StageConfig.getWidth(stage), StageConfig.getHeight(stage))
        scene.stylesheets.add(javaClass.getResource("/css/style.css").toExternalForm())
    }
}
