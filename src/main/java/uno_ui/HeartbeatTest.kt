package uno_ui

import uno_proto.common.Method

/**
 * Simple test to verify heartbeat functionality.
 * Connects to server and waits, letting heartbeat mechanism maintain the connection.
 */
object HeartbeatTest {
    @JvmStatic
    fun main(args: Array<String>) {
        println("=== Heartbeat Test Client ===")
        println("This client will connect and wait, testing the heartbeat mechanism")
        println("")
        
        val client = NetworkClient("localhost", 9090)
        
        // Set up message listener
        client.setMessageListener { message ->
            println("[Message Received] Method: ${message.method}, Timestamp: ${message.timestamp}")
        }
        
        // Connect
        println("Connecting to server...")
        if (!client.connect()) {
            System.err.println("Failed to connect to server!")
            return
        }
        
        println("Connected successfully!")
        println("Heartbeat will send PING every 30 seconds")
        println("Press Ctrl+C to exit")
        println("")
        
        // Keep the program running
        try {
            // Wait indefinitely, heartbeat will keep connection alive
            Thread.sleep(Long.MAX_VALUE)
        } catch (e: InterruptedException) {
            println("Interrupted, shutting down...")
        } finally {
            client.disconnect()
            println("Disconnected")
        }
    }
}
