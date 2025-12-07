package uno_ui

import java.util.logging.Level
import java.util.logging.Logger

/**
 * Manages reconnection logic with exponential backoff.
 */
class ReconnectManager(
    private val maxRetries: Int = 5,
    private val initialDelayMs: Long = 1000
) {
    private val logger = Logger.getLogger(ReconnectManager::class.java.name)
    private var currentRetry = 0
    private var reconnecting = false

    fun shouldRetry(): Boolean = currentRetry < maxRetries

    fun getDelayMs(): Long {
        val delay = initialDelayMs * (1 shl currentRetry)
        return delay.coerceAtMost(30000)
    }

    fun attemptReconnect(reconnectAction: () -> Boolean): Boolean {
        if (reconnecting) return false

        reconnecting = true
        currentRetry = 0

        while (shouldRetry()) {
            logger.log(Level.INFO, "Attempting reconnect ${currentRetry + 1}/$maxRetries")

            if (reconnectAction()) {
                logger.log(Level.INFO, "Reconnect successful")
                reset()
                return true
            }

            currentRetry++
            if (shouldRetry()) {
                val delay = getDelayMs()
                logger.log(Level.INFO, "Reconnect failed, waiting ${delay}ms before retry")
                try {
                    Thread.sleep(delay)
                } catch (e: InterruptedException) {
                    Thread.currentThread().interrupt()
                    break
                }
            }
        }

        logger.log(Level.WARNING, "Reconnect failed after $maxRetries attempts")
        reconnecting = false
        return false
    }

    fun reset() {
        currentRetry = 0
        reconnecting = false
    }

    fun isReconnecting(): Boolean = reconnecting
}
