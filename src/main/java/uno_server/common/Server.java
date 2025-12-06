package uno_server.common;

import java.io.Closeable;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Класс Server отвечает за прослушивание порта и обработку клиентов.
 * Создаёт отдельный поток для каждого подключившегося клиента.
 */
public class Server implements Closeable {

    private static final Logger logger = Logger.getLogger(Server.class.getName());

    private static final int PORT = 9090;

    private final ServerSocket serverSocket;
    private final List<Connection> connections = new CopyOnWriteArrayList<>();
    private final AtomicInteger index = new AtomicInteger(0);
    private volatile boolean running = false;

    /**
     * Создаёт сервер и подготавливает сокет на стандартном порту 9090.
     */
    public Server() {
        this(PORT);
    }

    /**
     * Создаёт сервер и слушает указанный порт.
     *
     * @param port порт для прослушивания
     */
    public Server(int port) {
        try {
            serverSocket = new ServerSocket(port);
            serverSocket.setReuseAddress(true);
            logger.log(Level.INFO, "Server socket created on port " + port);
        } catch (IOException e) {
            throw new RuntimeException("Failed to initialize server on port " + port, e);
        }
    }

    /**
     * Основной цикл обработки клиентов.
     * Принимает подключения и запускает отдельные потоки для чтения сообщений.
     */
    public void handle() {
        running = true;
        logger.log(Level.INFO, "Server started on port " + serverSocket.getLocalPort());

        while (running && !serverSocket.isClosed()) {
            try {
                Socket socket = serverSocket.accept();
                Connection connection = new Connection(socket);
                connections.add(connection);
                int clientId = index.incrementAndGet();
                logger.log(Level.INFO, "Client #" + clientId + " connected from " + connection.getRemoteAddress());

                Thread clientThread = new Thread(() -> processClient(connection, clientId));
                clientThread.setName("uno-server-client-" + clientId);
                clientThread.setDaemon(true);
                clientThread.start();
            } catch (SocketException e) {
                if (!running) {
                    logger.log(Level.INFO, "Server socket closed. Accept loop exiting.");
                    break;
                }
                logger.log(Level.WARNING, "Socket exception in accept loop", e);
            } catch (IOException e) {
                logger.log(Level.SEVERE, "IO exception in accept loop", e);
            }
        }

        logger.log(Level.INFO, "Server stopped accepting connections");
    }

    /**
     * Обрабатывает конкретного клиента в отдельном потоке.
     *
     * @param connection подключение клиента
     * @param clientId   номер клиента для логирования
     */
    private void processClient(Connection connection, int clientId) {
        try {
            String line;
            while ((line = connection.readLine()) != null) {
                logger.log(Level.INFO, "Message from client #" + clientId + ": " + line);
            }
        } catch (IOException e) {
            logger.log(Level.WARNING, "Client #" + clientId + " communication error", e);
        } finally {
            connections.remove(connection);
            connection.close();
            logger.log(Level.INFO, "Client #" + clientId + " disconnected");
        }
    }

    /**
     * Останавливает сервер, закрывает сокет и активные подключения.
     *
     * @throws IOException если произошла ошибка при закрытии серверного сокета
     */
    @Override
    public void close() throws IOException {
        running = false;
        logger.log(Level.INFO, "Stopping server and closing all connections");

        for (Connection connection : connections) {
            connection.close();
        }
        connections.clear();

        if (!serverSocket.isClosed()) {
            serverSocket.close();
        }
    }
}
