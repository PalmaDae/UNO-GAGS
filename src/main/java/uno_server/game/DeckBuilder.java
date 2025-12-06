package uno_server.game;

import uno_proto.dto.*;

import java.util.*;

/**
 * Utility class for building a standard UNO deck.
 * Creates all 108 cards in a complete UNO deck using the DTO enums.
 */
public class DeckBuilder {
    
    /**
     * Creates a complete standard UNO deck with 108 cards.
     * 
     * Deck composition:
     * - Number cards: 0 (1 per color), 1-9 (2 per color) = 76 cards
     * - Action cards: Skip, Reverse, Draw Two (2 per color) = 24 cards  
     * - Wild cards: Wild (4), Wild Draw Four (4) = 8 cards
     * 
     * @return A shuffled list of all UNO cards
     */
    public static List<Card> createStandardDeck() {
        List<Card> deck = new ArrayList<>();
        CardColor[] colors = {CardColor.RED, CardColor.BLUE, CardColor.GREEN, CardColor.YELLOW};
        
        // Add number cards (0-9 for each color)
        for (CardColor color : colors) {
            // One 0 card per color
            deck.add(new Card(generateCardId(color, CardType.NUMBER, 0), color, CardType.NUMBER, 0));
            
            // Two of each number 1-9 per color
            for (int number = 1; number <= 9; number++) {
                deck.add(new Card(generateCardId(color, CardType.NUMBER, number), color, CardType.NUMBER, number));
                deck.add(new Card(generateCardId(color, CardType.NUMBER, number) + "_2", color, CardType.NUMBER, number));
            }
            
            // Two of each action card per color
            // Skip cards
            deck.add(new Card(generateCardId(color, CardType.SKIP, null), color, CardType.SKIP, null));
            deck.add(new Card(generateCardId(color, CardType.SKIP, null) + "_2", color, CardType.SKIP, null));
            
            // Reverse cards
            deck.add(new Card(generateCardId(color, CardType.REVERSE, null), color, CardType.REVERSE, null));
            deck.add(new Card(generateCardId(color, CardType.REVERSE, null) + "_2", color, CardType.REVERSE, null));
            
            // Draw Two cards
            deck.add(new Card(generateCardId(color, CardType.DRAW_TWO, null), color, CardType.DRAW_TWO, null));
            deck.add(new Card(generateCardId(color, CardType.DRAW_TWO, null) + "_2", color, CardType.DRAW_TWO, null));
        }
        
        // Add Wild cards (4 of each type)
        for (int i = 1; i <= 4; i++) {
            deck.add(new Card("WILD_" + i, CardColor.WILD, CardType.WILD, null));
            deck.add(new Card("WILD_DRAW_FOUR_" + i, CardColor.WILD, CardType.WILD_DRAW_FOUR, null));
        }
        
        // Shuffle the deck
        Collections.shuffle(deck);
        
        return deck;
    }
    
    /**
     * Generates a unique card ID based on color, type, and number.
     * Used for consistent card identification.
     */
    private static String generateCardId(CardColor color, CardType type, Integer number) {
        StringBuilder id = new StringBuilder();
        id.append(color.name()).append("_");
        id.append(type.name());
        if (number != null) {
            id.append("_").append(number);
        }
        return id.toString();
    }
    
    /**
     * Creates a draw pile from the deck and returns both the draw pile and discard pile.
     * The top card of the deck is moved to the discard pile to start the game.
     */
    public static DeckPiles createDeckPiles() {
        List<Card> fullDeck = createStandardDeck();
        
        // Draw pile starts with all cards except the top one
        List<Card> drawPile = new ArrayList<>(fullDeck.subList(1, fullDeck.size()));
        
        // Discard pile starts with the top card
        List<Card> discardPile = new ArrayList<>();
        discardPile.add(fullDeck.get(0));
        
        return new DeckPiles(drawPile, discardPile);
    }
    
    /**
     * Container class for managing draw and discard piles.
     */
    public static class DeckPiles {
        private final List<Card> drawPile;
        private final List<Card> discardPile;
        
        public DeckPiles(List<Card> drawPile, List<Card> discardPile) {
            this.drawPile = new ArrayList<>(drawPile);
            this.discardPile = new ArrayList<>(discardPile);
        }
        
        public List<Card> getDrawPile() { return new ArrayList<>(drawPile); }
        public List<Card> getDiscardPile() { return new ArrayList<>(discardPile); }
        
        /**
         * Draw a card from the draw pile.
         * If draw pile is empty, reshuffle discard pile (except top card) into draw pile.
         */
        public Card drawCard() {
            if (drawPile.isEmpty()) {
                reshuffleDiscardIntoDraw();
            }
            
            if (drawPile.isEmpty()) {
                throw new IllegalStateException("No cards available to draw");
            }
            
            return drawPile.remove(drawPile.size() - 1);
        }
        
        /**
         * Play a card to the discard pile.
         */
        public void playCard(Card card) {
            discardPile.add(card);
        }
        
        /**
         * Get the current top card of the discard pile.
         */
        public Card getTopCard() {
            if (discardPile.isEmpty()) {
                throw new IllegalStateException("Discard pile is empty");
            }
            return discardPile.get(discardPile.size() - 1);
        }
        
        /**
         * Reshuffle all cards from discard pile except the top card into the draw pile.
         */
        private void reshuffleDiscardIntoDraw() {
            if (discardPile.size() <= 1) {
                return; // Nothing to reshuffle
            }
            
            // Keep the top card in discard pile
            Card topCard = discardPile.remove(discardPile.size() - 1);
            
            // Move all other cards to draw pile and shuffle
            drawPile.addAll(discardPile);
            discardPile.clear();
            Collections.shuffle(drawPile);
            
            // Put the top card back
            discardPile.add(topCard);
        }
    }
}