package client.screens

import client.controller.GameController
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.control.*
import javafx.scene.layout.BorderPane
import javafx.scene.layout.HBox
import javafx.scene.layout.VBox
import javafx.scene.text.Font

class Lobby(
    private val controller: GameController,
    private val onBack: () -> Unit
) : BorderPane() {

    private lateinit var titleLabel: Label
    private lateinit var playersListView: ListView<String>
    private lateinit var startButton: Button
    private lateinit var statusLabel: Label

    init {
        init()
    }

    fun init() {
        val topBar = VBox(10.0)
        topBar.padding = Insets(20.0)
        topBar.alignment = Pos.CENTER

        titleLabel = Label("Room Lobby")
        titleLabel.font = Font.font(24.0)
        titleLabel.style = "-fx-font-weight: bold;"

        statusLabel = Label("Waiting for players...")
        statusLabel.font = Font.font(14.0)

        topBar.children.addAll(titleLabel, statusLabel)

        val centerBox = VBox(10.0)
        centerBox.padding = Insets(20.0)

        val playersLabel = Label("Players:")
        playersLabel.font = Font.font(16.0)

        playersListView = ListView()
        playersListView.prefHeight = 300.0

        centerBox.children.addAll(playersLabel, playersListView)

        val buttons = HBox(10.0)
        buttons.alignment = Pos.CENTER
        buttons.padding = Insets(20.0)

        startButton = Button("Start Game")
        startButton.prefWidth = 150.0
        startButton.style = "-fx-font-size: 14px; -fx-background-color: #00aa00; -fx-text-fill: white;"
        startButton.setOnAction {
            controller.getCurrentRoomId()?.let { roomId ->
                controller.startGame(roomId)
            }
        }

        val leaveButton = Button("Leave Room")
        leaveButton.prefWidth = 150.0
        leaveButton.setOnAction { onBack() }

        buttons.children.addAll(startButton, leaveButton)

        top = topBar
        center = centerBox
        bottom = buttons
    }

    fun show() {
        updateLobbyState()
    }

    fun hide() {
        // Nothing to clean up
    }

    fun updateLobbyState() {
        val lobby = controller.getCurrentLobbyState()
        val roomId = controller.getCurrentRoomId()

        if (roomId != null) {
            titleLabel.text = "Room #$roomId"
        }

        if (lobby != null) {
            playersListView.items.clear()
            lobby.players.forEach { player ->
                val readyText = if (player.isReady) "[READY]" else ""
                playersListView.items.add("${player.username} $readyText")
            }

            statusLabel.text = "Players: ${lobby.players.size} | Status: ${lobby.roomStatus}"
            startButton.isDisable = lobby.players.size < 2
        }
    }
}


