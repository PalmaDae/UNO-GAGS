package uno_ui

import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.control.*
import javafx.scene.layout.BorderPane
import javafx.scene.layout.GridPane
import javafx.scene.layout.HBox
import javafx.scene.text.Font

class CreateRoomScreen(
    private val controller: GameController,
    private val onBack: () -> Unit
) : BorderPane() {

    private lateinit var roomNameField: TextField
    private lateinit var passwordField: PasswordField
    private lateinit var maxPlayersSpinner: Spinner<Int>
    private lateinit var statusLabel: Label

    init {
        init()
    }

    fun init() {
        val title = Label("Create New Room")
        title.font = Font.font(24.0)
        title.style = "-fx-font-weight: bold;"
        title.padding = Insets(20.0)

        val form = GridPane()
        form.hgap = 10.0
        form.vgap = 15.0
        form.padding = Insets(20.0)
        form.alignment = Pos.CENTER

        form.add(Label("Room Name:"), 0, 0)
        roomNameField = TextField("My Room")
        roomNameField.prefWidth = 250.0
        form.add(roomNameField, 1, 0)

        form.add(Label("Password (optional):"), 0, 1)
        passwordField = PasswordField()
        passwordField.prefWidth = 250.0
        form.add(passwordField, 1, 1)

        form.add(Label("Max Players:"), 0, 2)
        maxPlayersSpinner = Spinner(2, 10, 4)
        maxPlayersSpinner.prefWidth = 250.0
        form.add(maxPlayersSpinner, 1, 2)

        statusLabel = Label("")
        statusLabel.style = "-fx-text-fill: red;"
        form.add(statusLabel, 0, 3, 2, 1)

        val buttons = HBox(10.0)
        buttons.alignment = Pos.CENTER
        buttons.padding = Insets(20.0)

        val createButton = Button("Create")
        createButton.prefWidth = 120.0
        createButton.style = "-fx-font-size: 14px; -fx-background-color: #00aa00; -fx-text-fill: white;"
        createButton.setOnAction { handleCreate() }

        val backButton = Button("Back")
        backButton.prefWidth = 120.0
        backButton.setOnAction { onBack() }

        buttons.children.addAll(createButton, backButton)

        top = title
        center = form
        bottom = buttons
    }

    fun show() {
        roomNameField.requestFocus()
        statusLabel.text = ""
    }

    fun hide() {
        // Nothing to clean up
    }

    private fun handleCreate() {
        val name = roomNameField.text.trim()
        if (name.isEmpty()) {
            statusLabel.text = "Room name required"
            return
        }

        val maxPlayers = maxPlayersSpinner.value
        controller.createRoom(name, maxPlayers)
    }
}
