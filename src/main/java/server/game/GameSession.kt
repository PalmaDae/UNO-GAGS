package server.game

import proto.dto.*
import server.game.DeckBuilder.DeckPiles

class GameSession(
    val roomId: Long,
    initialPlayers: MutableList<PlayerState>
) {
    internal val players = HashMap<Long?, PlayerState>()
    private val deckPiles: DeckPiles = DeckBuilder.createDeckPiles()

    var direction: GameDirection = GameDirection.CLOCKWISE
        private set
    private var currentPlayerIndex: Int = 0
    private val playerOrder: MutableList<Long?>
    var gamePhase: GamePhase = GamePhase.WAITING_TURN
        private set
    private var chosenColor: CardColor? = null

    val currentPlayerId: Long
        get() = playerOrder[currentPlayerIndex]!!

    val currentCard: Card
        get() = deckPiles.topCard

    val gameState: GameState
        get() {
            val playerInfos: MutableMap<Long, PlayerGameInfo> = HashMap()

            for (player in players.values) {
                val info = PlayerGameInfo(
                    username = player.username!!,
                    cardCount = player.cardCount,
                    hasUno = player.hasDeclaredUno,
                    avatar = player.avatar // Мы добавили это ранее
                )
                playerInfos.put(player.playerId, info)
            }

            return GameState(
                roomId = roomId,
                players = playerInfos,
                currentCard = this.currentCard,
                currentPlayerId = this.currentPlayerId,
                direction = direction,
                gamePhase = gamePhase,
                chosenColor = this.chosenColor
            )
        }

    init {
        for (player in initialPlayers) {
            players.put(player.playerId, player)
        }

        this.playerOrder = ArrayList<Long?>(players.keys)

        dealInitialCards()
    }

    private fun dealInitialCards() {
        for (player in players.values) {
            repeat(6) {
                player.addCard(deckPiles.drawCard())
            }
        }
    }

    fun canPlayCard(card: Card): Boolean {
        if (card.type == CardType.WILD || card.type == CardType.WILD_DRAW_FOUR) {
            return true
        }

        if (chosenColor != null) {
            return card.color == chosenColor
        }

        if (card.color == currentCard.color) {
            return true
        }

        if (card.type == currentCard.type && card.type != CardType.NUMBER) {
            return true
        }

        return card.type == CardType.NUMBER && currentCard.type == CardType.NUMBER &&
                card.number == currentCard.number
    }

    fun playCard(playerId: Long, cardIndex: Int, chosenColor: CardColor?) {
        check(playerId == this.currentPlayerId) { "Not your turn" }

        val player: PlayerState = players[playerId] ?: throw IllegalArgumentException("Player not found")
        val hand = player.hand
        require(cardIndex in hand.indices) { "Invalid card index: $cardIndex" }

        val card = hand[cardIndex]
        check(canPlayCard(card)) { "Cannot play this card" }

        if ((card.type == CardType.WILD || card.type == CardType.WILD_DRAW_FOUR) && chosenColor == null) {
            val wildCard = player.removeCard(cardIndex)
            deckPiles.playCard(wildCard!!)
            this.gamePhase = GamePhase.CHOOSING_COLOR
            return
        }

        this.chosenColor = if (card.type == CardType.WILD || card.type == CardType.WILD_DRAW_FOUR) {
            chosenColor
        } else {
            null
        }

        if (hand.size == 2 && !player.hasDeclaredUno) {
            player.addCard(deckPiles.drawCard())
            player.addCard(deckPiles.drawCard())
        }

        val playedCard = player.removeCard(cardIndex)
        deckPiles.playCard(playedCard!!)

        applyCardEffect(playedCard)

        if (player.cardCount == 0) {
            gamePhase = GamePhase.FINISHED
            return
        }

        moveToNextPlayer()
        gamePhase = GamePhase.WAITING_TURN
    }

    fun setChosenColor(color: CardColor?) {
        check(gamePhase == GamePhase.CHOOSING_COLOR) { "Not waiting for color choice" }

        this.chosenColor = color

        val updatedCard = this.currentCard.copy(color = color ?: CardColor.WILD)
        deckPiles.setTopCard(updatedCard)

        applyCardEffect(updatedCard)

        gamePhase = GamePhase.DRAWING_CARD
    }

    private fun applyCardEffect(card: Card) {
        when (card.type) {
            CardType.SKIP ->
                moveToNextPlayer()

            CardType.REVERSE -> {
                direction = if (direction == GameDirection.CLOCKWISE)
                    GameDirection.COUNTER_CLOCKWISE
                else
                    GameDirection.CLOCKWISE
                if (players.size == 2) {
                    moveToNextPlayer()
                }
            }

            CardType.DRAW_TWO -> {
                moveToNextPlayer()
                val drawTwoTarget: PlayerState = players[this.currentPlayerId]!!
                drawTwoTarget.addCard(deckPiles.drawCard())
                drawTwoTarget.addCard(deckPiles.drawCard())
                moveToNextPlayer()
            }

            CardType.WILD_DRAW_FOUR -> {
                moveToNextPlayer()
                val drawFourTarget: PlayerState = players.get(this.currentPlayerId)!!
                repeat(4) {
                    drawFourTarget.addCard(deckPiles.drawCard())
                }
                moveToNextPlayer()
            }

            CardType.WILD, CardType.NUMBER, CardType.BACK -> {}
        }
    }

    private fun moveToNextPlayer() {
        currentPlayerIndex = if (direction == GameDirection.CLOCKWISE)
            (currentPlayerIndex + 1) % playerOrder.size
        else
            (currentPlayerIndex - 1 + playerOrder.size) % playerOrder.size
    }

    fun drawCard(playerId: Long) {
        check(playerId == this.currentPlayerId) { "Not your turn" }

        val player: PlayerState = players.get(playerId)!!
        player.addCard(deckPiles.drawCard())

        // Set DRAWING_CARD phase for client synchronization
        gamePhase = GamePhase.DRAWING_CARD
    }

    fun finishDrawing(playerId: Long) {
        check(playerId == this.currentPlayerId) { "Not your turn" }
        check(gamePhase == GamePhase.DRAWING_CARD) { "Not in DRAWING_CARD phase" }

        moveToNextPlayer()
        gamePhase = GamePhase.WAITING_TURN
    }

    fun finishColorSelection() {
        check(gamePhase == GamePhase.DRAWING_CARD) { "Not in DRAWING_CARD phase" }

        if (currentCard.type == CardType.WILD) {
            moveToNextPlayer()
        }

        gamePhase = GamePhase.WAITING_TURN
    }

    fun sayUno(playerId: Long) {
        val player = players[playerId]!!
        requireNotNull(player) { "Player not found: $playerId" }

        player.declareUno()
    }

}