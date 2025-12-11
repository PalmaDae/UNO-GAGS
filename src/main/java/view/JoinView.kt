package view

import config.StageConfig
import controller.GameController
import controller.JoinController
import controller.MainMenuController
import javafx.geometry.Pos
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.control.PasswordField
import javafx.scene.control.TextField
import javafx.scene.layout.VBox
import javafx.stage.Stage

class JoinView(private val stage: Stage, private val gameController: GameController) {
    private val controller = JoinController(stage, gameController)
    lateinit var scene: Scene

    init {
        val passwordField = TextField().apply {
            prefWidth = 220.0
            maxWidth = 220.0
            minWidth = 220.0
            styleClass.add("uno-input")
        }

        val joinButton = Button("Join").apply { setOnAction { controller.joinGame(passwordField.text) }  }
        val backButton = Button("Back").apply { setOnAction { controller.backTo() } }
        val pasteButton = Button("Paste password").apply { setOnAction { passwordField.text = controller.pastePassword() } }

        val root = VBox(16.0, passwordField, pasteButton , joinButton, backButton).apply {
            alignment = Pos.CENTER
            style = "-fx-padding: 24;"
        }
        scene = Scene(root, StageConfig.getWidth(stage), StageConfig.getHeight(stage))
        scene.stylesheets.add(javaClass.getResource("/css/style.css").toExternalForm())
    }
}