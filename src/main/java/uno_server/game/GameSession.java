package uno_server.game;

import uno_proto.dto.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Represents a complete UNO game session.
 * Manages game state, player turns, card play validation, and scoring.
 */
public class GameSession {
    private final long roomId;
    private final Map<Long, PlayerState> players;
    private final DeckBuilder.DeckPiles deckPiles;
    
    private GameDirection direction;
    private int currentPlayerIndex;
    private List<Long> playerOrder;
    private GamePhase gamePhase;
    private CardColor chosenColor; // For when a wild card is played
    
    public GameSession(long roomId, List<PlayerState> initialPlayers) {
        this.roomId = roomId;
        this.players = new HashMap<>();
        this.deckPiles = DeckBuilder.createDeckPiles();
        this.direction = GameDirection.CLOCKWISE;
        this.currentPlayerIndex = 0;
        this.gamePhase = GamePhase.WAITING_TURN;
        this.chosenColor = null;
        
        // Initialize players
        for (PlayerState player : initialPlayers) {
            players.put(player.getPlayerId(), player);
        }
        
        // Set player order
        this.playerOrder = new ArrayList<>(players.keySet());
        
        // Deal initial cards (7 per player)
        dealInitialCards();
    }
    
    /**
     * Deals 7 cards to each player from the draw pile.
     */
    private void dealInitialCards() {
        for (PlayerState player : players.values()) {
            for (int i = 0; i < 7; i++) {
                player.addCard(deckPiles.drawCard());
            }
        }
    }
    
    /**
     * Gets the current player's ID.
     */
    public long getCurrentPlayerId() {
        return playerOrder.get(currentPlayerIndex);
    }
    
    /**
     * Gets the current top card of the discard pile.
     */
    public Card getCurrentCard() {
        return deckPiles.getTopCard();
    }
    
