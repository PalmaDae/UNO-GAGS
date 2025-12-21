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
import javafx.scene.paint.Color
import javafx.scene.text.Font
import javafx.stage.Stage
import proto.dto.*

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
    private val root = BorderPane()
    private val topPlayerBox = HBox(8.0).apply { alignment = Pos.CENTER }
    private val leftPlayerBox = VBox(8.0).apply { alignment = Pos.CENTER_LEFT }
    private val rightPlayerBox = VBox(8.0).apply { alignment = Pos.CENTER_RIGHT }
    private var isColorChooserShowing = false

    init {
        gameController.setOnStateChanged { updateUI() }

        initializeComponents()
        setupLayouts()
        setupEventHandlers()

        updateUI()
    }

    private fun initializeComponents() {
        centerCardDisplay.fitWidth = CARD_WIDTH * 1.5
        centerCardDisplay.fitHeight = CARD_HEIGHT * 1.5

        playerHandBox.alignment = Pos.CENTER
        playerHandBox.minHeight = CARD_HEIGHT + 20.0

        unoButton.apply {
            prefWidth = 160.0
            setOnAction { handleSayUno() }
        }

        drawButton.apply {
            prefWidth = 120.0
            setOnAction { handleDrawCard() }
        }
    }

    private fun setupLayouts() {
        val centerBox = VBox(centerCardDisplay).apply {
            alignment = Pos.CENTER
        }

        val unoWrapper = VBox(10.0, unoButton).apply {
            alignment = Pos.CENTER
        }

        val exitButton = Button("Exit").apply {
            prefWidth = 120.0
            setOnAction { gameController.closedGame() }
        }

        val topControls = HBox(160.0, exitButton, drawButton).apply {
            alignment = Pos.TOP_RIGHT
            padding = Insets(16.0)
        }

        val statusArea = VBox(10.0, gameStatusLabel).apply {
            alignment = Pos.CENTER
            gameStatusLabel.font = Font(18.0)
        }

        root.apply {
            padding = Insets(20.0)
            this.center = centerBox
            this.bottom = VBox(10.0, unoWrapper, playerHandBox).apply { alignment = Pos.CENTER }
            this.left = leftPlayerBox
            this.right = rightPlayerBox
            this.top = VBox(topControls, topPlayerBox, statusArea).apply { alignment = Pos.CENTER }
        }

        root.styleClass.add("game-screen")

        scene = Scene(root, StageConfig.getWidth(stage), StageConfig.getHeight(stage))

        val cssUrl = GameView::class.java.getResource("/css/style.css")
        if (cssUrl != null) {
            scene.stylesheets.add(cssUrl.toExternalForm())
        } else {
            System.err.println("FATAL ERROR: style.css not found in resources/css/ (Path used: /css/style.css)")
        }
    }

    private fun setupEventHandlers() {
        scene.setOnKeyPressed { event ->
            if (event.code == KeyCode.SPACE) unoButton.fire()
        }
    }

    private fun autoDetectMyId() {
        // Используем свойство напрямую (без скобок)
        if (gameController.myPlayerId == null) {
            val myName = gameController.currentUserName
            val gameState = gameController.getCurrentGameState()

            // Ищем в Map: players - это Map<Long, PlayerGameInfo>
            val meInGame = gameState?.players?.entries?.find { it.value.username == myName }

            if (meInGame != null) {
                gameController.myPlayerId = meInGame.key
                println("ID auto-detected: ${meInGame.key}")
            }
        }
    }

    private fun renderOpponents(opponents: List<PlayerDisplayInfo>) {
        topPlayerBox.children.clear()
        leftPlayerBox.children.clear()
        rightPlayerBox.children.clear()

        if (opponents.isEmpty()) return

        gameController.myPlayerId ?: return
        val currentTurnPlayerId = gameController.getCurrentGameState()?.currentPlayerId

        val sortedOpponents = gameController.getOpponentsInOrder()

        println(sortedOpponents.size)

        when (sortedOpponents.size) {
            1 -> {
                topPlayerBox.children.add(createOpponentDisplay(sortedOpponents[0], currentTurnPlayerId?.toString()))
            }

            2 -> {
                leftPlayerBox.children.add(
                    createOpponentDisplay(
                        sortedOpponents[0],
                        currentTurnPlayerId?.toString(),
                        isVertical = true
                    )
                )
                rightPlayerBox.children.add(
                    createOpponentDisplay(
                        sortedOpponents[1],
                        currentTurnPlayerId?.toString(),
                        isVertical = true
                    )
                )
            }

            3 -> {
                leftPlayerBox.children.add(
                    createOpponentDisplay(
                        sortedOpponents[0],
                        currentTurnPlayerId?.toString(),
                        isVertical = true
                    )
                )
                topPlayerBox.children.add(createOpponentDisplay(sortedOpponents[1], currentTurnPlayerId?.toString()))
                rightPlayerBox.children.add(
                    createOpponentDisplay(
                        sortedOpponents[2],
                        currentTurnPlayerId?.toString(),
                        isVertical = true
                    )
                )
            }
        }
    }

    private fun createOpponentDisplay(
        player: PlayerDisplayInfo,
        currentTurnPlayerId: String?,
        isVertical: Boolean = false
    ): Pane {
        val isCurrentTurn = player.userId.toString() == currentTurnPlayerId

        val nameLabel = Label(player.username).apply {
            styleClass.add("player-name-label")
            if (isCurrentTurn) style = "-fx-font-weight: bold; -fx-text-fill: yellow;"
        }
        val cardCountLabel = Label("${player.cardCount} cards").apply {
            styleClass.add("player-card-count-label")
        }

        val textContainer = VBox(3.0, nameLabel, cardCountLabel).apply {
            alignment = Pos.CENTER
            padding = Insets(5.0)
        }

        val backCard = Card(id = "back", color = CardColor.WILD, type = CardType.BACK)
        val backImage = ResourceLoader.loadCardImage(backCard)

        val cardImages = (0 until minOf(player.cardCount, 5)).map {
            ImageView(backImage).apply {
                fitWidth = if (isVertical) SMALL_CARD_WIDTH else SMALL_CARD_WIDTH * 0.7
                fitHeight = if (isVertical) SMALL_CARD_HEIGHT else SMALL_CARD_HEIGHT * 0.7
            }
        }

        val stackPane = StackPane().apply {
            cardImages.forEachIndexed { index, imageView ->
                val offset = index * 5.0
                StackPane.setAlignment(imageView, Pos.TOP_LEFT)
                imageView.translateX = offset
                imageView.translateY = offset
                children.add(imageView)
            }

            if (player.cardCount > 5) {
                children.add(
                    Label("+${player.cardCount - 5}").apply {
                        font = Font(24.0)
                        style = "-fx-text-fill: white;"
                    }
                )
            }

            if (player.hasUno) {
                children.add(
                    Label("UNO!").apply {
                        font = Font("Arial", 30.0)
                        style = "-fx-text-fill: red; -fx-font-weight: bold; -fx-stroke: white; -fx-stroke-width: 1;"
                    }
                )
            }
        }

        val playerContainer = VBox(5.0).apply {
            alignment = Pos.CENTER
            children.addAll(textContainer, stackPane)

            if (isCurrentTurn) {
                border = Border(
                    BorderStroke(
                        Color.YELLOW,
                        BorderStrokeStyle.SOLID,
                        CornerRadii(5.0),
                        BorderWidths(3.0)
                    )
                )
            }
            padding = Insets(10.0)
        }

        return playerContainer
    }

    private fun handleDrawCard() {
        gameController.onDrawCardRequested()
    }

    private fun handleSayUno() {
        gameController.onSayUnoRequested()
    }

    private fun handleCardClick(cardIndex: Int, card: Card) {
        gameController.onCardSelected(cardIndex, card)
    }

    private fun renderPlayerHand(hand: List<Card>) {
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater { renderPlayerHand(hand) }
            return
        }

        println("[UI-Debug] Rendering hand. Cards count: ${hand.size}")

        playerHandBox.children.clear()

        if (hand.isEmpty()) {
            println("[UI-Debug] Warning: Hand is empty!")
            return
        }

        hand.forEachIndexed { index, card ->
            addCardToHandInternal(index, card)
        }
    }

    private fun addCardToHandInternal(index: Int, card: Card) {
        println(
            "[GameView] addCardToHandInternal(index=$index, cardId=${card.id}, type=${card.type}, color=${card.color}) (thread=${Thread.currentThread().name}, fx=${Platform.isFxApplicationThread()})"
        )

        val cardImage = ResourceLoader.loadCardImage(card)

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

    fun updateCurrentCard(card: Card?) {
        val displayCard = card ?: Card(id = "none", color = CardColor.WILD, type = CardType.BACK)
        centerCardDisplay.image = ResourceLoader.loadCardImage(displayCard)
        updateBackgroundFromCard(card)
    }

    private fun updateGameStatus(phase: GamePhase, isMyTurn: Boolean) {
        when (phase) {
            GamePhase.WAITING_TURN -> {
                drawButton.isDisable = !isMyTurn
                unoButton.isDisable = !isMyTurn
            }
            GamePhase.CHOOSING_COLOR -> {
                drawButton.isDisable = true
                unoButton.isDisable = true
            }
            GamePhase.DRAWING_CARD -> {
                drawButton.isDisable = true
                unoButton.isDisable = true
            }
            GamePhase.FINISHED -> {
                drawButton.isDisable = true
                unoButton.isDisable = true
            }
        }
    }

    private fun updateBackgroundFromCard(card: Card?) {
        val cardColor = card?.color ?: CardColor.WILD
        root.style = "-fx-background-color: derive(${ResourceLoader.toCssColor(cardColor)}, -60%); -fx-padding: 20;"
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
                        gameController.onColorSelected(color)
                        chooserStage.close()
                    }
                }
                children.add(btn)
            }
        }

        val layout =
            VBox(20.0, Label(message).apply { font = Font(Font.getDefault().name, 14.0) }, colorButtons).apply {
                alignment = Pos.CENTER
                padding = Insets(20.0)
            }

        chooserStage.scene = Scene(layout, 300.0, 150.0)
        chooserStage.showAndWait()
    }


    fun updateUI() {
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater { updateUI() }
            return
        }

        autoDetectMyId()

        val gameState = gameController.getCurrentGameState()
        val myHand = gameController.getMyHand()
        val myPlayerId = gameController.myPlayerId

        if (gameState == null || myPlayerId == null) {
            gameStatusLabel.text = "Loading game data... (ID: ${myPlayerId ?: "detecting..."})"
            return
        }

        updateGameStateUI(gameState, myPlayerId, myHand)
    }

    private fun updateGameStateUI(gameState: GameState, myPlayerId: Long, myHand: List<Card>) {
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater { updateGameStateUI(gameState, myPlayerId, myHand) }
            return
        }

        val isMyTurn = gameState.currentPlayerId == myPlayerId

        updateCurrentCard(gameState.currentCard)
        updateGameStatus(gameState.gamePhase, isMyTurn)

        val turnStatus = if (isMyTurn) " ★ YOUR TURN ★" else ""
        gameStatusLabel.text = "Current Player ID: ${gameState.currentPlayerId}$turnStatus"
        if (isMyTurn) gameStatusLabel.style = "-fx-text-fill: #FFD700; -fx-font-weight: bold;"
        else gameStatusLabel.style = "-fx-text-fill: white;"

        val shouldShowChooser = gameController.shouldShowColorChooser()
        if (!shouldShowChooser) {
            isColorChooserShowing = false
        } else if (!isColorChooserShowing) {
            isColorChooserShowing = true
            try {
                val isWildCard = gameState.currentCard?.type == CardType.WILD
                showColorChooser(isWildCard)
            } finally {
                isColorChooserShowing = false
            }
        }

        renderOpponents(gameController.getOpponentsInOrder())
        renderPlayerHand(myHand)
    }

    companion object {
        private const val CARD_WIDTH = 90.0
        private const val CARD_HEIGHT = 135.0
        private const val SMALL_CARD_WIDTH = 70.0
        private const val SMALL_CARD_HEIGHT = 105.0
    }
}