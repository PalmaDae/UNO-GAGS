package server.game

import proto.dto.*
import server.game.DeckBuilder.DeckPiles

class GameSession(
    val roomId: Long, initialPlayers: MutableList<PlayerState>
) {
    private val players = HashMap<Long?, PlayerState>()
    private val deckPiles: DeckPiles = DeckBuilder.createDeckPiles()

    var direction: GameDirection = GameDirection.CLOCKWISE
        private set
    private var currentPlayerIndex: Int = 0
    private val playerOrder: MutableList<Long?>
    var gamePhase: GamePhase = GamePhase.WAITING_TURN
        private set
    private var chosenColor: CardColor? = null

    init {
        for (player in initialPlayers) {
            players.put(player.playerId, player)
        }

        this.playerOrder = ArrayList<Long?>(players.keys)

        dealInitialCards()
    }


    private fun dealInitialCards() {
        for (player in players.values) {
            for (i in 0..6) {
                player.addCard(deckPiles.drawCard())
            }
        }
    }

    val currentPlayerId: Long
        get() = playerOrder[currentPlayerIndex]!!

    val currentCard: Card
        get() = deckPiles.topCard

    fun canPlayCard(card: Card): Boolean {
        val currentCard = this.currentCard

        // Wild cards can always be played
        if (card.type == CardType.WILD || card.type == CardType.WILD_DRAW_FOUR) {
            return true
        }

        // If a color was chosen from a wild card, match against that color
        if (chosenColor != null && card.color == chosenColor) {
            return true
        }

        // If current card is wild and no color chosen, any card can be played
        if ((currentCard.type == CardType.WILD || currentCard.type == CardType.WILD_DRAW_FOUR)
            && chosenColor == null
        ) {
            return true
        }

        // Match against the actual current card's color
        if (card.color == currentCard.color) {
            return true
        }

        // Match against the current card's type (for action cards)
        if (card.type == currentCard.type) {
            return true
        }

        // Match against the current card's number (for number cards)
        if (card.type == CardType.NUMBER && currentCard.type == CardType.NUMBER &&
            card.number == currentCard.number
        ) return true else return false
    }

    fun playCard(playerId: Long, cardIndex: Int, chosenColor: CardColor?) {
        // Validate it's the player's turn
        check(playerId == this.currentPlayerId) { "Not your turn" }

        val player: PlayerState = players.get(playerId)!!
        requireNotNull(player) { "Player not found: $playerId" }


        // Get the card to play
        val hand = player.hand
        require(!(cardIndex < 0 || cardIndex >= hand.size)) { "Invalid card index: $cardIndex" }

        val card = hand[cardIndex]


        // Validate the card can be played
        check(canPlayCard(card)) { "Cannot play this card" }


        // Check UNO rules if player has 2 cards after playing
        val willHaveOneCardLeft = hand.size == 2
        if (willHaveOneCardLeft && !player.hasDeclaredUno()) {
            // Player forgot to say UNO - penalty of 2 cards
            player.addCard(deckPiles.drawCard())
            player.addCard(deckPiles.drawCard())
        }


        // Remove and play the card
        val playedCard = player.removeCard(cardIndex)
        deckPiles.playCard(playedCard)


        // Handle wild card color choice
        if (card.type == CardType.WILD || card.type == CardType.WILD_DRAW_FOUR) {
            if (chosenColor == null) {
                this.gamePhase = GamePhase.CHOOSING_COLOR
                return  // Wait for color choice
            }
            this.chosenColor = chosenColor
        } else {
            this.chosenColor = null // Reset color choice for non-wild cards
        }


        // Apply card effects
        applyCardEffect(playedCard!!)


        // Check for win condition
        if (player.cardCount == 0) {
            gamePhase = GamePhase.FINISHED
            return
        }


        // Move to next player
        moveToNextPlayer()
        gamePhase = GamePhase.WAITING_TURN
    }


    fun setChosenColor(color: CardColor?) {
        check(gamePhase == GamePhase.CHOOSING_COLOR) { "Not waiting for color choice" }

        this.chosenColor = color


        // Apply the wild card effect and move to next player
        val currentCard = this.currentCard
        applyCardEffect(currentCard)
        moveToNextPlayer()
        gamePhase = GamePhase.WAITING_TURN
    }


    private fun applyCardEffect(card: Card) {
        when (card.type) {
            CardType.SKIP ->                 // Skip the next player
                moveToNextPlayer()

            CardType.REVERSE -> {
                // Reverse direction
                direction =
                    if (direction == GameDirection.CLOCKWISE) GameDirection.COUNTER_CLOCKWISE else GameDirection.CLOCKWISE
                // In 2-player game, reverse acts like skip
                if (players.size == 2) {
                    moveToNextPlayer()
                }
            }

            CardType.DRAW_TWO -> {
                // Next player draws 2 cards and loses their turn
                moveToNextPlayer()
                val drawTwoTarget: PlayerState = players.get(this.currentPlayerId)!!
                drawTwoTarget.addCard(deckPiles.drawCard())
                drawTwoTarget.addCard(deckPiles.drawCard())
            }

            CardType.WILD_DRAW_FOUR -> {
                // Next player draws 4 cards and loses their turn
                moveToNextPlayer()
                val drawFourTarget: PlayerState = players.get(this.currentPlayerId)!!
                var i = 0
                while (i < 4) {
                    drawFourTarget.addCard(deckPiles.drawCard())
                    i++
                }
            }

            CardType.WILD, CardType.NUMBER -> {}
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

        moveToNextPlayer()
        gamePhase = GamePhase.WAITING_TURN
    }


    fun sayUno(playerId: Long) {
        val player: PlayerState = players.get(playerId)!!
        requireNotNull(player) { "Player not found: $playerId" }

        player.declareUno()
    }

    val gameState: GameState
        get() {
            val playerInfos: MutableMap<Long, PlayerGameInfo> = HashMap()

            for (player in players.values) {
                val info = PlayerGameInfo(
                    player.username!!,
                    player.cardCount,
                    player.hasDeclaredUno()
                )
                playerInfos.put(player.playerId, info)
            }

            return GameState(
                roomId,
                playerInfos,
                this.currentCard,
                this.currentPlayerId,
                direction,
                gamePhase
            )
        }
}