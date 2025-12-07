package uno_ui

import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.control.*
import javafx.scene.layout.*
import uno_proto.dto.Card
import uno_proto.dto.CardType

class GameScreen(private val controller: GameController) : BorderPane() {

    private lateinit var statusLabel: Label
    private lateinit var currentCardLabel: Label
    private lateinit var cardsPane: FlowPane
    private lateinit var chatArea: TextArea
    private lateinit var chatInput: TextField
    private lateinit var playButton: Button

    init {
        init()
    }

    fun init() {
        val content = VBox(10.0)
        content.padding = Insets(10.0)

        statusLabel = Label("Game in progress")
        statusLabel.style = "-fx-font-weight: bold;"

        currentCardLabel = Label("(No card)")
        currentCardLabel.style = "-fx-font-size: 16px; -fx-min-height: 40px;"

        cardsPane = FlowPane(10.0, 10.0)
        cardsPane.style = "-fx-border-color: #ccc; -fx-padding: 10;"
        cardsPane.minHeight = 120.0

        val actions = HBox(10.0)
        actions.alignment = Pos.CENTER
        playButton = Button("Play Card")
        playButton.isDisable = true
        playButton.setOnAction { controller.playCard(null) }
        val drawBtn = Button("Draw Card").apply { setOnAction { controller.drawCard() } }
        val unoBtn = Button("Say UNO!").apply { 
            style = "-fx-background-color: #ff0000; -fx-text-fill: white;" 
            setOnAction { controller.sayUno() }
        }
        actions.children.addAll(playButton, drawBtn, unoBtn)

        chatArea = TextArea()
        chatArea.isEditable = false
        chatArea.prefHeight = 100.0

        val chatBox = HBox(5.0)
        chatInput = TextField()
        chatInput.promptText = "Message..."
        chatInput.setOnAction { sendChat() }
        val sendBtn = Button("Send").apply { setOnAction { sendChat() } }
        HBox.setHgrow(chatInput, Priority.ALWAYS)
        chatBox.children.addAll(chatInput, sendBtn)

        content.children.addAll(
            statusLabel,
            Label("Current Card:"),
            currentCardLabel,
            Label("Your Hand:"),
            cardsPane,
            actions,
            Label("Chat:"),
            chatArea,
            chatBox
        )
        center = content
    }

    fun show() = updateGameState()

    fun hide() {}

    fun updateGameState() {
        controller.getCurrentGameState()?.let { state ->
            statusLabel.text = "Player: ${state.currentPlayerId} | Phase: ${state.gamePhase}"
            state.currentCard?.let { currentCardLabel.text = formatCard(it) }
        }
    }

    fun updateChat() {
        chatArea.text = controller.getChatMessages().joinToString("\n") { 
            "[${it.senderName}]: ${it.content}" 
        }
    }

    private fun formatCard(card: Card) = if (card.type == CardType.NUMBER) {
        "${card.color} ${card.number}"
    } else {
        "${card.color} ${card.type}"
    }

    private fun sendChat() {
        chatInput.text.trim().takeIf { it.isNotEmpty() }?.let {
            controller.sendChat(it)
            chatInput.clear()
        }
    }
}
