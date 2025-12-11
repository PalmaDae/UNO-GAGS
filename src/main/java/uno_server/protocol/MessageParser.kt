package uno_server.protocol

import com.google.gson.*
import uno_proto.common.Method
import uno_proto.common.NetworkMessage
import uno_proto.common.Payload
import uno_proto.common.Version
import uno_proto.dto.*
import java.lang.reflect.Type

/**
 * MessageParser handles serialization and deserialization of NetworkMessage objects.
 * It uses Gson to convert between JSON strings and NetworkMessage objects,
 * with special handling for polymorphic Payload types based on the Method field.
 */
class MessageParser {
    private val gson: Gson

    init {
        // Create Gson instance with custom deserializer for NetworkMessage
        val builder = GsonBuilder()
        builder.registerTypeAdapter(NetworkMessage::class.java, NetworkMessageDeserializer())
        this.gson = builder.create()
    }

    /**
     * Converts a NetworkMessage to a JSON string.
     *
     * @param message The NetworkMessage to serialize
     * @return JSON string representation
     */
    fun toJson(message: NetworkMessage?): String? {
        return gson.toJson(message)
    }

    /**
     * Converts a JSON string to a NetworkMessage.
     * The Payload field is deserialized to the appropriate DTO type based on the Method.
     *
     * @param json The JSON string to deserialize
     * @return The deserialized NetworkMessage
     * @throws JsonSyntaxException if the JSON is invalid
     */
    @Throws(JsonSyntaxException::class)
    fun fromJson(json: String?): NetworkMessage? {
        return gson.fromJson(json, NetworkMessage::class.java)
    }

    /**
     * Custom deserializer for NetworkMessage that handles polymorphic Payload types.
     * Based on the Method field, it deserializes the payload to the correct DTO type.
     */
    private class NetworkMessageDeserializer : JsonDeserializer<NetworkMessage?> {
        @Throws(JsonParseException::class)
        override fun deserialize(
            json: JsonElement,
            typeOfT: Type?,
            context: JsonDeserializationContext
        ): NetworkMessage {
            val jsonObject = json.asJsonObject

            // Extract basic fields
            val id = jsonObject.get("id").asLong
            val version = context.deserialize<Version?>(jsonObject.get("version"), Version::class.java)
            val method = context.deserialize<Method?>(jsonObject.get("method"), Method::class.java)
            val timestamp =
                if (jsonObject.has("timestamp")) jsonObject.get("timestamp").asLong else System.currentTimeMillis()

            // Deserialize payload based on method type
            val payload = deserializePayload(jsonObject.get("payload"), method, context)

            // Create NetworkMessage using Kotlin constructor
            return NetworkMessage(id, version, method, payload, timestamp)
        }

        /**
         * Deserializes the payload field to the appropriate DTO type based on the method.
         *
         * @param payloadElement The JSON element containing the payload
         * @param method The method that determines the payload type
         * @param context The deserialization context
         * @return The deserialized Payload object
         */
        fun deserializePayload(
            payloadElement: JsonElement?,
            method: Method,
            context: JsonDeserializationContext
        ): Payload {
            if (payloadElement == null || payloadElement.isJsonNull) {
                return EmptyPayload
            }

            // Map method to payload DTO type
            when (method) {
                Method.CREATE_ROOM -> return context.deserialize(
                    payloadElement,
                    CreateRoomRequest::class.java
                )

                Method.ROOM_CREATED_SUCCESS, Method.ROOM_CREATED_ERROR -> return context.deserialize(
                    payloadElement,
                    CreateRoomResponse::class.java
                )

                Method.GET_ROOMS -> return EmptyPayload
                Method.ROOMS_LIST -> return context.deserialize(payloadElement, RoomsListPayload::class.java)
                Method.JOIN_ROOM -> return context.deserialize(payloadElement, JoinRoomRequest::class.java)
                Method.JOIN_ROOM_SUCCESS, Method.JOIN_ROOM_ERROR -> return context.deserialize(
                    payloadElement,
                    JoinRoomResponse::class.java
                )

                Method.LOBBY_UPDATE -> return context.deserialize(payloadElement, LobbyUpdate::class.java)
                Method.LOBBY_CHAT, Method.GAME_CHAT -> return context.deserialize(
                    payloadElement,
                    ChatMessage::class.java
                )

                Method.START_GAME, Method.GAME_START, Method.GAME_STATE -> return context.deserialize(
                    payloadElement,
                    GameState::class.java
                )

                Method.PLAY_CARD -> return context.deserialize(payloadElement, PlayCardRequest::class.java)
                Method.DRAW_CARD, Method.SAY_UNO -> return EmptyPayload

                Method.PING, Method.PONG, Method.OK -> return EmptyPayload
                Method.ERROR -> return context.deserialize(payloadElement, ErrorPayload::class.java)

                else -> return EmptyPayload
            }
        }
    }

    /**
     * Empty payload implementation for methods that don't require data.
     */
    internal object EmptyPayload : Payload {
        private fun readResolve(): Any = EmptyPayload
        private const val serialVersionUID = 1L
    }

    /**
     * Error payload for ERROR method responses.
     */
    class ErrorPayload(val message: String?, val code: String?) : Payload {
        companion object {
            private const val serialVersionUID = 1L
        }
    }

    /**
     * Payload for ROOMS_LIST method containing a list of available rooms.
     */
    class RoomsListPayload(val rooms: MutableList<RoomInfo?>?) : Payload {
        companion object {
            private const val serialVersionUID = 1L
        }
    }
}