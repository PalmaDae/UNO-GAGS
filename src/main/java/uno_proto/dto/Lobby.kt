package uno_proto.dto

import uno_proto.common.Payload

data class CreateRoomRequest(
    val roomName: String,
    val password: String? = null,
    val maxPlayers: Int = 4,
    val allowStuck: Boolean = false
) : Payload

data class CreateRoomResponse(
    val roomId: Long,
    val roomName: String,
    val isSuccessful: Boolean
) : Payload

data class JoinRoomRequest(
    val roomId: Long,
    val password: String? = null
) : Payload

data class JoinRoomResponse(
    val roomId: Long,
    val isSuccessful: Boolean
) : Payload

data class RoomInfo(
    val roomId: Long,
    val roomName: String,
    val hasPassword: Boolean,
    val maxPlayers: Int,
    val currentPlayers: Int,
    val status: RoomStatus,
    val creatorName: String
) : Payload

enum class RoomStatus {
    WAITING, IN_PROGRESS, FINISHED
}

data class LobbyUpdate(
    val players: List<PlayerInfo>,
    val roomStatus: RoomStatus
) : Payload

data class PlayerInfo(
    val userId: Long,
    val username: String,
    val isOwner: Boolean,
    val isReady: Boolean
) : Payload