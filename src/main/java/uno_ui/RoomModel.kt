package uno_ui

import uno_proto.dto.LobbyUpdate

/**
 * Tracks room and lobby state.
 */
class RoomModel {
    var currentRoomId: Long? = null
    var lobbyState: LobbyUpdate? = null

    fun isInRoom(): Boolean = currentRoomId != null

    fun joinRoom(roomId: Long) {
        currentRoomId = roomId
    }

    fun leaveRoom() {
        currentRoomId = null
        lobbyState = null
    }

    fun updateLobby(update: LobbyUpdate) {
        lobbyState = update
    }

    fun reset() {
        leaveRoom()
    }
}
