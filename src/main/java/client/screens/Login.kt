package client.screens

import client.controller.GameController
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.control.TextField
import javafx.scene.layout.BorderPane
import javafx.scene.layout.VBox
import javafx.scene.text.Font

class Login(
    private val controller: GameController,
    private val onLogin: (String) -> Unit
) : BorderPane() {

    private lateinit var nameField: TextField
    private lateinit var loginButton: Button
    private lateinit var statusLabel: Label

    init {
        init()
    }

    fun init() {
        val content = VBox(20.0)
        content.alignment = Pos.CENTER
        content.padding = Insets(40.0)

        val title = Label("UNO Game")
        title.font = Font.font(32.0)
        title.style = "-fx-font-weight: bold;"

        val subtitle = Label("Enter your name to start")
        subtitle.font = Font.font(16.0)

        nameField = TextField()
        nameField.promptText = "Player name"
        nameField.maxWidth = 300.0
        nameField.setOnAction { handleLogin() }

        loginButton = Button("Start Game")
        loginButton.prefWidth = 300.0
        loginButton.style = "-fx-font-size: 16px; -fx-padding: 10px;"
        loginButton.setOnAction { handleLogin() }

        statusLabel = Label("")
        statusLabel.style = "-fx-text-fill: red;"

        content.children.addAll(title, subtitle, nameField, loginButton, statusLabel)
        center = content
    }

    fun show() {
        nameField.requestFocus()
        statusLabel.text = ""
    }

    fun hide() {
        // Nothing to clean up
    }

    private fun handleLogin() {
        val name = nameField.text.trim()
        if (name.isEmpty()) {
            statusLabel.text = "Please enter a name"
            return
        }
        if (name.length < 2) {
            statusLabel.text = "Name must be at least 2 characters"
            return
        }

        if (!controller.isConnected()) {
            statusLabel.text = "Connecting to server..."
            val connected = controller.connect()
            if (!connected) {
                statusLabel.text = "Failed to connect to server"
                return
            }
        }

        onLogin(name)
    }
}


