package client.view

import client.common.ResourceLoader
import client.config.StageConfig
import client.controller.GameController
import javafx.geometry.Pos
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.control.TextField
import javafx.scene.control.ToggleButton
import javafx.scene.control.ToggleGroup
import javafx.scene.image.ImageView
import javafx.scene.layout.HBox
import javafx.scene.layout.VBox
import javafx.stage.Stage

class PlayerView(
    stage: Stage,
    isJoin: Boolean,
    private val gameController: GameController
) {
    var scene: Scene

    private var nameField: TextField = TextField().apply {
        promptText = "Place your name here"
        prefWidth = 220.0
        maxWidth = 220.0
        minWidth = 220.0
        styleClass.add("uno-input")
    }
    private var avatarGroup: ToggleGroup = ToggleGroup()

    init {
        val avatarsBox = HBox(15.0).apply {
            alignment = Pos.CENTER
            style = "-fx-padding: 10;"
        }

        val avatarFiles = listOf("avatar1.jpg","avatar2.jpg","avatar3.jpg","avatar4.jpg")

        avatarFiles.forEach { fileName ->
            val avatarImage = ResourceLoader.loadAvatar(fileName)
            val imageView = ImageView(avatarImage).apply {
                fitWidth = 60.0
                fitHeight = 60.0
            }

            val toggleButton = ToggleButton().apply {
                graphic = imageView
                toggleGroup = avatarGroup
                userData = fileName
                styleClass.add("avatar-selector")
            }
            avatarsBox.children.add(toggleButton)
        }

        avatarGroup.toggles.firstOrNull()?.isSelected = true

        val createButton = Button(if (isJoin) "Join and Play" else "Create and Play").apply {
            setOnAction {
                val name = nameField.text
                val selectedToggle = avatarGroup.selectedToggle as? ToggleButton
                val avatarFileName = selectedToggle?.userData as? String ?: "avatar1.jpg"

                if (name.isNotBlank()) {
                    gameController.onPlayerDataSubmitted(name, avatarFileName)
                }
            }
        }

        val backButton = Button("Back").apply { setOnAction { gameController.onBackRequested() } }

        val root = VBox(16.0, nameField, avatarsBox, createButton, backButton).apply {
            alignment = Pos.CENTER
            style = "-fx-padding: 24;"
        }

        scene = Scene(root, StageConfig.getWidth(stage), StageConfig.getHeight(stage))
        scene.stylesheets.add(javaClass.getResource("/css/style.css").toExternalForm())
    }
}