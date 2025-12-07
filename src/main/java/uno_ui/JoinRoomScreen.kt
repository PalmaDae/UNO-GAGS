package uno_ui

import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.control.TextField
import javafx.scene.layout.BorderPane
import javafx.scene.layout.HBox
import javafx.scene.layout.VBox
import javafx.scene.text.Font

class JoinRoomScreen(
    private val controller: GameController,
    private val onBack: () -> Unit
) : BorderPane() {

    private lateinit var roomIdField: TextField
    private lateinit var statusLabel: Label

    init {
        init()
    }

    fun init() {
        val content = VBox(20.0)
        content.alignment = Pos.CENTER
        content.padding = Insets(40.0)

        val title = Label("Join Room")
        title.font = Font.font(24.0)
        title.style = "-fx-font-weight: bold;"

        val subtitle = Label("Enter room ID")
        subtitle.font = Font.font(16.0)

        roomIdField = TextField()
        roomIdField.promptText = "Room ID"
        roomIdField.maxWidth = 300.0
        roomIdField.setOnAction { handleJoin() }

        statusLabel = Label("")
        statusLabel.style = "-fx-text-fill: red;"

        val buttons = HBox(10.0)
        buttons.alignment = Pos.CENTER

        val joinButton = Button("Join")
        joinButton.prefWidth = 140.0
        joinButton.style = "-fx-font-size: 14px; -fx-background-color: #0066cc; -fx-text-fill: white;"
        joinButton.setOnAction { handleJoin() }

        val backButton = Button("Back")
        backButton.prefWidth = 140.0
        backButton.setOnAction { onBack() }

        buttons.children.addAll(joinButton, backButton)

        content.children.addAll(title, subtitle, roomIdField, statusLabel, buttons)
        center = content
    }

    fun show() {
        roomIdField.requestFocus()
        statusLabel.text = ""
    }

    fun hide() {
        // Nothing to clean up
    }

    private fun handleJoin() {
        val roomIdText = roomIdField.text.trim()
        if (roomIdText.isEmpty()) {
            statusLabel.text = "Please enter room ID"
            return
        }

        try {
            val roomId = roomIdText.toLong()
            controller.joinRoom(roomId)
        } catch (e: NumberFormatException) {
            statusLabel.text = "Invalid room ID"
        }
    }
}
