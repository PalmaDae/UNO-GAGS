package uno_server.common

import java.io.*
import java.net.Socket
import java.net.SocketException
import java.util.logging.Level
import java.util.logging.Logger


/**
 * Класс Connection представляет собой обёртку над сокетом клиента.
 * Предоставляет удобные методы для чтения и отправки строк текста.
 * Автоматически устанавливает таймаут для операций чтения.
 */
class Connection @JvmOverloads constructor(
    private val socket: Socket?,
    private val socketTimeoutMs: Int = 120000 // 120 секунд таймаут по умолчанию
) : Closeable {
    private var inputStream: InputStreamReader? = null
    private var outputStream: OutputStreamWriter? = null
    private var reader: BufferedReader? = null
    private var writer: BufferedWriter? = null

    /**
     * Конструктор создаёт Connection из сокета.
     * Инициализирует потоки ввода-вывода и устанавливает таймаут.
     *
     * @param socket сокет клиента
     */
    init {
        initialize()
    }

    /**
     * Инициализация потоков ввода-вывода.
     * Устанавливает таймаут на чтение из сокета.
     */
    private fun initialize() {
        this.isAlive
        try {
            socket!!.setSoTimeout(socketTimeoutMs)
            inputStream = InputStreamReader(socket.getInputStream())
            outputStream = OutputStreamWriter(socket.getOutputStream())
            reader = BufferedReader(inputStream!!)
            writer = BufferedWriter(outputStream!!)
            logger.log(Level.INFO, "Connection initialized for " + socket.getRemoteSocketAddress())
        } catch (e: SocketException) {
            logger.log(Level.SEVERE, "Socket exception during initialization", e)
            throw RuntimeException("Failed to initialize connection", e)
        } catch (e: IOException) {
            logger.log(Level.SEVERE, "IO exception during initialization", e)
            throw RuntimeException("Failed to initialize connection", e)
        }
    }

    private val isAlive: Unit
        /**
         * Проверяет, что сокет открыт.
         * Бросает исключение, если сокет закрыт.
         */
        get() {
            if (socket!!.isClosed) {
                throw RuntimeException("Socket is closed")
            }
        }

    /**
     * Отправляет строку клиенту с переводом строки.
     * Автоматически сбрасывает буфер после отправки.
     *
     * @param msg сообщение для отправки
     * @throws IOException если произошла ошибка при отправке
     */
    @Throws(IOException::class)
    fun sendLine(msg: String) {
        try {
            writer!!.write(msg)
            writer!!.newLine()
            writer!!.flush()
            logger.log(Level.FINE, "Sent to " + socket!!.getRemoteSocketAddress() + ": " + msg)
        } catch (e: IOException) {
            logger.log(Level.WARNING, "Failed to send message to " + socket!!.getRemoteSocketAddress(), e)
            throw e
        }
    }

    /**
     * Читает одну строку от клиента.
     * Блокирует выполнение до получения данных или истечения таймаута.
     *
     * @return строка, полученная от клиента, или null если соединение закрыто
     * @throws IOException если произошла ошибка при чтении
     */
    @Throws(IOException::class)
    fun readLine(): String? {
        try {
            val line = reader!!.readLine()
            if (line != null) {
                logger.log(Level.FINE, "Received from " + socket!!.getRemoteSocketAddress() + ": " + line)
            } else {
                logger.log(Level.INFO, "Connection closed by " + socket!!.getRemoteSocketAddress())
            }
            return line
        } catch (e: IOException) {
            logger.log(Level.WARNING, "Failed to read from " + socket!!.getRemoteSocketAddress(), e)
            throw e
        }
    }

    /**
     * Закрывает соединение и освобождает ресурсы.
     * Закрывает все потоки и сокет.
     */
    override fun close() {
        logger.log(Level.INFO, "Closing connection for ${socket!!.getRemoteSocketAddress()}")
        runCatching {
            reader?.close()
            writer?.close()
            inputStream?.close()
            outputStream?.close()
            socket?.close()
        }.onSuccess {
            logger.log(Level.INFO, "All resources for ${socket.getRemoteSocketAddress()} are closed")
        }.onFailure {
            logger.log(Level.INFO, "Fail closing resources for ${socket.getRemoteSocketAddress()}")
        }
    }

    val remoteAddress: String?
        /**
         * Возвращает адрес клиента в виде строки.
         *
         * @return строковое представление адреса клиента
         */
        get() = socket!!.getRemoteSocketAddress().toString()

    companion object {
        private val logger: Logger = Logger.getLogger(Connection::class.java.getName())
    }
}