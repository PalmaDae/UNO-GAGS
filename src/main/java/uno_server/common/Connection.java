package uno_server.common;

import uno_proto.common.NetworkMessage;
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

    // Используем Object-потоки для сериализации объектов
    private ObjectInputStream objectInputStream;
    private ObjectOutputStream objectOutputStream;
    private final AtomicBoolean isClosed = new AtomicBoolean(false);

    // Очередь NetworkMessage
    private final BlockingQueue<NetworkMessage> sendQueue = new LinkedBlockingQueue<>();
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

        OutputStream outputStream = socket.getOutputStream();
        objectOutputStream = new ObjectOutputStream(outputStream);
        objectOutputStream.flush(); // Отправляем заголовок

        InputStream inputStream = socket.getInputStream();
        objectInputStream = new ObjectInputStream(inputStream);
    }

    private void startSendThread() {
        sendThread = new Thread(() -> {
            while (!isClosed.get() && socket.isConnected()) {
                try {
                    NetworkMessage message = sendQueue.take();
                    sendObjectInternal(message);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    System.err.println("Ошибка отправки сообщения клиенту #" + clientId);
                    break;
                }
            }
        });
        sendThread.setName("Connection-Send-Thread-" + clientId);
        sendThread.setDaemon(true);
        sendThread.start();
    }

    public NetworkMessage readNetworkMessage() throws IOException {
        if (isClosed.get()) {
            throw new IOException("Connection closed");
        }

        try {
            Object obj = objectInputStream.readObject();
            if (obj instanceof NetworkMessage) {
                return (NetworkMessage) obj;
            }
            return null;
        } catch (SocketTimeoutException e) {
            // Таймаут - нормальная ситуация, клиент может просто не отправлять данные
            return null;
        } catch (SocketException e) {
            if (!isClosed.get()) {
                System.err.println("Сокет исключение при чтении: " + e.getMessage());
            }
            throw new IOException("Connection lost", e);
        } catch (ClassNotFoundException e) {
            throw new IOException("Неизвестный класс при десериализации", e);
        } catch (EOFException e) {
            // Клиент закрыл соединение
            return null;
        }
    }

    public void sendNetworkMessageAsync(NetworkMessage message) {
        if (!isClosed.get() && socket.isConnected()) {
            sendQueue.offer(message);
        }
    }

    // Внутренний метод отправки объекта (синхронизирован для безопасности)
    private synchronized void sendObjectInternal(Object obj) throws IOException {
        try {
            objectOutputStream.writeObject(obj);
            objectOutputStream.flush();
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

            // Закрываем ресурсы в правильном порядке
            try {
                if (objectInputStream != null) objectInputStream.close();
                if (objectOutputStream != null) objectOutputStream.close();
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