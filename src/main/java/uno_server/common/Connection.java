package uno_server.common;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class Connection implements Closeable {

    private final Socket socket;
    private final int clientId;
    private static final int SOCKET_TIMEOUT_MS = 30000;
    private static final int BUFFER_SIZE = 8192;

    private BufferedReader reader;
    private BufferedWriter writer;
    private final AtomicBoolean isClosed = new AtomicBoolean(false);

    // Очередь для асинхронной отправки сообщений
    private final BlockingQueue<String> sendQueue = new LinkedBlockingQueue<>();
    private Thread sendThread;

    public Connection(Socket socket, int clientId) throws IOException {
        this.socket = socket;
        this.clientId = clientId;
        initialize();
        startSendThread();
    }

    private void initialize() throws IOException {
        if (socket.isClosed()) {
            throw new IOException("Socket is closed");
        }

        socket.setSoTimeout(SOCKET_TIMEOUT_MS);
        socket.setKeepAlive(true);
        socket.setTcpNoDelay(true);

        InputStream inputStream = socket.getInputStream();
        OutputStream outputStream = socket.getOutputStream();

        reader = new BufferedReader(new InputStreamReader(inputStream));
        writer = new BufferedWriter(new OutputStreamWriter(outputStream));
    }

    private void startSendThread() {
        sendThread = new Thread(() -> {
            while (!isClosed.get() && socket.isConnected()) {
                try {
                    String message = sendQueue.take(); // Блокируется, пока нет сообщений
                    internalSend(message);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (IOException e) {
                    System.err.println("Ошибка отправки сообщения клиенту #" + clientId +
                            ": " + e.getMessage());
                    break;
                }
            }
        });
        sendThread.setName("Connection-Send-Thread-" + clientId);
        sendThread.setDaemon(true);
        sendThread.start();
    }

    // Синхронное чтение сообщения
    public String readMessage() throws IOException {
        if (isClosed.get()) {
            throw new IOException("Connection closed");
        }

        try {
            return reader.readLine();
        } catch (SocketTimeoutException e) {
            // Таймаут - нормальная ситуация
            return null;
        } catch (SocketException e) {
            if (!isClosed.get()) {
                System.err.println("Сокет исключение при чтении: " + e.getMessage());
            }
            throw new IOException("Connection lost", e);
        }
    }

    // Асинхронная отправка (добавляет в очередь)
    public void sendMessageAsync(String message) {
        if (!isClosed.get() && socket.isConnected()) {
            sendQueue.offer(message);
        }
    }

    // Синхронная отправка
    public void sendMessage(String message) throws IOException {
        if (isClosed.get()) {
            throw new IOException("Connection closed");
        }
        internalSend(message);
    }

    private synchronized void internalSend(String message) throws IOException {
        try {
            writer.write(message);
            writer.newLine();
            writer.flush();
        } catch (SocketException e) {
            throw new IOException("Connection lost", e);
        }
    }

    public boolean isConnected() {
        return !isClosed.get() && socket.isConnected() && !socket.isClosed();
    }

    public String getRemoteAddress() {
        return socket.getInetAddress().getHostAddress();
    }

    public int getRemotePort() {
        return socket.getPort();
    }

    public int getClientId() {
        return clientId;
    }

    @Override
    public void close() {
        if (isClosed.compareAndSet(false, true)) {
            // Останавливаем поток отправки
            if (sendThread != null && sendThread.isAlive()) {
                sendThread.interrupt();
            }

            // Закрываем ресурсы
            try {
                if (writer != null) writer.close();
                if (reader != null) reader.close();
                if (!socket.isClosed()) socket.close();

                System.out.println("Соединение #" + clientId + " закрыто: " +
                        getRemoteAddress() + ":" + getRemotePort());
            } catch (IOException e) {
                System.err.println("Ошибка закрытия соединения #" + clientId +
                        ": " + e.getMessage());
            }
        }
    }
}