    /**
     * Validates if a card can be played on the current card.
     * A card can be played if:
     * - It matches the color of the current card
     * - It matches the number of the current card
     * - It matches the type of the current card
     * - It's a wild card
     * - A color has been chosen and the card matches that color
     */
    public boolean canPlayCard(Card card) {
        Card currentCard = getCurrentCard();
        
        // Wild cards can always be played
        if (card.getType() == CardType.WILD || card.getType() == CardType.WILD_DRAW_FOUR) {
            return true;
        }
        
        // If a color was chosen from a wild card, match against that color
        if (chosenColor != null && card.getColor() == chosenColor) {
            return true;
        }
        
        // If current card is wild and no color chosen, any card can be played
        if ((currentCard.getType() == CardType.WILD || currentCard.getType() == CardType.WILD_DRAW_FOUR) 
            && chosenColor == null) {
            return true;
        }
        
        // Match against the actual current card's color
        if (card.getColor() == currentCard.getColor()) {
            return true;
        }
        
        // Match against the current card's type (for action cards)
        if (card.getType() == currentCard.getType()) {
            return true;
        }
        
        // Match against the current card's number (for number cards)
        if (card.getType() == CardType.NUMBER && currentCard.getType() == CardType.NUMBER &&
            Objects.equals(card.getNumber(), currentCard.getNumber())) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Plays a card from the current player's hand.
     * Applies card effects and updates game state.
     */
    public void playCard(long playerId, int cardIndex, CardColor chosenColor) {
        // Validate it's the player's turn
        if (playerId != getCurrentPlayerId()) {
            throw new IllegalStateException("Not your turn");
        }
        
        PlayerState player = players.get(playerId);
        if (player == null) {
            throw new IllegalArgumentException("Player not found: " + playerId);
        }
        
        // Get the card to play
        List<Card> hand = player.getHand();
        if (cardIndex < 0 || cardIndex >= hand.size()) {
            throw new IllegalArgumentException("Invalid card index: " + cardIndex);
        }
        
        Card card = hand.get(cardIndex);
        
        // Validate the card can be played
        if (!canPlayCard(card)) {
            throw new IllegalStateException("Cannot play this card");
        }
        
        // Check UNO rules if player has 2 cards after playing
        boolean willHaveOneCardLeft = hand.size() == 2;
        if (willHaveOneCardLeft && !player.hasDeclaredUno()) {
            // Player forgot to say UNO - penalty of 2 cards
            player.addCard(deckPiles.drawCard());
            player.addCard(deckPiles.drawCard());
        }
        
        // Remove and play the card
        Card playedCard = player.removeCard(cardIndex);
        deckPiles.playCard(playedCard);
        
        // Handle wild card color choice
        if (card.getType() == CardType.WILD || card.getType() == CardType.WILD_DRAW_FOUR) {
            if (chosenColor == null) {
                this.gamePhase = GamePhase.CHOOSING_COLOR;
                return; // Wait for color choice
            }
            this.chosenColor = chosenColor;
        } else {
            this.chosenColor = null; // Reset color choice for non-wild cards
        }
        
        // Apply card effects
        applyCardEffect(playedCard);
        
        // Check for win condition
        if (player.getCardCount() == 0) {
            gamePhase = GamePhase.FINISHED;
            return;
        }
        
        // Move to next player
        moveToNextPlayer();
        gamePhase = GamePhase.WAITING_TURN;
    }
    
    /**
     * Sets the chosen color after playing a wild card.
     */
    public void setChosenColor(CardColor color) {
        if (gamePhase != GamePhase.CHOOSING_COLOR) {
            throw new IllegalStateException("Not waiting for color choice");
        }
        
        this.chosenColor = color;
        
        // Apply the wild card effect and move to next player
        Card currentCard = getCurrentCard();
        applyCardEffect(currentCard);
        moveToNextPlayer();
        gamePhase = GamePhase.WAITING_TURN;
    }
    
    /**
     * Applies the effect of the played card.
     */
    private void applyCardEffect(Card card) {
        switch (card.getType()) {
            case SKIP:
                // Skip the next player
                moveToNextPlayer();
                break;
                
            case REVERSE:
                // Reverse direction
                direction = (direction == GameDirection.CLOCKWISE) ? 
                    GameDirection.COUNTER_CLOCKWISE : GameDirection.CLOCKWISE;
                // In 2-player game, reverse acts like skip
                if (players.size() == 2) {
                    moveToNextPlayer();
                }
                break;
                
            case DRAW_TWO:
                // Next player draws 2 cards and loses their turn
                moveToNextPlayer();
                PlayerState drawTwoTarget = players.get(getCurrentPlayerId());
                drawTwoTarget.addCard(deckPiles.drawCard());
                drawTwoTarget.addCard(deckPiles.drawCard());
                break;
                
            case WILD_DRAW_FOUR:
                // Next player draws 4 cards and loses their turn
                moveToNextPlayer();
                PlayerState drawFourTarget = players.get(getCurrentPlayerId());
                for (int i = 0; i < 4; i++) {
                    drawFourTarget.addCard(deckPiles.drawCard());
                }
                break;
                
            case WILD:
            case NUMBER:
                // No additional effects
                break;
        }
    }
    
    /**
     * Moves to the next player based on current direction.
     */
    private void moveToNextPlayer() {
        if (direction == GameDirection.CLOCKWISE) {
            currentPlayerIndex = (currentPlayerIndex + 1) % playerOrder.size();
        } else {
            currentPlayerIndex = (currentPlayerIndex - 1 + playerOrder.size()) % playerOrder.size();
        }
    }
    
    /**
     * Current player draws a card and ends their turn.
     */
    public void drawCard(long playerId) {
        if (playerId != getCurrentPlayerId()) {
            throw new IllegalStateException("Not your turn");
        }
        
        PlayerState player = players.get(playerId);
        player.addCard(deckPiles.drawCard());
        
        moveToNextPlayer();
        gamePhase = GamePhase.WAITING_TURN;
    }
    
    /**
     * Player declares UNO (must have exactly 2 cards).
     */
    public void sayUno(long playerId) {
        PlayerState player = players.get(playerId);
        if (player == null) {
            throw new IllegalArgumentException("Player not found: " + playerId);
        }
        
        player.declareUno();
    }
    
    /**
     * Calculates scores for all players when the game ends.
     * The winner gets points equal to the sum of all cards in other players' hands.
     */
    public Map<Long, Integer> calculateScores() {
        Map<Long, Integer> scores = new HashMap<>();
        
        if (gamePhase != GamePhase.FINISHED) {
            return scores; // Game not finished yet
        }
        
        // Find the winner (player with 0 cards)
        PlayerState winner = null;
        for (PlayerState player : players.values()) {
            if (player.getCardCount() == 0) {
                winner = player;
                break;
            }
        }
        
        if (winner == null) {
            return scores; // No winner found
        }
        
        // Calculate total points from all other players
        int totalPoints = 0;
        for (PlayerState player : players.values()) {
            if (player.getPlayerId() != winner.getPlayerId()) {
                totalPoints += player.calculateHandScore();
            }
        }
        
        // Winner gets all the points
        scores.put(winner.getPlayerId(), totalPoints);
        
        return scores;
    }
    
    /**
     * Gets the current game state as a DTO for transmission to clients.
     */
    public GameState getGameState() {
        Map<Long, PlayerGameInfo> playerInfos = new HashMap<>();
        
        for (PlayerState player : players.values()) {
            PlayerGameInfo info = new PlayerGameInfo(
                player.getUsername(),
                player.getCardCount(),
                player.hasDeclaredUno()
            );
            playerInfos.put(player.getPlayerId(), info);
        }
        
        return new GameState(
            roomId,
            playerInfos,
            getCurrentCard(),
            getCurrentPlayerId(),
            direction,
            gamePhase
        );
    }
    
    // Getters for game properties
    public long getRoomId() { return roomId; }
    public GameDirection getDirection() { return direction; }
    public GamePhase getGamePhase() { return gamePhase; }
    public CardColor getChosenColor() { return chosenColor; }
    public Map<Long, PlayerState> getPlayers() { return new HashMap<>(players); }
}