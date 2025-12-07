package uno_ui

import uno_proto.dto.ChatMessage

/**
 * Tracks chat messages.
 */
class ChatModel {
    private val messages = mutableListOf<ChatMessage>()

    fun addMessage(message: ChatMessage) {
        messages.add(message)
    }

    fun getMessages(): List<ChatMessage> = messages.toList()

    fun clear() {
        messages.clear()
    }
}
