package client.view

import client.config.StageConfig
import client.controller.GameController
import javafx.geometry.Pos
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.control.TextField
import javafx.scene.layout.VBox
import javafx.stage.Stage

class JoinView(
    stage: Stage,
    private val gameController: GameController
) {
    var scene: Scene
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
                val roomPassword = idField.text.trim()
                if (roomPassword.isNotBlank()) {
                    gameController.onJoinRequested(roomPassword)
                }
            }
        }

        val pasteButton = Button("Paste Key").apply {
            setOnAction {
                val clipboard = javafx.scene.input.Clipboard.getSystemClipboard()
                if (clipboard.hasString()) {
                    idField.text = clipboard.string
                }
            }
        }

        val backButton = Button("Back").apply { setOnAction { gameController.onBackRequested() } }

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