package uno_server.common;

import uno_proto.common.NetworkMessage;

import java.io.Closeable;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class Server implements Closeable, Runnable {

    private static final int DEFAULT_PORT = 9090;

    private final int port;
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private ServerSocket serverSocket;
    private final List<Connection> connections = new CopyOnWriteArrayList<Connection>();
    private final ExecutorService clientThreadPool;
    private final ExecutorService serverThread;
    private final AtomicInteger clientCounter = new AtomicInteger(0);

    // Обработчик новых подключений
    private Consumer<Connection> connectionHandler;
    // Обработчик полученных сообщений
    private Consumer<MessageReceivedEvent> messageHandler;

    public Server() {
        this(DEFAULT_PORT);
    }

    public Server(int port) {
        this.port = port;
        // CachedThreadPool автоматически создает потоки по мере необходимости
        this.clientThreadPool = Executors.newCachedThreadPool();
        this.serverThread = Executors.newSingleThreadExecutor();
    }

    public void setConnectionHandler(Consumer<Connection> connectionHandler) {
        this.connectionHandler = connectionHandler;
    }

    public void setMessageHandler(Consumer<MessageReceivedEvent> messageHandler) {
        this.messageHandler = messageHandler;
    }

    public void start() {
        if (isRunning.get()) {
            System.out.println("Сервер уже запущен");
            return;
        }

        try {
            serverSocket = new ServerSocket(port);
            serverSocket.setReuseAddress(true); // Можно переиспользовать порт сразу после перезапуска
            isRunning.set(true);

            System.out.println("Сервер запущен на порту " + port);

            // Запускаем основной цикл сервера в отдельном потоке
            serverThread.submit(this);

        } catch (IOException e) {
            System.err.println("Ошибка запуска сервера: " + e.getMessage());
            close();
        }
    }

    @Override
    public void run() {
        while (isRunning.get() && !serverSocket.isClosed()) {
            try {
                Socket clientSocket = serverSocket.accept();
                int clientId = clientCounter.incrementAndGet();

                System.out.println("Новое подключение #" + clientId +
                        " от " + clientSocket.getInetAddress().getHostAddress());

                Connection connection = new Connection(clientSocket, clientId);
                connections.add(connection);

                // Обрабатываем подключение в отдельном потоке
                clientThreadPool.submit(() -> {
                    try {
                        if (connectionHandler != null) {
                            connectionHandler.accept(connection); // Вызываем внешний обработчик
                        }
                        handleClientConnection(connection);
                    } catch (Exception e) {
                        System.err.println("Ошибка обработки клиента #" + clientId +
                                ": " + e.getMessage());
                    } finally {
                        connections.remove(connection);
                        connection.close();
                        System.out.println("Клиент #" + clientId + " отключился");
                    }
                });

            } catch (IOException e) {
                if (isRunning.get()) {
                    System.err.println("Ошибка accept: " + e.getMessage());
                }
                break;
            }
        }
    }

    private void handleClientConnection(Connection connection) {
        System.out.println("Обработка клиента #" + connection.getClientId());

        while (connection.isConnected() && isRunning.get()) {
            try {
                NetworkMessage message = connection.readNetworkMessage();
                if (message == null) {
                    break; // Клиент отключился
                }

                System.out.println("Получено сообщение от клиента #" + connection.getClientId() +
                        ": метод=" + message.getMethod());

                // Отправляем событие обработчику сообщений
                if (messageHandler != null) {
                    messageHandler.accept(new MessageReceivedEvent(connection, message));
                }

            } catch (IOException e) {
                System.err.println("Ошибка чтения от клиента #" + connection.getClientId() +
                        ": " + e.getMessage());
                break;
            }
        }
    }

    public int getActiveConnectionsCount() {
        return connections.size();
    }

    public boolean isRunning() {
        return isRunning.get();
    }

    // Получение соединения по ID клиента
    public Connection getConnection(int clientId) {
        for (Connection connection : connections) {
            if (connection.getClientId() == clientId) {
                return connection;
            }
        }
        return null;
    }

    @Override
    public void close() {
        isRunning.set(false);

        // Закрываем все соединения
        connections.forEach(Connection::close);
        connections.clear();

        // Останавливаем пул потоков
        clientThreadPool.shutdown();
        serverThread.shutdown();

        // Закрываем серверный сокет
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                System.err.println("Ошибка закрытия серверного сокета: " + e.getMessage());
            }
        }

        System.out.println("Сервер остановлен");
    }

    // Событие получения сообщения
    public static class MessageReceivedEvent {
        private final Connection connection;
        private final NetworkMessage message;

        public MessageReceivedEvent(Connection connection, NetworkMessage message) {
            this.connection = connection;
            this.message = message;
        }

        public Connection getConnection() { return connection; }
        public NetworkMessage getMessage() { return message; }
        public int getClientId() { return connection.getClientId(); }
    }
}