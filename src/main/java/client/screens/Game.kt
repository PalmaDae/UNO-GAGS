package client.screens

import client.controller.GameController
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.control.*
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.layout.*
import javafx.scene.shape.Rectangle
import proto.dto.Card
import proto.dto.CardColor
import proto.dto.CardType
import proto.dto.GamePhase

class Game(private val controller: GameController) : BorderPane() {

    private lateinit var statusLabel: Label
    private lateinit var playersContainer: HBox
    private lateinit var gameAreaCenter: VBox
    private lateinit var currentCardView: StackPane
    private lateinit var deckCountLabel: Label
    private lateinit var handPane: HBox
    private lateinit var chatArea: TextArea
    private lateinit var chatInput: TextField
    private lateinit var playButton: Button
    private lateinit var drawButton: Button
    private lateinit var unoButton: Button
    private val cardButtons = mutableMapOf<Int, ToggleButton>()

    init {
        loadStylesheet()
        init()
    }

    private fun loadStylesheet() {
        val cssResource = this::class.java.classLoader.getResource("styles/game.css")
        if (cssResource != null) {
            stylesheets.add(cssResource.toExternalForm())
        }
    }

    fun init() {
        style = "-fx-background-color: #1a4d2e;"
        padding = Insets(10.0)

        left = createChatPanel()
        center = createGameArea()
        right = createRightPanel()
        bottom = createHandPanel()
    }

    private fun createChatPanel(): VBox {
        val chatPanel = VBox(5.0)
        chatPanel.prefWidth = 200.0
        chatPanel.style = "-fx-border-color: #333; -fx-border-width: 1; -fx-padding: 10;"

        val title = Label("Chat")
        title.style = "-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: white;"

        chatArea = TextArea()
        chatArea.isEditable = false
        chatArea.prefHeight = 300.0
        chatArea.style = "-fx-control-inner-background: #2a5f3f; -fx-text-fill: #ddd;"
        VBox.setVgrow(chatArea, Priority.ALWAYS)

        val inputBox = HBox(5.0)
        chatInput = TextField()
        chatInput.promptText = "Type message..."
        chatInput.style = "-fx-font-size: 12px; -fx-padding: 5;"
        chatInput.setOnAction { sendChat() }

        val sendBtn = Button("Send")
        sendBtn.style = "-fx-padding: 5 15; -fx-font-size: 11px; -fx-background-color: #2a5f3f; -fx-text-fill: white;"
        sendBtn.setOnAction { sendChat() }

        HBox.setHgrow(chatInput, Priority.ALWAYS)
        inputBox.children.addAll(chatInput, sendBtn)

        chatPanel.children.addAll(title, chatArea, inputBox)
        return chatPanel
    }

    private fun createGameArea(): VBox {
        gameAreaCenter = VBox(15.0)
        gameAreaCenter.alignment = Pos.TOP_CENTER
        gameAreaCenter.style = "-fx-padding: 20; -fx-background-color: #0d3b1f;"

        statusLabel = Label("Waiting for game state...")
        statusLabel.style = "-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #fff;"

        playersContainer = HBox(10.0)
        playersContainer.alignment = Pos.CENTER
        playersContainer.style = "-fx-padding: 10; -fx-background-color: rgba(0,0,0,0.2); -fx-border-radius: 5;"
        playersContainer.maxHeight = 80.0

        val centerGameBox = VBox(20.0)
        centerGameBox.alignment = Pos.CENTER

        val cardsLayout = HBox(30.0)
        cardsLayout.alignment = Pos.CENTER

        val deckBox = VBox(10.0)
        deckBox.alignment = Pos.CENTER
        val deckLabel = Label("Deck")
        deckLabel.style = "-fx-font-size: 12px; -fx-text-fill: #fff;"
        val deckCard = Rectangle(80.0, 120.0)
        deckCard.style = "-fx-fill: #2a5f3f; -fx-stroke: #fff; -fx-stroke-width: 2;"
        deckCard.arcWidth = 10.0
        deckCard.arcHeight = 10.0
        deckCountLabel = Label("52")
        deckCountLabel.style = "-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #fff;"
        deckBox.children.addAll(deckCard, deckLabel, deckCountLabel)

        currentCardView = StackPane()
        currentCardView.prefWidth = 100.0
        currentCardView.prefHeight = 150.0
        currentCardView.style = "-fx-border-color: #fff; -fx-border-width: 2; -fx-border-radius: 5;"
        val currentCardLabel = Label("Waiting...")
        currentCardLabel.style = "-fx-text-fill: #fff;"
        currentCardView.children.add(currentCardLabel)

        cardsLayout.children.addAll(deckBox, currentCardView)
        centerGameBox.children.addAll(cardsLayout)

        gameAreaCenter.children.addAll(statusLabel, playersContainer, centerGameBox)
        return gameAreaCenter
    }

