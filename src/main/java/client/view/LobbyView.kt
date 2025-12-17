package client.view

import client.config.StageConfig
import client.controller.GameController
import client.controller.LobbyController
import javafx.application.Platform
import javafx.geometry.Pos
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.control.TextField
import javafx.scene.layout.HBox
import javafx.scene.layout.VBox
import javafx.stage.Stage
import proto.dto.PlayerInfo

class LobbyView(
    private val stage: Stage,
    private val gameController: GameController
) {
    var scene: Scene
    private val controller = LobbyController(gameController)

    private val passwordField = TextField().apply {
        isEditable = false
        alignment = Pos.CENTER
        prefWidth = 160.0
        maxWidth = 160.0
        minWidth = 160.0
        styleClass.add("uno-input")
    }
    private val playersBox = VBox(8.0).apply { alignment = Pos.CENTER }

    private val startGameButton = Button("Start game").apply {
        setOnAction { controller.startGame() }
        isDisable = true
    }

    private val leaveButton = Button("Leave Lobby").apply {
        setOnAction {
            gameController.disconnect()
            stage.scene = MainMenuView(stage, gameController).scene
        }
    }

    init {
        gameController.setOnStateChanged {
            Platform.runLater {
                updateLobbyView()
            }
        }

        val copyButton = Button("Copy").apply {
            setOnAction { controller.copyPassword(passwordField.text) }
        }

        val passwordBox = HBox(10.0, passwordField, copyButton).apply {
            alignment = Pos.CENTER
        }

        val root = VBox(20.0).apply {
            alignment = Pos.TOP_CENTER
            style = "-fx-padding: 20;"
            children.addAll(
                Label("Room ID (Key)"),
                passwordBox,
                Label("Current Players:"),
                playersBox,
                startGameButton,
                leaveButton
            )
        }

        scene = Scene(root, StageConfig.getWidth(stage), StageConfig.getHeight(stage))
        scene.stylesheets.add(javaClass.getResource("/css/style.css")?.toExternalForm())

        updateLobbyView()
    }

    private fun updateLobbyView() {
        val lobbyState = gameController.getCurrentLobbyState()

        val currentPlayers = lobbyState?.players ?: emptyList()
        val myPlayerId = gameController.getMyPlayerId()

        passwordField.text = gameController.passwordRoom

        val isHost = currentPlayers.any { it.userId == myPlayerId && it.isOwner }

        playersBox.children.clear()
        if (currentPlayers.isEmpty()) {
            playersBox.children.add(Label("Waiting for players to join..."))
        } else {
            currentPlayers.forEach { playerInfo: PlayerInfo ->
                val playerBox = createPlayerBox(playerInfo)
                playersBox.children.add(playerBox)
            }
        }

        startGameButton.isVisible = isHost
        startGameButton.isDisable = currentPlayers.size < 2
    }

    private fun createPlayerBox(playerInfo: PlayerInfo): HBox {
        val nameText = "${playerInfo.username} ${if (playerInfo.isOwner) " (Host)" else ""}"
        val nameLabel = Label(nameText).apply {
            styleClass.add("player-label")
        }

        val playerBox = HBox(10.0, nameLabel).apply {
            alignment = Pos.CENTER
        }

        return playerBox
    }
}