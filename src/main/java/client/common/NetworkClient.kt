package client.common

import proto.common.Payload
import java.io.*
import java.net.Socket
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.function.Consumer

class NetworkClient(
    private val host: String = "localhost",
    private val port: Int = 9090
) {
    private var socket: Socket? = null
    private var input: ObjectInputStream? = null
    private var output: ObjectOutputStream? = null

    private val outgoingMessages: BlockingQueue<Payload> = LinkedBlockingQueue()
    
    private var messageListener: Consumer<Payload>? = null
    private var senderThread: Thread? = null
    private var receiverThread: Thread? = null
    
    @Volatile
    private var running = false

    fun setMessageListener(listener: (Payload) -> Unit) {
        messageListener = Consumer(listener)
    }

    fun connect(): Boolean {
        return try {
            println("[NetworkClient] Connecting to $host:$port...")
            socket = Socket(host, port)
            output = ObjectOutputStream(socket!!.getOutputStream())
            input = ObjectInputStream(socket!!.getInputStream())
            
            println("[NetworkClient] Connected!")
            
            running = true
            
            startSenderThread()
            startReceiverThread()
            
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
        
        println("[NetworkClient] Disconnected")
    }

    fun sendMessage(payload: Payload) {
        outgoingMessages.offer(payload)
    }

    private fun startSenderThread() {
        senderThread = Thread({
            println("[Sender] Thread started")
            
            while (running) {
                try {
                    val message = outgoingMessages.poll(1, TimeUnit.SECONDS)
                    
                    if (message != null) {
                        println("[Sender] Sending: ${message::class.simpleName}")
                        output?.writeObject(message)
                        output?.flush()
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
                    val obj = input?.readObject()
                    
                    if (obj == null) {
                        println("[Receiver] Server closed connection")
                        break
                    }
                    
                    if (obj is Payload) {
                        println("[Receiver] Received: ${obj::class.simpleName}")
                        messageListener?.accept(obj)
                    } else {
                        println("[Receiver] Received non-Payload object: ${obj::class.simpleName}")
                    }
                    
                } catch (e: EOFException) {
                    println("[Receiver] Server closed connection")
                    break
                } catch (e: IOException) {
                    if (running) {
                        System.err.println("[Receiver] Error reading from server: ${e.message}")
                    }
                    break
                } catch (e: ClassNotFoundException) {
                    System.err.println("[Receiver] Unknown class: ${e.message}")
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

    fun isConnected(): Boolean = socket?.let { !it.isClosed && running } ?: false
}