    private fun createRightPanel(): VBox {
        val rightPanel = VBox(10.0)
        rightPanel.prefWidth = 150.0
        rightPanel.style = "-fx-padding: 10; -fx-border-color: #333; -fx-border-width: 1; -fx-border-radius: 5;"

        val actionsLabel = Label("Actions")
        actionsLabel.style = "-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: white;"

        playButton = Button("Play Card")
        playButton.isDisable = true
        playButton.style = "-fx-padding: 10 20; -fx-font-size: 11px; -fx-background-color: #2a5f3f; -fx-text-fill: white;"
        playButton.setOnAction {
            controller.getCurrentRoomId()?.let { roomId ->
                controller.playCard(roomId, null)
            }
        }

        drawButton = Button("Draw Card")
        drawButton.style = "-fx-padding: 10 20; -fx-font-size: 11px; -fx-background-color: #3d6b4f; -fx-text-fill: white;"
        drawButton.setOnAction {
            controller.getCurrentRoomId()?.let { roomId ->
                controller.drawCard(roomId)
            }
        }

        unoButton = Button("Say UNO!")
        unoButton.style = "-fx-padding: 10 20; -fx-font-size: 11px; -fx-background-color: #d32f2f; -fx-text-fill: white; -fx-font-weight: bold;"
        unoButton.setOnAction {
            controller.getCurrentRoomId()?.let { roomId ->
                controller.sayUno(roomId)
            }
        }

        rightPanel.children.addAll(actionsLabel, playButton, drawButton, unoButton)
        return rightPanel
    }

    private fun createHandPanel(): HBox {
        val handPanel = HBox(8.0)
        handPanel.padding = Insets(10.0)
        handPanel.style = "-fx-background-color: #0d3b1f; -fx-border-color: #333; -fx-border-width: 1 0 0 0;"
        handPanel.alignment = Pos.CENTER_LEFT

        val title = Label("Your Hand:")
        title.style = "-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #fff;"

        handPane = HBox(8.0)
        handPane.alignment = Pos.CENTER_LEFT
        handPane.style = "-fx-padding: 5;"
        HBox.setHgrow(handPane, Priority.ALWAYS)

        handPanel.children.addAll(title, handPane)
        return handPanel
    }

    fun show() = updateGameState()

    fun hide() {}

    fun updateGameState() {
        controller.getCurrentGameState()?.let { state ->
            val isMyTurn = state.currentPlayerId == controller.getMyPlayerId()
            statusLabel.text = when (state.gamePhase) {
                GamePhase.WAITING_TURN -> if (isMyTurn) "Your turn!" else "Waiting for opponent..."
                GamePhase.CHOOSING_COLOR -> "Choose a color for Wild card"
                GamePhase.DRAWING_CARD -> "You must draw a card"
                GamePhase.FINISHED -> "Game Finished!"
            }

            updatePlayersDisplay(state.players, state.currentPlayerId)
            updateCurrentCard(state.currentCard)

            val hand = controller.getMyHand()
            updateHand(hand, isMyTurn)

            playButton.isDisable = !isMyTurn || controller.getSelectedCardIndex() < 0
            drawButton.isDisable = !isMyTurn
            unoButton.isDisable = !isMyTurn
        }
    }

    private fun updatePlayersDisplay(players: Map<Long, proto.dto.PlayerGameInfo>, currentPlayerId: Long) {
        playersContainer.children.clear()
        players.forEach { (userId, info) ->
            val playerBox = VBox(3.0)
            playerBox.alignment = Pos.CENTER
            
            val isCurrentPlayer = userId == currentPlayerId
            val bgColor = if (isCurrentPlayer) "rgba(76, 175, 80, 0.7)" else "rgba(42, 95, 63, 0.5)"
            val borderColor = if (isCurrentPlayer) "#4caf50" else "#333"
            playerBox.style = "-fx-padding: 5; -fx-background-color: $bgColor; -fx-border-radius: 5; -fx-border-color: $borderColor; -fx-border-width: 2;"

            val nameLabel = Label(info.username)
            nameLabel.style = "-fx-font-size: 11px; -fx-text-fill: #fff; -fx-font-weight: bold;"

            val cardCountLabel = Label("${info.cardCount} cards")
            cardCountLabel.style = "-fx-font-size: 10px; -fx-text-fill: #ddd;"

            val statusLabel = Label(if (info.hasUno) "UNO!" else if (isCurrentPlayer) "→ PLAYING" else "")
            statusLabel.style = "-fx-font-size: 10px; -fx-text-fill: ${if (info.hasUno) "#ffcc00" else if (isCurrentPlayer) "#4caf50" else "#ddd"}; -fx-font-weight: bold;"

            playerBox.children.addAll(nameLabel, cardCountLabel, statusLabel)
            playersContainer.children.add(playerBox)
        }
    }

