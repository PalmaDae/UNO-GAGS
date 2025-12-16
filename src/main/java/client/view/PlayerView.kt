package client.view

import client.controller.GameController
import client.controller.PlayerController
import javafx.geometry.Pos
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.control.TextField
import javafx.scene.control.ToggleGroup
import javafx.scene.layout.HBox
import javafx.scene.layout.VBox
import javafx.scene.paint.Color
import javafx.scene.shape.Rectangle
import javafx.stage.Stage
import client.config.StageConfig

class PlayerView(
    private val stage: Stage,
    private val gameController: GameController,
    private val isJoin: Boolean,
    private val initialRoomId: Long? = null
) {
    private val playerController = PlayerController(
        stage,
        gameController,
        isJoin,
        initialRoomId
    )

    lateinit var scene: Scene

    val toggleGroup = ToggleGroup()

    init {
        val nameField = TextField().apply {
            promptText = "Place your name here"
            prefWidth = 220.0
            maxWidth = 220.0
            minWidth = 220.0
            styleClass.add("uno-input")
        }

        val testAvatar1 = Rectangle(50.0, 50.0, Color.BLUE)
        val testAvatar2 = Rectangle(50.0, 50.0, Color.RED)
        val testAvatar3 = Rectangle(50.0, 50.0, Color.GREEN)

        var selectedAvatar: Rectangle? = null

        val avatarsBox = HBox(10.0, testAvatar1, testAvatar2, testAvatar3).apply { alignment = Pos.CENTER }

        listOf(testAvatar1, testAvatar2, testAvatar3).forEach { rect ->
            rect.setOnMouseClicked { selectedAvatar = rect }
        }

        val createButton = Button("Create and go play").apply {
            setOnAction {
                val name = nameField.text
                val avatar = selectedAvatar?.id ?: "default"
                playerController.createPlayer(name, avatar)
            }
        }

        val backButton = Button("Back").apply { setOnAction { playerController.backButton() } }

        val root = VBox(16.0, nameField, createButton, backButton, avatarsBox).apply {
            alignment = Pos.CENTER
            style = "-fx-padding: 24;"
        }

        scene = Scene(root, StageConfig.getWidth(stage), StageConfig.getHeight(stage))
        scene.stylesheets.add(javaClass.getResource("/css/style.css").toExternalForm())
    }
}