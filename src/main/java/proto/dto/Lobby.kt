package proto.dto

data class CreateRoomRequest(
    val password: String? = null,
    val maxPlayers: Int = 4,
    val allowStuck: Boolean = false, // stack +2 or +4
    val allowStuckCards: Boolean = false, // stack numeric cards
    val infinityDrawing: Boolean = false // drawing while not finded
) : Payload

data class CreateRoomResponse(
    val roomId: Long,
    val password: String? = null,
    val isSuccessful: Boolean
) : Payload

data class JoinRoomRequest(
    val roomId: Long? = null,
    val password: String? = null,
    val username: String, // информация для лобака
    val avatar: String // информация для лобака
) : Payload

data class JoinRoomResponse(
    val roomId: Long,
    val isSuccessful: Boolean
) : Payload

data class LeaveRoomRequest(
    val roomId: Long
) : Payload

data class StartGameRequest(
    val roomId: Long
) : Payload

data class DrawCardRequest(
    val roomId: Long
) : Payload

data class SayUnoRequest(
    val roomId: Long
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
    val avatar: String,
    val isOwner: Boolean,
    val isReady: Boolean,
    val cardCount: Int,
    val hasUnoDeclared: Boolean
) : Payload

data class RoomsListPayload(
    val rooms: List<RoomInfo> = emptyList()
) : Payload