package client.view

import client.config.StageConfig
import client.controller.GameController
import client.controller.JoinController
import javafx.geometry.Pos
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.control.TextField
import javafx.scene.layout.VBox
import javafx.stage.Stage

class JoinView(private val stage: Stage, private val gameController: GameController) {
    private val controller = JoinController(stage, gameController)
    lateinit var scene: Scene


    private val idField = TextField().apply {
        promptText = "Введите Ключ комнаты (Room ID)"
        prefWidth = 220.0
        maxWidth = 220.0
        minWidth = 220.0
        styleClass.add("uno-input")
    }

    init {
        val joinButton = Button("Join").apply {
            setOnAction {
                controller.joinGame(idField.text)
            }
        }

        val pasteButton = Button("Paste Key").apply {
            setOnAction {
                idField.text = controller.pasteKey()
            }
        }

        val backButton = Button("Back").apply { setOnAction { controller.backTo() } }

        val root = VBox(16.0,
            Label("Присоединиться по Ключу"),
            idField,
            pasteButton,
            joinButton,
            backButton
        ).apply {
            alignment = Pos.CENTER
            style = "-fx-padding: 24;"
        }
        scene = Scene(root, StageConfig.getWidth(stage), StageConfig.getHeight(stage))
        scene.stylesheets.add(javaClass.getResource("/css/style.css").toExternalForm())
    }
}