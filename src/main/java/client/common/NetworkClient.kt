package client.common

import proto.common.Method
import proto.common.NetworkMessage
import proto.dto.OkMessage
import proto.dto.Payload
import java.io.*
import java.net.Socket
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import java.util.function.Consumer

class NetworkClient(
    private val host: String = "localhost",
    private val port: Int = 9090
) {
    private lateinit var socket: Socket
    private lateinit var input: ObjectInputStream
    private lateinit var output: ObjectOutputStream

    // todo изменить на NetworkMessage
    private val outgoingPayloads: BlockingQueue<Payload> = LinkedBlockingQueue()
    private val outgoingMessages: BlockingQueue<NetworkMessage> = LinkedBlockingQueue()

    private lateinit var messageListener: Consumer<NetworkMessage>
    private var senderThread: Thread? = null
    private var receiverThread: Thread? = null

    @Volatile
    private var running = false

    @Volatile
    private var isLaunched: Boolean = false

    private val id = AtomicLong(0) // id сообщения

    fun setMessageListener(listener: (NetworkMessage) -> Unit) {
        messageListener = Consumer(listener)
    }

    fun connect(): Boolean {
        outgoingMessages.offer(NetworkMessage(0, method = Method.OK, payload = OkMessage()))

        return try {
            println("[NetworkClient] Connecting to $host:$port...")
            socket = Socket(host, port)
            output = ObjectOutputStream(socket.getOutputStream())
            input = ObjectInputStream(socket.getInputStream())

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
            socket.takeIf { !it.isClosed }?.close()
        } catch (e: IOException) {
            System.err.println("[NetworkClient] Error closing socket: ${e.message}")
        }

        senderThread?.join(1000)
        receiverThread?.join(1000)

        println("[NetworkClient] Disconnected")
    }

    // todo изменить с отправки Payload на отправку NetworkMessage
    fun sendMessage(payload: Payload, method: Method): Boolean {
        return outgoingMessages.offer(
            NetworkMessage(
                id = id.getAndIncrement(),
                payload = payload,
                method = method
            )
        )
    }


    private fun startSenderThread() =
        if (isLaunched) {
            senderThread = Thread({
                println("[Sender] Thread started")

                while (running) {
                    try {
                        val message = outgoingMessages.poll(1, TimeUnit.SECONDS)

                        if (message != null) {
                            println("[Sender] Sending: ${message::class.simpleName}")
                            output.writeObject(message)
                            output.flush()
                        }
                    } catch (_: InterruptedException) {
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
        } else { }

    private fun startReceiverThread() =
        if (isLaunched) {
            receiverThread = Thread({
                println("[Receiver] Thread started")

                while (running) {
                    try {
                        val obj = input.readObject()

                        if (obj == null) {
                            println("[Receiver] Server closed connection")
                            break
                        }

                        if (obj is NetworkMessage) {
                            println("[Receiver] Received: ${obj::class.simpleName}")
                            messageListener.accept(obj)
                        } else
                            println("[Receiver] Received non-NetworkMessage object: ${obj::class.simpleName}")
                    } catch (_: EOFException) {
                        println("[Receiver] Server closed connection")
                        break
                    } catch (e: IOException) {
                        if (running)
                            System.err.println("[Receiver] Error reading from server: ${e.message}")
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
        } else { }


    fun isConnected(): Boolean = socket.let { !it.isClosed && running }
}
