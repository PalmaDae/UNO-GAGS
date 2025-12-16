package client.view

import client.common.ResourceLoader
import client.config.StageConfig
import client.controller.GameController
import javafx.application.Platform
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.image.ImageView
import javafx.scene.input.KeyCode
import javafx.scene.layout.*
import javafx.scene.text.Font
import javafx.stage.Stage
import proto.dto.Card
import proto.dto.CardColor
import proto.dto.GamePhase

class GameView(
    private val stage: Stage,
    private val gameController: GameController
) {
    lateinit var scene: Scene

    private val centerCardDisplay = ImageView()
    private val playerHandBox = HBox(16.0)
    private val gameStatusLabel = Label("Waiting to start...")
    private val unoButton = Button("Say UNO! (SPACE)")
    private val drawButton = Button("Draw Card")

    private val CARD_WIDTH = 90.0
    private val CARD_HEIGHT = 135.0
    private val SMALL_CARD_WIDTH = 70.0
    private val SMALL_CARD_HEIGHT = 105.0


    init {
        gameController.setOnStateChanged {
            Platform.runLater { updateUI() }
        }

        centerCardDisplay.fitWidth = CARD_WIDTH * 1.5
        centerCardDisplay.fitHeight = CARD_HEIGHT * 1.5
        val centerBox = VBox(centerCardDisplay).apply {
            alignment = Pos.CENTER
        }

        playerHandBox.alignment = Pos.CENTER
        playerHandBox.minHeight = CARD_HEIGHT + 20.0

        unoButton.apply {
            prefWidth = 160.0
            setOnAction { handleSayUno() }
        }
        val unoWrapper = VBox(10.0, unoButton).apply {
            alignment = Pos.CENTER
        }

        drawButton.apply {
            prefWidth = 120.0
            setOnAction { handleDrawCard() }
        }

        val exitButton = Button("Exit").apply {
            prefWidth = 120.0
            setOnAction { gameController.closedGame() }
        }

        val topControls = HBox(160.0, exitButton, drawButton).apply {
            alignment = Pos.TOP_RIGHT
            padding = Insets(16.0)
        }

        val leftPlayer = createClosedDeck(4, Pos.CENTER_LEFT)
        val rightPlayer = createClosedDeck(4, Pos.CENTER_RIGHT)
        val topPlayer = HBox(8.0).apply {
            alignment = Pos.CENTER
            repeat(4) {
                children.add(
                    ImageView(ResourceLoader.loadCardImage("NONE", "BACK")).apply {
                        fitWidth = SMALL_CARD_WIDTH
                        fitHeight = SMALL_CARD_HEIGHT
                    }
                )
            }
        }

        val statusArea = VBox(10.0, gameStatusLabel).apply {
            alignment = Pos.CENTER
            gameStatusLabel.font = Font(18.0)
        }

        val root = BorderPane().apply {
            padding = Insets(20.0)

            this.center = centerBox
            this.bottom = VBox(10.0, unoWrapper, playerHandBox).apply { alignment = Pos.CENTER }
            this.left = VBox(leftPlayer).apply { alignment = Pos.CENTER_LEFT }
            this.right = VBox(rightPlayer).apply { alignment = Pos.CENTER_RIGHT }
            this.top = VBox(topControls, topPlayer, statusArea).apply { alignment = Pos.CENTER }
        }

        scene = Scene(root, StageConfig.getWidth(stage), StageConfig.getHeight(stage))
        scene.stylesheets.add(javaClass.getResource("/styles/game.css").toExternalForm())

        scene.setOnKeyPressed { event ->
            if (event.code == KeyCode.SPACE) unoButton.fire()
        }

        updateUI()
    }

    private fun createClosedDeck(count: Int, pos: Pos): VBox {
        return VBox(8.0).apply {
            alignment = pos
            repeat(count) {
                children.add(
                    ImageView(ResourceLoader.loadCardImage("NONE", "BACK")).apply {
                        fitWidth = SMALL_CARD_WIDTH
                        fitHeight = SMALL_CARD_HEIGHT
                    }
                )
            }
        }
    }

    private fun handleDrawCard() {
        gameController.getCurrentRoomId()?.let { roomId ->
            gameController.drawCard(roomId)
        }
    }

    private fun handleSayUno() {
        gameController.getCurrentRoomId()?.let { roomId ->
            gameController.sayUno(roomId)
        }
    }

    private fun handleCardClick(cardIndex: Int, card: Card) {
        gameController.handleCardSelection(cardIndex, card)
    }

    private fun renderPlayerHand(hand: List<Card>) {
        playerHandBox.children.clear()
        hand.forEachIndexed { index, card ->
            val cardImage = ResourceLoader.loadCardImage(
                card.color.name,
                card.type.name
            )

            val cardImageView = ImageView(cardImage).apply {
                fitWidth = CARD_WIDTH
                fitHeight = CARD_HEIGHT
            }

            val cardContainer = StackPane(cardImageView).apply {
                style = if (gameController.getSelectedCardIndex() == index) {
                    "-fx-border-color: yellow; -fx-border-width: 3;"
                } else {
                    "-fx-border-color: transparent; -fx-border-width: 3;"
                }

                setOnMouseClicked {
                    handleCardClick(index, card)
                }
            }

            playerHandBox.children.add(cardContainer)
        }
    }

    private fun showColorChooser(isWildCard: Boolean) {
        val message = if (isWildCard) "Choose a new color:" else "A Wild card was played. Choose the next color:"

        val chooserStage = Stage().apply {
            title = "Choose Color"
            initOwner(stage)
        }

        val colors = listOf(CardColor.RED, CardColor.BLUE, CardColor.GREEN, CardColor.YELLOW)
        val colorButtons = HBox(10.0).apply {
            alignment = Pos.CENTER
            padding = Insets(20.0)

            colors.forEach { color ->
                val btn = Button(color.name).apply {
                    style = "-fx-background-color: ${ResourceLoader.toCssColor(color)}; -fx-text-fill: black;"
                    setOnAction {
                        gameController.getCurrentRoomId()?.let { roomId ->
                            gameController.chooseColor(roomId, color)
                            chooserStage.close()
                        }
                    }
                }
                children.add(btn)
            }
        }

        val layout = VBox(20.0, Label(message).apply { font = Font(Font.getDefault().name, 14.0) }, colorButtons).apply {
            alignment = Pos.CENTER
            padding = Insets(20.0)
        }

        chooserStage.scene = Scene(layout, 300.0, 150.0)
        chooserStage.showAndWait()
    }

    fun updateUI() {
        val gameState = gameController.getCurrentGameState()
        val myHand = gameController.getMyHand()
        val myPlayerId = gameController.getMyPlayerId()

        if (gameState == null || myPlayerId == null) return

        val currentCard = gameState.currentCard
        val isMyTurn = gameState.currentPlayerId == myPlayerId

        val cardColorName = currentCard?.color?.name ?: "NONE"
        val cardValueName = currentCard?.type?.name ?: "BACK"

        centerCardDisplay.image = ResourceLoader.loadCardImage(
            cardColorName,
            cardValueName
        )

        val turnStatus = if (isMyTurn) " (YOUR TURN)" else ""
        gameStatusLabel.text = "Current Player: ${gameState.currentPlayerId}$turnStatus"

        renderPlayerHand(myHand)

        val selectedCardIndex = gameController.getSelectedCardIndex()
        val selectedCard = myHand.getOrNull(selectedCardIndex)

        val selectedIsWild = selectedCard != null &&
                selectedCard.type.name.contains("WILD")

        if (isMyTurn && (gameState.gamePhase == GamePhase.CHOOSING_COLOR || selectedIsWild)) {

            showColorChooser(true)

            if (selectedIsWild) {
                gameController.setSelectedCardIndex(-1)
            }
        }
    }
}