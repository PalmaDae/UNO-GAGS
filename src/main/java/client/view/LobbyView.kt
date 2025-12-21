package client.view

import client.common.ResourceLoader
import client.config.StageConfig
import client.controller.GameController
import javafx.application.Platform
import javafx.geometry.Pos
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.control.TextField
import javafx.scene.input.Clipboard
import javafx.scene.input.ClipboardContent
import javafx.scene.layout.HBox
import javafx.scene.layout.VBox
import javafx.stage.Stage
import proto.dto.PlayerInfo

class LobbyView(
    stage: Stage,
    private val gameController: GameController
) {
    var scene: Scene
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
        setOnAction { gameController.onStartGameRequested() }
    }

    private val leaveButton = Button("Leave Lobby").apply {
        setOnAction { gameController.onLeaveRequested() }
    }

    init {
        gameController.setOnStateChanged {
            Platform.runLater {
                updateLobbyView()
            }
        }

        val copyButton = Button("Copy").apply {
            setOnAction {
                val password = gameController.copyPassword()
                passwordField.text = password

                val clipboard = Clipboard.getSystemClipboard()
                val content = ClipboardContent()
                content.putString(password)
                clipboard.setContent(content)
            }
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

    fun updateLobbyView() {
        val lobbyState = gameController.getCurrentLobbyState()
        val currentPlayers = lobbyState?.players ?: emptyList()

        updateRoomStatus(gameController.passwordRoom ?: "")
        updatePlayerList(currentPlayers)
    }

    private fun updatePlayerList(players: List<PlayerInfo>) {
        playersBox.children.clear()
        if (players.isEmpty()) {
            playersBox.children.add(Label("Waiting for players to join..."))
        } else {
            players.forEach { playerInfo: PlayerInfo ->
                val playerBox = createPlayerBox(playerInfo)
                playersBox.children.add(playerBox)
            }
        }
    }

    private fun updateRoomStatus(status: String) {
        passwordField.text = status
    }

    private fun createPlayerBox(playerInfo: PlayerInfo): HBox {
        val avatarView = javafx.scene.image.ImageView().apply {
            fitWidth = 40.0
            fitHeight = 40.0
            isPreserveRatio = true

            image = ResourceLoader.loadAvatar(playerInfo.avatar)
        }

        val nameText = "${playerInfo.username} ${if (playerInfo.isOwner) " (Host)" else ""}"
        val nameLabel = Label(nameText).apply {
            styleClass.add("player-label")
        }

        val playerBox = HBox(10.0, nameLabel, avatarView).apply {
            alignment = Pos.CENTER
        }

        return playerBox
    }
}