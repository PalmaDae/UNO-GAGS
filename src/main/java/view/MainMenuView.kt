package view

import controller.MainMenuController
import javafx.geometry.Pos
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.layout.VBox
import javafx.stage.Stage
import config.StageConfig

class MainMenuView(private val stage: Stage) {
    private val controller = MainMenuController(stage)
    lateinit var scene: Scene

    init {
        val controller = MainMenuController(stage)

        val createButton = Button("Create").apply { setOnAction {controller.createButton()} }
        val joinButton = Button("Join").apply { setOnAction {controller.joinButton()} }
        val exitButton = Button("Exit").apply {setOnAction {controller.exitButton()}}

        listOf(createButton, joinButton, exitButton).forEach { it.prefWidth = 200.0 }

        val root = VBox(16.0);

        root.children.addAll(createButton, joinButton,  exitButton)
        root.alignment = Pos.CENTER

        root.style = "-fx.padding: 24;"

        scene = Scene(root, StageConfig.getWidth(stage), StageConfig.getHeight(stage))
        scene.stylesheets.add(javaClass.getResource("/css/style.css").toExternalForm())

        stage.scene = scene;
        stage.title = "Main Menu"
        stage.show()
    }

}