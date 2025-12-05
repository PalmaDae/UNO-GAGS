package view

import controller.MainMenuController
import javafx.*;
import javafx.application.Application
import javafx.geometry.Pos
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.layout.VBox
import javafx.stage.Stage

class MainMenuView : Application() {

    override fun start(primaryStage: Stage) {
        val controller = MainMenuController(primaryStage)

        val createButton = Button("Create").apply { setOnAction {controller.createButton()} }
        val joinButton = Button("Join").apply { setOnAction {controller.joinButton()} }
        val exitButton = Button("Exit").apply {setOnAction {controller.exitButton()}}

        listOf(createButton, joinButton, exitButton).forEach { it.prefWidth = 200.0 }

        val root = VBox(16.0);

        root.children.addAll(createButton, joinButton,  exitButton)
        root.alignment = Pos.CENTER

        root.style = "-fx.padding: 24;"

        val scene = Scene(root, 400.0, 600.0)

        primaryStage.scene = scene;
        primaryStage.title = "Main Menu"
        primaryStage.show()
    }
}