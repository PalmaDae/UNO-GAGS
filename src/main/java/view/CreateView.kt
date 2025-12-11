package view

import config.StageConfig
import controller.CreateController
import controller.GameController
import javafx.geometry.Pos
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.control.CheckBox
import javafx.scene.control.TextField
import javafx.scene.control.ToggleGroup
import javafx.scene.layout.HBox
import javafx.scene.layout.VBox
import javafx.scene.paint.Color
import javafx.scene.shape.Rectangle
import javafx.stage.Stage

class CreateView(private val stage: Stage, private val gameController: GameController) {
    private val controller = CreateController(stage, gameController);
    lateinit var scene: Scene;

    init {
        val rule1 = CheckBox("Stack +2, +4")
        val rule2 = CheckBox("Pick card until you not get actually color or num")
        val rule3 = CheckBox("Some rule yooo")

        val cancelButton = Button("Cancel").apply {
            setOnAction { controller.backButton() }
        }

        val createLobbyButton = Button("Create Lobby").apply {
            setOnAction { controller.createLobby(rule1.isSelected, rule2.isSelected, rule3.isSelected) }
        }

        val root = VBox(20.0, rule1, rule2, rule3, createLobbyButton, cancelButton).apply {
            alignment = Pos.CENTER
            style = "-fx-padding: 24;"
        }

        scene = Scene(root, StageConfig.getWidth(stage), StageConfig.getHeight(stage))
        scene.stylesheets.add(javaClass.getResource("/css/style.css").toExternalForm())
    }
}