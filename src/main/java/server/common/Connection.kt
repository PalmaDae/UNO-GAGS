package server.common

import proto.common.Method
import proto.common.NetworkMessage
import proto.dto.Payload
import java.io.*
import java.net.Socket
import java.util.concurrent.atomic.AtomicLong

/*
 * класс-обёртка над клиентом
 */

class Connection(val socket: Socket) {
    // потоки ввода-вывода
    private val output = ObjectOutputStream(socket.getOutputStream())
    private val input = ObjectInputStream(socket.getInputStream())

    private val messageId = AtomicLong(0)

    // пытаемся считать сообщение
    fun readMessage(): Payload? {
        return try {
            val message = input.readObject() as? NetworkMessage
            messageId.set(message?.id ?: messageId.get())
            message?.payload
        } catch (e: Exception) {
            println("[Server] Read Error: ${e.message}")
            null
        }
    }

    // костыль: клиент отсылает всегда метод OK :/
    fun sendMessage(msg: Payload) {
        try {
            val wrapped = NetworkMessage(
                id = messageId.getAndIncrement(),
                method = Method.OK,
                payload = msg
            )
            output.writeObject(wrapped)
            output.flush()
        } catch (e: IOException) {
            println("[Server] Write Error: ${e.message}")
        }
    }

    fun close() =
        try {
            input.close()
            output.close()
            socket.close()
        } catch (_: IOException) { }

    // строковое представление ip эндпоинта
    fun getRemoteAddress() = socket.remoteSocketAddress.toString()
}