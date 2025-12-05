package view

import controller.CreateController
import javafx.geometry.Pos
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.control.TextField
import javafx.scene.layout.VBox
import javafx.stage.Stage

class CreateView(private val stage: Stage) {
    private val controller = CreateController(stage)

    lateinit var scene: Scene;

    init {
        val nameField = TextField().apply { promptText = "Place your name here" }
        val createButton = Button("Create").apply {  }

        val backButton = Button("Back").apply { setOnAction { controller.backButton() } }

        val root = VBox(16.0, nameField, createButton, backButton).apply {
            alignment = Pos.CENTER
            style = "-fx-padding: 24;"
        }

        scene = Scene(root, 400.0, 600.0)
    }

}