package uno_server;

import uno_server.common.Server;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Главный класс приложения для запуска сервера UNO.
 * Поднимает сервер на порту 9090 и ожидает подключений клиентов.
 */
public class ServerLauncher {

    private static final Logger logger = Logger.getLogger(ServerLauncher.class.getName());

    /**
     * Точка входа приложения.
     * Создаёт сервер, регистрирует shutdown hook для корректного завершения,
     * и запускает основной цикл обработки подключений.
     *
     * @param args аргументы командной строки (не используются)
     */
    public static void main(String[] args) {
        logger.log(Level.INFO, "Starting UNO server...");
        logger.log(Level.INFO, "Press Ctrl+C to stop the server");

        try (Server server = new Server()) {
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                logger.log(Level.INFO, "Shutdown hook triggered. Closing server...");
                try {
                    server.close();
                } catch (IOException e) {
                    logger.log(Level.SEVERE, "Error closing server from shutdown hook", e);
                }
            }));

            server.handle();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Server terminated unexpectedly", e);
        }
    }
}
