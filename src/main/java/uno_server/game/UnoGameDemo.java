package uno_server.game;

import uno_proto.dto.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Simple test and demonstration of the UNO game engine.
 * Simulates a complete round to validate that all game mechanics work correctly.
 */
public class UnoGameDemo {
    
    public static void main(String[] args) {
        System.out.println("=== UNO Game Engine Demo ===\n");
        
        // Create a game session manager
        GameSessionManager manager = new GameSessionManager();
        
        // Create players for a 2-player game
        List<PlayerState> players = Arrays.asList(
            new PlayerState(1L, "Alice"),
            new PlayerState(2L, "Bob")
        );
        
        // Create a new game session
        long roomId = 1001L;
        GameSession session = manager.createSession(roomId, players);
        
        System.out.println("Game started! Room ID: " + roomId);
        System.out.println("Initial game state:");
        printGameState(manager.getGameState(roomId));
        
        // If the game starts with a wild card, choose a color
        if (session.getCurrentCard().getType() == CardType.WILD || 
            session.getCurrentCard().getType() == CardType.WILD_DRAW_FOUR) {
            System.out.println("\nGame started with a wild card! Choosing RED as the starting color...");
            session.setChosenColor(CardColor.RED);
            System.out.println("Updated game state:");
            printGameState(manager.getGameState(roomId));
        }
        
        // Simulate a few turns to demonstrate the mechanics
        simulateGameplay(manager, roomId);
        
        System.out.println("\n=== Demo Complete ===");
    }
    
    /**
     * Simulates a sequence of gameplay actions to demonstrate the UNO rules engine.
     */
    private static void simulateGameplay(GameSessionManager manager, long roomId) {
        GameSession session = manager.getSession(roomId);
        
        System.out.println("\n--- Starting Gameplay Simulation ---");
        
        // Turn 1: Alice's turn
        long aliceId = 1L;
        long bobId = 2L;
        
        System.out.println("\nTurn 1: Alice's turn");
        System.out.println("Alice's hand: " + formatHand(session.getPlayers().get(aliceId).getHand()));
        System.out.println("Current card: " + formatCard(session.getCurrentCard()));
        
        // Find a playable card for Alice
        PlayerState alice = session.getPlayers().get(aliceId);
        int playableCardIndex = findPlayableCard(alice, session.getCurrentCard());
        
        if (playableCardIndex >= 0) {
            Card cardToPlay = alice.getHand().get(playableCardIndex);
            System.out.println("Alice plays: " + formatCard(cardToPlay));
            
            try {
                manager.playCard(roomId, aliceId, playableCardIndex, null);
                System.out.println("Play successful!");
                printGameState(manager.getGameState(roomId));
            } catch (Exception e) {
                System.out.println("Play failed: " + e.getMessage());
            }
        } else {
            System.out.println("Alice has no playable cards, drawing...");
            manager.drawCard(roomId, aliceId);
            System.out.println("Alice drew a card");
        }
        
        // Turn 2: Next player's turn (check if it's Bob or still Alice due to reverse)
        System.out.println("\nTurn 2: " + (session.getCurrentPlayerId() == bobId ? "Bob's" : "Alice's") + " turn");
        long currentTurnPlayerId = session.getCurrentPlayerId();
        PlayerState currentPlayer = session.getPlayers().get(currentTurnPlayerId);
        
        System.out.println(currentPlayer.getUsername() + "'s hand: " + formatHand(currentPlayer.getHand()));
        System.out.println("Current card: " + formatCard(session.getCurrentCard()));
        
        playableCardIndex = findPlayableCard(currentPlayer, session.getCurrentCard());
        
        if (playableCardIndex >= 0) {
            Card cardToPlay = currentPlayer.getHand().get(playableCardIndex);
            System.out.println(currentPlayer.getUsername() + " plays: " + formatCard(cardToPlay));
            
            try {
                // Try to play a wild card if available
                if (cardToPlay.getType() == CardType.WILD || cardToPlay.getType() == CardType.WILD_DRAW_FOUR) {
                    System.out.println(currentPlayer.getUsername() + " chooses BLUE color");
                    manager.playCard(roomId, currentTurnPlayerId, playableCardIndex, CardColor.BLUE);
                } else {
                    manager.playCard(roomId, currentTurnPlayerId, playableCardIndex, null);
                }
                System.out.println("Play successful!");
                printGameState(manager.getGameState(roomId));
            } catch (Exception e) {
                System.out.println("Play failed: " + e.getMessage());
            }
        } else {
            System.out.println(currentPlayer.getUsername() + " has no playable cards, drawing...");
            manager.drawCard(roomId, currentTurnPlayerId);
            System.out.println(currentPlayer.getUsername() + " drew a card");
            printGameState(manager.getGameState(roomId));
        }
        
        // Demonstrate action card effects
        System.out.println("\n--- Action Card Effects Demo ---");
        demonstrateActionCards(manager, roomId);
        
        // Demonstrate UNO declaration
        System.out.println("\n--- UNO Declaration Demo ---");
        demonstrateUnoDeclaration(manager, roomId);
        
        // Test invalid move
        System.out.println("\n--- Invalid Move Test ---");
        System.out.println("Attempting to play out of turn...");
        try {
            long wrongPlayerId = (session.getCurrentPlayerId() == aliceId) ? bobId : aliceId;
            manager.playCard(roomId, wrongPlayerId, 0, null);
            System.out.println("ERROR: Invalid play was allowed!");
        } catch (Exception e) {
            System.out.println("Good! Invalid play was rejected: " + e.getMessage());
        }
        
        // Test playing an invalid card
        System.out.println("\nTesting invalid card play...");
        try {
            PlayerState currentTurnPlayer = session.getPlayers().get(session.getCurrentPlayerId());
            if (!currentTurnPlayer.getHand().isEmpty()) {
                // Try to play a card that likely doesn't match
                manager.playCard(roomId, session.getCurrentPlayerId(), 0, null);
                System.out.println("Card played (may be valid or invalid)");
            }
        } catch (Exception e) {
            System.out.println("Good! Invalid card was rejected: " + e.getMessage());
        }
        
        // Show final scores (game likely not finished, but demonstrate the method)
        System.out.println("\n--- Score Calculation Demo ---");
        Map<Long, Integer> scores = manager.calculateScores(roomId);
        if (scores.isEmpty()) {
            System.out.println("Game not finished yet, no scores to calculate");
        } else {
            System.out.println("Final scores:");
            for (Map.Entry<Long, Integer> entry : scores.entrySet()) {
                PlayerState player = session.getPlayers().get(entry.getKey());
                System.out.println("  " + player.getUsername() + ": " + entry.getValue() + " points");
            }
        }
    }
    
