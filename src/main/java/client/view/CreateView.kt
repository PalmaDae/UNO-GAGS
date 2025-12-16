package client.view

import client.config.StageConfig
import client.controller.CreateController
import client.controller.GameController
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.layout.HBox
import javafx.scene.layout.VBox
import javafx.stage.Stage

class CreateView(private val stage: Stage, private val gameController: GameController) {
    private val controller = CreateController(stage, gameController)
    lateinit var scene: Scene

    private val maxPlayersSpinner = Spinner<Int>(2, 4, 4)

    init {
        val rule1 = CheckBox("Stack +2, +4")
        val rule2 = CheckBox("Pick card until you get actual color/num")
        val rule3 = CheckBox("Allow Stuck")

        val cancelButton = Button("Cancel").apply {
            setOnAction { controller.backButton() }
        }

        val createLobbyButton = Button("Create Lobby").apply {
            setOnAction {
                controller.createLobby(
                    maxPlayersSpinner.value,
                    rule3.isSelected,
                    listOf(rule1.isSelected, rule2.isSelected, rule3.isSelected)
                )
            }
        }

        val maxPlayersBox = HBox(10.0, Label("Max Players:"), maxPlayersSpinner).apply {
            alignment = Pos.CENTER
            styleClass.add("setting-row")
            padding = Insets(5.0)
        }

        val root = VBox(20.0).apply {
            alignment = Pos.CENTER
            style = "-fx-padding: 24;"
            children.addAll(
                Label("Create New Room (Defaults Used)"),
                maxPlayersBox,
                Separator(),
                rule1, rule2, rule3,
                createLobbyButton,
                cancelButton
            )
        }

        scene = Scene(root, StageConfig.getWidth(stage), StageConfig.getHeight(stage))
        scene.stylesheets.add(javaClass.getResource("/css/style.css").toExternalForm())
    }
}