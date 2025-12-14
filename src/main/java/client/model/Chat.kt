package client.model

import proto.dto.ChatMessage

class Chat {
    private val messages = mutableListOf<ChatMessage>()

    fun addMessage(message: ChatMessage) {
        messages.add(message)
    }

    fun getMessages(): List<ChatMessage> = messages.toList()

    fun clear() {
        messages.clear()
    }
}
