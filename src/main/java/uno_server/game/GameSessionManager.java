package uno_server.game;

import uno_server.game.PlayerState;
import uno_server.game.GameSession;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages multiple UNO game sessions.
 * Provides methods to create, access, and manage game sessions by room ID.
 * This is the main interface that the server will use to interact with game logic.
 */
public class GameSessionManager {
    
    // Thread-safe map to store active game sessions
    private final Map<Long, GameSession> activeSessions;
    
    public GameSessionManager() {
        this.activeSessions = new ConcurrentHashMap<>();
    }
    
    /**
     * Creates a new game session for the specified room.
     * 
     * @param roomId The unique identifier for the room
     * @param players The list of players who will participate in the game
     * @return The newly created GameSession
     * @throws IllegalArgumentException if a session already exists for the room
     */
    public GameSession createSession(long roomId, java.util.List<PlayerState> players) {
        if (activeSessions.containsKey(roomId)) {
            throw new IllegalArgumentException("Game session already exists for room: " + roomId);
        }
        
        GameSession session = new GameSession(roomId, players);
        activeSessions.put(roomId, session);
        
        return session;
    }
    
    /**
     * Gets an existing game session by room ID.
     * 
     * @param roomId The room ID
     * @return The GameSession, or null if not found
     */
    public GameSession getSession(long roomId) {
        return activeSessions.get(roomId);
    }
    
    /**
     * Removes and ends a game session.
     * 
     * @param roomId The room ID
     * @return The removed GameSession, or null if not found
     */
    public GameSession removeSession(long roomId) {
        return activeSessions.remove(roomId);
    }
    
    /**
     * Checks if a session exists for the given room.
     * 
     * @param roomId The room ID
     * @return true if session exists, false otherwise
     */
    public boolean hasSession(long roomId) {
        return activeSessions.containsKey(roomId);
    }
    
    /**
     * Gets all active sessions.
     * 
     * @return A map of room ID to GameSession
     */
    public Map<Long, GameSession> getAllSessions() {
        return new HashMap<>(activeSessions);
    }
    
    /**
     * Starts a game session.
     * This is a convenience method that delegates to the GameSession.
     * 
     * @param roomId The room ID
     * @return The current game state
     * @throws IllegalArgumentException if session not found
     */
    public uno_proto.dto.GameState startGame(long roomId) {
        GameSession session = activeSessions.get(roomId);
        if (session == null) {
            throw new IllegalArgumentException("No game session found for room: " + roomId);
        }
        
        // The game is already started when the session is created
        // This method is mainly for API consistency
        return session.getGameState();
    }
    
    /**
     * Plays a card in the specified game session.
     * 
     * @param roomId The room ID
     * @param playerId The player attempting to play the card
     * @param cardIndex The index of the card in the player's hand
     * @param chosenColor The color chosen for wild cards (null for non-wild cards)
     * @return The updated game state
     * @throws IllegalArgumentException if session not found
     */
    public uno_proto.dto.GameState playCard(long roomId, long playerId, int cardIndex, uno_proto.dto.CardColor chosenColor) {
        GameSession session = activeSessions.get(roomId);
        if (session == null) {
            throw new IllegalArgumentException("No game session found for room: " + roomId);
        }
        
        session.playCard(playerId, cardIndex, chosenColor);
        return session.getGameState();
    }
    
    /**
     * Makes the current player draw a card.
     * 
     * @param roomId The room ID
     * @param playerId The player attempting to draw
     * @return The updated game state
     * @throws IllegalArgumentException if session not found
     */
    public uno_proto.dto.GameState drawCard(long roomId, long playerId) {
        GameSession session = activeSessions.get(roomId);
        if (session == null) {
            throw new IllegalArgumentException("No game session found for room: " + roomId);
        }
        
        session.drawCard(playerId);
        return session.getGameState();
    }
    
    /**
     * Player declares UNO.
     * 
     * @param roomId The room ID
     * @param playerId The player declaring UNO
     * @return The updated game state
     * @throws IllegalArgumentException if session not found
     */
    public uno_proto.dto.GameState sayUno(long roomId, long playerId) {
        GameSession session = activeSessions.get(roomId);
        if (session == null) {
            throw new IllegalArgumentException("No game session found for room: " + roomId);
        }
        
        session.sayUno(playerId);
        return session.getGameState();
    }
    
    /**
     * Sets the chosen color after playing a wild card.
     * 
     * @param roomId The room ID
     * @param playerId The player who played the wild card
     * @param color The chosen color
     * @return The updated game state
     * @throws IllegalArgumentException if session not found
     */
    public uno_proto.dto.GameState setChosenColor(long roomId, long playerId, uno_proto.dto.CardColor color) {
        GameSession session = activeSessions.get(roomId);
        if (session == null) {
            throw new IllegalArgumentException("No game session found for room: " + roomId);
        }
        
        session.setChosenColor(color);
        return session.getGameState();
    }
    
    /**
     * Gets the current game state for a room.
     * 
     * @param roomId The room ID
     * @return The current game state
     * @throws IllegalArgumentException if session not found
     */
    public uno_proto.dto.GameState getGameState(long roomId) {
        GameSession session = activeSessions.get(roomId);
        if (session == null) {
            throw new IllegalArgumentException("No game session found for room: " + roomId);
        }
        
        return session.getGameState();
    }
    
    /**
     * Calculates and returns scores for a finished game.
     * 
     * @param roomId The room ID
     * @return Map of player ID to score
     * @throws IllegalArgumentException if session not found
     */
    public java.util.Map<Long, Integer> calculateScores(long roomId) {
        GameSession session = activeSessions.get(roomId);
        if (session == null) {
            throw new IllegalArgumentException("No game session found for room: " + roomId);
        }
        
        return session.calculateScores();
    }
}