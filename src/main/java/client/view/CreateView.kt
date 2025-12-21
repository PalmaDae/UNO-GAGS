package client.view

import client.config.StageConfig
import client.controller.GameController
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.control.CheckBox
import javafx.scene.control.Label
import javafx.scene.control.Separator
import javafx.scene.control.Spinner
import javafx.scene.layout.HBox
import javafx.scene.layout.VBox
import javafx.stage.Stage

class CreateView(
    stage: Stage,
    private val gameController: GameController
) {
    var scene: Scene
    private val maxPlayersSpinner = Spinner<Int>(2, 4, 4)

    init {
        val rule1 = CheckBox("Stack +2, +4")
        val rule2 = CheckBox("Pick card until you get actual color/num")
        val rule3 = CheckBox("Allow Stuck")

        val cancelButton = Button("Cancel").apply {
            setOnAction { gameController.onBackRequested() }
        }

        val createLobbyButton = Button("Create Lobby").apply {
            setOnAction {
                gameController.onCreateLobbyRequested(
                    maxPlayers = maxPlayersSpinner.value,
                    allowStuck = rule1.isSelected,
                    allowStuckCards = rule3.isSelected,
                    infinityDrawing = rule2.isSelected
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
                Label("Create New Room"),
                maxPlayersBox,
                Separator(),
                rule1,
                rule2,
                rule3,
                createLobbyButton,
                cancelButton
            )
        }

        scene = Scene(root, StageConfig.getWidth(stage), StageConfig.getHeight(stage))
        scene.stylesheets.add(javaClass.getResource("/css/style.css").toExternalForm())
    }
}