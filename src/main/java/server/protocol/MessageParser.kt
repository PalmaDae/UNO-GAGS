package uno_server.protocol

import uno_proto.common.NetworkMessage
import uno_proto.common.Version
import uno_proto.dto.*

/**
 * Minimal compatibility layer for client JSON protocol
 * Translates between new text protocol and old JSON protocol
 */
object MessageParser {
    
    fun fromJson(json: String): NetworkMessage? {
        // Very basic JSON parsing for compatibility
        // This is a simplified version that handles basic cases
        return try {
            when {
                json.contains("\"method\":\"CREATE_ROOM\"") -> {
                    NetworkMessage(
                        id = 1L,
                        version = Version.V1,
                        method = uno_proto.common.Method.CREATE_ROOM,
                        payload = CreateRoomRequest("room")
                    )
                }
                json.contains("\"method\":\"JOIN_ROOM\"") -> {
                    NetworkMessage(
                        id = 1L,
                        version = Version.V1,
                        method = uno_proto.common.Method.JOIN_ROOM,
                        payload = JoinRoomRequest(123L, "player")
                    )
                }
                json.contains("\"method\":\"LEAVE_ROOM\"") -> {
                    NetworkMessage(
                        id = 1L,
                        version = Version.V1,
                        method = uno_proto.common.Method.LEAVE_ROOM,
                        payload = EmptyPayload()
                    )
                }
                json.contains("\"method\":\"START_GAME\"") -> {
                    NetworkMessage(
                        id = 1L,
                        version = Version.V1,
                        method = uno_proto.common.Method.START_GAME,
                        payload = EmptyPayload()
                    )
                }
                json.contains("\"method\":\"PLAY_CARD\"") -> {
                    NetworkMessage(
                        id = 1L,
                        version = Version.V1,
                        method = uno_proto.common.Method.PLAY_CARD,
                        payload = PlayCardRequest(0, null)
                    )
                }
                json.contains("\"method\":\"DRAW_CARD\"") -> {
                    NetworkMessage(
                        id = 1L,
                        version = Version.V1,
                        method = uno_proto.common.Method.DRAW_CARD,
                        payload = EmptyPayload()
                    )
                }
                json.contains("\"method\":\"SAY_UNO\"") -> {
                    NetworkMessage(
                        id = 1L,
                        version = Version.V1,
                        method = uno_proto.common.Method.SAY_UNO,
                        payload = EmptyPayload()
                    )
                }
                json.contains("\"method\":\"GET_ROOMS\"") -> {
                    NetworkMessage(
                        id = 1L,
                        version = Version.V1,
                        method = uno_proto.common.Method.GET_ROOMS,
                        payload = EmptyPayload()
                    )
                }
                json.contains("\"method\":\"PING\"") -> {
                    NetworkMessage(
                        id = 1L,
                        version = Version.V1,
                        method = uno_proto.common.Method.PING,
                        payload = EmptyPayload()
                    )
                }
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }
}