package uno_ui

import uno_proto.common.*
import uno_server.protocol.MessageParser

/**
 * Handles JSON serialization/deserialization of network messages.
 */
class MessageSerializer {
    private val parser = MessageParser()

    fun serialize(message: NetworkMessage): String = parser.toJson(message) ?: ""

    fun deserialize(json: String): NetworkMessage? = parser.fromJson(json)
}
