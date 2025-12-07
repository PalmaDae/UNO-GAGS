package uno_ui

import uno_proto.common.*
import uno_server.protocol.MessageParser
import java.io.*
import java.net.Socket
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import java.util.function.Consumer

/**
 * Handles TCP socket communication with the server.
 * Sends and receives NetworkMessage objects.
 * Includes heartbeat mechanism to keep connection alive.
 */
class NetworkClient(
    private val host: String = "localhost",
    private val port: Int = 9090
) {
    private var socket: Socket? = null
    private var out: PrintWriter? = null
    private var input: BufferedReader? = null

    private val serializer = MessageSerializer()
    private val outgoingMessages: BlockingQueue<NetworkMessage> = LinkedBlockingQueue()
    
    private var messageListener: Consumer<NetworkMessage>? = null
    private var senderThread: Thread? = null
    private var receiverThread: Thread? = null
    private var heartbeatThread: Thread? = null
    
    @Volatile
    private var running = false
    
    @Volatile
    private var lastPongTime = AtomicLong(System.currentTimeMillis())
    
    @Volatile
    private var lastPingSentTime = AtomicLong(0L)
    
    private var messageIdCounter = 1L
    
    companion object {
        private const val HEARTBEAT_INTERVAL_MS = 30000L // 30 seconds
        private const val PONG_TIMEOUT_MS = 10000L // 10 seconds
    }

    fun setMessageListener(listener: (NetworkMessage) -> Unit) {
        messageListener = Consumer(listener)
    }

    fun connect(): Boolean {
        return try {
            println("[NetworkClient] Connecting to $host:$port...")
            socket = Socket(host, port)
            out = PrintWriter(socket!!.getOutputStream(), true)
            input = BufferedReader(InputStreamReader(socket!!.getInputStream()))
            
            println("[NetworkClient] Connected!")
            
            running = true
            lastPongTime.set(System.currentTimeMillis())
            lastPingSentTime.set(0L)
            
            startSenderThread()
            startReceiverThread()
            startHeartbeatThread()
            
            true
        } catch (e: IOException) {
            System.err.println("[NetworkClient] Failed to connect: ${e.message}")
            false
        }
    }

    fun disconnect() {
        println("[NetworkClient] Disconnecting...")
        running = false
        
        try {
            socket?.takeIf { !it.isClosed }?.close()
        } catch (e: IOException) {
            System.err.println("[NetworkClient] Error closing socket: ${e.message}")
        }
        
        senderThread?.let {
            it.join(1000)
        }
        receiverThread?.let {
            it.join(1000)
        }
        heartbeatThread?.let {
            it.join(1000)
        }
        
        println("[NetworkClient] Disconnected")
    }

    fun sendMessage(method: Method, payload: Payload) {
        val message = NetworkMessage(
            messageIdCounter++,
            Version.V1,
            method,
            payload,
            System.currentTimeMillis()
        )
        outgoingMessages.offer(message)
    }

    private fun startSenderThread() {
        senderThread = Thread({
            println("[Sender] Thread started")
            
            while (running) {
                try {
                    val message = outgoingMessages.poll(1, TimeUnit.SECONDS)
                    
                    if (message != null) {
                        val json = serializer.serialize(message)
                        println("[Sender] Sending: $json")
                        out?.println(json)
                    }
                } catch (e: InterruptedException) {
                    break
                } catch (e: Exception) {
                    System.err.println("[Sender] Error sending message: ${e.message}")
                }
            }
            
            println("[Sender] Thread stopped")
        }, "NetworkClient-Sender").apply {
            isDaemon = true
            start()
        }
    }

    private fun startReceiverThread() {
        receiverThread = Thread({
            println("[Receiver] Thread started")
            
            while (running) {
                try {
                    val line = input?.readLine()
                    
                    if (line == null) {
                        println("[Receiver] Server closed connection")
                        break
                    }
                    
                    println("[Receiver] Received: $line")
                    
                    val message = serializer.deserialize(line)
                    message?.let { 
                        // Handle PONG internally for heartbeat
                        if (it.method == Method.PONG) {
                            lastPongTime.set(System.currentTimeMillis())
                            println("[Heartbeat] PONG received")
                        }
                        // Notify listener for all messages (including PONG if they want to handle it)
                        messageListener?.accept(it) 
                    }
                    
                } catch (e: IOException) {
                    if (running) {
                        System.err.println("[Receiver] Error reading from server: ${e.message}")
                    }
                    break
                } catch (e: Exception) {
                    System.err.println("[Receiver] Error parsing message: ${e.message}")
                    e.printStackTrace()
                }
            }
            
            println("[Receiver] Thread stopped")
        }, "NetworkClient-Receiver").apply {
            isDaemon = true
            start()
        }
    }

    private fun startHeartbeatThread() {
        heartbeatThread = Thread({
            println("[Heartbeat] Thread started")
            
            while (running) {
                try {
                    Thread.sleep(HEARTBEAT_INTERVAL_MS)
                    
                    if (!running) break
                    
                    // Send PING
                    println("[Heartbeat] Sending PING...")
                    lastPingSentTime.set(System.currentTimeMillis())
                    sendMessage(Method.PING, MessageParser.EmptyPayload)
                    
                    // Wait a bit to allow PONG to arrive
                    Thread.sleep(PONG_TIMEOUT_MS)
                    
                    if (!running) break
                    
                    // Check if we received PONG in time
                    val timeSinceLastPong = System.currentTimeMillis() - lastPongTime.get()
                    val timeSincePing = System.currentTimeMillis() - lastPingSentTime.get()
                    
                    if (timeSincePing < PONG_TIMEOUT_MS + 1000) {
                        // We just sent a ping, check if we got a pong
                        if (timeSinceLastPong > HEARTBEAT_INTERVAL_MS + PONG_TIMEOUT_MS) {
                            System.err.println("[Heartbeat] No PONG received in ${PONG_TIMEOUT_MS}ms, reconnecting...")
                            reconnect()
                            break
                        }
                    }
                    
                } catch (e: InterruptedException) {
                    break
                } catch (e: Exception) {
                    System.err.println("[Heartbeat] Error: ${e.message}")
                    e.printStackTrace()
                }
            }
            
            println("[Heartbeat] Thread stopped")
        }, "NetworkClient-Heartbeat").apply {
            isDaemon = true
            start()
        }
    }
    
    private fun reconnect() {
        println("[NetworkClient] Attempting to reconnect...")
        disconnect()
        Thread.sleep(1000) // Wait a bit before reconnecting
        connect()
    }

    fun isConnected(): Boolean = socket?.let { !it.isClosed && running } ?: false
}