    /**
     * Demonstrates action card effects like Draw Two, Skip, etc.
     */
    private static void demonstrateActionCards(GameSessionManager manager, long roomId) {
        GameSession session = manager.getSession(roomId);
        
        // Force a Draw Two scenario by manually setting up the state
        System.out.println("Demonstrating Draw Two effect...");
        
        // For demo purposes, let's simulate playing a Draw Two card
        PlayerState currentPlayer = session.getPlayers().get(session.getCurrentPlayerId());
        if (currentPlayer.getCardCount() > 1) {
            // Find a Draw Two card if available
            for (int i = 0; i < currentPlayer.getHand().size(); i++) {
                Card card = currentPlayer.getHand().get(i);
                if (card.getType() == CardType.DRAW_TWO) {
                    System.out.println(currentPlayer.getUsername() + " plays: " + formatCard(card));
                    try {
                        manager.playCard(roomId, session.getCurrentPlayerId(), i, null);
                        System.out.println("Draw Two played! Next player must draw 2 cards.");
                        printGameState(manager.getGameState(roomId));
                        return;
                    } catch (Exception e) {
                        System.out.println("Couldn't play Draw Two: " + e.getMessage());
                    }
                }
            }
        }
        
        System.out.println("No Draw Two cards available for demonstration");
    }
    
    /**
     * Demonstrates UNO declaration mechanics.
     */
    private static void demonstrateUnoDeclaration(GameSessionManager manager, long roomId) {
        GameSession session = manager.getSession(roomId);
        PlayerState currentPlayer = session.getPlayers().get(session.getCurrentPlayerId());
        
        if (currentPlayer.getCardCount() == 2) {
            System.out.println(currentPlayer.getUsername() + " has 2 cards and declares UNO!");
            manager.sayUno(roomId, currentPlayer.getPlayerId());
            printGameState(manager.getGameState(roomId));
        } else if (currentPlayer.getCardCount() > 2) {
            System.out.println(currentPlayer.getUsername() + " has " + currentPlayer.getCardCount() + 
                             " cards, cannot declare UNO yet");
            
            // Show what happens if someone forgets to say UNO
            System.out.println("Note: If a player with 2 cards plays a card without declaring UNO, " +
                             "they receive a 2-card penalty!");
        } else {
            System.out.println(currentPlayer.getUsername() + " has " + currentPlayer.getCardCount() + 
                             " cards, UNO declaration not applicable");
        }
    }
    
    /**
     * Finds the first playable card in a player's hand.
     * Returns -1 if no playable cards found.
     */
    private static int findPlayableCard(PlayerState player, Card currentCard) {
        List<Card> hand = player.getHand();
        for (int i = 0; i < hand.size(); i++) {
            // Simple validation: if it's a wild card, it's playable
            // For other cards, check if they match color or type/number
            Card card = hand.get(i);
            if (card.getType() == CardType.WILD || card.getType() == CardType.WILD_DRAW_FOUR) {
                return i;
            }
            if (card.getColor() == currentCard.getColor()) {
                return i;
            }
            if (card.getType() == currentCard.getType()) {
                return i;
            }
            if (card.getType() == CardType.NUMBER && currentCard.getType() == CardType.NUMBER &&
                Objects.equals(card.getNumber(), currentCard.getNumber())) {
                return i;
            }
        }
        return -1;
    }
    
    /**
     * Prints the current game state in a readable format.
     */
    private static void printGameState(GameState state) {
        System.out.println("Current Player: " + state.getCurrentPlayerId());
        System.out.println("Direction: " + state.getDirection());
        System.out.println("Phase: " + state.getGamePhase());
        System.out.println("Current Card: " + formatCard(state.getCurrentCard()));
        System.out.println("Players:");
        for (Map.Entry<Long, PlayerGameInfo> entry : state.getPlayers().entrySet()) {
            PlayerGameInfo info = entry.getValue();
            System.out.println("  " + info.getUsername() + " (ID: " + entry.getKey() + "): " + 
                             info.getCardCount() + " cards, UNO: " + info.getHasUno());
        }
    }
    
    /**
     * Formats a card for display.
     */
    private static String formatCard(Card card) {
        if (card == null) return "None";
        
        String color = card.getColor().name();
        String type = card.getType().name();
        
        if (card.getType() == CardType.NUMBER) {
            return color + " " + card.getNumber();
        } else {
            return color + " " + type;
        }
    }
    
    /**
     * Formats a player's hand for display.
     */
    private static String formatHand(List<Card> hand) {
        if (hand.isEmpty()) return "Empty";
        
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < hand.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append("[").append(i).append("]").append(formatCard(hand.get(i)));
        }
        return sb.toString();
    }
}