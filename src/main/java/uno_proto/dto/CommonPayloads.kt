package uno_proto.dto

import uno_proto.common.Payload

/**
 * Rooms list payload for GET_ROOMS response
 */
data class RoomsListPayload(
    val rooms: List<RoomInfo>
) : Payload

/**
 * Error payload for error responses
 */
data class ErrorPayload(
    val message: String,
    val code: String
) : Payload