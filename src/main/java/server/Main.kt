package server

import server.common.Server
import java.util.logging.Level
import java.util.logging.Logger

object Main {
    private val logger: Logger = Logger.getLogger(Main::class.java.getName())

    @JvmStatic
    fun main(args: Array<String>) {
        logger.log(Level.INFO, "Starting UNO server...")

        try {
            Server().use { server ->
                // код, выполняющийся для корректной остановки сервера
                // на это отводится отдельный поток
                Runtime.getRuntime().addShutdownHook(Thread {
                    logger.log(Level.INFO, "Shutdown hook triggered. Closing server...")
                    try {
                        server.close()
                    } catch (e: Exception) {
                        logger.log(Level.SEVERE, "Error closing server from shutdown hook", e)
                    }
                })
                // запускаем сервер
                server.listen()
            }
        } catch (e: Exception) {
            logger.log(Level.SEVERE, "Server terminated unexpectedly", e)
        }
    }
}