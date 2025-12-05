package uno_server.common;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ConnectionManager {

    private final Map<Integer, Connection> connections = new ConcurrentHashMap<>();
    private final Map<Integer, String> clientUsernames = new ConcurrentHashMap<>();

    public void addConnection(Connection connection) {
        connections.put(connection.getClientId(), connection);
    }

    public void removeConnection(int clientId) {
        connections.remove(clientId);
        clientUsernames.remove(clientId);
    }

    public Connection getConnection(int clientId) {
        return connections.get(clientId);
    }

    public void setClientUsername(int clientId, String username) {
        clientUsernames.put(clientId, username);
    }

    public String getClientUsername(int clientId) {
        return clientUsernames.get(clientId);
    }

    public void sendToClient(int clientId, String message) {
        Connection connection = connections.get(clientId);
        if (connection != null && connection.isConnected()) {
            try {
                connection.sendMessageAsync(message);
            } catch (Exception e) {
                System.err.println("Ошибка отправки клиенту #" + clientId);
            }
        }
    }

    public List<Integer> getConnectedClientIds() {
        return new ArrayList<>(connections.keySet());
    }

    public int getConnectionCount() {
        return connections.size();
    }

    public void closeAllConnections() {
        connections.values().forEach(Connection::close);
        connections.clear();
        clientUsernames.clear();
    }
}