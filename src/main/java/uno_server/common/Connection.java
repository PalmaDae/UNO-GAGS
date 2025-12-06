package uno_server.common;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Класс Connection представляет собой обёртку над сокетом клиента.
 * Предоставляет удобные методы для чтения и отправки строк текста.
 * Автоматически устанавливает таймаут 30 секунд для операций чтения.
 */
public class Connection implements Closeable {

    private static final Logger logger = Logger.getLogger(Connection.class.getName());

    private Socket socket;
    private final int socketTimeoutMs = 30000; // 30 секунд таймаут
    private InputStreamReader inputStream;
    private OutputStreamWriter outputStream;
    private BufferedReader reader;
    private BufferedWriter writer;

    /**
     * Конструктор создаёт Connection из сокета.
     * Инициализирует потоки ввода-вывода и устанавливает таймаут.
     *
     * @param socket сокет клиента
     */
    public Connection(Socket socket) {
        this.socket = socket;
        initialize();
    }

    /**
     * Инициализация потоков ввода-вывода.
     * Устанавливает таймаут на чтение из сокета.
     */
    private void initialize() {
        isAlive();
        try {
            socket.setSoTimeout(socketTimeoutMs);
            inputStream = new InputStreamReader(socket.getInputStream());
            outputStream = new OutputStreamWriter(socket.getOutputStream());
            reader = new BufferedReader(inputStream);
            writer = new BufferedWriter(outputStream);
            logger.log(Level.INFO, "Connection initialized for " + socket.getRemoteSocketAddress());
        } catch (SocketException e) {
            logger.log(Level.SEVERE, "Socket exception during initialization", e);
            throw new RuntimeException("Failed to initialize connection", e);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "IO exception during initialization", e);
            throw new RuntimeException("Failed to initialize connection", e);
        }
    }

    /**
     * Проверяет, что сокет открыт.
     * Бросает исключение, если сокет закрыт.
     */
    private void isAlive() {
        if (socket.isClosed()) {
            throw new RuntimeException("Socket is closed");
        }
    }

    /**
     * Отправляет строку клиенту с переводом строки.
     * Автоматически сбрасывает буфер после отправки.
     *
     * @param msg сообщение для отправки
     * @throws IOException если произошла ошибка при отправке
     */
    public void sendLine(String msg) throws IOException {
        try {
            writer.write(msg);
            writer.newLine();
            writer.flush();
            logger.log(Level.FINE, "Sent to " + socket.getRemoteSocketAddress() + ": " + msg);
        } catch (IOException e) {
            logger.log(Level.WARNING, "Failed to send message to " + socket.getRemoteSocketAddress(), e);
            throw e;
        }
    }

    /**
     * Читает одну строку от клиента.
     * Блокирует выполнение до получения данных или истечения таймаута.
     *
     * @return строка, полученная от клиента, или null если соединение закрыто
     * @throws IOException если произошла ошибка при чтении
     */
    public String readLine() throws IOException {
        try {
            String line = reader.readLine();
            if (line != null) {
                logger.log(Level.FINE, "Received from " + socket.getRemoteSocketAddress() + ": " + line);
            } else {
                logger.log(Level.INFO, "Connection closed by " + socket.getRemoteSocketAddress());
            }
            return line;
        } catch (IOException e) {
            logger.log(Level.WARNING, "Failed to read from " + socket.getRemoteSocketAddress(), e);
            throw e;
        }
    }

    /**
     * Закрывает соединение и освобождает ресурсы.
     * Закрывает все потоки и сокет.
     */
    @Override
    public void close() {
        logger.log(Level.INFO, "Closing connection for " + socket.getRemoteSocketAddress());
        try {
            if (reader != null) reader.close();
        } catch (IOException e) {
            logger.log(Level.WARNING, "Error closing reader", e);
        }
        try {
            if (writer != null) writer.close();
        } catch (IOException e) {
            logger.log(Level.WARNING, "Error closing writer", e);
        }
        try {
            if (inputStream != null) inputStream.close();
        } catch (IOException e) {
            logger.log(Level.WARNING, "Error closing input stream", e);
        }
        try {
            if (outputStream != null) outputStream.close();
        } catch (IOException e) {
            logger.log(Level.WARNING, "Error closing output stream", e);
        }
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            logger.log(Level.WARNING, "Error closing socket", e);
        }
    }

    /**
     * Возвращает адрес клиента в виде строки.
     *
     * @return строковое представление адреса клиента
     */
    public String getRemoteAddress() {
        return socket.getRemoteSocketAddress().toString();
    }
}
