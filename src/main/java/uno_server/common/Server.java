package uno_server.common;

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
    // Управление пулом потоков. ExecutorService исполняет асинхронный код в одном или нескольких потоках.
    private final ExecutorService clientThreadPool;
    private final ExecutorService serverThread;
    // ExecutorService - продвинутая замена Thread. Управляет потоками автоматически.
    private final AtomicInteger clientCounter = new AtomicInteger(0);
    // Коллбек для обработки новых подключений. Он и отвечает за реализацию обработки подключений
    private Consumer<Connection> connectionHandler;

    public Server() {
        this(DEFAULT_PORT);
    }

    public Server(int port) {
        this.port = port;
        // Создаём пул с десятью потоками
        this.clientThreadPool = Executors.newCachedThreadPool();
        // Создаём пул, который автоматически будет создавать, удалять и переиспользовать потоки
        this.serverThread = Executors.newSingleThreadExecutor();
    }

    public void setConnectionHandler(Consumer<Connection> connectionHandler) {
        this.connectionHandler = connectionHandler;
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
                        handleClientConnection(connection); // Основная работа с клиентом
                    } catch (Exception e) {
                        System.err.println("Ошибка обработки клиента #" + clientId +
                                ": " + e.getMessage());
                    } finally {
                        connections.remove(connection);
                        connection.close();
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
        // Здесь будет основная логика обработки клиента
        System.out.println("Обработка клиента #" + connection.getClientId());

        // Пример: чтение данных от клиента
        while (connection.isConnected() && isRunning.get()) {
            try {
                String message = connection.readMessage();
                if (message == null) {
                    break; // Клиент отключился
                }

                System.out.println("Получено от клиента #" + connection.getClientId() +
                        ": " + message);

                // Эхо1-ответ
                connection.sendMessage("ECHO: " + message);

            } catch (IOException e) {
                System.err.println("Ошибка чтения от клиента #" + connection.getClientId() +
                        ": " + e.getMessage());
                break;
            }
        }

        System.out.println("Клиент #" + connection.getClientId() + " отключился");
    }

    public void broadcastMessage(String message) {
        connections.forEach(connection -> {
            try {
                if (connection.isConnected()) {
                    connection.sendMessage(message);
                }
            } catch (IOException e) {
                System.err.println("Ошибка отправки сообщения клиенту #" +
                        connection.getClientId());
            }
        });
    }

    public int getActiveConnectionsCount() {
        return connections.size();
    }

    public boolean isRunning() {
        return isRunning.get();
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
}