package uno_server

import uno_server.common.Connection
import uno_server.protocol.ProtocolBridge
import java.io.IOException
import java.net.ServerSocket
import java.util.concurrent.atomic.AtomicInteger
import java.util.logging.Level
import java.util.logging.Logger

/**
 * Main server class - handles client connections and message processing
 */
class Server : AutoCloseable {
    companion object {
        private const val PORT = 9090
        private val logger = Logger.getLogger(Server::class.java.name)
    }

    private val serverSocket = ServerSocket(PORT)
    private val clientIndex = AtomicInteger(0)
    private val protocolBridge = uno_server.protocol.ProtocolBridge()
    private var running = true

    init {
        serverSocket.reuseAddress = true
        logger.info("Server created on port $PORT")
    }

    fun listen() {
        logger.info("Server started on port ${serverSocket.localPort}")
        
        while (running) {
            try {
                val socket = serverSocket.accept()
                val connection = Connection(socket)
                val clientId = clientIndex.incrementAndGet()
                logger.info("Client #$clientId connected from ${connection.getRemoteAddress()}")
                
                Thread {
                    handleClient(connection, clientId)
                }.apply {
                    name = "client-$clientId"
                    isDaemon = true
                    start()
                }
            } catch (e: IOException) {
                if (running) {
                    logger.warning("Accept error: ${e.message}")
                }
            }
        }
    }

    private fun handleClient(connection: Connection, clientId: Int) {
        try {
            var line: String?
            while (true) {
                line = connection.readLine() ?: break
                logger.info("Client #$clientId: $line")
                protocolBridge.handleMessage(connection, line, clientId)
            }
        } catch (e: Exception) {
            logger.warning("Client #$clientId error: ${e.message}")
        } finally {
            protocolBridge.handleDisconnect(connection, clientId)
            connection.close()
            logger.info("Client #$clientId disconnected")
        }
    }

    override fun close() {
        running = false
        serverSocket.close()
    }
}