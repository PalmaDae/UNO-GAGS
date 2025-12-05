package uno_server;

import uno_server.common.Server;

public class UnoServerApp {

    public static void main(String[] args) {
        Server server = new Server();

        // Устанавливаем обработчик подключений
        server.setConnectionHandler(connection -> {
            System.out.println("Новый клиент подключился: #" + connection.getClientId());

            // Здесь можно отправить приветственное сообщение
            try {
                connection.sendMessage("Добро пожаловать в UNO! Ваш ID: " + connection.getClientId());
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        // Запускаем сервер
        server.start();

        // Добавляем shutdown hook для graceful shutdown
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Завершение работы сервера...");
            server.close();
        }));

        // Пример: периодическая отправка статистики
        new Thread(() -> {
            while (server.isRunning()) {
                try {
                    Thread.sleep(60000); // Каждую минуту
                    String stats = "Сервер работает. Активных подключений: " +
                            server.getActiveConnectionsCount();
                    server.broadcastMessage(stats);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }).start();
    }
}