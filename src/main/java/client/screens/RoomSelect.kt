package client.screens

import client.controller.GameController
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.layout.BorderPane
import javafx.scene.layout.VBox
import javafx.scene.text.Font

class RoomSelect(
    private val controller: GameController,
    private val playerName: String,
    private val onCreateRoom: () -> Unit,
    private val onJoinRoom: () -> Unit
) : BorderPane() {

    init {
        init()
    }

    fun init() {
        val content = VBox(20.0)
        content.alignment = Pos.CENTER
        content.padding = Insets(40.0)

        val welcome = Label("Welcome, $playerName!")
        welcome.font = Font.font(24.0)
        welcome.style = "-fx-font-weight: bold;"

        val subtitle = Label("Choose an option")
        subtitle.font = Font.font(16.0)

        val createButton = Button("Create New Room")
        createButton.prefWidth = 300.0
        createButton.prefHeight = 60.0
        createButton.style = "-fx-font-size: 18px;"
        createButton.setOnAction { onCreateRoom() }

        val joinButton = Button("Join Existing Room")
        joinButton.prefWidth = 300.0
        joinButton.prefHeight = 60.0
        joinButton.style = "-fx-font-size: 18px;"
        joinButton.setOnAction { onJoinRoom() }

        content.children.addAll(welcome, subtitle, createButton, joinButton)
        center = content
    }

    fun show() {
        // Nothing special
    }

    fun hide() {
        // Nothing to clean up
    }
}
