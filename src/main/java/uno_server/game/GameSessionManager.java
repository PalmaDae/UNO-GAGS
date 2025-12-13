package uno_server.game;

import uno_proto.dto.GameState;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class GameSessionManager {
    
    // Thread-safe map to store active game sessions
    private final Map<Long, GameSession> activeSessions;
    
    public GameSessionManager() {
        this.activeSessions = new ConcurrentHashMap<>();
    }
    
    public GameSession createSession(long roomId, java.util.List<PlayerState> players) {
        if (activeSessions.containsKey(roomId)) {
            throw new IllegalArgumentException("Game session already exists for room: " + roomId);
        }
        
        GameSession session = new GameSession(roomId, players);
        activeSessions.put(roomId, session);
        
        return session;
    }

    public void removeSession(long roomId) {
        activeSessions.remove(roomId);
    }

    public GameState playCard(long roomId, long playerId, int cardIndex, uno_proto.dto.CardColor chosenColor) {
        GameSession session = activeSessions.get(roomId);
        if (session == null) {
            throw new IllegalArgumentException("No game session found for room: " + roomId);
        }
        
        session.playCard(playerId, cardIndex, chosenColor);
        return session.getGameState();
    }

    public GameState drawCard(long roomId, long playerId) {
        GameSession session = activeSessions.get(roomId);
        if (session == null) {
            throw new IllegalArgumentException("No game session found for room: " + roomId);
        }
        
        session.drawCard(playerId);
        return session.getGameState();
    }

    public GameState sayUno(long roomId, long playerId) {
        GameSession session = activeSessions.get(roomId);
        if (session == null) {
            throw new IllegalArgumentException("No game session found for room: " + roomId);
        }
        
        session.sayUno(playerId);
        return session.getGameState();
    }
}