    private fun updateCurrentCard(card: Card?) {
        currentCardView.children.clear()
        if (card != null) {
            val imageView = createCardImage(card, 80.0, 120.0)
            currentCardView.children.add(imageView)
        } else {
            val placeholder = Label("No card")
            placeholder.style = "-fx-text-fill: #fff;"
            currentCardView.children.add(placeholder)
        }
    }

    private fun updateHand(hand: List<Card>, isMyTurn: Boolean) {
        handPane.children.clear()
        cardButtons.clear()

        hand.forEachIndexed { index, card ->
            val cardButton = createCardButton(card, index, isMyTurn)
            cardButtons[index] = cardButton
            handPane.children.add(cardButton)
        }
    }

    private fun createCardButton(card: Card, index: Int, enabled: Boolean): ToggleButton {
        val button = ToggleButton()
        button.style = "-fx-padding: 0; -fx-background-color: transparent; -fx-border-width: 0;"

        val imageView = createCardImage(card, 60.0, 90.0)
        button.graphic = imageView

        button.selectedProperty().addListener { _, _, newValue ->
            if (newValue) {
                controller.setSelectedCardIndex(index)
                playButton.isDisable = false
                cardButtons.values.forEach { btn ->
                    if (btn !== button) {
                        btn.isSelected = false
                        btn.style = "-fx-border-width: 0;"
                    }
                }
                button.style = "-fx-border-color: #ffff00; -fx-border-width: 3; -fx-border-radius: 5;"
            } else {
                controller.setSelectedCardIndex(-1)
                button.style = "-fx-border-width: 0;"
            }
        }

        button.isDisable = !enabled

        return button
    }

    private fun createCardImage(card: Card, width: Double, height: Double): ImageView {
        val imagePath = getCardImagePath(card)
        val image = try {
            val resource = this::class.java.classLoader.getResourceAsStream(imagePath)
            if (resource != null) {
                Image(resource)
            } else {
                createPlaceholderImage(card, width, height)
            }
        } catch (e: Exception) {
            createPlaceholderImage(card, width, height)
        }

        return ImageView(image).apply {
            fitWidth = width
            fitHeight = height
            isPreserveRatio = true
        }
    }

    private fun createPlaceholderImage(card: Card, width: Double, height: Double): Image {
        val colorStr = when (card.color) {
            CardColor.RED -> "#e53935"
            CardColor.BLUE -> "#1e88e5"
            CardColor.GREEN -> "#43a047"
            CardColor.YELLOW -> "#fbc02d"
            CardColor.WILD -> "#616161"
        }

        val text = when (card.type) {
            CardType.NUMBER -> card.number?.toString() ?: "?"
            CardType.SKIP -> "SKIP"
            CardType.REVERSE -> "REV"
            CardType.DRAW_TWO -> "+2"
            CardType.WILD -> "WILD"
            CardType.WILD_DRAW_FOUR -> "+4"
        }

        return javafx.scene.image.WritableImage(width.toInt(), height.toInt())
    }

    private fun getCardImagePath(card: Card): String {
        val colorDir = card.color.name.lowercase()
        val fileName = when (card.type) {
            CardType.NUMBER -> "${card.number}.png"
            CardType.SKIP -> "skip.png"
            CardType.REVERSE -> "reverse.png"
            CardType.DRAW_TWO -> "+2.png"
            CardType.WILD -> "wildcard.png"
            CardType.WILD_DRAW_FOUR -> "+4.png"
        }
        return "images/cards/$colorDir/$fileName"
    }

    fun updateChat() {
        chatArea.text = controller.getChatMessages().joinToString("\n") {
            "[${it.senderName}]: ${it.content}"
        }
        chatArea.appendText("")
    }

    private fun sendChat() {
        chatInput.text.trim().takeIf { it.isNotEmpty() }?.let { text ->
            controller.getCurrentRoomId()?.let { roomId ->
                controller.sendChat(roomId, text)
                chatInput.clear()
            }
        }
    }
}
