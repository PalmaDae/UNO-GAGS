package client.view

import client.config.StageConfig
import client.controller.LobbyController
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
    private val playersBox = VBox(8.0)

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
                playersBox,
                startButton
            )
        }

        scene = Scene(root, StageConfig.getWidth(stage), StageConfig.getHeight(stage))
        scene.stylesheets.add(javaClass.getResource("/css/style.css").toExternalForm())
    }

//    fun updatePlayers(players: List<Player>) {
//        playersBox.children.clear()
//        for (player in players) {
//            val label = Label("${player.username} (${player.role})")
//            playersBox.children.add(label)
//        }
//    }
}
