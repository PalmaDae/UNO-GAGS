package view

import controller.CreateController
import controller.GameController
import javafx.geometry.Pos
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.control.TextField
import javafx.scene.control.ToggleButton
import javafx.scene.control.ToggleGroup
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.layout.HBox
import javafx.scene.layout.VBox
import javafx.scene.paint.Color
import javafx.scene.shape.Rectangle
import javafx.stage.Stage

class CreateView(private val stage: Stage) {
    private val createController = CreateController(stage)
    private val gameController = GameController(stage);

    lateinit var scene: Scene;

    val toggleGroup = ToggleGroup()

    init {
        val nameField = TextField().apply { promptText = "Place your name here" }

        val testAvatar1 = Rectangle(50.0, 50.0, Color.BLUE)
        val testAvatar2 = Rectangle(50.0, 50.0, Color.RED)
        val testAvatar3 = Rectangle(50.0, 50.0, Color.GREEN)

        var selectedAvatar: Rectangle? = null;

        val avatarsBox = HBox(10.0, testAvatar1, testAvatar2, testAvatar3).apply { alignment = Pos.CENTER }

        listOf(testAvatar1, testAvatar2, testAvatar3).forEach { rect ->
            rect.setOnMouseClicked { selectedAvatar = rect }
        }

        val createButton = Button("Create and go play").apply { setOnAction {

            val name = nameField.text
            val avatar = selectedAvatar?.id ?: "default"

            gameController.createPlayer(name,avatar)
            gameController.openLobby();

        } }

        val backButton = Button("Back").apply { setOnAction { createController.backButton() } }

        val root = VBox(16.0, nameField, createButton, backButton, avatarsBox).apply {
            alignment = Pos.CENTER
            style = "-fx-padding: 24;"
        }

        scene = Scene(root, 400.0, 600.0)
    }

}