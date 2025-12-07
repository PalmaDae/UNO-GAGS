package uno_ui

import uno_proto.common.Method
import uno_proto.dto.CreateRoomRequest
import uno_proto.dto.JoinRoomRequest
import uno_server.protocol.MessageParser
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.Socket

/**
 * Простой тест для проверки чата.
 * Создаёт двух клиентов, подключает к серверу и обменивается чат-сообщениями.
 */
fun main() {
    println("=== Chat Test ===")
    println("Этот тест проверяет простой протокол чата CHAT_MESSAGE|playerName|text")
    println()
    
    // Даём серверу время запуститься (если он не запущен, запустите вручную)
    Thread.sleep(1000)
    
    try {
        println("Подключаем Client 1...")
        val client1 = TestClient("Client1", "localhost", 9090)
        client1.connect()
        
        println("Подключаем Client 2...")
        val client2 = TestClient("Client2", "localhost", 9090)
        client2.connect()
        
        Thread.sleep(500)
        
        // Client 1 создаёт комнату
        println("\nClient 1 создаёт комнату...")
        client1.createRoom("TestRoom")
        Thread.sleep(500)
        
        // Client 2 присоединяется к комнате
        println("Client 2 присоединяется к комнате...")
        client2.joinRoom(1)
        Thread.sleep(500)
        
        // Отправляем чат-сообщения
        println("\n=== Тест чата ===")
        
        println("Client 1 отправляет чат...")
        client1.sendChat("Привет от Client 1!")
        Thread.sleep(300)
        
        println("Client 2 отправляет чат...")
        client2.sendChat("Привет от Client 2!")
        Thread.sleep(300)
        
        println("Client 1 отправляет ещё одно сообщение...")
        client1.sendChat("Как дела?")
        Thread.sleep(300)
        
        println("Client 2 отвечает...")
        client2.sendChat("Отлично! А у тебя?")
        Thread.sleep(1000)
        
        println("\n=== Результаты ===")
        println("\nClient 1 получил:")
        client1.printReceivedMessages()
        
        println("\nClient 2 получил:")
        client2.printReceivedMessages()
        
        // Закрываем соединения
        client1.disconnect()
        client2.disconnect()
        
        println("\n=== Тест завершён ===")
        
    } catch (e: Exception) {
        System.err.println("Ошибка в тесте: ${e.message}")
        e.printStackTrace()
    }
}

class TestClient(
    private val playerName: String,
    private val host: String,
    private val port: Int
) {
    private var socket: Socket? = null
    private var out: PrintWriter? = null
    private var reader: BufferedReader? = null
    private val receivedMessages = mutableListOf<String>()
    private var receiverThread: Thread? = null
    @Volatile
    private var running = false
    
    fun connect() {
        socket = Socket(host, port)
        out = PrintWriter(socket!!.getOutputStream(), true)
        reader = BufferedReader(InputStreamReader(socket!!.getInputStream()))
        
        running = true
        startReceiver()
        
        println("[$playerName] Подключён к $host:$port")
    }
    
    private fun startReceiver() {
        receiverThread = Thread {
            while (running) {
                try {
                    val line = reader?.readLine() ?: break
                    println("[$playerName] << $line")
                    receivedMessages.add(line)
                } catch (e: Exception) {
                    if (running) {
                        System.err.println("[$playerName] Ошибка чтения: ${e.message}")
                    }
                    break
                }
            }
        }.apply {
            isDaemon = true
            start()
        }
    }
    
    fun disconnect() {
        running = false
        socket?.close()
        receiverThread?.join(1000)
        println("[$playerName] Отключён")
    }
    
    fun createRoom(roomName: String) {
        val serializer = uno_ui.MessageSerializer()
        val request = CreateRoomRequest(roomName, null, 4, false)
        val message = uno_proto.common.NetworkMessage(
            1,
            uno_proto.common.Version.V1,
            Method.CREATE_ROOM,
            request,
            System.currentTimeMillis()
        )
        val json = serializer.serialize(message)
        out?.println(json)
        println("[$playerName] >> CREATE_ROOM")
    }
    
    fun joinRoom(roomId: Long) {
        val serializer = uno_ui.MessageSerializer()
        val request = JoinRoomRequest(roomId, null)
        val message = uno_proto.common.NetworkMessage(
            2,
            uno_proto.common.Version.V1,
            Method.JOIN_ROOM,
            request,
            System.currentTimeMillis()
        )
        val json = serializer.serialize(message)
        out?.println(json)
        println("[$playerName] >> JOIN_ROOM")
    }
    
    fun sendChat(text: String) {
        val message = "CHAT_MESSAGE|$playerName|$text"
        out?.println(message)
        println("[$playerName] >> $message")
    }
    
    fun printReceivedMessages() {
        val chatMessages = receivedMessages.filter { it.startsWith("CHAT_MESSAGE|") }
        if (chatMessages.isEmpty()) {
            println("  (чат-сообщений не получено)")
        } else {
            chatMessages.forEach { msg ->
                val parts = msg.split("|", limit = 3)
                if (parts.size >= 3) {
                    println("  [${parts[1]}]: ${parts[2]}")
                } else {
                    println("  $msg")
                }
            }
        }
    }
}
